(ns model-dsl.frontend.events
  (:require [re-frame.core :as rf]))

;; EVENTS

(rf/reg-event-db
 :update-selected-measure
 (fn [db [_ {:keys [name code name-in-model]}]]
   (let [code (or code (get-in db [:model-rows name :string-rep]))]
     (println "update-selected-measure fired with" name code)
     (assoc db :current-model-row
            {:name name :code code :name-in-model name-in-model}))))

(rf/reg-event-db
 :create-new-measure
 (fn [db [_ name]]
   (-> db
       (assoc-in [:model-rows name] {:code nil :string-rep ""})
       (update :row-order conj name))))

(rf/reg-event-db
 :update-measure
 (fn [db [_ {:keys [name code string-rep]}]]
   (assoc-in db [:model-rows name] {:code       code
                                    :string-rep string-rep})))

(rf/reg-event-db
 :remove-measure
 (fn [db [_ name]]
   (update db :row-order (fn [measures] (into [] (remove #(= % name) measures))))))

(rf/reg-event-db
 :change-measure-order
 (fn [db [_ new-order]]
   (println "change-measure-order fired with" new-order)
   (assoc db :row-order (vec new-order))))

(rf/reg-event-db
 :update-profile
 (fn [db [_ profile]]
   (assoc db :profile profile)))

;; SUBS

(rf/reg-sub :all (fn [db _] db))

(rf/reg-sub
 :measures
 (fn [db _]
   (:model-rows db)))

(rf/reg-sub
 :selected-measure
 (fn [db _]
   (:current-model-row db)))

(rf/reg-sub
 :measure-order
 (fn [db _]
   (:row-order db)))

(rf/reg-sub
 :profile
 (fn [db _]
   (:profile db)))