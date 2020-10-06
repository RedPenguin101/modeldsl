(ns model-dsl.frontend.main
  (:require [clojure.edn :as edn]
            [clojure.walk :refer [postwalk]]
            [clojure.string :as str]
            [reagent.core :as r]
            [reagent.dom :as rd]
            [re-frame.core :as rf]
            [goog.i18n.NumberFormat.Format]
            ["codemirror/mode/clojure/clojure"]
            ["codemirror/addon/edit/closebrackets"]
            ["codemirror/addon/edit/matchbrackets"]
            [cljsjs.codemirror]
            [model-dsl.frontend.db]
            [model-dsl.frontend.events]
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

(defn- extract-model-rows [model]
  (reduce (fn [A [name value]]
            (assoc A name (cons name (:code value))))
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

(defn- try-model [model-rows profile periods]
  (try (run-model model-rows profile periods)
       (catch js/Object e false)))

;; COMPONENTS
 
(defn entity-selector-dropdown []
  (fn []
    (let [entities @(rf/subscribe [:available-entities])]
      [:div.navbar-item.has-dropdown.is-hoverable
       #_[:div.dropdown-trigger {:on-click #(swap! active not)}]
       [:a.navbar-link "Entity"]
       [:div.navbar-dropdown
        (for [[id name] (dissoc entities :current-active)]
          [:a.navbar-item  {:on-click #(do (rf/dispatch [:set-current-entity id])
                                           (rf/dispatch [:load-entity id]))
                            :style {:font-weight (when (= id (:current-active entities)) :bold)}}
           name])]])))

(defn navbar []
  [:navbar {:role "navigation" :aria-label "main navigation"}
   [:div#catwalk-navbar.navbar-menu
    [:div.navbar-start
     [entity-selector-dropdown]]
    [:div.navbar-end
     [:div.navbar-item
      [:div.buttons
       [:a.button.is-link {:on-click #(rf/dispatch [:save-entity @(rf/subscribe [:loaded-entity])])}
        [:strong "Save"]]]]]]])

(defn create-codemirror [elem options]
  (js/CodeMirror.
   elem
   (clj->js options)))

(defn codemirror-model [code name]
  (let [ed (r/atom nil)
        name (r/atom name)]
    (r/create-class
     {:reagent-render (fn [] [:div])

      :component-did-mount
      (fn [component]
        (let [editor (create-codemirror
                      (rd/dom-node component)
                      {:mode "clojure"
                       :matchBrackets true
                       :autoCloseBrackets true
                       :value code})]
          (reset! ed editor)
          (.on editor "change"
               #(do
                  (println name code)
                  (rf/dispatch [:select-measure {:name @name :code (.getValue editor)}])))))

      :component-did-update
      (fn [this _]
        (let [[_ new-code new-name] (r/argv this)]
          (when (not= new-code (.getValue @ed))
            (reset! name new-name)
            (.setValue @ed new-code))))})))

(defn codemirror-profile [profile name]
  (let [ed (r/atom nil)
        name (r/atom name)]
    (r/create-class
     {:reagent-render (fn [] [:div])

      :component-did-mount
      (fn [component]
        (let [editor (create-codemirror
                      (rd/dom-node component)
                      {:mode "clojure"
                       :matchBrackets true
                       :autoCloseBrackets true
                       :value profile})]
          (reset! ed editor)
          (.on editor "change"
               #(do
                  (println name profile)
                  (rf/dispatch [:update-profile (.getValue editor)])))))

      :component-did-update
      (fn [this _]
        (let [[_ new-profile new-name] (r/argv this)]
          (when (not= new-profile (.getValue @ed))
            (reset! name new-name)
            (.setValue @ed new-profile))))})))

(defn new-measure-modal [active?]
  (let [new-measure-name (r/atom nil)]
    (fn [active?]
      [:div {:class [:modal (when @active? :is-active)]}
       [:div.modal-background]
       [:div.modal-content
        [:div.box
         [:h3.title.is_h3 "Enter new Measure Name"]
         [:form {:on-submit #(do (.preventDefault %)
                                 (rf/dispatch [:create-new-measure (keywordify-measure-name @new-measure-name)])
                                 (rf/dispatch [:select-measure {:name (keywordify-measure-name @new-measure-name)}])
                                 (reset! active? false)
                                 (reset! new-measure-name nil))}
          [:div.field
           [:input {:placeholder "Enter new measure name"
                    :on-change #(reset! new-measure-name (-> % .-target .-value))
                    :value @new-measure-name}]]
          [:button.button.is-primary "save"]]]]
       [:button.modal-close.is-large
        {:on-click #(do (reset! active? false)
                        (reset! new-measure-name nil))}]])))

(defn measure-dropdown []
  (let [s (r/atom {:dropdown-active false})
        modal-active? (r/atom false)]
    (fn []
      (let [measures         @(rf/subscribe [:measure-order])
            selected-measure @(rf/subscribe [:selected-measure])]
        [:div
         #_[:div.dev {:style {:border "1px solid red" :text "0.8em"}} @s]
         [:div.dropdown {:class (when (:dropdown-active @s) :is-active)}
          [:div.dropdown-trigger {:on-click #(swap! s update :dropdown-active not)}
           [:button.button {:style {:width 300 :justify-content :space-between}}
            [:span (:name selected-measure)]
            [:span.icon.is-small [:i.fas.fa-angle-down]]]
           [:div#dropdown-menu.dropdown-menu {:role :menu}
            [:div.dropdown-content
             (for [measure measures]
               [:a.dropdown-item
                {:class (when (= measure (:name selected-measure)) :is-active)
                 :on-click #(rf/dispatch [:select-measure {:name measure}])
                 :style {:border-top (when (= measure (:drag-over @s)) "1px solid blue")
                         :width 300
                         :display :flex
                         :justify-content :space-between
                         :padding-right "1em"}
                 :draggable true
                 :on-drag-start #(swap! s assoc :drag-item measure)
                 :on-drag-end #(do (rf/dispatch [:change-measure-order
                                                 (vec-reorder measures (:drag-over @s) (:drag-item @s))])
                                   (swap! s dissoc :drag-item :drag-over))
                 :on-drag-over #(do (.preventDefault %)
                                    (swap! s assoc :drag-over measure))
                 :on-drag-leave #(swap! s assoc :drag-over :nothing)}
                [:span (name measure)]
                [:span.icon.is-small
                 {:on-click #(do (rf/dispatch [:remove-measure measure])
                                 (rf/dispatch [:select-measure {:name (first measures)}])
                                 (swap! s update :dropdown-active not)
                                 (.stopPropagation %))}
                 [:i.fas.fa-backspace]]])
             [:a.dropdown-item
              {:style {:opacity 0.5 :border-top (when (= :nothing (:drag-over @s)) "1px solid blue")}
               :on-click #(reset! modal-active? true)}
              [:p "Add new measure"]]]]]]
         [new-measure-modal modal-active?]]))))

(defn model-input []
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
       #_[:div.dev {:style {:border    "1px solid red" :font-size "0.8em"}} (pr-str [name code])]
       [measure-dropdown]
       [codemirror-model code name]
       [:div.container {:style {:margin-top 10}}
        [:button.button.is-primary
         {:style    {:margin-right 20}
          :on-click (fn [e]
                      (.preventDefault e)
                      (when (valid-edn? code)
                        (rf/dispatch [:update-measure
                                      {:name       name
                                       :code       (keywordize (edn/read-string (str "[" code "]")))
                                       :string-rep code}])))}
         (if (valid-edn? code)
           "Update"
           "Invalid EDN")]]])))

(defn profile-window []
  (fn []
    (let [profile @(rf/subscribe [:profile])]
      [:div
       [:div {:style {:border        (if (valid-edn? profile) "1px solid #00d1b2" "1px solid red")
                      :margin-top    10
                      :margin-bottom 10
                      :border-radius 5
                      :padding       10
                      :box-shadow    (when (not (valid-edn? profile)) "0px 0px 5px red")}}
        [:div {:style {:height 390}} [codemirror-profile profile :dummy]]]])))

(defn output-window []
  (let [profile    @(rf/subscribe [:profile])
        profile  (if (valid-edn? profile) (edn/read-string profile) {})
        measures @(rf/subscribe [:measure-order])
        model-rows (extract-model-rows @(rf/subscribe [:model]))]
    (if-let [scenario (try-model (map model-rows measures)
                                 profile
                                 10)]
      (let [data (tabulate measures scenario)]
        [:div.table-container
         [:table.table.is-narrow.is-striped.is-hoverable
          [:thead
           [:tr {:style {:white-space :nowrap}}
            (for [h (first data)]
              [:th h])]]
          [:tbody
           (for [row (rest data)]
             [:tr
              (let [measure-name (first row)]
                [:td {:style {:white-space :nowrap}
                      :on-click #(rf/dispatch [:select-measure {:name (keyword measure-name)}])}
                 (stringify-measure-name measure-name)])
              (for [v (rest row)]
                (if (number? v)
                  [:td {:style {:text-align :right}} (decimal-format v)]
                  [:td v]))])]]])
      [:p "invalid model"])))

(defn app []
  [:div.container
   [navbar]
   [:div.dev {:style {:border    "1px solid red" :font-size "0.8em"}} (pr-str @(rf/subscribe [:all]))]
   [:section.section
    [:div#input.columns
     [:div#profile.column
      [:h4.title.is-4 "Profile"]
      [profile-window]]
     [:div#model.column
      [:h4.title.is-4 "Model"]
      [model-input]]]]
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
