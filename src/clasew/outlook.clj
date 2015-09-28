(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - Microsoft Outlook DSL"}
  clasew.outlook
  (:require [clasew.gen-as :as genas]
            [clasew.ast-emit :as ast]
            [clasew.identities :as ident]))


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
  (term-kw outlook-home-address :bad_error))

(defn outlook-mapset-business
  [term-kw]
  (term-kw outlook-business-address :bad_error))

(defn builder
  [fn & args]
  (apply fn args))

(defn build-tell
  [body]
  (ast/tell nil
            :outlook
            (ast/define-locals nil :results :cloop :ident)
            (ast/set-statement nil (ast/term nil :results) (ast/empty-list))
            body
            (ast/return nil :results)))


(defn- setaddressvalues
  "Given a list of vars, generate constructs to set a map value
  to a source value from another map"
  [token-fn mapvars targetmap sourcemap atypestring]
  (reduce #(conj
            %1
              (ast/record-value token-fn targetmap %2
                                (if (= %2 :address_type)
                                  (ast/scalar-value token-fn atypestring)
                                  (ast/value-of token-fn %2 sourcemap :cleanval))))
          [] mapvars))


(defn build-address
  [tkey args]
  (if (empty? args)
    nil
    (ast/block
     nil
     (ast/define-locals nil :aloop :hadd :badd)
     (ast/set-statement nil (ast/term nil :aloop) (ast/empty-list))
     (ast/set-empty-record nil :hadd args)
     (ast/set-empty-record nil :badd args)

     ;; Home address
     (apply (partial ast/block nil)
            (conj
             (setaddressvalues
              outlook-mapset-home
              args
              :hadd tkey "\"home\"")
             (ast/set-statement nil
                                (ast/eol-cmd nil :aloop nil)
                                (ast/term nil :hadd))))

     ;; Business address
     (apply (partial ast/block nil)
            (conj
             (setaddressvalues
              outlook-mapset-business
              args
              :badd tkey "\"work\"")
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
            (ast/record-value token-fn targetmap %2
                              (if (= %2 :email_type)
                                (ast/value-of-as-string token-fn %2 sourcemap :cleanval)
                                (ast/value-of token-fn %2 sourcemap :cleanval))))
          [] mapvars))

(defn build-emails
  [tkey args]
  (if (empty? args)
    nil
    (ast/block
     nil
     (ast/define-locals nil :elist :eadd)
     (ast/set-statement nil (ast/term nil :elist) (ast/empty-list))
     (apply (partial ast/for-in-expression
      nil
        (ast/term nil :eml)
        (ast/xofy-expression nil (ast/term nil :emails) (ast/term nil tkey)))
        (conj (seq (conj
                  (setemailvalues
                   nil
                   args
                   :eadd :eml)
                  (ast/set-statement nil
                                     (ast/eol-cmd nil :elist nil)
                                     (ast/term nil :eadd))))
            (ast/set-empty-record nil :eadd args)))
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
                                              (ast/null))
                               (ast/key-value nil
                                              (ast/key-term :number_type)
                                              (ast/null))))
       (ast/record-value nil g2 :number_value
                         (ast/value-of nil %2 fromval :cleanval))
       (ast/record-value nil g2 :number_type
                         (ast/scalar-value nil (str (%2 outlook-phones-types))))
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
     (ast/set-statement nil (ast/term nil :plist) (ast/empty-list))
     (setphonevalues tkey :plist)
     (ast/set-extend-record :ident :phone_list :plist))))

(defn build-individual
  [args lkw addr emls phns filt]
  (let [gets  (apply (partial ast/block nil)
                     (filter #(not (nil? %))
                             (conj (ident/setrecordvalues nil args :ident :cloop)
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
        (ast/set-empty-record nil :ident args)
        gets))))


(defn- get-contacts
  [{:keys [individuals filters emails addresses phones]}]
  (genas/ast-consume (builder (partial ast/block nil) ident/cleanval
                              (build-individual
                               individuals :cloop
                               (build-address :cloop (rest addresses))
                               (build-emails :cloop (rest emails))
                               (build-phones :cloop (rest phones))
                               filters))))

(defn- delete-contact
  [{:keys [filters]}]
  (genas/ast-consume
   (ast/tell
    outlook-mapset-core :outlook
    (ast/define-locals nil :results :dlist)
    (ast/set-statement nil (ast/term nil :results) (ast/empty-list))
    (ast/set-statement nil (ast/term nil :dlist)
                       (ast/filter-expression nil :contacts nil filters))
    (ast/delete-expression nil
                           (ast/filter-expression nil :contacts nil filters))
    (ast/set-statement nil
                       (ast/eol-cmd nil :results nil)
                       (ast/string-builder
                        nil
                        (ast/string-literal "Records deleted : ")
                        (ast/count-expression nil
                                              (ast/term nil :dlist))))
    (ast/save-expression)
    (ast/return nil :results))))

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

(defn- add-contacts
  "Adds new individuals to outlook contacts. Requires flattening
  addresses and phones to main map"
  [{:keys [adds]}]
  (genas/ast-consume
   (ast/tell
    outlook-mapset-core :outlook
    (ast/define-locals nil :results :alist :dlist :rstring)
    (ast/set-statement nil (ast/term nil :alist) (ast/empty-list))
    (ast/set-statement nil (ast/term nil :results) (ast/empty-list))
    (apply (partial ast/block outlook-mapset-core)
     (seq (reduce
           #(conj %1
                  (ast/set-statement
                   nil
                   (ast/eol-cmd nil :alist nil)
                   (ast/make-new-record nil :contact (flatten-map %2))))
           [] adds)))
    (ast/set-statement outlook-mapset-core
                       (ast/eol-cmd nil :results nil)
                       (ast/string-builder
                        nil
                        (ast/string-literal "Records added : ")
                        (ast/count-expression nil
                                              (ast/term nil :alist))))
    (ast/return nil :results))))

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

(defn- gen-subsets
  "Generate the additions for contained subsets, ie:
  set end of esets to {'record'}
  set email addresses of cloop to email addresses of cloop & esets"
  [tkw elistkw [subkey {:keys [filters sets adds]}]]
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
           (ast/xofy-expression
            nil
            (ast/term nil subkey)
            (ast/term nil tkw))
           (ast/append-object-expression
            nil
            elistkw))))))

(defn- filter-exception
  "Applies to filters not supportable by Office"
  [typekw {:keys [joins args]} sets]
  (if (or (not-empty joins) (> (count args) 2) (> (count (partition 2 sets)) 1)
          (and (= (count args) 2) (not (apply distinct? (map first args)))))
    (throw (Exception. (str "Filter expression for "
                            (name typekw)
                            " not supported in Outlook")))
    nil)
  )


(defn- phsetx
  [x k s]
  "Simplified set generation used for phone numbers"
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

(defn- phone-type1
  "When the filter has both type and number, it will perform
  a test on explicit type and set if match"
  [ckw args [n s]]
  (let [[_ fval _]  (first (filter #(= (first %) :number_value) args))
        [[x y]] (seq
                 (phone-reduce
                  {}
                  (reduce #(assoc %1 (first %2) (last %2)) {} args)))]
    (phifs
     {:joins '() :args (list (list x fval y ckw))}
     (phsetx x ckw s))))

(defn- phone-type2
  "When the filter only contained a 'type' it will be a direct set
  to the value for each phone type"
  [ckw [[t p v]] [n s]]
  (let [x (ffirst (phone-reduce {} {t v :number_value ""} ))]
    (phsetx x ckw s)))

(defn- phone-type3
  "When the filter only contains a 'number' it will test each phone
  type and set on match
  For each key in the phone-set generate a filter (xf) for key and
  number from the set (xs)"
  [ckw [[t p v]] [n s]]
  (let [xs (map #(phsetx %1 ckw s) (keys phone-set))
        xf (map #(assoc {} :joins '()
                   :args (list (list %1 ident/EQ v ckw)))
                (keys phone-set))]
    (ast/if-statement
     nil
     (phifs (first xf) (first xs))
     (map phelifs (rest xf) (rest xs)))))

(defn- eval-phone-type
  [{:keys [args]}]
  (cond
   (= (count args) 2) phone-type1
   (= (ffirst args) :number_type) phone-type2
   (= (ffirst args) :number_value) phone-type3))

(defn- phone-filter
  "Generates appropriate setters or if-then setters for
  phone updates"
  [ckw {:keys [joins args] :as filt} sets]
  (filter-exception :phones filt sets)
  ((eval-phone-type filt) ckw args sets))

(defn gen-ifs
  "Creates instructions for subset filtering. For phone
  and addresses it is inline to contact record. If emails
  a for-loop will be required"
  [ckw ifsets]
  (let [tb (reduce #(conj
                     %1
                     (condp = (first %2)
                       :phones (phone-filter ckw (:filters (second %2))
                                             (:sets (second %2)))))
                   [] ifsets)]
    (apply (partial ast/block nil) tb))
  )

(defn- redfilter
  [sset]
  (if (or (nil? sset) (and (nil? (:filters sset)) (empty? (:sets sset))))
    nil
    sset))

(defn- redistribute
  "Redistribute the subsets group
  1. if there are adds, redistributes to core sets
  2. if there are filters and sets then conj in remaining subsets"
  [mmap mkw smap]
  (let [red (seq (flatten-map {mkw (:adds smap)}))
        sset (redfilter (dissoc smap :adds))
        rmap (assoc mmap
               :sets (reduce
                      #(conj %1 (first %2) (second %2)) (:sets mmap) red))]
    (if sset
      (update-in rmap [:ifsets] conj [mkw sset])
      rmap)))

(defn- reduce-inline-subsets
  "Reduce through redistribution"
  [ckw {:keys [filters sets subsets] :as bblock}]
  (let [nblk (redistribute
              (redistribute
               (assoc bblock :subsets (ident/filter-forv :emails subsets))
               :phones (second (ident/filter-forv :phones subsets)))
              :addresses (second (ident/filter-forv :addresses subsets)))]
    (if (not-empty (:subsets nblk))
      nblk
      nblk)))

(defn- refactor-update
  [f block]
  (let [nmap (reduce-inline-subsets :cloop block)]
    (f nmap)))

(defn- update-contacts
  "Updates individuals or child values"
  [{:keys [filters sets subsets ifsets]}]
  (let [ssets nil
        ttl
   (ast/tell
    outlook-mapset-core :outlook
    (ast/define-locals nil :results :cloop :esets)
    (ast/set-statement nil (ast/term nil :results) (ast/empty-list))
    (ast/for-in-expression
     nil
     (ast/term nil :cloop)
     (if filters
       (ast/filter-expression
        nil :contacts nil filters)
       (ast/term nil :contacts))
     (ast/set-statement nil (ast/term nil :esets) (ast/empty-list))
     (apply (partial ast/block nil) (gen-sets :cloop sets))
     (gen-subsets :cloop :esets subsets)
     (gen-ifs :cloop ifsets))
    (ast/return nil :results))]
    (genas/ast-consume ttl)))

(defn script
  [{:keys [action] :as directives}]
  (condp = action
    :get-individuals     (get-contacts directives)
    :delete-individual   (delete-contact directives)
    :add-individuals     (add-contacts directives)
    :update-individual   (refactor-update update-contacts directives)
    (str "Don't know how to complete " action)))



