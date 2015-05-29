# clasew-numbers DSL

***clasew*** is based on a core set of primitives for calling AppleScript vis-a-vis the Java ScriptManager interop. The DSLs (delivered and planned) leverage these primitives to simplify the developer interface to targeted applications. However; you can use them for whatever type AppleScript you want evaluated.

***clasew.numbers*** is a minor DSL function that works in conjunction with ***clasew.spreads***. Go [here](clasew-spreads.md) for more informaiton on clasew.spreads.

The focus of this page is the higher order function (HOF), found in the clasew.numbers namespace, that directs ```clasew-core``` to simplify interacting with Apple's Numbers via AppleScript.

## clasew.numbers
As noted in the introduction, the main function calls the ```clasew.core``` primitive functions.

### Namespace Initialization
When clasew.numbers is loaded:
+ Establishes a local ```(defonce ...)``` engine for it's use

Unlike ```clasew.core```, ```clasew.numbers``` ***does not*** require providing an engine argument as it has a dedicated instance.

###Raw Materials

There is one (1) function that simplify the preperation of calling AppleScript for Numbers that interacts with ```clasew.core```:
##### clasew-numbers-call!
```clojure
(defn clasew-numbers-call!
  "Takes 1 or more maps produced from clasew-script and invokes AppleScript
  for execution.
  Return map is same as clasew.core/run-ascript!"

  [& scripts]
  (...)
```
This function packages up script inputs into the arguments accepted by ```run-ascript!```.

Here is a REPL ready example and resulting output:
```clojure
(ns
  ^{:author "Frank V. Castellucci"
      :doc "clasew example 5 - Apple Numbers Examples"}
  clasew.examples5
  (:require [clasew.spreads :as cs]
            [clasew.numbers :as an]
            [clojure.pprint :refer :all])
  )


;;; Setup for the example

(def p pprint)

;;;
;;; Demonstrate using Excel to create a new workbook and save to desktop
;;;

(def sample1 (cs/clasew-script "clasew-sample.numbers"
      cs/create cs/no-open "path to desktop" nil
      {:handler_list [] :arg_list []}))

(p sample1)

=> {"fqn_path" "path to desktop",
 "open_ifm" "false",
 "create_ifm" "true",
 "work_book" "clasew-sample.numbers",
 "handler_list" [],
 "arg_list" []}

; Call excel

(p (an/clasew-numbers-call! sample1))

=> {:reset-binding true,
 :arguments
 ([{"fqn_path" "path to desktop",
    "open_ifm" "false",
    "create_ifm" "true",
    "work_book" "clasew-sample.numbers",
    "handler_list" [],
    "arg_list" []}]),
 :bind-function "clasew_eval",
 :result [[{"create_wkbk" "clasew-sample.numbers success"}]]}
```

