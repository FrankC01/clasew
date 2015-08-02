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
  {:addresses :address_list})

;;
;; AST Emitters
;;

(defn locals
  [& vals]
  (let [r (into [] (map vector vals))]
    (fn [] [:locals r])))

(defn set-assign
  [targ source]
  (fn [] [targ source]))

(defn set-list-end
  [lid vid]
  (fn [] [lid :end-of-list vid]))

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
  [iter base & [source]]
  (fn rh-fns []
    (if (or (= (count source) 0))
      [:repeat [[iter :in base]]]
      [:repeat [(into [iter :in base] source)]])))

(defn reduce-expr
  [acc expressions]
  (reduce #(update-in %1 [1] conj (%2)) acc expressions))

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
                       (repeat-ast x)))))))

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
  [{:keys [instance sub-setters setters map-name mapset-fn] :as crs}]
  (let [ps (vector (primary-sets-ast map-name instance setters mapset-fn))
        ss (conj ps (map #(primary-sets-ast
                           (:map-name %)
                           instance
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
         (= k :map)  (set-map f a))))


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
  [crs]
    (flatten
     (filter #(not (nil? %))
             [(extract-locals crs)
              (extract-local-sets crs)
              (extract-primary-sets crs)
              (extract-primary-results crs)])))

(defn repeat-ast
  "Top level repeat block emitter"
  [{:keys [instance sub-setters target nesters setters map-name
           result-list result-map] :as crs}]
  (let [nested (extend-nesters :target instance
                            (set-nesters :result-map  map-name nesters))
        crs1 (assoc-in crs [:nesters] nested)
        ; Repeat fn
        rh   (repeat-head instance (first target) (rest target))
        ; Initial 'sets' and other data manip
        head (primary-setup crs1)
        ; sets fn post nesters
        ss (secondary-sets-ast crs1)
        block (sequence-args
               (flatten (filter #(not (nil? %)) [head nested ss])))]
    (repeater rh (apply do-block block))
    ))
