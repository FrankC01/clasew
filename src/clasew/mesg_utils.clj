(ns
  ^{:author "Frank V. Castellucci"
    :doc "Clojure AppleScriptEngine Wrapper - common message DSL utilities"}
  clasew.mesg-utils
  (:require   [clasew.ast-emit :as ast]
              [clasew.ast-utils :as astu]
              [clasew.messages :as mesg]))



(def ^:dynamic *application* nil)
(def ^:dynamic *token-terms* nil)

(defn clasew-bindings
  []
  [*application* *token-terms*])


(def ^:private boxes
  {:outlook "mail folders"
   :mail "mailboxes"})

(def ^:private  acc-list
  {:outlook #{"exchange accounts", "pop accounts", "imap accounts", "ldap accounts"}
   :mail #{"iCloud accounts", "pop accounts", "imap accounts"}})

(defn- email_list
  [fromkw]
  (if (= *application* :mail)
    (ast/xofy-expression
     nil
     (ast/expression
      nil
      ast/getin (ast/term *token-terms* :acct_emails))
     (ast/term nil fromkw))
    (ast/list-of
     nil
      (list (ast/get-statement
             nil
             (ast/xofy-expression
              nil
              (ast/term *token-terms* :acct_emails)
              (ast/term nil fromkw)))))))

;;
;; General purpose handler definitions
;;

(defn- match-block
  "Generates the script caller typically for filters
  e.g. if (my match(arg1,arg2))"
  []
  (ast/routine
   nil :match [:pat :aa]
   (ast/script
    nil :o true
    (ast/property
     nil
     (ast/key-value nil (ast/property-term :f)(ast/term nil :pat)))
    (ast/return
     nil
     (ast/routine-call nil (ast/term nil :f)(ast/term nil :aa))))))

(defn- account_list
  "Generates the account collection routine that returns
  a valid list of accounts"
  []
  (ast/block
   nil
   (match-block)
  (ast/routine
   nil :account_list [:aclist :pat]
   (ast/define-locals nil :alist :acc :acc1)
   (ast/tell
    nil *application*
    (ast/set-statement nil (ast/term nil :alist) ast/empty-list)
    (ast/for-in-expression
     nil
     (ast/term nil :acc) (ast/term nil :aclist)
     (ast/for-in-expression
      nil
      (ast/term nil :acc1) (ast/term nil :acc)
      (ast/if-statement
       nil
       (ast/if-expression
        nil
        (ast/routine-call
         nil
         (ast/term nil :match)
         (ast/term nil :pat) (ast/term nil :acc1))
          (ast/set-statement
           nil
           (ast/eol-cmd nil :alist nil)
           (ast/record-definition
            nil

            ; get account name and folder type properties
            (ast/kv-template *token-terms* :acct_name :acc1)
            (ast/kv-template *token-terms* :acct_user_name :acc1)
            (ast/kv-template *token-terms* :acct_user_fullname :acc1)
            (ast/kv-template *token-terms* :acct_mailboxes :acc1)

            ; account emails
            (ast/key-value
             nil
             (ast/key-term :acct_emails)
             (ast/expression nil (email_list :acc1)))))) nil))))
      (ast/return nil :alist))))


(defn- date-cleaner-return
  "Provides the return clause for a non missing date value condition"
  []
  (ast/return
   nil
   (ast/xofy-expression
    nil
    (ast/term *token-terms* :dstring)
    (ast/term nil :x))))

(defn- email-cleaner-return
  "Provides the return clause for a non missing email address value condition"
  []
  (if (= *application* :outlook)
    (ast/return
     nil
     (ast/xofy-expression
      nil
      (ast/term nil :address)
      (ast/term nil :x)))
    (ast/return
     nil
     (ast/term nil :x))))

(defn- generic-cleaner
  [namekw]
  (ast/routine
   nil namekw [:x]
   (ast/tell
    nil *application*
    (ast/if-statement
     nil
     (ast/if-expression
      nil
      (ast/predicate-condition
       nil
       (ast/term nil :x)
       (ast/predicate-operator astu/EQ)
       (ast/symbol-literal "missing value"))
      (ast/return
       nil
       (ast/string-literal "")))
     nil)
    (condp = namekw
      :emailcleaner (email-cleaner-return)
      :datecleaner  (date-cleaner-return)
      (ast/return nil (ast/term nil :x))))))

(defn- call-clean
  "Calls a cleaner (fnkw) and makes getxofy as argument"
  [fnkw term source]
  (ast/routine-call
   nil
   (ast/term nil fnkw)
   (ast/get-xofy *token-terms* term source)))



(declare predicate-cond)

; TODO - Outlook version

(defn recipient-count-pos-mail
  "Constructs a 'whose' clause for mail recipients list filtering"
  [lnskw op rhsval ownerkw]
  (ast/expression
   nil
   ast/lparen
   (ast/predicate-condition
    nil
    (ast/expression
     nil
     (ast/expression
      *token-terms*
      (ast/xofy-expression nil
                         (ast/term nil :msg_recipients)
                         (ast/term nil ownerkw))
      ast/whose
      (ast/predicate-condition
       nil
       (ast/term nil :address)
       (ast/predicate-operator op)
       (if (string? rhsval)
         (ast/string-literal rhsval)
         (ast/term nil rhsval)))
      ast/rparen))
    (ast/predicate-operator astu/!EQ)
    ast/empty-list)))

(defn recipient-count-pos-outlook
  "Constructs a 'whose' clause for outlook recipients list filtering"
  [lnskw op rhsval ownerkw]
  (ast/expression
   nil
   ast/lparen
   ast/lparen
   (ast/predicate-condition
    nil
    (ast/expression
     nil
     (ast/expression
      *token-terms*
      (ast/xofy-expression nil
                         (ast/term nil :msg_recipients)
                         (ast/term nil ownerkw))
      ast/where
      ast/its
      (ast/predicate-condition
       nil
       (ast/expression
        nil
        (ast/symbol-literal "email address's ")
        (ast/term nil :address))
       (ast/predicate-operator op)
       (if (string? rhsval)
         (ast/string-literal rhsval)
         (ast/term nil rhsval)))
      ast/rparen
      ast/as-list
      ast/rparen))
    (ast/predicate-operator astu/!EQ)
    ast/empty-list)))


(defn indirectors
  "Construct cleaner calls for various attributes"
  [lhskw ownerkw]
  (cond
   ; date handlers
   (or (= lhskw :msg_date_sent) (= lhskw :msg_date_recieved))
    (call-clean :datecleaner lhskw ownerkw)
   ; sender handlers - Need change for outlook (address of (get sender of x)...
   (= lhskw :msg_sender)
    (call-clean :emailcleaner lhskw ownerkw)
   ; recipient handlers
   ; all others
   :else
     (ast/xofy-expression
      *token-terms*
      (ast/term nil lhskw)
      (ast/term nil ownerkw))))

(defn predicate-cond
  "Generates the predicate condition used in filtering tests"
  [[lhskw op rhsval] ownerkw]
  (let [rfn (partial (if (= *application* :mail)
                       recipient-count-pos-mail
                       recipient-count-pos-outlook))]
    (if (= lhskw :msg_recipients)
      (rfn lhskw op rhsval ownerkw)
      (ast/predicate-condition
       nil
       (indirectors lhskw ownerkw)
       (ast/predicate-operator op)
       (if (string? rhsval)
         (ast/string-literal rhsval)
         (ast/term nil rhsval))))))

(defn predicate-args
  "Generates predicate expressions that include predicate conditions"
  [acc ownerkw {:keys [args joins] :as usrfilt}]
  (let [a (conj acc (apply (partial ast/predicate-expressions nil)
                (map #(predicate-cond % ownerkw) args)))]
    (if (empty? joins)
      a
      (let [x (map #(if (= (first %) :and)
                    (ast/and-predicate-join
                      nil
                      (predicate-args [] ownerkw (second %)))
                    (ast/or-predicate-join
                     nil
                     (predicate-args [] ownerkw (second %)))) joins)]
      (into a x) ))))

(defn predicate-builder
  "High level predicate constructor"
  [ownerkw usrfilt]
  (apply (partial ast/predicate-statement nil)
   (predicate-args [] ownerkw usrfilt)))

(defn- build-filter-routine
  "Generates a filter routine/handler"
  [fname argkw usrfilt]
  (ast/routine
   nil
   fname [argkw]
   (ast/tell
    nil *application*
    (ast/return *token-terms* (predicate-builder argkw usrfilt)))))

; Noop filter always returns true

(defn- noop-filter [] (ast/routine nil :noop_filter [:x] (ast/return nil :true)))


;; Support functions
; Functions to support traverse and extract
; filters to replace with handler calls

(defn- refactor-fetch
  "Look for filters to convert to handlers"
  [block]
  (let [x (map #(get % 2) block)
        y (if (not-empty x)
            (conj x (noop-filter)
                  (generic-cleaner :datecleaner)
                  (generic-cleaner :emailcleaner)
                  (generic-cleaner :cleanval))
            (list (generic-cleaner :cleanval)
                  (noop-filter)
                  (generic-cleaner :datecleaner)
                  (generic-cleaner :emailcleaner)))]
  (apply (partial ast/block nil) y)))


(defn- pfunc1
  "Gets appropriate filter and substitues the keyword to the handler
  generated in mapcat-fn"
  [fa [_ ft]]
  (let [r (first (filter #(= (first %) ft) fa))]
    (if r [:filters (second r)] [:filters nil])))

(defn- pfunc
  "Walk the hierarchy looking to substite filters designations"
  [fa bs]
  (let [a (atom [:filters nil])]
    (clojure.walk/prewalk #(do
                             ; Capture the context of the feed
                             (if (and (vector? %) (= (first %) :fetch-type))
                               (do (swap! a assoc 1 (second %))%)
                               %)
                             ; Swizzle the designations
                             (if (and (vector? %) (= (first %) :filters))
                              (pfunc1 fa @a) %))
                            bs)))

(defn- mapcat-fn
  "Mapcat function to collect filter statements"
  [c m]
  (let [x (keyword (str (gensym) "_filter"))]
    (if (:filters m)
      (swap! c conj [(:fetch-type m) x (build-filter-routine x :x (:filters m))]))
    (if (not-empty (:subsets m))
      (mapcat #(mapcat-fn c %) (:subsets m)))))

(defn- nest-block
  [ablck iblck]
  (assoc ablck :subsets (list iblck)))

(defn- dropargs
  [imap args]
  (assoc imap :args args))

(defn- setblock
  [block]
  (if (= (:fetch-type block) :accounts)
    block
    (if (= (:fetch-type block) :mailboxes)
      (nest-block (dropargs (mesg/accounts) '(:acct_name)) block)
      (nest-block (dropargs (mesg/accounts) '(:acct_name))
                  (nest-block (dropargs (mesg/mailboxes) '(:mb_name)) block)))))

(defn- mcat
  "Traverse input block to substitute inline filters
  with handler call"
  [iblock]
  (let [x (atom [])]
    (mapcat #(mapcat-fn x %) (list iblock))
    [@x (setblock (pfunc @x iblock))]))

;;
;; Fetch meta data
;;

(defn- meta-pattern
  [indirs block]
  (partial ((:fetch-type block) indirs) (assoc block :subsets nil)
   (if (not-empty (:subsets block))
     (map #(meta-pattern indirs %) (:subsets block))
     '())))

;;
;; Common builders and special handlers
;;

(defn- nest-dispatch
  [coll args]
  (if (not-empty coll)
    (apply (partial ast/block nil) (for [x coll] (x args)))
    (ast/block nil)))

(defn- contains-type?
  [kw args]
  (some? (some #(= % kw ) args)))

;;
;; Specific handlers
;;

(defn- generic-date-handler
  "Generic special handler"
  [targ dtype dtypekey src]
  (ast/set-statement
   nil
   (ast/term nil targ)
   (ast/eor-cmd
    nil targ nil
    (ast/record-definition
     nil
     (ast/key-value
      nil
      (ast/key-term dtype)
      (call-clean :datecleaner dtype src))))))

(defn- generic-email-handler
  "Generic email handler"
  [targ dtype dtypekey src]
  (ast/set-statement
   nil
   (ast/term nil targ)
   (ast/eor-cmd
    nil targ nil
    (ast/record-definition
     nil
     (ast/key-value
      nil
      (ast/key-term dtype)
      (call-clean :emailcleaner dtype src))))))

(def ^:private mail-restrictions #(and
            (not= % :msg_meeting)
            (not= % :msg_sender)
            (not= % :msg_recipients)
            (not= % :msg_date_sent)
            (not= % :msg_date_recieved)))

(def ^:private outlook-restrictions #(and
            (not= % :msg_sender)
            (not= % :msg_recipients)
            (not= % :msg_date_sent)
            (not= % :msg_date_recieved)))

(defn- strip-msg-args
  [args]
  (filter
   (condp = *application*
     :mail mail-restrictions
     :outlook outlook-restrictions) args))

(defn- date-handler
  "Builds extract statement specific to date properties"
  [tf dtype targ src]
  (if tf
    (generic-date-handler targ dtype :dstring src)
    (ast/block nil)))

(defn- sender-handler
  "Builds extract statement specific to sender email address"
  [tf dtype targ src]
  (if tf
    (generic-email-handler targ dtype :address src)
    (ast/block nil)))

(defn- recipient-handler
  "Builds loop construct to pull each message recipient email
  address"
  [tf dtype targ src]
  (if tf
    (ast/block
     nil
     (ast/define-locals :reclist :recrec :recloop)
     (ast/set-statement nil (ast/term nil :reclist) ast/empty-list)
     (ast/for-in-expression
      nil
      (ast/term nil :recloop)
      (ast/xofy-expression
       *token-terms*
       (ast/term nil dtype) (ast/term nil src))
      (ast/set-statement
       nil
       (ast/eol-cmd nil :reclist nil)
       (ast/record-definition
        nil
        (ast/key-value
         nil
         (ast/key-term :recipient_email)
         (if (= *application* :outlook)
           (ast/xofy-expression
            nil
            (ast/term nil :address)
            (ast/get-xofy *token-terms* :acct_emails :recloop))
           (ast/get-xofy *token-terms* :address :recloop))))))
     (ast/set-extend-record targ dtype :reclist))
    (ast/block nil)))

;;
;; Builders
;;

(defn- build-messages
  "Builds message fetch control"
  [{:keys [args filters]} cntrl {:keys [source accum property]}]
  (let [has-recp (contains-type? :msg_recipients args)
        has-send (contains-type? :msg_sender args)
        has-drec (contains-type? :msg_date_recieved args)
        has-dsen (contains-type? :msg_date_sent args)
        clean-args (strip-msg-args args)]
    (ast/block
     nil
     (ast/define-locals nil :msgloop :msgrec :msglist)
     (ast/set-statement nil (ast/term nil :msglist) ast/empty-list)
     (ast/for-in-expression
      nil
      (ast/term nil :msgloop)
      (ast/get-xofy nil property source)
      (ast/if-statement
       nil
       (ast/if-expression
        nil
        (ast/routine-call
         nil
         (ast/term nil :match)
         (ast/term nil (or filters :noop_filter ))
         (ast/term nil :msgloop))
        (ast/record-fetch *token-terms* clean-args :msgrec :msgloop)
        ; Sender special handler
        (sender-handler has-send :msg_sender :msgrec :msgloop)
        ; Recipient special handler
        (recipient-handler has-recp :msg_recipients :msgrec :msgloop)
        ; Date special handler
        (date-handler has-dsen :msg_date_sent :msgrec :msgloop)
        (date-handler has-drec :msg_date_recieved :msgrec :msgloop)
        (ast/set-statement
         nil
         (ast/eol-cmd nil :msglist nil)
         (ast/term nil :msgrec))) nil))
     (ast/set-extend-record accum :mb_messages :msglist))))

(defn- build-mailboxes
  "Builds mailbox  fetch control"
  [{:keys [args filters]} cntrl {:keys [source accum property] :as props}]
  (ast/block
   nil
   (ast/define-locals nil :mbloop :mrec :mlist)
   (ast/set-statement nil (ast/term nil :mlist) ast/empty-list)
   (ast/for-in-expression
    nil
    (ast/term nil :mbloop)
    (ast/get-statement
     nil
      (ast/xofy-expression
       nil
       (ast/term nil property)
        (ast/term nil source)))
    (ast/if-statement
     nil
     (ast/if-expression
      nil
      (ast/routine-call
       nil
       (ast/term nil :match)
       (ast/term nil (or filters :noop_filter ))
       (ast/term nil :mbloop))
      (ast/record-fetch *token-terms* args :mrec :mbloop)
      (nest-dispatch cntrl (assoc props
                           :source   :mbloop
                           :property :messages
                           :accum    :mrec
                           ))
      (ast/set-statement
       nil
       (ast/eol-cmd nil :mlist nil)
       (ast/term nil :mrec))) nil))
   (ast/set-extend-record accum :acct_mailboxes :mlist)))

(defn- account-block
  "Builds the account block for setting up and fetching
  account values"
  [{:keys [args]} cntrl {:keys [source accum] :as props}]
  (ast/block
   nil
   (ast/define-locals nil :indx)
   (ast/for-in-expression
    nil
    (ast/term nil :indx)
    (ast/term nil source)
    (ast/record-fetch nil args accum :indx)
    (nest-dispatch cntrl (assoc props
                           :source :indx
                           :property :acct_mailboxes))
    (ast/set-statement
     nil
     (ast/eol-cmd nil :alist nil)
     (ast/term nil accum)))))

(defn- term-gen
  [coll]
  (map #(ast/term nil %) coll))

(defn- build-account
  "Called when account information is requested. May contain
  filtered operation. Embeds any children as defined by
  source DSL"
  [accblock cntrl {:keys [source accum] :as args}]
  (ast/block
   nil
   (ast/define-locals nil source accum :alist)
   (ast/set-statement nil (ast/term nil :alist) ast/empty-list)
   (ast/set-statement
    nil
    (ast/term nil source)
    (ast/routine-call
     nil
     (ast/term nil :account_list)
     (ast/list-of nil (term-gen (*application* acc-list)))
     (ast/term nil (or (:filters accblock) :noop_filter ))))
   (account-block accblock cntrl args)
   (ast/set-statement
    nil
    (ast/term nil :results)
    (ast/term nil :alist))))

(def ^:private builder-lookup
  {:accounts   build-account
   :mailboxes  build-mailboxes
   :messages   build-messages})

(defn- build-get-messages
  [block]
  ; First build filter structures and metapattern information
  ; mcat converts the input into handler collection and new block format
  ; refactor-fetch creates the handler body references
  ; meta-pattern determines depth constraints and subset order
  (let [[filter-coll imap] (mcat block)]  ; Extract filters, reset block
    ; Setup the script header and handler routines
     (ast/block
      nil
      (account_list)
      (refactor-fetch filter-coll)   ; handler list to filter-block
      (ast/tell nil *application*
                (ast/define-locals nil :results)
                ((meta-pattern builder-lookup imap) {:source :aclist
                                                           :accum  :arec})
                (ast/return nil :results)))))

(defn fetch-messages
  "Main entry point binds target specific information
  then builds fetch routines"
  [appbiding token-binding block]
  (with-bindings {#'*application* appbiding
                  #'*token-terms* token-binding}
    (build-get-messages block)))


