(ns model-dsl.frontend.main
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            [re-frame.core :as rf]
            [clojure.edn :as edn]
            [model-dsl.frontend.db]
            [model-dsl.frontend.table-display :refer [tabulate]]
            [model-dsl.domain.core :refer [run-model]]))

(defonce state
  (r/atom
    {:profile           {:model-name    "Fund 5"
                         :commitments   1000000
                         :contributions [0 0.25 0.25 0.25 0.25]}
     :model-rows
     {:period-number '(:increment (:previous :period-number))
      :starting-aum  '(:previous :ending-aum)
      :drawdowns     '(:product (:profile-lookup :commitments)
                                (:nth (:profile-lookup :contributions)
                                      (:this :period-number) 0))
      :pnl           '(:product (:this :starting-aum) 0.05)
      :ending-aum    '(:sum (:this :starting-aum) (:this :drawdowns) (:this :pnl))}
     :row-order         [:period-number :starting-aum :drawdowns :pnl :ending-aum]
     :periods-to-model  10
     :current-model-row {:name :period-number
                         :code "(:increment (:previous :period-number))"}}))

(defn valid-edn? [string]
  (try (edn/read-string string)
       (catch js/Object e false)))

;; EVENTS

(rf/reg-event-fx
  :update-current-model-row
  (fn [{:keys [db]} [_ {:keys [name code]}]]
    (let [code (or code (pr-str (get-in db [:model-rows name])))]
      {:db (assoc db :current-model-row
                  {:name name :code code})})))

(rf/reg-event-fx
  :update-model-row
  (fn [{:keys [db _]} [_ {:keys [name code]}]]
    {:db (assoc-in db [:model-rows name] code)}))

;; SUBS

(rf/reg-sub
  :all
  (fn [db _]
    db))

(rf/reg-sub
  :current-model-row-updated
  (fn [db _]
    (:current-model-row db)))

(rf/reg-sub
  :model-row-order
  (fn [db _]
    (:row-order db)))

;; COMPONENTS

(defn model-component []
  (fn []
    (let [model-row @(rf/subscribe [:current-model-row-updated])]
      [:form {:on-submit #(.preventDefault %)}
       #_[:div.dev {:style {:border    "1px solid red"
                            :font-size "0.8em"}}
          (pr-str current-model-row)]
       [:div
        {:style {:margin-bottom 20}}
        [:label "Name"
         [:input {:name      "name"
                  :value     (:name model-row)
                  :on-change #(rf/dispatch
                                [:update-current-model-row
                                 {:name (keyword (-> % .-target .-value))}])}]]]
       [:div [:label "Code"
              [:textarea {:name      "code"
                          :value     (:code model-row)
                          :on-change #(rf/dispatch
                                        [:update-current-model-row
                                         {:name (:name model-row)
                                          :code (-> % .-target .-value)}])
                          :style     {:width 400
                                      :background-color
                                      (if (valid-edn? (:code model-row))
                                        :white
                                        :red)}}]]]
       [:button {:on-click (fn [e]
                             (.preventDefault e)
                             (when (valid-edn? (:code model-row))
                               (rf/dispatch
                                 [:update-model-row
                                  {:name (:name model-row)
                                   :code (edn/read-string (:code model-row))}])))}
        "Add"]])))

(defn model-display [current-edit]
  (let [rows      @(rf/subscribe [:model-row-order])
        selection (r/atom current-edit)]
    (fn []
      @selection
      [:div#modeldisplay
       (for [measure rows]
         [:p {:style    {:margin 0}
              :on-click #(do (reset! selection measure)
                             (rf/dispatch [:update-current-model-row
                                           {:name measure}]))}
          (when (= @selection measure)
            [:span ">"])
          (name measure)
          (when (= @selection measure)
            [:span " ^"])])])))

(defn profile-component [profile]
  (let [local (r/atom profile)]
    (fn [_]
      [:textarea
       {:style     {:width            400
                    :height           150
                    :background-color (if (valid-edn? @local)
                                        :white
                                        :red)}
        :value     @local
        :on-change (fn [e]
                     (reset! local (-> e .-target .-value))
                     (when (valid-edn? @local)
                       (swap! state assoc :profile (edn/read-string @local))))}])))

(defn try-model [model profile periods]
  (try (run-model model profile periods)
       (catch js/Object e false)))

(defn output-component []
  (if-let [model (try-model (:model-rows @state)
                            (:profile @state)
                            (:periods-to-model @state))]
    (let [data (tabulate (:model-rows @state)
                         model)]
      [:table
       [:tr {:style {:background-color :gray}}
        (for [h (first data)] [:th h])]
       (for [row (rest data)]
         [:tr {:style {:background-color :gainsboro}}
          (for [v row]
            [:td v])])])
    [:p "invalid model"]))

(defn app []
  [:div#content
   [:h1 "Catwalk"]
   [:div.dev {:style {:border    "1px solid red"
                      :font-size "0.8em"}}
    (pr-str @(rf/subscribe [:all]))]
   [:div#input {:style {:display :flex}}
    [:div#profile {:style {:margin-right 50}}
     [:h3 "Profile"]
     [profile-component (pr-str (:profile @state))]]
    [:div#model
     [:h3 "Model"]
     [model-component]
     [model-display :period-number]]]
   [:div#output
    [:h3 "Output"]
    [output-component]]]
  )

(defn mount []
  (rd/render [app] (.getElementById js/document "app")))

(defn main []
  (rf/dispatch-sync [:initialize-db])
  (mount))

(defn reload []
  (rf/dispatch-sync [:initialize-db])
  (mount))
