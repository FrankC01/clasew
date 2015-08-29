(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - common contacts DSL"}
  clasew.identities
  (:require   [clasew.core :as as]
              [clasew.utility :as util]
              [clasew.gen-as :as genas]
              [clasew.ast-emit :as ast]
              [clojure.java.io :as io]))

(defonce ^:private local-eng as/new-eng)   ; Use engine for this namespace
(def ^:private scrpteval (io/resource "clasew-identities.applescript"))

(def ^:private identity-attrs
  #{:name_suffix, :full_name :first_name, :middle_name, :last_name,
    :primary_company, :primary_title, :primary_department})

(def identity-standard identity-attrs)

(def ^:private address-attrs
  #{:city_name, :street_name, :zip_code, :country_name, :state_name})

(def address-standard address-attrs)

(def ^:private email-attrs
  #{:email_address})

(def email-standard email-attrs)

;;
;;
;;

(defn run-script!
  [& scripts]
  {:pre [(> (count scripts) 0)]}
  (let [argv (vector (into [] scripts))]
    (with-open [rdr (io/reader scrpteval)]
      (util/clean-result (as/run-ascript! local-eng rdr
                      :reset-binding true
                      :bind-function "clasew_eval"
                      :arguments argv)))))

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
  [term-kw]
  (get generic-predicate term-kw term-kw))

(def cleanval "Takes an argument and test for 'missing value'. Returns value or null"
  (ast/routine
   nil :cleanval :val
   (ast/define-locals nil :oval)
   (ast/assign nil :oval :null)
   (ast/if-then mapset-expressions :val :NE :missing
               (ast/assign nil :oval :val))
   (ast/return nil :oval)))


(defn setrecordvalues
  "Given a list of vars, generate constructs to set a map value
  to a source value from another map"
  [token-fn mapvars targetmap sourcemap]
  (reduce #(conj %1 (ast/record-value token-fn targetmap %2
                                    (ast/value-of token-fn %2 sourcemap :cleanval)))
          [] mapvars))


(defn quit
  [appkw]
  (genas/ast-consume (ast/tell nil appkw :results
                               (ast/define-locals nil :results)
                               (ast/define-list nil :results)
                               (ast/extend-list nil :results "\"quit successful\"")
                               (ast/quit))))

;;
;; High level DSL functions ---------------------------------------------------
;;

(defn addresses
  "Adds ability to retrieve standard address elements or those
  identified in collection"
  [& args]
  (into [:addresses] (if (empty? args) address-standard args)))

(defn emails
  "Adds ability to retrieve standard address elements or those
  identified in collection"
  [& args]
  (into [:emails] (if (empty? args) email-standard args)))

(defn individuals
  [& args]
  (let [ia (filter keyword? args)]
    {:individuals (if (empty? ia) identity-standard ia)
     :filters     (first (filter map? args))
     :emails      (first (filter #(and (vector? %) (= (first %) :emails)) args))
     :addresses   (first (filter #(and (vector? %) (= (first %) :addresses)) args))
     }))
