(ns model-dsl.frontend.db
  (:require [re-frame.core :as rf]))

(def initial-db
  {:profile           {:model-name    "Fund 5"
                       :commitments   1000000
                       :contributions [0 0.25 0.25 0.25 0.25]}
   :model-rows
   {:period-number '(:increment (:previous :period-number))
    :starting-aum  '(:previous :ending-aum)
    :drawdowns     '(:product (:profile-lookup :commitments)
                              (:nth (:profile-lookup :contributions)
                                    (:this :period-number) 0))
    :pnl           '(:product (:this :starting-aum) 0.05)
    :ending-aum    '(:sum (:this :starting-aum) (:this :drawdowns) (:this :pnl))}
   :row-order         [:period-number :starting-aum :drawdowns :pnl :ending-aum]
   :periods-to-model  10
   :current-model-row {:name          :period-number
                       :code          "(:increment (:previous :period-number))"
                       :name-in-model true}})

(rf/reg-event-db
  :initialize-db
  (fn [_ _]
    initial-db))
