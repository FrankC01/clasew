(ns
  ^{:author "Frank V. Castellucci"
      :doc "clasew example 2 - handlers and arguments"}
  clasew.examples2
  (:require [clasew.core :as as]
            [clojure.pprint :refer :all]
            [clojure.java.io :as io])
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

#_(p (as/run-ascript! local-eng argclass
      :reset-binding true
      :bind-function "argclass"
      :arguments 5
      ))

;; Call the script
;; Note 1: We are calling same hander, just change the :arguments
;; Note 2: Vectors are useful for handler  arguments (see examples3/sayargs)
;; Note 3: Only takes the first (a string) as the script only has one arg

#_(p (as/run-ascript! local-eng argclass
      :arguments ["clasew is" "very way" "cool"]
      ))

;; Coerce with AppleScript list
;; This is useful if the handler manages lists (see examples3/saylist)

#_(p (as/run-ascript! local-eng argclass
      :arguments (list ["clasew" "is" "way" "cool"])
      ))

;; maps get coerced to AppleScript record

#_(p (as/run-ascript! local-eng argclass
      :arguments {"project" "clasew", "way" [6], 7 9}
      ))

;; Demonstrate record coercion roundtrip

(def getback
  "on getback(arg)
    return arg
  end getback")

#_(p (as/run-ascript! local-eng getback
      :reset-binding true
      :bind-function "getback"
      :arguments {"project" "clasew", "way" [6]}
      ))

;; Demonstrate lists of lists roundtrip

#_(p (as/run-ascript! local-eng getback
      :reset-binding true
      :bind-function "getback"
      :arguments (list [[1 2] [3 4] [5 6]])
      ))

;; Demonstrate record manipulation.
;; Note 1: input maps require strings as keys but you can reference in script
;; using standard Applescript name (without quotes)
;; Note 2: keys can not be numbers! The script will error out.

(def test_rec
  "on test_rec(arg)
    set res to {}
    set end of res to project of arg
    set end of res to way of arg
    set end of res to lds of arg
    return res
  end test_rec")

(p (as/run-ascript! local-eng test_rec
      :reset-binding true
      :bind-function "test_rec"
      :arguments {"project" "clasew", "way" [6], "lds" "thing"}
      ))

;; Demonstrates slurping into string to pass to eval
;; Returns name of every item on desktop

#_(p (as/run-ascript! local-eng (slurp (io/resource "samp.applescript"))
                   :reset-binding true))

;; Demonstrates passing a buffered reader to an uncompiled text script
;; Also shows handlers and arguments being passed
;; Returns name of every file in argument path.
;; Note 1: Script assumes path from user home folder
;; Example arguments: "Documents" or "Library/Application Support"

#_(with-open [rdr (io/reader (io/resource "samp_listfile.applescript"))]
  (p (as/run-ascript! local-eng rdr
                   :reset-binding true
                   :bind-function "filename_list"
                   :arguments "Library/Application Support")))




