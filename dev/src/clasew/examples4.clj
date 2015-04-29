(ns
  ^{:author "Frank V. Castellucci"
      :doc "clasew example 4 - An Excel Example"}
  clasew.examples4
  (:require [clasew.excel :as es]
            [clojure.pprint :refer :all])
  )

;;; Setup for the example

(def p pprint)

(def wrkbk-name "clasew-ex4.xlsx")
(def wrkbk-path "path to desktop")

;;; Demonstrate using Excel as a data store

;; ---------------- RAW CALLING -----------------------------------------------

;; Demo basic calling

(def sample1 (es/clasew-excel-script "clasew-sample.xlsx"
      es/create es/no-open "path to desktop"
      {:handler_list [] :arg_list []}))

(p sample1)
;(p (es/clasew-excel-call! sample1))

;; Demo chaining handlers

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

;; Demo pushing your own agenda

(def my_script
  "on run(caller, args)
    say \"I have control!\"
    return {my_script: \"No you don't\"}
  end run")

(def sample3 (es/clasew-excel-script "clasew-sample.xlsx"
      es/no-create es/open "path to desktop"
      (es/clasew-excel-handler [[:run-script my_script []]
                                [:quit]])))

; (p (es/clasew-excel-call! sample3))

;; ----------------- CREATE - POPULATE - SAVE - CLOSE -------------------------

;; create-wkbk - demonstrates creating new workbook
;; Note 0: Used on Yosemite with Excel 2011 for Mac
;; Note 1: Puts file on desktop
;; Note 2: Will overwite existing with new workbook
;; Note 3: Excel and workbook are still active after call

(def sampleX (es/create-wkbk wrkbk-name wrkbk-path
                     (es/chain-put-range-data "Sheet1" datum)
                     [:save-quit]))


;(p sampleX)
;(def t0r (es/clean-excel-result (es/clasew-excel-call! sampleX)))
;(p t0r)

;; ----------------- OPEN - READ - WRITE - SAVE - CLOSE -----------------------



