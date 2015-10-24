(ns
  ^{:author "Frank V. Castellucci"
    :doc "Clojure AppleScriptEngine Wrapper - common message DSL"}
  clasew.ast-utils
  (:require   [clasew.core :as as]
              [clasew.utility :as util]
              [clasew.gen-as :as genas]
              [clasew.ast-emit :as ast])

  (:refer-clojure :rename {filter cfilter
                           or     cor
                           and    cand}))

;;
;; Special handlers
;;

(defn filter-!v
  "Filter for non-vector data types"
  [args]
  (cfilter #(not (vector? %)) args))

(defn- filter-v
  "Filter for vector data types"
  [args]
  (cfilter #(vector? %) args))

(defn filter-form
  [args]
  (cfilter map? args))

(defn-  filter-for-vkw
  "Filter for vector with first position keyword match"
  [kw args]
  (cfilter #(cand (vector? %) (= (first %) kw)) args))

(defn filter-!forv
  "Filter for vector not containing first position key"
  [kw args]
  (cfilter #(cand (vector? %) (not= (first %) kw)) args))

(defn filter-forv
  "[kw args] First position return from filter-for-vkw "
  ([kw args] (filter-forv kw args first))
  ([kw args f] (f (filter-for-vkw kw args))))

;; Filter predicates

(def EQ :equal-to)
(def !EQ :not-equal-to)
(def LT :less-than)
(def !LT :not-less-than)
(def GT :greater-than)
(def !GT :not-greater-than)
(def CT :contains)
(def !CT :not-contains)
(def SW :starts-with)
(def EW :ends-with)
(def II :is-in)
(def !II :is-not-in)

;;
;; Filtering
;;


(defn- filter-parse
  "Prepares filter constructs for emitting"
  [coll]
  (assoc {}
  :args (partition 3 (cfilter #(not (vector? %)) coll))
   :joins (cfilter vector? coll)))

(defn filter
  "Used to construct complex filter for individuals"
  [& args]
  [:filter (filter-parse args)])

(defn and
  "Used in complex filter construction. Use 'cand' for core function
  in this namespeace"
  [& args]
  [:and (filter-parse args)])

(defn or
  "Used in complex filter construction. Use 'cor' for core function
  in this namespeace"
  [& args]
  [:or (filter-parse args)])

;;
;; Predefined utilities
;;

(def cleanval "Takes an argument and test for 'missing value'.
  Returns value or null"
  (ast/routine
   nil :cleanval [:val]
   (ast/define-locals nil :oval)
   (ast/set-statement nil (ast/term nil :oval) ast/null)
   (ast/if-statement
    nil
    (ast/if-expression
     nil
     (ast/predicate nil (second (filter :val !EQ :missing)))
     (ast/set-statement nil (ast/term nil :oval)
                                  (ast/term nil :val))) nil)
   (ast/return nil :oval)))

(defn quit
  "Script to quit an application
  appkw - keyword (:outlook or :contacts) identies the application
  to shut down"
  [appkw]
  (genas/ast-consume
   (ast/tell nil appkw
             (ast/define-locals nil :results)
             (ast/set-statement nil (ast/term nil :results) ast/empty-list)
             (ast/set-statement
              nil
              (ast/eol-cmd nil :results nil)
              (ast/string-literal "quit successful"))
             ast/quit
             (ast/return nil :results))))
