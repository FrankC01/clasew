(ns
  ^{:author "Frank V. Castellucci"
      :doc "Apple Contacts Examples"}
  clasew.examples6
  (:require [clojure.pprint :refer :all]
            [clasew.contacts :as contacts]
            [clasew.identities :as ident])
  )

;; Demonstrate record coercion
(def p pprint)

;;; Apple Contacts

#_(p (ident/run-script!
    (cont/script
     (ident/individuals
      (ident/addresses)))
    (ident/quit :contacts)))

#_(p (ident/run-script!
      (cont/script
       (ident/individuals :full_name
                          (ident/addresses :zip_code)
                          {:first_name "Frank"}))
      (ident/quit :contacts)))


