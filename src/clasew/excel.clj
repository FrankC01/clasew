(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - Excel DSL"}
  clasew.excel
  (:require [clasew.core :as as]
            [clojure.java.io :as io]))


;; Setup our own engine

(defonce ^:private prv-eng as/new-eng)   ; Use engine for this namespace


;;
;; Low level DSL functions ----------------------------------------------------
;;

(def ^:private scrptfile (io/resource "clasew-excel.applescript"))

(defn clasew-excel-call!
  "Takes 1 or more maps produced from clasew-script and invokes AppleScript
  for execution.
  Return map is same as clasew.core/run-ascript!"
  [& scripts]
  {:pre [(> (count scripts) 0)]}
  (let [argv (list (into [] scripts))]
    (with-open [rdr (io/reader scrptfile)]
      (as/run-ascript! prv-eng rdr
                      :reset-binding true
                      :bind-function "clasew_eval"
                      :arguments argv))))

