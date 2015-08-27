(ns user
  (:require [clojure.java.io :as io]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [com.stuartsierra.component :as component]
            [duct.component.figwheel :as figwheel]
            [reloaded.repl :refer [system init start stop go reset]]
            [ring.component.jetty :as jetty]))

(defroutes app-routes
  (GET "/" [] (io/resource "public/index.html"))
  (route/resources "/")
  (route/not-found "<h1>Not Found</h1>"))

(def figwheel-config
  {:css-dirs ["dev/resources"]
   :builds   [{:source-paths ["dev/src/cljs"]
               :build-options {:output-to "target/js/public/main.js"
                               :output-dir "target/js/public"
                               :optimizations :none}}]})

(defn new-system []
  (-> (component/system-map
       :app      {:handler app-routes}
       :http     (jetty/jetty-server {:port 3000})
       :figwheel (figwheel/server figwheel-config))
      (component/system-using
       {:http [:app]})))

(reloaded.repl/set-init! #(new-system))
