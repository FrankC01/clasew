(ns
  ^{:author "Frank V. Castellucci"
      :doc "Microsoft Office Examples"}
  clasew.examples7
  (:require [clojure.pprint :refer :all]
            [clasew.outlook :as outlook]
            [clasew.identities :as ident])
  )

;;

(def p pprint)


;; Setup a number of fictional contacts for demonstration

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
   :phones    (list {:number_type "home phone"
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

;; Create new individuals

#_(p (ident/run-script!
    (outlook/script
     (ident/add-individuals oxnard ))
    (ident/quit :outlook)))


;;
;; Fetch ALL individuals from outlook contacts
;;

#_(p (ident/run-script!
    (outlook/script
     (ident/individuals))
    (ident/quit :outlook)))

;; Fetch all individuals and email addresses where
;; individual's first name contains "Oxnard"
;; This uses the simple filter option

#_(p (ident/run-script!
    (outlook/script
     (ident/individuals
      (ident/filter :first_name ident/EQ "Oxnard")
      (ident/email-addresses)))
    (ident/quit :outlook)))

;; Fetch all individuals and phone numbers where
;; individual's first name contains "Sally"
;; This uses the simple filter option

#_(p (ident/run-script!
    (outlook/script
     (ident/individuals
      (ident/filter :first_name ident/EQ "Sally")
      (ident/phones)))
    (ident/quit :outlook)))

;; Fetch all individuals (full name only) their streeet address,
;; phones and emails where individuals first name contains Oxnard
;; This uses the simple filter option

#_(p (ident/run-script!
      (outlook/script
       (ident/individuals :full_name
                          (ident/addresses)
                          (ident/email-addresses)
                          (ident/phones)
                          (ident/filter :first_name ident/EQ "Oxnard")))
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

; ******* CONTINUE WITH DEPRECATE REPLACEMENTS*******

#_(println (outlook/script
     (ident/individuals-all)))

;; Using a complex filter

(def filter-sample (ident/filter
            :first_name ident/CT "Oxnard"
            (ident/or :first_name ident/EQ "Sally"
                      :last_name ident/EQ "Abercrombe")
            ))

;; Fetch individuals based on complex filter


#_(p   (ident/update-individual
    (ident/filter :first_name ident/CT "Oxnard")
    :last_name "Skippy"
           :home_phone "0"
   (ident/update-email-addresses
    (ident/adds
     {:email_type "work" :email_address "oxnard1@mybusiness.com"}
     {:email_type "home" :email_address "oxnard2@myhome.com"}))
    (ident/update-phones
     (ident/filter :number_type ident/EQ "mobile" )
     :number_value "991 991 9991"
     (ident/adds
      {:number_value "999 999 9999" :number_type "work"}
      {:number_value "999 999 9999" :number_type "home"}))))

#_(println (outlook/script
  (ident/update-individual
    (ident/filter :first_name ident/CT "Oxnard")
    :last_name "Skippy"
   (ident/update-email-addresses
    (ident/adds
     {:email_type "work" :email_address "oxnard1@mybusiness.com"}
     {:email_type "home" :email_address "oxnard2@myhome.com"}))
    (ident/update-phones
     (ident/filter  :number_value ident/EQ "000 000 0000")
     :number_value "991 991 9991"
     (ident/adds
      {:number_value "999 999 9999" :number_type "work"}
      {:number_value "999 999 9999" :number_type "home"})))))


#_(p (ident/run-script!    (outlook/script
  (ident/update-individual
    (ident/filter :first_name ident/CT "Oxnard")
    :last_name "Skippy"
   (ident/update-email-addresses
    (ident/adds
     {:email_type "work" :email_address "oxnard1@mybusiness.com"}
     {:email_type "home" :email_address "oxnard2@myhome.com"}))
    (ident/update-phones
     (ident/adds
      {:number_value "999 999 9999" :number_type "work"}
      {:number_value "999 999 9999" :number_type "home"}))))))

;(p (clasew.gen-as/ginstrument nil))
#_(p (ident/run-script!
    (outlook/script
     (ident/individuals filter-sample))
    (ident/quit :outlook)))

;; Delete individuals based on complex filter

#_(p (ident/run-script!
      (outlook/script
       (ident/delete-individual filter-sample))
      (ident/quit :outlook)))



