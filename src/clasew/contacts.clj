(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - Apple Contacts DSL"}
  clasew.contacts
  (:require [clasew.utility :as util]
            [clasew.identities :as ident]
            [clojure.java.io :as io]))


(defn- individuals
  "Apple's Contacts people extract AST generation.
  no-args : creates prepares for retriving all standard fields
  svec : a vector of keys to extract from record type"
  ([] (individuals (into [] ident/identity-standard)))
  ([svec]
   (if (empty? svec)
     (individuals)
     (ident/merge-repeat-cstruct {:setters svec :target [:people]}))
   ))

(defn- addresses
  "Apple's Contacts address extract AST generation.
  no-args : creates prepares for retriving all standard fields
  svec : a vector of keys to extract from record type"
  ([] (addresses (into [] ident/address-standard)))
  ([svec]
   (if (empty? svec)
     (addresses)
     (ident/merge-repeat-cstruct {:setters svec
                                :result-list :addlist
                                :global-locals [[:addlist :list]]
                                :target [:addresses]}))))


(defn get-address
  "Adds ability to retrieve standard address elements or those
  identified in collection"
  ([] [:addresses])
  ([& col] (into [:addresses] col)))

(defn- reduce-addendum
  [[v & args]]
  (cond
   (= v :addresses) (addresses args)))

(defn get-individuals
  [& args]
  (let [va (reduce #(conj %1 (reduce-addendum %2)) [] (filter vector? args))
        sa (individuals (into [] (filter #(= (vector? %) false) args)))]
    (ident/genscript (ident/emit-ast :contacts
                    (assoc-in sa [:nesters] va)))))
