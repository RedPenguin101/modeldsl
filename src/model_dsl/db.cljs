(ns model-dsl.db
  (:require [re-frame.core :as rf]))

(def initial-db {:profile     "{:model-name \"test model\"}"
                 :model       "[[:period-number 5]]"
                 :output-size 1
                 :output      [{:period-number 1}]})

(rf/reg-event-db
  :initialize-db
  (fn [_ _]
    initial-db))
