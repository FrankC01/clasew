(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - Apple Contacts DSL"}
  clasew.contacts
  (:require [clasew.gen-as :as genas]
            [clasew.ast-emit :as ast]
            [clasew.identities :as ident]))

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

;; Contacts test structure
#_(def t1 (ast/block
         nil
         ident/cleanval
         (ast/tell nil :contacts :results
                      (ast/define-locals nil :ident :results :cloop)
                      (ast/define-list nil :results)
                      (ast/filtered-repeat-loop
                       contacts-mapset-core
                       :cloop
                       {:first_name "Frank"}
                       :people nil
                       (ast/define-record nil :ident ident/identity-standard)
                       (ast/blockf
                        nil
                        (conj
                         (ident/setrecordvalues nil ident/identity-standard
                                          :ident :cloop)

                         ;; Address Management - Contacts
                         (ast/define-locals nil :add_list :hadd)
                         (ast/define-list nil :add_list)
                         (ast/define-record nil :hadd ident/address-standard)
                         (ast/repeat-loopf
                          nil :addr :addresses :cloop
                          (conj

                           (ident/setrecordvalues
                            nil
                            ident/address-standard
                            :hadd :addr)
                           (ast/extend-list nil :add_list :hadd)
                           ))
                         (ast/extend-record nil :ident :address_list :add_list)
                         (ast/extend-list nil :results :ident)))))))

;(p t1)
;(println (time (genas/ast-consume t1)))

(defn builder
  [fn & args]
  (apply fn args))

(defn build-tell
  [body]
  (ast/tell nil :contacts :results
               (ast/define-locals nil :results :cloop :ident)
               (ast/define-list nil :results)
               body))


(defn build-address
  [tkey args]
  (if (nil? args)
    args
    (ast/block
     (ast/define-locals nil :add_list :hadd)
     (ast/define-list nil :add_list)
     (ast/define-record nil :hadd args)
     (ast/repeat-loopf
      nil :addr :addresses tkey
      (conj
       (ident/setrecordvalues
        contacts-mapset-core
        args
        :hadd :addr)
       (ast/extend-list nil :add_list :hadd)))
     (ast/extend-record nil :ident :address_list :add_list))))

(defn build-individual
  [args lkw addr filt]
  (let [gets  (apply (partial ast/block nil)
                     (filter #(not (nil? %))
                             (conj (ident/setrecordvalues nil args :ident :cloop)
                                   (if addr addr)
                                   (ast/extend-list nil :results :ident))))]
    (build-tell
     (if (not-empty filt)
       (ast/filtered-repeat-loop contacts-mapset-core lkw filt :people nil
                               (ast/define-record nil :ident args)
                               gets)
       (ast/repeat-loop contacts-mapset-core lkw :people nil
                      (ast/define-record nil :ident args)
                      gets)))))


(defn script
  [{:keys [individuals filters emails addresses] :as directives}]
  (genas/ast-consume (builder (partial ast/block nil) ident/cleanval
                              (build-individual
                               individuals :cloop
                               (build-address :cloop (rest addresses))
                               filters))))


