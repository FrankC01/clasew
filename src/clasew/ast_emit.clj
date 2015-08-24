(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - Experimental AST emitter"}
  clasew.ast-emit
  (:require [clasew.utility :as util]
            [clojure.java.io :as io]
            [clojure.walk :as w]))

;;
;; Hack
;;

(def lookaside
  {:addresses :address_list
   :emails :email_list}
  )

;;
;; AST Emitters - Old version
;;

(defn locals
  [& vals]
  (let [r (into [] (map vector vals))]
    (fn [] [:locals r])))

(defn set-assign
  [targ source]
  (fn [] [targ source]))

(defn set-properties-of
  [targ source]
  (fn [] [targ :properties-of source]))

(defn set-list-end
  [lid vid]
  (fn [] [lid :end-of-list vid]))

(defn set-filter-on
  "Creates the filtering construct
  filters : instance of clasew.identities/repeat-filters
  mnfn : name lookup mapping function"
  [filters mnfn]
  (fn [] [(:control-field filters)
          :filters
          (first (:control-target filters))
          (:user-filter filters)
          mnfn]))

(defn set-map
  [target & srcs]
  (fn [] (into [target :map] srcs)))

(defn set-map-set
  [mapset-fn target & srcs]
  (fn [] (into [target :map-set mapset-fn] srcs)))

(defn set-map-extend
  [localmap targetkey targetlist]
  (fn sme []
    [localmap :map-extend targetkey targetlist]))

(defn repeat-head
  [iter base mapset-fn & [source]]
  (fn rh-fns []
    (if (or (= (count source) 0))
      [:repeat [[mapset-fn iter :in base]]]
      [:repeat [(into [mapset-fn iter :in base] source)]])))

(defn reduce-expr
  [acc expressions]
  (let [fltr nil]
  (reduce #(if (nil? %2)
             %1
             (update-in %1 [1] conj (%2))) acc expressions)))

(defn sets
  [& expressions]
  (fn s-fns []
    (reduce-expr [:sets []] expressions)))

(defn do-block
  [& expressions]
  (fn db-fns []
    (reduce-expr [:do-block []] expressions)))

(defn repeater
  [head & expressions]
  (fn peter []
    (reduce-expr (head) expressions)))

;;;
;;; AST Generation
;;;


(defn- extend-nesters
  "Extends kw collection with newv(alue) in nesters"
  [kw newv nesters]
  (map #(update-in % [kw] conj newv) nesters))

(defn- set-nesters
  "Extends kw element with newv(alue) in nesters"
  [kw newv nesters]
  (map #(assoc-in % [kw] newv) nesters))

(declare repeat-ast)

(defn- sequence-args
  [block]
  (loop [x (first (drop 1 block))
         y (drop 2 block)
         a (vector (first block))]
    (if (nil? x)
      a
      (recur (first y) (rest y)
             (conj a (if (fn? x)
                       x
                       (repeat-ast x true)))))))

(defn- primary-sets-ast
  ""
  [mymap myloop setters setlu-fn]
  (loop [x (first setters)
         r (rest setters)
         a (vector (apply set-map mymap (seq setters)))]
    (if (nil? x)
      (apply sets a)
      (recur (first r) (rest r) (conj a (set-map-set setlu-fn x mymap x myloop))))))

(defn extract-primary-sets
  [{:keys [instance instance-flt sub-setters setters map-name mapset-fn]}]
  (let [ps (vector (primary-sets-ast map-name (or instance-flt instance)
                                     setters mapset-fn))
        ss (conj ps (map #(primary-sets-ast
                           (:map-name %)
                           (or instance-flt instance)
                           (:setters %)
                           (:mapset-fn %)) sub-setters))]
  (flatten ss)))

(defn extract-primary-results
  "Creates the set end of or map extend results for sub-setters"
  [{:keys [map-name sub-setters] :as crs}]
  (let [lr (map #(set-list-end (:result-list %) (:map-name %) ) sub-setters)
        fs (filter #(not (nil? (:result-map %))) sub-setters)
        mr (reduce #(conj %1 (set-map-extend
                              map-name
                              (get lookaside (:result-map %2))
                              (:result-list %2))) (vector lr) fs)]
    (apply sets (flatten mr)))
  )

(defn- secondary-sets-ast
  ""
  [{:keys [result-list map-name nesters]}]
  (let [fs (set-list-end result-list map-name)
        res (reduce #(conj %1 (set-map-extend map-name
                                      (get lookaside (first (:target %2)) (first (:target %2)))
                                      (:result-list %2)
                                      ))
            (list fs) nesters)]
    (apply sets res))
  )

(defn collect-locals
  "Mapcat function that recurses through tree pulling all
  :global-locals declarations"
  [[k v]]
  (if (or (= k :sub-setters) (= k :nesters))
    (if (seq? v)
      (for [x v
            y (mapcat collect-locals x)]
        y)
      (mapcat collect-locals v))
    (if (= k :global-locals)
      v
      nil))
  )


(defn- extract-locals
  "Extracts the symbol(s) designated for the local(s)
  and returns the locals-fn"
  [crs]
  (let [x (map first (mapcat collect-locals crs))]
    (if (empty? x)
      nil
      (apply locals x))))


(defn- local-set-reduction
  ""
  [acc [f k & a]]
  (conj acc
        (cond
         (= k :list) (set-assign f k)
         (= k :map)  (set-map f a)
         (= k :properties) (set-properties-of f a))))


(defn- extract-local-sets
  "Extracts the set of type to the locals and includes
  in a set-fn"
  [crs]
  (let [x (mapcat collect-locals crs)]
    (if (empty? x)
      nil
      (apply sets (reduce local-set-reduction '() x)))))

;; Extract for:
;; global-locals (i.e. local x, y , z) for immediate/sub-setters
;; initializing locals (i.e. set x to {}) for immediate/sub-setters
;; set or set map (i.e. set x of y to z) for immediate/sub-setters
;; result assignments (i.e. set x to end of y) sub-setters
;; map extension (i.e. set x to x & {foo:bar} sub-setters

(defn primary-setup
  [crs inbase]
    (flatten
     (filter #(not (nil? %))
             [(if inbase (extract-locals crs))
              (if inbase (extract-local-sets crs))
              (extract-primary-sets crs)
              (extract-primary-results crs)])))

(defn repeat-ast
  "Top level repeat block emitter"
  [{:keys [instance instance-flt sub-setters target nesters setters map-name
           mapset-fn result-list result-map] :as crs} & [lvl]]
  (let [nested (extend-nesters :target (or instance-flt instance)
                            (set-nesters :result-map  map-name nesters))
        crs1 (assoc-in crs [:nesters] nested)
        ; Repeat fn
        rh   (repeat-head instance (first target) mapset-fn (rest target))
        ; Initial 'sets' and other data manip
        head (primary-setup crs1 (nil? lvl))
        ; sets fn post nesters
        ss (secondary-sets-ast crs1)
        block (sequence-args
               (flatten (filter #(not (nil? %)) [head nested ss])))]
    (repeater rh (apply do-block block))))

(defn preprocess-locals
  "Processes any pre-process assertions if present for
  local declarations"
  [cstruct]
  (if (nil? (:filters cstruct))
    nil
    (locals (:control-field (:filters cstruct)))))

(defn preprocess-filters
  "Processes any pre-procession assertions if present for
  creating the executable filtering setter"
  [cstruct]
  (if (nil? (:filters cstruct))
    nil
    (sets (set-filter-on (:filters cstruct)
                         (:mapset-fn cstruct)))))

;;;
;;;   AST Model - New Version
;;;

(defn mapset-none
  [term-kw]
  term-kw)

(def ^:private generic-predicate "If predicate expression"
  {
   :E   " equal "
   :NE  " not equal "
   :GT  " greater than "
   :LT  " less than "
   :missing "missing value"
   })

(defn mapset-expressions
  [term-kw]
  (get generic-predicate term-kw term-kw))

(defn reduce-gen
  [dmap]
  (reduce-kv #(assoc %1 %2 (if (= %3 :gen) (keyword (gensym)) %3)) dmap dmap))

(defn do-tell
  "Sets up the enclosing tell application construct"
  [token-fn target returns & expressions]
  {:type :do-tell
   :token-fn token-fn
   :target target
   :return returns
   :expressions expressions})


(defn do-routine
  "Setup the routine (in AS this is a handler)"
  [token-fn rname parameters & expressions]
  {:type :do-routine
   :token-fn token-fn
   :routine-name rname
   :parameters parameters
   :expressions expressions})

(defn do-if-then
  "Setup if statement"
  [token-fn value pred operand & then-expressions]
  {:type :do-ifthen
   :token-fn token-fn
   :test-value value
   :predicate pred
   :operand operand
   :expressions then-expressions})

(defn do-if-else
  "Set else statements"
  [token-fn & else-expressions]
  {:type :do-ifelse
   :token-fn token-fn
   :expressions else-expressions})

(defn do-return
  [token-fn retval]
  {:type :do-return
   :token-fn token-fn
   :return-val retval})


(defn do-block
  "Primary container of expressions - does not emit"
  [token-fn & expressions]
  {:type :do-block
   :token-fn token-fn
   :expressions expressions})

(defn do-blockf
  "Primary container of expressions - does not emit"
  [token-fn expressions]
  {:type :do-block
   :token-fn token-fn
   :expressions expressions})

(defn do-setlocal
  "Sets any locals described - emits 'local x,y,z'"
  [token-fn & terms]
  {:type :do-setlocal
   :token-fn token-fn
   :local-terms terms})


(defn do-setassign
  [token-fn target source]
  {:type :do-setassign
   :token-fn token-fn
   :setvalue target
   :setvalue-of source})


(defn do-setmap
  "Sets a map to mapvars - emits 'set target to {mv1: null, mvN:null}'"
  [token-fn target mapvars]
  {:type :do-setmap
   :token-fn token-fn
   :set target
   :set-to mapvars})


(defn do-setpropertiesof
  "Sets a variable to the properties of a class"
  [token-fn value properties-of]
  {:type :do-setpropertiesof
   :token-fn token-fn
   :value value
   :properties-of properties-of})

(defn do-setfilter
  "Set a variable to the result of a filter 'whose' statement"
  [token-fn value source source-of user-filter]
  {:type :do-setfilter
   :token-fn token-fn
   :source source       ; e.g. contacts/people
   :source-of source-of ;
   :value value         ; what var are we setting result to
   :user-filter user-filter})


(defn do-setvalueof
  "Sets a value from another value"
  [token-fn value from apply-function]
  {:type :do-setvalueof
   :token-fn token-fn
   :value value
   :from from
   :apply-function apply-function})


(defn do-setmapextent
  "Sets a value from another value"
  [token-fn targetmap keyw value]
  {:type :do-setmapextent
   :token-fn token-fn
   :target-map targetmap
   :value value
   :keywrd keyw})

(defn do-setmapvalue
  "Sets a value of key in map"
  [token-fn ofmap mapval to-expression]
  {:type :do-setmapvalue
   :token-fn token-fn
   :mapvalue mapval
   :ofmap ofmap
   :to to-expression})

(defn do-setlist
  "Sets target to a list type - emits 'set target to {}'"
  [token-fn target]
  {:type :do-setlist
   :token-fn token-fn
   :set target})

(defn do-setlist-end
  "Sets the end of target list to source - emits 'set end of target to source'"
  [token-fn target source]
  {:type :do-setlist-end
   :token-fn token-fn
   :set target
   :to source})

(defn do-repeat
  "Creates a repeat block - emits 'repeat with itervar in source'"
  [token-fn itervar source sourceof & repeat-expressions]
  {:type :do-repeat
   :token-fn token-fn
   :source source
   :iteration-var itervar
   :source-of sourceof
   :expressions repeat-expressions})

(defn do-repeatf
  "Creates a repeat block - emits 'repeat with itervar in source'"
  [token-fn itervar source sourceof repeat-expressions]
  {:type :do-repeat
   :token-fn token-fn
   :source source
   :iteration-var itervar
   :source-of sourceof
   :expressions repeat-expressions})

(defn do-filtered-repeat
  "Creates a filtering construct for repeating over"
  [token-fn property-var user-filter source sourceof & repeat-expressions]
  (reduce-gen {:type :do-filtered-repeat
   :token-fn token-fn
   :user-filter user-filter   ; user filter map
   :filter-result :gen        ; put result of filter 'whose' set
   :source source             ; target source object
   :source-of sourceof        ; target source object owner (optional)
   :iteration-var :gen        ; internal loop over filter-result
   :property-var property-var ; target of get properties of iteration-var
   :expressions repeat-expressions}))

;;
;; AST Functions
;;

(def cleanval "Takes an argument and test for 'missing value'. Returns value or null"
  (do-routine
   nil :cleanval :val
   (do-setlocal nil :oval)
   (do-setassign nil :oval :null)
   (do-if-then mapset-expressions :val :NE :missing
               (do-setassign nil :oval :val))
   (do-return nil :oval)))

