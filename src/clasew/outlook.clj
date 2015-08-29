(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - Microsoft Outlook DSL"}
  clasew.outlook
  (:require [clasew.gen-as :as genas]
            [clasew.ast-emit :as ast]
            [clasew.identities :as ident]))



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

(defn outlook-mapset-core
  [term-kw]
  (get outlook-identities term-kw term-kw))

(defn outlook-mapset-home
  [term-kw]
  (term-kw outlook-home-address :bad_error))

(defn outlook-mapset-business
  [term-kw]
  (term-kw outlook-business-address :bad_error))

(defn outloook-mapset-emails
  [term-kw]
  (term-kw outlook-emails :bad_error))

;; Outlook test structure

#_(def t0 (ast/block
         nil
         ident/cleanval
         (ast/tell nil :outlook :results
                      (ast/define-locals nil :ident :results :cloop)
                      (ast/define-list nil :results)
                      (ast/filtered-repeat-loop
                       outl/outlook-mapset-core
                       :cloop
                       {:first_name "Frank"}
                       :contacts nil
                       (ast/define-record nil :ident ident/identity-standard)
                       (ast/blockf
                        nil
                        (conj
                         (ident/setrecordvalues nil ident/identity-standard
                                          :ident :cloop)
                         ;; Address Management - Outlook
                         (ast/define-locals nil :aloop :hadd :badd)
                         (ast/define-list nil :aloop)
                         (ast/define-record nil :hadd ident/address-standard)
                         (ast/define-record nil :badd ident/address-standard)

                         ;; Home address
                         (ast/blockf
                          nil
                          (conj
                           (ident/setrecordvalues
                            outl/outlook-mapset-home
                            ident/address-standard
                            :hadd :cloop)
                           (ast/extend-list nil :aloop :hadd)))

                         ;; Business address
                         (ast/blockf
                          nil
                          (conj
                           (ident/setrecordvalues
                            outl/outlook-mapset-business
                            ident/address-standard
                            :badd :cloop)
                           (ast/extend-list nil :aloop :badd)))
                         (ast/extend-record nil :ident :address_list :aloop)
                         (ast/extend-list nil :results :ident)))))))

;(p t0)
;(println (time (genas/ast-consume t0)))


(defn builder
  [fn & args]
  (apply fn args))

(defn build-tell
  [body]
  (ast/tell nil :outlook :results
               (ast/define-locals nil :results :cloop :ident)
               (ast/define-list nil :results)
               body))


(defn build-address
  [tkey args]
  (if (nil? args)
    args
    (ast/block
     (ast/define-locals nil :aloop :hadd :badd)
     (ast/define-list nil :aloop)
     (ast/define-record nil :hadd args)
     (ast/define-record nil :badd args)

     ;; Home address
     (ast/blockf
      nil
      (conj
       (ident/setrecordvalues
        outlook-mapset-home
        args
        :hadd tkey)
       (ast/extend-list nil :aloop :hadd)))

     ;; Business address
     (ast/blockf
      nil
      (conj
       (ident/setrecordvalues
        outlook-mapset-business
        args
        :badd tkey)
       (ast/extend-list nil :aloop :badd)))
     (ast/extend-record nil :ident :address_list :aloop))))


(defn build-individual
  [args lkw addr filt]
  (let [gets  (apply (partial ast/block nil)
                     (filter #(not (nil? %))
                             (conj (ident/setrecordvalues nil args :ident :cloop)
                                   (if addr addr)
                                   (ast/extend-list nil :results :ident))))]
    (build-tell
     (if (not-empty filt)
       (ast/filtered-repeat-loop outlook-mapset-core lkw filt :contacts nil
                               (ast/define-record nil :ident args)
                               gets)
       (ast/repeat-loop outlook-mapset-core lkw :contacts nil
                      (ast/define-record nil :ident args)
                      gets)))))


(defn script
  [{:keys [individuals filters emails addresses] :as directives}]
  (genas/ast-consume (builder (partial ast/block nil) ident/cleanval
                              (build-individual
                               individuals :cloop
                               (build-address :cloop (rest addresses))
                               filters))))


