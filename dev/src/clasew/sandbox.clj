(ns
  ^{:author "Frank V. Castellucci"
      :doc "spike space"}
  clasew.sandbox
  (:require [clojure.pprint :refer :all]
            [clasew.messages :as mesg]
            [clasew.mesg-utils :as mesgu]
            [clasew.ast-emit :as ast]
            [clasew.ast-utils :as astu]
            [clasew.outlook-m :as outm]
            [clasew.gen-as :as gold]
            [clasew.gen-asmm :as gen]))

(def p pprint)

(def t0 (mesgu/fetch-messages
          :outlook
          outm/outlook-mapcore-messages
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
     :msg_date_recieved)))))

;(p t0)
(println (time (gen/ast-consume t0)))


(comment

  (def platform {
    :name (System/getProperty "os.name"),
    :version (System/getProperty "os.version"),
    :arch (System/getProperty "os.arch")})

  (vals platform)
)


