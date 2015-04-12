(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper core"}
  clasew.core
  (:import (apple.applescript AppleScriptEngineFactory)))

;;; An instance of the factory produces engine instances

;;; Common usage short-cuts

(defonce ^:private as-factory (AppleScriptEngineFactory.))
(defonce ^:private as-function-key "javax.script.function")
(defonce ^:private as-argument-key apple.applescript.AppleScriptEngine/ARGV)
(defonce ^:private as-escope javax.script.SimpleScriptContext/ENGINE_SCOPE)

;;; Core getters

(def new-eng "Create an engine instance" (.getScriptEngine as-factory))
(def eng-ctx "Gets an engines context" #(.getContext %))
(def eng-bnd "Gets the engines binding" #(.getBindings (eng-ctx %) as-escope))

;;; Binding functions

(defn empty-binding!
  [engine]
  (.createBindings engine))

(defn bind-eng-function!
  "Binds func-name to the standard script function key
  engine - instance of AppleScriptEngine
  func-name - string naming the AppleScript event to run
  bnd - (optional) instance of SimpleBindings to write to.
  If 'bnd' not provided, bind-eng-function! will use the engines binding"
  ([engine func-name]
   (bind-eng-function! engine func-name (eng-bnd engine)))
  ([engine func-name bnd]
   (.put bnd as-function-key func-name)))

(defn bind-eng-args!
  "Binds func-name to the standard script function key
  engine - instance of AppleScriptEngine
  arg-coll - {} [] or () arguments being passed.
  bnd - (optional) instance of SimpleBindings to write to.
  If 'bnd' not provided, bind-eng-args! will use the engines binding"
  ([engine arg-coll] (bind-eng-args! engine arg-coll (eng-bnd engine)))
  ([engine arg-coll bnd]
    (.put bnd as-argument-key arg-coll)))

(defn remove-eng-function!
  ([engine] (remove-eng-function! engine (eng-bnd engine)))
  ([engine bnd] (.remove bnd as-function-key)))


(defn reset-eng-bnd!
  "Reset the script engine bindings to their defaults
  engine - instance of AppleScriptEngine
  bnd - (optional) instance of SimpleBindings
  If 'bnd' not provided, reset-eng-bnd! will reset the engine bindings`"
  ([engine]
   (do
     (remove-eng-function! engine )
     (bind-eng-args! engine "")) nil)
  ([engine bnd]
   (do
     (remove-eng-function! engine bnd)
     (bind-eng-args! engine "" bnd))))

;;; Invoke AppleScript

(defn run-ascript
  "Runs the AppleScript
  engine - instance of AppleScriptEngine
  script - the script (string) to run. Mind your escapes and new lines
  parms - optional parameters:
  :reset-binding [(default) false | true] - Ask to clear binding
  :bind-function string - Binds the function name string to the binding
  :use-binding instance of SimpleBindings - Used as alternate to engines bindings
  :arguments either a vector or a list containing a vector - arguments to function
  "
  [engine script & {:keys [reset-binding bind-function use-binding arguments]
                    :or {reset-binding false} :as full-dump}]
  (let [target (or use-binding (eng-bnd engine))]
    (or (false? reset-binding) (reset-eng-bnd! engine target))
    (or (nil? bind-function)  (bind-eng-function! engine bind-function target))
    (or (nil? arguments) (bind-eng-args! engine arguments target))
    (assoc full-dump :result (.eval engine script target))))

