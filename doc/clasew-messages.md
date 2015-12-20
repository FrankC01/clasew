# Introduction to clasew.messages DSL

***clasew*** is based on a core set of primitives for calling AppleScript vis-a-vis the Java ScriptManager interop. The DSLs (delivered and planned) leverage these primitives to simplify the developer interface to targeted applications.

***clasew.messages*** is a DSL that builds on ***clasew.core***. Go [here](intro.md) for more informaiton on clasew.core. The primary functionality that `clasew.messages` brings to the table is to abstract 99% of a generic email fetch and send managemenet DSL that is then finalized by  `clasew.outlook` or `clasew.mail` DSLs.

The focus of this page are the higher order functions (HOF), found in the `clasew.messages` namespace, that can be utilized to simplify interacting with either Microsoft Outlook or Apple's Mail applications via AppleScript.

## clasew.messages
As noted in the introduction, the main functions in this namespace are HOFs that provide a DSL that, under the covers, call the clasew.core primitive functions.

### Namespace Initialization
When clasew.messages is loaded:
+ Establishes a local ```(defonce ...)``` engine for it's use

Unlike `clasew.core`, `clasew.messages` ***does not*** require providing an engine argument as it has a dedicated instance.

###Raw Materials

There are a number of message related forms that simplify the preperation of calling AppleScript for either the Outlook or Mail applications. The primary focus of these forms is on getting ready the script payload before calling out to the respective technollogy DSL.

#####clasew.identities/run-script!

This form is the core function that invokes AppleScript to execute one or more scripts. It takes as ***input*** the ***output*** from either `clasew.outlook/script or clasew.mail/script` functions.

```clojure
(defn run-script!
  "Invokes the AppleScript interpreter passing one or more scripts
  to be evaluated"
  [& scripts]
  (...)

```


####Filtering
The filtering functions have been moved to `clasew.ast-utils` and are [documented here](clasew-filters.md).

There are some restrictions on using filters at the function usage level as well as the application level (specifically Outlook).

#####Filter function applicability
The following table identifies where filters may be provided to modify a functions operation:

<table>
<tr><th>Function</th><th>Sub Function</th><th>Filter Supported</th></tr>
<tr><td>clasew.messages/accounts</td><td>na</td><td>Y</td></tr>
<tr><td>clasew.messages/mailboxes</td><td>na</td><td>Y</td></tr>
<tr><td>clasew.messages/messages</td><td>na</td><td>Y</td></tr>
<tr><td>clasew.messages/send-message</td><td>na</td><td>N</td></tr>
</table>

#####Filter applied function restrictions
The following table identifies additional restrictions on using filters on various functions:

<table>
<tr><th>Application</th><th>Context</th><th>Restriction</th></tr>
<tr><td>All</td><td>`clasew.identities/accounts`</td><td>filter-expressions can only apply to account properties and not mailboxes or messages</td></tr>
<tr><td>All</td><td>`clasew.messages/mailboxes`</td><td>filter-expressions can only apply to mailbox properties and not accounts or messages</td></tr>
<tr><td>All</td><td>`clasew..messages/messages`</td><td>filter-expressions can only apply to message properties and not accounts or mailboxes</td></tr>
</table>


#####clasew.messages/accounts
This form is used as ***input*** to the respective ```clasew.APPLICATION/script``` functions invoked for the target application to fetch all or partial information for individuals.
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
<tr><td>keywords</td><td>(optional) Keywords identifying attributes of the individual you want to return with the results. If omitted, attributes returned are defined by ```clasew.identities/identity-standard```.</tr>
<tr><td>vectors</td><td>(optional) Vectors as produced from ```clasew.identities/addresses, clasew.identities/email-addresses and clasew.identities/phones```</td></tr>
<tr><td>filters</td><td>(optional) Contains filter designation to limit scope of individuals whose information is being fetched. See ```identities/filter``` description and limitations above.</td></tr>
</table>

Here is a REPL ready examples. **Note that the output from the following are not runnable scripts but purely for demonstrating form results**:
```clojure
(ns clasew.sandbox
  (:require [clasew.identities :as ident]
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
    (ident/filter :first_name ident/EQ "Oxnard)
    :full_name
    (ident/email-addresses)))

;;;
;;; Alternatley, a variation of individuals is to collect all
;;; information including addresses, email-addresses and phones
;;;

(p (ident/individuals-all))

=> {:individuals
 #{:primary_department :first_name :name_suffix :middle_name
   :primary_company :primary_title :last_name :full_name},
 :filters nil,
 :emails [:emails :email_type :email_address],
 :addresses
 [:addresses
  :zip_code
  :street_name
  :address_type
  :city_name
  :country_name
  :state_name],
 :phones [:phones :number_type :number_value]}
```
#####clasew.identities/add-individuals
This form is used as ***input*** to the respective ```clasew.APPLICATION/script``` functions invoked for the target application to add individuals to the target application.

```clojure
(defn add-individuals
  "Returns script directives for adding  one or more individuals"
  [add1 & adds]
  (...)
```

#####clasew.identities/update-individuals
This form is used as ***input*** to the respective ```clasew.APPLICATION/script``` functions invoked for the target application to update individuals and/or subproperties in the target application.
```clojure
(defn update-individual
  "Returns script directives for updates to one or more bits of information of an individual"
  [& update-blocks]
  (...)
```

The following is a simple BNF describing ```update-blocks``` in the update-individuals arguments
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


#####clasew.identities/delete-individual
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
<tr><td>filter</td><td>(required) Contains filter designation to limit individuals being deleted. See ```identities/filter``` description and limitations above.</td></tr>
</table>


#####clasew.APPLICATION/script
(where Application is either the clasew.mail or clasew.outlook name spaces


```clojure

;;; Produce script output ready for input into clasew.messages/run-script!

(p (clasew.outlook/script
    (mesg/accounts)))

(p (clasew.mail/script
    (mesg/accounts)))

;;; Take the output from the script generation and run it

(p (ident/run-script!
    (clasew.outlook/script
      (mesg/accounts))))

;;; Take the output from the script generation and run it, then quit application

(p (ident/run-script!
    (clasew.outlook/script
     (mesg/accounts))
    (astu/quit :outlook)))


```


