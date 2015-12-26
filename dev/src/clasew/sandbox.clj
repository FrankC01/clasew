(ns
  ^{:author "Frank V. Castellucci"
      :doc "spike space"}
  clasew.sandbox
  (:require [clojure.pprint :refer :all]
            [clasew.messages :as mesg]
            [clasew.ast-utils :as astu]
            [clasew.outlook :as outlook]
            [clasew.mail :as mail]))

(def p pprint)

(comment

  (def platform {
    :name (System/getProperty "os.name"),
    :version (System/getProperty "os.version"),
    :arch (System/getProperty "os.arch")})

  (vals platform)
)


