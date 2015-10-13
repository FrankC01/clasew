(ns
  ^{:author "Frank V. Castellucci"
      :doc "spike space"}
  clasew.sandbox
  (:require [clojure.pprint :refer :all]
            [clasew.messages :as mesg]
            [clasew.ast-emit :as ast]
            [clasew.ast-utils :as astu]))

;; Demonstrate record coercion
(def p pprint)

(p (mesg/messages nil))


