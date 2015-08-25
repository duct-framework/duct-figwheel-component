(ns user
  (:require [clojure.java.io :as io]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [com.stuartsierra.component :as component]
            [reloaded.repl :refer [system init start stop go reset]]
            [ring.component.jetty :as jetty]))

(defroutes app-routes
  (GET "/" [] (io/resource "public/index.html"))
  (route/not-found "<h1>Not Found</h1>"))

(defn new-system []
  (-> (component/system-map
       :app  {:handler app-routes}
       :http (jetty/jetty-server {:port 3000}))
      (component/system-using
       {:http [:app]})))

(reloaded.repl/set-init! #(new-system))
