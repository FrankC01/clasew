(ns
  ^{:author "Frank V. Castellucci"
    :doc "Clojure AppleScriptEngine Wrapper - common message DSL utilities"}
  clasew.mesg-utils
  (:require   [clasew.utility :as util]
              [clasew.ast-emit :as ast]
              [clasew.ast-utils :as astu]
              [clasew.messages :as mesg]))



(def ^:dynamic *application* nil)
(def ^:dynamic *token-terms* nil)
(def ^:dynamic *build-for* nil)

(def ^:private boxes
  {:outlook "mail folders"
   :mail "mailboxes"})

(def ^:private  acc-list
  {:outlook #{"exchange accounts", "pop accounts", "imap accounts", "ldap accounts"}
   :mail #{"iCloud accounts", "pop accounts", "imap accounts"}})

;;
;; General purpose functions
;;

(defn- tell-app
  [& expressions]
  (apply ast/tell nil *application* expressions))

(defn- builder-reducer
  "Used to reduce all the callers into accumulator"
  [acc targ]
  (if (not-empty (second targ)) (conj acc (second targ)) acc))

(defn- flatten-stack
  "Flattens resulting stack for generation"
  [fmap]
  (apply (partial ast/block nil)
         (flatten (reduce builder-reducer [] fmap))))

(def ^:private routine-frame {:script [] :filter [] :routine [] :body []})

(defn- routines
  "Builds routine stack"
  [coll]
  (reduce #(%2 %1) routine-frame coll))
;;
;; General purpose routine generators
;;

(defn- script-body
  "Build the main 'tell' routine for the script whose
  only job is to call the starting handler"
  [handlerkw fmap]
  (update-in
   fmap
   [:body] conj
   (tell-app
    (ast/return
     nil
     (ast/routine-call
      nil
      (ast/term nil handlerkw))))))

(defn- match-script
  "Generates the script caller typically for filters
  e.g. if (my match(arg1,arg2))"
  [fmap]
  (update-in
   fmap [:script] conj
   (ast/routine
    nil :match [:pat :aa]
    (ast/script
     nil :o true
     (ast/property
      nil
      (ast/key-value nil (ast/property-term :f)(ast/term nil :pat)))
     (ast/return
      nil
      (ast/routine-call nil (ast/term nil :f)(ast/term nil :aa)))))))

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
   (tell-app
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

(defn- include-cleaners
  "Builds cleaner handlers"
  [vcleans fmap]
  (reduce #(update-in %1 [:filter] conj (generic-cleaner %2)) fmap vcleans))

; Noop filter always returns true

(defn- noop-filter
  [fmap]
  (update-in fmap [:filter] conj
             (ast/routine nil :noop_filter [:x] (ast/return nil :true))))


;;
;; Fetch handlers
;;

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

(defn- fetch-account-set
  "Sets the results of account filter for fetching"
  [acc src actype]
  (ast/set-statement
   nil
   (ast/eol-cmd nil acc nil)
   (ast/record-definition
    nil
    ; get account name and folder type properties
    (ast/key-value nil (ast/key-term actype) (ast/term nil actype))
    (ast/kv-template *token-terms* :acct_name src)
    (ast/kv-template *token-terms* :acct_user_name src)
    (ast/kv-template *token-terms* :acct_user_fullname src)
    (ast/kv-template *token-terms* :acct_mailboxes src)

    ; account emails
    (ast/key-value
     nil
     (ast/key-term :acct_emails)
     (ast/expression nil (email_list src))))))

(defn- send-account-set
  "Sets the results of account filter for sending"
  [acc src]
  (ast/set-statement
   nil
   (ast/eol-cmd nil acc nil)
   (condp = *application*
     :mail (ast/get-xofy
            nil ast/first-of (ast/get-xofy *token-terms* :acct_emails src))
     :outlook (ast/term nil src))))

(defn- account-list
  "Generates the account collection routine that returns
  a valid list of accounts"
  [fmap]
  (update-in fmap [:routine] conj
  (ast/routine
   nil :account_list [:aclist :pat]
   (ast/define-locals nil :alist :acc :acc1 :acct_type)
   (tell-app
    (ast/set-statement nil (ast/term nil :alist) ast/empty-list)
    (ast/for-in-expression
     nil
     (ast/term nil :acc) (ast/term nil :aclist)
     (ast/set-statement
      nil
      (ast/term nil :acct_type)
      (ast/xofy-expression
       nil
       ast/first-of
       (ast/term nil :acc)))
     (ast/for-in-expression
      nil
      (ast/term nil :acc1) (ast/xofy-expression
                            nil ast/second-of (ast/term nil :acc))
      (ast/if-statement
       nil
       (ast/if-expression
        nil
        (ast/routine-call
         nil
         (ast/term nil :match)
         (ast/term nil :pat) (ast/term nil :acc1))
        (condp = *build-for*
          :fetch (fetch-account-set :alist :acc1 :acct_type)
          :send (send-account-set :alist :acc1)))
       nil))))
   (ast/return nil :alist))))

(defn- call-clean
  "Calls a cleaner (fnkw) and makes getxofy as argument"
  [fnkw term source]
  (ast/routine-call
   nil
   (ast/term nil fnkw)
   (ast/get-xofy *token-terms* term source)))

;;; Fetch Content

(declare predicate-cond)

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
       (cond
        (string? rhsval) (ast/string-literal rhsval)
        (number? rhsval) (ast/numeric-literal rhsval)
        :else (ast/term nil rhsval))))))

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
   (tell-app
    (ast/return *token-terms* (predicate-builder argkw usrfilt)))))

(defn- frame-build-filter
  [fmap usrfilt]
  (let [fname (str (gensym) "_filter")]
    [(update-in fmap [:filter] conj (build-filter-routine fname :x usrfilt))
     fname]))

;;
;; Specific handlers
;;

(defn- generic-special-fetch
  "Creates specialized fetch for sender and date/time message types"
  [dtype targ src clnkw]
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
      (call-clean clnkw dtype src))))))

(defn- recipient-special-fetch
  "Builds loop construct to pull each message recipient email
  address"
  [dtype targ src]
  (ast/block
   nil
   (ast/define-locals nil :reclist :recloop)
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
   (ast/set-extend-record targ dtype :reclist)))

;;
;; Builders
;;

(def ^:private special-types
  #{:msg_sender :msg_recipients :msg_date_sent :msg_date_recieved})

(def ^:private special-type-args
  {:msg_sender          #(generic-special-fetch % :msgrec :msgloop :emailcleaner)
   :msg_recipients      #(recipient-special-fetch % :msgrec :msgloop)
   :msg_date_sent       #(generic-special-fetch % :msgrec :msgloop :datecleaner)
   :msg_date_recieved   #(generic-special-fetch % :msgrec :msgloop :datecleaner)})

(defn- frame-filter
  [fmap {:keys [filters] :as block}]
  (if filters
    (frame-build-filter fmap filters)
    [fmap :noop_filter]))


(defn- build-specials-fetch
  "Calls the special fetch builders for special types
  uses 'mapv' to avoid lazy sequence issues and dynamic bindings"
  [args]
  (apply (partial ast/block nil)
         (into [] (mapv #((% special-type-args) %)
               (filter (into #{} args) special-types)))))

(declare frame-next-block)

(defn- frame-build-messages
  "Builds the 'fetch_messages' handler"
  [fmap {:keys [args filters] :as block}]
  (let [[zmap mfilt] (frame-filter fmap block)]
    (update-in
     zmap
     [:routine] conj
     (ast/routine
      nil :fetch_messages [:accum :rsrc]
      (tell-app
       (ast/define-locals nil :msgloop :msgrec :msglist)
       (ast/set-statement nil (ast/term nil :msglist) ast/empty-list)
       (ast/for-in-expression
        nil
        (ast/term nil :msgloop)
        (ast/get-xofy nil :messages :rsrc)
        (ast/if-statement
         nil
         (ast/if-expression
          nil
          (ast/routine-call
           nil
           (ast/term nil :match)
           (ast/term nil mfilt)
           (ast/term nil :msgloop))
          (ast/record-fetch *token-terms*
                            (remove special-types args) :msgrec :msgloop)
          (build-specials-fetch args)
          (ast/set-statement
           nil
           (ast/eol-cmd nil :msglist nil)
           (ast/term nil :msgrec))) nil))
       (ast/set-extend-record :accum :mb_messages :msglist)
       (ast/return nil :accum))))))

(defn- if-then-else-nil-builder
  "Ease of use function to setup if statement with filtered validation
  and the AST if true:
  filt - The filter keyword identifier to call
  obj  - The object to call filter with
  pass-block - The pass filter AST"
  [filt obj pass-block]
  (ast/if-statement
   nil
   (ast/if-expression
    nil
    (ast/routine-call
     nil
     (ast/term nil filt)
     (ast/term nil obj))
    pass-block)
    nil))

(defn- if-pass-block
  "Ease of use function to setup a pass block common to usage
  set-to   - The target of a set statement
  args     - arguments to fetch
  objkw    - the source object to fetch from
  callback - callback AST defined elsewhere"
  [set-to args accum objkw callback]
  (ast/block
   nil
   (ast/set-statement
    nil
    (ast/term nil set-to)
    (apply (partial ast/record-definition nil)
           (mapv #(ast/kv-template *token-terms* % objkw) args)))
   (ast/set-statement
    nil (ast/eol-cmd nil accum nil) callback)))

(defn- reuse-callback-diff-arg
  "Low level update of an argument to a routine call so as to
  reuse the construct without creating duplicate handlers
  mmap - the base ast for the routine call
  posi - the argument of the call position to update
  targkw - the target keyword to be updated
  valu - the value to set the targkw to in the map"
  [mmap posi targkw valu]
  (if (= (:type mmap) :term)
    mmap
    (update-in
     (update-in mmap [:routine-arguments] #(into [] %))
     [:routine-arguments] #(update-in % [posi] assoc targkw valu))))

(defn- frame-flatten-mailboxes
  "Flatten mailbox return. Include if filter passes then process messages
  if any to do or recurses on self to dig deeper"
  [fmap {:keys [args filters] :as block}]
  (let [[zmap mfilt] (frame-filter fmap block)
        [zfmap1 callblk1] (frame-next-block
                           zmap block {:source :par :accum :mrec})
        callblk2 (reuse-callback-diff-arg callblk1 1 :to-value :indx)]
  (update-in
   zfmap1
   [:routine] conj
   (ast/routine
    nil :flatten_mailboxes [:accum :par]
    (tell-app
     (ast/define-locals nil :mrec :indx)
     ; Check inbound parent for match
     (if-then-else-nil-builder
      mfilt :par (if-pass-block :mrec args :accum :par callblk1))
    (ast/for-in-expression
     nil
     (ast/term nil :indx)
     (ast/get-xofy *token-terms* :acct_mailboxes :par)
     (ast/if-statement
      nil
      (ast/if-expression
       nil
       (ast/predicate-condition
        nil
        (ast/get-xofy *token-terms* :acct_mailboxes :indx)
        (ast/predicate-operator astu/!EQ)
        (ast/symbol-literal "missing value"))
       (ast/block nil (ast/routine-call
        nil
        (ast/term nil :flatten_mailboxes)
        (ast/term nil :accum) (ast/term nil :indx)) ast/new-line))
      (ast/else-if-expression
       nil
       (if-then-else-nil-builder
        mfilt :indx (if-pass-block :mrec args :accum :indx callblk2)))))
     (ast/return nil :accum))))))

(defn- frame-hierarchical-mailboxes
  "Placeholder for hierarchical mailbox"
  [fmap block]
  fmap)

(defn- refactor-mailbox-filter
  "Placeholder for hierarchical mailbox"
  [filters]
  [false filters])

(defn- frame-process-mailboxes
  [fmap {:keys [fetch-type filters] :as block} parm1 parm2]
  (let [[cf? nfilter] (refactor-mailbox-filter filters)]
  [(if (and (= fetch-type :mailboxes) (not cf?))
     (frame-flatten-mailboxes fmap block)
     (frame-hierarchical-mailboxes fmap block))
   (ast/routine-call
    nil
    (ast/term nil (if (= fetch-type :mailboxes)
                    :flatten_mailboxes
                    :hierarchy_mailboxes))
    (ast/term nil parm1)
    (ast/term nil parm2))]))

(defn- frame-build-mailboxes
  "Builds the 'fetch_mailboxes' handler"
  [fmap {:keys [args] :as block}]
  (let [[fzap callmb] (frame-process-mailboxes fmap block :inlist :mbloop)]
    (update-in
     fzap
     [:routine] conj
     (ast/routine
      nil :fetch_mailboxes [:accum :rsrc]
      (tell-app
       (ast/define-locals nil :mbloop :mrec :mlist :inlist)
       (ast/set-statement nil (ast/term nil :mlist) ast/empty-list)
       (ast/set-statement nil (ast/term nil :inlist) ast/empty-list)
       (ast/for-in-expression
        nil
        (ast/term nil :mbloop)
        (ast/get-statement
         nil
         (ast/xofy-expression
          nil
          (ast/term nil :acct_mailboxes)
          (ast/term nil :rsrc)))
        callmb)
       (ast/set-extend-record :accum :acct_mailboxes :inlist)
       (ast/return nil :accum))))))

(def ^:private frame-jumps
  {:mailboxes [:fetch_mailboxes frame-build-mailboxes]
   :messages  [:fetch_messages frame-build-messages]})

(defn- frame-next-block
  "Returns either a null block (no subsets) or the
  calling routine block procurred as part of deeper
  processing"
  [fmap {:keys [subsets] :as block} {:keys [accum source]}]
  (let [frs (first subsets)]
    (if frs
      (let [[routinekw f] ((:fetch-type frs) frame-jumps)
            zmap (f fmap frs)]
      [zmap (ast/routine-call
             nil
             (ast/term nil routinekw)
             (ast/term nil accum)
             (ast/term nil source))])
      [fmap (ast/term nil accum)])))

(defn- account-block
  "Builds the account block for setting up and fetching
  account values"
  [fmap {:keys [args] :as block} {:keys [source accum] :as props}]
  (let [[smap code] (frame-next-block
                     fmap block (assoc props
                                  :source :indx
                                  :property :acct_mailboxes))]
    [smap
     (ast/block
      nil
      (ast/define-locals nil :indx)
      (ast/for-in-expression
       nil
       (ast/term nil :indx)
       (ast/term nil source)
       (ast/record-fetch nil args accum :indx)
       (ast/set-statement
        nil
        (ast/eol-cmd nil :alist nil)
        code ))
      (ast/return nil :alist))]))

(defn- term-gen
  "Term generator"
  [coll]
  (map #(ast/list-of
         nil
         [(ast/string-literal (first (clojure.string/split % #" ")))
          (ast/term nil %)]) coll))

(defn- frame-account-fetch
  "Builds the 'fetch_account' handler to be called by
  the frame_body entry point"
  [block fmap]
  (let [[zmap mfilt] (frame-filter fmap block)
        [smap code] (account-block zmap block {:source :aclist :accum :arec})]
    (update-in
     smap
     [:routine] conj
     (ast/routine
      nil :fetch_accounts []
      (tell-app
       (ast/define-locals nil :aclist :arec :alist)
       (ast/set-statement nil (ast/term nil :alist) ast/empty-list)
       (ast/set-statement
        nil
        (ast/term nil :aclist)
        (ast/routine-call
         nil
         (ast/term nil :account_list)
         (ast/list-of nil (term-gen (*application* acc-list)))
         (ast/term nil mfilt)))
       code)))))


(def ^:private routines-fetch [noop-filter
                               (partial include-cleaners
                                        [:emailcleaner :datecleaner :cleanval])
                               match-script
                               account-list])

(defn- message-fetch-builder
  "Take the original request and convert to handlers and
  callers to retrieve content"
  [block]
  (flatten-stack
    (routines
     (conj routines-fetch
           (partial frame-account-fetch block)
           (partial script-body :fetch_accounts)))
    ))

(defn- fill-block-gaps
  "Takes a request and, if needed, prepends account and mailbox
  information request blocks"
  [{:keys [fetch-type] :as block}]
  (condp = fetch-type
    :accounts block
    :mailboxes (mesg/accounts block)
    :messages  (mesg/accounts (mesg/mailboxes block))))

(defn fetch-messages
  "Main entry point binds target specific information
  then builds fetch routines"
  [appbiding token-binding block]
  (with-bindings {#'*application* appbiding
                  #'*token-terms* token-binding
                  #'*build-for* :fetch}
    (message-fetch-builder (fill-block-gaps block))))

;;; End fetch Content

;;; Send content

(defn- make-recipients-address
  "Generate the email address property for recipients"
  [srckw]
  (ast/record-definition
   nil
   (ast/key-value
    nil
    (ast/key-term :address)
    (ast/term nil srckw))))

(defn- make-to-address
  [srckw]
  "Generates recipieint structure given
  application."
  (condp = *application*
    :mail (make-recipients-address srckw)
    :outlook (ast/record-definition
              nil
              (ast/key-value
               *token-terms*
               (ast/key-term :acct_emails)
               (make-recipients-address srckw)))))

(defn- make-recipients
  "Generates the repeat loop for setting the recipient
  emails on the outbound message"
  [imsgkw listkw omsgkw]
  (ast/for-in-expression
   nil
   (ast/term nil :rec)
   (ast/xofy-expression
    nil
    (ast/term nil listkw) (ast/term nil imsgkw))
   (ast/make-new
    nil
    (ast/term *token-terms* :to_recipient)
    (ast/expression
     nil
     ast/at
     (ast/expression
      nil
      (ast/term nil omsgkw)
      ast/with-properties
      (make-to-address :rec))))
   ))

(defn- make-message
  "Generates the 'make new outgoing message...'
  script for a valid filtered account result"
  [targkw imsgkw acclistkw]
  (ast/block
   nil
   (ast/define-locals nil :acc1)
   (ast/set-statement
    nil
    (ast/term nil :acc1)
    ; Mail version
    (ast/get-xofy
     nil
     ast/first-of
     (ast/term nil acclistkw)))
   (ast/set-statement
    nil
    (ast/term nil targkw)
    (ast/make-new
     nil
     (ast/term *token-terms* :out_message_mb)
     ast/with-properties
     (ast/record-definition
      nil
      (ast/key-value
       *token-terms*
       (condp = *application*
         :mail (ast/key-term :msg_sender)
         :outlook (ast/key-term :account))
       (ast/term nil :acc1))
      (ast/kv-template-t *token-terms* :msg_subject imsgkw)
      (ast/kv-template-t *token-terms* :msg_text imsgkw))))))

(defn- make-message-default
  "Generates teh 'make new outgoing message...'
  script for default account assignment"
  [targkw imsgkw]
  (ast/set-statement
   nil
   (ast/term nil targkw)
   (ast/make-new
    nil
    (ast/term *token-terms* :out_message_mb)
    ast/with-properties
    (ast/record-definition
     nil
     (ast/kv-template-t *token-terms* :msg_subject imsgkw)
     (ast/kv-template-t *token-terms* :msg_text imsgkw)))))

(defn- if-count-then-else
  "Sets up the 'if account count' logic"
  [targkw trueform falseform]
  ; if (count of targkw is equal to 0)
  (ast/if-statement
   nil
   (ast/if-expression
    nil
    (ast/predicate-condition
     nil
     (ast/count-expression nil (ast/term nil targkw))
     (ast/predicate-operator astu/!EQ)
     (ast/numeric-literal 1))
    ; then set omsg to make new outgoing message
    trueform)
   ; else
   (ast/else-if-expression
    nil
    falseform)))

(defn- send-account-fetch
  "Builds the 'send_message' handler to be called by
  the body entry point"
  [{:keys [message filters] :as block} fmap]
  (let [[zmap mfilt] (frame-filter fmap block)]
    (update-in
     zmap
     [:routine] conj
     (ast/routine
      nil :send_message []
      (tell-app
       ; Setup the locals
       (ast/define-locals nil :alist :imsg :omsg)
       (ast/set-statement
        nil
        (ast/term nil :imsg)
        (ast/setrecord-frommap (:message block)))
       (if filters
         (ast/block
          nil
          (ast/set-statement
           nil
           (ast/term nil :alist)
           (ast/routine-call
            nil
            (ast/term nil :account_list)
            (ast/list-of nil (term-gen (*application* acc-list)))
            (ast/term nil mfilt)))
          (if-count-then-else
           :alist
           (make-message-default :omsg :imsg)
           (make-message :omsg :imsg :alist)))
         (make-message-default :omsg :imsg))
       (make-recipients :imsg :msg_recipients :omsg)
       (ast/expression
        nil
        ast/as_send (ast/term nil :omsg) ast/new-line))))))


(defn- send-msg-subs
  "Validate and re-arrange for filtering accounts"
  [{{:keys [msg_sender msg_recipients
            msg_subject msg_text] :or {msg_sender nil} :as msg} :message :as block}]
  {:pre [(and (not (nil? msg_subject))
              (not-empty msg_subject)
              (not (nil? msg_text))
              (not-empty msg_text)
              (not (nil? msg_recipients))
              (not-empty msg_recipients))]}
  (assoc block
    :message (dissoc msg :msg_sender)
    :filters (cond
               (nil? msg_sender)    nil
               (empty? msg_sender)  nil
               (string? msg_sender) (second (astu/filter :acct_emails astu/EQ msg_sender))
               (vector? msg_sender) (second msg_sender)
               :else (assert false (str "Unable to resolve " msg_sender)))))

(defn- message-send-builder
  "Construct the AST for sending messages"
  [block]
  (let [nblk (send-msg-subs block)]
  (flatten-stack
   (routines
    (conj (if (:filters nblk) [match-script account-list] [])
          (partial send-account-fetch nblk)
          (partial script-body :send_message))))))

(defn send-message
  "Main entry point for sending messages"
  [appbinding token-binding block]
  (with-bindings {#'*application* appbinding
                  #'*token-terms* token-binding
                  #'*build-for* :send}
    (message-send-builder block)))

;;; End send content
