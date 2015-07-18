(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - common contacts DSL"}
  clasew.identities
  (:require [clasew.utility :as util]
            [clasew.ast-emit :as aste]
            [clasew.gen-as :as gen]
            [clojure.java.io :as io]
            [clojure.walk :as w]))


(def ^:private handler-map
  {
   :all-identities    "clasew_get_identities"
   :all-groups        "clasew_get_groups"
   :add-identities    "clasew_add_identities"
   :add-groups        "clasew_add_groups"
   :put-identities    "clasew_put_identities"
   :put-groups        "clasew_put_groups"
   :delete-identities "clasew_drop_identities"
   :delete-groups     "clasew_drop_groups"

   :run-script        "clasew_run"
   })

(def ^:private identity-attrs
  #{:suffix, :name :first_name, :middle_name, :last_name,
    :company, :title, :department})

(def identity-standard identity-attrs)

(def ^:private address-attrs
  #{:city, :street})

(def address-standard address-attrs)

(def repeat-controls
  {:instance       :gen    ; The first part 'repeat with instance ...' | :gen
   :target         nil     ; The second 'repeat with instance in target'
   :global-locals  nil     ; List of tokens with initialization instructions
   :setters        nil     ; List of setters to construct my map
   :map-name       :gen    ; Name of map used to collect from setters | :gen
   :result-list    :gen    ; If the map results are put end of list
   :result-map     nil     ; If the map results are put in list and extends a map
   :nesters        nil     ; List of nested cstruct chunks
   })

(defn- genpass
  "Reduction function to generate unique keywords"
  [acc [k v]]
  (assoc acc k (if (= v :gen) (keyword (gensym)) v)))

(defn merge-repeat-cstruct
  "Populates repeat control structure data and auto-gens if selects"
  [cr & imap]
  (let [acc (if (empty? imap) repeat-controls (merge repeat-controls imap))
        sts (merge acc cr)]
  (reduce genpass acc sts)))


(def ^:private ast-template
  {:root nil
   :do-block []
   :returns nil})

(defn produce-ast
  "Sets up the basic AST specific to target application"
  [context]
  (update-in ast-template [:root]
             str
             (cond
              (= context :contacts) "Contacts"
              (= context :outlook) "Microsoft Outlook")))

(defn emit-ast
  "Returns an AST ready for generation into Applescript.
  target-app is either :contacts or :outlook
  repeat-ast-struct is a repeat-controls"
  [target-app repeat-ast-struct]
  (let [result    (keyword (gensym "res"))
        ast       (assoc-in (produce-ast target-app) [:returns] result)
        lcls      (aste/locals result)
        sts       (aste/sets (aste/set-assign result :list))
        rpts      (aste/repeat-ast
                   (assoc-in repeat-ast-struct [:result-list] result))
        outr      (aste/do-block lcls sts rpts)
        ]
    (conj ast (outr))
    ))

(defn genscript
  "Generate the AppleScript from the passed in AST"
  [ast]
  (gen/generate-script ast))

(def address-attrs
  #{})

(def phone-attrs
  #{})

(def email-attrs
  #{})

