(ns model-dsl.demoacf2
  (:require [model-dsl.view :refer [tabulate view-scenario write-out-scenario]]
            [model-dsl.core :refer [increment sum product
                                    this previous accumulated if
                                    profile-lookup profile-period-lookup
                                    next-period]]))

(def profile
  {:commitments          {:gp    22500000
                          :lp    727500000
                          :total 750000000}
   :contribution-profile [0.1 0.1 0.1 0.1 0.15 0.15 0.15 0.15]
   :distribution-profile [[1   10   0]
                          [11 100 0.18]]
   :mgmt-fee-profile     [[1 100 (/ 0.014 4)]]
   :return               (- (Math/pow 1.1535 1/4) 1)
   :tax-rate             0.4
   :incentive            0.2
   :catchup              1
   :hurdle               (/ 0.08 4)
   :illiquid-share       0.5})

(def incentive 1)

(defn incentive-on
  "Calculates the share of profit to be paid to the GP, based on a hurdle amount and carry rate"
  [profit hurdle rate]
  (cond (< profit hurdle)                        0
        (> profit
           (* (+ 1 (/ rate (- 1 rate))) hurdle)) (* rate profit)
        :else                                    (- profit hurdle)))

(incentive-on 410 132.7 0.2)

(def model
  '[[:investor "lp"]
    [:period-number (increment (previous :period-number))
     {:initial-value 1}]

    [:commitments   (:total (profile-lookup :commitments))]

    [:starting-aum  (previous :ending-aum)
     {:initial-value 0}]

    [:contribution  (product (this :commitments)
                             (nth (profile-lookup :contribution-profile)
                                  (dec (this :period-number))
                                  0))]

    [:pnl           (product (profile-lookup :return) (this :starting-aum))]

    [:illiquid-share (max 0 (* (profile-lookup :illiquid-share)
                               (- 1 (/ (this :period-number) 27))))]

    [:fas-pnl       (product (this :illiquid-share) (this :pnl))]

    [:mgmt-fee      (product -1
                             (min (this :starting-aum) (this :commitments))
                             (profile-period-lookup
                               :mgmt-fee-profile
                               (this :period-number)))
     {:initial-value 0}]

    [:distribution  (product -1
                             (this :starting-aum)
                             (profile-period-lookup
                               :distribution-profile
                               (this :period-number)))]

    [:ending-aum    (sum (this :starting-aum)
                         (this :contribution)
                         (this :mgmt-fee)
                         (this :pnl))]

    [:hurdle        (product (profile-lookup :hurdle) (this :starting-aum))]

    [:accrued-incentive (incentive-on (sum (accumulated :mgmt-fee) (accumulated :pnl))
                                      (accumulated :hurdle)
                                      (profile-lookup :incentive))]])

(view-scenario
  (tabulate model
            (last (take 10 (iterate (partial next-period model profile) [])))))


(write-out-scenario
  "hello"
  (tabulate model
            (last (take 28 (iterate (partial next-period model profile) [])))))
