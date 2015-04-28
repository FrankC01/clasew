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

;; ----------------- CREATE - POPULATE - SAVE - CLOSE -------------------------

;; create-wkbk - demonstrates creating new workbook
;; Note 0: Used on Yosemite with Excel 2011 for Mac
;; Note 1: Puts file on desktop
;; Note 2: Will overwite existing with new workbook
;; Note 3: Excel and workbook are still active after call

;; Contrived data blob

(def datum (into []
      (map vec
           (for [i (range 10)]
             (repeatedly 10 #(rand-int 100))))))

(def t0 (es/create-wkbk wrkbk-name wrkbk-path
                     (es/chain-put-range-data "Sheet1" datum)
                     [:save-quit]))

(p t0)
(def t0r (es/clean-excel-result (es/casew-excel-call! t0)))
(p t0r)

;; ----------------- OPEN - READ - WRITE - SAVE - CLOSE -----------------------



