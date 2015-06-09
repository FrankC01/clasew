(ns
  ^{:author "Frank V. Castellucci"
      :doc "clasew example 4 - Excel Example"}
  clasew.examples4
  (:require [clasew.spreads :as cs]
            [clasew.excel :as es]
            [clojure.pprint :refer :all])
  )

;;; Setup for the example

(def p pprint)

;;; Demonstrate using Excel as a data store

;; ---------------- BASIC CALLING ---------------------------------------------


(def sample1 (cs/clasew-script "clasew-ex4-sample1.xlsx"
      cs/create cs/no-open "path to desktop" nil
      {:handler_list [] :arg_list []}))

;(p sample1)
;(p (es/clasew-excel-call! sample1))

(def sample1a (cs/clasew-script
               "clasew-ex5-sample1a.xlsx"
               cs/create cs/no-open "path to desktop"
               (cs/create-parms
                :sheet_name "Content"
                :table_list (cs/tables
                             (cs/table :table_name "First Table",
                                    :column_offset 1,
                                    :row_offset 1,
                                    :column_count 5,
                                    :row_count 5,
                                    :header_content ["Date","Region","Sales"])))
               {:handler_list ["clasew_get_book_info","clasew_quit"] :arg_list [[],[]]}))

;(p sample1a)
;(def s1ar (es/clasew-excel-call! sample1a))
;(p (first (:result s1ar)))

;; Handler Chaining - Create, put values, get first row, save, quit

(def datum (into []
      (map vec
           (for [i (range 10)]
             (repeatedly 10 #(rand-int 100))))))

;(p datum)

(def sample2 (cs/clasew-script "clasew-ex4-sample2.xlsx"
      cs/create cs/no-open "path to desktop"  nil
      (cs/clasew-handler [[:put-range-data "Sheet1" "A1:J10" datum]
                             [:get-range-data "Sheet1" "A1:A10"]
                             [:save-quit]])))

;(p sample2)
;(p (es/clasew-excel-call! sample2))

(def sample2a (cs/clasew-script "clasew-ex4-sample2.xlsx"
      cs/no-create cs/open "path to desktop"  nil
      (cs/clasew-handler [[:get-range-formulas "Sheet1" "used"]
                                [:quit-no-save]])))


;(p sample2a)
;(p (es/clasew-excel-call! sample2a))
;; Demo - Open, Read, Quit

(def sample3 (cs/clasew-script "clasew-ex4-sample2.xlsx"
      cs/no-create cs/open "path to desktop"  nil
      (cs/clasew-handler [[:book-info]
                             [:quit]])))

;(p sample3)
;(p (es/clasew-excel-call! sample3))

;; Demo pushing your own agenda

(def my_script
  "on run(caller, args)
    say \"I have control!\"
    return {my_script: \"No you don't\"}
  end run")

(def sample4 (cs/clasew-script "clasew-ex4-sample2.xlsx"
      cs/no-create cs/open "path to desktop"  nil
      (cs/clasew-handler [[:run-script my_script []]
                                [:quit]])))

;(p (es/clasew-excel-call! sample4))

;; Demo HOF - Create, put values, get first row, save, quit

(def wrkbk-name "clasew-ex4-sample5.xlsx")
(def wrkbk-path "path to desktop")

(def sample5 (cs/create-wkbk wrkbk-name wrkbk-path
                     (cs/chain-put-range-data "Sheet1" datum)
                     [:save-quit]))


;(p (es/clasew-excel-call! sample5))


;; Demo misc forms


; Uniform collection
(def uniform [[1 2 3] [4 5 6] [7 8 9]])

; Jagged collection
(def jagged [[1] [2 2] [3 3 3]])

(cs/format-range-a1 [0 0 0 9])
(cs/format-range-a1 [0 1 1 9])

(cs/formula-wrap "SUM" cs/format-range-a1 [0 0 0 9])
(cs/formula-wrap "AVG" cs/format-range-a1 [0 0 0 9])

(cs/fsum cs/format-range-a1 [0 0 0 9])
(cs/favg cs/format-range-a1 [0 0 0 9])

(cs/row-ranges uniform)
(cs/row-ranges jagged)
(cs/row-ranges (cs/pad-rows jagged))

(cs/column-ranges uniform)
(cs/column-ranges jagged)
(cs/column-ranges (cs/pad-rows jagged))

(map cs/format-range-a1 (cs/column-ranges uniform))
(map cs/format-range-a1 (cs/row-ranges uniform 4 3))
(map cs/format-range-a1 (cs/row-ranges jagged))

(cs/sum-by-row uniform)
(cs/sum-by-col uniform)

(cs/avg-by-row uniform)
(cs/avg-by-col uniform)

(cs/extend-rows uniform 0 0 cs/sum-by-row)
(cs/extend-columns uniform 0 0 cs/avg-by-col)

;; Put it together

(def sample5a (cs/create-wkbk "clasew-ex4-sample5a.xlsx" wrkbk-path
      (cs/chain-put-range-data "Sheet1"
                               (cs/extend-columns
                                (cs/extend-rows
                                 datum 0 0 cs/sum-by-row cs/avg-by-row)
                                0 0 cs/avg-by-col))
                               [:save-quit]))

;(p sample5a)
;(p (es/clasew-excel-call! sample5a))

;; Demo - cleaning up results

(def sample6 (cs/open-wkbk wrkbk-name wrkbk-path
                     [:all-book-info] [:quit]))

;(def s6r (cs/clean-result (es/clasew-excel-call! sample6)))
;(p s6r)

;; Demo - Sheet manipulations

(def sample7 (cs/create-wkbk "clasew-ex4-sample7.xlsx" wrkbk-path
                             (cs/chain-add-sheet
                              "Before Sheet1" :before "Sheet1"
                              "After Before Sheet1" :after "Before Sheet1"
                              "The End" :at :end
                              (cs/tables
                               (cs/table :table_name "First Table",
                                         :row_count 5,
                                         :column_count 5))
                              "The Beginning" :at :beginning
                              "Also Before Sheet1" :at 4)
                             [:save-quit]))


;(p sample7)
;(p (cs/clean-result (es/clasew-excel-call! sample7)))

(def sample8 (cs/open-wkbk "clasew-ex4-sample7.xlsx" wrkbk-path
                 (cs/chain-delete-sheet "Sheet1") [:save-quit]))

;(p sample8)
;(p (cs/clean-result (es/clasew-excel-call! sample8)))
