(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - Experimental AppleScript Generator"}
  clasew.gen-as
  (:require [clasew.utility :as util]
            [clasew.ast-emit :as ast]
            [clojure.java.io :as io]
            [clojure.walk :as w]))


(declare ast-consume)

(def ^:dynamic *lookup-fn*)

(defn nil-handler
  [expression]
  (str expression " not handled.\n"))

(defn tell-handler
  [expression]
  (let   [body (apply str (map ast-consume (:expressions expression)))]
  (str "tell application " (:target expression) "\n" body "end tell\n")))

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


(defn assign-handler
  [{:keys [setvalue setvalue-of]}]
  (str "set "(name setvalue)" to "(name setvalue-of)"\n"))

(defn scalar-handler
  [{:keys [to-value]}]
  (str " to " to-value))

(defn term-handler
  [{:keys [to-value]}]
  (name to-value))

(defn count-of-handler
  [{:keys [set-target expressions]}]
  (str "set "(name set-target)" to count of " (ast-consume expressions) "\n"))

(defn sp1r-handler
  [{:keys [set-target string-0 ref-1]}]
  (str "set "(name set-target)" to \"" string-0"\" & " (name ref-1)"\n"))

(defn list-handler
  [expression]
  (str "set " (name (:set expression)) " to {}\n"))

(defn extendlist-handler
  [expression]
  (str "set end of " (name (:set expression))
       " to " (name (:to expression)) "\n"))

(defn extendlist-expression-handler
  [{:keys [target to-expression]}]
  (str "set end of "(name target) " to "(ast-consume to-expression)))

(defn record-handler
  [expression]
  (str "set " (name (:set expression)) " to {"
       (apply str
              (interpose ","
                         (map #(str (name %)":null")(:set-to expression)))) "}\n"))

(defn valueof-handler
  "The value requires a name resolution lookup"
  [{:keys [type value from apply-function] :as to-map}]
  (if apply-function
    (str " to my " (name apply-function) "(" (*lookup-fn* value) " of " (name from) ")")
  (str " to " (*lookup-fn* value) " of " (name from) )))

(defn valueof-asstring-handler
  [vo-map]
  (str (valueof-handler vo-map) " as string"))

(defn recordvalue-handler
  "Results in 'set x of y to z'"
  [{:keys [mapvalue ofmap to]}]
  (str "set " (name mapvalue) " of " (name ofmap) (ast-consume to)"\n"))

(defn propertiesof-handler
  "Results in 'set x to properties of y'"
  [{:keys [value properties-of]}]
  (str "set " (name value) " to properties of " (name properties-of) "\n"))

(defn extendrecord-handler
  [{:keys [target-map value keywrd]}]
  (str "set " (name target-map) " to " (name target-map) " & {"(name keywrd)":" (name value)"}\n")
  )

(defn filter-reduce
  "Converts filter map to individual search criteria. Multiple are
  conjoined (e.g. and'ed)"
  [filter-map]
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

(defn filter-expression-handler
  [{:keys [source source-of user-filter]}]
  (str "("(*lookup-fn* source) " whose " (filter-reduce user-filter) ")"))


(defn filtered-delete-handler
  [{:keys [user-filter record-set]}]
  (str "delete ("(*lookup-fn* record-set) " whose " (filter-reduce user-filter) ")\n")
  )

(defn repeat-handler
  [{:keys [source source-of expressions iteration-var] :as block}]
  (let [body (apply str (map ast-consume expressions))]
  (str "repeat with " (name iteration-var)
       " in (get " (*lookup-fn* source) (if source-of
                              (str " of " (name source-of) ")\n") ")\n")
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
  (let [lcls (ast/define-locals nil filter-result)
        flt  (ast/from-filter token-fn filter-result source soure-of user-filter)
        rep  (ast/repeat-loopf token-fn iteration-var filter-result nil
                        (conj expressions
                        (ast/properties-of nil property-var iteration-var)))
        expres (conj (conj (conj '() rep) flt) lcls)
        body (apply str (map ast-consume expres))]
  (str body)))

(defn quit-handler
  [args]
  (str (name :quit)"\n"))

(defn save-handler
  [args]
  (str (name :save)"\n"))


(defn- symboltype
  [k v]
  (if (or (= k :email_type) (= k :number_type) (= k :address_type))
    (symbol v)
    v))

(defn- inner-records
  [rmap]
  (let [i (interpose "," rmap)]
  (loop [x (first i)
         z (rest i)
        y []]
    (if (nil? x)
      (symbol (str "{" (apply str y) "}"))
      (recur (first z) (rest z) (conj y x))))))

(defn- reduce-record
  [property-map]
  (reduce-kv #(assoc %1 (symbol (str (*lookup-fn* %2) ":"))
                (if (map? %3)
                  (reduce-record %3)
                  (if (seq? %3)
                    (inner-records (map reduce-record %3))
                    (symboltype %2 %3))))
             {} property-map))

(defn make-new-record-handler
  [{:keys [property-map record-type]}]
  (str "make new " (*lookup-fn* record-type)
       " with properties " (reduce-record property-map)
       "\n"))

(def ast-jump "Jump Table for AST Expression"
  {:routine          routine-handler
   :ifthen           ifthen-handler
   :return           return-handler
   :tell             tell-handler
   :block            block-handler
   :define-locals    local-handler
   :define-record    record-handler
   :define-list      list-handler
   :assign           assign-handler
   :scalar-value     scalar-handler
   :count-of         count-of-handler
   :record-value     recordvalue-handler
   :value-of         valueof-handler
   :value-of-as-string valueof-asstring-handler
   :properties-of    propertiesof-handler
   :extend-list      extendlist-handler
   :extend-list-with-expression extendlist-expression-handler
   :extend-record    extendrecord-handler
   :make-new-record  make-new-record-handler
   :from-filter      filter-handler
   :repeat-loop      repeat-handler
   :filtered-repeat-loop  filtered-repeat-handler
   :filtered-delete    filtered-delete-handler
   :filter-expression  filter-expression-handler
   :string-p1-reference sp1r-handler
   :quit             quit-handler
   :save             save-handler
   :term             term-handler
   })


(defn ast-consume
  [{:keys [type token-fn] :as block}]
  (if token-fn
    (binding [*lookup-fn* token-fn]
      ((get ast-jump type nil-handler) block))
    ((get ast-jump type nil-handler) block)))

