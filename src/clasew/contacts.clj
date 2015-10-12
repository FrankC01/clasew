(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - Apple Contacts DSL"}
  clasew.contacts
  (:require [clasew.gen-as :as genas]
            [clasew.ast-emit :as ast]
            [clasew.identities :as ident]
            [clasew.ident-utils :as utils]))

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

;;

(defn- contacts-mapset-core
  "Term lookup function"
  [term-kw]
  (get contacts-identities term-kw (name term-kw)))

;;
;; Consistent functional args/terms
;;

(def ^:private CONTACTS  :contacts)
(def ^:private RESULTS   :results)
(def ^:private CLOOP     :cloop)

(defn- builder
  [fn & args]
  (apply fn args))

(defn- build-tell
  "Build the outermost tell application x (...) AST"
  [body]
  (ast/tell
   nil
   CONTACTS
   (ast/define-locals nil RESULTS CLOOP :ident)
   (ast/set-statement nil (ast/term nil RESULTS) ast/empty-list)
   body
   (ast/return nil RESULTS)))


;;
;; Fetch people
;;

;; Subrecord fetch support

(def ^:private address-dmap
  {:clist  :add_list
   :owner  :hadd
   :loopc  :addr
   :target :addresses
   :resl   :address_list
   :extnd  :ident
   })

(def ^:private emails-dmap
  {:clist  :elist
   :owner  :eadd
   :loopc  :eml
   :target :emails
   :resl   :email_list
   :extnd  :ident
   })

(def ^:private phones-dmap
  {:clist  :plist
   :owner  :padd
   :loopc  :phn
   :target :phones
   :resl   :phone_list
   :extnd  :ident
   })


(defn- build-subrecord
  "Create fetch statements for subrecords.
  tkey : Source target key
  args : properties to fetch
  dmap : control structure where
        {:clist - identifies the collection list (:add_list)
        :owner - identifies the result record (:hadd)
        :loopc - identifies the var used as the loop variable (:addr)
        :target- identifies the application target (:addresses)
        :resl  - identifies the key to the result list (:address_list)
        :extnd - identifies the record to extend with list (:ident)}"
  [tkey args {:keys [clist owner loopc target resl extnd]}]
  (if (empty? args)
    nil
    (ast/block
     nil
     (ast/define-locals nil clist owner loopc)
     (ast/set-statement nil (ast/term nil clist) ast/empty-list)
     (apply
      (partial ast/for-in-expression
               contacts-mapset-core
               (ast/term nil loopc)
               (ast/get-statement
                nil
                (ast/xofy-expression
                 nil
                 (ast/term nil target)
                 (ast/term nil tkey)))
                 (ast/set-empty-record nil owner args
                                       {:ktfn ast/key-term-nl}))
      (conj
       (ast/setrecordvalues
        nil
        args
        owner loopc)
       (ast/set-statement nil
                          (ast/eol-cmd nil clist nil)
                          (ast/term nil owner))))
     (ast/set-extend-record extnd resl clist))))

(defn- build-people
  "Construct the overall individual fetch construct allowing for
  filtering"
  [args lkw addr emls phns filt]
  (let [gets  (apply (partial ast/block nil)
                     (filter #(not (nil? %))
                             (conj (ast/setrecordvalues
                                    nil args :ident CLOOP)
                                   addr emls phns
                                   (ast/set-statement
                                    nil
                                    (ast/eol-cmd nil RESULTS nil)
                                    (ast/term nil :ident)))))]
    (build-tell
     (ast/for-in-expression
      contacts-mapset-core
      (ast/term nil lkw)
      (if (empty? filt)
        (ast/term nil :people)
        (ast/where-filter nil
                          (ast/term nil :people)
                          filt))
      (ast/set-empty-record nil :ident args {:ktfn ast/key-term-nl})
      gets))))


(defn- get-people
  "Fetch individuals from Contacts"
  [{:keys [individuals filters emails addresses phones]}]
  (genas/ast-consume
   (builder (partial ast/block nil) ident/cleanval
            (build-people
             individuals CLOOP
             (build-subrecord CLOOP (rest addresses) address-dmap)
             (build-subrecord CLOOP (rest emails) emails-dmap)
             (build-subrecord CLOOP (rest phones) phones-dmap)
             filters))))
;;
;; Delete people
;;

(defn- delete-people
  "Delete individuals with filters"
  [{:keys [filters]}]
  (genas/ast-consume
   (ast/tell
    contacts-mapset-core CONTACTS
    (ast/define-locals nil RESULTS :dlist :dloop)
    (ast/set-statement nil (ast/term nil RESULTS) ast/empty-list)
    (ast/set-statement nil (ast/term nil :dlist)
                       (ast/where-filter nil
                                         (ast/term nil :people)
                                         filters))
    (ast/for-in-expression
     nil
     (ast/term nil :dloop) (ast/term nil :dlist)
     (ast/expression nil ast/delete (ast/term nil :dloop)))
    (ast/set-result-msg-with-count "Records deleted :" RESULTS :dlist)
    (ast/save-statement)
    (ast/return nil RESULTS))))

;;
;; Add new people
;;

(defn- expand-map
  "Takes an input map and generates a block statement that:
  1. Make a new person record
  2. Extends the emails list of person to one or more (if any) new email-addys
  3. Extends the addresses list of person to one or more (if any) new addys
  4. Extends the phones list of person to one or more (if any) new numbers"
  [imap mkey mlist]
  ;(println imap mkey mlist)
  (let [base (dissoc imap :addresses :emails :phones)
        emls (utils/expand (get imap :emails nil) :email :emails mkey)
        phns (utils/expand (get imap :phones nil) :phone :phones mkey)
        adds (utils/expand (get imap :addresses nil) :address :addresses mkey)
        lst  (ast/set-statement
              nil
              (ast/eol-cmd nil mlist nil)
              (ast/term nil mkey))
        nnl  (flatten (conj '() lst emls phns adds))]
    (apply (partial ast/block nil)
             (conj nnl
                   (ast/set-statement nil (ast/term nil mkey)
                                 (ast/make-new
                                  nil
                                  (ast/term nil :person)
                                  ast/with-properties
                                  (utils/add-record-definitions base)))))))

(defn- add-people
  "Adds new individuals to contacts people. Requires breaking out
  emails, phones and addresses to seperate make sets and adding them
  to the appropriate list of the new person"
  [{:keys [adds]}]
  (genas/ast-consume
   (ast/tell
    contacts-mapset-core CONTACTS
    (ast/define-locals nil RESULTS :alist :dlist :thePerson)
    (ast/set-statement nil (ast/term nil :alist) ast/empty-list)
    (ast/set-statement nil (ast/term nil RESULTS) ast/empty-list)
    (apply (partial ast/block nil)
     (seq
      (reduce #(conj %1 (expand-map %2 :thePerson :alist)) [] adds)))
    (ast/set-result-msg-with-count "Records added :" RESULTS :alist)
    (ast/save-statement)
    (ast/return nil RESULTS))))

;;
;; Update people
;;

(defn- update-people
  "Updates individuals or child values allowing for the addition of
  subtype (phone, email, address) creations as part of the update"
  [{:keys [filters sets subsets] :as iblock}]
  (genas/ast-consume
   (ast/tell
    contacts-mapset-core CONTACTS
    (ast/define-locals nil RESULTS CLOOP)
    (ast/set-statement nil (ast/term nil RESULTS) ast/empty-list)
    (utils/update-filter-block
     CLOOP :people nil iblock
     (apply (partial ast/block nil)
            (reduce utils/update-subsets-reduce [] subsets)))
    (ast/set-statement
     nil
     (ast/eol-cmd nil RESULTS nil)
     (ast/string-literal "Update successful"))
    (ast/save-statement)
    (ast/return nil RESULTS))))

(defn script
  [{:keys [action] :as directives}]
  (condp = action
    :get-individuals     (get-people directives)
    :delete-individual   (delete-people directives)
    :add-individuals     (add-people directives)
    :update-individual   (update-people directives)
    (str "Don't know how to complete " action)))
