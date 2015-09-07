(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - Apple Contacts DSL"}
  clasew.contacts
  (:require [clasew.gen-as :as genas]
            [clasew.ast-emit :as ast]
            [clasew.identities :as ident]))

(def ^:private contacts-identities
  {:name_suffix          "suffix",
   :full_name            "name",
   :first_name           "first name",
   :middle_name,         "middle name",
   :last_name            "last name",
   :primary_company      "organization",
   :primary_title        "job title",
   :primary_department   "department",
   :address_type         "label",
   :city_name            "city",
   :street_name          "street",
   :zip_code             "zip",
   :country_name         "country",
   :state_name           "state",
   :people               "people",
   :addresses            "addresses",
   :emails               "emails",
   :email_address        "value",
   :email_type           "label",
   :phones               "phones",
   :number_value         "value",
   :number_type          "label"
    })

(defn- contacts-mapset-core
  "Term lookup function"
  [term-kw]
  (get contacts-identities term-kw (name term-kw)))

(defn builder
  [fn & args]
  (apply fn args))

(defn build-tell
  [body]
  (ast/tell nil :contacts
               (ast/define-locals nil :results :cloop :ident)
               (ast/define-list nil :results)
               body
            (ast/return nil :results)))


(defn build-address
  [tkey args]
  (if (empty? args)
    nil
    (ast/block
     (ast/define-locals nil :add_list :hadd)
     (ast/define-list nil :add_list)
     (ast/define-record nil :hadd args)
     (ast/repeat-loopf
      contacts-mapset-core :addr :addresses tkey
      (conj
       (ident/setrecordvalues
        nil
        args
        :hadd :addr)
       (ast/extend-list nil :add_list :hadd)))
     (ast/extend-record nil :ident :address_list :add_list))))

(defn build-emails
  [tkey args]
  (if (empty? args)
    nil
    (ast/block nil
     (ast/define-locals nil :elist :eadd)
     (ast/define-list nil :elist)
     (ast/repeat-loopf
      contacts-mapset-core :eml :emails tkey
      (conj (seq (conj
       (ident/setrecordvalues
        nil
        args
        :eadd :eml)
       (ast/extend-list nil :elist :eadd))) (ast/define-record nil :eadd args)))
     (ast/extend-record nil :ident :email_list :elist))))

(defn build-phones
  [tkey args]
  (if (empty? args)
    nil
    (ast/block nil
     (ast/define-locals nil :plist :padd)
     (ast/define-list nil :plist)
     (ast/repeat-loopf
      contacts-mapset-core :phn :phones tkey
      (conj (seq (conj
       (ident/setrecordvalues
        nil
        args
        :padd :phn)
       (ast/extend-list nil :plist :padd))) (ast/define-record nil :padd args)))
     (ast/extend-record nil :ident :phone_list :plist))))


(defn build-individual
  [args lkw addr emls phns filt]
  (let [gets  (apply (partial ast/block nil)
                     (filter #(not (nil? %))
                             (conj (ident/setrecordvalues nil args :ident :cloop)
                                   addr emls phns
                                   (ast/extend-list nil :results :ident))))]
    (build-tell
     (if (not-empty filt)
       (ast/filtered-repeat-loop contacts-mapset-core lkw filt :people nil
                               (ast/define-record nil :ident args)
                               gets)
       (ast/repeat-loop contacts-mapset-core lkw :people nil
                      (ast/define-record nil :ident args)
                      gets)))))


(defn- get-individuals
  [{:keys [individuals filters emails addresses phones]}]
  (let [lctl (if (not-empty filters) :fitr :cloop)]
  (genas/ast-consume (builder (partial ast/block nil) ident/cleanval
                              (build-individual
                               individuals :cloop
                               (build-address lctl (rest addresses))
                               (build-emails lctl (rest emails))
                               (build-phones lctl (rest phones))
                               filters)))))

(defn script
  [{:keys [action] :as directives}]
  (condp = action
    :get-individuals (get-individuals directives)
    (str "Don't know how to complete " action)))
