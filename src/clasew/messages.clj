(ns
  ^{:author "Frank V. Castellucci"
    :doc "Clojure AppleScriptEngine Wrapper - common message DSL"}
  clasew.messages
  (:require   [clasew.core :as as]
              [clasew.utility :as util]
              [clasew.ast-utils :as astu]
              [clojure.java.io :as io]))

(defonce ^:private local-eng as/new-eng)   ; Use engine for this namespace
(def ^:private scrpteval (io/resource "clasew-messages.applescript"))

(def ^:private account-attrs
  #{:acct_name,:acct_emails,:acct_user_name,:acct_user_fullname})

(def account-standard account-attrs)

(def ^:private message-attrs
  #{:msg_subject, :msg_sender, :msg_text, :msg_recipient, :msg_date_recieved,
    :msg_date_sent, :msg_replied?})

(def message-standard message-attrs)

(def ^:private mailbox-attrs
  #{:mb_name, :mb_message_count, :mb_unread_message_count, :mb_owner,
    :mb_account_name})

(def mailbox-standard mailbox-attrs)

;;
;;  Script runner for messages
;;

(defn run-script!
  "Invokes the AppleScript interpreter passing one or more scripts
  to be evaluated"
  [& scripts]
  {:pre [(> (count scripts) 0)]}
  (let [argv (vector (into [] scripts))]
    (with-open [rdr (io/reader scrpteval)]
      (:result (util/clean-result (as/run-ascript! local-eng rdr
                      :reset-binding true
                      :bind-function "clasew_eval"
                      :arguments argv))))))


;;
;; High level DSL functions ---------------------------------------------------
;;

(defn- make-fetch
  [kw standard attrs]
  (let [ia (filter keyword? attrs)]
    {:action :get-messages
     :fetch-type kw
     :args (if (empty? ia) (seq standard) ia)
     :filters (first (rest (astu/filter-forv :filter attrs)))
     :subsets (astu/filter-form attrs)}))

(defn mailboxes
  [& attrs]
  (make-fetch :mailboxes mailbox-standard attrs))

(defn accounts
  [& attrs]
  (make-fetch :accounts account-standard attrs))

(defn messages
  "Fetch messages from application"
  [& attrs]
  (make-fetch :messages message-standard attrs))


(defn send-message
  [msg recipient  & recipients]
  (if (empty? (conj recipients recipient))
    (println "No recipients error"))
  (if (nil? msg)
    (println "No message error"))
  )
