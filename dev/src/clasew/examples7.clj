(ns
  ^{:author "Frank V. Castellucci"
      :doc "Identities Examples (Multi-Application)"}
  clasew.examples7
  (:require [clojure.pprint :refer :all]
            [clasew.outlook :as outlook]
            [clasew.contacts :as contacts]
            [clasew.identities :as ident]
            [clasew.ast-utils :as astu]))

(def p pprint)

;;
;; Sample ease of use forms
;;

(def ident-apps
  {:contacts #(contacts/script %)
   :outlook  #(outlook/script %)})

(def ident-app-quit
  {:contacts (astu/quit :contacts)
   :outlook  (astu/quit :outlook)})

(defn- map-requests
  "Performs substitution or passthrough of script function"
  [app coll]
  (map #(condp = %
          :quit (app ident-app-quit)
          ((app ident-apps) %))
       coll))

(defn run-sample
  "Calls the target application script generator to create
  script as defined by request and then executes the results:
  app - Defines target app. Can be :mail or :outlook
  & rqs - script requests (supports substitution)"
  [app & rqs]
  (apply ident/run-script! (map-requests app rqs)))

(defn print-sample
  "Calls the target application script generator to create
  script as defined by request and then prints the results:
  app - Defines target app. Can be :mail or :outlook
  & rqs - script requests (supports substitution)"
  [app & rqs]
  (apply println (map-requests app rqs)))

;;
;; Setup a number of fictional contacts for demonstration
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
   :phones    (list {:number_type "home phone"
                     :number_value "999 999-0000"}
                    {:number_type "mobile"
                     :number_value "999 999 0001"}
                    {:number_type "work"
                     :number_value "000 000 0000"})
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

;; Uncomment to run
;(print-sample :contacts (ident/add-individuals oxnard sally))
;(print-sample :outlook (ident/add-individuals oxnard sally))

;(p (run-sample :contacts (ident/add-individuals oxnard sally)))
;(p (run-sample :outlook (ident/add-individuals oxnard sally)))

;;
;; Fetch ALL individuals from outlook contacts
;;

;; Uncomment to run
;(p (run-sample :contacts (ident/individuals)))
;(p (run-sample :outlook (ident/individuals)))

;; Fetch all individuals and email addresses where
;; individual's first name contains "Oxnard"
;; This uses the simple filter option

(def s-get-individuals-with-name-filter
  (ident/individuals
    (astu/filter :first_name astu/CT "Oxnard")
    (ident/email-addresses)))

;; Uncomment to run
;(p (run-sample :contacts s-get-individuals-with-name-filter))
;(p (run-sample :outlook s-get-individuals-with-name-filter))

;; Fetch all individuals and phone numbers where
;; individual's first name contains "Sally"
;; This uses a simple filter option

(def s-get-individuals-and-phones-with-name-filter
  (ident/individuals
    (astu/filter :first_name astu/EQ "Sally")
    (ident/phones)))

;; Uncomment to run
;(p (run-sample :contacts s-get-individuals-and-phones-with-name-filter))
;(p (run-sample :outlook s-get-individuals-and-phones-with-name-filter))

;; Fetch all individuals (full name only) their streeet address,
;; phones and emails where individuals first name contains Oxnard
;; This uses a simple filter option

(def s-get-fullname-and-info-with-name-filter
  (ident/individuals :full_name
                          (ident/addresses)
                          (ident/email-addresses)
                          (ident/phones)
                          (astu/filter :first_name astu/EQ "Oxnard")))

;; Uncomment to run
;(p (run-sample :contacts s-get-fullname-and-info-with-name-filter))
;(p (run-sample :outlook s-get-fullname-and-info-with-name-filter))


;; Fetch all individuals their email addresses,street addresses and
;; phone numbers. This is equivalent to call (individuals-all...) as
;; shown below

(def s-get-all-individuals-ala-carte
  (ident/individuals
      (ident/addresses)
      (ident/email-addresses)
      (ident/phones)))

;; Uncomment to run
;(p (run-sample :contacts s-get-all-individuals-ala-carte))
;(p (run-sample :outlook s-get-all-individuals-ala-carte))

;; Short version to do the same

(def s-get-all-individuals-fixed-price (ident/individuals-all))

;; Uncomment to run
;(p (run-sample :contacts s-get-all-individuals-fixed-price))
;(p (run-sample :outlook s-get-all-individuals-fixed-price))

;; Updates with filters

(def s-update-individuals-meeting-criteria
  (ident/update-individual
      (astu/filter
       :first_name astu/EQ "Oxnard"
       :last_name astu/EQ "Gimbel")
      :first_name "Oxnardio"
      (ident/update-addresses
       (astu/filter
        :address_type astu/EQ  "work"
        :city_name astu/EQ "West Somewhere")
       :city_name "West Palm Beach")
      (ident/update-addresses
       (astu/filter
        :address_type astu/EQ  "home"
        :city_name astu/EQ "New York")
       :city_name "NJ")
      (ident/update-email-addresses
       (astu/filter
        :email_type astu/EQ "home"
        :email_address astu/EQ "oxnard@myhome.com")
       :email_address "oxnard@my_new_home.com")
      (ident/update-email-addresses
       (astu/filter
        :email_type astu/EQ "work"
        :email_address astu/EQ "oxnard@mybusiness.com")
       :email_address "oxnard@my_old_business.com"
       (ident/adds
        {:email_type "work" :email_address "oxnard1@mybusiness.com"}))
      (ident/update-phones
       (astu/filter
        :number_type astu/EQ "work"
        :number_value astu/EQ "000 000 0000")
       :number_value "991 991 9991")))

;; Uncomment to run
;(p (run-sample :contacts s-update-individuals-meeting-criteria))
;(p (run-sample :outlook s-update-individuals-meeting-criteria))

;;
;; Construct a complex filter
;;

(def s-reusable-nested-filter
  (astu/filter
    :first_name astu/CT "Oxnard"
    :last_name astu/EQ "Gimbel"
    (astu/or :first_name astu/EQ "Sally"
             :last_name astu/EQ "Abercrombe")))

;; Fetch individuals based on complex filter

;; Uncomment to run
;(p (run-sample :contacts (ident/individuals s-reusable-nested-filter)))
;(p (run-sample :outlook (ident/individuals s-reusable-nested-filter)))

;; Delete individuals based on complex filter

;; Uncomment to run
;(p (run-sample :contacts (ident/delete-individual s-reusable-nested-filter) :quit))
;(p (run-sample :outlook (ident/delete-individual s-reusable-nested-filter) :quit))




