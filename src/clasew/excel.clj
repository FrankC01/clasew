(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - Excel DSL"}
  clasew.excel
  (:require [clasew.core :as as]
            [clojure.pprint :refer :all]
            [clojure.java.io :as io]))

;; Ease of printing
(def p pprint)

;; Setup our own engine

(defonce ^:private excel-eng as/new-eng)   ; Use engine for this namespace

;; True/False strings for sending into AppleScript (converted in script)

(defonce create "true")
(defonce no-create "false")
(defonce open "true")
(defonce no-open "false")

;; Well know handler map

(def handler-map
  {
   :get-used-range-info "clasew_excel_get_used_range_info"
   :get-range-info      "clasew_excel_get_range_info"

   :get-range-formulas  "clasew_excel_get_range_formulas"

   :get-range-data      "clasew_excel_get_range_values"
   :put-range-data      "clasew_excel_put_range_values"

   :book-info           "clasew_excel_get_book_info"
   :all-book-info       "clasew_excel_get_all_book_info"

   :save                "clasew_excel_save"
   :save-as             "clasew_excel_save_as"
   :save-quit           "clasew_excel_save_and_quit"
   :quit                "clasew_excel_quit"

   :run-script          "clasew-excel_run"
   })

;; Pure helpers

(defn modify-keys
  "Uses zipmap to process 'f' on keys
  Used for record types in scripts"
  [f m]
  (if (map? m)
    (zipmap (map f (keys m)) (vals m))
    m))

(defn clean-excel-result
  "Iterates through the result vectors exchanging keywords for
  strings in any maps"
  [{:keys [result] :as return-map}]
  (loop [acc []
         f (first result)
         r (rest result)]
    (if (nil? f)
      (assoc return-map :result acc)
      (recur
       (conj acc (into [] (map #(if (instance? java.util.HashMap %)
                                  (modify-keys keyword (into {} %))
                                  %) f)))
       (first r)
       (rest r)))))

(def ^:const ^:private base-char 65)

(defn get-excel-a1
  "Convert zero based column and row number to Excel address"
  [col-num row-num]
  {:pre [(>= col-num 0) (< col-num 16384)
         (>= row-num 0) (< row-num 1048576)]}
  (loop [cc (inc col-num)
         acc (conj '() (inc row-num))]
    (if (>= 0 cc)
        (apply str acc)
      (let [md (mod (- cc 1) 26)]
        (recur (int (/ (- cc md) 26))
               (conj acc (char (+ base-char md))))))))


;; Low level DSL functions ----------------------------------------------------

(def ^:private scrptfile (io/resource "clasew-excel.applescript"))

(defn casew-excel-call!
  "Takes 1 or more results of casew_excel_script and invokes AppleScript
  for execution"
  [& scripts]
  {:pre [(> (count scripts) 0)]}
  (let [argv (list (into [] scripts))]
    (with-open [rdr (io/reader scrptfile)]
      (as/run-ascript! excel-eng rdr
                      :reset-binding true
                      :bind-function "clasew_excel_eval"
                      :arguments argv))))

(defn casew-excel-script
  "produces a script object format ready to be used in casew-exec-call
  wkbk     - String containing name of the workbook (with extension)
  crf      - create if workbook named is not opened
  opf      - open if workbook named is not already opened
  fqn      - Fully qualifed unix path or AppleScript 'path to xxx' command
  handler  - result from casew_excel_handler call"
  [wkbk crf opf fqn handlers]
  (modify-keys #(str (name %))
               (merge {:work_book wkbk, :create_ifm crf
                       :open_ifm opf :fqn_path fqn} (identity handlers))))

(defn- handler-acc
  "Accumulator function for hanlder setup reduce"
  [acc [v1 & v2]]
  (update-in (update-in acc [:handler_list] conj (or (get handler-map v1) v1))
             [:arg_list] conj (if (nil? v2)
                                []
                                (if (vector? v2) v2 (into [] v2)))))
(defn casew-excel-handler
  "Sets up the handler and argument lists
  chains - a collection of one or more chains"
  [chains]
  (reduce handler-acc {:handler_list [] :arg_list []} chains))


;; High level DSL functions ---------------------------------------------------

(defn create-wkbk
  "Creates the script to create new workbook in excel (if not open)
  bookname - Name of workbook (with extension) that will be created
  path - fully qualified unix path to where the file will be saved - or -
         'path to ...' AppleScript command string
  chains - 0 or more vectors, each describing handler call and arguments"
  [bookname path & chains]
  (casew-excel-script bookname create no-open path
                      (casew-excel-handler chains)))

(defn open-wkbk
  "Creates the script to open an existintg workbook in excel (if not open)
  bookname - Name of workbook (with extension) that will be created
  path - fully qualified unix path to where the file will be saved - or -
         'path to ...' AppleScript command string
  chains - 0 or more vectors, each describing handler call and arguments"
  [bookname path & chains]
  (casew-excel-script bookname no-create open path
                      (casew-excel-handler chains)))

(defn- get-data-dimensions
  [data]
  {:pre [(and (vector? data) (vector? (first data)))]}
  [(dec (count (first data))) (dec (count data))])

;; Chain Sugar ----------------------------------------------------------------

(defn chain-put-range-data
  "Sets up a chain for putting a data range into workbook's sheet-name
  sheet-name - the name of the sheet to put the data in
  data - a vector of vectors with inner vectors containing the data
  start-col offset column for data 0 = A
  start-row offset row for data 0 = 1"
  [sheet-name data & [start-col start-row]]
  (let [sc (or start-col 0)
        sr (or start-row 0)
        [ec er] (get-data-dimensions data)
        srange (get-excel-a1 sc sr)
        erange (get-excel-a1 (+ sc ec) (+ sr er))]
    [:put-range-data sheet-name (str srange ":" erange) data])
  )

(defn chain-get-range-data
  "Sets up a chain for getting a data range from workbook's sheet-name
  sheet-name - the name of the sheet to put the data in
  start-col offset column for data 0 = A
  start-row offset row for data 0 = 1
  for-col number of columns to retrieve
  for-row number of rows to retrieve"
  [sheet-name start-col start-row for-col for-row]
  (let [ec (+ start-col (dec for-col))
        er (+ start-row (dec for-row))]
    [:get-range-data sheet-name (str (get-excel-a1 start-col start-row) ":"
                                   (get-excel-a1 ec er))]))


