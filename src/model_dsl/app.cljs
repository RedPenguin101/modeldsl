(ns ^:figwheel-hooks model-dsl.app
  (:require
   [reagent.dom :as r.dom]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [model-dsl.db]
   [model-dsl.subs]
   [model-dsl.events]
   [clojure.edn :as edn]))


(defn profile-component [profile]
  [:textarea {:value     (str profile)
              :on-change #(rf/dispatch [:set-profile (.. % -target -value)])}])

(defn model-component [model]
  [:textarea {:value     (pr-str model)
              :read-only true}])

(defn output-component [output]
  [:textarea {:value     (pr-str output)
              :read-only true}])

(defn app []
  (let [current-profile @(rf/subscribe [:current-profile])
        current-model   @(rf/subscribe [:current-model])
        current-output  @(rf/subscribe [:current-output])]
    [:<>
     [:h1.site__title
      [:span.site__title-text "Modelang"]]
     [:div.container
      [:div [profile-component current-profile]]
      [:div [model-component current-model]]]
     [:div [output-component current-output]]]))

(defn mount []
  (r.dom/render [app]
                (js/document.getElementById "app")))

(defn ^:after-load re-render []
  (mount))

(defonce start-up
  (do
    (rf/dispatch-sync [:initialize-db])
    (mount)
    true))
