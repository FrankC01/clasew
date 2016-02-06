(ns
  ^{:author "Frank V. Castellucci"
      :doc "spike space"}
  clasew.sandbox
  (:require [clojure.pprint :refer :all]
            [clasew.messages :as mesg]
            [clasew.mesg-utils :as mesgu]
            [clasew.ast-emit :as ast]
            [clasew.ast-utils :as astu]
            [clasew.outlook-m :as outm]
            [clasew.gen-as :as gold]
            [clasew.gen-asmm :as gen]))

(def p pprint)


(comment

  (def platform {
    :name (System/getProperty "os.name"),
    :version (System/getProperty "os.version"),
    :arch (System/getProperty "os.arch")})

  (vals platform)
)


