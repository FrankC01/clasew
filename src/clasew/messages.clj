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
  #{:acct_type,:acct_name,:acct_emails,:acct_user_name,:acct_user_fullname})

(def account-standard account-attrs)

(def ^:private mailbox-attrs
  #{:mb_name, :mb_message_count, :mb_unread_message_count})

(def mailbox-standard mailbox-attrs)

(def ^:private message-attrs
  #{:msg_subject, :msg_sender, :msg_text, :msg_recipients, :msg_date_recieved,
    :msg_read :msg_date_sent, :msg_replied})

(def message-standard message-attrs)



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
;; High level DSL functions - Fetch ------------------------------------------------
;;

(defn- make-fetch
  [kw standard args]
  (let [ia (filter keyword? args)]
    {:action :get-messages
     :fetch-type kw
     :args (if (empty? ia) (seq standard) ia)
     :filters (first (rest (astu/filter-forv :filter args)))
     :subsets (astu/filter-form args)}))

(defn mailboxes
  "Prepares the script for retrieving attributes of mailboxes from the message source
  application along with any additional sub-attributes. Also supports mailbox filtering."
  [& args]
  (make-fetch :mailboxes mailbox-standard args))

(defn accounts
  "Prepares the script for retrieving attributes of accounts from the message source
  application along with any additional sub-attributes. Also supports account filtering."
  [& args]
  (make-fetch :accounts account-standard args))

(defn messages
  "Prepares the script for retrieving attributes of messages from the message source
  application. Also supports message filtering."
  [& args]
  (make-fetch :messages message-standard args))

;;
;; High level DSL functions - Send ------------------------------------------------
;;

;; Message template

;(def send-message-template
;  {:msg_sender     nil ; email string or account filter to get email address
;   :msg_recipients []  ; currently support To: - A vector of email addresses
;   :msg_subject    nil ; String
;   :msg_text       nil ; String
;   })


(defn send-message
  [msg]
  {:action :send-message
   :filters nil
   :message msg})

