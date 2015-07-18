(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - Experimental AST emitter"}
  clasew.ast-emit
  (:require [clasew.utility :as util]
            [clojure.java.io :as io]
            [clojure.walk :as w]))

;;;
;;; AST Emitters
;;;

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
  [target & srcs]
  (fn [] (into [target :map-set] srcs)))

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
  [mymap myloop setters]
  (loop [x (first setters)
         r (rest setters)
         a (vector (apply set-map mymap (seq setters)))]
    (if (nil? x)
      (apply sets a)
      (recur (first r) (rest r) (conj a (set-map-set x mymap x myloop))))))

(defn- secondary-sets-ast
  [{:keys [result-list map-name nesters]}]
  (let [fs (set-list-end result-list map-name)
        res (reduce #(conj %1 (set-map-extend map-name
                                      (first (:target %2))
                                      (:result-list %2)
                                      ))
            (list fs) nesters)]
    (apply sets res))
  )

(defn- get-glocals
  [nesters]
  (map #(:global-locals %) nesters))

(defn- nester-locals
  [nesters]
  (let [gl (first (get-glocals nesters))]
    (if (nil? gl)
      nil
      (apply locals (reduce #(conj %1 (first %2)) '() gl)))))


(defn- nester-set-reduction
  [acc [f k & a]]
  (conj acc
        (cond
         (= k :list) (set-assign f k)
         (= k :map)  (set-map f a))))


(defn- nester-locals-set
  [nesters]
  (let [gl (filter #(> (count %) 1) (first (get-glocals nesters)))]
    (if (empty? gl)
      nil
      (apply sets (reduce nester-set-reduction '() gl)))))

(defn repeat-ast
  [{:keys [instance target nesters setters map-name
           result-list result-map] :as crs}]
  (let [nst (extend-nesters :target instance
                            (set-nesters :result-map  map-name nesters))
        crs1 (assoc-in crs [:nesters] nst)
        ; Repeat fn
        rh   (repeat-head instance (first target) (rest target))
        ; local fn for nesteds
        gl   (nester-locals nst)
        ; sets fn - sets for nested locals
        gs   (nester-locals-set nst)
        ; sets fn before nesters
        ps (primary-sets-ast map-name instance setters)
        ; sets fn post nesters
        ss (secondary-sets-ast crs1)
        block (sequence-args
               (flatten (filter #(not (nil? %)) [ps gl gs nst ss])))]
    (repeater rh (apply do-block block))
    ))
