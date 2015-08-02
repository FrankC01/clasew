(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - Microsoft Outlook DSL"}
  clasew.outlook
  (:require [clasew.core :as as]
            [clasew.utility :as util]
            [clasew.identities :as ident]
            [clojure.java.io :as io]))

(defonce ^:private local-eng as/new-eng)   ; Use engine for this namespace
(def ^:private scrpteval (io/resource "clasew-outlook.applescript"))
(def ^:private scrptcore (io/resource "clasew-core.applescript"))

(def identities-outlook
  {:name_suffix          "suffix",
   :full_name            "display name",           ; display name in outlook
   :first_name           "first name",
   :middle_name,         "middle name",
   :last_name            "last name",
   :primary_company      "company",
   :primary_title        "job title",
   :primary_department   "department",
   :city_name            "city",           ; home xxx and business xxx in outlook
   :street_name          "street",         ; home xxx and business xxx in outlook
   :zip_code             "zip",            ; home xxx and business xxx in outlook
   :country_name         "country",        ; home xxx and business xxx in outlook
   :state_name           "state"           ; home xxx and business xxx in outlook
    })


(def outlook-home-address
  {
   :zip_code         "home zip",
   :city_name        "home city",
   :street_name      "home street address",
   :country_name     "home country",
   :state_name       "home state"
   })

(def outlook-business-address
  {
   :zip_code         "business zip",
   :city_name        "business city",
   :street_name      "business street address",
   :country_name     "business country",
   :state_name       "business state"
   })

(defn outlook-mapset-core
  [term-kw]
  (term-kw identities-outlook :bad_error))

(defn outlook-mapset-home
  [term-kw]
  (term-kw outlook-home-address :bad_error))

(defn outlook-mapset-business
  [term-kw]
  (term-kw outlook-business-address :bad_error))

(defn setup-addys
  [imap]
  (reduce-kv #(assoc %1 %2 (if (= %3 :gen) (keyword (gensym)) %3))
             {} imap ))

(defn gen-cstruct-individuals
  "MS Outlook people extract AST generation.
  no-args : creates prepares for retriving all standard fields
  svec : a vector of keys to extract from record type"
  ([] (gen-cstruct-individuals (into [] ident/identity-standard)))
  ([svec]
   (if (empty? svec)
     (gen-cstruct-individuals)
     (ident/merge-repeat-cstruct {:map-name :indy
                                  :setters svec
                                  :target [:contacts]
                                  :mapset-fn outlook-mapset-core}))
   ))

(defn gen-cstruct-addresses
  "Microsoft Outlook address extract AST generation.
  no-args : creates prepares for retriving all standard fields
  svec : a vector of keys to extract from record type"
  ([] (gen-cstruct-addresses (into [] ident/address-standard)))
  ([svec]
   (if (empty? svec)
     (gen-cstruct-addresses)
     (map setup-addys (list
      (assoc (assoc ident/repeat-subsetters :mapset-fn outlook-mapset-home)
                 :setters svec
                 :global-locals [[:add_list :list]]
                 :result-list :add_list)
      (assoc (assoc ident/repeat-subsetters :mapset-fn outlook-mapset-business)
                 :setters svec
                 :result-list :add_list
                 :result-map :addresses))
               ))))

(defn addresses
  "Adds ability to retrieve standard address elements or those
  identified in collection"
  ([] [:addresses ident/address-standard])
  ([& col] [:addresses (into [] col)]))

(defn- reduce-addendum
  [[v args]]
  (cond
   (= v :addresses) (gen-cstruct-addresses args)))

(defn individuals
  [& args]
  (let [va (reduce #(conj %1 (reduce-addendum %2)) [] (filter vector? args))
        sa (gen-cstruct-individuals(into [] (filter #(= (vector? %) false) args)))
        sv (assoc sa :sub-setters (first va))
        sc (ident/genscript (ident/emit-ast :outlook sv))]
  (with-open [rdr (io/reader scrptcore)]
    [:run-script (str (slurp rdr) sc)])
    ))

(defn clasew-contacts-call!
  "Takes 1 or more maps produced from XXX and invokes AppleScript
  for execution.
  Return map is same as clasew.core/run-ascript!"
  [& scripts]
  {:pre [(> (count scripts) 0)]}
  (let [argv (list (into [] scripts))]
    (with-open [rdr (io/reader scrpteval)]
      (util/clean-result (as/run-ascript! local-eng rdr
                      :reset-binding true
                      :bind-function "clasew_eval"
                      :arguments argv)))))
