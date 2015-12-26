(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - Outlook messages support"}
  clasew.outlook-m
  (:require [clasew.mesg-utils :as mesgu]
            [clasew.gen-as :as genas]))


(def outlook-messages
  {
   :dstring                 "date string"
   :out_message_mb          "outgoing message",
   :to_recipient            "to recipient"
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
   :msg_text                "content";"plain text content"
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

(defn get-messages
  [block]
  (genas/ast-consume
   (mesgu/fetch-messages :outlook outlook-mapcore-messages block)))

(defn send-message
  [block]
  (genas/ast-consume
   (mesgu/send-message :outlook outlook-mapcore-messages block)))
