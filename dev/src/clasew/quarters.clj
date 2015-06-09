(ns
  ^{:author "Frank V. Castellucci"
      :doc "clasew example 5 - Apple Numbers Examples"}
  clasew.quarters
  (:require [clasew.spreads :as cs]
            [clasew.numbers :as an]
            [clasew.excel :as es]
            [clojure.pprint :refer :all]))

(def p pprint)


;;; Environment data

(def excel! es/clasew-excel-call!)
(def numbers! an/clasew-numbers-call!)

;;; Faux table setup and data

(def sheet "FY15")
(def ytd-sales "YTD - Sales")
(def q1-sales "Q1 - Sales")
(def q2-sales "Q2 - Sales")
(def q3-sales "Q3 - Sales")
(def q4-sales "Q4 - Sales")
(def tbls [ytd-sales,q1-sales ,q2-sales,q3-sales,q4-sales])
(def header ["Region", "Sales", "Returns"])

(def Q115 [["A",100,0],["B",82,5],["C",120,20],["D",59,0],["E",240,10]])
(def Q215 [["A",205,18],["B",52,0],["C",140,10],["D",87,3],["E",120,10]])
(def Q315 [["A",13,0],["B",42,0],["C",240,10],["D",78,3],["E",320,30]])
(def Q415 [["A",50,8],["B",32,0],["C",340,12],["D",110,3],["E",110,5]])
(def data-columns 3)
(def data-rows 6)

(def wrkbk-path "path to desktop")
(def wrkbk-name "2015 Sales By Quarter")

;;; Application speceific

(def numbers-wrkbk (str wrkbk-name ".numbers"))
(def excel-wrkbk (str wrkbk-name ".xlsx"))
(def excel-col-offset 2)
(def excel-row-offset 1)

;;;
;;; Ease of use functions
;;;

(defn tableseq
  "Creates a table sequence."
  [tabls hdrs cols rows & [col_offset row_offset]]
  (if-let [roff (or row_offset col_offset)]
    (loop [tg  (first tabls)
           x   (rest tabls)
           nc  (or row_offset 0)
           acc []]
      (if (nil? tg)
        (seq acc)
        (recur (first x) (rest x) (+ nc rows (or row_offset 0))
               (conj acc
                     (cs/table :table_name tg :row_count rows
                               :row_offset nc
                               :column_offset (or col_offset 2)
                               :column_count cols :header_content hdrs)))))
    ; Otherwise no offset passed
    (map #(cs/table :table_name % :row_count rows
                    :column_count cols :header_content hdrs) tabls)))

(defn gen-tables
  "Generates the tables for our FY15 sales tracking model.
  table-seq is a sequence of table maps"
  [table-seq]
  (apply cs/tables table-seq))

(defn quote-table-name
  [st]
  (str "'" st "'"))

(defn address-reducer
  "Reduce the address information"
  [cfun rfun data tprefix]
  (reduce #(conj %1 (if (false? tprefix)
                 (cs/format-a1 (cfun (:column_offset %2))
                               (rfun (:row_offset %2)))
                 (str (quote-table-name (:table_name %2)) tprefix
                      (cs/format-a1 (cfun (:column_offset %2))
                                    (rfun (:row_offset %2))))))
          [] data))

(defn ytd-formulas
  "Calculate the formulas to insert into YTD table."
  [prefix sht & [[t0 & dt]]]

  ; First column is for referencing the titles (regions) from the first
  ; data table only (e.g. t0(a1)=t1(a1))
  (let [cnt (:row_count t0)
        v0  (into [] (for [x (range 1 cnt)]
              (apply cs/set-to-formula
                     (address-reducer identity
                                      (partial + x)
                                      (list (first dt))
                                      prefix))))
        ; The sum forumulas apply to everything else
        ; (e.g. t0(b1) = t1(b1)+t2(b1)+...)
        vx (for [ox (range 1 cnt)]
             (for [x (range 1 (count (rest (:header_content t0))))]
               (apply cs/set-to-formula
                      (interpose "+"
                                 (address-reducer
                                  (partial + x)
                                  (partial + ox)
                                  dt
                                  prefix)))))
        vy (into [] (map #(into [%1] %2) v0 vx))
        ]
    (cs/chain-put-range-data sht vy
                               (:column_offset t0) (inc (:row_offset t0)))
    ))

(defn set-ytd-formulas
  "Create all the formulas to populate the YTD table"
  [prefix sht table-seq]
  (ytd-formulas prefix sht table-seq))

(defn get-table
  "Performs fetch of table definitions"
  [t-name table-model]
  (if-let [trg (first (filter #(= t-name (:table_name %)) table-model))]
    trg
    (throw (Exception. (str "Table " t-name " not found")))))

(defn put-data
  "Setup the call to populate the quarter tables"
  [t-name data table-model]
  (let [trg (get-table t-name table-model)]
    (cs/chain-put-range-data sheet data
                             (:column_offset trg)
                             (inc (:row_offset trg))
                             (:table_name trg))))

(defn get-data
  "Setup parameters to fetch data totals"
  [t-name table-model dkw]
  (let [trg (get-table t-name table-model)
        fnc (if (= dkw :data)
              (partial cs/chain-get-range-data)
              (partial cs/chain-get-range-formulas))]
    (fnc sheet
         (:column_offset trg)
         (inc (:row_offset trg))
         (:column_count trg)
         (dec (:row_count trg))
         (:table_name trg))))

;;;
;;; High order Table Definitions stored for downstream functions
;;;

(def excel-tables (tableseq tbls header
                            data-columns data-rows
                            excel-col-offset excel-row-offset))

(def numbers-tables (tableseq tbls header
                              data-columns data-rows))

;(p excel-tables)
;(p numbers-tables)

;;;
;;; Script definitions for workbook and table creation
;;;

(def quarters-excel (cs/create-wkbk
                     excel-wrkbk wrkbk-path
                     (cs/create-parms
                      :sheet_name sheet
                      :table_list (gen-tables excel-tables))
                     [:save-quit]))

(def quarters-numbers (cs/create-wkbk
                       numbers-wrkbk wrkbk-path
                       (cs/create-parms
                        :sheet_name sheet
                        :table_list (gen-tables numbers-tables))
                       [:save-quit]))

;; Execute table creations
;(p quarters-excel)
(p (excel! quarters-excel))
;(p (numbers! quarters-numbers))

;;;
;;; Script definitions for populating YTD table with formulas
;;;

(def formulas-excel (cs/open-wkbk
                     excel-wrkbk wrkbk-path
                     (set-ytd-formulas false sheet excel-tables)
                     [:save-quit]))

(def formulas-numbers (cs/open-wkbk
                     numbers-wrkbk wrkbk-path
                     (set-ytd-formulas "::" sheet numbers-tables)
                     [:save-quit]))

;; Execute YTD formula creations
;(p (excel! formulas-excel))
;(p (numbers! formulas-numbers))

;;;
;;; Script definitions for populating Q tables with data
;;;

(def dataQs-excel (cs/open-wkbk
                   excel-wrkbk wrkbk-path
                   (put-data q1-sales Q115 excel-tables)
                   (put-data q2-sales Q215 excel-tables)
                   (put-data q3-sales Q315 excel-tables)
                   (put-data q4-sales Q415 excel-tables)
                   (get-data ytd-sales excel-tables :data)
                   [:save-quit]))

(def dataQs-numbers (cs/open-wkbk
                     numbers-wrkbk wrkbk-path
                     (put-data q1-sales Q115 numbers-tables)
                     (put-data q2-sales Q215 numbers-tables)
                     (put-data q3-sales Q315 numbers-tables)
                     (put-data q4-sales Q415 numbers-tables)
                     (get-data ytd-sales numbers-tables :data)
                     [:save-quit]))

;; Execute quarter population and YTD results
;(p (excel! dataQs-excel))
;(p (numbers! dataQs-numbers))
