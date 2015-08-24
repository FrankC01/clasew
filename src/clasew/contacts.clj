(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - Apple Contacts DSL"}
  clasew.contacts
  (:require [clasew.core :as as]
            [clasew.utility :as util]
            [clasew.identities :as ident]
            [clojure.java.io :as io]))

(defonce ^:private local-eng as/new-eng)   ; Use engine for this namespace
(def ^:private scrpteval (io/resource "clasew-contacts.applescript"))
(def ^:private scrptcore (io/resource "clasew-core.applescript"))

(def ^:private contacts-identities
  {:name_suffix          "suffix",
   :full_name            "name",           ; display name in outlook
   :first_name           "first name",
   :middle_name,         "middle name",
   :last_name            "last name",
   :primary_company      "organization",   ; company in outlook
   :primary_title        "job title",
   :primary_department   "department",
   :city_name            "city",           ; home xxx and business xxx in outlook
   :street_name          "street",         ; home xxx and business xxx in outlook
   :zip_code             "zip",            ; home xxx and business xxx in outlook
   :country_name         "country",        ; home xxx and business xxx in outlook
   :state_name           "state"           ; home xxx and business xxx in outlook
   :people               "people"
   :addresses            "addresses"
    })

(def ^:private contacts-emails
  {
   :emails          "emails"
   :email_address   "value"
   })

(defn- contacts-mapset-core
  [term-kw]
  (get contacts-identities term-kw :bad_error))

(defn- contacts-mapset-emails
  [term-kw]
  (get contacts-emails term-kw :bad_error))


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


(defn- gen-cstruct-individuals
  "Apple's Contacts people extract AST generation.
  no-args : creates prepares for retriving all standard fields
  svec : a vector of keys to extract from record type"
  ([]
   (gen-cstruct-individuals (into [] ident/identity-standard)))
  ([svec]
   (if (empty? svec)
     (gen-cstruct-individuals)
       (ident/merge-repeat-cstruct {:map-name :indy
                                  :setters svec
                                  :target [:people]
                                  :mapset-fn contacts-mapset-core})))
   ([svec flt-map]
    (let [base (if (empty? svec)
                 (gen-cstruct-individuals)
                 (gen-cstruct-individuals svec))]
      (gen-cstruct-filter base flt-map)
      )))

(defn- gen-cstruct-addresses
  "Apple's Contacts address extract AST generation.
  no-args : creates prepares for retriving all standard fields
  svec : a vector of keys to extract from record type"
  ([] (gen-cstruct-addresses (into [] ident/address-standard)))
  ([svec]
   (if (empty? svec)
     (gen-cstruct-addresses)
     (ident/merge-repeat-cstruct {:setters svec
                                :result-list :addlist
                                :global-locals [[:addlist :list]]
                                :target [:addresses]
                                :mapset-fn contacts-mapset-core}))))

(defn- gen-cstruct-emails
  "Apple's Contacts email extract AST generation.
  no-args : creates prepares for retriving all standard fields
  svec : a vector of keys to extract from record type"
  ([] (gen-cstruct-addresses (into [] ident/email-standard)))
  ([svec]
   (if (empty? svec)
     (gen-cstruct-addresses)
     (ident/merge-repeat-cstruct {:setters svec
                                :result-list :elist
                                :global-locals [[:elist :list]]
                                :target [:emails]
                                :mapset-fn contacts-mapset-emails}))))


(defn- reduce-addendum
  [[v args]]
  (cond
   (= v :addresses) (gen-cstruct-addresses args)
   (= v :emails) (gen-cstruct-emails args)))

(def ^:private split-nested #(and (vector? %1) (or (= (first %1) :emails) (= (first %1) :addresses))))
;(def ^:private split-subset #(and (vector? %1) (= (first %1) :addresses)))

(defn split-up
  "Separates the nester types from subsetter types"
  [args]
  [(reduce #(conj %1 (reduce-addendum %2)) [] (filter split-nested args))
   ;(reduce #(conj %1 (reduce-addendum %2)) [] (filter split-subset args))
   '(ni)
   ])


(defn individuals
  "EXPERIMENTAL: Entry point to setup for ast-emit"
  [& args]
  (let [[nst ssp] (split-up args)
        sa (gen-cstruct-individuals
            (into [] (filter #(and (= (vector? %) false)
                                   (= (map? %) false)) args))
            (filter map? args))
        sv (assoc (assoc sa :sub-setters  (first ssp)) :nesters nst)
        sc (ident/genscript (ident/emit-ast :contacts sv))]
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
