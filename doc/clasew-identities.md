# Introduction to clasew.identities DSL

***clasew*** is based on a core set of primitives for calling AppleScript vis-a-vis the Java ScriptManager interop. The DSLs (delivered and planned) leverage these primitives to simplify the developer interface to targeted applications. However; you can use them for whatever type AppleScript you want evaluated.

***clasew.identities*** is a DSL that builds on ***clasew.core***. Go [here](intro.md) for more informaiton on clasew.core. The primary functionality that ```clasew.identities``` brings to the table is to abstract 99% of a generic identity managemenet DSL that is then finalized by  ```clasew.outlook``` or ```clasew.contacts``` DSLs.

The focus of this page are the higher order functions (HOF), found in the clasew.identities namespace, that can be utilized to simplify interacting with either Microsoft Outlook or Apple's Contacts via AppleScript.

## clasew.identities
As noted in the introduction, the main functions in this namespace are HOFs that provide a DSL that, under the covers, call the clasew.core primitive functions.

### Namespace Initialization
When clasew.identities is loaded:
+ Establishes a local ```(defonce ...)``` engine for it's use

Unlike ```clasew.core```, ```clasew.identities``` ***does not*** require providing an engine argument as it has a dedicated instance.


###Raw Materials

There are a number of identity forms that simplify the preperation of calling AppleScript for either the Outlook or Contacts applications. The primary focus of these forms is on getting ready the script payload before calling out to the respective technollogy DSL.

#####clasew.identities/run-script!

This form is the core function that invokes AppleScript to execute one or more scripts. It takes as ***input*** the ***output*** from either ```clasew.outlook/script or clasew.contacts/script```

```clojure
(defn run-script!
  "Invokes the AppleScript interpreter passing one or more scripts
  to be evaluated"
  [& scripts]
  (...)

```

#####clasew.identities/individuals
This form is used as ***input*** to the respective ```script``` functions invoked for the target application.
```clojure
(defn individuals
  "Prepares the script for retrieving attributes of individuals from the identity source
  along with any additional sub-attributes. Also supports minor filtering."
  [& args]
  (...)
```
The following is a breakdown of the arguments and variations
<table>
<tr><th>Argument</th><th>Description</th></tr>
<tr><td>keywords</td><td>(optional) Keywords identifying attributes of the individual you want to return with the results. If omitted, attributes returned are defined by ```clasew.identities/identity-standard```.</tr>
<tr><td>vectors</td><td>(optional) Vectors as produced from ```clasew.identities/addresses, clasew.identities/email-addresses and clasew.identities/phones```</td></tr>
<tr><td>map</td><td>(optional) Contains filter designation to limit information returned from the AppleScript call. Right now the filter request logically ***ands*** together the filter criteria. See the Filter Notes below.</td></tr>
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
;;; Variates

(p (ident/individuals
    (ident/addresses)))      ; All individuals with their addresses

(p (ident/individuals
    :full_name
    (ident/phones)))         ; All individuals full name only along with phones

(p (ident/individuals
    :full_name
    (ident/email-addresses)
    {:first_name "Frank"}))  ; All individuals full name only along with email info

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

######Filter Notes
Filtering the individuals returned from the script requires setting up a map where the ***key*** is an attribute of individuals and the value is the string that satisfies the criteria. For example:

```clojure
{:first_name "Frank"} ; Restricts fetched values to those whose first name is Frank

{:first_name "Frank"
 :last_name "Castellucci"} ; Restricts to those whose first name is Frank AND last name is Castellucci
```

#####clasew.identities/quit
Support script that is used to close the target application after functional scripts are completed.

```clojure
(defn quit
  "Script to quit an application
  appkw - keyword (:outlook or :contacts) identies the application
  to shut down"
  [appkw]
  (...)

```
#####clasew.APPLICATION/script
(where Application is either the clasew.contacts or clasew.outlook name spaces`


```clojure

;;; Produce script output ready for input into clasew.identities/run-script!

(p (clasew.outlook/script
    (ident/individuals)))

;;; Take the output from the script generation and run it

(p (ident/run-script!
    (clasew.outlook/script
      (ident/individuals))))

;;; Take the output from the script generation and run it, then quit application

(p (ident/run-script!
    (clasew.outlook/script
     (ident/individuals))
    (ident/quit :outlook)))


```


