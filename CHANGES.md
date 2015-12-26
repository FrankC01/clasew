# clasew Change Log

clasew: A Clojure AppleScriptEngine Wrapper

## 0.3.0
This release includes a medium impact breaking change to clasew:
1. Filters have been moved to a utility namespace to share between
clasew.individuals and clasew.messages (NEW).

### Enhancements
This release includes for ***Microsoft Outlook*** and ***Apple Mail***
applications email capabilities.

+ New Functions
<table>
<tr><th>Function</th><th>Description</th></tr>
<tr><td>accounts</td><td>Fetch accounts. Filters supported.</td></tr>
<tr><td>mailboxes</td><td>Fetch mailboxes of accounts. Filters supported.</td></tr>
<tr><td>messages</td><td>Fetch messages of mailboxes of accounts. Filters supported.</td></tr>
<tr><td>send-message</td><td>Send email. Minor filter support.</td></tr>
</table>

## 0.2.1
This release includes a minor breaking change to clasew:
1. Filters have been enhanced to support more complex expressions in this release and requie use of the ```clasew.identities/filter``` function instead of just a map.

### Enhancements
With this release we have *extended support* for ***Microsoft Outlook*** and ***Apple Contacts*** applications.


+ New Functions
<table>
<tr><th>Function</th><th>Description</th></tr>
<tr><td>add-individuals</td><td>Ability to add one or more new individuals</td></tr>
<tr><td>delete-individual</td><td>Ability to delete one or more existing individuals. Supports filters.</td></tr>
<tr><td>update-individuals</td><td>Filtered updates to individuals</td></tr>
</table>

### Bug Fixes

<table>
<tr><th>GitHub issue #</th><th>Description</th></tr>
<tr><td>02</td><td>Namespace incorrect</td></tr>
</table>

## 0.2.0
This release includes a few minor breaking changes to clasew.
### Enhancements
With this release we have added support for an identities DSL backed by ***Microsoft Outlook*** and ***Apple Contacts*** applications. Right now the functions are fairly simple as this release also introduces an ***experimental*** AST builder and AppleScript emitter. Some refactoring has occured, primarily between ```spreads.clj``` and a new namespace ```utility.clj```.

+ Namespace Additions
<table>
<tr><th>Namespace</th><th>Description</th></tr>
<tr><td>utility</td><td>Common functions applicable across clasew DSL</td></tr>
<tr><td>identities</td><td>generic contact functions</td></tr>
<tr><td>outlook</td><td>MS Outlook support</td></tr>
<tr><td>contacts</td><td>Apple Contacts support</td></tr>
</table>

+ Functions moved from ```spreads``` to ```utility```
<table>
<tr><th>Function</th><th>Description</th></tr>
<tr><td>modify-keys</td><td>changes map keys (e.g. string to keywords)</td></tr>
<tr><td>clean-result</td><td>converts from java collections to clojure, modify-keys, etc.</td></tr>
<tr><td>handler-acc</td><td>reducing function for handlers and arguments</td></tr>
</table>

### Bug Fixes

## 0.1.10

This release includes breaking changes to clasew.

### Enhancements
With this release we have added support for Apple's Numbers spreadsheet application. In doing so we re-factored the common functions that are applicable to both Excel or Numbers into a new namespace ```spreads```.


+ Namespace Additions
<table>
<tr><th>Namespace</th><th>Description</th></tr>
<tr><td>spreads</td><td>Common functions applicable to spreadsheet based scripting</td></tr>
<tr><td>numbers</td><td>Function specific to Apple's Numbers</td></tr>
</table>

+ Function Additions
<table>
<tr><th>Function</th><th>Namespaces</th><th>Description (see documentation)</th></tr>
<tr><td>clasew-numbers-call!</td><td>numbers</td><td>main script invocation</td></tr>
<tr><td>format-data-range</td><td>spreads</td><td>produes A1 range address from offsets and data definition</td></tr>
<tr><td>create-parms</td><td>spreads</td><td>create workbook optional parameters</td></tr>
<tr><td>tables</td><td>spreads</td><td>reduces table definitions (applicable to creating workbooks and/or adding sheets)</td></tr>
<tr><td>table</td><td>spreads</td><td>table definitions</td></tr>
</table>

+ Modifications - Keywords and Form names
<table>
<tr><th>Prior</th><th>Current</th><th>ns</th><th>notes</th></tr>
<tr><td>clean-excel-result</td><td>clean-result</td><td>spreads</td><td></td></tr>
<tr><td>get-excel-a1</td><td>format-a1</td><td>spreads</td><td>Removed 'pre' check for row and column boundaries</td></tr>
<tr><td>get-excel-range-a1</td><td>format-range-a1</td><td>spreads</td><td></td></tr>
<tr><td>get-data-dimensions</td><td>data-dimensions</td><td>spreads</td><td></td></tr>
<tr><td>clasew-excel-script</td><td>clasew-script</td><td>spreads</td><td>new parameter (arity), see ```clasew-spreads``` documentation</td></tr>
<tr><td>clasew-excel-handler</td><td>clasew-handler</td><td>spreads</td><td></td></tr>
<tr><td>chain-add-sheet</td><td>chain-add-sheet</td><td>spreads</td><td>takes a table definition as last optional argument</td></tr>
</table>


**Note:**All other functions that were formally in ```clasew.excel``` have been moved to ```clasew.spreads``` with exception of ```clasew-excel-call!```.

### Bug Fixes

## 0.1.9

### Enhancements
+ all handler result maps are now in form ```{handler_name result}``` Refer to [clasew.excel DSL doc](doc/clasew-excel.md) for details

+ Additions - Handlers, Chain Functions
<table>
<tr><th>handler (keyword)</th><th>chain function</th><tr>
<tr><td>:quit-no-save</td><td>n/a</td></tr>
<tr><td>:add-sheet</td><td>chain-add-sheet</td></tr>
<tr><td>:delete-sheet</td><td>chain-delete-sheet</td></tr>
<table>

+ Additions - Miscellaneous forms
<table>
<tr><th>form</th><th>description</th><tr>
<tr><td>get-excel-range-a1</td><td>create Excel "A1" type range</td></tr>
<tr><td>get-data-dimensions</td><td>returns zero based column row dims</td></tr>
<tr><td>pad-rows</td><td>pads uneven rows in data block</td></tr>
<tr><td>formula-wrap</td><td>string wrapper for production of Excel formula</td></tr>
<tr><td>fsum</td><td>thin wrapper for formula-wrap: produces "=SUM(xx)"</td></tr>
<tr><td>favg</td><td>thin wrapper for formula-wrap: produces "=AVERAGE(xx)"</td></tr>
<tr><td>row-ranges</td><td>Produces sequence of row ranges calculated from input data</td></tr>
<tr><td>column-ranges</td><td>Produces sequence of row column calculated from input data</td></tr>
<tr><td>sum-by-row</td><td>Produces sequence of row sum formulas from input data</td></tr>
<tr><td>sum-by-col</td><td>Produces sequence of column sum formulas from input data</td></tr>
<tr><td>avg-by-row</td><td>Produces sequence of row average formulas from input data</td></tr>
<tr><td>avg-by-col</td><td>Produces sequence of column average formulas from input data</td></tr>
<tr><td>extend-rows</td><td>Returns the collection with rows extended to include results of applying one or more functions to the input collection</td></tr>
<tr><td>avg-by-col</td><td>Returns the collection with columns extended to include results of applying one or more functions to the input collection</td></tr>
<table>

+ Modifications
<table>
<tr><th>handler (keyword)</th><th>modified/removed</th><th>changed to</th><tr>
<tr><td>:get-used-range-info</td><td>removed</td><td>see :get-range-info</tr>
<tr><td>:get-range-info</td><td>modified</td><td>second argument can specify range or "used"</td></tr>
<tr><td>:get-range-vallues</td><td>modified</td><td>renamed to :get-range-data</td></tr>
<tr><td>:put-range-vallues</td><td>modified</td><td>renamed to :put-range-data</td></tr>
<table>


### Bug Fixes

## 0.1.8

### Enhancements
+ Announced

### Bug Fixes

## 0.1.8-SNAPSHOT

### Enhancements
+ Additions made to example4.clj, found [here](dev/src/clasew/examples4.clj)

### Bug Fixes
+ Typos in user markdowns
+ Fixed inconsistent terms, keywords, etc.

## 0.1.7-SNAPSHOT

### Enhancements
+ Big updates clasew.excel [documentation](doc/clasew-excel.md)
+ example4.clj, found [here](dev/src/clasew/examples4.clj), aligned to documentation

### Bug Fixes
+ Typos in user markdowns
+ Fixed inconsistent terms, keywords, etc.

## 0.1.6-SNAPSHOT

### Enhancements
+ Updated clasew.excel [documentation](doc/clasew-excel.md)

### Bug Fixes

## 0.1.5-SNAPSHOT

### Enhancements

#### clasew.excel DSL
This snapshot introduces a clasew DSL for Excel. More information can be found [here](doc/clasew-excel.md)

#### Examples

+ examples3.clj extended to include passing scripts to as/run-ascript via slurp
  and clojure.java.io.
+ examples4.clj major rework to demonstrate using the **clasew.excel DSL**.

### Bug Fixes


## 0.1.4-SNAPSHOT

### Enhancements

+ Added examples4.clj to demonstrate using excel as data store

### Bug Fixes
