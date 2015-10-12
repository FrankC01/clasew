(ns
  ^{:author "Frank V. Castellucci"
    :doc "Clojure AppleScriptEngine Wrapper - common contacts DSL"}
  clasew.identities
  (:require   [clasew.core :as as]
              [clasew.utility :as util]
              [clasew.gen-as :as genas]
              [clasew.ast-emit :as ast]
              [clojure.java.io :as io])
  (:refer-clojure :rename {filter cfilter
                           or     cor
                           and    cand}))

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
;; Special handlers
;;

(defn- filter-!v
  "Filter for non-vector data types"
  [args]
  (cfilter #(not (vector? %)) args))

(defn- filter-v
  "Filter for vector data types"
  [args]
  (cfilter #(vector? %) args))

(defn-  filter-for-vkw
  "Filter for vector with first position keyword match"
  [kw args]
  (cfilter #(cand (vector? %) (= (first %) kw)) args))

(defn- filter-!forv
  "Filter for vector not containing first position key"
  [kw args]
  (cfilter #(cand (vector? %) (not= (first %) kw)) args))

(defn filter-forv
  "[kw args] First position return from filter-for-vkw "
  ([kw args] (filter-forv kw args first))
  ([kw args f] (f (filter-for-vkw kw args))))

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
  (let [ia (cfilter keyword? args)]
    {:individuals (if (empty? ia) identity-standard ia)
     :filters     (first (rest (filter-forv :filter args)))
     :emails      (filter-forv :emails args)
     :addresses   (filter-forv :addresses args)
     :phones      (filter-forv :phones args)
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
  (if (cand (not-empty sets) (= nil filters))
      (throw (Exception. "Can not set a value without a corresponding filter"))
    mmap))

(defn update-individual
  "Returns script directives for updates to one or more bits of information of an individual"
  [& newvalmaps]
  (assert-condition {:action :update-individual
   :filters (first (rest (filter-forv :filter newvalmaps)))
   :sets    (into [] (filter-!v newvalmaps))
   :subsets (filter-!forv :filter newvalmaps)}))


(defn- update-child
  [kw args]
  [kw (assert-condition {:filters (first (rest (filter-forv :filter args)))
       :sets (filter-!v args)
       :adds (rest (filter-forv :adds args))})])

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

;;
;; Filtering
;;

(def EQ :equal-to)
(def !EQ :not-equal-to)
(def LT :less-than)
(def !LT :not-less-than)
(def GT :greater-than)
(def !GT :not-greater-than)
(def CT :contains)
(def !CT :not-contains)
(def SW :starts-with)
(def EW :ends-with)
(def II :is-in)
(def !II :is-not-in)

(defn- filter-parse
  "Prepares filter constructs for emitting"
  [coll]
  (assoc {}
  :args (partition 3 (cfilter #(not (vector? %)) coll))
   :joins (cfilter vector? coll)))

(defn filter
  "Used to construct complex filter for individuals"
  [& args]
  [:filter (filter-parse args)])

(defn and
  "Used in complex filter construction. Use 'cand' for core function
  in this namespeace"
  [& args]
  [:and (filter-parse args)])

(defn or
  "Used in complex filter construction. Use 'cor' for core function
  in this namespeace"
  [& args]
  [:or (filter-parse args)])

;; Predefined utility types

(def cleanval "Takes an argument and test for 'missing value'.
  Returns value or null"
  (ast/routine
   nil :cleanval :val
   (ast/define-locals nil :oval)
   (ast/set-statement nil (ast/term nil :oval) ast/null)
   (ast/if-statement
    nil
    (ast/if-expression
     nil
     (second (filter :val !EQ :missing))
     (ast/set-statement nil (ast/term nil :oval)
                                  (ast/term nil :val))) nil)
   (ast/return nil :oval)))

(defn quit
  "Script to quit an application
  appkw - keyword (:outlook or :contacts) identies the application
  to shut down"
  [appkw]
  (genas/ast-consume
   (ast/tell nil appkw
             (ast/define-locals nil :results)
             (ast/set-statement nil (ast/term nil :results) ast/empty-list)
             (ast/set-statement
              nil
              (ast/eol-cmd nil :results nil)
              (ast/string-literal "quit successful"))
             ast/quit
             (ast/return nil :results))))
