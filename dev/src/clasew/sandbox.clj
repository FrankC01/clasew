(ns
  ^{:author "Frank V. Castellucci"
      :doc "spike space"}
  clasew.sandbox
  (:require [clojure.pprint :refer :all]
            [clasew.messages :as mesg]
            [clasew.mesg-utils :as mesgu]
            [clasew.ast-emit :as ast]
            [clasew.ast-utils :as astu]
            [clasew.gen-as :as genas]
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

;; Sample 1
;; Fetch account information

#_(println (mail/script (mesg/accounts
                       (astu/filter :acct_name astu/EQ "Google")
                       (mesg/mailboxes
                        (astu/filter :mb_name astu/EQ "INBOX"
                                     :mb_unread_message_count astu/GT 0)
                        (mesg/messages :msg_subject)
                        ))))

#_(p (run-sample :mail (mesg/accounts
                       (astu/filter :acct_name astu/EQ "Google")
                       (mesg/mailboxes
                        (astu/filter :mb_name astu/EQ "INBOX"
                                     :mb_unread_message_count astu/GT 0)
                        (mesg/messages :msg_subject)
                        ))))

#_(p (run-sample :outlook (mesg/accounts
                       (astu/filter :acct_name astu/EQ "Axiom1inc")
                       (mesg/mailboxes
                        (astu/filter :mb_name astu/EQ "Inbox"
                                     :mb_unread_message_count astu/GT 0)
                        (mesg/messages :msg_subject)
                        ))))

(def s-fetch-accounts (mesg/accounts))

;; Uncomment to run

;(p (run-sample :mail s-fetch-accounts))
;(p (run-sample :outlook s-fetch-accounts))

;; Sample 2
;; Fetch mailbox information for each account
;; clasew will inject higher levels automatically
;; i.e. These all create the same script
;; (mesg/mailboxes)
;; (mesg/accounts (mesg/mailboxes))

(def s-fetch-mailboxes (mesg/mailboxes))

;; Uncomment to run

;(p (run-sample :mail s-fetch-mailboxes))
;(p (run-sample :outlook s-fetch-mailboxes))

;; Sample 3
;; Get all messages from all accounts mail boxes
;; clasew will inject higher levels automatically
;; i.e. These all create the same script
;; (mesg/messages)
;; (mesg/mailboxes (mesg/messages))
;; (mesg/accounts (mesg/mailboxes (mesg/messages)))
;;
;; CAUTION - potentially massive cycles!!!

(def s-fetch-messages-1 (mesg/messages))
(def s-fetch-messages-2 (mesg/accounts (mesg/mailboxes (mesg/messages))))

;; Uncomment to run
;; CAUTION - potentially massive cycles!!!

;(p (run-sample :mail s-fetch-messages-1))
;(p (run-sample :outlook s-fetch-messages-1))

;; Sample 4
;; Limit the properties of what is returned by putting
;; explicit type keywords in the context of the information
;; type
;; For example, if I only want account names:

(def s-account-name-fetch (mesg/accounts :acct_name :acct_user_name))

;(p (run-sample :mail s-account-name-fetch))
;(p (run-sample :outlook s-account-name-fetch))

;; Or abbreviated account and mailbox. Obviously also applicable
;; to messages

(def s-account-mailbox-name-fetch
  (mesg/accounts :acct_name :acct_user_name
                 (mesg/mailboxes :mb_name :mb_unread_message_count)))


;(p (run-sample :mail s-account-mailbox-name-fetch))
;(p (run-sample :outlook s-account-mailbox-name-fetch))

;;;;; TODO --- Dynamic Filter call for account_list

;; Tests

#_(p (mesg/run-script!
          (outlook/script
           (mesg/accounts
            (astu/filter :acct_name astu/EQ "Axiom1inc")
            (mesg/mailboxes
             (astu/filter :mb_name astu/EQ "Deleted Items")
             (mesg/messages :msg_subject
                            :msg_sender
                            :msg_recipients
                            :msg_date_sent
                            :msg_date_recieved))))
           ))

#_(p (mesg/run-script!
          (outlook/script
           (mesg/accounts
            (astu/filter :acct_name astu/EQ "Axiom1inc")
            (mesg/mailboxes
             (astu/filter :mb_name astu/EQ "Inbox")
             (mesg/messages))))))

;(p (mesg/run-script! (mail/script (mesg/accounts (mesg/mailboxes (mesg/messages))))))

#_(println
          (outlook/script
           (mesg/accounts
            (astu/filter :acct_name astu/EQ "Axiom1inc")
            (mesg/mailboxes
             (astu/filter :mb_name astu/EQ "Inbox")
             (mesg/messages
              (astu/filter :msg_sender astu/CT "shaun")
                            :msg_subject
                            :msg_read
                            :msg_sender
                            :msg_recipients
                            :msg_date_sent
                            :msg_date_recieved
              ))))
           )

#_(println
          (mail/script
           (mesg/accounts
            (astu/filter :acct_name astu/EQ "Google")
            (mesg/mailboxes
             (astu/filter :mb_name astu/EQ "INBOX")
             (mesg/messages (astu/filter
                             :msg_sender astu/CT "google.com"
                             (astu/and :msg_recipients astu/CT "fv.cast"))
                            :msg_subject
                            :msg_sender
                            :msg_recipients
                            :msg_date_sent
                            :msg_date_recieved))))
           )

