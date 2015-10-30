(ns
  ^{:author "Frank V. Castellucci"
    :doc "Clojure AppleScriptEngine Wrapper - common message DSL utilities"}
  clasew.mesg-utils
  (:require   [clasew.ast-emit :as ast]
              [clasew.ast-utils :as astu]
              ))


(def ^:private boxes
  {:outlook "mail folders"
   :mail "mailboxes"})

(def acc-list
  {:outlook #{"exchange accounts", "pop accounts", "imap accounts", "ldap accounts"}
   :mail #{"iCloud accounts", "pop accounts", "imap accounts"}})

(def ^:private lookups
  {:acct_name    "name"
   :name         "name"
   :user-name    "user name"
   :full-name    "full name"
   :email-names  "email addresses"
   :email-name   "email address"})

(defn message-lookup
  [termkw]
  (get lookups termkw (name termkw)))

(defn email_list
  [appkw fromkw]
  (if (= appkw :mail)
    (ast/xofy-expression
     nil
     (ast/expression
      nil
      ast/getin (ast/term message-lookup :email-names))
     (ast/term nil fromkw))
    (ast/list-of
     nil
      (list (ast/get-statement
             nil
             (ast/xofy-expression
              nil
              (ast/term message-lookup :email-name)
              (ast/term nil fromkw)))))))

(defn- imbue-filter-args
  "Converts the arguments collection of filter with
  the owner context"
  [ownerkw argcoll]
  (map #(sequence [(first %) (second %) (last %) ownerkw]) argcoll))

(defn build-filter-routine
  "Generates a filter routine/handler"
  [fname argkw usrfilt]
  (ast/routine
   nil
   fname [argkw]
   (ast/return message-lookup
               (ast/precedence
                nil
                (ast/predicate nil
                               {:joins (:joins usrfilt)
                                :args  (imbue-filter-args argkw (:args usrfilt))}
                               )))))

(defn noop-filter [] (ast/routine nil :noop_filter [:x] (ast/return nil :true)))

(defn- match-block
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


(defn account_list
  [appkw]
  (ast/block
   nil
   (match-block)
  (ast/routine
   nil :account_list [:aclist :pat]
   (ast/define-locals nil :alist :acc :acc1)
   (ast/tell
    nil appkw
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
            (ast/kv-template message-lookup :acct_name :name :acc1)
            (ast/kv-template message-lookup :acct_user_name :user-name :acc1)
            (ast/kv-template message-lookup :acct_user_fullname :full-name :acc1)
            (ast/kv-template nil :acct_mailboxes (appkw boxes) :acc1)

            ; account emails
            (ast/key-value
             nil
             (ast/key-term :acct_emails)
             (ast/expression nil (email_list appkw :acc1)))))) nil))))
      (ast/return nil :alist))))

;; Support functions
; Function to traverse and extract
; filters to replace with handlers

(defn- pfunc1
  "Gets appropriate filter and substitues the keyword to the handler
  generated in mapcat-fn"
  [fa [_ ft]]
  (let [r (first (filter #(= (first %) ft) fa))]
    (if r [:filters (second r)] [:filters nil])))

(defn pfunc
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

(defn mcat
  "Traverse input block to substitute inline filters
  with handler call"
  [iblock]
  (let [x (atom [])]
    (mapcat #(mapcat-fn x %) (list iblock))
    [@x (pfunc @x iblock)]))

;;
;; Fetch meta data
;;

(def depth-patterns
  {[0] true
   [1 0] true
   [1 1 0] true
   [2 0 0] true})

;; Need pattern match for exception handling

(defn- pattern-mcat-fn
  "Takes a map (m) and builds topological nesting level information"
  [{:keys [fetch-type subsets]}]
  (let [tdata (vector fetch-type (count subsets))]
    (if (not-empty subsets)
      (conj tdata (mapcat pattern-mcat-fn subsets))
      tdata)))

(defn- extract-pattern
  "Extracts the intermediate pattern form from the
  'fetch' message input map"
  [m]
  (mapcat pattern-mcat-fn (list m)))


(defn- pattern-reduce
  "Divies up the count pattern from the type pattern
  recursive on inner collections"
  [acc c]
  (reduce #(cond
            (keyword? %2) (update-in %1 [1] conj %2)
            (number? %2) (update-in %1 [0] conj %2)
            (coll? %2) (pattern-reduce %1 %2)
            ) acc c))

(defn meta-pattern
  [imap]
  (pattern-reduce [[][]] (extract-pattern imap)))
