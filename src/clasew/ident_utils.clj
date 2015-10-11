(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - Identity Utilities"}
  clasew.ident-utils
  (:require [clasew.ast-emit :as ast]))

;;
;; General purpose identity functions
;;

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

(def ^:private type-instance
  {:emails :email
   :phones :phone
   :addresses :address})

(defn update-sets
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

(defn update-filter-block
  "Creates a filtered set and/or adds to the base results"
  [loopkw target subterm {:keys [filters sets adds] :or {adds '()}} subsets]
  (ast/block
   nil
    (ast/for-in-expression
       nil
       (ast/term nil loopkw)
       (if filters
         (ast/where-filter nil
                           (if subterm
                             (ast/xofy-expression
                              nil
                              (ast/term nil subterm)
                              (ast/term nil target))
                             (ast/term nil target))
                           filters)
         (ast/term nil target))
       (or subsets  ast/noop)
       (apply (partial ast/block nil) (update-sets loopkw sets)))
   (if (empty? adds)
     ast/noop
     (expand adds (subterm type-instance) subterm target))))

(defn update-if-filter-block
  "Creates a filtered if set and/or adds to the base results"
  [tkey loopkw target subterm {:keys [filters sets adds] :or {adds '()}} subsets]
  (ast/block
   nil
   (ast/define-locals nil tkey loopkw)
   (ast/set-statement
    nil
    (ast/term nil tkey)
    (ast/get-statement
     nil
     (ast/xofy-expression nil (ast/term nil subterm) (ast/term nil target))))
   (ast/for-in-expression
    nil (ast/term nil loopkw) (ast/term nil tkey)
    (ast/if-statement
     nil
     (ast/if-expression
      nil filters
      (apply (partial ast/block nil) (update-sets loopkw sets))) nil))
   (ast/set-statement
    nil
    (ast/xofy-expression nil (ast/term nil subterm) (ast/term nil target))
    (ast/term nil tkey))
   (if (empty? adds)
     ast/noop
     (expand adds (subterm type-instance) subterm target))))

(defn update-subsets-reduce
  "Reduce subsets to filters and adds or just adds"
  [acc [tkey {:keys [filters sets adds] :as iblock}]]
  (conj acc
        (if filters
          (update-filter-block :sloop :cloop tkey iblock nil)
          (if (not-empty adds)
            (expand adds (tkey type-instance) tkey :cloop)
            ast/noop))))

