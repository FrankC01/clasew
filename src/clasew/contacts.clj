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

(defn gen-cstruct-individuals
  "Apple's Contacts people extract AST generation.
  no-args : creates prepares for retriving all standard fields
  svec : a vector of keys to extract from record type"
  ([] (gen-cstruct-individuals (into [] ident/identity-standard)))
  ([svec]
   (if (empty? svec)
     (gen-cstruct-individuals)
     (ident/merge-repeat-cstruct {:map-name :indy :setters svec :target [:people]}))
   ))

(defn gen-cstruct-addresses
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
                                :target [:addresses]}))))


(defn addresses
  "Adds ability to retrieve standard address elements or those
  identified in collection"
  ([] [:addresses])
  ([& col] (into [:addresses] col)))

(defn- reduce-addendum
  [[v & args]]
  (cond
   (= v :addresses) (gen-cstruct-addresses args)))

(defn individuals
  [& args]
  (let [va (reduce #(conj %1 (reduce-addendum %2)) [] (filter vector? args))
        sa (gen-cstruct-individuals
            (into [] (filter #(= (vector? %) false) args)))
        sc (ident/genscript (ident/emit-ast :contacts
                    (assoc-in sa [:nesters] va)))]
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
