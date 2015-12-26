(ns
  ^{:author "Frank V. Castellucci"
      :doc "Email Messages Examples (Multi-Application)"}
  clasew.examples9
  (:require [clojure.pprint :refer :all]
            [clasew.messages :as mesg]
            [clasew.ast-utils :as astu]
            [clasew.outlook :as outlook]
            [clasew.mail :as mail]))

(def p pprint)

(def mail-apps
  {:mail     #(mail/script %)
   :outlook  #(outlook/script %)})

(defn run-sample
  "Calls the target application script generator to create
  script as defined by request and then executes the results:
  app - Defines target app. Can be :mail or :outlook
  request - result of calling '(mesg/xxxxx)'"
  [app request]
  (mesg/run-script! ((app mail-apps) request)))


; *********** Fetch Examples ***********************

;;;; Sample 1
;; Fetch account information

(def s-fetch-account-info (mesg/accounts))

;; Uncomment either run command

;(p (run-sample :mail s-fetch-account-info))
;(p (run-sample :outlook s-fetch-account-info))

;;;; Sample 2
;; Fetch mailbox information for each account
;; clasew will inject higher levels automatically
;; i.e. These all create the same script
;; (mesg/mailboxes) -- Implicit
;; (mesg/accounts (mesg/mailboxes)) -- Explicit

(def s-fetch-mailbox-info (mesg/mailboxes))

;; Uncomment either run command

;(p (run-sample :mail s-fetch-mailbox-info))
;(p (run-sample :outlook s-fetch-mailbox-info))

;;;; Sample 3
;; Get ALL messages from ALL accounts mail boxes
;; clasew will inject higher levels automatically
;; i.e. These all create the same script
;; (mesg/messages)
;; (mesg/mailboxes (mesg/messages))
;; (mesg/accounts (mesg/mailboxes (mesg/messages)))
;;
;; CAUTION - potentially massive!!!
;; CAUTION - SEE FILTERING OPTIONS BELOW!!!

(def s-fetch-messages (mesg/messages))

;; Uncomment either run command AT YOUR OWN RISK!!!
;; CAUTION - potentially massive!!!

;(p (run-sample :mail s-fetch-messages))
;(p (run-sample :outlook s-fetch-messages))


; *********** SUBSET INFORMATION EXAMPLES ***********************

;;;; Sample 4
;; Subset of properties is returned by putting
;; explicit type keywords in the context of the information
;; type
;; For example, if I only want account name and associated user name:

(def s-fetch-account-name (mesg/accounts :acct_name :acct_user_name))

;; Uncomment either run command

;(p (run-sample :mail s-fetch-account-name))
;(p (run-sample :outlook s-fetch-account-name))

;;;; Sample 5
;; Or abbreviated account and mailbox. Obviously also applicable
;; to messages
;; This one is good to get information to use in filtering

(def s-fetch-account-and-mailbox-name
  (mesg/accounts :acct_name :acct_user_name
                 (mesg/mailboxes :mb_name :mb_unread_message_count)))

;; Uncomment either run command

;(p (run-sample :mail s-fetch-account-and-mailbox-name))
;(p (run-sample :outlook s-fetch-account-and-mailbox-name))

; *********** Filter Examples ***********************

;;;; Sample 6
;; Filtered account fetch. This requires knowledge about
;; the account (see Sample 1) to substitute a valid name.

(def s-fetch-filtered-account
  (mesg/accounts (astu/filter :acct_name astu/EQ "ACCOUNT NAME HERE")))

;; Uncomment either run command

;(p (run-sample :mail s-fetch-filtered-account))
;(p (run-sample :outlook s-fetch-filtered-account))


;;;; Sample 7
;; Filtered mailbox fetch. This may require knowledge about
;; the mailboxes (see Sample 2) to substitute a valid name.

(def s-fetch-filtered-mailbox
  (mesg/mailboxes (astu/filter :mb_name astu/EQ "MAILBOX NAME HERE")))

;; Uncomment either run command

;(p (run-sample :mail s-fetch-filtered-mailbox))
;(p (run-sample :outlook s-fetch-filtered-mailbox))

;;;; Sample 8
;; Filtered mailbox fetch. This does not require knowledge about
;; the mailboxes as the filter is applied to the generic unread count.

(def s-fetch-filtered-unread-mailbox
  (mesg/mailboxes (astu/filter :mb_unread_message_count astu/GT 0)))

;; Uncomment either run command

;(p (run-sample :mail s-fetch-filtered-unread-mailbox))
;(p (run-sample :outlook s-fetch-filtered-unread-mailbox))

;;;; Sample 9
;; Filtered message fetch. A complex filter to narrow a specific
;; account (see Sample 6) with specific mailbox (see Sample 7) and
;; a filter on partial sender information.

(def s-fetch-filtered-message
  (mesg/accounts
   (astu/filter :acct_name astu/EQ "ACCOUNT NAME HERE")
   (mesg/mailboxes
    (astu/filter :mb_name astu/EQ "MAILBOX NAME HERE")
    (mesg/messages
     (astu/filter :msg_sender astu/CT "PARTIAL SENDER INFO HERE")
     :msg_subject
     :msg_read
     :msg_sender
     :msg_recipients
     :msg_date_sent
     :msg_date_recieved))))

;; Uncomment either run command

;(p (run-sample :mail s-fetch-filtered-message))
;(p (run-sample :outlook s-fetch-filtered-message))

; *********** Send Messages Examples ****************

;;;; Sample 10
;; Send an email message and let the called application
;; use the default email account to determine who the 'sender' is
;; Substitute recipients with valid data

(def s-send-message-default-sender
  (mesg/send-message
   {:msg_recipients ["RECIPIENT1@SOMEWHERE.COM"
                     "RECIPIENT1@SOMEWHERE.COM"]
    :msg_text "This is the test"
    :msg_subject "Hey, look at this"}))

;; Uncomment either run command

;(p (run-sample :mail s-send-message-default-sender))
;(p (run-sample :outlook s-send-message-default-sender))

;;;; Sample 11
;; Send an email message using an account filter
;; to resolve 'sender' email account
;; Substitute sender and recipients with valid data

(def s-send-message-from-filtered-account
  (mesg/send-message
   {:msg_sender (astu/filter :acct_name astu/EQ "ACCOUNT NAME HERE")
    :msg_recipients ["RECIPIENT1@SOMEWHERE.COM"
                     "RECIPIENT1@SOMEWHERE.COM"]
    :msg_text "This is the test"
    :msg_subject "Hey, look at this"}))

;; Uncomment either run command

;(p (run-sample :mail s-send-message-from-filtered-account))
;(p (run-sample :outlook s-send-message-from-filtered-account))

;;;; Sample 12
;; Send an email message using an explicit sender
;; Substitute sender and recipients with valid data

(def s-send-message-from-explicit-sender
  (mesg/send-message
   {:msg_sender "SENDER@SOMEWHERE.COM"
    :msg_recipients ["RECIPIENT1@SOMEWHERE.COM"
                     "RECIPIENT1@SOMEWHERE.COM"]
    :msg_text "This is the test"
    :msg_subject "Hey, look at this"}))

;; Uncomment either run command

;(p (run-sample :mail s-send-message-from-explicit-sender))
;(p (run-sample :outlook s-send-message-from-explicit-sender))


