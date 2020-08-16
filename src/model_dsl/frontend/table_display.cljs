(ns model-dsl.frontend.table-display)

(defn- row-order [model]
  (map first model))

(defn- to-vector [headers period]
  (mapv period headers))

(defn- invert [periods]
  (apply map vector periods))

(defn tabulate [model data]
  (invert
    (concat [(map name (row-order model))]
            (map (partial to-vector (row-order model)) data))))
