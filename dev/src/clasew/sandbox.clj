(ns
  ^{:author "Frank V. Castellucci"
      :doc "spike space"}
  clasew.sandbox
  (:require [clojure.pprint :refer :all]
            ;[clasew.contacts :as cont]
            [clasew.outlook :as outl]
            [clasew.identities :as ident])
  )

;; Demonstrate record coercion
(def p pprint)


;;; Apple Contacts

;(def cs0 (ident/clasew-script (ident/get-identities)[:quit]))
;(p (time (cont/clasew-contacts-call! cs0)))

#_(def cs1 (ident/clasew-script
          (ident/get-identities (cont/individuals))
          [:quit]))

;(println cs1)
;(p (time (cont/clasew-contacts-call! cs1)))


#_(def cs2 (ident/clasew-script
          (ident/get-identities (cont/individuals (cont/addresses)))
          [:quit]))


;(println cs2)
;(p (time (cont/clasew-contacts-call! cs2)))

;;; Microsoft Outlook

;(def cs3 (ident/clasew-script (ident/get-identities)[:quit]))
;(p cs3)
;(p (time (outl/clasew-contacts-call! cs3)))

#_(def cs4 (ident/clasew-script
          (ident/get-identities (outl/individuals))
          [:quit]))

;(println cs4)
;(p (time (outl/clasew-contacts-call! cs4)))

#_(def cs5 (ident/clasew-script
          (ident/get-identities (outl/individuals (outl/addresses)))
          [:quit]))


;(println cs5)
;(p (time (outl/clasew-contacts-call! cs5)))

#_(def cs6 (ident/clasew-script
          (ident/get-identities
           (outl/individuals :full_name
                             (outl/addresses :state_name)))))

;(println (p cs6))
;(p (time (outl/clasew-contacts-call! cs6)))


