(ns
  ^{:author "Frank V. Castellucci"
      :doc "spike space"}
  clasew.sandbox
  (:require [clojure.pprint :refer :all]
            [clojure.core.reducers :as r]
            [clasew.messages :as mesg]
            [clasew.mesg-utils :as mesgu]
            [clasew.gen-as :as genas]
            [clasew.ast-emit :as ast]
            [clasew.ast-utils :as astu]))

;; Demonstrate record coercion
(def p pprint)


;(println (genas/ast-consume (mesgu/account_list :outlook)))

(defn term-gen
  [coll]
  (map #(ast/term nil %) coll))

(defn account-block
  [accblock]
  (ast/block
   nil
   (apply (partial ast/define-locals nil) (:args accblock))
   (ast/define-locals nil :x :i)
   (ast/for-in-expression
    nil
    (ast/term nil :i)
    (ast/term nil :aclist)
    (ast/set-statement
     nil
     (ast/term nil :x)
     (ast/xofy-expression
      nil
      (apply (partial ast/record-definition nil)
             (map #(ast/key-value nil (ast/key-term %) (ast/term nil %)) (:args accblock)))
      (ast/term nil :i)))
    (ast/set-statement
     nil
     (ast/eol-cmd nil :results nil)
     (ast/term nil :x)))))


(defn- refactor-fetch
  "Look for filters to convert to handlers"
  [block]
  (let [x (map #(get % 2) block)
        y (if (not-empty x) (conj x (mesgu/noop-filter)) (list (mesgu/noop-filter)))]
  (apply (partial ast/block nil) y)))

(defn build-fetch
  [block]
  (let [[filter-coll imap] (mesgu/mcat block)
        filter-block (refactor-fetch filter-coll)]
  (ast/block
   nil
   (mesgu/account_list :outlook)
   filter-block
   (ast/tell nil :outlook
             (ast/define-locals nil :aclist :results)
             (ast/set-statement nil (ast/term nil :results) ast/empty-list)
             (ast/set-statement
              nil
              (ast/term nil :aclist)
              (ast/routine-call
               nil
               (ast/term nil :account_list)
               (ast/list-of nil (term-gen (:outlook mesgu/acc-list)))
               (ast/term nil (or (:filters imap) :noop_filter ))))
             (account-block block)

             (ast/return nil :results)))))


;(clojure.walk/prewalk-demo (mesg/accounts ))
;(p (mesg/accounts  (mesg/messages (astu/filter :a astu/EQ :b)(mesg/mailboxes) )))

;;;;; TODO --- Dynamic Filter call for account_list
#_(p (mesg/run-script! (genas/ast-consume
          (build-fetch
           (mesg/accounts (astu/filter :name astu/EQ "Planview") )))))

#_(println (genas/ast-consume
          (build-fetch
           (mesg/accounts (astu/filter :name astu/EQ "Planview") )
           ;(mesg/messages (astu/filter :name astu/EQ "Planview"))
           )))


