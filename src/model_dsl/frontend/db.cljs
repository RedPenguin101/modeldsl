(ns model-dsl.frontend.db
  (:require [re-frame.core :as rf]))

(def initial-db
  {:current-entity-id "abc"
   :current-entity-name "Bravo"
   :profile
   "{:model-name \"Fund 5\", \n :commitments 1000000, \n :contributions [0 0.25 0.25 0.25 0.25]\n :hello \"world\"\n :happiness 123}"
   :model
   {:period-number
    {:code       '[(:increment (:previous :period-number))]
     :string-rep "(increment (previous period-number))\n"}
    :starting-aum {:code '[(:previous :ending-aum)] :string-rep "(previous ending-aum)"}
    :drawdowns    {:code       '[(:product (:profile-lookup :commitments)
                                           (:nth (:profile-lookup :contributions)
                                                 (:this :period-number) 0))]
                   :string-rep "(product \n (profile-lookup commitments) \n (nth (profile-lookup contributions) \n (this period-number) 0))"}
    :pnl          {:code       '[(:product (:this :starting-aum) 0.05)]
                   :string-rep "(product (this starting-aum) \n 0.05)"}
    :ending-aum   {:code       '[(:sum (:this :starting-aum)
                                       (:this :drawdowns)
                                       (:this :pnl))]
                   :string-rep "(sum (this starting-aum) \n (this drawdowns) \n (this pnl))"}}
   :measure-order         [:period-number :starting-aum :drawdowns :pnl :ending-aum], :periods-to-model 10
   :selected-measure {:name          :period-number
                      :code          "(increment (previous period-number))\n"}})

(rf/reg-event-db
 :initialize-db
 (fn [_ _]
   initial-db))
