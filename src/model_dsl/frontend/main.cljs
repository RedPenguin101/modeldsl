(ns model-dsl.frontend.main
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            [re-frame.core :as rf]
            [clojure.edn :as edn]
            [clojure.walk :refer [postwalk]]
            [goog.string :as gstring]
            [goog.string.format]
            [model-dsl.frontend.db]
            [model-dsl.frontend.table-display :refer [tabulate]]
            [model-dsl.domain.core :refer [run-model]]))

;; HELPERS

(defn valid-edn? [string]
  (try (edn/read-string string)
       (catch js/Object e false)))

(defn keywordize [form]
  (postwalk #(if (symbol? %) (keyword %) %) form))

(defn symbolize [form]
  (postwalk #(if (keyword? %) (symbol %) %) form))

;; EVENTS

(rf/reg-event-db
  :update-current-model-row
  (fn [db [_ {:keys [name code name-in-model]}]]
    (let [code (or code (pr-str (symbolize (get-in db [:model-rows name]))))]
      (assoc db :current-model-row
             {:name name :code code :name-in-model name-in-model}))))

(rf/reg-event-db
  :update-model-row
  (fn [db [_ {:keys [name code]}]]
    (assoc-in db [:model-rows name] code)))

(rf/reg-event-db
  :update-profile
  (fn [db [_ profile]]
    (assoc db :profile profile)))

;; SUBS

(rf/reg-sub :all (fn [db _] db))

(rf/reg-sub
  :model
  (fn [db _]
    (:model-rows db)))

(rf/reg-sub
  :current-model-row-updated
  (fn [db _]
    (:current-model-row db)))

(rf/reg-sub
  :model-row-order
  (fn [db _]
    (:row-order db)))

(rf/reg-sub
  :profile-updated
  (fn [db _]
    (:profile db)))

;; COMPONENTS

(defn model-component []
  (let [dropdown-active (r/atom nil)]
    (fn []
      (let [row-order         @(rf/subscribe [:model-row-order])
            model-rows        @(rf/subscribe [:model])
            current-selection @(rf/subscribe [:current-model-row-updated])]
        [:div
         [:div.dropdown (when @dropdown-active {:class :is-active})
          [:div.dropdown-trigger {:on-click #(swap! dropdown-active not)}
           [:button.button {:width         "100%"
                            :aria-haspopup "true"
                            :aria-controls "dropdown-menu"}
            [:span (:name current-selection)]
            [:span.icon.is-small {:aria-hidden true} [:i.fas.fa-angle-down]]]
           [:div#dropdown-menu.dropdown-menu {:role :menu}
            [:div.dropdown-content
             (for [measure-name row-order]
               [:a.dropdown-item
                {:class (when (= measure-name (:name current-selection)) :is-active)
                 :on-click
                 #(rf/dispatch [:update-current-model-row
                                {:name          measure-name
                                 :name-in-model true}])}
                (name measure-name)])]]]]
         [:div
          [:textarea.textarea.is-primary
           {:name      "code"
            :value     (:code current-selection)
            :on-change #(rf/dispatch
                          [:update-current-model-row
                           {:name (:name current-selection)
                            :code (-> % .-target .-value)}])
            :style     {:width      400
                        :margin-top 20
                        :background-color
                        (if (valid-edn? (:code current-selection))
                          :white
                          :red)}}]
          [:button.button.is-primary
           {:style    {:margin-top 20}
            :on-click (fn [e]
                        (.preventDefault e)
                        (when (valid-edn? (:code current-selection))
                          (rf/dispatch
                            [:update-model-row
                             {:name (:name current-selection)
                              :code (keywordize
                                      (edn/read-string (:code current-selection)))}])))}
           (if ((set row-order) (:name current-selection))
             "Update"
             "Add")]]]))))

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
                                           {:name          measure
                                            :name-in-model true}]))}
          (when (= @selection measure)
            [:span ">"])
          (name measure)
          (when (= @selection measure)
            [:span " ^"])])])))

(defn profile-component [profile-atom]
  (let [local (r/atom (pr-str @profile-atom))]
    (fn [_]
      [:form {:on-submit #(.preventDefault %)}
       [:textarea.textarea.is-primary
        {:style {:width            400
                 :height           150
                 :margin-bottom    10
                 :background-color (if (valid-edn? @local)
                                     :white
                                     :red)}
         :value @local
         :on-change
         (fn [e]
           (reset! local (-> e .-target .-value))
           (js/console.log @local))}]
       [:button.button.is-primary {:on-click #(do (.preventDefault %)
                                                  (when (valid-edn? @local)
                                                    (rf/dispatch [:update-profile @local])))}
        (if (valid-edn? @local)
          "Update"
          "Invalid EDN")]])))

(defn try-model [model profile periods]
  (try (run-model model profile periods)
       (catch js/Object e false)))

(defn output-component []
  (let [profile    @(rf/subscribe [:profile-updated])
        model-rows @(rf/subscribe [:model])]
    (if-let [model (try-model model-rows
                              profile
                              10)]
      (let [data (tabulate model-rows
                           model)]
        [:div.table-container
         [:table.table.is-narrow.is-striped.is-hoverable
          [:thead
           [:tr
            (for [h (first data)] [:th h])]]
          [:tbody
           (for [row (rest data)]
             [:tr
              (for [v row]
                (if (number? v)
                  [:td {:style {:text-align :right}}
                   (gstring/format "%d" v)]
                  [:td v]))])]]])
      [:p "invalid model"])))

(defn app []
  [:div.container
   [:div.container {:style {:margin-bottom 20}}
    [:h1.title.is-1 "Catwalk"]]
   #_[:div.dev {:style {:border    "1px solid red"
                        :font-size "0.8em"}}
      (pr-str @(rf/subscribe [:all]))]
   [:div#input.level
    [:div#profile.container {:style {:margin-right 50}}
     [:h4.title.is-4 "Profile"]
     [profile-component (rf/subscribe [:profile-updated])]]
    [:div#model.container
     [:h4.title.is-4 "Model"]
     [model-component]
     #_[model-display :period-number]]]
   [:div#output
    [:h4.title.is-4 "Output"]
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

