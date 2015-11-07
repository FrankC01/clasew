(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - Apple Mail DSL"}
  clasew.mail
  (:require [clasew.mesg-utils :as mesgu]
            [clasew.gen-as :as genas]))

(def mail-messages
  {
   :dstring                 "date string"
   :acct_name               "name",
   :acct_emails             "email addresses",
   :acct_user_name          "user name",
   :acct_user_fullname      "full name",
   :acct_mailboxes          "mailboxes",
   :mb_name                 "name"
   :mb_message_count        "count of messages"
   :mb_unread_message_count "unread count"
   :msg_subject             "subject"
   :msg_sender              "sender"
   :msg_text                "content"
   :msg_recipients          "recipients"
   :msg_date_recieved       "date received"
   :msg_date_sent           "date sent"
   :msg_read                "read status"
   :msg_meeting             "null"
   :msg_replied             "was replied to"
   })

(defn- mail-mapcore-messages
  [termkw]
  (get mail-messages termkw (name termkw)))

(defn script
  [block]
  (genas/ast-consume
   (mesgu/fetch-messages :mail mail-mapcore-messages block)))
