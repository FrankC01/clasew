(ns
  ^{:author "Frank V. Castellucci"
      :doc "clasew example 5 - An Apple Numbers Example"}
  clasew.examples5
  (:require [clasew.spreads :as cs]
            [clasew.numbers :as ans]
            [clojure.pprint :refer :all])
  )

;;; Setup for the example

(def p pprint)

;;; Demonstrate using Excel as a data store

;; ---------------- BASIC CALLING ---------------------------------------------


(def sample1 (cs/clasew-script "clasew-ex5-sample1.numbers"
      cs/create cs/no-open "path to desktop"
      {:handler_list [] :arg_list []}))

;(p sample1)
;(p (ans/clasew-numbers-call! sample1))

;; Handler Chaining - Create, put values, get first row, save, quit

(def datum (into []
      (map vec
           (for [i (range 5)]
             (repeatedly 5 #(rand-int 100))))))

; (p datum)

(def sample2 (cs/clasew-script "clasew-ex5-sample2.numbers"
      cs/create cs/no-open "path to desktop"
      (cs/clasew-handler [[:put-range-data "Sheet 1" "A1:J10" datum]
                             [:get-range-data "Sheet 1" "A1:A10"]
                             [:save-quit]])))

;(p sample2)
;(p (ans/clasew-numbers-call! sample2))

(def sample2a (cs/clasew-script "clasew-ex5-sample2.numbers"
      cs/no-create cs/open "path to desktop"
      (cs/clasew-handler [[:get-range-formulas "Sheet 1" "used"]
                                [:quit-no-save]])))


;(p sample2a)
;(p (ans/clasew-numbers-call! sample2a))
;; Demo - Open, Read, Quit

(def sample3 (cs/clasew-script "clasew-ex5-sample2.numbers"
      cs/no-create cs/open "path to desktop"
      (cs/clasew-handler [[:all-book-info]
                             [:quit]])))

; (p sample3)
; (p (ans/clasew-numbers-call! sample3))

;; Demo pushing your own agenda

(def my_script
  "on run(caller, args)
    say \"I have control!\"
    return {my_script: \"No you don't\"}
  end run")

(def sample4 (cs/clasew-script "clasew-ex5-sample2.numbers"
      cs/no-create cs/open "path to desktop"
      (cs/clasew-handler [[:run-script my_script []]
                                [:quit]])))

; (p (ans/clasew-numbers-call! sample4))

;; Demo HOF - Create, put values, get first row, save, quit

(def wrkbk-name "clasew-ex5-sample5.numbers")
(def wrkbk-path "path to desktop")


(def sample5 (cs/create-wkbk wrkbk-name wrkbk-path
                     (cs/chain-put-range-data "Sheet 1" datum)
                     [:save-quit]))


(p sample5)

(p (ans/clasew-numbers-call! sample5))
; (def s5r (ans/clasew-numbers-call! sample5))

; (p (cs/chain-put-range-data "Sheet 1" datum))

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

#_(p sample5a)
#_(p (ans/clasew-numbers-call! sample5a))

;; Demo - cleaning up results

;(p (cs/clean-result s6r))

;; Demo HOF - Open, get info, quit

(def sample6 (cs/open-wkbk wrkbk-name wrkbk-path
                     [:all-book-info] [:quit]))

;(def s6r (cs/clean-result (ans/clasew-numbers-call! sample6)))

;(p s6r)

(def sample7 (cs/create-wkbk wrkbk-name wrkbk-path
                           (cs/chain-add-sheet
                            "After Sheet 1" :after "Sheet 1"
                            "Before After Sheet 1" :before "After Sheet 1"
                            "The End" :at :end
                            "The Beginning" :at :beginning
                            "Towards last" :at 4)
                             [:save-quit]))


; (p sample7)

; (p (cs/clean-result (ans/clasew-numbers-call! sample7)))

(def sample8 (cs/open-wkbk wrkbk-name wrkbk-path
                 (cs/chain-delete-sheet "Sheet 1")))


; (p (cs/clean-result (ans/clasew-numbers-call! sample8)))
