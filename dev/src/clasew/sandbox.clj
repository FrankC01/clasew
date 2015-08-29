(ns
  ^{:author "Frank V. Castellucci"
      :doc "spike space"}
  clasew.sandbox
  (:require [clojure.pprint :refer :all]
            [clasew.contacts :as cont]
            [clasew.outlook :as outl]
            [clasew.identities :as ident])
  )

;; Demonstrate record coercion
(def p pprint)


#_(p (ident/run-script!
      (outl/script
       (ident/individuals :full_name
                          (ident/addresses :zip_code)
                          {:first_name "Frank"}))
      (ident/quit :outlook)))

#_(p (ident/run-script!
      (cont/script
       (ident/individuals :full_name
                          (ident/addresses :zip_code)
                          {:first_name "Frank"}))
      (ident/quit :contacts)))

#_(p (ident/run-script!
    (cont/script
     (ident/individuals
      (ident/addresses)))
    (ident/quit :contacts)))


