(ns model-dsl.demo
  (:require [model-dsl.view :refer [view-scenario]]
            [model-dsl.core :refer :all]))

(def profile
  {:commitments          100000
   :contribution-profile [0.25 0.25 0.25 0.25]
   :distribution-profile [[1    4   0]
                          [5   10 0.1]
                          [11 100 0.4]]
   :mgmt-fee-profile     [[1 100 (/ 0.02 4)]]
   :return               0.2})

(def model
  '[[:period-number (increment (previous :period-number)) {:initial-value 1}]
    [:commitments   (profile-lookup :commitments)]
    [:starting-aum  (previous :ending-aum) {:initial-value 0}]
    [:contribution  (product (this :commitments)
                             (nth (profile-lookup :contribution-profile)
                                  (dec (this :period-number))
                                  0))]
    [:pnl           (product (profile-lookup :return) (this :starting-aum))]
    [:illiquid-share 0.5 {:display false}]
    [:fas-pnl       (product (this :illiquid-share) (this :pnl))]
    [:special-fee   (if (zero? (mod (this :period-number) 4))
                      -100
                      0)]
    [:mgmt-fee      (product -1
                             (min (this :starting-aum) (this :commitments))
                             (profile-period-lookup
                               :mgmt-fee-profile
                               (this :period-number))) {:initial-value 0}]
    [:distribution  (product -1
                             (this :starting-aum)
                             (profile-period-lookup
                               :distribution-profile
                               (this :period-number)))]
    [:ending-aum    (sum (this :starting-aum)
                         (this :contribution)
                         (this :pnl))]])

(view-scenario
  model
  (last (take 10 (iterate (partial next-period model profile) []))))
