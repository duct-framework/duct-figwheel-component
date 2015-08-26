(ns duct.component.figwheel
  (:require [clojurescript-build.auto :as auto]
            [com.stuartsierra.component :as component]
            [figwheel-sidecar.core :as fig-core]
            [figwheel-sidecar.auto-builder :as fig-auto]))

(defrecord FigwheelBuild [])

(defmethod clojure.core/print-method FigwheelBuild
  [system ^java.io.Writer writer]
  (.write writer "#<FigwheelBuild>"))

(defrecord FigwheelServer [])

(defmethod clojure.core/print-method FigwheelServer
  [system ^java.io.Writer writer]
  (.write writer "#<FigwheelServer>"))

(defn- start-build [builder build]
  (-> build auto/prep-build builder map->FigwheelBuild))

(defrecord Server [builds]
  component/Lifecycle
  (start [component]
    (if (:server component)
      component
      (let [server  (map->FigwheelServer (fig-core/start-server component))
            builder (auto/make-conditional-builder (fig-auto/builder server))
            state   (atom (mapv (partial start-build builder) builds))]
        (assoc component :server server, :builder builder, :state state))))
  (stop [component]
    (if-let [server (:server component)]
      (do (fig-core/stop-server server)
          (dissoc component :server :builder :state))
      component)))

(defn rebuild [{:keys [state builds builder]}]
  (reset! state (mapv (partial start-build builder) builds)))

(defn build [{:keys [state builder]}]
  (swap! state (partial mapv builder)))

(defn server [options]
  (map->Server options))
