(ns model-dsl.api
  (:require [org.httpkit.server :as http]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [clojure.edn :as edn]
            [model-dsl.domain.core :refer :all]))

(def demo-model
  '[[:period-number (increment (previous :period-number)) {:initial-value 1}]
    [:starting-aum  (previous :ending-aum) {:initial-value 0}]
    [:drawdown      (product (profile-lookup :commitments)
                             (nth (profile-lookup :contributions)
                                  (this :period-number)
                                  0))]
    [:pnl           (product (this :starting-aum) (profile-lookup :return))]
    [:distribution  (product -1 (this :starting-aum)
                             (profile-period-lookup
                               :distributions
                               (this :period-number)))]
    [:ending-aum    (sum (this :drawdown)
                         (this :starting-aum)
                         (this :pnl)
                         (this :distribution))]])

(def demo-model
  '[[:period-number (increment (previous :period-number)) {:initial-value 1}]])

(def demo-profile {:commitments   100
                   :contributions [0.25 0.25 0.25 0.25]
                   :return        0.2
                   :distributions [[0 3 0]
                                   [4 10 1/10]]})

(run-model demo-model demo-profile 5)

(defn bleh []
  (pr-str (run-model demo-model demo-profile 5)))

(bleh)

(defn- handler [req]
  {:status  200
   :headers {"Content-Type" "text"}
   :body    (pr-str  (+ 1 2))
   })

(defroutes api-routes
  (GET "/" [] handler))

(defn run-server [config]
  (http/run-server
    (wrap-defaults #'api-routes api-defaults)
    {:port 3000}))

(comment
  (def server (run-server {}))

  (server :time-out 100))
