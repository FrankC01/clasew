(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - Experimental AppleScript Generator"}
  clasew.gen-as
  (:require [clasew.utility :as util]
            [clasew.ast-emit :as ast]
            [clojure.java.io :as io]
            [clojure.walk :as w]))


(declare ast-consume)

(def ^:dynamic *lookup-fn* (fn [term-kw] (name term-kw)))

(defn nil-handler
  [expression]
  (throw (Exception. (str expression " not handled"))))

(defn endline
  [s]
  (if (= (get s (dec (count s))) \newline)
    s
    (str s "\n")))

(defn map-and-interpose
  [coll s]
  (apply str (interpose s (map ast-consume coll))))

(defn term-handler
  [{:keys [to-value]}]
  (*lookup-fn* to-value))

(defn key-term-handler
  [{:keys [key-term]}]
  (str (*lookup-fn* key-term) ":"))

(defn key-term-nl-handler
  [{:keys [key-term]}]
  (str (name key-term) ":"))

(defn string-literal-handler
  [{:keys [svalue]}]
  (str "\"" svalue "\""))

(defn symbol-literal-handler
  [{:keys [svalue]}]
  (symbol svalue))

(defn end-of-list-cmd-handler
  [{:keys [target-list list-owner]}]
  (str "end of "(*lookup-fn* target-list)
       (if list-owner (str " of " (*lookup-fn* list-owner)) "")))

(defn end-of-rec-cmd-handler
  [{:keys [target-rec rec-owner source]}]
  (str (*lookup-fn* target-rec)
       (if rec-owner (str " of " (*lookup-fn* rec-owner)) "")
       " & "
       (ast-consume source)))

(defn list-items-cmd-handler
  [{:keys [target-owner]}]
  (str " " (name target-owner)"'s items"))

(defn expression-handler
  [{:keys [expressions]}]
  (endline (apply str (map ast-consume expressions))))

(defn append-object-handler
  [{:keys [svalue]}]
  (str " & " (*lookup-fn* svalue)))

(defn key-value-handler
  [{:keys [key-term value-expression]}]
  (str (ast-consume key-term) (ast-consume value-expression)))

(defn set-statement-handler
  "Emit set (expression) to (expression) cr"
  [{:keys [set-lhs-expression set-rhs-expression]}]
  (endline (str "set " (ast-consume set-lhs-expression)
       " to " (ast-consume set-rhs-expression))))

(defn string-builder-handler
  [{:keys [expressions]}]
  (str (apply str
              (interpose " & "(map ast-consume expressions)))))

(defn record-definition-handler
  [{:keys [expressions]}]
  (str "{" (map-and-interpose expressions ",") "}"))

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

(defn routine-callhandler
  [{:keys [routine-name routine-arguments]}]
  (str "my "
       (ast-consume routine-name)
       "("
       (ast-consume routine-arguments)
       ")"))

(defn elseif-handler
  [{:keys [ifexp]}]
  (str "else " (ast-consume ifexp)))

(defn ifs-handler
  [{:keys [i-expression e-expressions]}]
  (str (ast-consume i-expression)
       (apply str (map ast-consume e-expressions))
       "end if\n")
  )

(defn return-handler
  [{:keys [return-val]}]
  (str "return " (name return-val)"\n"))

(defn xofy-handler
  [{:keys [x-expression y-expression]}]
  (str (ast-consume x-expression) " of " (ast-consume y-expression)))


(defn for-in-handler
  [{:keys [control in expressions]}]
  (str "repeat with "
       (ast-consume control)
       " in "
       (ast-consume in)
       "\n"
       (apply str (map ast-consume expressions))
       "end repeat\n"))

(defn local-handler
  [expression]
  (str "local "
       (apply str
              (interpose ","
                         (map name (:local-terms expression)))) "\n"))

(defn block-handler
  [expression]
  (apply str (map ast-consume (:expressions expression))))

(defn count-of-handler
  [{:keys [set-target expressions]}]
  (str "set "(name set-target)" to count of " (ast-consume expressions) "\n"))

(defn valueof-handler
  "The value requires a name resolution lookup"
  [{:keys [type value from apply-function] :as to-map}]
  (if apply-function
    (str " to my " (name apply-function) "(" (*lookup-fn* value) " of " (name from) ")")
  (str " to " (*lookup-fn* value) " of " (name from) )))

(defn valueof-asstring-handler
  [vo-map]
  (str (valueof-handler vo-map) " as string"))

(defn propertiesof-handler
  "Results in 'set x to properties of y'"
  [{:keys [value properties-of]}]
  (str "set " (name value) " to properties of " (name properties-of) "\n"))

(declare filter-reduce)

(def filterp
  {:equal-to "equals"
   :not-equal-to "is not equal to"
   :less-than "<"
   :not-less-than "not less than"
   :greater-than ">"
   :not-greater-than "not greater than"
   :contains "contains"
   :not-contains "does not contain"
   :starts-with "starts with"
   :ends-with "ends with"
   :is-in "is in"
   :is-not-in "is not in"
   :and "and"
   :or "or"
   :missing "missing value"
   })


(defn reduce-filter-args
  [acc exp]
  (let [[term predicate value & [larg]] exp]
  (conj acc
        (apply str
               (interpose " "
                          (conj []
                                (*lookup-fn* term)
                                (if larg (str " of " (*lookup-fn* larg)) "")
                                (get filterp predicate)
                                (if (string? value)
                                  (str "\"" value "\"")
                                  (or (get filterp value nil)
                                      (*lookup-fn* value))
                                  )))))))

(defn reduce-filter-joins
  [acc [kw joinmap]]
  (conj acc
        (str " " (name kw) " " "(" (filter-reduce joinmap) ")")))

(defn filter-reduce
  [{:keys [args joins]}]
  (let [base (apply str (interpose " and " (reduce reduce-filter-args [] args)))]
    (if (empty? joins)
      base
      (str base (apply str (reduce reduce-filter-joins [] joins))))))

(defn if-handler
  [{:keys [predicate expressions]}]
  (str "if (" (filter-reduce predicate) ") then\n"
       (apply str (map ast-consume expressions))))

(defn where-filter-handler
  "Emits expressions whose (filter-expression)"
  [{:keys [target predicate]}]
  (str (ast-consume target) " whose (" (filter-reduce predicate) ")"))

(defn make-new-handler
  "Emits 'make new expressions'"
  [{:keys [target-expr expressions]}]
  (str "make new "
       (ast-consume target-expr)
       (apply str (map ast-consume expressions))
       "\n"))

(defn list-of-handler
  [{:keys [expressions] }]
  (str "{" (map-and-interpose expressions ",") "}"))


(def ast-jump "Jump Table for AST Expression"
  {
   :term             term-handler
   :key-term         key-term-handler
   :key-term-nl      key-term-nl-handler
   :string-literal   string-literal-handler
   :symbol-literal   symbol-literal-handler
   :xofy-expression  xofy-handler
   :eol-cmd          end-of-list-cmd-handler
   :eor-cmd          end-of-rec-cmd-handler
   :li-cmd           list-items-cmd-handler
   :expression       expression-handler
   :append-object    append-object-handler
   :set-statement    set-statement-handler
   :string-builder   string-builder-handler
   :record-definition record-definition-handler
   :list-of          list-of-handler
   :key-value        key-value-handler

   :routine          routine-handler
   :routine-call     routine-callhandler

   :for-in-expression for-in-handler
   :where-filter     where-filter-handler
   :make-new         make-new-handler

   :tell             tell-handler
   :block            block-handler
   :return           return-handler

   :if-expression      if-handler
   :else-if-expression elseif-handler
   :if-statement     ifs-handler

   :define-locals    local-handler

   ;; TODO: Evaluate below for deprecation

   :count-of         count-of-handler
   :value-of         valueof-handler
   :value-of-as-string valueof-asstring-handler
   :properties-of    propertiesof-handler
   })

(def instrument (atom {}))

(defn ginstrument
  [arg]
  (condp = arg
    nil @instrument
    :r  (reset! instrument {})))

(defn ast-consume
  [{:keys [type token-fn] :as block}]
  (if (nil? (get @instrument type nil))
    (swap! instrument assoc type 1)
    (swap! instrument update-in [type] inc))
  (if token-fn
    (do
      (binding [*lookup-fn* token-fn]
        ((get ast-jump type nil-handler) block)))
    (do
      ((get ast-jump type nil-handler) block))))

