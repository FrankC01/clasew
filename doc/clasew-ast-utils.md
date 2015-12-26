# Introduction to clasew.ast-utils

***clasew*** is based on a core set of primitives for calling AppleScript vis-a-vis the Java ScriptManager interop. The DSLs (delivered and planned) leverage these primitives to simplify the developer interface to targeted applications.

***clasew.ast-utils*** is a new namespace to centralize common functions that can be used from other HOF namespaces. As of this release, the bulk of function in the clasew.ast-utils namespace are focused on providing filtering but also include the predefined `quit` script.

## clasew.ast-utils
As noted in the introduction, the main functions in this namespace are HOFs that provide a DSL that constructs application context specific filters.

### Namespaces Supported
The new filter forms are supported for the following DSL:
1. `clasew.identities`
2. `clasew.messages`

###Raw Materials

#### Filter BNF

The following is a simple BNF describing the filtering
```
; Low level
block             ::= ({filter-expresssion} | {extension})
extension         ::= (and-block | or-block)
filter-expression ::= application context keyword predicate value
predicate         ::= EQ     ; equal to
                    | !EQ    ; not equal to
                    | LT     ; less than
                    | !LT    ; not-less-than
                    | GT     ; greater-than
                    | !GT    ; not-greater-than
                    | CT     ; contains
                    | !CT    ; not-contains
                    | SW     ; starts-with
                    | EW     ; ends-with
                    | II     ; is-in
                    | !II    ; is-not-in

value             ::= string | number

; Functions
filter-block      ::= clasew.ast-utils/filter block
and-block         ::= clasew.ast-utils/and block
or-block          ::= clasew.ast-utils/or block
```

#####clasew.ast-utils/filter
There are a number of variants for filtering as demonstrated in the BNF above however; some restrictions exist for specific target applications. See the appropriate application DSL documentation for those restrictions.

Simple, contrived, example follows. Practical examples can also be found in the associated `exampleN`namespaces.

```clojure
(ns
  ^{:author "Frank V. Castellucci"
      :doc "Contrived Filters"}
  clasew.contrived
  (:require [clasew.ast-utils :as astu]))

(astu/filter :first_name ident/EQ "Oxnard"}

; Restricts individuals to those whose first name is Oxnard AND last name is Gimbel
; This example uses attributes in context of the `clasew.individuals` DSL

(astu/filter
  :first_name astu/CT "Oxnard"
  :last_name astu/EQ "Gimbel"}

; More complex -or- filter

(def filter-sample
      (astu/filter :first_name astu/CT "Oxnard"
                    (astu/or :first_name astu/EQ "Sally"
                              :last_name astu/EQ "Abercrombe")))
```



#####clasew.ast-utils/quit
