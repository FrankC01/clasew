# Introduction to clasew

***clasew*** is based on a core set of primitives for calling AppleScript vis-a-vis the Java ScriptManager interop. The DSLs (delivered and planned) leverage these primitives to simplify the developer interface to targeted applications. However; you can use them for whatever type AppleScript you want evaluated.

The focus of this page are the primitive functions found in the clasew.core namespace. Examples have been provided in the dev/src/clasew directionr and we will reference those from here.

At the bottom of this page you will find links to the currently packaged DSLs.

## clasew.core

### Namespace Initialization
When core is loaded (REPL or Excecution) an instance of the AppleScriptEngineFactory is created. From that point forward, all references to Java or objects of the factory are ecapsulated by casew equivalents. The core object for evaluating AppleScript (in-line strings or resource files) is the ScriptEngine. Most functions in clasew require an engine reference.

### Quickstart
Let's explore the functions. The codeblocks in the quickstart are taken from  [here](../dev/src/clasew/examples1.clj).

Setup the namespace

```
(ns
  ^{:author "Frank V. Castellucci"
      :doc "clasew example 1 - basic"}
  clasew.examples1
  (:require [clasew.core :as as]
            [clojure.pprint :refer :all])
  )

```
Get an engine instance to use locally
```
(defonce ^:private local-eng as/new-eng)   ; Use engine for this namespace
(def p pprint)                             ; Pretty printer shorthand
```
Define a script. Note that if your editor does not insert new lines at the end of each line, you will need to add ```"\n"``` yourself, in addition all embeded strings are escaped, this is required if you are sending a script as a string into the engine. See below for using script files which avoid all the doctoring of new lines and escaped text.
```
(def hello-world
  "tell application \"Finder\"
    try
      display dialog \"Hello World\"
    on error number -128
    {not_ok: \"Cancel\"}
    end try
  end tell")

```
Execute the script.
```
;; Call the script using most basic form of clasew script execution.
;; run-ascript always returns a map containing, at least, :result of the call
;; Assumes:
;; Note 1. Use of the engine's default binding

(p (as/run-ascript! local-eng hello-world))

;; returns {:result {1651009908 "OK"}} or {:result {"not_ok" "Cancel"}}
```
At this point, OSX Finder should be alerting you to a dialog box that needs answering. In addition, the results of the execution are returned to the caller in a map (explained below).

### Raw Materials
Let's start by reviewing ```run-ascript!```
````
(defn run-ascript!
  [engine script & {:keys [reset-binding bind-function use-binding arguments]
                    :or {reset-binding false} :as full-dump}]

````
Here are the argument references. Examples of the calling combinations can be found  [example 2](../dev/src/clasew/examples2.clj) and [example 3](../dev/src/clasew/examples3.clj) in the **dev/src/clasew** folder.
<table>
<tr><th>Argument</th><th>Description</th><th>Notes</th></tr>
<tr><td>engine</td><td>(required) reference to an engine instance</td><td></td></tr>
<tr><td>script</td><td>(required) string or clojure.java.io/reader</td><td>See example 2 for latter</td></tr>
<tr><td>:reset-binding</td><td>(optional) if true, clears out bindings. Defaults to false. This applies to either the engine default binding or the binding passed in the ':use-binding' option</td><td></td></tr>
<tr><td>:bind-function</td><td>(optional) if the target of the script invocation is a handler, set this to the name of the applescript handler. This value is set on the binding "javax.script.function" key.</td><td>example 2</td></tr>
<tr><td>:use-binding</td><td>(optional) use a different binding map for engine invocation. If not set, the engine binding is used</td><td>example 3</td></tr>
<tr><td>:arguments</td><td>(optional) if a function is identied and it takes arguments, this should be set to the data being passed into the function. This value is set on the binding "apple.applescript.AppleScriptEngine/ARGV" key </td><td>example 2 and 3</td></tr>
</table>
