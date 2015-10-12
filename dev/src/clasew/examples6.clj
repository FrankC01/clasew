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

;;
;; Setup a number of fictional contacts for demonstration
;; Also demonstrates add-individuals
;;

(def oxnard
  {:first_name "Oxnard" :last_name "Gimbel"
   :emails (list {:email_type "work"
                  :email_address "oxnard@mybusiness.com"}
                 {:email_type "home"
                  :email_address "oxnard@myhome.com"})
   :addresses (list {:address_type "work"
                     :city_name "West Somewhere"}
                    {:address_type "home"
                     :city_name "New York"})
   :phones    (list {:number_type "home"
                     :number_value "999 999-0000"}
                    {:number_type "mobile"
                     :number_value "999 999 0001"})
   })

(def sally
  {:first_name "Sally" :last_name "Abercrombe"
   :emails (list {:email_type "work"
                  :email_address "sally@clasew.com"}
                 {:email_type "home"
                  :email_address "sally@gmail.com"})
   :addresses (list {:address_type "work"
                     :city_name "East Somewhere"}
                    {:address_type "home"
                     :city_name "Maine"})
   :phones    (list {:number_type "home"
                     :number_value "999 999-0002"}
                    {:number_type "mobile"
                     :number_value "999 999 0003"})
   })

;; Create a new individual

#_(p (ident/run-script!
    (contacts/script
     (ident/add-individuals oxnard sally))
    (ident/quit :contacts)))

;; Fetch individuals from Contacs people

#_(p (ident/run-script!
    (contacts/script
     (ident/individuals))
    (ident/quit :contacts)))

;; Fetch all individuals and email addresses where
;; individual's first name contains "Oxnard"

#_(p (ident/run-script!
    (contacts/script
     (ident/individuals {:first_name "Oxnard"}
      (ident/email-addresses)))
    (ident/quit :contacts)))

;; Fetch all individuals and phone numbers where
;; individual's first name contains "Sally"

#_(p (ident/run-script!
    (contacts/script
     (ident/individuals {:first_name "Sally"}
      (ident/phones)))
    (ident/quit :contacts)))

;; Fetch all individuals (full name only) their streeet address,
;; phones and emails where individuals first name contains Oxnard

#_(p (ident/run-script!
      (contacts/script
       (ident/individuals :full_name
                          (ident/addresses)
                          (ident/email-addresses)
                          (ident/phones)
                          {:first_name "Oxnard"}))
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

#_(p (ident/run-script!
      (contacts/script
       (ident/delete-individual {:first_name "Oxnard"}))
      (ident/quit :contacts)))

#_(p (ident/run-script!
      (contacts/script
       (ident/delete-individual {:first_name "Sally"
                                 :last_name "Abercrombe"}))
      (ident/quit :contacts)))

