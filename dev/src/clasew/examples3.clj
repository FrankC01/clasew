(ns
  ^{:author "Frank V. Castellucci"
      :doc "clasew example 3 - individual handler bindings"}
  clasew.examples3
  (:require [clasew.core :as as]
            [clojure.pprint :refer :all])
  )

;;; Setup for the example

(def p pprint)

(defonce ^:private local-eng as/new-eng)   ; Use engine for this namespace

;; Setup a map of bindings

(def mybindings
  {:saylist (as/empty-binding! local-eng)
   :sayargs (as/empty-binding! local-eng)
   })


;; Demonstrate a simple handler call using a new binding instead of engines

;; saylist - demonstrates looping over a list
;; Note 1: Using a separate managed binding
;; Note 2: Requires a 'list' containing a vector

(def saylist
  "on saylist(names)
    repeat with name in names
      say name
    end repeat
    return names
  end saylist")

(p (as/run-ascript! local-eng saylist
                   :reset-binding true
                   :bind-function "saylist"
                   :use-binding (:saylist mybindings)
                   :arguments (list ["Clasew" "is" "cool"])))

;; With our bindings set (and stored), we can call it anytime as is

(p (as/run-ascript! local-eng saylist
                   :use-binding (:saylist mybindings)))

;; Or change the arguments in it and call again
(p (as/run-ascript! local-eng saylist
                   :use-binding (:saylist mybindings)
                   :arguments (list ["First arg" "Second arg" "Third arg"])))

;; sayargs - demonstrates setting one or more signature arguments
;; Note 1: Using a separate managed binding
;; Note 2: Requires a vector
;; Hint 1: remove the leading "--" to hear the middle argument

(def sayargs
  "on sayargs(name1, name2, name3)
    say name1
    -- say name2
    say name3
    return {arg1: name1, arg2: name2, arg3: name3} -- record/map
  end sayargs")

(p (as/run-ascript! local-eng sayargs
      :reset-binding true
      :bind-function "sayargs"
      :arguments ["Clasew" "is" "cool"]
      :use-binding (:sayargs mybindings)
      ))
