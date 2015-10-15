(ns
  ^{:author "Frank V. Castellucci"
    :doc "Clojure AppleScriptEngine Wrapper - common contacts DSL"}
  clasew.identities
  (:require   [clasew.core :as as]
              [clasew.utility :as util]
              [clasew.ast-utils :as astu]
              [clojure.java.io :as io]))

(defonce ^:private local-eng as/new-eng)   ; Use engine for this namespace
(def ^:private scrpteval (io/resource "clasew-identities.applescript"))

(def ^:private identity-attrs
  #{:name_suffix, :full_name :first_name, :middle_name, :last_name,
    :primary_company, :primary_title, :primary_department})

(def identity-standard identity-attrs)

(def ^:private address-attrs
  #{:address_type, :city_name, :street_name, :zip_code, :country_name, :state_name})

(def address-standard address-attrs)

(def ^:private email-attrs
  #{:email_address, :email_type})

(def email-standard email-attrs)

(def ^:private phone-attrs
  #{:number_value, :number_type})

(def phone-standard phone-attrs)

;;
;;  Script runner for indentities
;;

(defn run-script!
  "Invokes the AppleScript interpreter passing one or more scripts
  to be evaluated"
  [& scripts]
  {:pre [(> (count scripts) 0)]}
  (let [argv (vector (into [] scripts))]
    (with-open [rdr (io/reader scrpteval)]
      (:result (util/clean-result (as/run-ascript! local-eng rdr
                      :reset-binding true
                      :bind-function "clasew_eval"
                      :arguments argv))))))


;;
;; High level DSL functions ---------------------------------------------------
;;

(defn addresses
  "Adds ability to retrieve addresses"
  [& args]
  (into [:addresses] (if (empty? args) address-standard args)))

(defn email-addresses
  "Adds ability to retrieve email addresses"
  []
  (into [:emails] email-standard))

(defn phones
  "Adds ability to retrieve phone numbers"
  []
  (into [:phones] phone-standard))

(defn individuals
  "Returns script directives for retrieving attributes of individuals from the identity
  source (e.g. Outlook vs. Contacts)
  along with any additional sub-attributes. Also supports minor filtering."
  [& args]
  (let [ia (filter keyword? args)]
    {:individuals (if (empty? ia) identity-standard ia)
     :filters     (first (rest (astu/filter-forv :filter args)))
     :emails      (astu/filter-forv :emails args)
     :addresses   (astu/filter-forv :addresses args)
     :phones      (astu/filter-forv :phones args)
     :action      :get-individuals
     }))

(defn individuals-all
  "Prepares script for retriving **all** individuals and associated attributes"
  []
  (individuals (addresses) (email-addresses) (phones)))

(defn add-individuals
  "Returns script directives for adding  one or more individuals"
  [add1 & adds]
  {:action     :add-individuals
   :adds       (conj adds add1)})

(defn delete-individual
  "Returns script directives for deleting individuals matching feature"
  [filt]
  {:action   :delete-individual
   :filters  (first (rest filt))})

(defn- assert-condition
  [{:keys [sets filters] :as mmap}]
  (if (and (not-empty sets) (= nil filters))
      (throw (Exception. "Can not set a value without a corresponding filter"))
    mmap))

(defn update-individual
  "Returns script directives for updates to one or more bits of information of an individual"
  [& newvalmaps]
  (assert-condition {:action :update-individual
   :filters (first (rest (astu/filter-forv :filter newvalmaps)))
   :sets    (into [] (astu/filter-!v newvalmaps))
   :subsets (astu/filter-!forv :filter newvalmaps)}))


(defn- update-child
  [kw args]
  [kw (assert-condition {:filters (first (rest (astu/filter-forv :filter args)))
       :sets (astu/filter-!v args)
       :adds (rest (astu/filter-forv :adds args))})])

(defn update-addresses
  [& newvalmaps]
  (update-child :addresses newvalmaps))

(defn update-phones
  [& newvalmaps]
  (update-child :phones newvalmaps))

(defn update-email-addresses
  [& newvalmaps]
  (update-child :emails newvalmaps))

(defn adds
  [& newvalmaps]
  (into [:adds]  newvalmaps))


