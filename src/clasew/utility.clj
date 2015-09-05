(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - common utilities"}
  clasew.utility
  (:require [clojure.walk :as w]))

(def to-strings #(str (name %)))

;;;
;;;   Result cleanups
;;;

(defn modify-keys
  "Uses zipmap to process 'f' on keys
  Used for record types in scripts"
  [f m]
  (if (map? m)
    (zipmap (map f (keys m)) (vals m))
    m))

(defn- ocm
  "prewalk function takes output from AppleScript
  converts to clojure types and, if map, swizzle string to keyword keys"
  [item]
  (if (instance? java.util.ArrayList item)
    (into [] item)
    (if (instance? java.util.HashMap item)
      (modify-keys keyword (into {} item))
      item)))

(defn clean-result
  "Iterates through the result vectors exchanging keywords for
  strings in any maps"
  [{:keys [result] :as return-map}]
  (assoc return-map :result (w/prewalk ocm result) ))

(defn handler-acc
  "Accumulator function for handler setup reduce"
  [handler-map acc [v1 & v2]]
  (update-in (update-in acc [:handler_list] conj (get handler-map v1 v1))
             [:arg_list] conj (if (nil? v2)
                                []
                                (if (vector? v2) v2 (into [] v2)))))

