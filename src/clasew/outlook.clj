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

(def ^:private outlook-identities
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
   :contacts             "contacts"
    })

(def ^:private outlook-home-address
  {
   :zip_code         "home zip",
   :city_name        "home city",
   :street_name      "home street address",
   :country_name     "home country",
   :state_name       "home state"
   })

(def ^:private outlook-business-address
  {
   :zip_code         "business zip",
   :city_name        "business city",
   :street_name      "business street address",
   :country_name     "business country",
   :state_name       "business state"
   })

(def ^:private outlook-emails
  {
   :emails          "email addresses"
   :email_address   "address"
   })

(defn- outlook-mapset-core
  [term-kw]
  (get outlook-identities term-kw term-kw))

(defn- outlook-mapset-home
  [term-kw]
  (term-kw outlook-home-address :bad_error))

(defn- outlook-mapset-business
  [term-kw]
  (term-kw outlook-business-address :bad_error))

(defn- outloook-mapset-emails
  [term-kw]
  (term-kw outlook-emails :bad_error))

(defn- setup-addys
  [imap]
  (reduce-kv #(assoc %1 %2 (if (= %3 :gen) (keyword (gensym)) %3))
             {} imap ))

;; Ability to specify a pre-filter to the repeat
;; in essence, the result of the filter is what should be
;; iterated upon

(defn gen-cstruct-filter
  [base-map flt-map]
  (if (empty? (first flt-map))
    base-map
    (let [flt   (assoc ident/repeat-filters :user-filter (first flt-map))
          fltg  (assoc (reduce ident/genpass flt flt) :control-target (:target base-map))]
      (merge base-map {:instance (:loop-field fltg)
                       :filters fltg
                       :instance-flt (:prop-field fltg)
                       :target [(:control-field fltg)]
                       :global-locals [[(:prop-field fltg) :properties (:loop-field fltg) ]]
                       })
      )))

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
                                  :mapset-fn outlook-mapset-core})))
   ([svec flt-map]
    (let [base (if (empty? svec)
                 (gen-cstruct-individuals)
                 (gen-cstruct-individuals svec))]
      (gen-cstruct-filter base flt-map)
      )))

(defn- gen-cstruct-addresses
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

(defn- gen-cstruct-emails
  ([] (gen-cstruct-emails (into [] ident/email-standard)))
  ([svec]
   (if (empty? svec)
     (gen-cstruct-emails)
     (ident/merge-repeat-cstruct {:setters svec
                                :result-list :elist
                                :global-locals [[:elist :list]]
                                :target [:emails]
                                :mapset-fn outloook-mapset-emails})
     )))

(defn- reduce-addendum
  [[v args]]
  (cond
   (= v :addresses) (gen-cstruct-addresses args)
   (= v :emails) (gen-cstruct-emails args)))

(def ^:private split-nested #(and (vector? %1) (= (first %1) :emails)))
(def ^:private split-subset #(and (vector? %1) (= (first %1) :addresses)))

(defn split-up
  "Separates the nester types from subsetter types"
  [args]
  [(reduce #(conj %1 (reduce-addendum %2)) [] (filter split-nested args))
   (reduce #(conj %1 (reduce-addendum %2)) [] (filter split-subset args))])

(defn individuals
  "EXPERIMENTAL: Entry point to setup for ast-emit"
  [& args]
  (let [[nst ssp] (split-up args)
        sa (gen-cstruct-individuals
            (into [] (filter #(and (= (vector? %) false)
                                   (= (map? %) false)) args))
            (filter map? args))
        sv (assoc (assoc sa :sub-setters  (first ssp)) :nesters nst)
        sc (ident/genscript (ident/emit-ast :outlook sv))]
    (with-open [rdr (io/reader scrptcore)]
      [:run-script (str (slurp rdr) sc)])))

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
