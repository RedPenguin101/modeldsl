(ns model-dsl.frontend.main
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            [clojure.edn :as edn]
            [model-dsl.domain.core :refer [run-model]]))

(defonce state (r/atom {:profile          {:model-name "Fund 5"}
                        :model-rows       {:period-number
                                           '(:increment (:previous :period-number))}
                        :periods-to-model 10}))

(defn- row-order [model]
  (map first model))

(defn- to-vector [headers period]
  (mapv period headers))

(defn- invert [periods]
  (apply map vector periods))

(defn noshows [[name _ & [options]]]
  (if (and options (= :hide (:display options)))
    name
    nil))

(defn valid-edn? [string]
  (try (edn/read-string string)
       (catch js/Object e false)))

(defn model-component []
  (let [s   (r/atom {:name :period-number
                     :code "(:increment (:previous :period-number))"})
        ref (r/atom nil)]
    (fn []
      [:form {:on-submit #(.preventDefault %)
              :ref       #(reset! ref %)}
       #_[:div.dev {:style {:border    "1px solid red"
                            :font-size "0.8em"}}
          (pr-str @s)
          (if (valid-edn? (:code @s))
            (pr-str (edn/read-string (:code @s)))
            "Invalid Edn")]
       [:div
        {:style {:margin-bottom 20}}
        [:label "Name"
         [:input {:name      "name"
                  :value     (name (:name @s))
                  :on-change #(swap! s assoc :name
                                     (keyword (-> % .-target .-value)))}]]]
       [:div [:label "Code"
              [:textarea {:name      "code"
                          :value     (:code @s)
                          :on-change #(swap! s assoc :code (-> % .-target .-value))
                          :style     {:width 400
                                      :background-color
                                      (if (valid-edn? (:code @s))
                                        :white
                                        :red)}}]]]
       [:button {:on-click (fn [e]
                             (.preventDefault e)
                             (when (valid-edn? (:code @s))
                               (let [name (:name @s)
                                     code (edn/read-string (:code @s))]
                                 (swap! state assoc-in [:model-rows name] code))))}
        "Add"]
       [:div [:p "temp"]]])))

(defn profile-component [profile]
  (let [local (r/atom profile)]
    (fn [_]
      [:textarea
       {:style     {:width            400
                    :background-color (if (valid-edn? @local)
                                        :white
                                        :red)}
        :value     @local
        :on-change (fn [e]
                     (reset! local (-> e .-target .-value))
                     (when (valid-edn? @local)
                       (swap! state assoc :profile (edn/read-string @local))))}])))

(defn tabulate [model data]
  (invert
    (concat [(map name (row-order model))]
            (map (partial to-vector (row-order model)) data))))

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
    (pr-str @state)]
   [:div#input {:style {:display :flex}}
    [:div#profile {:style {:margin-right 50}}
     [:h3 "Profile"]
     [profile-component (pr-str (:profile @state))]]
    [:div#model
     [:h3 "Model"]
     [model-component]]]
   [:div#output
    [:h3 "Output"]
    [output-component]]]
  )

(defn mount []
  (rd/render [app] (.getElementById js/document "app")))

(defn main []
  (mount))

(defn reload []
  (mount))
