(ns ^:figwheel-hooks model-dsl.app
  (:require
   [reagent.dom :as r.dom]
   [re-frame.core :as rf]
   [model-dsl.db]
   [model-dsl.subs]
   [clojure.edn :as edn]))

(defn profile-component [profile]
  [:textarea (pr-str profile)])

(defn model-component [model]
  [:textarea (pr-str model)])

(defn output-component [output]
  [:textarea (pr-str output)])

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
