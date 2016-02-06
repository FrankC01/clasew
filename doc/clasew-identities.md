# Introduction to clasew.identities DSL

***clasew*** is based on a core set of primitives for calling AppleScript vis-a-vis the Java ScriptManager interop. The DSLs (delivered and planned) leverage these primitives to simplify the developer interface to targeted applications.

***clasew.identities*** is a DSL that builds on ***clasew.core***. Go [here](intro.md) for more informaiton on clasew.core. The primary functionality that `clasew.identities` brings to the table is to abstract 99% of a generic identity managemenet DSL that is then finalized by  `clasew.outlook` or `clasew.contacts` DSLs.

The focus of this page are the higher order functions (HOF), found in the clasew.identities namespace, that can be utilized to simplify interacting with either Microsoft Outlook or Apple's Contacts via AppleScript.

## clasew.identities
As noted in the introduction, the main functions in this namespace are HOFs that provide a DSL that, under the covers, call the clasew.core primitive functions.

### Namespace Initialization
When clasew.identities is loaded:
+ Establishes a local `(defonce ...)` engine for it's use

Unlike `clasew.core`, `clasew.identities` ***does not*** require providing an engine argument as it has a dedicated instance.

###Raw Materials

There are a number of identity forms that simplify the preperation of calling AppleScript for either the Outlook or Contacts applications. The primary focus of these forms is on getting ready the script payload before calling out to the respective technollogy DSL.

#####clasew.identities/run-script!

This form is the core function that invokes AppleScript to execute one or more scripts. It takes as ***input*** the ***output*** from either `clasew.outlook/script or clasew.contacts/script`

```clojure
(defn run-script!
  "Invokes the AppleScript interpreter passing one or more scripts
  to be evaluated"
  [& scripts]
  (...)

```

####Filtering
The filtering functions have been moved to `clasew.ast-utils` and are [documented here](clasew-ast-utils.md).
Filtering is supported on the following functions: ```identities/individuals```, ```identities/add-indivudals```, ```identities/delete-individuals``` and ```identities/update-individuals```.

There are some restrictions on using filters at the function usage level as well as the application level (specifically Outlook).

#####Filter function restrictions
The following table identifies where filters may be provided to modify a functions operation:

<table>
<tr><th>Function</th><th>Sub Function</th><th>Filter Supported</th></tr>
<tr><td>clasew.identities/individuals</td><td>na</td><td>Y</td></tr>
<tr><td></td><td>clasew.identities/phones</td><td>N</td></tr>
<tr><td></td><td>clasew.identities/addresses</td><td>N</td></tr>
<tr><td></td><td>clasew.identities/email-addresses</td><td>N</td></tr>
<tr><td>clasew.identities/delete-individual</td><td>na</td><td>Y</td></tr>
<tr><td>clasew.identities/update-individual</td><td>na</td><td>Y</td></tr>
<tr><td></td><td>clasew.identities/update-phones</td><td>Y</td></tr>
<tr><td></td><td>clasew.identities/update-addresses</td><td>Y</td></tr>
<tr><td></td><td>clasew.identities/update-email-addresses</td><td>Y</td></tr>
<tr><td>clasew.identities/add-individual</td><td>na</td><td>N</td></tr>
<tr><td>clasew.identities/all-individuals</td><td>na</td><td>N</td></tr>
</table>

#####Filter application restrictions
The following table identifies restrictions based on the application limitations:

<table>
<tr><th>Application</th><th>Context</th><th>Restriction</th></tr>
<tr><td>All</td><td>`clasew.identities/individuals`</td><td>filter-expressions can only apply to individual properties and not addresses, email-addresses or phones</td></tr>
<tr><td>All</td><td>`clasew.identities/delete-individuals`</td><td>filter-expressions can only apply to individual properties and not addresses, email-addresses or phones</td></tr>
<tr><td>All</td><td>`clasew.identities/update-individuals`</td><td>filter-expressions can only apply to individual properties and not addresses, email-addresses or phones</td></tr>
<tr><td>All</td><td>`clasew.identities/update-addresses`</td><td>filter-expressions can only apply to address properties and not individuals, email-addresses or phones</td></tr>
<tr><td>All</td><td>`clasew.identities/update-email-addresses`</td><td>filter-expressions can only apply to email-address properties and not addresses, individuals or phones</td></tr>
<tr><td>All</td><td>`clasew.identities/update-phones`</td><td>filter-expressions can only apply to phone properties and not addresses, email-addresses or individuals</td></tr>
<tr><td>Outlook</td><td>`clasew.identities/update-phones` `clasew.identities/update-addresses`</td><td>Must include a filter-expressions for respective property type</td></tr>
</table>

####clasew.identities/individuals
This form is used as ***input*** to the respective `clasew.APPLICATION/script` functions invoked for the target application to fetch all or partial information for individuals.

```clojure
(defn individuals
  "Prepares the script for retrieving attributes of individuals from the identity source
  along with any additional sub-attributes. Also supports individual filtering."
  [& args]
  (...)
```
The following is a breakdown of the arguments and variations
<table>
<tr><th>Argument</th><th>Description</th></tr>
<tr><td>keywords</td><td>(optional) Keywords identifying attributes of the individual you want to return with the results. If omitted, attributes returned are defined by `clasew.identities/identity-standard`.</tr>
<tr><td>vectors</td><td>(optional) Vectors as produced from `clasew.identities/addresses, clasew.identities/email-addresses and clasew.identities/phones`</td></tr>
<tr><td>filters</td><td>(optional) Contains filter designation to limit scope of individuals whose information is being fetched. See the Filtering description and restrictions above.</td></tr>
</table>

Here is a REPL ready examples. **Note that the output from the following are not runnable scripts but purely for demonstrating form results**:
```clojure
(ns clasew.sandbox
  (:require [clasew.identities :as ident]
            [clasew.ast-utils :as astu]
            [clojure.pprint :refer :all])
  )

;;; Setup for the example

(def p pprint)

;;;
;;; Demonstrate most basic call
;;;

(p (ident/individuals))

=> {:individuals
 #{:primary_department :first_name :name_suffix :middle_name
   :primary_company :primary_title :last_name :full_name},
 :filters nil,
 :emails nil,
 :addresses nil,
 :phones nil}

;;;
;;; Variants

; All individuals with their addresses

(p (ident/individuals
    (ident/addresses)))

; All individuals full name only along with phones

(p (ident/individuals
    :full_name
    (ident/phones)))

; Full name only and email addresses for individuals with first name of "Oxnard"

(p (ident/individuals
    (astu/filter :first_name ident/EQ "Oxnard)
    :full_name
    (ident/email-addresses)))

;;; Alternatley, a variation of individuals is to collect all
;;; information including addresses, email-addresses and phones

(p (ident/individuals-all))

```
####clasew.identities/add-individuals
This form is used as ***input*** to the respective `clasew.APPLICATION/script` functions invoked for the target application to add individuals to the target application.

```clojure
(defn add-individuals
  "Returns script directives for adding  one or more individuals"
  [add1 & adds]
  (...)
```

####clasew.identities/update-individuals
This form is used as ***input*** to the respective ```clasew.APPLICATION/script``` functions invoked for the target application to update individuals and/or subproperties in the target application.
```clojure
(defn update-individual
  "Returns script directives for updates to one or more bits of information of an individual"
  [& update-blocks]
  (...)
```

The following is a simple BNF describing `update-blocks` in the update-individuals arguments
```
; Low level
adds              ::= adds {clojure map}
setter            ::= keyword value
filter-set        ::= filter-block setter {setter}
update-block      ::= filter-set {(update-addr | update-phn | update-emls)}

; Functions
update-indy       ::= clasew.identities/update-individuals update-block
update-addr       ::= clasew.identities/update-addresses filter-set adds
update-phn        ::= clasew.identities/update-phones filter-set adds
update-emls       ::= clasew.identities/update-email-addresses filter-set adds

```


####clasew.identities/delete-individual
This form is used as ***input*** to the respective ```clasew.APPLICATION/script``` functions invoked for the target application to delete individuals in the target application.

```clojure
(defn delete-individual
  "Returns script directives for deleting individuals matching feature"
  [filt]
  (...)
```

The following is a breakdown of the arguments and variations
<table>
<tr><th>Argument</th><th>Description</th></tr>
<tr><td>filter</td><td>(required) Contains filter designation to limit individuals being deleted.</td></tr>
</table>


#####clasew.ast-utils/quit
Support script that is used to close the target application after functional scripts are completed.

```clojure
(defn quit
  "Script to quit an application
  appkw - keyword [:outlook | :mail | :contacts]
  Identifies the application to shut down"
  [appkw]
  (...)

```
####clasew.APPLICATION/script
(where Application is either the clasew.contacts or clasew.outlook name spaces`


```clojure

;;; Produce script output ready for input into clasew.identities/run-script!

(p (clasew.outlook/script
    (ident/individuals)))

(p (clasew.contacts/script
    (ident/individuals)))

;;; Take the output from the script generation and run it

(p (ident/run-script!
    (clasew.outlook/script
      (ident/individuals))))

;;; Take the output from the script generation and run it, then quit application

(p (ident/run-script!
    (clasew.outlook/script
     (ident/individuals))
    (astu/quit :outlook)))


```


