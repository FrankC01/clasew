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
               (ast/set-statement nil (ast/term nil :results) (ast/empty-list))
               body
            (ast/return nil :results)))


(defn build-address
  [tkey args]
  (if (empty? args)
    nil
    (ast/block
     (ast/define-locals nil :add_list :hadd)
     (ast/set-statement nil (ast/term nil :add_list) (ast/empty-list))
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
    (ast/set-statement nil (ast/term nil :elist) (ast/empty-list))
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
    (ast/set-statement nil (ast/term nil :plist) (ast/empty-list))
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


(defn- get-contacts
  [{:keys [individuals filters emails addresses phones]}]
  (let [lctl (if (not-empty filters) :fitr :cloop)]
  (genas/ast-consume (builder (partial ast/block nil) ident/cleanval
                              (build-individual
                               individuals :cloop
                               (build-address lctl (rest addresses))
                               (build-emails lctl (rest emails))
                               (build-phones lctl (rest phones))
                               filters)))))

(defn- delete-contact
  [{:keys [filters]}]
  (genas/ast-consume
   (ast/tell
    contacts-mapset-core :contacts
    (ast/define-locals nil :results :dlist)
    (ast/set-statement nil (ast/term nil :results) (ast/empty-list))
    (ast/set-statement nil (ast/term nil :dlist)
                       (ast/filter-expression nil :people nil filters))
    (ast/delete-expression nil (ast/list-items-cmd nil :dlist))
    (ast/set-statement nil
                       (ast/eol-cmd nil :results nil)
                       (ast/string-builder
                        nil
                        (ast/string-literal "Records deleted :")
                        (ast/count-expression nil
                                              (ast/term nil :dlist))))
    (ast/save)
    (ast/return nil :results))))


(defn expand
  [coll pkey skey ckey]
  (reduce #(conj %1 (ast/make-new-inlist-record
                     nil
                     pkey skey ckey %2 true)) (list) coll))

(defn expand-map
  "Takes an input map and generates a block statement that:
  1. Make a new person record
  2. Extends the emails list of person to one or more (if any) new email-addys
  3. Extends the addresses list of person to one or more (if any) new addys
  4. Extends the phones list of person to one or more (if any) new numbers"
  [imap mkey mlist]
  (let [base (dissoc imap :addresses :emails :phones)
        emls (expand (get imap :emails nil) :email :emails mkey)
        phns (expand (get imap :phones nil) :phone :phones mkey)
        adds (expand (get imap :addresses nil) :address :addresses mkey)
        lst  (list (ast/extend-list-with-expression nil mlist (ast/term nil mkey)))
        nnl  (flatten (conj lst emls phns adds))]
    (ast/blockf nil
             (conj nnl
                   (ast/set-expression nil mkey
                                 (ast/make-new-record nil :person base))))))

(defn- add-contacts
  "Adds new individuals to contacts people. Requires breaking out
  emails, phones and addresses to seperate make sets and adding them
  to the appropriate list of the new person"
  [{:keys [adds]}]
  (genas/ast-consume
   (ast/tell
    contacts-mapset-core :contacts
    (ast/define-locals nil :results :alist :dlist :rstring :thePerson)
    (ast/set-statement nil (ast/term nil :alist) (ast/empty-list))
    (ast/set-statement nil (ast/term nil :results) (ast/empty-list))
    (ast/blockf
     nil
     (seq
      (reduce #(conj %1 (expand-map %2 :thePerson :alist)) [] adds)))
    (ast/count-of nil :dlist
                  (ast/term nil :alist))
    (ast/string-p1-reference nil :rstring "Records added = " :dlist)
    (ast/extend-list nil :results :rstring)
    (ast/save)
    (ast/return nil :results))))


(defn script
  [{:keys [action] :as directives}]
  (condp = action
    :get-individuals     (get-contacts directives)
    :delete-individual   (delete-contact directives)
    :add-individuals     (add-contacts directives)
    (str "Don't know how to complete " action)))
