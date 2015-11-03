(ns
  ^{:author "Frank V. Castellucci"
    :doc "Clojure AppleScriptEngine Wrapper - common message DSL utilities"}
  clasew.mesg-utils
  (:require   [clasew.ast-emit :as ast]
              [clasew.ast-utils :as astu]
              [clasew.messages :as mesg]))


(def ^:private boxes
  {:outlook "mail folders"
   :mail "mailboxes"})

(def acc-list
  {:outlook #{"exchange accounts", "pop accounts", "imap accounts", "ldap accounts"}
   :mail #{"iCloud accounts", "pop accounts", "imap accounts"}})

(defn email_list
  [appkw fromkw token-fn]
  (if (= appkw :mail)
    (ast/xofy-expression
     nil
     (ast/expression
      nil
      ast/getin (ast/term token-fn :acct_emails))
     (ast/term nil fromkw))
    (ast/list-of
     nil
      (list (ast/get-statement
             nil
             (ast/xofy-expression
              nil
              (ast/term token-fn :acct_emails)
              (ast/term nil fromkw)))))))

(defn- imbue-filter-args
  "Converts the arguments collection of filter with
  the owner context"
  [ownerkw argcoll]
  (map #(sequence [(first %) (second %) (last %) ownerkw]) argcoll))

(defn build-filter-routine
  "Generates a filter routine/handler"
  [fname argkw usrfilt token-fn]
  (ast/routine
   nil
   fname [argkw]
   (ast/return token-fn
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
  [appkw token-fn]
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
            (ast/kv-template token-fn :acct_name :acc1)
            (ast/kv-template token-fn :acct_user_name :acc1)
            (ast/kv-template token-fn :acct_user_fullname :acc1)
            (ast/kv-template token-fn :acct_mailboxes :acc1)

            ; account emails
            (ast/key-value
             nil
             (ast/key-term :acct_emails)
             (ast/expression nil (email_list appkw :acc1 token-fn)))))) nil))))
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
  [c m token-fn]
  (let [x (keyword (str (gensym) "_filter"))]
    (if (:filters m)
      (swap! c conj [(:fetch-type m) x (build-filter-routine x :x (:filters m) token-fn)]))
    (if (not-empty (:subsets m))
      (mapcat #(mapcat-fn c % token-fn) (:subsets m)))))

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

(defn mcat
  "Traverse input block to substitute inline filters
  with handler call"
  [iblock token-fn]
  (let [x (atom [])]
    (mapcat #(mapcat-fn x % token-fn) (list iblock))
    [@x (setblock (pfunc @x iblock))]))

;;
;; Fetch meta data
;;

(defn meta-pattern
  [indirs block]
  (partial ((:fetch-type block) indirs) (assoc block :subsets nil)
   (if (not-empty (:subsets block))
     (map #(meta-pattern indirs %) (:subsets block))
     '())))

