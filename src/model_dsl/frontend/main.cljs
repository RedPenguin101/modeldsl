(ns model-dsl.frontend.main
  (:require [clojure.edn :as edn]
            [clojure.walk :refer [postwalk]]
            [clojure.string :as str]
            [reagent.core :as r]
            [reagent.dom :as rd]
            [re-frame.core :as rf]
            [goog.i18n.NumberFormat.Format]
            ["codemirror/mode/clojure/clojure"]
            ["react-codemirror2" :refer [UnControlled]]
            [model-dsl.frontend.db]
            [model-dsl.frontend.table-display :refer [tabulate]]
            [model-dsl.domain.core :refer [run-model]])
  (:import (goog.i18n NumberFormat)
           (goog.i18n.NumberFormat Format)))

;; HELPERS

(defn valid-edn? [string]
  (try (edn/read-string string)
       (catch js/Object e false)))

(defn keywordize [form]
  (postwalk #(if (symbol? %) (keyword %) %) form))

(defn symbolize [form]
  (postwalk #(if (keyword? %) (symbol %) %) form))

(defn extract-code [model-rows]
  (reduce (fn [A [name value]]
            (if (map? value)
              (assoc A name (:code value))
              (assoc A name value)))
          {}
          model-rows))

(defn decimal-format [num]
  (let [rounded (Math/round num)]
    (if (zero? rounded)
      "-"
      (.format (NumberFormat. Format/DECIMAL) (str rounded)))))

(defn stringify-measure-name [measure-name]
  (str/join " " (map str/capitalize (str/split (name measure-name) #"-"))))

(defn keywordify-measure-name [measure-name]
  (keyword (str/lower-case (str/replace measure-name #" " "-"))))

;; EVENTS

(rf/reg-event-db
  :update-current-model-row
  (fn [db [_ {:keys [name code name-in-model]}]]
    (let [code (or code (get-in db [:model-rows name :string-rep]))]
      (assoc db :current-model-row
             {:name name :code code :name-in-model name-in-model}))))

(rf/reg-event-db
  :update-model-row
  (fn [db [_ {:keys [name code string-rep]}]]
    (assoc-in db [:model-rows name] {:code       code
                                     :string-rep string-rep})))

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

(defn codemirror-model []
  (fn []
    (let [{:keys [name code]} @(rf/subscribe [:current-model-row-updated])]
      [:div {:style {:border        (if (valid-edn? code)
                                      "1px solid #00d1b2"
                                      "1px solid red")
                     :margin-top    10
                     :border-radius 5
                     :padding       10
                     :box-shadow    (when (not (valid-edn? code))
                                      "0px 0px 5px red")}}
       [:> UnControlled
        {:value     code
         :options   {:mode "clojure"}
         :on-change (fn [_ _ v] (rf/dispatch [:update-current-model-row
                                              {:name name
                                               :code v}]))}]])))

(defn model-component []
  (let [local (r/atom nil)]
    (fn []
      (let [row-order         @(rf/subscribe [:model-row-order])
            current-selection @(rf/subscribe [:current-model-row-updated])]
        [:div
         [codemirror-model]
         [:div.container {:style {:margin-top 10}}
          [:button.button.is-primary
           {:style    {:margin-right 20}
            :on-click (fn [e]
                        (.preventDefault e)
                        (when (valid-edn? (:code current-selection))
                          (rf/dispatch
                            [:update-model-row
                             {:name       (:name current-selection)
                              :code       (keywordize
                                            (edn/read-string (:code current-selection)))
                              :string-rep (:code current-selection)}])))}
           (if ((set row-order) (:name current-selection))
             "Update"
             "Add")]
          [:div.dropdown (when (:dropdown-active @local) {:class :is-active})
           [:div.dropdown-trigger {:on-click #(swap! local update :dropdown-active not)}
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
                 (name measure-name)])]]]]]]))))

(defn codemirror-profile [profile-atom]
  (let [local (r/atom @profile-atom)]
    (fn []
      [:div
       [:div {:style {:border        (if (valid-edn? @local)
                                       "1px solid #00d1b2"
                                       "1px solid red")
                      :margin-top    10
                      :margin-bottom 10
                      :border-radius 5
                      :padding       10
                      :box-shadow    (when (not (valid-edn? @local))
                                       "0px 0px 5px red")}}
        [:> UnControlled
         {:value     @local
          :options   {:mode "clojure"}
          :on-change (fn [_ _ v] (reset! local v))}]]
       [:button.button.is-primary
        {:on-click #(do (.preventDefault %)
                        (when (valid-edn? @local)
                          (rf/dispatch [:update-profile @local])))}
        (if (valid-edn? @local)
          "Update"
          "Invalid EDN")]])))

(defn try-model [model profile periods]
  (try (run-model model profile periods)
       (catch js/Object e false)))

(defn output-component []
  (let [profile    (edn/read-string @(rf/subscribe [:profile-updated]))
        model-rows @(rf/subscribe [:model])
        model-rows (extract-code model-rows)]
    (if-let [model (try-model model-rows
                              profile
                              10)]
      (let [data (tabulate model-rows
                           model)]
        [:div.table-container
         [:table.table.is-narrow.is-striped.is-hoverable
          [:thead
           [:tr {:style {:white-space :nowrap}}
            (for [h (first data)] [:th h])]]
          [:tbody
           (for [row (rest data)]
             [:tr
              (let [measure-name (first row)]
                [:td {:style {:white-space :nowrap}
                      :on-click 
                      #(rf/dispatch [:update-current-model-row
                                     {:name (keyword measure-name)}])}
                 (format-measure-name measure-name)])
              (for [v (rest row)]
                (if (number? v)
                  [:td {:style {:text-align :right}}
                   (decimal-format v)]
                  [:td v]))])]]])
      [:p "invalid model"])))

(defn app []
  [:div.container
   [:div.container {:style {:margin-bottom 20}}
    [:h1.title.is-1 "Catwalk"]]
   #_[:div.dev {:style {:border    "1px solid red"
                        :font-size "0.8em"}}
      (pr-str @(rf/subscribe [:all]))]
   [:div#input.columns
    [:div#profile.column
     [:h4.title.is-4 "Profile"]
     [codemirror-profile (rf/subscribe [:profile-updated])]]
    [:div#model.column
     [:h4.title.is-4 "Model"]
     [model-component]]]
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
  #_(rf/dispatch-sync [:initialize-db])
  (mount))
