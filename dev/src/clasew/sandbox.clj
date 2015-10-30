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


;(mesgu/meta-pattern (mesg/accounts))
;(mesgu/meta-pattern (mesg/accounts (mesg/mailboxes)))
;(mesgu/meta-pattern (mesg/accounts (mesg/mailboxes (mesg/mailboxes))))
;(mesgu/meta-pattern (mesg/accounts (mesg/mailboxes)  (mesg/messages)))
;(mesgu/meta-pattern (mesg/accounts (mesg/mailboxes) (mesg/messages) (mesg/messages)))


(defn term-gen
  [coll]
  (map #(ast/term nil %) coll))

;;
;; Builders
;;


(defn account-block
  ([accblock srcterm] (account-block accblock srcterm [:accounts] [0]))
  ([accblock srcterm tlist dcnt]
    (ast/block
     nil
     (apply (partial ast/define-locals nil) (:args accblock))
     (ast/define-locals nil :arec :indx)
     (ast/for-in-expression
      nil
      (ast/term nil :indx)
      (ast/term nil srcterm)
      (ast/set-statement
       nil
       (ast/term nil :arec)
       (ast/xofy-expression
        nil
        (apply (partial ast/record-definition nil)
             (map #(ast/key-value nil (ast/key-term %) (ast/term nil %)) (:args accblock)))
        (ast/term nil :indx)))))))

(defn build-account
  "Called when account information is requested. May contain
  filtered operation. Embeds any children as defined by
  tlist and dcnt"
  [imap tlist dcnt]
  (ast/block
   nil
   (ast/define-locals nil :aclist)
   (ast/set-statement
    nil
    (ast/term nil :aclist)
    (ast/routine-call
     nil
     (ast/term nil :account_list)
     (ast/list-of nil (term-gen (:outlook mesgu/acc-list)))
     (ast/term nil (or (:filters imap) :noop_filter ))))
   (account-block imap :aclist)
   (ast/set-statement
    nil
    (ast/eol-cmd nil :results nil)
    (ast/term nil :arec))))

(defn- refactor-fetch
  "Look for filters to convert to handlers"
  [block]
  (let [x (map #(get % 2) block)
        y (if (not-empty x) (conj x (mesgu/noop-filter)) (list (mesgu/noop-filter)))]
  (apply (partial ast/block nil) y)))

(defn build-blocks
  "Pattern driven block builder"
  [imap [dcnt tlist]]
  (condp = (first tlist)
    :accounts (build-account imap tlist dcnt)
    :mailboxes (do (println "mailboxes") (ast/block nil))
    :messages (do (println "messages") (ast/block nil))))

(defn build-fetch
  [block]
  ; First build filter structures and metapattern information
  ; mcat converts the input into handler collection and new block format
  ; refactor-fetch creates the handler body references
  ; meta-pattern determines depth constraints and subset order
  (let [[filter-coll imap] (mesgu/mcat block)     ; Extract filters, reset block
        filter-block (refactor-fetch filter-coll) ; handler list to filter-block
        meta-info (mesgu/meta-pattern block)]
    ; Setup the script header and handler routines
    (ast/block
     nil
     (mesgu/account_list :outlook)
     filter-block
     ; Build the script body
     (ast/tell nil :outlook
               (ast/define-locals nil :results)
               (ast/set-statement nil (ast/term nil :results) ast/empty-list)
               (build-blocks imap meta-info)
               (ast/return nil :results)))))


#_(p (mesg/accounts (astu/filter :name astu/EQ "Planview")
                  (mesg/mailboxes (mesg/messages))))

;;;;; TODO --- Dynamic Filter call for account_list
#_(p (mesg/run-script! (genas/ast-consume
          (build-fetch
           (mesg/accounts (astu/filter :acct_name astu/EQ "Planview") )))))

#_(println (genas/ast-consume
          (build-fetch
           (mesg/accounts (astu/filter :acct_name astu/EQ "Planview") )
           ;(mesg/messages (astu/filter :name astu/EQ "Planview"))
           )))

#_(p
          (mesgu/meta-pattern
           (mesg/accounts (astu/filter :acct_name astu/EQ "Planview")
                          (mesg/mailboxes (mesg/messages)) )
           ))

#_(p (mfoo (mesg/accounts
       (mesg/mailboxes)
       (mesg/messages))))

