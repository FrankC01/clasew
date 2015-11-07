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


;;;;; TODO --- Dynamic Filter call for account_list

;; Tests

#_(p (mesg/run-script! (genas/ast-consume
          (outlook/script
           (mesg/accounts (astu/filter :acct_name astu/EQ "Planview") )))))

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
             (astu/filter :mb_name astu/EQ "Deleted Items")
             (mesg/messages
              (astu/filter :msg_sender astu/EQ "frank.castellucci@axiom1inc.com"
                           (astu/and :msg_recipients astu/CT "gmail.com"))
              :msg_subject
                            :msg_sender
                            :msg_recipients
                            :msg_date_sent
                            :msg_date_recieved))))
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

