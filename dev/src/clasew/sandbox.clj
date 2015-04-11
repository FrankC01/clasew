(ns
  ^{:author "Frank V. Castellucci"
      :doc "spike space"}
  clasew.sandbox
  (:require [clasew.core :as as]
            [clojure.pprint :refer :all])
  )

(def p pprint)

(def local-eng as/new-eng)

(p (as/eng-bnd local-eng))

(def saystrings "on saystrings(names)
  repeat with name in names
  say name
  end repeat
  return names
  end saystrings")

(as/bind-eng-function-name! local-eng "saystrings")
(as/bind-eng-args! local-eng "Casew" "is" "way cool")

(p (as/run-ascript local-eng saystrings))

(as/reset-eng-bnd! local-eng)




