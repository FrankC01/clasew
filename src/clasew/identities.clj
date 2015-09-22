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

(def ^:private generic-predicate "If predicate expression"
  {
   :E   " equal "
   :NE  " not equal "
   :GT  " greater than "
   :LT  " less than "
   :missing "missing value"
   })

(defn mapset-expressions
  "Lookup to expression predicate"
  [term-kw]
  (get generic-predicate term-kw term-kw))

(defn mapset-generic
  [term-kw]
  (name term-kw))

(def cleanval "Takes an argument and test for 'missing value'.
  Returns value or null"
  (ast/routine
   nil :cleanval :val
   (ast/define-locals nil :oval)
   (ast/set-statement nil (ast/term nil :oval) (ast/null))
   (ast/if-then mapset-expressions :val :NE :missing
               (ast/set-statement nil (ast/term nil :oval)
                                  (ast/term nil :val)))
   (ast/return nil :oval)))

(defn quit
  "Script to quit an application
  appkw - keyword (:outlook or :contacts) identies the application
  to shut down"
  [appkw]
  (genas/ast-consume
   (ast/tell mapset-generic appkw
             (ast/define-locals nil :results)
             (ast/set-statement nil (ast/term nil :results) (ast/empty-list))
             (ast/set-statement
              nil
              (ast/eol-cmd nil :results nil)
              (ast/string-literal "quit successful"))
             (ast/quit)
             (ast/return nil :results))))


(defn setrecordvalues
  "Given a list of vars, generate constructs to set a Applescript record value
  from a source value found in another record"
  [token-fn mapvars targetmap sourcemap]
  (reduce #(conj
            %1
            (ast/record-value token-fn targetmap %2
                              (ast/value-of token-fn %2 sourcemap :cleanval)))
          [] mapvars))

(defn- filter-!v
  [args]
  (cfilter #(not (vector? %)) args))

(defn- filter-v
  [args]
  (cfilter #(vector? %) args))

(defn filter-forv
  [kw args]
  (first (cfilter #(cand (vector? %) (= (first %) kw)) args)))

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
  (if (not-empty sets)
    (if (= nil filters)
      (throw (Exception. "Can not set a value without a corresponding filter"))
      mmap)
    mmap))

(defn update-individual
  "Updates one or more bits of information of an individual"
  [filt & newvalmaps]
  (assert-condition {:action :update-individual
   :filters (first (rest filt))
   :sets    (into [] (filter-!v newvalmaps))
   :subsets (filter-v newvalmaps)}))


(defn- update-child
  [kw filt args]
  [kw (assert-condition {:filters (first (rest filt))
       :sets (filter-!v args)
       :adds (rest (filter-forv :adds args))})])

(defn update-addresses
  [filt & newvalmaps]
  (update-child :addresses filt newvalmaps))

(defn update-phones
  [filt & newvalmaps]
  (update-child :phones filt newvalmaps))

(defn update-email-addresses
  [filt & newvalmaps]
  (update-child :emails filt newvalmaps))

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
