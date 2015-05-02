# clasew Change Log

clasew: A Clojure AppleScriptEngine Wrapper

## 0.1.9

### Enhancements
+ The following Excel handlers have been added. Refer to [clasew.excel DSL doc](doc/clasew-excel.md) for details

<table>
<tr><th>handler (keyword)</th><th>chain function</th><tr>
<tr><td>:quit-no-save</td><td>n/a</td></tr>
<tr><td>:add-sheet</td><td>chain-add-sheet</td></tr>
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
