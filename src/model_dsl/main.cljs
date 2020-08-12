(ns model-dsl.main
  (:require [reagent.core :as r]
            [reagent.dom :as rd]))

(defn app []
  [:div
   [:h1 "Hello there"]
   [:h2 "General Kenobi"]])

(defn mount []
  (rd/render [app] (.getElementById js/document "app")))

(defn main []
  (mount))

(defn reload []
  (mount))
