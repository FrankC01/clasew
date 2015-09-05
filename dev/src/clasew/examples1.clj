(ns
  ^{:author "Frank V. Castellucci"
      :doc "clasew example 1 - basic"}
  clasew.examples1
  (:require [clasew.core :as as]
            [clojure.pprint :refer :all])
  )

;;; Setup for the example

(def p pprint)

(defonce ^:private local-eng as/new-eng)   ; Use engine for this namespace

;; Gratuitous Hello World
;; hello-world - demonstrates fundemental calling conventions
;; Note 1: Dialog signals 'on error...': ("user canceled" error) for Cancel
;; Note 2: Anything in quotes, within the script, must be escaped
;; Note 3: If your editor doesn't multi-line newlines, you must add

(def hello-world
  "tell application \"Finder\"
    try
      display dialog \"Hello World\"
    on error number -128
    {not_ok: \"Cancel\"}
    end try
  end tell")

;; Call the script using most basic form of clasew script execution.
;; run-ascript always returns a map containing, at least, :result of the call
;; Assumes:
;;   1. Use of the engine's default binding

;(p (as/run-ascript! local-eng hello-world))

;; returns {:result {1651009908 "OK"}} or {:result {"not_ok" "Cancel"}}

;; Call the script using most basic form but ensure that the bindings are clean
;; This is typical when calling a script wihtout parameters or handler target
;; Also useful when you have preceding calls from other scripts
;; run-ascript, in addition to :result, will contain any calling options

;(p (as/run-ascript! local-eng hello-world :reset-binding true))

;; returns {:reset-binding true, :result {1651009908 "OK"}}
