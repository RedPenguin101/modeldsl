(ns modeldsl.core)

(defn profile-period-lookup [profile key period]
  (or (first (keep (fn [[start end value]]
                     (when (some #{period} (range start (inc end)))
                       value))
                   (key profile))) 0))

(defn previous [previous-periods key]
  (when (not-empty previous-periods)
    (key (last previous-periods))))

(defn this [new-period key]
  (key new-period))

(defn from-profile [profile key]
  (key profile))

(defn interpret [function options]
  (if (coll? function)
    (let [[operator & operands] function]
      (println operator)
      (cond
        (= (count function) 1)              (interpret operator options)
        (= operator 'previous)              (previous (:previous-periods options) (interpret operands options))
        (= operator 'this)                  (this (:new-period options) (interpret operands options))
        (= operator 'from-profile)          (from-profile (:profile options) (interpret operands options))
        (= operator 'profile-period-lookup) (apply profile-period-lookup (:profile options)
                                                   (map #(interpret % options) operands))
        :else                               (apply operator (map #(interpret % options) operands))))
    function))

(= (quote previous) 'previous)

(defn do-row [[key function default] profile previous-periods new-period]
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

(def model
  [[:period-number
    [inc ['previous :period-number]] 0]
   [:starting-aum
    ['previous :ending-aum] 0]
   [:drawdown
    [* ['from-profile :commitments]
     [nth ['from-profile :contributions] ['this :period-number] 0]]]
   [:pnl
    [* ['this :starting-aum] ['from-profile :return]]]
   [:distribution
    [* -1 ['this :starting-aum] ['profile-period-lookup :distributions ['this :period-number]]]]
   [:ending-aum
    [+ ['this :drawdown] ['this :starting-aum] ['this :pnl] ['this :distribution]]]])

(def model2
  [[:period-number
    [inc ['previous :period-number]] 0]])

(def roll (partial next-period model {:commitments   100
                                      :contributions [0.25 0.25 0.25 0.25]
                                      :return        0.2
                                      :distributions [[0 3 0]
                                                      [4 10 1/10]]                                      }))

(last (take 5 (iterate roll [])))

(name 'previous)

(comment
  (def fund-profile {:commitments   100000
                     :contributions [1/4 1/4 1/4 1/4]
                     :return        0.2
                     :distributions [[0 3 0]
                                     [4 10 1/10]]})

  (def model
    [[:period-number (inc (previous :period-number)) 0]
     [:starting-aum  (previous :ending-aum) 0]
     [:drawdown      (* (:commitments profile)
                        ((this :period-number) (:contributions profile)))]
     [:pnl           (* (:return profile) (this :starting-aum))]
     [:distribution  (* (+ (this :starting-aum) (this :pnl) (this :drawdown))
                        (profile-lookup :distributions (this :period-number)))]
     [:ending-aum    (+ (this :starting-aum) (this :drawdown) (this :pnl) (this :distribution))]]))
