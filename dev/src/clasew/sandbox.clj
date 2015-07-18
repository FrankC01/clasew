(ns
  ^{:author "Frank V. Castellucci"
      :doc "spike space"}
  clasew.sandbox
  (:require [clojure.pprint :refer :all]
            [clasew.identities :as ident]
            [clasew.contacts :as cont])
  )

;; Demonstrate record coercion
(def p pprint)



(println (cont/get-individuals (cont/get-address)))
