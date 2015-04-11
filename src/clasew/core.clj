(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper core"}
  clasew.core
  (:import (apple.applescript AppleScriptEngineFactory)))

;;; An instance of the factory produces engine instances

;;; Common usage short-cuts

(defonce ^:private as-factory (AppleScriptEngineFactory.))
(defonce ^:private as-function-key "javax.script.function")
(defonce ^:private as-argument-key "javax.script.argv")
(defonce ^:private as-escope javax.script.SimpleScriptContext/ENGINE_SCOPE)

;;; Core getters

(def new-eng "Create an engine instance" (.getScriptEngine as-factory))
(def eng-ctx "Gets an engines context" #(.getContext %))
(def eng-bnd "Gets an engines binding" #(.getBindings (eng-ctx %) as-escope))

;;; Binding functions

(defn bind-eng-function-name!
  "Binds the func-name string to the script function key in an engines binding"
  [engine func-name]
  (.put (eng-bnd engine) as-function-key func-name))

(defn bind-eng-args!
  "Binds the args to the script argument key in an engines bindings"
  [engine & args]
  (.put (eng-bnd engine) as-argument-key
        (if (= (count args) 1)
          (first args)
          (list (vec args)))))

(defn reset-eng-bnd!
  "Reset the script engine bindings to their defaults"
  [engine]
  (do
    (bind-eng-function-name! engine "")
    (bind-eng-args! engine "")) nil)

;;; Run functions

(defn run-ascript
  "runs an applescript engine script as passed in argument"
  [engine script]
  (.eval engine script))
