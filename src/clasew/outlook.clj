(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - Microsoft Outlook DSL"}
  clasew.outlook
  (:require [clasew.gen-as :as genas]
            [clasew.ast-emit :as ast]
            [clasew.ast-utils :as astu]
            [clojure.core.reducers :as r]
            [clasew.identities :as ident]
            [clasew.ident-utils :as utils]
            [clasew.outlook-m :as outm]))


(def ^:private phone-set
  {
   :default_phone            "phone",
   :home_phone               "home phone number"
   :home2_phone              "other home phone number"
   :home_fax                 "home fax number"
   :business_phone           "business phone number"
   :business2_phone          "other business phone number"
   :business_fax             "business fax number"
   :pager                    "pager number"
   :mobile                   "mobile number"
   })

(def ^:private address-set
  {
   :home_zip                 "home zip",
   :home_city                "home city",
   :home_street_address      "home street address",
   :home_country             "home country",
   :home_state               "home state",
   :business_zip             "business zip",
   :business_city            "business city",
   :business_street_address  "business street address",
   :business_country         "business country",
   :business_state           "business state"
   })

(def ^:private outlook-identities
  (merge {
   :name_suffix              "suffix",
   :full_name                "display name",
   :first_name               "first name",
   :middle_name,             "middle name",
   :last_name                "last name",
   :primary_company          "company",
   :primary_title            "job title",
   :primary_department       "department",
   :city_name                "city",
   :street_name              "street",
   :zip_code                 "zip",
   :country_name             "country",
   :state_name               "state",
   :contacts                 "contacts",
   :emails                   "email addresses",
   :email_individual         "contact email address",
   :email_address            "address",
   :email_type               "type"
    } phone-set address-set))


(defn outlook-mapset-core
  [term-kw]
  (get outlook-identities term-kw (name term-kw)))

(def ^:private outlook-home-address
  {
   :zip_code         "home zip",
   :city_name        "home city",
   :street_name      "home street address",
   :country_name     "home country",
   :state_name       "home state",
   :address_type     "id"
   })


(def ^:private outlook-business-address
  {
   :zip_code         "business zip",
   :city_name        "business city",
   :street_name      "business street address",
   :country_name     "business country",
   :state_name       "business state",
   :address_type     "id"
   })

(defn outlook-mapset-home
  [term-kw]
  (term-kw outlook-home-address (name term-kw)))

(defn outlook-mapset-business
  [term-kw]
  (term-kw outlook-business-address (name term-kw)))

;;
;; Get functions
;;

(defn builder
  [fn & args]
  (apply fn args))

(defn build-tell
  [body]
  (ast/tell nil
            :outlook
            (ast/define-locals nil :results :cloop :ident)
            (ast/set-statement nil (ast/term nil :results) ast/empty-list)
            body
            (ast/return nil :results)))



(defn- setaddressvalues
  "Given a list of vars, generate constructs to set a map value
  to a source value from another map"
  [token-fn mapvars targetmap sourcemap atypestring]
  (reduce #(conj
            %1
            (ast/set-statement token-fn
                               (ast/xofy-expression
                                (fn [term-kw] (name term-kw))
                                (ast/term nil %2)
                                (ast/term nil targetmap))
                               (if (not= %2 :address_type)
                                (ast/routine-call
                                 token-fn
                                 (ast/term nil :cleanval)
                                 (ast/xofy-expression
                                  nil
                                  (ast/term nil %2)
                                  (ast/term nil sourcemap)))
                                 (ast/string-literal atypestring))))
          [] mapvars))


(defn build-address
  [tkey args]
  (if (empty? args)
    nil
    (ast/block
     nil
     (ast/define-locals nil :aloop :hadd :badd)
     (ast/set-statement nil (ast/term nil :aloop) ast/empty-list)
     (ast/set-empty-record nil :hadd args {:ktfn ast/key-term-nl})
     (ast/set-empty-record nil :badd args {:ktfn ast/key-term-nl})

     ;; Home address
     (apply (partial ast/block nil)
            (conj
             (setaddressvalues
              outlook-mapset-home
              args
              :hadd tkey "home")
             (ast/set-statement nil
                                (ast/eol-cmd nil :aloop nil)
                                (ast/term nil :hadd))))

     ;; Business address
     (apply (partial ast/block nil)
            (conj
             (setaddressvalues
              outlook-mapset-business
              args
              :badd tkey "work")
             (ast/set-statement nil
                                (ast/eol-cmd nil :aloop nil)
                                (ast/term nil :badd))))
     (ast/set-extend-record :ident :address_list :aloop))))

(defn- setemailvalues
  "Given a list of vars, generate constructs to set a map value
  to a source value from another map"
  [token-fn mapvars targetmap sourcemap]
  (reduce #(conj
            %1
            (ast/set-statement token-fn
                               (ast/xofy-expression
                                nil
                                (ast/term-nl %2)
                                (ast/term nil targetmap))
                               (ast/expression
                                nil
                                (ast/routine-call
                                 nil
                                 (ast/term nil :cleanval)
                                 (ast/xofy-expression
                                  nil
                                  (ast/term nil %2)
                                  (ast/term nil sourcemap)))
                                (if (= %2 :email_type)
                                  ast/as-string
                                  ast/noop))))
          [] mapvars))

(defn build-emails
  [tkey args]
  (if (empty? args)
    nil
    (ast/block
     nil
     (ast/define-locals nil :elist :eadd)
     (ast/set-statement nil (ast/term nil :elist) ast/empty-list)
     (apply (partial ast/for-in-expression
      nil
        (ast/term nil :eml)
                     (ast/get-statement
                      nil
        (ast/xofy-expression
         nil
         (ast/term nil :emails)
         (ast/term nil tkey))))
        (conj (seq (conj
                  (setemailvalues
                   nil
                   args
                   :eadd :eml)
                  (ast/set-statement nil
                                     (ast/eol-cmd nil :elist nil)
                                     (ast/term nil :eadd))))
            (ast/set-empty-record nil :eadd args {:ktfn ast/key-term-nl})))
     (ast/set-extend-record :ident :email_list :elist))))


(def ^:private outlook-phones-types
  {
   :default_phone    "\"default\"",
   :home_phone       "\"home\""
   :home2_phone      "\"home\""
   :home_fax         "\"home fax\""
   :business_phone   "\"work\""
   :business2_phone  "\"work\""
   :business_fax     "\"work fax\""
   :pager            "\"pager\""
   :mobile           "\"mobile\""
   })


(defn- setphonevalues
  "Generates the set statements for retrieving values from application"
  [fromval reslist]
  (apply (partial ast/block nil)
   (reduce
    #(conj
      %1
      (let [g2 (gensym)]
      (ast/block
       nil
       (ast/define-locals nil g2)
       (ast/set-statement
        nil (ast/term nil g2)
        (ast/record-definition nil
                               (ast/key-value nil
                                              (ast/key-term :number_value)
                                              ast/null)
                               (ast/key-value nil
                                              (ast/key-term :number_type)
                                              ast/null)))
       (ast/set-statement nil
                          (ast/xofy-expression
                           nil
                           (ast/term nil :number_value)
                           (ast/term nil g2))
                          (ast/routine-call
                           nil
                           (ast/term nil :cleanval)
                           (ast/xofy-expression
                            nil
                            (ast/term nil %2)
                            (ast/term nil fromval))))
       (ast/set-statement nil
                          (ast/xofy-expression
                           nil
                           (ast/term nil :number_type)
                           (ast/term nil g2))
                          (ast/term nil (str (%2 outlook-phones-types))))
       (ast/set-statement
        nil
        (ast/eol-cmd nil reslist nil)
        (ast/term nil g2))
       )))
    [] (keys phone-set))))


(defn build-phones
  [tkey args]
  (if (empty? args)
    nil
    (ast/block
     nil
     (ast/define-locals nil :plist)
     (ast/set-statement nil (ast/term nil :plist) ast/empty-list)
     (setphonevalues tkey :plist)
     (ast/set-extend-record :ident :phone_list :plist))))

(defn build-individual
  [args lkw addr emls phns filt]
  (let [gets  (apply (partial ast/block nil)
                     (filter #(not (nil? %))
                             (conj (ast/setrecordvalues nil args :ident :cloop)
                                   addr emls  phns
                                   (ast/set-statement
                                    outlook-mapset-core
                                    (ast/eol-cmd outlook-mapset-core :results nil)
                                    (ast/term nil :ident)))))]
    (build-tell
       (ast/for-in-expression
        outlook-mapset-core
        (ast/term nil lkw)
        (if (empty? filt)
          (ast/term nil :contacts)
          (ast/where-filter nil
                            (ast/term nil :contacts)
                            filt))
        (ast/set-empty-record nil :ident args {:ktfn ast/key-term-nl})
        gets))))


(defn- get-contacts
  [{:keys [individuals filters emails addresses phones]}]
  (genas/ast-consume (builder (partial ast/block nil) astu/cleanval
                              (build-individual
                               individuals :cloop
                               (build-address :cloop (rest addresses))
                               (build-emails :cloop (rest emails))
                               (build-phones :cloop (rest phones))
                               filters))))
;;
;; Delete function
;;

(defn- delete-contact
  [{:keys [filters]}]
  (genas/ast-consume
   (ast/tell
    outlook-mapset-core :outlook
    (ast/define-locals nil :results :dlist :dloop)
    (ast/set-statement nil (ast/term nil :results) ast/empty-list)
    (ast/set-statement nil (ast/term nil :dlist)
                       (ast/where-filter nil
                                         (ast/term nil :contacts)
                                         filters))
    (ast/for-in-expression
     nil
     (ast/term nil :dloop) (ast/term nil :dlist)
     (ast/expression nil ast/delete (ast/term nil :dloop)))
    (ast/set-result-msg-with-count "Records deleted :" :results :dlist)
    (ast/save-statement)
    (ast/return nil :results))))

;;
;; Add functions
;;

(defn kwmaker
  [strw]
  (keyword (clojure.string/replace strw #" " "_")))

(defn phone-reduce
  [a m]
  (if (nil? (:number_value m))
    a
    (assoc a
      (condp = (kwmaker (:number_type m))
        :default :default_phone
        :home (if (:home_phone a)
                :home2_phone
                :home_phone)
        :home_phone :home_phone
        :home_fax  :home_fax
        :work (if (:business_phone a)
                :business2_phone
                :business_phone)
        :work_fax :business_fax
        :mobile :mobile
        :pager  :pager
        :unknown
        )
      (:number_value m))))

(defn- flatten-phones
  [coll]
  (if (nil? coll)
    nil
    (reduce phone-reduce {} coll)))

(defn- address-reduce
  [a m]
  (if (nil? (:address_type m))
    a
    (merge a (let [kw (kwmaker (:address_type m))
          lu (if (= kw :home) outlook-mapset-home outlook-mapset-business)
          m1 (dissoc m :address_type)]
      (reduce-kv #(assoc %1 (kwmaker (lu %2)) %3) {} m1)))))

(defn- flatten-addresses
  [coll]
  (if (nil? coll)
    nil
    (reduce address-reduce {} coll)))

(defn- flatten-map
  [imap]
  (merge (dissoc imap :addresses :phones)
         (flatten-phones (get imap :phones nil))
         (flatten-addresses (get imap :addresses nil))))

(defn- add-record-definitions
  "Creates the record definition that creates the types and
  subtypes and puts result in a list collection for returning
  success information and count of record creations to user"
  [imap]
  (let [fimap (flatten-map imap)]
    (apply (partial ast/record-definition nil)
           (seq
            (reduce
             #(conj
               %1
               (if (coll? (second %2))
                 (ast/key-value
                  nil
                  (ast/key-term (first %2))
                  (ast/list-of nil (map add-record-definitions (second %2))))
                 (ast/key-value
                  nil
                  (ast/key-term (first %2))
                  (if (= (first %2) :email_type)
                    (ast/symbol-literal (second %2))
                    (ast/string-literal (second %2))))))
             [] fimap)))))

(defn- add-contacts
  "Adds new individuals to outlook contacts. Requires flattening
  addresses and phones to main map"
  [{:keys [adds]}]
  (genas/ast-consume
   (ast/tell
    outlook-mapset-core :outlook
    (ast/define-locals nil :results :alist :dlist :rstring)
    (ast/set-statement nil (ast/term nil :alist) ast/empty-list)
    (ast/set-statement nil (ast/term nil :results) ast/empty-list)
    (apply (partial ast/block outlook-mapset-core)
     (seq (reduce
           #(conj %1
                  (ast/set-statement
                   nil
                   (ast/eol-cmd nil :alist nil)
                   (ast/make-new
                    nil
                    (ast/term nil :contact)
                    ast/with-properties
                    (add-record-definitions %2))))
           [] adds)))
    (ast/set-result-msg-with-count "Records added :" :results :alist)
    (ast/return nil :results))))

;;
;; Update functions
;;

;; Reducible support

(def nepred "Not Equal Predicate: used for filtering emptry record in collection"
  #(or (not-empty (:sets (second %))) (not (nil? (:filters (second %))))))

(def neflt  "Filter empty records based on nepred predicate"
  (r/filter nepred))

(def neflt1 "Filter empty record based on empty collection"
  (r/filter #(not-empty (second %))))

(def eflt   "Filter return of email updates only"
  (r/filter #(and (vector? %) (= (first %) :emails))))

(def pflt   "Filter return of phone updates only"
  (r/filter #(and (vector? %) (= (first %) :phones))))

(def aflt   "Filter return of address updates only"
  (r/filter #(and (vector? %) (= (first %) :addresses))))

(def !eflt  "Filter return of non-email updates"
  (r/filter #(not (and (vector? %) (= (first %) :emails)))))


(def mscnd "Retrieve only 'second' item in record"
  (r/map second))

(def stripadd "Set 'adds' in update record to empty list"
  (r/map #(vector (first %) (assoc (second %) :adds '()))))

(def getadd   "Retrieve adds and append to qualified vector"
  (r/map #(vector (first %) (:adds (second %)))))

(defn convert-phone
  "Lookup phone type and conjoin with value"
  [pm]
  (flatten (seq (reduce phone-reduce {} pm))))

(defn convert-address
  "Lookup address type and conjoin with value"
  [am]
  (flatten (seq (reduce address-reduce {} am))))

(def resolve-add "Send record to get refitted for targets specific to outlook"
  (r/map #(if (= (first %) :phones)
            (convert-phone (second %))
            (if (= (first %) :addresses)
              (convert-address (second %))
              (throw (Exception. (str (first %) " not known")))))))

(def pipe-emails "Pipe to retrieve email update directives only"
  (comp mscnd eflt))

(def pipe-strip-adds "Pipe to strip adds and empty records (phone,address)"
  (comp neflt stripadd !eflt))

(def pipe-adds-to-sets
  (comp resolve-add neflt1 getadd !eflt))

(defn refactor-update
  "Take base map
  Assign email updates to subsets
  Assign striped phone and address adds to ifsets
  Convert adds to sets in main block"
  [{:keys [subsets] :as m}]
  (r/reduce
   #(update-in %1 [:sets] into %2)
   (merge m
          {:subsets (into [] (pipe-emails subsets))
           :ifsets  (into [] (pipe-strip-adds subsets))})
   (pipe-adds-to-sets subsets)))


(defn- gen-sets
  "Generate the value set to's in process"
  [ckw setters]
  (let [smap (reduce #(assoc %1 (first %2) (second %2)) {} (partition 2 setters))]
  (reduce-kv #(conj %1
                 (ast/set-statement
                  nil
                  (ast/xofy-expression
                   nil
                   (ast/term nil %2) (ast/term nil ckw))
                  (ast/string-literal %3))) [] smap)))

(defn- gen-records
  [emap]
  (reduce-kv
   #(update-in %1 [:expressions] conj
               (ast/key-value
                nil
                (ast/key-term %2)
                (if (= %2 :email_type)
                  (ast/symbol-literal %3)
                  (ast/string-literal %3))))
   (ast/record-definition nil) emap))


(defn- phone-filter-exception
  "Applies to phone filters not supportable by Office"
  [typekw {:keys [joins args]} sets]
  (if (or (not-empty joins) (> (count args) 2) (> (count (partition 2 sets)) 1)
          (and (= (count args) 2) (not (apply distinct? (map first args)))))
    (throw (Exception. (str "Filter expression for "
                            (name typekw)
                            " not supported in Outlook")))
    nil))

(defn- address-filter-exception
  "Applies to address filters not supportable by Office"
  [typekw {:keys [joins args]} sets]
  (if (or (not-empty joins) (> (count args) 2) (> (count (partition 2 sets)) 1)
          (and (= (count args) 1) (= (ffirst args) :address_type))
          (and (= (count args) 2) (not (apply distinct? (map first args)))))
    (throw (Exception. (str "Filter expression for "
                            (name typekw)
                            " not supported in Outlook")))
    nil))

;
; Simplified expansion functions
;

(defn- phsetx
  [x k s]
  "Simplified set generation used for phone numbers and
  addersses"
  (ast/set-statement
      nil
      (ast/xofy-expression
       nil
       (ast/term nil x) (ast/term nil k))
      (ast/string-literal s)))

(defn phifs
  "Generate an if-expression with filter and expressions"
  [filt setast]
  (ast/if-expression nil filt setast))

(defn phelifs
  "Generate an else-if-expression with filter and expressions"
  [filt setast]
  (ast/else-if-expression nil (phifs filt setast)))

;
; Common update handlers for phones and addresses
;

(defn- update-type1
  "Generator for having a type and a value in the filter
  f is filter predicate
  tr-fn is type reduce function"
  [ckw args [n s] f tr-fn]
  (let [[_ fval _]  (first (filter f args))
        [[x y]] (seq
                 (tr-fn
                  {}
                  (reduce #(assoc %1 (first %2) (last %2)) {} args)))]
    (ast/if-statement
     nil
     (phifs
      {:joins '() :args (list (list x fval y ckw))}
      (phsetx x ckw s))
     nil)))

(defn- update-type2
  "Generator if/then having a test on each value of a set"
  [ckw v s lu pred]
  (let [xs (map #(phsetx %1 ckw s) lu)
        xf (map #(assoc {} :joins '()
                   :args (list (list %1 pred v ckw)))
                lu)]
    (ast/if-statement
     nil
     (phifs (first xf) (first xs))
     (map phelifs (rest xf) (rest xs)))))

(defn- phone-filter
  "Generates appropriate setters or if-then setters for
  phone updates"
  [ckw {:keys [joins args] :as filt} sets]
  (phone-filter-exception :phones filt sets)
  (let [[[t p v]] args
        [n s] sets]
    (cond
     (= (count args) 2)
       (update-type1 ckw args sets #(= (first %) :number_value) phone-reduce)
     (= (ffirst args) :number_type)
       (let [x (ffirst (phone-reduce {} {t v :number_value ""} ))]
         (phsetx x ckw s))
     (= (ffirst args) :number_value)
       (update-type2 ckw v s (keys phone-set) p))))

(def ^:private address-stubs
  {:zip_code         "zip",
   :city_name        "city",
   :street_name      "street address",
   :country_name     "country",
   :state_name       "state"})

(defn- address-filter
  [ckw {:keys [joins args] :as filt} sets]
  (address-filter-exception :addresses filt sets)
  (if (= (count args) 2)
    (update-type1 ckw args sets #(not= (first %) :address_type) address-reduce)
    (let [[[t p v]] args
          [n s] sets
          k (t address-stubs)
          xs (list (keyword (str "home_" k)) (keyword (str "business_" k)))]
      (update-type2 ckw v s xs p))))

(defn- gen-ifs
  "Creates instructions for subset filtering. For phone
  and addresses it is inline to contact record."
  [ckw ifsets]
  (let [tb (reduce
            #(if (not (nil? (second %2)))
               (conj
                %1
                (condp = (first %2)
                  :phones (phone-filter ckw (:filters (second %2))
                                        (:sets (second %2)))
                  :addresses (address-filter ckw (:filters (second %2))
                                             (:sets (second %2))) ))
               %1)
            [] ifsets)]
    (apply (partial ast/block nil) tb)))

(defn- gen-subset-adds
  "Generate the additions for contained subsets, ie:
  set email addresses of cloop to email addresses of cloop & esets"
  [tkw elistkw [subkey {:keys [adds]}]]
  (if (empty? adds)
    (ast/block nil)
    (ast/block
     nil
     (ast/define-locals nil elistkw)
     (ast/set-statement nil (ast/term nil elistkw) ast/empty-list)
     (apply (partial ast/block nil)
            (conj (reduce #(conj %1
                                 (ast/set-statement
                                  nil
                                  (ast/eol-cmd nil elistkw nil)
                                  (gen-records %2))) [] adds)

                  (ast/set-statement
                   nil
                   (ast/xofy-expression
                    nil
                    (ast/term nil subkey) (ast/term nil tkw))
                   (ast/block
                    nil
                    (ast/precedence
                     nil
                     (ast/expression
                      nil
                      (ast/xofy-expression
                       nil
                       (ast/term nil subkey)
                       (ast/term nil tkw))))
                    (ast/append-object-expression
                     nil
                     elistkw))))))))


(defn- clean-email-type
  [loopkw {:keys [filters sets]}]
  (assoc {}
    :filters {:joins (:joins filters)
              :args (map #(if (= (first %) :email_type)
                            (list (first %) (second %) (symbol (last %)) loopkw)
                            (list (first %) (second %) (last %) loopkw))
                         (:args filters))}
    :sets sets))

(defn- email-subset-reduce
  "Reducer for email subsets"
  [acc block]
  (let [cb (clean-email-type :sloop block)]
    (conj
     acc
     (ast/block`
      nil
      (if (and (:filters cb) (not-empty (:sets cb)))
        (utils/update-if-filter-block :etmp :sloop :cloop :emails cb nil)
        ast/noop)
      (if (not-empty (:adds cb))
        (gen-subset-adds :cloop :esets [:emails block])
        ast/noop)))))

(defn- gen-email-subsets
  "Creates the email adds and filtered sets if applicable"
  [subsets]
  (if (not-empty subsets)
    (apply (partial ast/block nil)
           (reduce email-subset-reduce [] subsets))
    (ast/block nil ast/noop)))

(defn update-contacts
  "Updates individuals or child values allowing for the addition of
  subtype (phone, email, address) creations as part of the update"
  [{:keys [filters sets subsets ifsets]}]
  (genas/ast-consume
        (ast/tell
         outlook-mapset-core :outlook
         (ast/define-locals nil :results :cloop)
         (ast/set-statement nil (ast/term nil :results) ast/empty-list)
         (ast/for-in-expression
          nil
          (ast/term nil :cloop)
          (if filters
            (ast/where-filter nil (ast/term nil :contacts) filters)
            (ast/term nil :contacts))
          (gen-email-subsets subsets)
          (gen-ifs :cloop ifsets)
          (apply (partial ast/block nil) (gen-sets :cloop sets)))
         (ast/set-statement
          nil
          (ast/eol-cmd nil :results nil)
          (ast/string-literal "Update successful"))
         (ast/return nil :results))))

(defn script
  [{:keys [action] :as directives}]
  (condp = action
    :get-individuals     (get-contacts directives)
    :delete-individual   (delete-contact directives)
    :add-individuals     (add-contacts directives)
    :update-individual   (update-contacts (refactor-update  directives))
    :get-messages        (outm/get-messages directives)
    :send-message        (outm/send-message directives)
    (str "Don't know how to complete " action)))



