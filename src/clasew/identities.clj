(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - common contacts DSL"}
  clasew.identities
  (:require [clasew.utility :as util]
            [clasew.ast-emit :as aste]
            [clasew.gen-as :as gen]
            ;[clojure.java.io :as io]
            [clojure.walk :as w]))


(def ^:private handler-map
  {
   :all-identities     "clasew_get_all_identities"
   :all-groups         "clasew_get_all_groups"
   :add-identities     "clasew_add_identities"
   :add-groups         "clasew_add_groups"
   :update-identities  "clasew_update_identities"
   :update-groups      "clasew_update_groups"
   :delete-identities  "clasew_delete_identities"
   :delete-groups      "clasew_delete_groups"

   :quit               "clasew_quit"

   :run-script         "clasew_run"
   })

(def ^:private identity-attrs
  #{:name_suffix, :full_name :first_name, :middle_name, :last_name,
    :primary_company, :primary_title, :primary_department})

(def identity-standard identity-attrs)

(def ^:private address-attrs
  #{:city_name, :street_name, :zip_code, :country_name, :state_name})

(def address-standard address-attrs)

(def ^:private email-attrs
  #{:email_address})

(def email-standard email-attrs)

;;;
;;; EXPERIMENTAL - EXPR->AST->AS emit/gen
;;;

;; Core repeat controls

(def repeat-controls
  {:filters        nil     ; If present, alters the repeat instance
   :instance       :gen    ; The first part 'repeat with instance ...' | :gen
   :instance-flt   nil     ; Set when filtering is managing the loop
   :target         nil     ; The second 'repeat with instance in target'
   :global-locals  nil     ; List of tokens with initialization instructions
   :setters        nil     ; List of setters to construct my map
   :sub-setters    nil     ; Sub-setters used to extend in block set logic
   :map-name       :gen    ; Name of map used to collect from setters | :gen
   :mapset-fn      nil
   :result-list    :gen    ; If the map results are put end of list
   :result-map     nil     ; If  map results are put in list and extends a map
   :nesters        nil     ; List of nested cstruct chunks (optional)
   })

;; Additional setter blocks (optional)

(def repeat-subsetters
  {:setters       nil
   :map-name      :gen
   :mapset-fn     nil
   :result-list   :gen
   :result-map    nil
   :global-locals nil
   })

;; Filter control - is merge/extended with user field filters
;; Modifies associated repeat controls as needed

(def repeat-filters
  {:control-target  nil    ; Set to target object type (e.g. contacts/people)
   :control-field   :gen   ; Receipient of the 'whose' query (local and set)
   :loop-field      :gen   ; Same as repeat instance for intermmediate looping
   :prop-field      :gen   ; Property of loop-field instance, the meat
   :user-filter     nil    ; Placeholder for filterable keys/values map
   })

(defn genpass
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
        plcls     (aste/preprocess-locals repeat-ast-struct)
        sts       (aste/sets (aste/set-assign result :list))
        psts      (aste/preprocess-filters repeat-ast-struct)
        rpts      (aste/repeat-ast
                   (assoc-in repeat-ast-struct [:result-list] result))
        outr      (aste/do-block lcls sts plcls psts rpts)
        ]
    (conj ast (outr))))

(defn genscript
  "Generate the AppleScript from the passed in AST"
  [ast]
  (gen/generate-script ast))

(defn addresses
  "Adds ability to retrieve standard address elements or those
  identified in collection"
  ([] [:addresses address-standard])
  ([& col] [:addresses (into [] col)]))

(defn emails
  "Adds ability to retrieve standard address elements or those
  identified in collection"
  ([] [:emails email-standard])
  ([& col] [:emails (into [] col)]))

;;
;; Low level DSL functions ----------------------------------------------------
;;

(defn- reduce-chandler
  "Reducer of handler descriptors"
  [acc [h_key h_arg]]
  (conj acc (assoc {} "handler_name" (get handler-map h_key "ERROR")
    "handler_args" h_arg)))

(defn clasew-script
  "Takes N handler vectors [:handler_map_key argument(s)] and produces
  a vector of maps converted"
  [& handlers]
  (reduce reduce-chandler [] handlers))

;;
;; High level DSL functions ---------------------------------------------------
;;

(defn get-identities
  ([] [:all-identities])
  ([args] args))
