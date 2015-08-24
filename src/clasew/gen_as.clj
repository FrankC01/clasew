(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - Experimental AppleScript Generator"}
  clasew.gen-as
  (:require [clasew.utility :as util]
            [clojure.java.io :as io]
            [clojure.walk :as w]))


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
   :state_name          null,
   :email_address      null,
   :email_type         null
    })

(defn- gen-map
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
  (str (name lhs)
       " of " (name lhsc)
       (str " to my cleanval(" (name (mapset-fn rhs))
            (if rhsc (str " of " (name rhsc)) "") ")")))

(defn- map-extend-handler
  [lhs [rhk rhv]]
  (str (name lhs) " to " (name lhs) " & {" (name rhk) ":"(name rhv)"}"))

(defn properties-set-handler
  [lhs [[rhs]]]
  (str (name lhs) " to properties of " (name rhs)))

(defn filter-reduce
  "Converts filter map to individual search criteria. Multiple are
  conjoined (e.g. and'ed)"
  [filter-map mpnm-fn]
  (apply str
         (interpose " and "
                    (reduce-kv
                     #(conj %1 (str (mpnm-fn %2) " contains " \" %3 \"))
                     []
                     filter-map))))

(defn filter-set-handler
  [lhs [rhs rhm mpnm-fn]]
  (let [d (filter-reduce rhm mpnm-fn)]
  (str (name lhs) " to " (name rhs) " whose (" d ")")))

(defn- repeat-head-handler
  [map-fn [item & [oftarget]]]
  (str " in (get " (name (map-fn item))
       (if oftarget (str " of " (name oftarget)) "")
       ")"))

(def ^:private as-ast-handlers
  {
   :list           "{}"
   :map            #(str (name %1) " to " (gen-map %2))
   :map-set        map-set-handler
   :map-extend     map-extend-handler
   :properties-of  properties-set-handler
   :filters        filter-set-handler
   :end-of-list    #(str "end of " (name %1) " to " (name (first %2)))
   :in             repeat-head-handler
   :from           #(name %2)
   })


(defn- assignment
  [[lhs assign & rhs]]
  ;(println "LHS = " lhs " assign = " assign " RHS = " rhs)
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
  [[mapset-fn base infor & targs]]
  (str "repeat with " (name base) ((infor as-ast-handlers) mapset-fn targs)))

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

