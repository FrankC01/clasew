(ns
  ^{:author "Frank V. Castellucci"
      :doc "spike space"}
  clasew.sandbox
  (:require [clasew.core :as as]
            [clojure.pprint :refer :all]
            [clojure.string :as s])
  )

;; Demonstrate record coercion
(def p pprint)

(defonce ^:private local-eng as/new-eng)   ; Use engine for this namespace
