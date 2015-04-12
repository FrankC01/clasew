(ns
  ^{:author "Frank V. Castellucci"
      :doc "clasew example 2 - handlers and arguments"}
  clasew.examples2
  (:require [clasew.core :as as]
            [clojure.pprint :refer :all])
  )

;;; Setup for the example

(def p pprint)

(defonce ^:private local-eng as/new-eng)   ; Use engine for this namespace

;; Demonstrate a simple handler call to get argument types
;; A handler, in AppleScript, is synonoymous to function, sub-routine, etc.
;; richer examples of handlers and arguments can be found in examples3.clj

;; argclass - a handler that retreives the class of it's argument
;; Hack 1: Don't know how to pass a true boolean in so test string value

(def argclass
  "on argclass(arg)
    if arg is in {\"true\", \"false\"}
      set res to {type: \"boolean\"}
    else
      set arg_class to class of arg
      if the arg_class is integer then
        set res to {type: \"integer\"}
      else if the arg_class is real then
        set res to {type: \"real\"}
      else if the arg_class is text then
        set res to {type: \"text\"}
      else if the arg_class is list then
        set res to {type: \"list\"}
      else if the arg_class is record then
        set res to {type: \"record\"}
      else
        set res to {type: arg_class}
      end if
    end if
  return res
  end argclass")


;; calls the script
;; Note 1: Doesn't assume the engines binding is clean (:reset-binding)
;; Note 2: Identify the handler inside the script being called (:bind-function)
;; Note 3: Pass arguments (:arguments) - in this case just a number

(p (as/run-ascript local-eng argclass
      :reset-binding true
      :bind-function "argclass"
      :arguments 5
      ))

;; Call the script
;; Note 1: We are calling same hander, just change the :arguments
;; Note 2: Vectors are useful for handler  arguments (see examples3/sayargs)
;; Note 3: Only takes the first (a string) as the script only has one arg

(p (as/run-ascript local-eng argclass
      :arguments ["clasew is" "very way" "cool"]
      ))

;; Coerce with AppleScript list
;; This is useful if the handler manages lists (see examples3/saylist)

(p (as/run-ascript local-eng argclass
      :arguments (list ["clasew" "is" "way" "cool"])
      ))

;; maps get coerced to AppleScript record

(p (as/run-ascript local-eng argclass
      :arguments {"project" "clasew", "way" [6], 7 9}
      ))

;; Demonstrate record coercion

(def getback
  "on getback(arg)
    return arg
  end getback")

(p (as/run-ascript local-eng getback
      :reset-binding true
      :bind-function "getback"
      :arguments {"project" "clasew", "way" [6], 7 9}
      ))

