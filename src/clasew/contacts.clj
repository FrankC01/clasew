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

;;
;; Consistent functional args/terms
;;

(def ^:private CONTACTS  :contacts)
(def ^:private RESULTS   :results)

;;
;;
;;

(defn- contacts-mapset-core
  "Term lookup function"
  [term-kw]
  (get contacts-identities term-kw (name term-kw)))

(defn- builder
  [fn & args]
  (apply fn args))

(defn- build-tell
  "Build the outermost tell application x (...) AST"
  [body]
  (ast/tell
   nil
   CONTACTS
   (ast/define-locals nil RESULTS :cloop :ident)
   (ast/set-statement nil (ast/term nil RESULTS) ast/empty-list)
   body
   (ast/return nil RESULTS)))

;;
;; Subrecord build support
;;

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

(defn- build-individual
  "Construct the overall individual fetch construct allowing for
  filtering"
  [args lkw addr emls phns filt]
  (let [gets  (apply (partial ast/block nil)
                     (filter #(not (nil? %))
                             (conj (ast/setrecordvalues
                                    nil args :ident :cloop)
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


(defn- get-contacts
  [{:keys [individuals filters emails addresses phones]}]
  (genas/ast-consume
   (builder (partial ast/block nil) ident/cleanval
            (build-individual
             individuals :cloop
             (build-subrecord
              :cloop
              (rest addresses)
              address-dmap)
             (build-subrecord
              :cloop
              (rest emails)
              emails-dmap)
             (build-subrecord
              :cloop
              (rest phones)
              phones-dmap)
             filters))))

(defn- delete-contact
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

(defn add-record-definitions
  [imap]
  (apply (partial ast/record-definition nil)
         (seq
          (reduce
           #(conj
             %1
             (ast/key-value
              nil
              (ast/key-term (first %2))
              (ast/string-literal (second %2))))
           [] imap))))

(defn expand
  "Creates a new sub-record type (pkey) of type (ckey) at end of
  list (skey) of (ckey) with properties (coll)"
  [coll pkey skey ckey]
  (apply (partial ast/block nil)
         (reduce
          #(conj
            %1
            (ast/make-new
             nil
             (ast/term nil pkey)
             (ast/expression
              nil
              (ast/term nil " at ")
              (ast/eol-cmd nil skey ckey)
              ast/with-properties
              (add-record-definitions %2)))) [] coll)))


(defn expand-map
  "Takes an input map and generates a block statement that:
  1. Make a new person record
  2. Extends the emails list of person to one or more (if any) new email-addys
  3. Extends the addresses list of person to one or more (if any) new addys
  4. Extends the phones list of person to one or more (if any) new numbers"
  [imap mkey mlist]
  ;(println imap mkey mlist)
  (let [base (dissoc imap :addresses :emails :phones)
        emls (expand (get imap :emails nil) :email :emails mkey)
        phns (expand (get imap :phones nil) :phone :phones mkey)
        adds (expand (get imap :addresses nil) :address :addresses mkey)
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
                                  (add-record-definitions base)
                                  ))))))

(defn- add-contacts
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

(defn script
  [{:keys [action] :as directives}]
  (condp = action
    :get-individuals     (get-contacts directives)
    :delete-individual   (delete-contact directives)
    :add-individuals     (add-contacts directives)
    (str "Don't know how to complete " action)))
