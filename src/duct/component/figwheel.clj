(ns duct.component.figwheel
  (:require [clojurescript-build.auto :as auto]
            [com.stuartsierra.component :as component]
            [figwheel-sidecar.core :as fig-core]
            [figwheel-sidecar.auto-builder :as fig-auto]))

(defrecord Server [builds]
  component/Lifecycle
  (start [component]
    (if (:server component)
      component
      (let [server  (fig-core/start-server component)
            builder (auto/make-conditional-builder (fig-auto/builder server))
            builds  (mapv auto/prep-build builds)]
        (assoc component :server server, :builder builder, :prepped-builds builds))))
  (stop [component]
    (if-let [server (:server component)]
      (do (fig-core/stop-server server)
          (dissoc component :server :builder :prepped-builds))
      component)))

(defn build [{:keys [builder prepped-builds]}]
  (doseq [build prepped-builds]
    (builder build)))

(defn server [options]
  (map->Server options))
