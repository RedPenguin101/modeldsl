(ns model-dsl.frontend.table-display)

(defn- row-order [model]
  (map first model))

(defn- to-vector [headers period]
  (mapv period headers))

(defn- invert [periods]
  (apply map vector periods))

(defn tabulate [row-order data]
  (invert
    (concat [(map name row-order)]
            (map (partial to-vector row-order) data))))
