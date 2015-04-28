# clasew

A Clojure AppleScriptEngine Wrapper

## Intent

clasew provide an idiomatic clojure wrapper for Java ScriptManager, specifically AppleScriptManager,  and scriptable application DSLs for common usage.


## Architecture
As per the Intent, the clasew architecture is very straight forward.

<img style="float: " src="doc/clasew-arch.png" alt="clasew architecture" title="clasew architecture" height="300" width="600"/>

### Foundation
Consists of the clasew.core namespace. Contains the raw functions for interacting with AppleScript. See the examples code and scripts for more detail.

### DSLs
Consists of clasew.*appname* namespaces where ***appname*** is collection of idiomatic functions specific to a scriptable application.

## Examples

See examplesX.clj in [sandbox](dev/src/clasew)

1. [Example 1](dev/src/clasew/examples1.clj) - Demonstrates standard 'tell' calls
2. [Example 2](dev/src/clasew/examples2.clj) - Demonstrates handlers and argument passing
3. [Example 3](dev/src/clasew/examples3.clj) - Demonstrates separate argument bindings
4. [Example 4](dev/src/clasew/examples4.clj) - Demonstrates clasew-excel DSL

My dev environment:

* Yosemite 10.10.3
* Oracle Java 1.8.0_31
* Lighttable 0.72

## License

Copyright © Frank V. Castellucci. All rights reserved.

Distributed under the Eclipse Public License (EPL) version 1.0.
