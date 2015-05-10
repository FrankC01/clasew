(ns
  ^{:author "Frank V. Castellucci"
      :doc "clasew example 4 - An Excel Example"}
  clasew.examples4
  (:require [clasew.excel :as es]
            [clojure.pprint :refer :all]
            [clojure.walk :as w])
  )

;;; Setup for the example

(def p pprint)

;;; Demonstrate using Excel as a data store

;; ---------------- BASIC CALLING ---------------------------------------------


(def sample1 (es/clasew-excel-script "clasew-ex4-sample1.xlsx"
      es/create es/no-open "path to desktop"
      {:handler_list [] :arg_list []}))

; (p sample1)
; (p (es/clasew-excel-call! sample1))

;; Handler Chaining - Create, put values, get first row, save, quit

(def datum (into []
      (map vec
           (for [i (range 10)]
             (repeatedly 10 #(rand-int 100))))))

;(p datum)

(def sample2 (es/clasew-excel-script "clasew-ex4-sample2.xlsx"
      es/create es/no-open "path to desktop"
      (es/clasew-excel-handler [[:put-range-data "Sheet1" "A1:J10" datum]
                             [:get-range-data "Sheet1" "A1:A10"]
                             [:save-quit]])))

;(p sample2)
;(p (es/clasew-excel-call! sample2))

(def sample2a (es/clasew-excel-script "clasew-ex4-sample2.xlsx"
      es/no-create es/open "path to desktop"
      (es/clasew-excel-handler [[:get-range-formulas "Sheet1" "used"]
                                [:quit-no-save]])))


;(p sample2a)
;(p (es/clasew-excel-call! sample2a))
;; Demo - Open, Read, Quit

(def sample3 (es/clasew-excel-script "clasew-ex4-sample2.xlsx"
      es/no-create es/open "path to desktop"
      (es/clasew-excel-handler [[:all-book-info]
                             [:quit]])))

; (p sample3)
; (p (es/clasew-excel-call! sample3))

;; Demo pushing your own agenda

(def my_script
  "on run(caller, args)
    say \"I have control!\"
    return {my_script: \"No you don't\"}
  end run")

(def sample4 (es/clasew-excel-script "clasew-ex4-sample2.xlsx"
      es/no-create es/open "path to desktop"
      (es/clasew-excel-handler [[:run-script my_script []]
                                [:quit]])))

; (p (es/clasew-excel-call! sample4))

;; Demo HOF - Create, put values, get first row, save, quit

(def wrkbk-name "clasew-ex4-sample5.xlsx")
(def wrkbk-path "path to desktop")

(def sample5 (es/create-wkbk wrkbk-name wrkbk-path
                     (es/chain-put-range-data "Sheet1" datum)
                     [:save-quit]))


; (def s5r (es/clasew-excel-call! sample5))

; (p (es/chain-put-range-data "Sheet1" datum))

;; Demo misc forms


; Uniform collection
(def uniform [[1 2 3] [4 5 6] [7 8 9]])

; Jagged collection
(def jagged [[1] [2 2] [3 3 3]])

(es/get-excel-range-a1 [0 0 0 9])
(es/get-excel-range-a1 [0 1 1 9])

(es/formula-wrap "SUM" es/get-excel-range-a1 [0 0 0 9])
(es/formula-wrap "AVG" es/get-excel-range-a1 [0 0 0 9])

(es/fsum es/get-excel-range-a1 [0 0 0 9])
(es/favg es/get-excel-range-a1 [0 0 0 9])

(es/row-ranges uniform)
(es/row-ranges jagged)
(es/row-ranges (es/pad-rows jagged))

(es/column-ranges uniform)
(es/column-ranges jagged)
(es/column-ranges (es/pad-rows jagged))

(map es/get-excel-range-a1 (es/column-ranges uniform))
(map es/get-excel-range-a1 (es/row-ranges uniform 4 3))
(map es/get-excel-range-a1 (es/row-ranges jagged))

(es/sum-by-row uniform)
(es/sum-by-col uniform)

(es/avg-by-row uniform)
(es/avg-by-col uniform)

(es/extend-rows uniform 0 0 es/sum-by-row)
(es/extend-columns uniform 0 0 es/avg-by-col)

;; Put it together

(def sample5a (es/create-wkbk "clasew-ex4-sample5a.xlsx" wrkbk-path
      (es/chain-put-range-data "Sheet1"
                               (es/extend-columns
                                (es/extend-rows
                                 datum 0 0 es/sum-by-row es/avg-by-row)
                                0 0 es/avg-by-col))
                               [:save-quit]))

#_(p sample5a)
#_(p (es/clasew-excel-call! sample5a))

;; Demo - cleaning up results

;(p (es/clean-excel-result s6r))

;; Demo HOF - Open, get info, quit

(def sample6 (es/open-wkbk wrkbk-name wrkbk-path
                     [:all-book-info] [:quit]))

;(def s6r (es/clean-excel-result (es/clasew-excel-call! sample6)))

;(p s6r)

(def sample7 (es/create-wkbk wrkbk-name wrkbk-path
                           (es/chain-add-sheet
                            "Before Sheet1" :before "Sheet1"
                            "After Before Sheet1" :after "Before Sheet1"
                            "The End" :at :end
                            "The Beginning" :at :beginning
                            "Also Before Sheet1" :at 4)
                             [:save-quit]))


; (p sample7)

; (p (es/clean-excel-result (es/clasew-excel-call! sample7)))

(def sample8 (es/open-wkbk wrkbk-name wrkbk-path
                 (es/chain-delete-sheet "Sheet1")))

; (p sample8)

; (p (es/clean-excel-result (es/clasew-excel-call! sample8)))
