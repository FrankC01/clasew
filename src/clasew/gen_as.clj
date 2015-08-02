(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - Experimental AppleScript Generator"}
  clasew.gen-as
  (:require [clasew.utility :as util]
            [clojure.java.io :as io]
            [clojure.walk :as w]))


(def identities
  {:name_suffix          "suffix",
   :full_name            "name",           ; display name in outlook
   :first_name           "first name",
   :middle_name,         "middle name",
   :last_name            "last name",
   :primary_company      "organization",   ; company in outlook
   :primary_title        "job title",
   :primary_department   "department",
   :city_name            "city",           ; home xxx and business xxx in outlook
   :street_name          "street",         ; home xxx and business xxx in outlook
   :zip_code             "zip",            ; home xxx and business xxx in outlook
   :country_name         "country",        ; home xxx and business xxx in outlook
   :state_name           "state"           ; home xxx and business xxx in outlook
    })

(def null ":null")

(def identities-v
  {:name_suffix          null,
   :full_name            null,
   :first_name           null,
   :middle_name,         null,
   :last_name            null,
   :primary_company      null,
   :primary_title        null,
   :primary_department   null,
   :city_name           null,
   :street_name         null,
   :zip_code            null,
   :country_name        null,
   :state_name          null
    })

(defn gen-map
  [kws]
  (str "{"
  (apply str (interpose ","
  (reduce #(conj %1 (str (symbol (name %2)) (%2 identities-v))) [] kws)))
       "}"))
;;;
;;;   AST->Applescript Transforms
;;;

(defn- map-set-handler
  "Generates a 'set lhs of lhsc to rhs of rhsc'"
  [lhs & [[mapset-fn  lhsc rhs & [rhsc]]]]
  (let [z (or mapset-fn (partial get identities))]
    (str (name lhs)
           " of " (name lhsc)
           (str " to my cleanval(" (name (z rhs))
             (if rhsc (str " of " (name rhsc)) "") ")"))))

(defn- map-extend-handler
  [lhs [rhk rhv]]
  (str (name lhs) " to " (name lhs) " & {" (name rhk) ":"(name rhv)"}"))

(defn- repeat-head-handler
  [[item & [oftarget]]]
  (str " in " (name item) (if oftarget (str " of " (name oftarget)) "")))

(def ^:private as-ast-handlers
  {
   :list         "{}"
   :map          #(str (name %1) " to " (gen-map %2))
   :map-set      map-set-handler
   :map-extend   map-extend-handler
   :end-of-list  #(str "end of " (name %1) " to " (name (first %2)))
   :in           repeat-head-handler
   :from         #(name %2)
   })


(defn- assignment
  [[lhs assign & rhs]]
  (let [r (get as-ast-handlers assign :not-found)]
  (str "set "
       (cond
        (and (= r :not-found) (keyword? assign))
          (str (name lhs) " to " (name assign))
        (and (= r :not-found) (string? assign))
          (str (name lhs) " to " (str "\"" assign "\""))
        (fn? r) (r lhs rhs)
        :else (str (name lhs) " to " r))
        )))

(defn- set-block
  "Creates a 'set' line for variables
  idefs are a collection of vectors where each vector is of the form:
  :kw the variable that is being set
  :value the value to set to the :kw
  Keywords are changed to names"
  [idefs]
  (loop [b (first idefs)
         r (rest idefs)
         a []]
    (if (nil? b)
            (clojure.string/join "\n" a)
      (recur (first r) (rest r) (conj a (assignment b))))))

(defn- local-block
  [ldefs]
  (if (empty? ldefs) ""
  (str "local "
       (apply str (interpose "," (map #(name (first %)) ldefs))))))

(defn- repeat-header
  [[base infor & targs]]
  (str "repeat with " (name base) ((infor as-ast-handlers) targs)))

(declare block-dispatch)

(defn- repeat-block
  [[header block]]
  (clojure.string/join "\n"
                       (conj []
                             (repeat-header header)
                             (block-dispatch block)
                             (str "end repeat")
                             )))

(declare block-handler)

(defn- block-dispatch
  [[bkw args]]
  (cond
    (= bkw :locals) (local-block args)
    (= bkw :sets) (set-block args)
    (= bkw :do-block) (block-handler args)
    (= bkw :repeat) (repeat-block args)
    (= bkw :quit) (str "quit")
   :else (throw (Exception. (str "block-dispatch: Unknown type "bkw)))))

(defn- block-handler
  [vect-blocks]
  (loop [fb (first vect-blocks)
         rb (rest vect-blocks)
         acc []]
    (if (nil? fb)
      (clojure.string/join "\n" acc)
      (recur (first rb) (rest rb) (conj acc (block-dispatch fb))))))

(defn generate-script
  [{:keys [root do-block returns] :as ast}]
  (let [d  (block-handler do-block)
        r (if returns (str "\nreturn " (name returns)) "")]
    (str "\ntell application \"" root "\" \n" d r "\nend tell")))

