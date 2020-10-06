(ns model-dsl.frontend.dummy-backend)

(defonce backend-db (atom {}))

(defn update-state [id state]
  (swap! backend-db assoc id state))

(defn get-state [id]
  (id @backend-db))

(comment
  backend-db

  (update-state :bosh {:hello 1 :world 2}))