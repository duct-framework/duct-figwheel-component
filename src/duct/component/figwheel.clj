(ns duct.component.figwheel
  (:require [clojurescript-build.auto :as auto]
            [com.stuartsierra.component :as component]
            [compojure.core :as compojure :refer [GET]]
            [compojure.route :as route]
            [figwheel-sidecar.core :as fig-core]
            [figwheel-sidecar.auto-builder :as fig-auto]
            [org.httpkit.server :as httpkit]
            [ring.middleware.cors :as cors]))

(defrecord FigwheelBuild [])
(defrecord FigwheelServer [])

(defmethod print-method FigwheelBuild [_ ^java.io.Writer writer]
  (.write writer "#<FigwheelBuild>"))

(defmethod print-method FigwheelServer [_ ^java.io.Writer writer]
  (.write writer "#<FigwheelServer>"))

(defn- figwheel-server [state]
  (-> (compojure/routes
       (GET "/figwheel-ws/:desired-build-id" [] (fig-core/reload-handler state))
       (GET "/figwheel-ws" [] (fig-core/reload-handler state))
       (route/not-found "<h1>Page not found</h1>"))
      (cors/wrap-cors
       :access-control-allow-origin #".*"
       :access-control-allow-methods [:head :options :get :put :post :delete :patch])
      (httpkit/run-server
       {:port      (:server-port state)
        :server-ip (:server-ip state "0.0.0.0")
        :worker-name-prefix "figwh-httpkit-"})))

(defn- start-figwheel-server [opts]
  (let [state  (fig-core/create-initial-state (fig-core/resolve-ring-handler opts))
        server (figwheel-server state)]
    (map->FigwheelServer (assoc state :http-server server))))

(defn- start-build [builder build]
  (-> build auto/prep-build builder map->FigwheelBuild))

(defrecord Server [builds]
  component/Lifecycle
  (start [component]
    (if (:server component)
      component
      (let [server  (start-figwheel-server component)
            builder (auto/make-conditional-builder (fig-auto/builder server))
            state   (atom (mapv (partial start-build builder) builds))]
        (assoc component :server server, :builder builder, :state state))))
  (stop [component]
    (if-let [server (:server component)]
      (do (fig-core/stop-server server)
          (dissoc component :server :builder :state))
      component)))

(defn rebuild-cljs [{:keys [state builds builder]}]
  (reset! state (mapv (partial start-build builder) builds)) nil)

(defn build-cljs [{:keys [state builder]}]
  (swap! state (partial mapv builder)) nil)

(defn refresh-css [{:keys [server]}]
  (fig-core/check-for-css-changes server) nil)

(defn server [options]
  (map->Server options))
