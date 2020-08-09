(ns ^:figwheel-hooks model-dsl.app
  (:require
   [reagent.dom :as r.dom]))

(def model (atom []))
(def profile (atom {}))
(def output (atom {}))

(defn app []
  [:<>
   [:h1.site__title
    [:span.site__title-text "Modelang"]]
   [:div.container
    [:div [:textarea "profile"]]
    [:div [:textarea "model"]]]
   [:div [:textarea "output"]]])

(defn mount []
  (r.dom/render [app] (js/document.getElementById "app")))

(defn ^:after-load re-render []
  (mount))

(defonce start-up (do (mount) true))
