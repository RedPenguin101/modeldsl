(ns model-dsl.api
  (:require [org.httpkit.server :as http]
            [compojure.core :refer :all]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [clojure.edn :as edn]
            [model-dsl.domain.core :refer [run-model]]))

(defn extract-data-from-request [req]
  (->> req
       :body
       slurp
       edn/read-string))

(defn- handler [_]
  {:status  200
   :headers {"Content-Type" "text"}
   :body    "Model"})

(defn run [{:keys [model profile periods]}]
  (run-model model profile periods))

(defn- handler2 [req]
  {:status  200
   :headers {"Content-Type" "text"}
   :body    (->> req
                 extract-data-from-request
                 run
                 pr-str)})

(defroutes api-routes
  (GET "/" [] handler)
  (POST "/api/model" [] handler2))

(defn run-server [config]
  (http/run-server
    (wrap-defaults #'api-routes api-defaults)
    {:port 3000}))

(comment
  (def server (run-server {}))

  (def demo-model
    '[[:period-number (:increment (:previous :period-number)) {:initial-value 1}]
      [:starting-aum  (:previous :ending-aum) {:initial-value 0}]
      [:drawdown      (:product (:profile-lookup :commitments)
                                (:nth (:profile-lookup :contributions)
                                      (:this :period-number)
                                      0))]
      [:pnl           (:product (:this :starting-aum) (:profile-lookup :return))]
      [:distribution  (:product -1 (:this :starting-aum)
                                (:profile-period-lookup
                                 :distributions
                                 (:this :period-number)))]
      [:ending-aum    (:sum (:this :drawdown)
                            (:this :starting-aum)
                            (:this :pnl)
                            (:this :distribution))]])

  (def demo-profile {:commitments   100
                     :contributions [0.25 0.25 0.25 0.25]
                     :return        0.2
                     :distributions [[0 3 0]
                                     [4 10 1/10]]})

  (require '[org.httpkit.client :as client])

  (edn/read-string (:body @(client/get "http://localhost:3000")))

  (def r (pr-str {:model   demo-model
                  :profile demo-profile
                  :periods 10}))

  (edn/read-string (:body @(client/post "http://localhost:3000/api/model"
                                        {:body r})))

  (server :time-out 100))
