# Introduction to clasew.spreads DSL

***clasew*** is based on a core set of primitives for calling AppleScript vis-a-vis the Java ScriptManager interop. The DSLs (delivered and planned) leverage these primitives to simplify the developer interface to targeted applications. However; you can use them for whatever type AppleScript you want evaluated.

***clasew.spreads*** is a DSL that builds on ***clasew.core***. Go [here](intro.md) for more informaiton on clasew.core. The primary functionality that ```clasew.spreads``` brings to the table is to abstract 99% of a generic spreadsheet DSL all the way up to using either the ```clasew.excel``` or ```clasew.numbers``` DSLs.

The focus of this page are the higher order functions (HOF), found in the clasew.spreads namespace, that can be utilized to simplify interacting with either Excel or Numbers via AppleScript.

## clasew.spreads
As noted in the introduction, the main functions in this namespace are HOFs that provide a DSL that, under the covers, call the clasew.core primitive functions.

### Namespace Initialization
When clasew.spreads is loaded:
+ Creates a number of operational vars (scalars, maps, etc.)

Unlike clasew.core, ```clasew.spreads``` functions ***do not*** require providing an engine as that is relegated to the specific spreadsheet DSL. For more information and release notes regarding application specific handling,  refer to:

+ Apple Numbers forms - See the  [documentation](clasew-numbers.md)
+ Microsoft Excel forms - See the  [documentation](clasew-excel.md)

###Raw Materials

There are two (2) core functions that simplify the preperation of calling AppleScript for Excel or Numbers applications. The primary focus of these forms is on getting ready the script payload before calling out to the respective technollogy DSL.


#####clasew-script
```clojure
(defn clasew-script
  "Produces a script data structure (map) ready to be used by casew-yyyy-call!
  wkbk     - String containing name of the workbook (with extension)
  crf      - Create if workbook named is not opened
  opf      - Open if workbook named is not already opened
  fqn      - Fully qualifed unix path or AppleScript 'path to xxx' command
  cprm     - Creation Parameter Map
  handlers - result from clasew-handler call"

  [wkbk crf opf fqn cprm handlers]
  (...)
```
The output of calling this is a map that gets set to the ```:arguments ...``` option of ```run-ascript!``` The following is a breakdown of the arguments and variations
<table>
<tr><th>Argument</th><th>Description</th><th>Examples</th></tr>
<tr><td>wkbk</td><td>(required) String specifying the target workbook to operate on. Must include the file name extension</td><td>"clasew-ex4.xlsx",<br>"My Workbook.numbers"</td></tr>
<tr><td>crf</td><td>(required) A flag indicating that if the workbook identified in wkbk is not loaded in Excel already, that it should or should not be created</td><td>(predefined vars)<br>create,<br>no-create</td></tr>
<tr><td>opf</td><td>(required) A flag indicating that, if the workbook identified in wkbk is not loaded in Excel already, an attempt should be or should not be made to open it</td><td>(predefined vars)<br>open,<br>no-open</td></tr>
<tr><td>fqn</td><td>(required) Either a fully qualified unix style path or a AppleScript predfined [path command](https://developer.apple.com/library/mac/documentation/AppleScript/Conceptual/AppleScriptLangGuide/reference/ASLR_cmds.html#//apple_ref/doc/uid/TP40000983-CH216-SW19) (in quotes). This identifies where to load from, create in and save path for wkbk</td><td>"/Users/yourname/Deskop",<br>"path to desktop"</td></tr>
<tr><td>cprm</td><td>(required) A map describing creation properties (if calling to create a new spreadsheet). Can be nil</td><td>See the reference to cprm keys below</td></tr>
<tr><td>handlers</td><td>(required) A map that identifies the handler(s) and their respective arguments to be called during execution</td><td>See the reference to ```clasew-handler``` below</td></tr>
</table>

#####clasew-script creation parameters (cprm) keys
This parameter of ```clasew.spreads/clasew-script``` can be *nil* or a map produced by the ```create-parms, tables and table```functions. If you are going to create your own parameters and forego the supplied functions, you must ensure that map keys are strings. If a technology does not use a particular keyword, it's value can be set to nil.

#####create-parms
The table below describes the function arguments keywords, values, default substitutions and spreadsheet technology behaviors.

<table>
<tr><th>Keys</th><th>Value</th><th>Default</th><th>Excel Behavior</th><th>Numbers Behavior</th><tr>
<tr><td>:template_name</td><td>A string containing the name of the template to base initial format/style of the new workbook</td><td>"Blank"</td><td>ignored</td><td>Creates the new workbook based on template name, if template is found</td></tr>
<tr><td>:sheet_name</td><td>A string containing the name of the initial sheet of the new workbook</td><td>"Sheet 1"</td><td>Sets name of first sheet of the new workbook</td><td>same as Excel</td></tr>
<tr><td>:table_list</td><td>A vector of table definition maps. *See descriptions of ```tables and table``` functions below.*</td><td>[]</td><td>Creates one or more tables on the first sheet of the new workbook</td><td>same as Excel</td></tr>
</table>

#####tables
The ```tables``` consumes and reduces 1 or more ```table``` definition maps and readies for usage in the AppleScript creation of workbook or additions of sheets.

```clojure
(defn tables
  "Collects and reduces table definitions to vector"
  [& table-defs]
```

#####table
The ```table``` function produces a coherent table definition for table creation on spreadsheets. The result is ready to be consumed by the ```tables``` reduction function (see above). Table definitions are applicable to workbook and sheet creation. The following descripes the function argument keywords, value description, default substitutions and spreadsheet technology behaviors.

<table>
<tr><th>Keys</th><th>Value</th><th>Default</th><th>Excel Behavior</th><th>Numbers Behavior</th><tr>
<tr><td>:table_name</td><td>A string containing the name to set the new table to</td><td>"Table 1"</td><td>Creates the table with the name supplied</td><td>same as Excel</td></tr>
<tr><td>:column_offset</td><td>Table starting column offset in sheet</td><td>0</td><td>Used in  range string production</td><td>ignored</td></tr>
<tr><td>:row_offset</td><td>Table starting row offset in sheet</td><td>0</td><td>Used in range string production</td><td>ignored</td></tr>
<tr><td>:column_count</td><td>Total column count inclusive of headers when setting up table.</td><td>10</td><td>Sets the number of columns for the new table</td><td>same as Excel</td></tr>
<tr><td>:row_count</td><td>Total row count inclusive of headers when setting up table.</td><td>10</td><td>Sets the number of columns for the new table</td><td>same as Excel</td></tr>
<tr><td>:header_column_count</td><td>Sets the number of header columns for the new table</td><td>0</td><td>ignored</td><td>Sets template contrast or default color for number of columns indicated</td></tr>
<tr><td>:header_row_count</td><td>Sets the number of header rows for the new table</td><td>0</td><td>ignored</td><td>Sets contrast template or default color for number of rows indicated</td></tr>
<tr><td>:header_content</td><td>A vector of strings containing the column header names</td><td>[]</td><td>Fills the top of the column (header) names in the table range</td><td>same as Excel</td></tr>
</table>

Here is the usage of ```tables and table``` in the context of ```create-parms```:
```clojure

(cs/create-parms
  :sheet_name "Content"
  :table_list
  (cs/tables (cs/table :table_name "First Table",
          :column_offset 1,
          :row_offset 1,
          :column_count 5,
          :row_count 5,
          :header_content ["Date","Region","Sales"])))
```
Usage of ```tables and table``` is also applicable with ```chain-add-sheet```:
```clojure

(cs/chain-add-sheet
  "Before Content" :before "Content"
  (cs/tables (cs/table :table_name "Second Table",
          :column_offset 1,
          :row_offset 1,
          :column_count 5,
          :row_count 5,
          :header_content ["Date","Region","Sales"])))
```


Here is a REPL ready example and resulting output **not using cprm**:
```clojure
(ns clasew.sample
  (:require [clasew.spreads :as cs]
            [clasew.excel :as es]         ; If calling MS Excel
            [clasew.numbers :as an]       ; If calling Apple's Numbers
            [clojure.pprint :refer :all])
  )

;;; Setup for the example

(def p pprint)

;;;
;;; Demonstrate creating a new Excel workbook and save to desktop
;;;

(p (cs/clasew-script "clasew-ex5-sample1.xlsx"
      cs/create cs/no-open "path to desktop" nil
      {:handler_list ["clasew_save_and_quit"] :arg_list [[]]}))


=> {"create_parms" {},
"fqn_path" "path to desktop",
 "open_ifm" "false",
 "create_ifm" "true",
 "work_book" "clasew-ex5-sample1.xlsx",
 "handler_list" ["clasew_save_and_quit"],
 "arg_list" [[]]}

;;;
;;; Demonstrate same for creating a new Numbers workbook and save to desktop
;;; The only difference is in the file name extensions
;;;

(cs/clasew-script "clasew-ex5-sample1.numbers"
      cs/create cs/no-open "path to desktop" nil
      {:handler_list ["clasew_save_and_quit"] :arg_list [[]]})

```
Here is a REPL ready example and resulting output **using cprm**:
```clojure
;;; Using create parameters

(p (cs/clasew-script "clasew-ex5-sample1.xlsx"
      cs/create cs/no-open "path to desktop"
      {"template_name" "Blank",     ; ignored by Excel
       "sheet_name" "Home",         ; will rename sheet
       "table_name" "Info Table",   ; ignored by Excel
       "row_count" 10,              ; ignored by Excel
       "column_count" 10,           ; ignored by Excel
       "header_row_count" 0,        ; ignored by Excel
       "header_column_count" 0}     ; ignored by Excel
      {:handler_list ["clasew_save_and_quit"] :arg_list [[]]}))

=> {"create_parms"
 {"template_name" "Blank",
  "sheet_name" "Home",
  "table_name" "Info Table",
  "row_count" 10,
  "column_count" 10,
  "header_row_count" 0,
  "header_column_count" 0},
 "fqn_path" "path to desktop",
 "open_ifm" "false",
 "create_ifm" "true",
 "work_book" "clasew-ex5-sample1.xlsx",
 "handler_list" ["clasew_save_and_quit"],
 "arg_list" [[]]}


;;; Numbers using create parameters

(cs/clasew-script "clasew-ex5-sample1.numbers"
      cs/create cs/no-open "path to desktop"
      {"template_name" "Blank",
       "sheet_name" "Home",
       "table_name" "Info Table",
       "row_count" 10,
       "column_count" 10,
       "header_row_count" 0,
       "header_column_count" 0}
      {:handler_list ["clasew_save_and_quit"] :arg_list [[]]})

```

A few comments before we move on:
+ Notice that the keys in the output map are strings. This is to get past the Java AppleScript Manager transition to AppleScript record types. Even though we passed in keywords for the handler map, ```clasew-script``` automatically converts keys to strings.
+ The open and create flags, and their corollaries, are strings as well that represent "true" or "false" to get past the interop inability to recognize native clojure booleans.
+ The following table demonstrates behavior of the open and create flags settings, specifically the ```opf``` and ```crf``` arguments:

<table>
<tr><th>if the workbook is already</th><th>crf</th><th>opf</th><th>notes</th></tr>
<tr><td>opened</th><td>any</td><td>any</td><td>flags ignored</td></tr>
<tr><td>not opened</th><td>create</td><td>any</td><td>created and opened</td></tr>
<tr><td>not opened</th><td>no-create</td><td>open</td><td>attempts to open</td></tr>
<tr><td>not opened</th><td>no-create</td><td>no-open</td><td>error thrown</td></tr>
</table>

#####But wait?????
Where is the actual AppleScript script? The answer is that both the ```clasew-excel``` and ```clasew.numbers``` DSL come equiped with a master script that is used to manage the execution context and support a very flexible and better performing options. The scripts are hidden in the implementation and are automagically inserted during the execution of either ```clasew-excel-call!``` or ```clasew-numbers-call!```.

####AppleScript performant... not really
Summary of the execution path (after clasew has done the pre-execution housekeeping):
1. The script, function entry point and arguments are passed to the Java AppleScriptEngine 'eval' method and,
2. In AppleScriptEngine the script is parsed, verified and compiled into an AppleScript object and,
3. The arguments are coerced (eg vectors to AppleScript lists) and,
4. The script executes and,
5. The return (if any) is coerced to Java objects and finally,
6. Control is passed back to clojure

Pretty clear why these things won't move too quickly. And if we are doing something repetitively  (imagine calling this path from inside a clojure ```(for [...])``` or multiple discrete calls exhibited in examples1.clj through examples3.clj) the novelty quickly wears thin.

We could enhance the throughput if we could have the script support the ability to do more than one thing while it has control. This is the basis of the provided, albeit hidden, ```clasew-excel.applescript``` and ```clasew-numbers.applescript``` scripts.


####Chained Handlers
As oppresive as it sounds, chaining AppleScript handler (i.e. function) calls provides the ability to do something roughly similar to:
```clojure
(comp quit-excel
  save-workbook
  read-data-here
  recalc-sheet1
  put-formula-in-range
  put-data-in-range
  create-workbook
  open-excel)
```
#####clasew-handler


```
(defn clasew-handler
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
;;;   example          [:get-range-data "Sheet1" "A1:A10"]
;;;   example          ["clasew_get_range_data" "Sheet1" "A1:A10"]
;;;
;;;   example          [:save-quit]
;;;   example          ["clasew_save_and_quit"]
```


```clasew-handler``` will convert the sequence to the form expected by ```clasew-script```, as shown in the following example that builds upon the earlier demonstration:

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


(def sample2 (cs/clasew-script "clasew-ex4-sample2.xlsx"
      cs/create cs/no-open "path to desktop" nil
      (cs/clasew-handler [[:put-range-data "Sheet1" "A1:J10" datum]
                             [:get-range-data "Sheet1" "A1:A10"]
                             [:save-quit]])))

(p sample2)

;; Observe that the input was transposed and for each position item in the
;; "handler_list" vector there are associative parameters in "arg_list" vector matrix.
;; The third "arg_list" vector is empty as there are no arguments to the :save-quit handler

=> {"create_parms" {},
 "fqn_path" "path to desktop",
 "open_ifm" "false",
 "create_ifm" "true",
 "work_book" "clasew-ex4-sample2.xlsx",
 "handler_list"
 ["clasew_excel_put_range_data"
  "clasew_excel_get_range_data"
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
 ([{"create_parms" {},
    "fqn_path" "path to desktop",
    "open_ifm" "false",
    "create_ifm" "true",
    "work_book" "clasew-ex4-sample2.xlsx",
    "handler_list"
    ["clasew_put_range_data"
     "clasew_get_range_data"
     "clasew_save_and_quit"],
    "arg_list"
    [["Sheet1"
      "A1:J10"
      [[58 13 87 58 98 78 84 36 59 55]
       [57 5 69 40 36 12 14 70 78 91]
       [79 55 48 46 66 60 17 17 69 29]
       [95 28 4 80 93 58 41 35 84 57]
       [13 23 72 50 36 52 4 39 45 83]
       [68 48 28 31 78 95 29 61 36 97]
       [77 41 83 35 56 53 38 82 95 80]
       [82 79 25 26 33 53 22 38 84 45]
       [41 77 34 48 99 48 12 14 85 39]
       [9 45 99 68 66 41 9 62 33 31]]]
     ["Sheet1" "A1:A10"]
     []]}]),
 :bind-function "clasew_eval",
 :result
 [[{"create_wkbk" "clasew-ex4-sample2.xlsx success"}
 {"clasew_put_range_data" "success"}
 {"clasew_get_range_data" [[58.0] [57.0] [79.0] [95.0] [13.0] [68.0] [77.0] [82.0] [41.0] [9.0]]}
 {"clasew_quit" "success"}]]}


```

As you can see, the input enables our main excel script to iterate through multiple handler calls while still in context of the AppleScriptEngine and eliminates the overhead of individual calls.

The following table (volatile and subject to change) describes each handler-identifier currently available to call, it's keyword shorthand and required arguments are shown as well:
<table>
<tr><th>handler (string)</th><th> _or_ handler (keyword)</th><th>description</th><th>argument position and sample</th></tr>
<tr><td>clasew_get_range_info</td><td>:get-range-info</td>
  <td>retrieves general information about user defined range in the workbook's worksheet</td><td><ol><li>"Sheet1"<li>"A1:J2"</ol></td></tr>
<tr><td>clasew_get_range_formulas</td><td>:get-range-formulas</td>
  <td>retrieves forumulas from user defined range in the workbook's worksheet</td><td><ol><li>"Sheet1"<li>"A1:J2"</ol></td></tr>
<tr><td>clasew_get_range_data</td><td>:get-range-data</td>
  <td>retrieves values from  user defined range in the workbook's worksheet</td><td>see chain-get-range-data topic below</td></tr>
<tr><td>clasew_put_range_data</td><td>:put-range-data</td>
  <td>sets values in user defined range in the workbook's worksheet</td><td>see chain-put-range-data topic below</td></tr>
<tr><td>clasew_add_sheet</td><td>:add-sheet</td>
  <td>adds worksheet(s) to workbook in positional order</td><td>see chain-add-sheet topic below </td></tr>
<tr><td>clasew_delete_sheet</td><td>:delete-sheet</td>
  <td>deletes worksheet(s) from workbook by name or positional order</td><td>see chain-delete-sheet topic below </td></tr>
<tr><td>clasew_get_all_book_info</td><td>:all-book-info</td>
  <td>retrieves summary information about all open workbooks</td><td>n/a</td></tr>
<tr><td>clasew_get_book_info</td><td>:book-info</td>
  <td>retrieves summary information about the containing script's workbook</td><td>n/a</td></tr>
<tr><td>clasew_save</td><td>:save</td>
  <td>saves the containing script's workbook</td><td>n/a</td></tr>
<tr><td>clasew_save_and_quit</td><td>:save-quit</td>
  <td>saves the containing script's workbook and quits the application</td><td>n/a</td></tr>
<tr><td>clasew_quit</td><td>:quit</td>
  <td>quits the application however; if there are modified workbooks the application may prompt to save</td><td>n/a</td></tr>
<tr><td>clasew_quit_no_save</td><td>:quit-no-save</td>
  <td>quits the application without saving any open and modified workbooks</td><td>n/a</td></tr>
<tr><td>clasew_save_as</td><td>:save-as</td>
  <td>saves the containing script's workbook to new name and path of choice</td><td><ol><li>"clasew-sample-copy.xlsx"<li>"path to desktop"</ol></td></tr>
<tr><td>clasew_run</td><td>:run-script</td>
  <td>executes an arbitrary user provided script</td><td>see clasew_run usage topic below</td></tr>
</table>

#####clasew clasew_run usage
clasew is not trying to realize every possible AppleScript application command. To that end, the intent is to provide the most common constructs in idiomatic clojure way. So in order to take advantage of the main script and the functions that clasew does provide, the ```clasew_run``` option provides the means to execute any additional/arbitrary AppleScript.

At a minimum, when using this handler, the script string passed into clasew must be in the form of:
```clojure
-- Before the script is executed, clasew prepares the two arguments
-- as input to this script
-- caller is the calling script object (i.e. clasew-excel.applescript)
-- args is the argument list passed in

on run(caller, args)
-- The meat of your script goes here
end run
```
Here is the source for the ```clasew_run``` handler:
```clojure

on clasew_run(arguments)
  -- get the user script text from item one in the argument list (scpt)
  -- get the user script arguments from item two in the argument list (scpt_args)

  set scpt to item 1 of arguments
  set scpt_args to item 2 of arguments

  -- pass myself as the first argument to the scpt in case
  -- the script wants to call back into my handlers or access my properties
  -- pass the user provided arguments (scpt_args) as the second
  -- run script and return results

  return run script scpt with parameters {me, scpt_args}

end clasew_run
```

Here is an simple example:
```clojure
;; Demo pushing your own agenda

(def my_script
  "on run(caller, args)
    say \"I have control!\"
    return {my_script: \"No you don't\"}
  end run")

(def sample3 (cs/clasew-script "clasew-sample.xlsx"
      cs/no-create cs/open "path to desktop" nil
      (es/clasew-handler [[:run-script my_script []]
                                [:quit]])))

(p (es/clasew-excel-call! sample3))

=> {:reset-binding true,
 :arguments
 ([{"create_parms" {},
    "fqn_path" "path to desktop",
    "open_ifm" "true",
    "create_ifm" "false",
    "work_book" "clasew-sample.xlsx",
    "handler_list" ["clasew_excel_run" "clasew_excel_quit"],
    "arg_list"
    [["on run(caller, args)\n
        say \"I have control!\"\n
        return {my_script: \"No you don't\"}\n
      end run"
      []]
     []]}]),
 :bind-function "clasew_eval",
 :result
 [[{"open_wkbk" "clasew-sample.xlsx success"}
   {"my_script" "No you don't"}
   {"clasew_quit" "success"}]]}


```
####Raw Materials Conclusion
The previous has provided information on how to use the raw building block functions of the clasew-spreads DSL. For the sample REPL code, feel free to tool around with them in [examples4.clj](../dev/src/clasew/examples4.clj), they have identifiers of sample1, sample2 and sample3 if you want to uncomment the associated ```(p (es/clasew-excel-call! ...))``` calls.

###Higher Order Functions
As noted earlier, we wanted to provide what we expect satisfies the most basic operational paradigm:

+ Create a new workbook, perform a number of opertions, save and exit
+ Open an existing workbook, perform a number of operations, save and exit

For that, the number of clasew-excel HOF (at the time of this writing) rings in at... not many.

#####create-wkbk
As the name implies, the primary purpose if to setup the script with pre-defined flag settings for creating a new workbook environment and executing as many handler chains as needed. This HOF, and ```open-wkbk``` also eliminate the need to call ```clasew-excel-script``` and ```clasew-excel-handler``` directly.

Quick example showing a side by side of using the raw materials and HOF:
```clojure
;;
;;   Raw material setup call shown (tastes great)
;;   Create a workbook, throw some data at it, save and quit
;;

(def sample2 (cs/clasew-script "clasew-sample.xlsx"
      cs/create cs/no-open "path to desktop" nil
      (cs/clasew-excel-handler [[:put-range-values "Sheet1" "A1:J10" datum]
                             [:get-range-values "Sheet1" "A1:A10"]
                             [:save-quit]])))


;;
;;   HOF (less filling)
;;

(def sample5 (cs/create-wkbk "clasew-sample.xlsx" "path to desktop"
                     (cs/chain-put-range-data "Sheet1" datum)
                     [:save-quit]))

```

#####creation parms redux
As noted previously, the ```create-wkbk``` simplifies creation parameters:
<ol>
<li> Supports using keywords instead of strings
<li> Fills in the blanks through map deconstruction and testing for keyword presence
</ol>

**If passing creation parameters, it must be provided *after* the file path and before handlers or chains**
```clojure
;;; Have Numbers use Excel sheet names (no spaces)

(def sample5n (cs/create-wkbk "clasew-sample.numbers" "path to desktop"
                     {:sheet_name "Sheet1"}
                     (cs/chain-put-range-data "Sheet1" datum)
                     [:save-quit]))
```

#####open-wkbk
Like ```create-wkbk``` this HOF pre-defines a "opened workbook" environment.

Side by side:
```clojure
;;   Raw material setup call shown (tastes great)

(def sample3 (cs/clasew-script "clasew-sample.xlsx"
      cs/no-create cs/open "path to desktop" nil
      (cs/clasew-excel-handler [[:all-book-info]
                             [:quit]])))

;;   HOF (less filling)

(def sample6 (cs/open-wkbk "clasew-sample.xlsx" "path to desktop"
                     [:all-book-info] [:quit]))

```
Looking back up the screen at the ```create-wkbk``` side by side, you may have noticed a function not discussed yet: ```chain-put-range-data```. This is one of a number of functions that further simplify the creation of handler chain.

####Chain Functions

#####chain-put-range-data
This function will auto-caculate the range signature (e.g. "A1:J10") from the dimensions of the data matrix passed in as an argument. Opetional column and row offsets can be supplied.
```clojure
(defn chain-put-range-data
  "Sets up a chain for putting a data range into workbook's sheet-name. Output
  also includes the necessary range signature.
  sheet-name - the name of the sheet to put the data in
  data - a vector of vectors with inner vectors containing the data
  start-col offset column for data 0 = A
  start-row offset row for data 0 = 1"

  [sheet-name data & [start-col start-row]]
  (...))

;; Examples
;;
;; (cs/chain-put-range-data "Sheet1" datum)
;; (cs/chain-put-range-data "Sheet1" datum 4 4)
;; (cs/chain-put-range-data "Jan2015" [[Week Sales Returns] [1 20 2] [2 18 4] [3 52 6]])

```
#####chain-get-range-data
This function supports specifying zero based range and offset coordinates.
```clojure
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
;; (cs/chain-get-range-data "Sheet1" 0 0) => A1
;; (cs/chain-get-range-data "Sheet1" 0 0 1 1) => A1:B2
;; (cs/chain-get-range-data "Sheet1" 3 0 2 2) => C1:E3

```
#####chain-get-range-formulas
This function supports specifying zero based range and offset coordinates.
```clojure
(defn chain-get-range-formulas
  "Sets up a chain for getting the formulas in range from workbook's sheet-name
  sheet-name - the name of the sheet to get the formulas from
  start-col offset column for formulas 0 = A
  start-row offset row for formulas 0 = 1
  for-col number of columns to retrieve
  for-row number of rows to retrieve"

  [sheet-name start-col start-row for-col for-row]
  (...))

;; Examples
;;
;; (cs/chain-get-range-formulas "Sheet1" 0 0) => A1
;; (cs/chain-get-range-formulas "Sheet1" 0 0 1 1) => A1:B2
;; (cs/chain-get-range-formulas "Sheet1" 3 0 2 2) => C1:E3

```

#####chain-add-sheet
As noted earlier, ```chain-add-sheet``` supports table creations as well
```clojure
(defn chain-add-sheet
  "Adds new sheets to current workbook
  directives - collection of expressions where each expression is in the form
    new_sheet (string)
    target    (keyword)
    relative_to (keyword | positive number)
    table_list vector of table definitions
  where target is one of:
    :before - insert this new sheet before sheet-name
    :after  - insert this new sheeet after sheet-name
    :at     - insert this sheet at relative_to
  where relative_to is one of:
    :beginning
    :end
    positive number - if greater then worksheet count in book, put first"

  [& directives]
  (...))

; Example
(def sample7 (cs/create-wkbk "clasew-ex4-sample7.xlsx" wrkbk-path
              (cs/chain-add-sheet
              "Before Sheet1"       :before  "Sheet1"
              "After Before Sheet1" :after   "Before Sheet1"
              "The End"             :at      :end
              "The Beginning"       :at      :beginning
              "Also Before Sheet1"  :at      4)
              [:save-quit]))

; Example with table creation as well
(def sample7 (cs/create-wkbk "clasew-ex4-sample7.xlsx" wrkbk-path
              (cs/chain-add-sheet
              "First Add"          :after   "Sheet1"
              "Before First Add"   :before  "First Add"
              "The End"            :at      :end
                (cs/tables
                  (cs/table :table_name "First Table",
                  :row_count 5,
                  :column_count 5))
              "The Beginning"      :at     :beginning
              "Towards last"       :at     5)
              [:save-quit]))


```

###Miscellaneous
A number of functions are available in ```clasew.spreads``` to simplify common needs:

#####clean-result
Used to convert ```:result``` block containing return of calling AppleScript. As noted previously, AppleScript converts it's 'record' type to a map however; the keys are converted to string upon return. ```clean-result``` changes map keys to keywords.

```clojure
(defn clean-result
  "Iterates through the result vectors exchanging keywords for
  strings in any maps"

  [{:keys [result] :as return-map}]

```

Example:

```clojure
(def wrkbk-name "clasew-ex4.xlsx")
(def wrkbk-path "path to desktop")


(def sample5 (cs/create-wkbk wrkbk-name wrkbk-path
                     (cs/chain-put-range-data "Sheet1" datum)
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
    ["clasew_put_range_values" "clasew_save_and_quit"],
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
 :bind-function "clasew_eval",
 :result
 [[{"create_wkbk" "clasew-ex4.xlsx success"}
 {"clasew_put_range_values" "success"} {
 "clasew_quit" "success"}]]}

;; Clean and display

(p (cs/clean-result s5r))

= > {:reset-binding true,
 :arguments
 ([{"fqn_path" "path to desktop",
    "open_ifm" "false",
    "create_ifm" "true",
    "work_book" "clasew-ex4.xlsx",
    "handler_list"
    ["clasew_put_range_values" "clasew_save_and_quit"],
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
 :bind-function "clasew_eval",
 :result
 [[{:create_wkbk "clasew-ex4.xlsx success"}
   {:clasew_put_range_values "success"}
   {:clasew_quit "success"}]]}
```
####Other useful forms

```clojure
(ns
  ^{:author "Frank V. Castellucci"
      :doc "clasew example 4 -  Excel Example"}
  clasew.examples4
  (:require [clasew.spreads :as cs]
            [clasew.excel :as es]
            [clojure.pprint :refer :all])
  )

; Uniform collection
(def uniform [[1 2 3] [4 5 6] [7 8 9]])

; Jagged collection
(def jagged [[1] [2 2] [3 3 3]])

;; format-a1
(cs/format-a1 0 0)  => "A1"
(cs/format-a1 7 14) => "H15"

;; format-range-a1
(cs/format-range-a1 [0 0 0 9]) => "A1:A10"
(cs/format-range-a1 [0 1 1 9]) => "A2:B10"

;; formula-wrap
(cs/formula-wrap "SUM" cs/format-range-a1 [0 0 0 9]) => "=SUM(A1:A10)"
(cs/formula-wrap "AVG" cs/format-range-a1 [0 0 0 9]) => "=AVERAGE("A1:A10")"

;; row/column-ranges and cure for the jagged
(cs/row-ranges uniform)               => ((0 0 2 0) (0 1 2 1) (0 2 2 2))
(cs/row-ranges jagged)                => ((0 0 0 0) (0 1 1 1) (0 2 2 2)) ;INCORRECT
(cs/row-ranges (cs/pad-rows jagged))  => ((0 0 2 0) (0 1 2 1) (0 2 2 2)) ;Better

(cs/column-ranges uniform)               => ((0 0 0 2) (1 0 1 2) (2 0 2 2))
(cs/column-ranges jagged)                => ((0 0 0 2)) ;INCORRECT
(cs/column-ranges (cs/pad-rows jagged))  => ((0 0 0 2) (1 0 1 2) (2 0 2 2)) ;Better

;; sum-by-row/col
(cs/sum-by-row uniform) => ["=SUM(A1:C1)" "=SUM(A2:C2)" "=SUM(A3:C3)"]
(cs/sum-by-col uniform) => ["=SUM(A1:A3)" "=SUM(B1:B3)" "=SUM(C1:C3)"]
(cs/avg-by-row uniform) => ["=AVERAGE(A1:C1)" "=AVERAGE(A2:C2)" "=AVERAGE(A3:C3)"]

;; extend-rows/cols
(cs/extend-rows uniform 0 0 cs/sum-by-row)
=> [[1 2 3 "=SUM(A1:C1)"] [4 5 6 "=SUM(A2:C2)"] [7 8 9 "=SUM(A3:C3)"]]

(cs/extend-columns uniform 0 0 cs/avg-by-col)
=> [[1 2 3]
 [4 5 6]
 [7 8 9]
 ["=AVERAGE(A1:A3)" "=AVERAGE(B1:B3)" "=AVERAGE(C1:C3)"]]

;; The Curse of Lono
(def sample5a (cs/create-wkbk "clasew-ex4-sample5a.xlsx" wrkbk-path
      (cs/chain-put-range-data "Sheet1"
                               (cs/extend-columns
                                (cs/extend-rows
                                 datum 0 0 cs/sum-by-row cs/avg-by-row)
                                0 0 es/avg-by-col))
                               [:save-quit]))

=> {"fqn_path" "path to desktop",
 "open_ifm" "false",
 "create_ifm" "true",
 "work_book" "clasew-ex4-sample5a.xlsx",
 "handler_list"
 ["clasew_put_range_data" "clasew_save_and_quit"],
 "arg_list"
 [["Sheet1"
   "A1:L11"
   [[74 78 81 47 61 6 0 57 63 29 "=SUM(A1:J1)" "=AVERAGE(A1:J1)"]
    [4 39 10 99 97 97 6 59 98 83 "=SUM(A2:J2)" "=AVERAGE(A2:J2)"]
    [75 39 45 2 80 90 20 95 17 50 "=SUM(A3:J3)" "=AVERAGE(A3:J3)"]
    [56 62 53 39 5 64 67 61 40 70 "=SUM(A4:J4)" "=AVERAGE(A4:J4)"]
    [2 84 12 99 81 35 29 83 24 7 "=SUM(A5:J5)" "=AVERAGE(A5:J5)"]
    [83 38 58 35 34 26 49 52 53 89 "=SUM(A6:J6)" "=AVERAGE(A6:J6)"]
    [71 31 44 40 73 7 73 68 32 22 "=SUM(A7:J7)" "=AVERAGE(A7:J7)"]
    [0 98 92 3 42 10 84 30 87 53 "=SUM(A8:J8)" "=AVERAGE(A8:J8)"]
    [8 77 56 92 90 67 23 87 6 56 "=SUM(A9:J9)" "=AVERAGE(A9:J9)"]
    [15 96 69 14 54 80 56 52 58 85 "=SUM(A10:J10)" "=AVERAGE(A10:J10)"]
    ["=AVERAGE(A1:A10)"
     "=AVERAGE(B1:B10)"
     "=AVERAGE(C1:C10)"
     "=AVERAGE(D1:D10)"
     "=AVERAGE(E1:E10)"
     "=AVERAGE(F1:F10)"
     "=AVERAGE(G1:G10)"
     "=AVERAGE(H1:H10)"
     "=AVERAGE(I1:I10)"
     "=AVERAGE(J1:J10)"
     "=AVERAGE(K1:K10)"
     "=AVERAGE(L1:L10)"]]]
  []]}

```
