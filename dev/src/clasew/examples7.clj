(ns
  ^{:author "Frank V. Castellucci"
      :doc "Microsoft Office Examples"}
  clasew.examples7
  (:require [clojure.pprint :refer :all]
            [clasew.outlook :as outl]
            [clasew.identities :as ident])
  )

;; Demonstrate record coercion
(def p pprint)


;;; Microsoft Outlook

;(def ol0 (ident/clasew-script (ident/get-identities)[:quit]))
;(p ol0)
;(p (time (outl/clasew-contacts-call! ol0)))

#_(def ol1 (ident/clasew-script
          (ident/get-identities (outl/individuals))
          [:quit]))

;(println ol1)
;(p (time (outl/clasew-contacts-call! ol1)))

#_(def ol2 (ident/clasew-script
          (ident/get-identities (outl/individuals (outl/addresses)))
          [:quit]))


;(println ol2)
;(p (time (outl/clasew-contacts-call! ol2)))

#_(def ol3 (ident/clasew-script
          (ident/get-identities
           (outl/individuals :full_name
                             (outl/addresses :state_name)))
            [:quit]))

;(println ol3)
;(p (time (outl/clasew-contacts-call! ol3)))

#_(def ol4 (ident/clasew-script
         (ident/get-identities
          (outl/individuals :full_name :primary_company
                            {:first_name "Frank"}))))

;(println ol4)
;(p (time (outl/clasew-contacts-call! ol4)))

#_(def ol5 (ident/clasew-script
            (ident/get-identities
             (outl/individuals :full_name :primary_company
                               {:first_name "Frank"}
                               (ident/addresses)
                               (ident/emails)))
            [:quit]))

;(println ol5)
;(p (time (outl/clasew-contacts-call! ol5)))
