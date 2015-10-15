(ns
  ^{:author "Frank V. Castellucci"
    :doc "Clojure AppleScriptEngine Wrapper - common message DSL"}
  clasew.messages
  (:require   [clasew.core :as as]
              [clasew.utility :as util]
              [clojure.java.io :as io]))

(defonce ^:private local-eng as/new-eng)   ; Use engine for this namespace
(def ^:private scrpteval (io/resource "clasew-messages.applescript"))

(def ^:private message-attrs
  #{:msg_subject, :msg_sender, :msg_text, :msg_recipient, :msg_date_recieved,
    :msg_date_sent, :msg_replied?})

(def message-standard message-attrs)

(def ^:private mailbox-attrs
  #{:mb_name, :mb_message_count, :mb_unread_message_count, :mb_owner,
    :mb_account_name})

(def mailbox-standard mailbox-attrs)

;;
;;  Script runner for indentities
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

(def message (reduce #(assoc %1 %2 nil) {} message-standard))

(def mailbox (reduce #(assoc %1 %2 nil) {} mailbox-standard))

(defn mailboxes
  [filt & attrs]
  )

(defn messages
  "Fetch messages from application"
  [filt & attrs]
  (if (empty? attrs)
    (println "No attributes, defaulting to " message-standard)
    (println "Will fetch attributes " attrs))
  (if (nil? filt)
    (println "Should specify filter")
    (println "Filter supplied " filt))
  )


(defn send-message
  [msg recipient  & recipients]
  (if (empty? (conj recipients recipient))
    (println "No recipients error"))
  (if (nil? msg)
    (println "No message error"))
  )
