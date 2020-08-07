(ns modeldsl.core)

(def sum +)
(def product *)
(def increment inc)

(defn previous [{previous-periods :previous-periods} key]
  (when (not-empty previous-periods)
    (key (last previous-periods))))

(defn this [{new-period :new-period} key]
  (key new-period))

(defn from-profile [{profile :profile} key]
  (key profile))

(defn profile-period-lookup [{profile :profile} key period]
  (or (first (keep (fn [[start end value]]
                     (when (some #{period} (range start (inc end)))
                       value))
                   (key profile))) 0))

(def put-ins #{'previous 'this 'from-profile 'profile-period-lookup})

(defn- interpret [function options]
  (if (coll? function)
    (let [[operator & operands] function]
      (cond
        (= (count function) 1) (interpret operator options)
        (put-ins operator)     (apply (eval operator) options (map #(interpret % options) operands))
        :else                  (apply (eval operator) (map #(interpret % options) operands))))
    function))

(defn- do-row [[key function default] profile previous-periods new-period]
  {key (if (and default (empty? previous-periods))
         default
         (interpret function {:profile          profile
                              :previous-periods previous-periods
                              :new-period       new-period}))})

(defn next-period [model profile previous-periods]
  (conj previous-periods
        (reduce (fn [new-period model-row]
                  (merge new-period (do-row model-row profile previous-periods new-period)))
                {}
                model)))
(comment
  (def model
    '[[:period-number (increment (previous :period-number)) 0]
      [:starting-aum  (previous :ending-aum) 0]
      [:drawdown      (product (from-profile :commitments) (nth (from-profile :contributions) (this :period-number) 0))]
      [:pnl           (product (this :starting-aum) (from-profile :return))]
      [:distribution  (product -1 (this :starting-aum) (profile-period-lookup :distributions (this :period-number)))]
      [:ending-aum    (sum (this :drawdown) (this :starting-aum) (this :pnl) (this :distribution))]])

  (def model
    '[(:period-number (inc (previous :period-number)) 0)])

  (def profile {:commitments   100
                :contributions [0.25 0.25 0.25 0.25]
                :return        0.2
                :distributions [[0 3 0]
                                [4 10 1/10]]})

  (last (take 15 (iterate (partial next-period model profile)
                          []))))

