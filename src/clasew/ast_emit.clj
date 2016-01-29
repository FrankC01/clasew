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
  {:type TERM
   :token-fn token-fn
   :to-value to-value})

(defn term-nl
  [to-value]
  {:type TERM-NL
   :token-fn nil
   :to-value to-value})

(defn key-term
  [key-value]
  {:type KEY-TERM
   :token-fn nil
   :key-term key-value})

(defn property-term
  [key-value]
  {:type PROPERTY-TERM
   :token-fn nil
   :key-term key-value})

(defn key-term-nl
  [key-value]
  {:type KEY-TERM-NL
   :token-fn nil
   :key-term key-value})

(defn string-literal
  [value]
  {:type STRING-LITERAL
   :token-fn nil
   :svalue value})

(defn symbol-literal
  [value]
  {:type SYMBOL-LITERAL
   :token-fn nil
   :svalue value})

(defn numeric-literal
  [value]
  {:type NUMERIC-LITERAL
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
  {:type BLOCK
   :token-fn token-fn
   :expressions expressions})

(defn expression
  [token-fn exp & exps]
  {:type EXPRESSION
   :token-fn token-fn
   :expressions (conj exps exp)})

(defn tell
  "Sets up the enclosing tell application construct"
  [token-fn target & expressions]
  {:type TELL
   :token-fn token-fn
   :target (get-application target)
   :expressions expressions})

(defn routine
  "Setup the routine (in AS this is a handler)"
  [token-fn rname parmcoll & expressions]
  {:type ROUTINE
   :token-fn token-fn
   :routine-name rname
   :parameters parmcoll
   :expressions expressions})

(defn routine-call
  [token-fn routine-expression & arg-expressions]
  {:type ROUTINE-CALL
   :token-fn token-fn
   :routine-name routine-expression
   :routine-arguments arg-expressions})

(defn script
  [token-fn scriptkw autorun & expressions]
  {:type SCRIPT
   :token-fn token-fn
   :script-name scriptkw
   :auto-run autorun
   :expressions expressions})

(defn return
  [token-fn retval]
  {:type RETURN
   :token-fn token-fn
   :return-val retval})

;; AST predicate tokens

(def PREDICATE-STATEMENT    ::predicate-statement)
(def PREDICATE-EXPRESSIONS  ::predicate-expressions)
(def PREDICATE-OPERATOR     ::predicate-operator)
(def PREDICATE-CONDITION    ::predicate-condition)

;; AST predicate constructs

(defn predicate-statement
  "Predicate statement expression builder"
  [token-fn & predicate-expressions]
  {:type PREDICATE-STATEMENT
   :token-fn token-fn
   :expressions predicate-expressions})

(defn predicate-expressions
  "Predicate expression: 1 or more predicate mappings"
  [token-fn & predicate-conditions]
  {:type PREDICATE-EXPRESSIONS
   :token-fn token-fn
   :conditions predicate-conditions})

(defn predicate-operator
  "Handles operator conversion"
  [ptype]
  {:type PREDICATE-OPERATOR
   :token-fn nil
   :term ptype})

(defn predicate-condition
  [token-fn lhs-expression predicate-op rhs-expression]
  {:type PREDICATE-CONDITION
   :token-fn token-fn
   :lhs-expression lhs-expression
   :operator predicate-op
   :rhs-expression rhs-expression})

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

;; AST data CRUD constructs

(defn define-locals
  "Sets any locals described - emits 'local x,y,z'"
  [token-fn & terms]
  {:type DEFINE-LOCALS
   :token-fn token-fn
   :local-terms terms})

(defn set-statement
  [token-fn set-lhs set-rhs]
  {:type SET-STATEMENT
   :token-fn token-fn
   :set-lhs-expression set-lhs
   :set-rhs-expression set-rhs})

(defn key-value
  [token-fn key-term-value value-expression]
  {:type     KEY-VALUE
   :token-fn token-fn
   :key-term key-term-value
   :value-expression value-expression})

(defn property
  [token-fn key-value-statement]
  {:type PROPERTY
   :token-fn token-fn
   :kv key-value-statement})

(defn xofy-expression
  [token-fn x y]
  {:type XOFY-EXPRESSION
   :token-fn token-fn
   :x-expression x
   :y-expression y})

(defn list-of
  [token-fn coll]
  {:type LIST-OF
   :token-fn token-fn
   :expressions coll})

(defn eol-cmd
  "End of list command"
  [token-fn target-list list-owner]
  {:type END-OF-LIST
   :token-fn token-fn
   :target-list target-list
   :list-owner  list-owner})

(defn record-definition
  [token-fn & key-value-pairs]
  {:type RECORD
   :token-fn token-fn
   :expressions key-value-pairs})

(defn eor-cmd
  "Emit target-rec & {source key source}"
  [token-fn target-rec rec-owner source-expression]
  {:type END-OF-RECORD
   :token-fn token-fn
   :target-rec target-rec
   :rec-owner  rec-owner
   :source      source-expression})

;; AST conditional tokens

(def IF-STATEMENT          ::if-statement)
(def IF-EXPRESSION         ::if-expression)
(def ELSE-IF-EXPRESSION    ::else-if-expression)

;; AST conditional constructs

(defn if-statement
  [token-fn ifexpression elseexpressions]
  {:type        IF-STATEMENT
   :token-fn    token-fn
   :i-expression  ifexpression
   :e-expressions elseexpressions})

(defn if-expression
  [token-fn pred-filter & expressions]
  {:type        IF-EXPRESSION
   :token-fn    token-fn
   :predicate   pred-filter
   :expressions expressions})

(defn else-if-expression
  [token-fn if-expression]
  {:type ELSE-IF-EXPRESSION
   :token-fn token-fn
   :ifexp if-expression
   })

;; AST iteration tokens

(def FOR-EXPRESSION        ::for-expression)

;; AST iteration constructs

(defn for-in-expression
  [token-fn control-expression in-expression & expressions]
  {:type FOR-EXPRESSION
   :token-fn token-fn
   :control control-expression
   :in      in-expression
   :expressions expressions})

;; TODO
;; TODO
;; ======================= NEEDS EXCERCISE ===========================
;; TODO
;; TODO

;;
;; Filter/predicate support
;;


(defn predicate
  "Used for legacy filtering"
  [token-fn userfilt]
  {:type :predicate
   :token-fn token-fn
   :pred userfilt})

(defn and-predicate-join
  "Emits 'and (...)'"
  [token-fn predicate-expressions]
  {:type :and-predicate
   :token-fn token-fn
   :expressions predicate-expressions})

(defn or-predicate-join
  "Emits 'or (...)'"
  [token-fn predicate-expressions]
  {:type :or-predicate
   :token-fn token-fn
   :expressions predicate-expressions})

(defn where-filter
  [token-fn target userfilt]
  {:type :where-filter
   :token-fn token-fn
   :target    target
   :predicate userfilt})

(defn make-new
  [token-fn target-expr & exprs]
  {:type :make-new
   :token-fn token-fn
   :target-expr target-expr
   :expressions exprs})

(defn list-items-cmd
  ""
  [token-fn target-list]
  {:type :li-cmd
   :token-fn token-fn
   :target-owner target-list})

(defn append-object-expression
  [token-fn value]
  {:type :append-object
   :token-fn token-fn
   :svalue value})

(defn string-builder
  [token-fn & expressions]
  {:type :string-builder
   :token-fn token-fn
   :expressions expressions})

(defn extend-record
  "Sets a value from another value"
  [token-fn targetmap keyw value]
  {:type :extend-record
   :token-fn token-fn
   :target-map targetmap
   :value value
   :keywrd keyw})


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

; Quick expressions

(defn count-expression
  [token-fn of-expression]
  (expression token-fn (term nil "count of ") of-expression))


;;
;; Misc
;;

(defn get-xofy
  [token-fn x y]
  (get-statement
   token-fn
   (xofy-expression
    nil
    (if (keyword? x) (term nil x) x)
    (if (keyword? y) (term nil y) y))))


;;
;; Misc expressions
;;

(defn save-statement
  ([] (expression
       nil
       (term nil :save)
       new-line))
  ([object]
   (expression
    nil
    (term nil :save)
    (term nil object)
    new-line)))


;;
;; Ease of use forms
;;

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

