(ns
  ^{:author "Frank V. Castellucci"
      :doc "Microsoft Office Examples"}
  clasew.examples7
  (:require [clojure.pprint :refer :all]
            [clasew.outlook :as outlook]
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
   })

(def picard
  {:first_name "Jean-Luc" :last_name "Picard"})


#_(p (ident/run-script!
    (outlook/script
     (ident/add-individuals oxnard picard))
    (ident/quit :outlook)))


;; Fetch individuals from outlook contacts

#_(p (ident/run-script!
    (outlook/script
     (ident/individuals))
    (ident/quit :outlook)))


;; Fetch all individuals and email addresses where
;; individual's first name contains "Frank"

#_(p (ident/run-script!
    (outlook/script
     (ident/individuals {:first_name "Oxnard"}
      (ident/email-addresses)))
    (ident/quit :outlook)))


;; Fetch all individuals and phone numbers where
;; individual's first name contains "Frank"

#_(p (ident/run-script!
    (outlook/script
     (ident/individuals {:first_name "Frank"}
      (ident/phones)))
    (ident/quit :outlook)))

;; Fetch all individuals (full name only) their streeet address,
;;phones and emails where individuals first name contains Frank

#_(p (ident/run-script!
      (outlook/script
       (ident/individuals :full_name
                          (ident/addresses)
                          (ident/email-addresses)
                          (ident/phones)
                          {:first_name "Frank"}))
      (ident/quit :outlook)))


;; Fetch all individuals their email addresses,street addresses and
;; phone numbers. This is equivalent to call (individuals-all...) as
;; shown below

#_(p (ident/run-script!
    (outlook/script
     (ident/individuals
      (ident/addresses)
      (ident/email-addresses)
      (ident/phones)))
    (ident/quit :outlook)))

#_(p (ident/run-script!
    (outlook/script
     (ident/individuals-all))
    (ident/quit :outlook)))


#_(p (ident/run-script!
      (outlook/script
       (ident/delete-individual {:first_name "Oxnard"}))
      (ident/quit :outlook)))

#_(p (ident/run-script!
      (outlook/script
       (ident/delete-individual {:last_name "Picard"}))
      (ident/quit :outlook)))


