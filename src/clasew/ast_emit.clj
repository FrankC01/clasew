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

(defn if-then
  "Setup if statement"
  [token-fn value pred operand & then-expressions]
  {:type :ifthen
   :token-fn token-fn
   :test-value value
   :predicate pred
   :operand operand
   :expressions then-expressions})

(defn if-else
  "Set else statements"
  [token-fn & else-expressions]
  {:type :ifelse
   :token-fn token-fn
   :expressions else-expressions})

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

(defn blockf
  "Primary container of expressions - does not emit"
  [token-fn expressions]
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
  [token-fn rectype prop-map]
  {:type :make-new-record
   :token-fn token-fn
   :record-type rectype
   :property-map prop-map})


(defn filtered-delete
  [token-fn user-filter rectype]
  {:type         :filtered-delete
   :token-fn     token-fn
   :record-set   rectype
   :user-filter  user-filter})

(defn define-list
  "Sets target to a list type - emits 'set target to {}'"
  [token-fn target]
  {:type :define-list
   :token-fn token-fn
   :set target})


(defn assign
  [token-fn target source]
  {:type :assign
   :token-fn token-fn
   :setvalue target
   :setvalue-of source})

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

(defn term
  [token-fn to-value]
  {:type :term
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

(defn repeat-loopf
  "Creates a repeat block - emits 'repeat with itervar in source'"
  [token-fn itervar source sourceof repeat-expressions]
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

(defn quit
  []
  {:type :quit :token-fn nil})

(defn save
  []
  {:type :save :token-fn nil})


