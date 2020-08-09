(ns model-dsl.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [model-dsl.domain.core :refer [run-model]]
            [clojure.edn :as edn]))

(reg-event-fx
  :set-profile
  (fn [{:keys [db]} [_ new-profile]]
    {:db       (assoc db :profile new-profile)
     :dispatch [:recalc-output]}))

(reg-event-fx
  :set-model
  (fn [{:keys [db]} [_ new-model]]
    {:db       (assoc db :model new-model)
     :dispatch [:recalc-output]}))

(defn recalc-output [model profile]
  (try (str (run-model (edn/read-string model) (edn/read-string profile)
                       10))
       (catch js/Object e (.log js/console (str "not valid edn" e)))))

(reg-event-db
  :recalc-output
  (fn [db [_ _]]
    (assoc db :output (recalc-output (:model db) (:profile db)))))
