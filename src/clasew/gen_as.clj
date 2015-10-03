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

;;
;; Generic ease of use functions
;;

(defn nil-handler
  [expression]
  (throw (Exception. (str expression " not handled"))))

(defn- endline
  [s]
  (if (= (get s (dec (count s))) \newline)
    s
    (str s "\n")))

(defn- map-and-interpose
  [s coll ]
  (apply str (interpose s (map ast-consume coll))))

;;
;; Filter management functions
;;

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


(defn- reduce-filter-args
  [acc exp]
  (let [[term predicate value & [larg]] exp]
    (conj
     acc
     (apply str(interpose
      " "
      (conj []
            (*lookup-fn* term)
            (if larg (str " of " (*lookup-fn* larg)) "")
            (get filterp predicate)
            (if (string? value)
              (str "\"" value "\"")
              (or (get filterp value nil)
                  (*lookup-fn* value)))))))))

(defn- reduce-filter-joins
  [acc [kw joinmap]]
  (conj acc
        (str " " (name kw) " " "(" (filter-reduce joinmap) ")")))

(defn- filter-reduce
  "Supports general filter emits applicable to if/then and where statements"
  [{:keys [args joins]}]
  (let [base (apply str(interpose " and " (reduce reduce-filter-args [] args)))]
    (if (empty? joins)
      base
      (str base (apply str (reduce reduce-filter-joins [] joins))))))

;;
;;   AST handlers - Emits AppleScript equivalent strings
;;

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
  (str (map-and-interpose " & " expressions)))

(defn record-definition-handler
  [{:keys [expressions]}]
  (str "{" (map-and-interpose "," expressions) "}"))

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
       (apply str (interpose "," (map name (:local-terms expression))))
       "\n"))

(defn block-handler
  [expression]
  (apply str (map ast-consume (:expressions expression))))

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
  (str "{" (map-and-interpose "," expressions ) "}"))


(def ast-jump "Jump Table for AST Expression"
  {
   :term             term-handler
   :key-term         key-term-handler
   :key-term-nl      key-term-nl-handler
   :string-literal   string-literal-handler
   :symbol-literal   symbol-literal-handler

   :expression       expression-handler
   :xofy-expression  xofy-handler
   :string-builder   string-builder-handler

   :define-locals    local-handler

   :make-new         make-new-handler
   :where-filter     where-filter-handler

   :if-expression      if-handler
   :else-if-expression elseif-handler
   :for-in-expression for-in-handler

   :tell             tell-handler
   :block            block-handler
   :return           return-handler
   :routine          routine-handler
   :routine-call     routine-callhandler

   :set-statement    set-statement-handler
   :if-statement     ifs-handler

   :li-cmd           list-items-cmd-handler
   :list-of          list-of-handler
   :eol-cmd          end-of-list-cmd-handler

   :record-definition record-definition-handler
   :key-value        key-value-handler
   :eor-cmd          end-of-rec-cmd-handler

   :append-object    append-object-handler

   ;; TODO: Evaluate below for deprecation
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

