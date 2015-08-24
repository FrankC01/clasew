(ns
  ^{:author "Frank V. Castellucci"
      :doc "spike space"}
  clasew.sandbox
  (:require [clojure.pprint :refer :all]
            ;[clasew.contacts :as cont]
            ;[clasew.outlook :as outl]
            [clasew.ast-emit :as ast]
            [clasew.identities :as ident])
  )

;; Demonstrate record coercion
(def p pprint)


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

(defn- outlook-mapset-core
  [term-kw]
  (get outlook-identities term-kw term-kw))

(defn- outlook-mapset-home
  [term-kw]
  (term-kw outlook-home-address :bad_error))

(defn- outlook-mapset-business
  [term-kw]
  (term-kw outlook-business-address :bad_error))


(defn gen-setmapvalue
  "Given a list of vars, generate constructs to set a map value
  to a source value from another map"
  [token-fn mapvars targetmap sourcemap]
  (reduce #(conj %1 (ast/do-setmapvalue token-fn targetmap %2
                                    (ast/do-setvalueof token-fn %2 sourcemap :cleanval)))
          [] mapvars))

(def t0 (ast/do-block
         nil
         ast/cleanval
         (ast/do-tell nil "\"Microsoft Outlook\"" :results
                      (ast/do-setlocal nil :ident :results :cloop)
                      (ast/do-setlist nil :results)
                      (ast/do-filtered-repeat
                       outlook-mapset-core
                       :cloop
                       {:first_name "Frank"}
                       :contacts nil
                       (ast/do-setmap nil :ident ident/identity-standard)
                       (ast/do-blockf
                        nil
                        (conj
                         (gen-setmapvalue nil ident/identity-standard
                                          :ident :cloop)
                         ;; Address Management - Outlook
                         (ast/do-setlocal nil :aloop :hadd :badd)
                         (ast/do-setlist nil :aloop)
                         (ast/do-setmap nil :hadd ident/address-standard)
                         (ast/do-setmap nil :badd ident/address-standard)

                         ;; Home address
                         (ast/do-blockf
                          nil
                          (conj
                           (gen-setmapvalue
                            outlook-mapset-home
                            ident/address-standard
                            :hadd :cloop)
                           (ast/do-setlist-end nil :aloop :hadd)))

                         ;; Business address
                         (ast/do-blockf
                          nil
                          (conj
                           (gen-setmapvalue
                            outlook-mapset-business
                            ident/address-standard
                            :badd :cloop)
                           (ast/do-setlist-end nil :aloop :badd)))
                         (ast/do-setmapextent nil :ident :address_list :aloop)
                         (ast/do-setlist-end nil :results :ident)))))))

;(p t0)

;; Replace in gen-as

(declare ast-consume)

(def ^:dynamic *lookup-fn*)

(defn nil-handler
  [expression]
  (p expression)
  (str expression " not handled.\n"))

(defn tell-handler
  [expression]
  (let   [body (apply str (map ast-consume (:expressions expression)))]
  (str "tell application " (:target expression) "\n" body
       "return " (name (:return expression))"\nend tell\n")))

(defn routine-handler
  [{:keys [routine-name parameters expressions]}]
  (let [body (apply str (map ast-consume expressions))]
  (str "on " (name routine-name) "(" (name parameters)")\n"
       body
       "end "(name routine-name)"\n")))

(defn ifthen-handler
  [{:keys [test-value predicate operand expressions]}]
  (str "if " (name test-value) " is"
       (*lookup-fn* predicate)
       (*lookup-fn* operand) " then \n"
       (apply str (map ast-consume expressions))
       "end if\n"
       ))


(defn return-handler
  [{:keys [return-val]}]
  (str "return " (name return-val)"\n"))

(defn local-handler
  [expression]
  (str "local "
       (apply str
              (interpose ","
                         (map name (:local-terms expression)))) "\n"))

(defn block-handler
  [expression]
  (apply str (map ast-consume (:expressions expression))))


(defn setassign-handler
  [{:keys [setvalue setvalue-of]}]
  (str "set "(name setvalue)" to "(name setvalue-of)"\n"))

(defn setlist-handler
  [expression]
  (str "set " (name (:set expression)) " to {}\n"))

(defn setlistend-handler
  [expression]
  (str "set end of " (name (:set expression))
       " to " (name (:to expression)) "\n"))

(defn setmap-handler
  [expression]
  (str "set " (name (:set expression)) " to {"
       (apply str
              (interpose ","
                         (map #(str (name %)":null")(:set-to expression)))) "}\n"))

(defn setmapvalueof-handler
  "The value requires a name resolution lookup"
  [{:keys [type value from apply-function] :as to-map}]
  (if apply-function
    (str " to my " (name apply-function) "(" (*lookup-fn* value) " of " (name from) ")")
  (str " to " (*lookup-fn* value) " of " (name from) )))

(defn setmapvalue-handler
  "Results in 'set x of y to z'"
  [{:keys [mapvalue ofmap to]}]
  (str "set " (name mapvalue) " of " (name ofmap) (ast-consume to)"\n"))

(defn setpropertiesof-handler
  "Results in 'set x to properties of y'"
  [{:keys [value properties-of]}]
  (str "set " (name value) " to properties of " (name properties-of) "\n"))

(defn setmapextend-handler
  [{:keys [target-map value keywrd]}]
  (str "set " (name target-map) " to " (name target-map) " & {"(name keywrd)":" (name value)"}\n")
  )

(defn filter-reduce
  "Converts filter map to individual search criteria. Multiple are
  conjoined (e.g. and'ed)"
  [filter-map]
  (println "filter-map" filter-map)
  (apply str
         (interpose " and "
                    (reduce-kv
                     #(conj %1 (str (*lookup-fn* %2) " contains " \" %3 \"))
                     []
                     filter-map))))

(defn filter-handler
  [{:keys [source value soure-of user-filter]}]
  (let [d (filter-reduce user-filter)]
  (str "set " (name value) " to " (name source) " whose (" d ")\n")))

(defn repeat-handler
  [{:keys [source source-of expressions iteration-var]}]
  (let [body (apply str (map ast-consume expressions))]
  (str "repeat with " (name iteration-var)
       " in " (name source) (if source-of
                              (str " of " (name source-of) " \n") "\n")
       body
       "end repeat\n")))

(defn filtered-repeat-handler
  "Expands to setting local values to filter results, setting up a loop and
  including a 'get properties of' the iteration-var"
  [{:keys [token-fn
           source soure-of
           user-filter
           filter-result expressions
           iteration-var property-var] :as expression}]
  (let [lcls (ast/do-setlocal nil filter-result)
        flt  (ast/do-setfilter token-fn filter-result source soure-of user-filter)
        rep  (ast/do-repeatf token-fn iteration-var filter-result nil
                        (conj expressions
                        (ast/do-setpropertiesof nil property-var iteration-var)))
        expres (conj (conj (conj '() rep) flt) lcls)
        body (apply str (map ast-consume expres))]
  (str body)))


(def ast-jump "Jump Table for AST Expression"
  {:do-routine          routine-handler
   :do-ifthen           ifthen-handler
   :do-return           return-handler
   :do-tell             tell-handler
   :do-block            block-handler
   :do-setlocal         local-handler
   :do-setassign        setassign-handler
   :do-setlist          setlist-handler
   :do-setlist-end      setlistend-handler
   :do-setmap           setmap-handler
   :do-setmapvalue      setmapvalue-handler
   :do-setvalueof       setmapvalueof-handler
   :do-setpropertiesof  setpropertiesof-handler
   :do-setmapextent     setmapextend-handler
   :do-setfilter        filter-handler
   :do-repeat           repeat-handler
   :do-filtered-repeat  filtered-repeat-handler
   })


(defn ast-consume
  [{:keys [type token-fn] :as block}]
  (if token-fn
    (binding [*lookup-fn* token-fn]
      ((get ast-jump type nil-handler) block))
    ((get ast-jump type nil-handler) block)))


;(println (time (ast-consume t0)))

(def stnd-tell
  (ast/do-tell nil "\"Microsoft Outlook\"" :results
                      (ast/do-setlocal nil :results :cloop :ident)
                      (ast/do-setlist nil :results)))

(defn builder
  [fn & args]
  (apply fn args))

(defn build-tell
  [body]
  (ast/do-tell nil "\"Microsoft Outlook\"" :results
               (ast/do-setlocal nil :results :cloop :ident)
               (ast/do-setlist nil :results)
               body))


(defn address-details
  [tkey args]
  (ast/do-block
   (ast/do-setlocal nil :aloop :hadd :badd)
   (ast/do-setlist nil :aloop)
   (ast/do-setmap nil :hadd args)
   (ast/do-setmap nil :badd args)

   ;; Home address
   (ast/do-blockf
    nil
    (conj
     (gen-setmapvalue
      outlook-mapset-home
      args
      :hadd tkey)
     (ast/do-setlist-end nil :aloop :hadd)))

   ;; Business address
   (ast/do-blockf
    nil
    (conj
     (gen-setmapvalue
      outlook-mapset-business
      args
      :badd tkey)
     (ast/do-setlist-end nil :aloop :badd)))
   (ast/do-setmapextent nil :ident :address_list :aloop)))


(defn build-address
  [tkey args]
  (let [addr (first (filter #(and (vector? %) (= (first %) :addresses)) args))]
    (println addr (count addr))
    (if (nil? addr)
      addr
      (address-details tkey (rest addr)))))

(defn build-indy
  [args lkw addr [filt]]
  (let [iargs (if (empty? args) ident/identity-standard args)
        gets  (apply (partial ast/do-block nil)
                     (filter #(not (nil? %))
                             (conj (gen-setmapvalue nil iargs :ident :cloop)
                                   (if addr addr)
                                   (ast/do-setlist-end nil :results :ident))))]
    (build-tell
     (if (not-empty filt)
       (ast/do-filtered-repeat outlook-mapset-core lkw filt :contacts nil
                               (ast/do-setmap nil :ident iargs)
                               gets)
       (ast/do-repeat outlook-mapset-core lkw :contacts nil
                      (ast/do-setmap nil :ident iargs)
                      gets)))))


(defn faddy
  [& args]
  (into [:addresses] (if (empty? args) ident/address-standard args)))

(defn findy
  [& args]
  (let [emls (filter #(and (vector? %) (= (first %) :emails)) args)
        ind  (build-indy (filter keyword? args)
                         :cloop
                         (build-address :cloop args)
                         (filter map? args))]
    (builder (partial ast/do-block nil) ast/cleanval ind)))

;(println (ast-consume (findy :full_name (faddy :zip_code) {:first_name "Frank"})))
;(println (ast-consume (findy (faddy) )))

