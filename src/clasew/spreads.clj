(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - common Spreadsheet DSL"}
  clasew.spreads
  (:require [clojure.java.io :as io]
            [clojure.walk :as w]))

;; True/False strings for sending into AppleScript (converted in script)

(defonce create "true")
(defonce no-create "false")
(defonce open "true")
(defonce no-open "false")

;; Well know handler map

(def handler-map
  {
   :get-range-info      "clasew_get_range_info"

   :get-range-formulas  "clasew_get_range_formulas"

   :get-range-data      "clasew_get_range_data"
   :put-range-data      "clasew_put_range_data"

   :book-info           "clasew_get_book_info"
   :all-book-info       "clasew_get_all_book_info"

   :add-sheet           "clasew_add_sheet"
   :delete-sheet        "clasew_delete_sheet"

   :save                "clasew_save"
   :save-as             "clasew_save_as"
   :save-quit           "clasew_save_and_quit"
   :quit                "clasew_quit"
   :quit-no-save        "clasew_quit_no_save"

   :run-script          "clasew_run"
   })

;;
;; Pure helpers
;;

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


(defn clean-result
  "Iterates through the result vectors exchanging keywords for
  strings in any maps"
  [{:keys [result] :as return-map}]
  (assoc return-map :result (w/prewalk ocm result)))

(def ^:const ^:private base-divisor 26)          ; letters in ascii alphabet
(def ^:const ^:private base-char 65)             ; "A"

(defn format-a1
  "Convert zero based column and row number to 'A1' address form"
  [col-num row-num]
  (loop [cc (inc col-num)
         acc (conj '() (inc row-num))]
    (if (>= 0 cc)
        (apply str acc)
      (let [md (mod (- cc 1) base-divisor)]
        (recur (int (/ (- cc md) base-divisor))
               (conj acc (char (+ base-char md))))))))

(defn format-range-a1
  "Produces a range signature in the form '??:??'"
  [[sr sc er ec]]
  (str (format-a1 sr sc) ":" (format-a1 er ec)))

(defn data-dimensions
  "Returns a zero based dimension of the data block and
  assumes that the first internal collection is representative of all rows
  data - vector of vectors
  returns vector of two values: column count and row count"
  [data]
  {:pre [(and (vector? data) (vector? (first data)))]}
  [(dec (count (first data))) (dec (count data))])

(defn pad-rows
  "Returns a collection with uniform length sub-collections. Uniform Length is
  determined by max length of sub-collections in coll. Those with length less
  than max are padded to the right with padval or 0 by default.
  coll - the input collection of collections
  padval - pad value (defaults to 0)"
  [coll & [padval]]
  (let [basel (into #{} (map count coll))
        rmax (apply max basel)
        pad (or padval 0)]
    (if (<= (count basel) 1)
      coll
      (reduce #(conj %1 (vec %2)) (empty coll)
              (partition
               rmax
               (mapcat #(into % (repeat (- rmax (count %)) pad))
                       coll))))))
;;
;; Low level DSL functions ----------------------------------------------------
;;

(defn clasew-script
  "Produces a script data structure (map) ready to be used by casew-yyyy-call!
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
(defn clasew-handler
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

;;
;; High level DSL functions ---------------------------------------------------
;;


(defn create-wkbk
  "Creates the script to create new workbook in excel (if not open)
  bookname - Name of workbook (with extension) that will be created
  path - fully qualified unix path to where the file will be saved - or -
         'path to ...' AppleScript command string
  chains - 0 or more vectors, each describing handler call and arguments"
  [bookname path & chains]
  (clasew-script bookname create no-open path
                      (clasew-handler chains)))

(defn open-wkbk
  "Creates the script to open an existintg workbook (if not open)
  bookname - Name of workbook (with extension) that will be created
  path - fully qualified unix path to where the file will be saved - or -
         'path to ...' AppleScript command string
  chains - 0 or more vectors, each describing handler call and arguments"
  [bookname path & chains]
  (clasew-script bookname no-create open path
                      (clasew-handler chains)))

;;
;; Chain Sugar ----------------------------------------------------------------
;;

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
        [ec er] (data-dimensions data)
        a1_range (format-range-a1 (list sc sr (+ sc ec) (+ sr er)))]
    [:put-range-data sheet-name a1_range data])
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
    [:get-range-data sheet-name (str (format-a1 start-col start-row) ":"
                                   (format-a1 ec er))]))

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
;;
;; More Sugar -----------------------------------------------------------------
;;

(defn formula-wrap
  "Setup the standard  '=formula(blah blah blah)' format.
  form-prefix is one of SUM, AVERAGE, etc.
  f           function to perform on arg that results in valid formula arguments
  arg -       whatever arguments are required for the f function to do it's thing"
  [form-prefix f arg]
  (str "=" form-prefix "(" (f arg) ")"))

(defn fsum
  "Thin wrapper for setting up '=SUM(...)'"
  [f arg]
  (formula-wrap "SUM" f arg))

(defn favg
  "Thin wrapper for setting up '=AVERAGE(...)'"
  [f arg]
  (formula-wrap "AVERAGE" f arg))

(defn row-ranges
  "Creates a sequence of full row ranges derived from the data dimensions
  along with offsets
  data - a vector of vectors
  sc - starting column offset (0 based)
  sr - starting row offset (0 based)"
  [data & [sc sr]]
  (let [dc (or sc 0)
        dr (or sr 0)
        rwrng (range dr (+ dr (count data)))]
    (map #(list dc %2 (dec (+ (count %1) dc)) %2) data rwrng)))

(defn column-ranges
  "Creates a sequence of full column ranges derived from the data dimensions
  along with offsets
  data - a vector of vectors
  sc - starting column offset (0 based)
  sr - starting row offset (0 based)"
  [data & [sc sr]]
  (let [dc (or sc 0)
        dr (or sr 0)]
    (map #(list % dr % (dec (+ (count data) dr)))
         (range dc (+ (count (first data)) dc)))))

(defn sum-by-row
  "Produces sequence of row sum formulas from input data"
  [data & [sc sr]]
  (into [] (map #(fsum format-range-a1 %)
                (row-ranges data sc sr))))

(defn sum-by-col
  "Produces sequence of column sum formulas from input data"
  [data & [sc sr]]
  (into [] (map #(fsum format-range-a1 %)
                (column-ranges data sc sr))))

(defn avg-by-row
  "Produces sequence of row average formulas from input data"
  [data & [sc sr]]
  (into [] (map #(favg format-range-a1 %)
                (row-ranges data sc sr))))

(defn avg-by-col
  "Produces sequence of row average formulas from input data"
  [data & [sc sr]]
  (into [] (map #(favg format-range-a1 %)
                (column-ranges data sc sr))))

(defn extend-rows
  "Returns the collection with rows extended to include results of applying one
  or more functions to the input collection."
  [coll start-col start-row formfn & formfns]
  (let [nd (map #(% coll start-col start-row) (conj formfns formfn))]
    (vec (map #(into %1 %2) coll (apply map vector nd)))
    ))

(defn extend-columns
  "Returns the collection with columns extended to include results of applying
  one or more functions to the input collection."
  [coll start-col start-row formfn & formfns]
  (let [nd (map #(% coll start-col start-row) (conj formfns formfn))]
    (into coll nd)))
