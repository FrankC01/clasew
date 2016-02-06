(ns
  ^{:author "Frank V. Castellucci"
      :doc "clasew - Experimental AppleScript Generator - Multi Methods"}
  clasew.gen-asmm
  (:require [clasew.ast-emit :refer :all]))

;;
;; Predefs and utilities
;;

(def nolookup (fn [term-kw] (name term-kw)))
(def ^:dynamic *lookup-fn* nolookup)

(def ^:private COMMA ",")
(def ^:private AND " and ")
(def ^:private AGG " & ")

(def ^:private predicate-operators
  {:equal-to "is equal to"
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


(defn- endline
  "Puts a newline if one doesn't exist"
  [s]
  (if (= (get s (dec (count s))) \newline) s (str s "\n")))

(defn- map-and-interpose
  "String with seperator interposed"
  [s coll f]
  (apply str (interpose s (map f coll))))

;; Multimethod dispatch

(defmulti consume :ast-type)

;; wrapper for dispatch

(defn consume-it [{:keys [token-fn]:as expression}]
  "Wraps consume to imbue term name resolutions"
  (if token-fn
    (do (binding [*lookup-fn* token-fn]
      (consume expression)))
    (consume expression)))

;; consume multimethods
;;

; Default handler - TODO: Hook exception

(defmethod consume :default
  [{:keys [ast-type] :as ivexp}]
  (println type " not handled yet " ivexp))

;;
;; Simple Term types
;;

(defmethod consume TERM
  [{:keys [to-value]}]
  (*lookup-fn* to-value))

(defmethod consume KEY-TERM
  [{:keys [key-term]}]
  (str (*lookup-fn* key-term) ":"))

(defmethod consume TERM-NL
  [{:keys [to-value]}]
  (name to-value))

(defmethod consume KEY-TERM-NL
  [{:keys [key-term]}]
  (str (name key-term) ":"))

(defmethod consume STRING-LITERAL
  [{:keys [svalue]}]
  (str "\"" svalue "\""))

(defmethod consume SYMBOL-LITERAL
  [{:keys [svalue]}]
  (symbol svalue))

(defmethod consume NUMERIC-LITERAL
  [{:keys [nvalue]}]
  (symbol (str nvalue)))

;;
;; Generic expressions types
;;

(defmethod consume BLOCK
  [{:keys [expressions]}]
  (apply str (map consume-it expressions)))

(defmethod consume TELL
  [{:keys [target expressions]}]
  (let   [body (apply str (map consume-it expressions))]
  (str "tell application " target "\n" body "end tell\n")))

(defmethod consume SCRIPT
  [{:keys [script-name auto-run expressions]}]
  (str "script " (name script-name) "\n"
       (apply str (map consume-it expressions))
       "end script\n"
       (if auto-run (str "tell " (name script-name) " to run\n") "")))

(defmethod consume ROUTINE
  [{:keys [routine-name parameters expressions]}]
  (let [body (apply str (map consume-it expressions))
        rname (name routine-name)]
    (str "on " rname
         "(" (map-and-interpose COMMA parameters name) ")\n"
         body
         "end " rname "\n\n")))

(defmethod consume ROUTINE-CALL
  [{:keys [routine-name routine-arguments]}]
  (str "my "
       (consume-it routine-name)
       "(" (map-and-interpose COMMA routine-arguments consume-it) ")"))

(defmethod consume RETURN
  [{:keys [return-val]}]
  (endline
    (str "return "
         (if (map? return-val) (consume-it return-val) (name return-val)))))

;;
;; Predicate processing
;;

(defmethod consume PREDICATE-STATEMENT
  [{:keys [expressions]}]
  (let [x (apply str (map consume-it expressions))]
    (str "(" x ")")))

(defmethod consume PREDICATE-EXPRESSIONS
  [{:keys [conditions]}]
  (str "(" (map-and-interpose AND conditions consume-it) ")"))

(defmethod consume PREDICATE-CONDITION
  [{:keys [lhs-expression operator rhs-expression]}]
  (str (apply str (consume-it lhs-expression)
       (consume-it operator))
       (consume-it rhs-expression)))

(defmethod consume PREDICATE-AND
  [{:keys [expressions]}]
  (str " and " (apply str (map consume-it expressions))))

(defmethod consume PREDICATE-OR
  [{:keys [expressions]}]
  (str " or " (apply str (map consume-it expressions))))

(defmethod consume PREDICATE-OPERATOR
  [{:keys [term]}]
  (str " " (get predicate-operators term) " "))

(defmethod consume WHERE-FILTER
  [{:keys [target predicate]}]
  (str (consume-it target) " whose " (consume-it predicate)))

;;
;; Data statement CRUD processing
;;

(defmethod consume DEFINE-LOCALS
  [{:keys [local-terms]}]
  (endline (str "local " (map-and-interpose COMMA local-terms name))))

(defmethod consume SET-STATEMENT
  [{:keys [set-lhs-expression set-rhs-expression]}]
  (endline (str "set " (consume-it set-lhs-expression)
       " to " (consume-it set-rhs-expression))))

(defmethod consume KEY-VALUE
  [{:keys [key-term value-expression]}]
  (str (consume-it key-term) (consume-it value-expression)))

(defmethod consume PROPERTY
  [{:keys [kv]}]
  (endline (str "property " (consume-it kv))))

(defmethod consume XOFY-EXPRESSION
  [{:keys [x-expression y-expression]}]
  (str (consume-it x-expression) " of " (consume-it y-expression)))

(defmethod consume LIST-OF
  [{:keys [expressions]}]
  (str "{" (map-and-interpose COMMA expressions consume-it) "}"))

(defmethod consume END-OF-LIST
  [{:keys [target-list list-owner]}]
  (str "end of "(*lookup-fn* target-list)
       (if list-owner (str " of " (*lookup-fn* list-owner)) "")))

(defmethod consume RECORD
  [{:keys [expressions]}]
  (str "{" (map-and-interpose COMMA expressions consume-it) "}"))

(defmethod consume END-OF-RECORD
  [{:keys [target-rec rec-owner source]}]
  (str (*lookup-fn* target-rec)
       (if rec-owner (str " of " (*lookup-fn* rec-owner)) "")
       " & "
       (consume-it source)))

(defmethod consume STRING-BUILDER
  [{:keys [expressions]}]
  (str (map-and-interpose AGG expressions consume-it)))

(defmethod consume MAKE-NEW-OBJECT
  [{:keys [target-expr expressions]}]
  (endline
    (str "make new "
       (consume-it target-expr)
       (apply str (map consume-it expressions)))))


; Conditional processing

(defmethod consume IF-STATEMENT
  [{:keys [i-expression e-expressions]}]
  (endline
    (str (consume-it i-expression)
         (apply str (map consume-it
                         (if (map? e-expressions)
                           (list e-expressions)
                           e-expressions)))
         "end if")))

(defmethod consume IF-EXPRESSION
  [{:keys [predicate expressions]}]
  (str "if (" (consume-it predicate) ") then\n"
       (apply str (map consume-it expressions))))

(defmethod consume ELSE-IF-EXPRESSION
  [{:keys [ifexp]}]
  (str "else\n" (consume-it ifexp)))

; For loop processing

(defmethod consume FOR-EXPRESSION
  [{:keys [control in expressions]}]
  (endline
    (str "repeat with "
       (consume-it control)
       " in "
       (consume-it in)
       "\n"
       (endline (apply str (map consume-it expressions)))
       "end repeat")))

;; Entry point

(defn ast-consume
  [block]
  (consume-it block))
