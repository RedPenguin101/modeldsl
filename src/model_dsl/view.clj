(ns model-dsl.view
  (:require [clojure.inspector :refer [inspect-table]]))

(defn- row-order [model]
  (map first model))

(defn- to-vector [headers period]
  (mapv period headers))

(defn- invert [periods]
  (apply map vector periods))

(defn noshows [[name _ & [options]]]
  (if (and options (= :hide (:display options)))
    name
    nil))

(keep noshows model)

(defn view-scenario [model data]
  (inspect-table
    (invert
      (concat [(map name (row-order model))]
              (map (partial to-vector (row-order model)) data)))))

(comment
  (def model
    '[[:period-number (increment (previous :period-number)) {:initial-value 1}]
      [:commitments   (profile-lookup :commitments)]
      [:starting-aum  (previous :ending-aum) {:initial-value 0}]
      [:contribution  (product (this :commitments)
                               (nth (profile-lookup :contribution-profile)
                                    (dec (this :period-number))
                                    0))]
      [:pnl           (product (profile-lookup :return) (this :starting-aum))]
      [:illiquid-share 0.5 {:display :hide}]
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
                           (this :pnl))]]))
