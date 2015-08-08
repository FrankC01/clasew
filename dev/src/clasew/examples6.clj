(ns
  ^{:author "Frank V. Castellucci"
      :doc "Apple Contacts Examples"}
  clasew.examples6
  (:require [clojure.pprint :refer :all]
            [clasew.contacts :as cont]
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

#_(def cs3 (ident/clasew-script
          (ident/get-identities
           (cont/individuals :full_name
                             (cont/addresses :state_name)))
            [:quit]))

;(println (p cs3))
;(p (time (cont/clasew-contacts-call! cs3)))

#_(def cs4 (ident/clasew-script
         (ident/get-identities
          (cont/individuals :full_name :primary_company
                            {:first_name "Frank" :last_name "Castellucci"}))))

;(println cs4)
;(p (time (cont/clasew-contacts-call! cs4)))
