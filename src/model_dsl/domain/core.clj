(ns model-dsl.domain.core)

(def sum +)
(def product *)
(def increment inc)

(defn previous [{previous-periods :previous-periods} key]
  (when (not-empty previous-periods)
    (key (last previous-periods))))

(defn this [{new-period :new-period} key]
  (key new-period))

(defn profile-lookup [{profile :profile} key]
  (key profile))

(defn match-period [period-params period-number]
  (or (first (keep (fn [[start end value]]
                     (when (some #{period-number} (range start (inc end)))
                       value))
                   period-params))
      0))

(defn profile-period-lookup [{profile :profile} key period]
  (match-period (key profile) period))

(defn accumulated [{:keys [new-period previous-periods]} key]
  (reduce + (map key (conj previous-periods new-period))))

(defn model-if [options pred t-branch e-branch]
  (if (interpret pred options)
    (interpret t-branch options)
    (interpret e-branch options)))

(def put-ins #{'previous 'this 'profile-lookup 'profile-period-lookup 'if
               'accumulated})

(def put-ins2 #{:previous :this :profile-lookup :profile-period-lookup :if
                :accumulated})

(def replacements
  {:this           this
   :previous       previous
   :profile-lookup profile-lookup
   :profile-period profile-period-lookup
   :if             model-if
   :accumulated    accumulated})

(defn- interpret [function options]
  (if (coll? function)
    (let [[operator & operands] function]
      (cond
        (= (count function) 1) (interpret operator options)
        (put-ins2 operator)    (apply (operator replacements) options
                                      (map #(interpret % options) operands))
        (put-ins operator)     (apply (eval operator) options (map #(interpret % options) operands))
        :else                  (apply (eval operator) (map #(interpret % options) operands))))
    function))


(defn- do-row [[key function {default :initial-value}] profile previous-periods new-period]
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

(defn run-model [model profile periods]
  (last (take (inc periods) (iterate (partial next-period model profile) []))))

(comment
  (def demo-model
    '[[:period-number (increment (previous :period-number)) {:initial-value 1}]
      [:starting-aum  (previous :ending-aum) {:initial-value 0}]
      [:drawdown      (product (profile-lookup :commitments)
                               (nth (profile-lookup :contributions)
                                    (this :period-number)
                                    0))]
      [:pnl           (product (this :starting-aum) (profile-lookup :return))]
      [:distribution  (product -1 (this :starting-aum)
                               (profile-period-lookup
                                 :distributions
                                 (this :period-number)))]
      [:ending-aum    (sum (this :drawdown)
                           (this :starting-aum)
                           (this :pnl)
                           (this :distribution))]])

  (def demo-model2 '[[:period-number [:previous :period-number]
                      {:initial-value 1}]])

  (def demo-profile {:commitments   100
                     :contributions [0.25 0.25 0.25 0.25]
                     :return        0.2
                     :distributions [[0 3 0]
                                     [4 10 1/10]]}))

(comment
  (last (take 15 (iterate (partial next-period demo-model2 demo-profile)
                          [])))
  (run-model demo-model demo-profile 5))

