(ns
  ^{:author "Frank V. Castellucci"
    :doc "Clojure AppleScriptEngine Wrapper - common message DSL"}
  clasew.messages
  (:require   [clasew.core :as as]
              [clasew.utility :as util]
              [clasew.gen-as :as genas]
              [clasew.ast-emit :as ast]
              [clasew.ast-utils :as astu]
              [clojure.java.io :as io])
  (:refer-clojure :rename {filter cfilter
                           or     cor
                           and    cand}))

(defonce ^:private local-eng as/new-eng)   ; Use engine for this namespace
(def ^:private scrpteval (io/resource "clasew-messages.applescript"))

(def ^:private message-attrs
  #{:subject, :sender, :recipient, :date_recieved, :date_sent, :replied?})

(def message-standard message-attrs)

(def ^:private mailbox-attrs
  #{:mailbox_name, :mailbox_count, :mailbox_unread_count, :mailbox_parent,
    :mailbox_account_name})

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

(def message
  {:from    nil
   :subject nil
   :text    nil})

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
