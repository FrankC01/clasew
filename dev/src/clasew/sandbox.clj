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

(def outlook-messages
  {
   :dstring                 "date string"
   :acct_name               "name",
   :acct_emails             "email address",
   :acct_user_name          "user name",
   :acct_user_fullname      "full name",
   :acct_mailboxes          "mail folders",
   :mb_name                 "name"
   :mb_message_count        "count of messages"
   :mb_unread_message_count "unread count"
   :msg_subject             "subject"
   :msg_sender              "sender"
   :msg_text                "plain text content"
   :msg_recipients          "recipients"
   :msg_date_recieved       "time received"
   :msg_date_sent           "time sent"
   :msg_read                "is read"
   :msg_meeting             "is meeting"
   :msg_replied             "replied to"
   })

(defn- outlook-mapcore-messages
  [termkw]
  (get outlook-messages termkw (name termkw)))

;;
;; Builders
;;

(defn nest-dispatch
  [coll args]
  (if (not-empty coll)
    (apply (partial ast/block nil) (for [x coll] (x args)))
    (ast/block nil)))

(defn- contains-type?
  [kw args]
  (some? (some #(= % kw ) args)))

(defn- strip-msg-args
  "Strips certain values from attributes to fetch"
  [args]
  (filter #(and
            (not= % :msg_sender)
            (not= % :msg_recipients)
            (not= % :msg_date_sent)
            (not= % :msg_date_recieved)) args))

; set msgrec to msgrec & {msg_date_received:date string of (get time received of msgloop)}

(defn- date-handler
  [tf dtype targ src]
  (if tf
    (ast/set-statement
     nil
     (ast/term nil targ)
     (ast/eor-cmd
      nil targ nil
      (ast/record-definition
       nil
       (ast/key-value
        nil
        (ast/key-term dtype)
        (ast/xofy-expression
         nil
         (ast/term outlook-mapcore-messages :dstring)
         (ast/get-statement
          nil
          (ast/xofy-expression
           outlook-mapcore-messages
           (ast/term nil dtype)
           (ast/term nil src))))))))
    (ast/block nil)))

(defn- sender-handler
  "Builds extract statement specific to sender email address"
  [tf targ src]
  (if tf
    (ast/set-statement
     nil
     (ast/term nil targ)
     (ast/eor-cmd
      nil targ nil
      (ast/record-definition
       nil
       (ast/key-value
        nil
        (ast/key-term :msg_sender)
        (ast/xofy-expression
         nil
         (ast/term nil :address)
         (ast/get-statement
          outlook-mapcore-messages
          (ast/xofy-expression
           nil (ast/term nil :msg_sender) (ast/term nil src))))))))
    (ast/block nil)))

(defn- recipient-handler
  "Builds loop construct to pull each message recipient email
  address"
  [tf targ src]
  (if tf
    (ast/block
     nil
     (ast/define-locals :reclist :recrec :recloop)
     (ast/set-statement nil (ast/term nil :reclist) ast/empty-list)
     (ast/for-in-expression
      nil
      (ast/term nil :recloop)
      (ast/xofy-expression
       outlook-mapcore-messages
       (ast/term nil :msg_recipients) (ast/term nil src))
      (ast/set-statement
       nil
       (ast/eol-cmd nil :reclist nil)
       (ast/record-definition
        nil
        (ast/key-value
         nil
         (ast/key-term :recipient_email)
         (ast/xofy-expression
          nil
          (ast/term nil :address)
          (ast/get-statement
           nil
           (ast/xofy-expression
            outlook-mapcore-messages
            (ast/term nil :acct_emails)(ast/term nil :recloop))))))))
     (ast/set-extend-record targ :msg_recipients :reclist))
    (ast/block nil)))

(defn build-messages
  "Builds message fetch control"
  [{:keys [args]} cntrl {:keys [source accum property]}]
  (let [has-recp (contains-type? :msg_recipients args)
        has-send (contains-type? :msg_sender args)
        has-drec (contains-type? :msg_date_recieved args)
        has-dsen (contains-type? :msg_date_sent args)
        clean-args (strip-msg-args args)]
    (ast/block
     nil
     (ast/define-locals nil :msgloop :msgrec :msglist)
     (ast/set-statement nil (ast/term nil :msglist) ast/empty-list)
     (ast/for-in-expression
      nil
      (ast/term nil :msgloop)
      (ast/get-statement
       nil
       (ast/xofy-expression
        nil
        (ast/term nil property)
        (ast/term nil source)))
      (ast/record-fetch outlook-mapcore-messages clean-args :msgrec :msgloop)
      ; Sender special handler
      (sender-handler has-send :msgrec :msgloop)
      ; Recipient special handler
      (recipient-handler has-recp :msgrec :msgloop)
      ; Date special handler
      (date-handler has-dsen :msg_date_sent :msgrec :msgloop)
      (date-handler has-drec :msg_date_recieved :msgrec :msgloop)
      (ast/set-statement
       nil
       (ast/eol-cmd nil :msglist nil)
       (ast/term nil :msgrec)))
     (ast/set-extend-record accum :mb_messages :msglist))))

(defn build-mailboxes
  "Builds mailbox (mail folders in outlook) fetch control"
  [{:keys [args filters]} cntrl {:keys [source accum property] :as props}]
  (ast/block
   nil
   (ast/define-locals nil :mbloop :mrec :mlist)
   (ast/set-statement nil (ast/term nil :mlist) ast/empty-list)
   (ast/for-in-expression
    nil
    (ast/term nil :mbloop)
    (ast/get-statement
     nil
      (ast/xofy-expression
       nil
       (ast/term nil property)
        (ast/term nil source)))
    (ast/if-statement
     nil
     (ast/if-expression
      nil
      (ast/routine-call
       nil
       (ast/term nil :match)
       (ast/term nil (or filters :noop_filter ))
       (ast/term nil :mbloop))
      (ast/record-fetch outlook-mapcore-messages args :mrec :mbloop)
      (nest-dispatch cntrl (assoc props
                           :source   :mbloop
                           :property :messages
                           :accum    :mrec
                           ))
      (ast/set-statement
       nil
       (ast/eol-cmd nil :mlist nil)
       (ast/term nil :mrec))) nil))
   (ast/set-extend-record accum :acct_mailboxes :mlist)))

(defn account-block
  "Builds the account block for setting up and fetching
  account values"
  [{:keys [args]} cntrl {:keys [source accum] :as props}]
  (ast/block
   nil
   (ast/define-locals nil :indx)
   (ast/for-in-expression
    nil
    (ast/term nil :indx)
    (ast/term nil source)
    (ast/record-fetch nil args accum :indx)
    (nest-dispatch cntrl (assoc props
                           :source :indx
                           :property :acct_mailboxes))
    (ast/set-statement
     nil
     (ast/eol-cmd nil :alist nil)
     (ast/term nil accum)))))

(defn build-account
  "Called when account information is requested. May contain
  filtered operation. Embeds any children as defined by
  source DSL"
  [accblock cntrl {:keys [source accum] :as args}]
  (ast/block
   nil
   (ast/define-locals nil source accum :alist)
   (ast/set-statement nil (ast/term nil :alist) ast/empty-list)
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
    (ast/term nil :results)
    (ast/term nil :alist))))

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

(defn build-fetch
  [block]
  ; First build filter structures and metapattern information
  ; mcat converts the input into handler collection and new block format
  ; refactor-fetch creates the handler body references
  ; meta-pattern determines depth constraints and subset order
  (let [[filter-coll imap] (mesgu/mcat block outlook-mapcore-messages)]  ; Extract filters, reset block
    ; Setup the script header and handler routines
    (ast/block
     nil
     (mesgu/account_list :outlook outlook-mapcore-messages)
     (refactor-fetch filter-coll)   ; handler list to filter-block
     (ast/tell nil :outlook
               (ast/define-locals nil :results)
               ((mesgu/meta-pattern builder-lookup imap) {:source :aclist
                                              :accum  :arec})
               (ast/return nil :results)))))

;(p (newdisp (mesg/accounts (mesg/mailboxes (mesg/messages)) )))

;;;;; TODO --- Dynamic Filter call for account_list
#_(p (mesg/run-script! (genas/ast-consume
          (build-fetch
           (mesg/accounts (astu/filter :acct_name astu/EQ "Planview") )))))

#_(p (build-fetch (mesg/accounts (astu/filter :name astu/EQ "Planview")
                  (mesg/mailboxes (mesg/messages)))))

(p (mesg/run-script!
    (genas/ast-consume
          (build-fetch
           (mesg/accounts
            (astu/filter :acct_name astu/EQ "Axiom1inc")
            (mesg/mailboxes
             (astu/filter :mb_name astu/EQ "Deleted Items")
             (mesg/messages :msg_subject
                            :msg_sender
                            :msg_recipients
                            :msg_date_sent
                            :msg_date_recieved))))
           )))



