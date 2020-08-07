(ns model-dsl.view
  (:require [clojure.inspector :refer [inspect-table]]))

(defn- row-order [model]
  (map first model))

(defn- to-vector [headers period]
  (mapv period headers))

(defn- invert [periods]
  (apply map vector periods))

(defn view-scenario [model data]
  (inspect-table
    (invert
      (concat [(map name (row-order model))]
              (map (partial to-vector (row-order model)) data)))))
