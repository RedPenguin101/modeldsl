(ns model-dsl.events
  (:require [re-frame.core :refer [reg-event-db]]))

(reg-event-db
  :set-profile
  (fn [db [_ new-profile]]
    (assoc db :profile new-profile)))

(reg-event-db
  :set-model
  (fn [db [_ new-model]]
    (assoc db :model new-model)))
