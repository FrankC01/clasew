(ns
  ^{:author "Frank V. Castellucci"
    :doc "Clojure AppleScriptEngine Wrapper - common message DSL utilities"}
  clasew.mesg-utils
  (:require   [clasew.ast-emit :as ast]
              [clasew.ast-utils :as astu]
              [clasew.messages :as mesg]))



(def ^:dynamic *application* nil)
(def ^:dynamic *token-terms* nil)

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

(defn- frame-match-script
  [fmap]
  (update-in fmap [:script] conj (match-block)))

(defn- account_list
  "Generates the account collection routine that returns
  a valid list of accounts"
  []
  (ast/routine
   nil :account_list [:aclist :pat]
   (ast/define-locals nil :alist :acc :acc1 :acct_type)
   (ast/tell
    nil *application*
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
      (ast/term nil :acc1) (ast/xofy-expression nil ast/second-of (ast/term nil :acc))
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
            (ast/key-value nil (ast/key-term :acct_type) (ast/term nil :acct_type))
            (ast/kv-template *token-terms* :acct_name :acc1)
            (ast/kv-template *token-terms* :acct_user_name :acc1)
            (ast/kv-template *token-terms* :acct_user_fullname :acc1)
            (ast/kv-template *token-terms* :acct_mailboxes :acc1)

            ; account emails
            (ast/key-value
             nil
             (ast/key-term :acct_emails)
             (ast/expression nil (email_list :acc1)))))) nil))))
      (ast/return nil :alist)))

(defn- frame-account-routine
  [fmap]
  (update-in fmap [:routine] conj (account_list)))


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

(defn- frame-cleaner-filters
  [fmap]
  (reduce #(update-in %1 [:filter] conj (generic-cleaner %2))
          fmap [:emailcleaner :datecleaner :cleanval]))

(defn- call-clean
  "Calls a cleaner (fnkw) and makes getxofy as argument"
  [fnkw term source]
  (ast/routine-call
   nil
   (ast/term nil fnkw)
   (ast/get-xofy *token-terms* term source)))

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
   (ast/tell
    nil *application*
    (ast/return *token-terms* (predicate-builder argkw usrfilt)))))

(defn- frame-build-filter
  [fmap usrfilt]
  (let [fname (str (gensym) "_filter")]
    [(update-in fmap [:filter] conj (build-filter-routine fname :x usrfilt))
     fname]))

; Noop filter always returns true

(defn- frame-noop-filter
  [fmap]
  (update-in fmap [:filter] conj
             (ast/routine nil :noop_filter [:x] (ast/return nil :true))))

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
  [fmap {:keys [fetch-type filters] :as block}]
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
  [fmap {:keys [args filters] :as block}]
  (let [[zmap mfilt] (frame-filter fmap block)]
    (update-in
     zmap
     [:routine] conj
     (ast/routine
      nil :fetch_messages [:accum :rsrc]
      (ast/tell
       nil *application*
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
        [zfmap1 callblk1] (frame-next-block zmap block {:source :par :accum :mrec})
        callblk2 (reuse-callback-diff-arg callblk1 1 :to-value :indx)]
  (update-in
   zfmap1
   [:routine] conj
   (ast/routine
    nil :flatten_mailboxes [:accum :par]
    (ast/tell
     nil *application*
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
  [fmap block]
  fmap)

(defn- frame-process-mailboxes
  [fmap {:keys [fetch-type] :as block} parm1 parm2]
  [(if (= fetch-type :mailboxes)
     (frame-flatten-mailboxes fmap block)
     (frame-hierarchical-mailboxes fmap block))
   (ast/routine-call
    nil
    (ast/term nil (if (= fetch-type :mailboxes) :flatten_mailboxes :hierarchy_mailboxes))
    (ast/term nil parm1)
    (ast/term nil parm2))])

(defn- frame-build-mailboxes
  [fmap {:keys [args] :as block}]
  (let [[fzap callmb] (frame-process-mailboxes fmap block :inlist :mbloop)]
    (update-in
     fzap
     [:routine] conj
     (ast/routine
      nil :fetch_mailboxes [:accum :rsrc]
      (ast/tell
       nil *application*
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
  (let [[zfmap callblk] (frame-next-block fmap block (assoc props
                                                       :source :indx
                                                       :property :acct_mailboxes))]
    [zfmap
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
        callblk ))
      (ast/return nil :alist))]))

(defn- term-gen
  [coll]
  (map #(ast/list-of
         nil
         [(ast/string-literal (first (clojure.string/split % #" ")))
          (ast/term nil %)]) coll))

(defn- frame-account-fetch
  [fmap block {:keys [source accum] :as args}]
  (let [[zmap mfilt] (frame-filter fmap block)
        [smap code] (account-block zmap block args)]
  (update-in
   smap
   [:routine] conj
   (ast/routine
    nil :fetch_accounts []
  (ast/tell nil *application*
   (ast/define-locals nil source accum :alist)
   (ast/set-statement nil (ast/term nil :alist) ast/empty-list)
   (ast/set-statement
    nil
    (ast/term nil source)
    (ast/routine-call
     nil
     (ast/term nil :account_list)
     (ast/list-of nil (term-gen (*application* acc-list)))
     (ast/term nil mfilt)))
   code)))))


(defn- frame-body
  [fmap]
  (update-in
   fmap
   [:body] conj
   (ast/tell
    nil
    *application*
    (ast/return
     nil
     (ast/routine-call
      nil
      (ast/term nil :fetch_accounts))))))

(defn- fetch-reducer
  [acc targ]
  (if (not-empty (second targ)) (conj acc (second targ)) acc))

(defn- message-fetch-builder
  [block]
  (let [fmap (frame-body
              (frame-account-fetch
               (frame-account-routine
                (frame-match-script
                 (frame-cleaner-filters
                  (frame-noop-filter {:script []
                                      :filter []
                                      :routine []
                                      :body []}))))
               block  {:source :aclist :accum :arec}))]
    (apply (partial ast/block nil)
           (flatten (reduce fetch-reducer [] fmap)))))

(defn fetch-messages
  "Main entry point binds target specific information
  then builds fetch routines"
  [appbiding token-binding block]
  (with-bindings {#'*application* appbiding
                  #'*token-terms* token-binding}
    (message-fetch-builder block)))
