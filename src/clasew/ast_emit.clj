(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - Experimental AST emitter"}
  clasew.ast-emit)

;;;
;;;   AST Model - New Version
;;;

(defn mapset-none
  [term-kw]
  term-kw)

(defn reduce-gen
  [dmap]
  (reduce-kv #(assoc %1 %2 (if (= %3 :gen) (keyword (gensym)) %3)) dmap dmap))

(def ^:private applications
  {:outlook "\"Microsoft Outlook\""
   :contacts "\"Contacts\""
   })

(defn- get-application
  [akey]
  (get applications akey "Unknown Application"))

(defn term
  [token-fn to-value]
  {:type :term
   :token-fn token-fn
   :to-value to-value})

(defn key-term
  [key-value]
  {:type :key-term
   :token-fn nil
   :key-term key-value})

(defn string-literal
  [value]
  {:type :string-literal
   :token-fn nil
   :svalue value})

(defn symbol-literal
  [value]
  {:type :symbol-literal
   :token-fn nil
   :svalue value})

(defn eol-cmd
  "End of list command"
  [token-fn target-list list-owner]
  {:type :eol-cmd
   :token-fn token-fn
   :target-list target-list
   :list-owner  list-owner})

(defn eor-cmd
  "Emit target-rec & {source key source}"
  [token-fn target-rec rec-owner source-expression]
  {:type :eor-cmd
   :token-fn token-fn
   :target-rec target-rec
   :rec-owner  rec-owner
   :source      source-expression})

(defn list-items-cmd
  ""
  [token-fn target-list]
  {:type :li-cmd
   :token-fn token-fn
   :target-owner target-list})

(defn expression
  [token-fn exp & exps]
  {:type :expression
   :token-fn token-fn
   :expressions (conj exps exp)})

(defn append-object-expression
  [token-fn value]
  {:type :append-object
   :token-fn token-fn
   :svalue value})

(defn set-statement
  [token-fn set-lhs set-rhs]
  {:type :set-statement
   :token-fn token-fn
   :set-lhs-expression set-lhs
   :set-rhs-expression set-rhs})

; Quick terms

(defn empty-list [] (term nil "{}"))
(defn quit [] (term nil "quit\n"))
(defn null [] (term nil "null"))

; Quick expressions

(defn delete-expression
  [token-fn target-expression]
  (expression token-fn (term nil :delete)
              target-expression))

(defn count-expression
  [token-fn of-expression]
  (expression token-fn (term nil "count of ") of-expression))

(defn xofy-expression
  [token-fn x y]
  {:type :xofy-expression
   :token-fn token-fn
   :x-expression x
   :y-expression y})

;;
;; if/then, else if/then
;;

(defn if-expression
  [token-fn pred-filter & expressions]
  {:type        :if-expression
   :token-fn    token-fn
   :predicate   pred-filter
   :expressions expressions})

(defn else-if-expression
  [token-fn if-expression]
  {:type :else-if-expression
   :token-fn token-fn
   :ifexp if-expression
   })

(defn if-statement
  [token-fn ifexpression elseexpressions]
  {:type        :if-statement
   :token-fn    token-fn
   :i-expression  ifexpression
   :e-expressions elseexpressions})


(defn for-in-expression
  [token-fn control-expression in-expression & expressions]
  {:type :for-in-expression
   :token-fn token-fn
   :control control-expression
   :in      in-expression
   :expressions expressions})

(defn save-expression
  ([] (expression nil (term nil :save)))
  ([object]
   (expression nil (term nil :save) (term nil object))))

(defn string-builder
  [token-fn & expressions]
  {:type :string-builder
   :token-fn token-fn
   :expressions expressions})

(defn key-value
  [token-fn key-term-value value-expression]
  {:type     :key-value
   :token-fn token-fn
   :key-term key-term-value
   :value-expression value-expression})

(defn record-definition
  [token-fn & key-value-pairs]
  {:type :record-definition
   :token-fn token-fn
   :expressions key-value-pairs})

(defn tell
  "Sets up the enclosing tell application construct"
  [token-fn target & expressions]
  {:type :tell
   :token-fn token-fn
   :target (get-application target)
   :expressions expressions})


(defn routine
  "Setup the routine (in AS this is a handler)"
  [token-fn rname parameters & expressions]
  {:type :routine
   :token-fn token-fn
   :routine-name rname
   :parameters parameters
   :expressions expressions})


(defn return
  [token-fn retval]
  {:type :return
   :token-fn token-fn
   :return-val retval})


(defn block
  "Primary container of expressions - does not emit"
  [token-fn & expressions]
  {:type :block
   :token-fn token-fn
   :expressions expressions})


(defn define-locals
  "Sets any locals described - emits 'local x,y,z'"
  [token-fn & terms]
  {:type :define-locals
   :token-fn token-fn
   :local-terms terms})

(defn define-record
  "Sets a record to mapvars - emits 'set target to {mv1: null, mvN:null}'"
  [token-fn target mapvars]
  {:type :define-record
   :token-fn token-fn
   :set target
   :set-to mapvars})

(defn make-new-record
  "Create a new record type with associated properties
  emits 'make new rectype with properties {...}'"
  [token-fn rectype prop-map & [symbolflag]]
  {:type :make-new-record
   :token-fn token-fn
   :record-type rectype
   :symbol-flag (or symbolflag false)
   :property-map prop-map})

(defn make-new-inlist-record
  "Create a new record type with associated properties
  emits 'make new rectype with properties {...}'"
  [token-fn rectype reclist in-container prop-map & [symbolflag]]
  {:type :make-new-inlist-record
   :token-fn token-fn
   :record-type rectype
   :record-list reclist
   :symbol-flag (or symbolflag false)
   :container in-container
   :property-map prop-map})


(defn filtered-delete
  [token-fn user-filter rectype]
  {:type         :filtered-delete
   :token-fn     token-fn
   :record-set   rectype
   :user-filter  user-filter})


(defn string-p1-reference
  [token-fn target sp0 sp1]
  {:type :string-p1-reference
   :token-fn token-fn
   :set-target target
   :string-0 sp0
   :ref-1 sp1})


(defn scalar-value
  [token-fn to-value]
  {:type :scalar-value
   :token-fn token-fn
   :to-value to-value})


(defn count-of
  "Sets a value to the count of expression results"
  [token-fn target expression]
  {:type :count-of
   :token-fn token-fn
   :set-target target
   :expressions expression})

(defn properties-of
  "Sets a variable to the properties of a class"
  [token-fn value properties-of]
  {:type :properties-of
   :token-fn token-fn
   :value value
   :properties-of properties-of})

(defn value-of
  "Sets a value from another value"
  [token-fn value from apply-function]
  {:type :value-of
   :token-fn token-fn
   :value value
   :from from
   :apply-function apply-function})

(defn value-of-as-string
  "Sets a value from another value"
  [token-fn value from apply-function]
  {:type :value-of-as-string
   :token-fn token-fn
   :value value
   :from from
   :apply-function apply-function})


(defn extend-record
  "Sets a value from another value"
  [token-fn targetmap keyw value]
  {:type :extend-record
   :token-fn token-fn
   :target-map targetmap
   :value value
   :keywrd keyw})

(defn record-value
  "Sets a value of key in map"
  [token-fn ofmap mapval to-expression]
  {:type :record-value
   :token-fn token-fn
   :mapvalue mapval
   :ofmap ofmap
   :to to-expression})

(defn filter-expression
  [token-fn source source-of user-filter]
  {:type :filter-expression
   :token-fn token-fn
   :source source
   :source-of source-of
   :user-filter user-filter})

(defn from-filter
  "Set a variable to the result of a filter 'whose' statement"
  [token-fn value source source-of user-filter]
  {:type :from-filter
   :token-fn token-fn
   :source source       ; e.g. contacts/people
   :source-of source-of ;
   :value value         ; what var are we setting result to
   :user-filter user-filter})


(defn extend-list
  "Sets the end of target list to source - emits 'set end of target to source'"
  [token-fn target source]
  {:type :extend-list
   :token-fn token-fn
   :set target
   :to source})

(defn extend-list-with-expression
  "Sets the end of target list to source - emits 'set end of target to source'"
  [token-fn target to-expression]
  {:type :extend-list-with-expression
   :token-fn token-fn
   :target target
   :to-expression to-expression})

(defn repeat-loop
  "Creates a repeat block - emits 'repeat with itervar in source'"
  [token-fn itervar source sourceof & repeat-expressions]
  {:type :repeat-loop
   :token-fn token-fn
   :source source
   :iteration-var itervar
   :source-of sourceof
   :expressions repeat-expressions})


(defn filtered-repeat-loop
  "Creates a filtering construct for repeating over"
  [token-fn property-var user-filter source sourceof & repeat-expressions]
  (reduce-gen {:type :filtered-repeat-loop
   :token-fn token-fn
   :user-filter user-filter   ; user filter map
   :filter-result :gen        ; put result of filter 'whose' set
   :source source             ; target source object
   :source-of sourceof        ; target source object owner (optional)
   :iteration-var :fitr       ; internal loop over filter-result
   :property-var property-var ; target of get properties of iteration-var
   :expressions repeat-expressions}))



