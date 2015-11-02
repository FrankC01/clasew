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


(defn term-gen
  [coll]
  (map #(ast/term nil %) coll))

;;
;; Builders
;;

(defn nest-dispatch
  [coll args]
  (if (not-empty coll)
    (apply (partial ast/block nil) (for [x coll] (x args)))
    (ast/block nil)))

(defn build-mailboxes
  [mbblock cntrl {:keys [source accum property] :as props}]
  (ast/block
   nil
   (ast/define-locals nil :mbloop)
   (ast/for-in-expression
    nil
    (ast/term nil :mbloop)
    (ast/get-statement
     nil
      (ast/xofy-expression
       nil
       (ast/term nil property)
        (ast/term nil source)))
    (nest-dispatch cntrl (assoc props
                           :source :mbloop
                           :property :messages
                           )))))

(defn build-messages
  [msgblock cntrl {:keys [source accum property]}]
  (ast/block
   nil
   (ast/define-locals nil :msgloop)
   (ast/for-in-expression
    nil
    (ast/term nil :msgloop)
    (ast/get-statement
     nil
     (ast/xofy-expression
      nil
      (ast/term nil property)
      (ast/term nil source))))))

(defn account-block
  [{:keys [args]} cntrl {:keys [source accum] :as props}]
    (ast/block
     nil
     (ast/define-locals nil :arec :indx)
     (if (not-empty args)
       (apply (partial ast/define-locals nil) args)
       (ast/set-statement nil (ast/term nil accum) ast/empty-list))
     (ast/for-in-expression
      nil
      (ast/term nil :indx)
      (ast/term nil source)
      (if (empty? args)
        (ast/block nil)
        (ast/set-statement
         nil
         (ast/term nil accum)
         (ast/xofy-expression
          nil
          (apply (partial ast/record-definition nil)
               (map #(ast/key-value nil (ast/key-term %) (ast/term nil %)) args))
          (ast/term nil :indx))))
      (nest-dispatch cntrl (assoc props
                             :source :indx
                             :property :acct_mailboxes)))))

(defn build-account
  "Called when account information is requested. May contain
  filtered operation. Embeds any children as defined by
  tlist and dcnt"
  [accblock cntrl {:keys [source accum] :as args}]
  (ast/block
   nil
   (ast/define-locals nil source accum)
   (ast/set-statement
    nil
    (ast/term nil source)
    (ast/routine-call
     nil
     (ast/term nil :account_list)
     (ast/list-of nil (term-gen (:outlook mesgu/acc-list)))
     (ast/term nil (or (:filters accblock) :noop_filter ))))
   (account-block accblock cntrl args)
   (ast/set-statement
    nil
    (ast/eol-cmd nil :results nil)
    (ast/term nil accum))))

(defn- refactor-fetch
  "Look for filters to convert to handlers"
  [block]
  (let [x (map #(get % 2) block)
        y (if (not-empty x) (conj x (mesgu/noop-filter)) (list (mesgu/noop-filter)))]
  (apply (partial ast/block nil) y)))

(def ^:private builder-lookup
  {:accounts   build-account
   :mailboxes  build-mailboxes
   :messages   build-messages})

(defn newdisp
  [block]
  ((mesgu/meta-pattern builder-lookup block) {:source :aclist
                                              :accum  :arec}))

(defn build-fetch
  [block]
  ; First build filter structures and metapattern information
  ; mcat converts the input into handler collection and new block format
  ; refactor-fetch creates the handler body references
  ; meta-pattern determines depth constraints and subset order
  (let [[filter-coll imap] (mesgu/mcat block)]      ; Extract filters, reset block
    ; Setup the script header and handler routines
    (ast/block
     nil
     (mesgu/account_list :outlook)
     (refactor-fetch filter-coll)   ; handler list to filter-block
     ; Build the script body
     (ast/tell nil :outlook
               (ast/define-locals nil :results)
               (ast/set-statement nil (ast/term nil :results) ast/empty-list)
               (newdisp imap)
               (ast/return nil :results)))))

;(p (newdisp (mesg/accounts (mesg/mailboxes (mesg/messages)) )))

;;;;; TODO --- Dynamic Filter call for account_list
#_(p (mesg/run-script! (genas/ast-consume
          (build-fetch
           (mesg/accounts (astu/filter :acct_name astu/EQ "Planview") )))))

#_(p (build-fetch (mesg/accounts (astu/filter :name astu/EQ "Planview")
                  (mesg/mailboxes (mesg/messages)))))

#_(println (genas/ast-consume
          (build-fetch
           (mesg/mailboxes (mesg/messages)))
           ))



