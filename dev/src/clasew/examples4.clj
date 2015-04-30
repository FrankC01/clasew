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

;; ---------------- RAW CALLING -----------------------------------------------

;; Demo basic calling

(def sample1 (es/clasew-excel-script "clasew-sample.xlsx"
      es/create es/no-open "path to desktop"
      {:handler_list [] :arg_list []}))

(p sample1)
;(p (es/clasew-excel-call! sample1))

;; Demo Handler Chaining - Create, put values, get first row, save, quit

(def datum (into []
      (map vec
           (for [i (range 10)]
             (repeatedly 10 #(rand-int 100))))))

(p datum)

(def sample2 (es/clasew-excel-script "clasew-sample.xlsx"
      es/create es/no-open "path to desktop"
      (es/clasew-excel-handler [[:put-range-values "Sheet1" "A1:J10" datum]
                             [:get-range-values "Sheet1" "A1:A10"]
                             [:save-quit]])))

(p sample2)
; (p (es/clasew-excel-call! sample2))

;; Demo - Open, Read, Quit

(def sample3 (es/clasew-excel-script "clasew-sample.xlsx"
      es/no-create es/open "path to desktop"
      (es/clasew-excel-handler [[:all-book-info]
                             [:quit]])))

(p sample3)
; (p (es/clasew-excel-call! sample3))

;; Demo pushing your own agenda

(def my_script
  "on run(caller, args)
    say \"I have control!\"
    return {my_script: \"No you don't\"}
  end run")

(def sample4 (es/clasew-excel-script "clasew-sample.xlsx"
      es/no-create es/open "path to desktop"
      (es/clasew-excel-handler [[:run-script my_script []]
                                [:quit]])))

; (p (es/clasew-excel-call! sample4))

;; Demo HOF - Create, put values, get first row, save, quit

(def wrkbk-name "clasew-ex4.xlsx")
(def wrkbk-path "path to desktop")

(def sample5 (es/create-wkbk wrkbk-name wrkbk-path
                     (es/chain-put-range-data "Sheet1" datum)
                     [:save-quit]))


;(def s5r (es/clasew-excel-call! sample5))

;(p (es/clean-excel-result s5r))

;; Demo - cleaning up results

(p (es/clean-excel-result s6r))

;; Demo HOF - Open, get info, quit

(def sample6 (es/open-wkbk wrkbk-name wrkbk-path
                     [:all-book-info] [:quit]))

;(def s6r (es/clean-excel-result (es/clasew-excel-call! sample6)))

;(p s6r)
