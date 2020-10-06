(ns model-dsl.frontend.events
  (:require [re-frame.core :as rf]))

;; EVENTS

(rf/reg-event-db
 :select-measure
 (fn [db [_ {:keys [name code]}]]
   (let [code (or code (get-in db [:model name :string-rep]))]
     (println "select-measure fired with" name code)
     (assoc db :selected-measure {:name name :code code}))))

(rf/reg-event-db
 :create-new-measure
 (fn [db [_ name]]
   (-> db
       (assoc-in [:model name] {:code nil :string-rep ""})
       (update :measure-order conj name))))

(rf/reg-event-db
 :update-measure
 (fn [db [_ {:keys [name code string-rep]}]]
   (assoc-in db [:model name] {:code code :string-rep string-rep})))

(rf/reg-event-db
 :remove-measure
 (fn [db [_ name]]
   (update db :measure-order (fn [measures] (into [] (remove #(= % name) measures))))))

(rf/reg-event-db
 :change-measure-order
 (fn [db [_ new-order]]
   (println "change-measure-order fired with" new-order)
   (assoc db :measure-order (vec new-order))))

(rf/reg-event-db
 :update-profile
 (fn [db [_ profile]]
   (assoc db :profile profile)))

(rf/reg-event-db
 :set-current-entity
 (fn [db [_ id]]
   (assoc-in db [:available-entities :current-active] id)))

;; SUBS

(rf/reg-sub :all (fn [db _] db))

(rf/reg-sub
 :model
 (fn [db _]
   (:model db)))

(rf/reg-sub
 :selected-measure
 (fn [db _]
   (:selected-measure db)))

(rf/reg-sub
 :measure-order
 (fn [db _]
   (:measure-order db)))

(rf/reg-sub
 :profile
 (fn [db _]
   (:profile db)))

(rf/reg-sub
 :available-entities
 (fn [db _]
   (:available-entities db)))