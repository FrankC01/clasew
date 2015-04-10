(ns
  ^{:author "Frank V. Castellucci"
      :doc "spike space"}
  clasew.sandbox
  (:require [clojure.string :as s]
            [clojure.pprint :refer :all])
  (:import (apple.applescript AppleScriptEngineFactory))
  )

(def p pprint)

;;; An instance of the factory produces engine instances

(def as-factory (AppleScriptEngineFactory.))
(def binding-function-key "javax_script_function")
(def binding-argument-key "javax.script.argv")

(defn new-engine
  "Produce a new engine instance from the core factory"
  []
  (.getScriptEngine as-factory))

(defn engine-context
  "Get an engine context, typically to set it's bindings for calling
  applescript with arguments"
  [engine]
  (.getContext engine))


(defn engine-bindings
  "Get an engines binding object, usually to set it with function and
  parameters (optional)"
  [engine]
  (.getBindings (engine-context engine) javax.script.SimpleScriptContext/ENGINE_SCOPE))

(defn bind-function!
  [engine func-name]
  (.put (engine-bindings engine) binding-function-key func-name))

(defn bind-arg!
  [engine & args]
  (.put (engine-bindings engine) binding-argument-key
        (if (= (count args) 1)
          (first args)
          (list (vec args)))))

(defn reset-arg!
  ([engine] (reset-arg! engine binding-argument-key))
  ([engine kw] (.put (engine-bindings engine kw ""))))

(defn remove-arg!
  ([engine] (remove-arg! engine binding-argument-key))
  ([engine kw] (.remove (engine-bindings engine) kw)))

(def t1 (new-engine))

(def saystrings "on saystrings(names)
  repeat with name in names
  say name
  end repeat
  return names
  end saystrings")

(bind-function! t1 "saystrings")
(bind-arg! t1 "Casew" "is" "way cool")
(p (.eval t1 saystrings))

(def evalarg "on evalarg(x)
  return class of x
  end evalarg")
;
;(bind-function! t1 "evalarg")
;(bind-arg! t1 {"name:" "Frank"})
;(p (.eval t1 evalarg))

(p (engine-bindings t1))




