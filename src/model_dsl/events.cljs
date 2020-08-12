(ns model-dsl.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [model-dsl.domain.core :refer [run-model]]
            [model-dsl.domain.view :refer [tabulate]]
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

(defn valid-edn? [string]
  (try (edn/read-string string)
       (catch js/Object e false)))

(defn recalc-output [model profile]
  (if (and (valid-edn? model) (valid-edn? profile))
    (let [model (edn/read-string model)]
      (pr-str (tabulate model (run-model model (edn/read-string profile) 5))))
    "Invalid EDN"))

(reg-event-db
  :recalc-output
  (fn [db [_ _]]
    (assoc db :output (recalc-output (:model db) (:profile db)))))
