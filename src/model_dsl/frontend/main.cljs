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

(defn- valid-edn? [string]
  (try (edn/read-string string)
       (catch js/Object e false)))

(defn- keywordize [form]
  (postwalk #(if (symbol? %) (keyword %) %) form))

(defn- extract-code [model]
  (reduce (fn [A [name value]]
            (if (map? value)
              (assoc A name (:code value))
              (assoc A name value)))
          {}
          model))

(defn- decimal-format [num]
  (let [rounded (Math/round num)]
    (if (zero? rounded)
      "-"
      (.format (NumberFormat. Format/DECIMAL) (str rounded)))))

(defn- stringify-measure-name [measure-name]
  (str/join " " (map str/capitalize (str/split (name measure-name) #"-"))))

(defn- keywordify-measure-name [measure-name]
  (keyword (str/lower-case (str/replace measure-name #" " "-"))))

(defn- vec-reorder [order before item]
  (let [xs (remove #{item} order)
        [head tail] (split-with #(not= % before) xs)]
    (concat head [item] tail)))

(defn- try-model [model profile periods]
  (try (run-model model profile periods)
       (catch js/Object e false)))

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

;; COMPONENTS

(defn new-row-modal [modal-active?]
  (let [new-row-name (r/atom nil)]
    (fn [modal-active?]
      [:div {:class [:modal (when @modal-active? :is-active)]}
       [:div.modal-background]
       [:div.modal-content
        [:div.box
         [:h3.title.is_h3 "Enter new model row name"]
         [:form {:on-submit #(do (.preventDefault %)
                                 (rf/dispatch [:create-new-measure (keywordify-measure-name @new-row-name)])
                                 (rf/dispatch [:update-selected-measure {:name (keywordify-measure-name @new-row-name)}])
                                 (reset! modal-active? false)
                                 (reset! new-row-name nil))}
          [:div.field
           [:input {:placeholder "Enter new model row name"
                    :on-change #(reset! new-row-name (-> % .-target .-value))
                    :value @new-row-name}]]
          [:button.button.is-primary "save"]]]]
       [:button.modal-close.is-large
        {:on-click #(do (reset! modal-active? false)
                        (reset! new-row-name nil))}
        "x"]])))

(defn measure-dropdown []
  (let [s (r/atom {})
        modal-active? (r/atom false)]
    (fn []
      (let [measures         @(rf/subscribe [:measure-order])
            selected-measure @(rf/subscribe [:selected-measure])]
        [:div
         #_[:div.dev {:style {:border "1px solid red" :text "0.8em"}} @s]
         [:div.dropdown {:class (when (:dropdown-active @s) :is-active)}
          [:div.dropdown-trigger {:on-click #(swap! s update :dropdown-active not)}
           [:button.button {:style {:width 300 :justify-content :space-between }
                            :aria-haspopup "true"
                            :aria-controls "dropdown-menu"}
            [:span (:name selected-measure)]
            [:span.icon.is-small {:aria-hidden true} [:i.fas.fa-angle-down]]]
           [:div#dropdown-menu.dropdown-menu {:role :menu}
            [:div.dropdown-content
             (for [measure measures]
               [:a.dropdown-item
                {:class (when (= measure (:name selected-measure)) :is-active)
                 :on-click #(rf/dispatch [:update-selected-measure {:name measure}])
                 :style {:border-top (when (= measure (:drag-over @s)) "1px solid blue")
                         :width 300
                         :display :flex
                         :justify-content :space-between
                         :padding-right "1em"}
                 :draggable true
                 :on-drag-start #(swap! s assoc :drag-item measure)
                 :on-drag-end (fn [_]
                                (rf/dispatch [:change-measure-order
                                              (vec-reorder measures (:drag-over @s) (:drag-item @s))])
                                (swap! s dissoc :drag-item :drag-over))
                 :on-drag-over (fn [e]
                                 (.preventDefault e)
                                 (swap! s assoc :drag-over measure))
                 :on-drag-leave #(swap! s assoc :drag-over :nothing)}
                [:span (name measure)]
                [:span.icon.is-small
                 {:on-click #(do (rf/dispatch [:remove-measure measure])
                                 (rf/dispatch [:update-selected-measure {:name (first measures)}])
                                 (swap! s update :dropdown-active not)
                                 (.stopPropagation %))}
                 [:i.fas.fa-backspace]]])
             [:a.dropdown-item
              {:style {:opacity 0.5
                       :border-top (when (= :nothing (:drag-over @s)) "1px solid blue")}
               :on-click #(reset! modal-active? true)}
              [:p "Add new row"]]]]]]
         [new-row-modal modal-active?]]))))


(defn codemirror-model []
  (fn []
    (let [{:keys [name code]} @(rf/subscribe [:selected-measure])]
      [:div {:style {:border        (if (valid-edn? code)
                                      "1px solid #00d1b2"
                                      "1px solid red")
                     :margin-top    10
                     :border-radius 5
                     :padding       10
                     :box-shadow    (when (not (valid-edn? code))
                                      "0px 0px 5px red")}}
       [measure-dropdown]
       [:> UnControlled
        {:value     code
         :options   {:mode "clojure"}
         :on-change (fn [_ _ v] (rf/dispatch [:update-selected-measure
                                              {:name name
                                               :code v}]))}]])))

(defn model-window []
  (let [row-order         @(rf/subscribe [:measure-order])
        current-selection @(rf/subscribe [:selected-measure])]
    [:div
     [codemirror-model]
     [:div.container {:style {:margin-top 10}}
      [:button.button.is-primary
       {:style    {:margin-right 20}
        :on-click (fn [e]
                    (.preventDefault e)
                    (when (valid-edn? (:code current-selection))
                      (rf/dispatch
                       [:update-measure
                        {:name       (:name current-selection)
                         :code       (keywordize
                                      (edn/read-string (:code current-selection)))
                         :string-rep (:code current-selection)}])))}
       (if ((set row-order) (:name current-selection))
         "Update"
         "Add")]]]))

(defn profile-window [profile-atom]
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
                      :height        362
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

(defn output-window []
  (let [profile    (edn/read-string @(rf/subscribe [:profile]))
        row-order @(rf/subscribe [:measure-order])
        code (extract-code @(rf/subscribe [:measures]))]
    (if-let [scenario (try-model (for [measure row-order]
                                   [measure (measure code)])
                              profile
                              10)]
      (let [data (tabulate row-order scenario)]
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
                      #(rf/dispatch [:update-selected-measure
                                     {:name (keyword measure-name)}])}
                 (stringify-measure-name measure-name)])
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
     [profile-window (rf/subscribe [:profile])]]
    [:div#model.column
     [:h4.title.is-4 "Model"]
     [model-window]]]
   [:div#output
    [:h4.title.is-4 "Output"]
    [output-window]]])

(defn mount []
  (rd/render [app] (.getElementById js/document "app")))

(defn main []
  (rf/dispatch-sync [:initialize-db])
  (mount))

(defn reload []
  (mount))
