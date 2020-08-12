(ns model-dsl.main
  (:require [reagent.core :as r]
            [reagent.dom :as rd]))

(defonce state (atom {:profile {:model-name "my model"}
                      :model   [:period-number [:increment [:previous :period-number]]]
                      :output  [["period-number" 1 2 3 4 5]]}))

(defn key-value [key value]
  [:div {:class :kv-pair}
   [:input {:type  "text"
            :value key
            :key   key}]
   [:input {:type  "text"
            :value value
            :key   value}]])

(defn kv-list [map]
  (vec (cons :div
             (concat (for [[key value] map]
                       (key-value key value))
                     [(key-value "" "")]))))

(println (kv-list (:profile @state)))

(comment
  ([:div
    [:input {:type text, :value :model-name, :key :model-name}]
    [:input {:type text, :value my model, :key my model}]]
   :div
   [:input {:type text, :value :key }]
   [:input {:type text, :value :key }])

  ([[:input {:type text, :value :model-name, :key :model-name}]
    [:input {:type text, :value my model, :key my model}]]
   [:input {:type text, :value :key }]
   [:input {:type text, :value :key }])

  (:div [:div {:class :kv-pair}
         [:input {:type text, :value :model-name, :key :model-name}]
         [:input {:type text, :value my model, :key my model}]]
        [:div {:class :kv-pair}
         [:input {:type text, :value :key }]
         [:input {:type text, :value :key }]]))

(defn app []
  [:div
   [:div
    [:h1 "Modelang"]]
   [:div {:id    "inputs"
          :style {:display         :flex
                  :justify-content :space-around}}
    (kv-list (:profile @state))
    [:p "model goes here"]]
   [:div {:id    "output"
          :style {:display         :flex
                  :justify-content :center}}
    [:p "output goes here"]]])

(defn mount []
  (rd/render [app] (.getElementById js/document "app")))

(defn main []
  (mount))

(defn reload []
  (mount))
