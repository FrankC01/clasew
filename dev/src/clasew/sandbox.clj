(ns
  ^{:author "Frank V. Castellucci"
      :doc "spike space"}
  clasew.sandbox
  (:require [clojure.pprint :refer :all]
            [clojure.core.reducers :as r]
            [clasew.messages :as mesg]
            [clasew.identities :as ident]
            [clasew.ast-utils :as astu]))

;; Demonstrate record coercion
(def p pprint)



(def _t0 (ident/update-individual
      (astu/filter
       :first_name astu/EQ "Oxnard"
       :last_name astu/EQ "Gimbel")
      :first_name "Oxnardio"
      :last_name "Pocinos"
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
       :number_value "991 991 9991"
       (ident/adds
        {:number_type "work fax" :number_value "111 111-1111"}
        {:number_type "pager" :number_value "222 222-222"}))
          (ident/update-phones
           (ident/adds {:number_type "home" :number_value "333 333-3333"}))))

;(p _t0)

(println (clasew.outlook/script _t0))

