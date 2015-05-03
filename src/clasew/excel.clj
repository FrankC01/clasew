(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - Excel DSL"}
  clasew.excel
  (:require [clasew.core :as as]
            [clojure.pprint :refer :all]
            [clojure.java.io :as io]
            [clojure.walk :as w]))

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
   :get-range-info      "clasew_excel_get_range_info"

   :get-range-formulas  "clasew_excel_get_range_formulas"

   :get-range-data      "clasew_excel_get_range_data"
   :put-range-data      "clasew_excel_put_range_data"

   :book-info           "clasew_excel_get_book_info"
   :all-book-info       "clasew_excel_get_all_book_info"

   :add-sheet           "clasew_excel_add_sheet"
   :delete-sheet        "clasew_excel_delete_sheet"

   :save                "clasew_excel_save"
   :save-as             "clasew_excel_save_as"
   :save-quit           "clasew_excel_save_and_quit"
   :quit                "clasew_excel_quit"
   :quit-no-save        "clasew_excel_quit_no_save"

   :run-script          "clasew_excel_run"
   })

;; Pure helpers

(defn modify-keys
  "Uses zipmap to process 'f' on keys
  Used for record types in scripts"
  [f m]
  (if (map? m)
    (zipmap (map f (keys m)) (vals m))
    m))

(defn- ocm
  "prewalk function takes output from AppleScript
  converts to clojure types and, if map, swizzle string to keyword keys"
  [item]
  (if (instance? java.util.ArrayList item)
    (into [] item)
    (if (instance? java.util.HashMap item)
      (modify-keys keyword (into {} item))
      item)))


(defn clean-excel-result
  "Iterates through the result vectors exchanging keywords for
  strings in any maps"
  [{:keys [result] :as return-map}]
  (assoc return-map :result (w/prewalk ocm result)))

(def ^:const ^:private base-char 65)

(defn get-excel-a1
  "Convert zero based column and row number to Excel 'A1' address form"
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

(defn clasew-excel-call!
  "Takes 1 or more maps produced from clasew-excel-script and invokes AppleScript
  for execution.
  Return map is same as clasew.core/run-ascript!"
  [& scripts]
  {:pre [(> (count scripts) 0)]}
  (let [argv (list (into [] scripts))]
    (with-open [rdr (io/reader scrptfile)]
      (as/run-ascript! excel-eng rdr
                      :reset-binding true
                      :bind-function "clasew_excel_eval"
                      :arguments argv))))

(defn clasew-excel-script
  "Produces a script data structure (map) ready to be used by casew-exec-call
  wkbk     - String containing name of the workbook (with extension)
  crf      - create if workbook named is not opened
  opf      - open if workbook named is not already opened
  fqn      - Fully qualifed unix path or AppleScript 'path to xxx' command
  handler  - result from clasew-excel-handler call"
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
(defn clasew-excel-handler
  "Sets up the handler and argument lists
  chains - a collection of one or more chains"
  [chains]
  (reduce handler-acc {:handler_list [] :arg_list []} chains))

;; High level support

(defonce ^:private sheet-coords
  {
     :before     -1
     :after      1
     :at         0
     :beginning  0
     :end        -1
   })

(defn- resolve-add-directives
  "Fixup add sheet directives"
  [[new_sheet target relative_to]]
  {"new_sheet" new_sheet "target" (get sheet-coords target)
   "relative_to" (get sheet-coords relative_to relative_to)})


;; High level DSL functions ---------------------------------------------------

(defn create-wkbk
  "Creates the script to create new workbook in excel (if not open)
  bookname - Name of workbook (with extension) that will be created
  path - fully qualified unix path to where the file will be saved - or -
         'path to ...' AppleScript command string
  chains - 0 or more vectors, each describing handler call and arguments"
  [bookname path & chains]
  (clasew-excel-script bookname create no-open path
                      (clasew-excel-handler chains)))

(defn open-wkbk
  "Creates the script to open an existintg workbook in excel (if not open)
  bookname - Name of workbook (with extension) that will be created
  path - fully qualified unix path to where the file will be saved - or -
         'path to ...' AppleScript command string
  chains - 0 or more vectors, each describing handler call and arguments"
  [bookname path & chains]
  (clasew-excel-script bookname no-create open path
                      (clasew-excel-handler chains)))

(defn- get-data-dimensions
  [data]
  {:pre [(and (vector? data) (vector? (first data)))]}
  [(dec (count (first data))) (dec (count data))])

;; Chain Sugar ----------------------------------------------------------------

(defn chain-put-range-data
  "Sets up a chain for putting a data range into workbook's sheet-name. Output
  also includes the necessary Excel range signature.
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
  sheet-name - the name of the sheet to get the data from
  start-col offset column for data 0 = A
  start-row offset row for data 0 = 1
  for-col number of columns to retrieve
  for-row number of rows to retrieve"
  [sheet-name start-col start-row for-col for-row]
  (let [ec (+ start-col (dec for-col))
        er (+ start-row (dec for-row))]
    [:get-range-data sheet-name (str (get-excel-a1 start-col start-row) ":"
                                   (get-excel-a1 ec er))]))

(defn chain-add-sheet
  "Adds new sheets to current workbook
  directives - collection of expressions where each expression is in the form
    new_sheet (string)
    target    (keyword)
    relative to (keyword | positive number)'
  where target is one of:
    :before - insert this new sheet before sheet-name
    :after  - insert this new sheeet after sheet-name
    :at     - insert this sheet at relative_to
  where relative_to is one of:
    :beginning
    :end
    positive number - if greater then worksheet count in book, put first"
  [& directives]
  {:pre [(and (not (empty? directives)) (>= (count directives) 3)
              (= (rem (count directives) 3) 0))]}
  (into [:add-sheet] (map resolve-add-directives (partition 3 directives))))

(defn chain-delete-sheet
  "Delete existing sheets from workbook
  Supports specifying sheet name or ordinal position. Before delete occurs
  each ordinal is resolved to a sheet name"
  [& sheets]
  {:pre [(and (not (empty? sheets)) (> (count sheets) 0))]}
  (into [:delete-sheet] sheets))

