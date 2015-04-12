(ns
  ^{:author "Frank V. Castellucci"
      :doc "spike space"}
  clasew.sandbox
  (:require [clasew.core :as as]
            [clojure.pprint :refer :all])
  )

(def p pprint)

(def local-eng as/new-eng)


(comment

;; To call applescript function (event) with arguments as a list
;; (as/bind-eng-args! engine script :arguments (list [arg1 arg2 ... argx]))

(def saylist "on saylist(names)
  repeat with name in names
  say name
  end repeat
  return names
  end saylist")

;; Using the engines default bindings (good to reset first)
(p (as/run-ascript local-eng saylist
                   :reset-binding true
                   :bind-function "saylist"
                   :arguments (list ["Clasew" "is" "cool"])))

;; Full options usage

(def bnd-saylist (as/empty-binding! local-eng))

(p (as/run-ascript local-eng saylist
                   :reset-binding true
                   :bind-function "saylist"
                   :arguments (list ["Clasew" "is" "cool"])
                   :use-binding bnd-saylist))


;; With the user bindings set, just call it again
(p (as/run-ascript local-eng saylist
                   :use-binding bnd-saylist))

;; Change the arguments in our trodden on bindings and call again
(p (as/run-ascript local-eng saylist
                   :use-binding bnd-saylist
                   :arguments (list ["First arg" "Second arg" "Third arg"])))

;; To call applescript function (event) with fixed arguments
;; (as/bind-eng-args! engine script :arguments [arg1 arg2 ... argx])

(def bnd-sayargs (as/empty-binding! local-eng))

(def sayargs "on sayargs(name1, name2, name3)
  say name1
  say name2
  say name3
  return {arg1: name1, arg2: name2, arg3: name3} -- record/map
  end sayargs")

(p (as/run-ascript local-eng sayargs
      :reset-binding true
      :bind-function "sayargs"
      :arguments ["Clasew" "is" "cool"]
      :use-binding bnd-sayargs
      ))

;; To call applescript function (event) with a applescript record
;; (as/bind-eng-args! engine script :arguments {keystr1 val1, keystr2 val2, ...})

(def simplereturn "on simplereturn(arg)
  return class of arg
  end simplereturn")

(p (as/run-ascript local-eng simplereturn
      :reset-binding true
      :bind-function "simplereturn"
      :arguments {"clasew:" "is", "way:" "cool"}
      ))

;; Hello world

(def simpletest "tell application \"Finder\"
  try
  display dialog \"Hello Workd\"
  on error number -128
  {not_ok: \"Cancel\"}
  end try
  end tell")

(p (as/run-ascript local-eng simpletest :reset-binding true))


)
