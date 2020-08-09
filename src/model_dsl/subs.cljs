(ns model-dsl.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  :current-model
  (fn [db _]
    (get db :model)))

(reg-sub
  :current-profile
  (fn [db _]
    (get db :profile)))

