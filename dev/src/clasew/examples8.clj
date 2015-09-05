(ns
  ^{:author "Frank V. Castellucci"
      :doc "Apple Contacts Examples"}
  clasew.examples8
  (:require [clojure.pprint :refer :all]
            [clasew.contacts :as contacts]
            [clasew.identities :as ident])
  )

;; Demonstrate record coercion
(def p pprint)

;; Fetch individuals from Contacs people

#_(p (ident/run-script!
    (contacts/script
     (ident/individuals))
    (ident/quit :contacts)))

;; Fetch all individuals and email addresses where
;; individual's first name contains "Frank"

#_(p (ident/run-script!
    (contacts/script
     (ident/individuals {:first_name "Frank"}
      (ident/email-addresses)))
    (ident/quit :contacts)))

;; Fetch all individuals and street addresses where
;; individual's first name contains "Frank"

#_(p (ident/run-script!
    (contacts/script
     (ident/individuals
      (ident/addresses)))
    (ident/quit :contacts)))

;; Fetch all individuals and phone numbers

#_(p (ident/run-script!
    (contacts/script
     (ident/individuals
      (ident/phones)))
    (ident/quit :contacts)))


;; Fetch all individuals (full name only) their streeet address,
;; phones and emails where individuals first name contains Frank

#_(p (ident/run-script!
      (contacts/script
       (ident/individuals :full_name
                          (ident/addresses)
                          (ident/email-addresses)
                          (ident/phones)
                          {:first_name "Frank"}))
      (ident/quit :contacts)))


;; Fetch all individuals their email addresses,street addresses and
;; phone numbers. This is equivalent to call (individuals-all...) as
;; shown below

#_(p (ident/run-script!
    (contacts/script
     (ident/individuals
      (ident/addresses)
      (ident/email-addresses)
      (ident/phones)))
    (ident/quit :contacts)))

#_(p (ident/run-script!
    (contacts/script (ident/individuals-all))
    (ident/quit :contacts)))
