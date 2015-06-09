(ns
  ^{:author "Frank V. Castellucci"
      :doc "clasew example 5 - Apple Numbers Examples"}
  clasew.examples5
  (:require [clasew.spreads :as cs]
            [clasew.numbers :as an]
            [clojure.pprint :refer :all])
  )

;;; Setup for the example

(def p pprint)

;;; Demonstrate using Excel as a data store

;; ---------------- BASIC CALLING ---------------------------------------------


(def sample1 (cs/clasew-script "clasew-ex5-sample1.numbers"
      cs/create cs/no-open "path to desktop" nil
      {:handler_list ["clasew_save_and_quit"] :arg_list [[]]}))

;(p sample1)
;(p (an/clasew-numbers-call! sample1))

(def sample1a (cs/clasew-script
               "clasew-ex5-sample1a.numbers"
               cs/create cs/no-open "path to desktop"
               (cs/create-parms
                :sheet_name "FY15"
                :table_list (cs/tables
                             (cs/table :table_name "Q1 - Sales & Returns",
                                    :column_count 8,
                                    :row_count 10,
                                    :header_content ["Region","Sales", "Returns"])))
               {:handler_list ["clasew_get_book_info","clasew_quit"] :arg_list [[],[]]}))

;(p sample1a)
;(p (an/clasew-numbers-call! sample1a))

;; Handler Chaining - Create, put values, get first row, save, quit

(def datum (into []
      (map vec
           (for [i (range 5)]
             (repeatedly 5 #(rand-int 100))))))

; (p datum)

(def sample2 (cs/clasew-script "clasew-ex5-sample2.numbers"
      cs/create cs/no-open "path to desktop" nil
      (cs/clasew-handler [[:put-range-data "Sheet 1" "A1:E5" datum]
                             [:get-range-data "Sheet 1" "A1:A5"]
                             [:save-quit]])))

;(p sample2)
;(p (an/clasew-numbers-call! sample2))

(def sample2a (cs/clasew-script "clasew-ex5-sample2.numbers"
      cs/no-create cs/open "path to desktop" nil
      (cs/clasew-handler [[:get-range-formulas "Sheet 1" "used"]
                                [:quit-no-save]])))


;(p sample2a)
;(p (an/clasew-numbers-call! sample2a))
;; Demo - Open, Read, Quit

(def sample3 (cs/clasew-script "clasew-ex5-sample2.numbers"
      cs/no-create cs/open "path to desktop" nil
      (cs/clasew-handler [[:book-info]
                             [:quit]])))

; (p sample3)
; (p (an/clasew-numbers-call! sample3))

;; Demo pushing your own agenda

(def my_script
  "on run(caller, args)
    say \"I have control!\"
    return {my_script: \"No you don't\"}
  end run")

(def sample4 (cs/clasew-script "clasew-ex5-sample2.numbers"
      cs/no-create cs/open "path to desktop" nil
      (cs/clasew-handler [[:run-script my_script []]
                                [:quit]])))

; (p (an/clasew-numbers-call! sample4))

;; Demo HOF - Create, put values, get first row, save, quit

(def wrkbk-name "clasew-ex5-sample5.numbers")
(def wrkbk-path "path to desktop")


(def sample5 (cs/create-wkbk wrkbk-name wrkbk-path
                     (cs/chain-put-range-data "Sheet 1" datum)
                     [:save-quit]))


; (p sample5)
; (p (an/clasew-numbers-call! sample5))
; (def s5r (an/clasew-numbers-call! sample5))

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

(def sample5a (cs/create-wkbk "clasew-ex5-sample5a.numbers" wrkbk-path
      (cs/chain-put-range-data "Sheet 1"
                               (cs/extend-columns
                                (cs/extend-rows
                                 datum 0 0 cs/sum-by-row cs/avg-by-row)
                                0 0 cs/avg-by-col))
                               [:save-quit]))

;(p sample5a)
;(p (an/clasew-numbers-call! sample5a))

;; Demo - cleaning up results

(def sample6 (cs/open-wkbk wrkbk-name wrkbk-path
                     [:all-book-info] [:quit]))

;(def s6r (cs/clean-result (an/clasew-numbers-call! sample6)))

;(p s6r)

(def sample7 (cs/create-wkbk "clasew-ex5-sample7.numbers" wrkbk-path
                           (cs/chain-add-sheet
                            "First Add" :after "Sheet 1"
                            "Before First Add" :before "First Add"
                            "The End" :at :end (cs/tables
                             (cs/table :table_name "First Table",
                                    :row_count 5,
                                    :column_count 5))
                            "The Beginning" :at :beginning
                            "Towards last" :at 5)
                             [:save-quit]))


; (p sample7)

; (p (cs/clean-result (an/clasew-numbers-call! sample7)))

(def sample8 (cs/open-wkbk "clasew-ex5-sample7.numbers" wrkbk-path
                 (cs/chain-delete-sheet "Sheet 1")))


; (p (cs/clean-result (an/clasew-numbers-call! sample8)))
