# Introduction to clasew-excel DSL

***clasew*** is based on a core set of primitives for calling AppleScript vis-a-vis the Java ScriptManager interop. The DSLs (delivered and planned) leverage these primitives to simplify the developer interface to targeted applications. However; you can use them for whatever type AppleScript you want evaluated.

***clasew.excel*** is a DSL that builds on ***clasew.core***. Go [here](intro.md) for more informaiton on clasew.core.

The focus of this page are the higher order functions (HOF), found in the clasew.excel namespace, that can be utilized to simplify interacting with Excel via AppleScript.

## clasew.excel
As noted in the introduction, the main functions in this namespace are HOFs that provide a DSL that, under the covers, call the clasew.core primitive functions.

### Namespace Initialization
When clasew.excel is loaded:
+ Establishes a local ```(defonce ...)``` engine for it's use
+ Creates a number of operational vars (scalars and maps)

Unlike clasew.core, clasew.excel functions ***do not*** require providing an engine argument as it has a dedicated instance.

###Raw Materials

There are three (3) functions that simplify the preperation of calling AppleScript for Excel. Only one of these interact with clasew.core:
##### clasew-excel-call!
```
(defn clasew-excel-call!
  "Takes 1 or more maps produced from clasew-excel-script and invokes AppleScript
  for execution.
  Return map is same as clasew.core/run-ascript!"

  [& scripts]
  (...)
```
This function packages up the script input into the arguments accepted by ```run-ascript!```. Let's look at going about creating it's input.

#####clasew-excel-script
```
(defn clasew-excel-script
  "Produces a script data structure (map) ready to be used by casew-exec-call
  wkbk     - String containing name of the workbook (with extension)
  crf      - create if workbook named is not opened
  opf      - open if workbook named is not already opened
  fqn      - Fully qualifed unix path or AppleScript 'path to xxx' command
  handler  - result from clasew-excel-handler call"

  [wkbk crf opf fqn handlers]
  (...)
```
The output of calling this is a map that gets set to the ```:arguments ...``` option of ```run-ascript!``` The following is a breakdown of the arguments and variations
<table>
<tr><th>Argument</th><th>Description</th><th>Examples</th></tr>
<tr><td>wkbk</td><td>(required) String specifying the target workbook to operate on. Must include the file name extension</td><td>"clasew-ex4.xlsx",<br>"My Super Fantatic Workbook.xlsx"</td></tr>
<tr><td>crf</td><td>(required) A flag indicating that if the workbook identified in wkbk is not loaded in Excel already, that it should or should not be created</td><td>(predefined vars)<br>create,<br>no-create</td></tr>
<tr><td>opf</td><td>(required) A flag indicating that, if the workbook identified in wkbk is not loaded in Excel already, an attempt should be or should not be made to open it</td><td>(predefined vars)<br>open,<br>no-open</td></tr>
<tr><td>fqn</td><td>(required) Either a fully qualified unix style path or a AppleScript predfined [path command](https://developer.apple.com/library/mac/documentation/AppleScript/Conceptual/AppleScriptLangGuide/reference/ASLR_cmds.html#//apple_ref/doc/uid/TP40000983-CH216-SW19) (in quotes). This identifies where to load from, create in and save path for wkbk</td><td>"/Users/yourname/Deskop",<br>"path to desktop"</td></tr>
<tr><td>handler</td><td>(required) A map that identifies the handler(s) and their respective arguments to be called during execution</td><td>See the reference to ```clasew-excel-handler``` below</td></tr>
</table>

Here is a REPL ready example and resulting output:
```
(ns clasew.sample
  (:require [clasew.excel :as es]
            [clojure.pprint :refer :all])
  )

;;; Setup for the example

(def p pprint)

;;;
;;; Demonstrate using Excel to create a new workbook and save to desktop
;;;

(def sample1 (es/clasew-excel-script "clasew-sample.xlsx"
      es/create es/no-open "path to desktop"
      {:handler_list [] :arg_list []}))

(p sample1)

=> {"fqn_path" "path to desktop",
 "open_ifm" "false",
 "create_ifm" "true",
 "work_book" "clasew-sample.xlsx",
 "handler_list" [],
 "arg_list" []}

; Call excel

(p (es/clasew-excel-call! sample1))

=> {:reset-binding true,
 :arguments
 ([{"fqn_path" "path to desktop",
    "open_ifm" "false",
    "create_ifm" "true",
    "work_book" "clasew-sample.xlsx",
    "handler_list" [],
    "arg_list" []}]),
 :bind-function "clasew_excel_eval",
 :result [[{"create_wkbk" "clasew-sample.xlsx success"}]]}


```

A few comments before we move on:
+ The keys in the map are strings. This is to get past the AppleScript transition to AppleScript record types as keywords, numbers and other data structures don't translate. Even though we passed in keywords to the handler map, ```clasew-excel-script``` automatically converts keys to strings.
+ The open and create flags are in strings as well, "false" and "true" respectivly, to get past the interop inability to recognize native clojure booleans.
+ The following table demonstrates behavior of executing a script and the argument settings, specifically the ```opf``` and ```crf``` arguments:

<table>
<tr><th>if the workbook is</th><th>crf</th><th>opf</th><th>notes</th></tr>
<tr><td>opened</th><td>any</td><td>any</td><td>flags ignored</td></tr>
<tr><td>not opened</th><td>create</td><td>any</td><td>created and opened</td></tr>
<tr><td>not opened</th><td>no-create</td><td>open</td><td>attempts to open</td></tr>
<tr><td>not opened</th><td>no-create</td><td>no-open</td><td>error thrown</td></tr>
</table>

#####But wait?????
Where is the actual AppleScript script? The answer is that the clasew-excel DSL comes equiped with a master script that is used to manage the execution context and support a very flexible and better performing options. The script is hidden from view in the implementation and is automagically inserted during the execution of ```clasew-excel-call!```. You can take a look at it [here](../resources/clasew-excel.applescript). While this script is a bit bigger then you've seen in examples, there is a reason to this madness. But don't take my word for it...

####AppleScript performant... not really
Summary of the execution path (after clasew has done the pre-execution housekeeping):
1. The script, function entry point and arguments are passed to the Java AppleScriptEngine 'eval' method and,
2. In AppleScriptEngine the script is parsed, verified and compiled into an AppleScript object and,
3. The arguments are coerced (eg vectors to AppleScript lists) and,
4. The script executes and,
5. The return (if any) is coerced to Java objects and finally,
6. Control is passed back to clojure

Woof! You can see why these things won't move too quickly. And if we are doing something repetitively  (imagine calling this path from inside a clojure ```(for [...])``` or multiple discrete calls exhibited in examples1.clj through examples3.clj) the novelty quickly wears thin.

We could boost the throughput if we could have the script support the ability to do more than one thing while it has control. This is the basis of the provided, albeit hidden, clasew-excel.applescript script, to support...

####Chained Handlers
As oppresive as it sounds, chaining AppleScript handler (i.e. function) calls provides the ability to do something roughly similar to:
```
(comp quit-excel
  save-workbook
  read-data-here
  recalc-sheet1
  put-formula-in-range
  put-data-in-range
  create-workbook
  open-excel)
```
#####clasew-excel-handler


```
(defn clasew-excel-handler
  "Sets up the handler and argument lists
  chains - a collection of one or more chains"

  [chains]
  (...)
```

where **_chains_** are a collection (vector, sequence, set) of items and each item (a chain) is a vector containing a handler-identifier and optional arguments as exemplified in the following:
```
;;;
;;;   chain structure: [ handler-identity & arg0 argN ]
;;;
;;;   example          [:get-range-values "Sheet1" "A1:A10"]
;;;   example          ["clasew_excel_get_range_values" "Sheet1" "A1:A10"]
;;;
;;;   example          [:save-quit]
;;;   example          ["clasew_excel_save_and_quit"]
```


```clasew-excel-handler``` will convert the sequence to the form expected by ```clasew-excel-script```, as shown in the following example that builds upon the earlier demonstration:

```

;;; Demonstrate starting Excel, creating a new workbook, populating a range with values
;;; and getting the values from a subset of that range (1st column) to return and
;;; then saving the workbook and quitting Excel, returning the results

;; First, let's contrive some data
(def datum (into []
      (map vec
           (for [i (range 10)]
             (repeatedly 10 #(rand-int 100))))))

(p datum)


(def sample2 (es/clasew-excel-script "clasew-sample.xlsx"
      es/create es/no-open "path to desktop"
      (es/clasew-excel-handler [[:put-range-values "Sheet1" "A1:J10" datum]
                             [:get-range-values "Sheet1" "A1:A10"]
                             [:save-quit]])))

(p sample2)

;; Observe that the input was transposed and for each position item in the
;; "handler_list" vector there are associative parameters in "arg_list" vector matrix.
;; The third "arg_list" vector is empty as there are no arguments to the :save-quit handler

=> {"fqn_path" "path to desktop",
 "open_ifm" "false",
 "create_ifm" "true",
 "work_book" "clasew-sample.xlsx",
 "handler_list"
 ["clasew_excel_put_range_values"
  "clasew_excel_get_range_values"
  "clasew_excel_save_and_quit"],
 "arg_list"
 [["Sheet1"
   "A1:J10"
   [[79 64 50 96 1 38 50 77 68 17]
    [92 90 97 27 86 52 25 40 51 52]
    [21 88 96 5 79 87 17 81 9 90]
    [4 44 6 75 93 17 62 80 73 89]
    [65 63 38 79 7 9 92 56 5 43]
    [91 50 38 2 35 46 88 7 4 33]
    [60 44 49 88 80 86 77 44 29 18]
    [48 5 16 16 65 33 82 19 17 6]
    [27 51 90 26 88 26 69 24 95 29]
    [0 39 60 4 98 42 74 79 42 15]]]
  ["Sheet1" "A1:A10"]
  []]}

;; Call and show the results

(p (es/clasew-excel-call! sample2))

=> {:reset-binding true,
 :arguments
 ([{"fqn_path" "path to desktop",
    "open_ifm" "false",
    "create_ifm" "true",
    "work_book" "clasew-sample.xlsx",
    "handler_list"
    ["clasew_excel_put_range_values"
     "clasew_excel_get_range_values"
     "clasew_excel_save_and_quit"],
    "arg_list"
    [["Sheet1"
      "A1:J10"
      [[79 64 50 96 1 38 50 77 68 17]
       [92 90 97 27 86 52 25 40 51 52]
       [21 88 96 5 79 87 17 81 9 90]
       [4 44 6 75 93 17 62 80 73 89]
       [65 63 38 79 7 9 92 56 5 43]
       [91 50 38 2 35 46 88 7 4 33]
       [60 44 49 88 80 86 77 44 29 18]
       [48 5 16 16 65 33 82 19 17 6]
       [27 51 90 26 88 26 69 24 95 29]
       [0 39 60 4 98 42 74 79 42 15]]]
     ["Sheet1" "A1:A10"]
     []]}]),
 :bind-function "clasew_excel_eval",
 :result
 [[{"create_wkbk" "clasew-sample.xlsx success"}
   {"clasew_excel_put_range_values" "success"}
   {"clasew_excel_get_range_values" [[79.0] [92.0] [21.0] [4.0] [65.0] [91.0] [60.0] [48.0] [27.0] [0.0]]}
   {"clasew_excel_quit" "success"}]]}


```

As you can see, the input enables our main excel script to iterate through multiple handler calls while still in context of the AppleScriptEngine and eliminates the overhead of individual calls.

The following table (volatile and subject to change) describes each handler-identifier currently available to call, it's keyword shorthand and required arguments are shown as well:
<table>
<tr><th>handler (string)</th><th> _or_ handler (keyword)</th><th>description</th><th>argument position and sample</th></tr>
  <tr><td>clasew_excel_get_used_range_info</td><td>:get-used-range-info</td>
  <td>retrieves general information about the "used" range in the workbook's worksheet argument</td><td><ol><li>"Sheet1"</ol></td></tr>
<tr><td>clasew_excel_get_range_info</td><td>:get-range-info</td>
  <td>retrieves general information about user defined range in the workbook's worksheet</td><td><ol><li>"Sheet1"<li>"A1:J2"</ol></td></tr>
<tr><td>clasew_excel_get_range_formulas</td><td>:get-range-formulas</td>
  <td>retrieves forumulas from user defined range in the workbook's worksheet</td><td><ol><li>"Sheet1"<li>"A1:J2"</ol></td></tr>
<tr><td>clasew_excel_get_range_values</td><td>:get-range-values</td>
  <td>retrieves values from  user defined range in the workbook's worksheet</td><td><ol><li>"Sheet1"<li>"A1:J2"</ol></td></tr>
<tr><td>clasew_excel_put_range_values</td><td>:put-range-values</td>
  <td>sets values in user defined range in the workbook's worksheet</td><td><ol><li>"Sheet1"<li>"A1:B2"<li>[[1 2] [3 4]]</ol></td></tr>
<tr><td>clasew_excel_get_all_book_info</td><td>:all-book-info</td>
  <td>retrieves summary information about all open workbooks</td><td>n/a</td></tr>
<tr><td>clasew_excel_get_book_info</td><td>:book-info</td>
  <td>retrieves summary information about the containing script's workbook</td><td>n/a</td></tr>
<tr><td>clasew_excel_save</td><td>:save</td>
  <td>saves the containing script's workbook</td><td>n/a</td></tr>
<tr><td>clasew_excel_save_and_quit</td><td>:save-quit</td>
  <td>saves the containing script's workbook and quits Excel</td><td>n/a</td></tr>
<tr><td>clasew_excel_quit</td><td>:quit</td>
  <td>quits Excel however; if there are modified workbooks Excel will prompt to save</td><td>n/a</td></tr>
<tr><td>clasew_excel_save_as</td><td>:save-as</td>
  <td>saves the containing script's workbook to new name and path of choice</td><td><ol><li>"clasew-sample-copy.xlsx"<li>"path to desktop"</ol></td></tr>
<tr><td>clasew_excel_run</td><td>:run-script</td>
  <td>executes an arbitrary user provided script</td><td>see below for full description</td></tr>
</table>

#####clasew clasew_excel_run usage
clasew is not trying to realize every possible AppleScript Excel command. To that end, the intent is to provide the most common constructs in idiomatic clojure way. So in order to take advantage of the main script and the functions that clasew does provide, the ```clasew_excel_run``` option provides the means to execute any additional/arbitrary AppleScript.

At a minimum, when using this handler, the script string passed into clasew must be in the form of:
```
-- Before the script is executed, clasew prepares the two arguments
-- as input to this script
-- caller is the calling script object (i.e. clasew-excel.applescript)
-- args is the argument list passed in

on run(caller, args)
-- The meat of your script goes here
end run
```
Here is the source for the ```clasew_excel_run``` handler:
```

on clasew_excel_run(arguments)
  -- get the user script text from item one in the argument list (scpt)
  -- get the user script arguments from item two in the argument list (scpt_args)

  set scpt to item 1 of arguments
  set scpt_args to item 2 of arguments

  -- pass myself as the first argument to the scpt in case
  -- the script wants to call back into my handlers or access my properties
  -- pass the user provided arguments (scpt_args) as the second
  -- run script and return results

  return run script scpt with parameters {me, scpt_args}

end clasew_excel_run
```

Here is an simple example:
```
;; Demo pushing your own agenda

(def my_script
  "on run(caller, args)
    say \"I have control!\"
    return {my_script: \"No you don't\"}
  end run")

(def sample3 (es/clasew-excel-script "clasew-sample.xlsx"
      es/no-create es/open "path to desktop"
      (es/clasew-excel-handler [[:run-script my_script []]
                                [:quit]])))

(p (es/clasew-excel-call! sample3))

=> {:reset-binding true,
 :arguments
 ([{"fqn_path" "path to desktop",
    "open_ifm" "true",
    "create_ifm" "false",
    "work_book" "clasew-sample.xlsx",
    "handler_list" ["clasew_excel_run" "clasew_excel_quit"],
    "arg_list"
    [["on run(caller, args)\n    say \"I have control!\"\n    return {my_script: \"No you don't\"}\n  end run"
      []]
     []]}]),
 :bind-function "clasew_excel_eval",
 :result
 [[{"open_wkbk" "clasew-sample.xlsx success"}
   {"my_script" "No you don't"}
   {"clasew_excel_quit" "success"}]]}


```
####Raw Materials Conclusion
The previous has provided information on how to use the raw building block functions of the clasew-excel DSL. For the sample REPL code, feel free to tool around with them in [examples4.clj](../dev/src/clasew/examples4.clj), they have identifiers of sample1, sample2 and sample3 if you want to uncomment the associated ```(p (es/clasew-excel-call! ...))``` calls.

###Higher Order Functions
As noted earlier, we wanted to provide what we expect satisfies the most basic operational paradigm:

+ Create a new workbook, perform a number of opertions, save and exit
+ Open an existing workbook, perform a number of operations, save and exit

For that, the number of clasew-excel HOF (at the time of this writing) rings in at... not many.
#####create-wkbk
As the name implies, the primary purpose if to setup the script with pre-defined flag settings for creating a new workbook environment and executing as many handler chains as needed. This HOF, and ```open-wkbk``` also eliminate the need to call ```clasew-excel-script``` and ```clasew-excel-handler``` directly.

Quick example showing a side by side of using the raw materials and HOF:
````
;;
;;   Raw material setup call shown (tastes great)
;;   Create a workbook, throw some data at it, save and quit
;;

(def sample2 (es/clasew-excel-script "clasew-sample.xlsx"
      es/create es/no-open "path to desktop"
      (es/clasew-excel-handler [[:put-range-values "Sheet1" "A1:J10" datum]
                             [:get-range-values "Sheet1" "A1:A10"]
                             [:save-quit]])))


;;
;;   HOF (less filling)
;;

(def sample5 (es/create-wkbk "clasew-sample.xlsx" "path to desktop"
                     (es/chain-put-range-data "Sheet1" datum)
                     [:save-quit]))



````

#####open-wkbk
Like ```create-wkbk``` this HOF pre-defines a "opened workbook" environment.

Side by side:
````
;;   Raw material setup call shown (tastes great)

(def sample3 (es/clasew-excel-script "clasew-sample.xlsx"
      es/no-create es/open "path to desktop"
      (es/clasew-excel-handler [[:all-book-info]
                             [:quit]])))

;;   HOF (less filling)

(def sample6 (es/open-wkbk "clasew-sample.xlsx" "path to desktop"
                     [:all-book-info] [:quit]))

````
Looking back up the screen at the ```create-wkbk``` side by side, you may have noticed a function not discussed yet: ```chain-put-range-data```.

#####chain-put-range-data
This function will auto-caculate the Excel range signature (e.g. "A1:J10") from the dimensions of the data matrix passed in as an argument. Opetional column and row offsets can be supplied.
````
(defn chain-put-range-data
  "Sets up a chain for putting a data range into workbook's sheet-name. Output
  also includes the necessary Excel range signature.
  sheet-name - the name of the sheet to put the data in
  data - a vector of vectors with inner vectors containing the data
  start-col offset column for data 0 = A
  start-row offset row for data 0 = 1"

  [sheet-name data & [start-col start-row]]
  (...))

;; Examples
;;
;; (es/chain-put-range-data "Sheet1" datum)
;; (es/chain-put-range-data "Sheet1" datum 4 4)
;; (es/chain-put-range-data "Jan2015" [[Week Sales Returns] [1 20 2] [2 18 4] [3 52 6]])


````
#####chain-get-range-data
This function will supports specifying zero based range and offset coordinates.
````
(defn chain-get-range-data
  "Sets up a chain for getting a data range from workbook's sheet-name
  sheet-name - the name of the sheet to gett the data from
  start-col offset column for data 0 = A
  start-row offset row for data 0 = 1
  for-col number of columns to retrieve
  for-row number of rows to retrieve"

  [sheet-name start-col start-row for-col for-row]
  (...))

;; Examples
;;
;; (es/chain-get-range-data "Sheet1" 0 0) => A1
;; (es/chain-get-range-data "Sheet1" 0 0 1 1) => A1:B2
;; (es/chain-get-range-data "Sheet1" 3 0 2 2) => C1:E3


````

###Miscellaneous
A number of functions are available in ```clasew.excel``` to simplify common needs:

#####clean-excel-result
Used to convert ```:result``` block containing return of calling AppleScript. As noted previously, AppleScript converts it's 'record' type to a map however; the keys are converted to string upon return. ```clean-excel-result``` changes map keys to keywords.

```
(defn clean-excel-result
  "Iterates through the result vectors exchanging keywords for
  strings in any maps"

  [{:keys [result] :as return-map}]

```

Example:

```
(def wrkbk-name "clasew-ex4.xlsx")
(def wrkbk-path "path to desktop")


(def sample5 (es/create-wkbk wrkbk-name wrkbk-path
                     (es/chain-put-range-data "Sheet1" datum)
                     [:save-quit]))

;; Call and save result to s5r

(def s5r (es/clasew-excel-call! sample5))

;; Display

(p s5r)

=> {:reset-binding true,
 :arguments
 ([{"fqn_path" "path to desktop",
    "open_ifm" "false",
    "create_ifm" "true",
    "work_book" "clasew-ex4.xlsx",
    "handler_list"
    ["clasew_excel_put_range_values" "clasew_excel_save_and_quit"],
    "arg_list"
    [["Sheet1"
      "A1:J10"
      [[40 97 47 21 83 63 24 44 26 24]
       [90 69 44 30 95 28 65 63 33 55]
       [11 54 72 68 24 6 18 24 44 2]
       [94 66 2 98 25 36 42 19 17 91]
       [86 47 77 94 43 72 12 81 15 96]
       [69 68 5 8 39 61 49 57 42 64]
       [66 5 73 20 56 1 13 98 23 64]
       [52 60 73 48 25 72 83 69 95 76]
       [94 24 81 59 67 98 75 34 89 46]
       [58 51 81 73 68 76 80 73 54 25]]]
     []]}]),
 :bind-function "clasew_excel_eval",
 :result
 [[{"create_wkbk" "clasew-ex4.xlsx success"}
 {"clasew_excel_put_range_values" "success"} {
 "clasew_excel_quit" "success"}]]}

;; Clean and display

(p (es/clean-excel-result s5r))

= > {:reset-binding true,
 :arguments
 ([{"fqn_path" "path to desktop",
    "open_ifm" "false",
    "create_ifm" "true",
    "work_book" "clasew-ex4.xlsx",
    "handler_list"
    ["clasew_excel_put_range_values" "clasew_excel_save_and_quit"],
    "arg_list"
    [["Sheet1"
      "A1:J10"
      [[40 97 47 21 83 63 24 44 26 24]
       [90 69 44 30 95 28 65 63 33 55]
       [11 54 72 68 24 6 18 24 44 2]
       [94 66 2 98 25 36 42 19 17 91]
       [86 47 77 94 43 72 12 81 15 96]
       [69 68 5 8 39 61 49 57 42 64]
       [66 5 73 20 56 1 13 98 23 64]
       [52 60 73 48 25 72 83 69 95 76]
       [94 24 81 59 67 98 75 34 89 46]
       [58 51 81 73 68 76 80 73 54 25]]]
     []]}]),
 :bind-function "clasew_excel_eval",
 :result
 [[{:create_wkbk "clasew-ex4.xlsx success"}
   {:clasew_excel_put_range_values "success"}
   {:clasew_excel_quit "success"}]]}
```

#####get-excel-a1
Used by previously discussed ```chain-put-range-data``` and ```chain-get-range-data``` to convert arguments to Excel "A1" format
```
(defn get-excel-a1
  "Convert zero based column and row number to Excel 'A1' address form"

  [col-num row-num]
```
Example:
```
(es/get-excel-a1 0 0)
=> "A1"

(es/get-excel-a1 7 14)
=> "H15"

(let [range_start (es/get-excel-a1 3 2)
      range_end   (es/get-excel-a1 7 3)]
  (str range_start ":" range_end))
=>"D3:H4"
```
