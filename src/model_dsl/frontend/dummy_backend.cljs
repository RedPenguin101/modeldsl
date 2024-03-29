(ns model-dsl.frontend.dummy-backend)

(defonce backend-db 
  (atom {:abc {:profile-name "Bravo"
               :profile "{:model-name \"Bravo\", \n :commitments 10000, \n :contributions [0 0.25 0.25 0.25 0.25]\n :hello \"world\"\n :happiness 123}", 
               :model '{:period-number {:code [(:increment (:previous :period-number))], :string-rep "(increment (previous period-number))\n"}, :starting-aum {:code [(:previous :ending-aum)], :string-rep "(previous ending-aum)"}, :drawdowns {:code [(:product (:profile-lookup :commitments) (:nth (:profile-lookup :contributions) (:this :period-number) 0))], :string-rep "(product \n (profile-lookup commitments) \n (nth (profile-lookup contributions) \n (this period-number) 0))"}, :pnl {:code [(:product (:this :starting-aum) 0.05)], :string-rep "(product (this starting-aum) \n 0.05)"}, :ending-aum {:code [(:sum (:this :starting-aum) (:this :drawdowns) (:this :pnl))], :string-rep "(sum (this starting-aum) \n (this drawdowns) \n (this pnl))"}}, 
               :measure-order [:period-number :starting-aum :drawdowns :pnl :ending-aum], 
               :selected-measure {:name :period-number, :code "(increment (previous period-number))\n"}}, 
         :def {:profile-name "Delta"
               :profile "{:model-name \"Delta\", \n :commitments 1000000, \n :contributions [0 0.25 0.25 0.25 0.25]\n :hello \"world\"\n :happiness 123}", 
               :model '{:period-number {:code [(:previous :period-number) {:initial-value 1}], :string-rep "(previous period-number)\n{:initial-value 1}"}, :starting-aum {:code [(:previous :ending-aum)], :string-rep "(previous ending-aum)"}, :drawdowns {:code [(:product (:profile-lookup :commitments) (:nth (:profile-lookup :contributions) (:this :period-number) 0))], :string-rep "(product \n (profile-lookup commitments) \n (nth (profile-lookup contributions) \n (this period-number) 0))"}, :pnl {:code [(:product (:this :starting-aum) 0.05)], :string-rep "(product (this starting-aum) \n 0.05)"}, :ending-aum {:code [(:sum (:this :starting-aum) (:this :drawdowns) (:this :pnl))], :string-rep "(sum (this starting-aum) \n (this drawdowns) \n (this pnl))"}}, 
               :measure-order [:period-number :starting-aum :drawdowns :pnl :ending-aum], 
               :selected-measure {:name :period-number, :code "(previous period-number)\n{:initial-value 1}"}}
         :ghi {:profile-name "Bingo"
               :profile "{:model-name \"Bingo\"}", 
               :model '{:period-number {:code nil, :string-rep ""}}, 
               :measure-order [:period-number], 
               :selected-measure {:name :period-number, :code ""}}}))

(defn update-state [id state]
  (swap! backend-db assoc id state))

(defn get-state [id]
  (id @backend-db))

(defn get-saved-entities []
  (assoc (into {} (map (fn [[k {:keys [profile-name]}]] (vector k profile-name)) @backend-db))
         :current-active :abc))

(get-saved-entities)

(comment
  backend-db

  (update-state :bosh {:hello 1 :world 2}))