(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - Experimental AST emitter"}
  clasew.ast-emit)

;;;
;;;   AST Model - New Version
;;;

(def ^:private applications
  {:outlook "\"Microsoft Outlook\""
   :contacts "\"Contacts\""
   :mail     "\"Mail\""
   })

(defn- get-application
  [akey]
  (get applications akey "Unknown Application"))

;; AST terminal tokens

(def TERM               ::term)
(def TERM-NL            ::term-nl)
(def KEY-TERM           ::key-term)
(def PROPERTY-TERM      ::key-term)
(def KEY-TERM-NL        ::key-term-NL)
(def STRING-LITERAL     ::string-literal)
(def SYMBOL-LITERAL     ::symbol-literal)
(def NUMERIC-LITERAL    ::numeric-literal)

;; AST terminal constructs

(defn term
  [token-fn to-value]
  {:ast-type TERM
   :token-fn token-fn
   :to-value to-value})

(defn term-nl
  [to-value]
  {:ast-type TERM-NL
   :token-fn nil
   :to-value to-value})

(defn key-term
  [key-value]
  {:ast-type KEY-TERM
   :token-fn nil
   :key-term key-value})

(defn property-term
  [key-value]
  {:ast-type PROPERTY-TERM
   :token-fn nil
   :key-term key-value})

(defn key-term-nl
  [key-value]
  {:ast-type KEY-TERM-NL
   :token-fn nil
   :key-term key-value})

(defn string-literal
  [value]
  {:ast-type STRING-LITERAL
   :token-fn nil
   :svalue value})

(defn symbol-literal
  [value]
  {:ast-type SYMBOL-LITERAL
   :token-fn nil
   :svalue value})

(defn numeric-literal
  [value]
  {:ast-type NUMERIC-LITERAL
   :token-fn nil
   :nvalue value})

;; AST expression tokens

(def BLOCK              ::block)
(def EXPRESSION         ::block)
(def TELL               ::tell)
(def ROUTINE            ::routine)
(def ROUTINE-CALL       ::routine-call)
(def SCRIPT             ::script)
(def RETURN             ::return)

;; AST expression constructs

(defn block
  "Primary container of expressions - does not emit"
  [token-fn & expressions]
  {:ast-type BLOCK
   :token-fn token-fn
   :expressions expressions})

(defn expression
  [token-fn exp & exps]
  {:ast-type EXPRESSION
   :token-fn token-fn
   :expressions (conj exps exp)})

(defn tell
  "Sets up the enclosing tell application construct"
  [token-fn target & expressions]
  {:ast-type TELL
   :token-fn token-fn
   :target (get-application target)
   :expressions expressions})

(defn routine
  "Setup the routine (in AS this is a handler)"
  [token-fn rname parmcoll & expressions]
  {:ast-type ROUTINE
   :token-fn token-fn
   :routine-name rname
   :parameters parmcoll
   :expressions expressions})

(defn routine-call
  [token-fn routine-expression & arg-expressions]
  {:ast-type ROUTINE-CALL
   :token-fn token-fn
   :routine-name routine-expression
   :routine-arguments arg-expressions})

(defn script
  [token-fn scriptkw autorun & expressions]
  {:ast-type SCRIPT
   :token-fn token-fn
   :script-name scriptkw
   :auto-run autorun
   :expressions expressions})

(defn return
  [token-fn retval]
  {:ast-type RETURN
   :token-fn token-fn
   :return-val retval})

;; AST predicate tokens

(def PREDICATE-STATEMENT    ::predicate-statement)
(def PREDICATE-EXPRESSIONS  ::predicate-expressions)
(def PREDICATE-OPERATOR     ::predicate-operator)
(def PREDICATE-CONDITION    ::predicate-condition)
(def PREDICATE-AND          ::predicate-and)
(def PREDICATE-OR           ::predicate-or)
(def WHERE-FILTER           ::where-filter)

;; AST predicate constructs

(defn predicate-statement
  "Predicate statement expression builder"
  [token-fn & predicate-expressions]
  {:ast-type PREDICATE-STATEMENT
   :token-fn token-fn
   :expressions predicate-expressions})

(defn predicate-expressions
  "Predicate expression: 1 or more predicate mappings"
  [token-fn & predicate-conditions]
  {:ast-type PREDICATE-EXPRESSIONS
   :token-fn token-fn
   :conditions predicate-conditions})

(defn predicate-operator
  "Handles operator conversion"
  [ptype]
  {:ast-type PREDICATE-OPERATOR
   :token-fn nil
   :term ptype})

(defn predicate-condition
  [token-fn lhs-expression predicate-op rhs-expression]
  {:ast-type PREDICATE-CONDITION
   :token-fn token-fn
   :lhs-expression lhs-expression
   :operator predicate-op
   :rhs-expression rhs-expression})

(defn and-predicate-join
  "Emits 'and (...)'"
  [token-fn predicate-expressions]
  {:ast-type PREDICATE-AND
   :token-fn token-fn
   :expressions predicate-expressions})

(defn or-predicate-join
  "Emits 'or (...)'"
  [token-fn predicate-expressions]
  {:ast-type PREDICATE-OR
   :token-fn token-fn
   :expressions predicate-expressions})

(defn where-filter
  [token-fn target userfilt]
  {:ast-type WHERE-FILTER
   :token-fn token-fn
   :target    target
   :predicate userfilt})



;; AST data CRUD tokens

(def DEFINE-LOCALS           ::define-locals)
(def SET-STATEMENT           ::set-statement)
(def KEY-VALUE               ::key-value)
(def PROPERTY                ::property)
(def XOFY-EXPRESSION         ::xofy-expression)
(def LIST-OF                 ::list-of)
(def END-OF-LIST             ::eol)
(def RECORD                  ::record)
(def END-OF-RECORD           ::eor)
(def STRING-BUILDER          ::string-builder)
(def MAKE-NEW-OBJECT         ::make-new-object)

;; AST data CRUD constructs

(defn define-locals
  "Sets any locals described - emits 'local x,y,z'"
  [token-fn & terms]
  {:ast-type DEFINE-LOCALS
   :token-fn token-fn
   :local-terms terms})

(defn set-statement
  [token-fn set-lhs set-rhs]
  {:ast-type SET-STATEMENT
   :token-fn token-fn
   :set-lhs-expression set-lhs
   :set-rhs-expression set-rhs})

(defn key-value
  [token-fn key-term-value value-expression]
  {:ast-type     KEY-VALUE
   :token-fn token-fn
   :key-term key-term-value
   :value-expression value-expression})

(defn property
  [token-fn key-value-statement]
  {:ast-type PROPERTY
   :token-fn token-fn
   :kv key-value-statement})

(defn xofy-expression
  [token-fn x y]
  {:ast-type XOFY-EXPRESSION
   :token-fn token-fn
   :x-expression x
   :y-expression y})

(defn list-of
  [token-fn coll]
  {:ast-type LIST-OF
   :token-fn token-fn
   :expressions coll})

(defn eol-cmd
  "End of list command"
  [token-fn target-list list-owner]
  {:ast-type END-OF-LIST
   :token-fn token-fn
   :target-list target-list
   :list-owner  list-owner})

(defn record-definition
  [token-fn & key-value-pairs]
  {:ast-type RECORD
   :token-fn token-fn
   :expressions key-value-pairs})

(defn eor-cmd
  "Emit target-rec & {source key source}"
  [token-fn target-rec rec-owner source-expression]
  {:ast-type END-OF-RECORD
   :token-fn token-fn
   :target-rec target-rec
   :rec-owner  rec-owner
   :source      source-expression})

(defn string-builder
  [token-fn & expressions]
  {:ast-type STRING-BUILDER
   :token-fn token-fn
   :expressions expressions})

(defn make-new
  [token-fn target-expr & exprs]
  {:ast-type MAKE-NEW-OBJECT
   :token-fn token-fn
   :target-expr target-expr
   :expressions exprs})



;; AST conditional tokens

(def IF-STATEMENT          ::if-statement)
(def IF-EXPRESSION         ::if-expression)
(def ELSE-IF-EXPRESSION    ::else-if-expression)

;; AST conditional constructs

(defn if-statement
  [token-fn ifexpression elseexpressions]
  {:ast-type        IF-STATEMENT
   :token-fn    token-fn
   :i-expression  ifexpression
   :e-expressions elseexpressions})

(defn if-expression
  [token-fn pred-filter & expressions]
  {:ast-type        IF-EXPRESSION
   :token-fn    token-fn
   :predicate   pred-filter
   :expressions expressions})

(defn else-if-expression
  [token-fn if-expression]
  {:ast-type ELSE-IF-EXPRESSION
   :token-fn token-fn
   :ifexp if-expression
   })

;; AST iteration tokens

(def FOR-EXPRESSION        ::for-expression)

;; AST iteration constructs

(defn for-in-expression
  [token-fn control-expression in-expression & expressions]
  {:ast-type FOR-EXPRESSION
   :token-fn token-fn
   :control control-expression
   :in      in-expression
   :expressions expressions})

; Quick terms

(def   new-line         (term nil "\n"))
(def   empty-list       (term nil "{}"))
(def   quit             (term nil "quit\n"))
(def   null             (term nil "null"))
(def   delete           (term nil "delete "))
(def   with-properties  (term nil " with properties "))
(def   as-string        (term nil " as string"))
(def   as-list          (term nil " as list "))
(def   noop             (term nil ""))
(def   conjoin          (term nil " & "))
(def   count-of         (term nil "count of "))
(def   getin            (term nil "get "))
(def   whose            (term nil " whose "))
(def   where            (term nil " where "))
(def   its              (term nil " it's "))
(def   lparen           (term nil "("))
(def   rparen           (term nil ")"))
(def   first-of         (term nil "first item"))
(def   second-of        (term nil "second item"))
(def   at               (term nil " at "))
(def   as_send          (term nil "send "))
(def   as_save          (term nil "save"))

; Quick expressions

(defn count-expression
  [token-fn of-expression]
  (expression
    token-fn
    count-of
    of-expression))

(defn append-object-expression
  [token-fn with-expression]
  (expression
    token-fn
    (if (map? with-expression)
      with-expression
      (term nil with-expression))))

(defn get-statement
  [token-fn expressions]
  (expression
   token-fn
   lparen
   getin
   expressions
   rparen))

(defn precedence
  [token-fn expressions]
  (expression
   token-fn
   lparen
   expressions
   rparen))

;;
;; Ease of use forms
;;

(defn get-xofy
  "Explicit get over xofy"
  [token-fn x y]
  (get-statement
   token-fn
   (xofy-expression
    nil
    (if (keyword? x) (term nil x) x)
    (if (keyword? y) (term nil y) y))))


(defn save-statement
  "Generic save AST"
  ([] (expression nil as_save new-line))
  ([object]
   (expression
    nil
    as_save
    (term nil object)
    new-line)))

(defn predicate-conditions-from-filter
  "Generates a predicate condition from
  a clasew filter map"
  [[lhs op rhs & [owner]]]
  (let [nrhs (cond
               (string? rhs) (string-literal rhs)
               (number? rhs) (numeric-literal rhs)
               :else (term nil rhs))
        nlhs (if (nil? owner)
               (term nil lhs)
               (xofy-expression
                 nil
                 (term nil lhs)
                 (term nil owner)))]
    (predicate-condition
      nil
      nlhs
      (predicate-operator op)
      nrhs)))

(defn predicate-expression-from-filter
  "Genreates predicate expression(s) from
  a clasew filter map"
  [acc {:keys [joins args] :as filtexp}]
  (let [acc1 (conj acc
               (apply (partial predicate-expressions nil)
                      (map predicate-conditions-from-filter args)))]
    (if (empty? joins)
      acc1
      (into acc1
            (map #(if (= (first %) :and)
                    (and-predicate-join
                      nil
                      (predicate-expression-from-filter
                        [] (second %)))
                    (or-predicate-join
                      nil
                      (predicate-expression-from-filter
                        [] (second %))))
                 joins)))))

(defn predicate-from-filter
  "Generates the predicate construct from a clasew filter map"
  [token-fn filtexp]
  (let [exps (predicate-expression-from-filter [] filtexp)]
    (apply (partial predicate-statement token-fn) exps)))

(defn if-only
  [& expressions]
  (if-statement
   nil
   (apply (partial if-expression nil) expressions)
   nil))

(defn set-result-msg-with-count
  "Creates a result s including a count of ct and
  placed in rt"
  [s rt ct]
  (set-statement
   nil
   (eol-cmd nil rt nil)
   (string-builder
    nil
    (string-literal s)
    (count-expression nil (term nil ct)))))

(defn kv-template
  "Creates a key-value AST of the form:
  'termkw: (get targkw of sourcekw)' applying
  the tokenfn to targkw"
  [target-token-fn termkw sourcekw]
  (key-value
   nil
   (key-term termkw)
   (get-statement
    nil
    (xofy-expression
     nil
     (term target-token-fn termkw) (term nil sourcekw)))))

(defn kv-template-t
  "Creates a key-value AST of the form:
  'termkw: (get targkw of sourcekw)' applying
  the tokenfn to targkw of key-value"
  [target-token-fn termkw sourcekw]
  (key-value
   target-token-fn
   (key-term termkw)
   (get-statement
    nil
    (xofy-expression
     nil
     (term-nl termkw) (term nil sourcekw)))))

(defn- kv-template2
  [[termkw ofvalue]]
  (key-value
   nil
   (key-term termkw)
   (condp #(%1 %2) ofvalue
     nil?    null
     string? (string-literal ofvalue)
     vector? (list-of
              nil
              (map #(string-literal %) ofvalue)))))

(defn setrecord-frommap
  "Generates a AS record from a clojure map"
  [fmap]
  (apply (partial record-definition nil)
         (map #(kv-template2 %) fmap)))

(defn set-empty-record
  "Creates a record with keys whose values are null"
  [token-fn rectarget argtargets & [{:keys [ktfn] :or {ktfn key-term}}]]
  (set-statement
   nil
   (term nil rectarget)
   (apply (partial record-definition nil)
    (map #(key-value nil (ktfn %) null) argtargets))))

(defn setrecordvalues
  "Given a list of vars, generate constructs to set a Applescript record value
  from a source value found in another record"
  [token-fn mapvars targetmap sourcemap]
  (reduce #(conj
            %1
            (set-statement
             token-fn
             (xofy-expression
              (fn [term-kw] (name term-kw))
              (term nil %2)
              (term nil targetmap))
             (routine-call
              token-fn
              (term nil :cleanval)
              (xofy-expression
               nil
               (term nil %2)
               (term nil sourcemap)))))
          [] mapvars))


(defn set-extend-record
  "Similar to clojure assoc call, set-extend-record emits:
  set targ to targ & {skey:sval}"
  [targ skey sval]
  (set-statement
   nil
   (term nil targ)
   (eor-cmd
    nil
    targ nil
    (record-definition
     nil
     (key-value nil (key-term skey) (term nil sval))))))

(defn record-fetch
  "Builds inline record population/setters and sets
  token lookup on source attribute"
  [rhstoken args accum source]
  (if (empty? args)
    (block nil)
    (set-statement
     nil
     (term nil accum)
     (xofy-expression
      nil
      (apply (partial record-definition nil)
             (into [] (map #(key-value
                    nil
                    (key-term %)
                    (term rhstoken %)) args)))
      (term nil source)))))

