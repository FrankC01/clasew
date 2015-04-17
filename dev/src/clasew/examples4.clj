(ns
  ^{:author "Frank V. Castellucci"
      :doc "clasew example 4 - An Excel Example"}
  clasew.examples4
  (:require [clasew.core :as as]
            [clojure.pprint :refer :all])
  )

;;; Setup for the example

(def p pprint)

(defonce ^:private local-eng as/new-eng)   ; Use engine for this namespace

;; Demonstrate using Excel as a data store

;; create-wkbk - demonstrates creating new workbook
;; Note 0: Used on Yosemite with Excel 2011 for Mac
;; Note 1: Puts file on desktop
;; Note 2: Will overwite existing with new workbook
;; Note 3: Excel and workbook are still active after call

(def create-wkbk
  "set theOutputPath to (path to desktop folder as text) & \"clasew-ex4.xlsx\"
  tell application \"Microsoft Excel\"
    launch
    make new workbook
    tell active workbook
      save workbook as filename theOutputPath overwrite yes
    end tell
  end tell")

;; Step 1

(p (as/run-ascript local-eng create-wkbk
                   :reset-binding true))

;; put_values - demonstrates populating the current sheet of workbook with data
;; Note 1: Assumes that our example workbook is the active one
;; Note 2: A 'list of lists' is expected as argument (square matrix)
;; Note 3: Each list reflect a row
;; Note 4: Starting row, in Excel, is 1

(def put_values
  "on put_values(arg)
    tell application \"Microsoft Excel\"
      tell worksheet \"Sheet1\" of active workbook
        repeat with i from 1 to count of arg
          set aRow to item i of arg
          set lastCol to count of items of aRow
          set firstCell to get address of cell 1 of row i
          set lastCell to get address of cell lastCol of row i
          set myrange to range (firstCell & \":\" & lastCell)
          set value of myrange to aRow
        end repeat
      end tell
    end tell
  end put_values")

;; Contrived data blob

(def datum (list (into []
      (map vec
           (for [i (range 10)]
             (repeatedly 10 #(rand-int 100)))))))

;; Step 2

(p (as/run-ascript local-eng put_values
      :reset-binding true
      :bind-function "put_values"
      :arguments datum
      ))

;; put_values - demonstrates saving the workbook changes and quit Excel
;; Note 1: Assumes that our example workbook is the active one

(def close-wrkbk
  "tell application \"Microsoft Excel\"
    save active workbook
    quit
  end tell")

;; Step 3

(p (as/run-ascript local-eng close-wrkbk
                   :reset-binding true))
