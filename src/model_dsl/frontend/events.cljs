(ns model-dsl.frontend.events
  (:require [re-frame.core :as rf]
            [model-dsl.frontend.dummy-backend :as backend]))

;; EVENTS

(rf/reg-event-db
 :initialize-db
 (fn [_ _]
   (let [entities (backend/get-saved-entities)]
     (println {:available-entities (backend/get-saved-entities)})
     (merge {:available-entities entities}
            (backend/get-state (:current-active entities))))))

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
   (assoc (assoc-in db [:model name] {:code code :string-rep string-rep})
          :model-state-changed true)))

(rf/reg-event-db
 :remove-measure
 (fn [db [_ name]]
   (update db :measure-order (fn [measures] (into [] (remove #(= % name) measures))))))

(rf/reg-event-db
 :change-measure-order
 (fn [db [_ new-order]]
   (println "change-measure-order fired with" new-order)
   (assoc db 
          :measure-order (vec new-order)
          :model-state-changed true)))

(rf/reg-event-db
 :update-profile
 (fn [db [_ profile]]
   (println "update-profile fired with" profile)
   (assoc db :profile profile)))

(rf/reg-event-db
 :flag-profile-state-change
 (fn [db [_ _]]
   (assoc db :profile-state-changed true)))

(rf/reg-event-db
 :set-current-entity
 (fn [db [_ id]]
   (assoc-in db [:available-entities :current-active] id)))

(rf/reg-event-db
 :load-entity
 (fn [db [_ id]]
   (let [{:keys [profile model measure-order selected-measure]} (backend/get-state id)]
     (assoc db
            :profile profile
            :model model
            :measure-order measure-order
            :selected-measure selected-measure
            :model-state-changed false
            :profile-state-changed false))))

(rf/reg-event-db
 :save-entity
 (fn [db [_ id]]
   (backend/update-state id (select-keys db [:profile :model :measure-order :selected-measure]))
   (assoc db 
          :model-state-changed false
          :profile-state-changed false)))

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

(rf/reg-sub
 :loaded-entity
 (fn [db _]
   (get-in db [:available-entities :current-active])))

(rf/reg-sub
 :state-changes
 (fn [db _]
   (println (select-keys db [:model-state-changed :profile-state-changed]))
   (select-keys db [:model-state-changed :profile-state-changed])))