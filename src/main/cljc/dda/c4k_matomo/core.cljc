(ns dda.c4k-matomo.core
 (:require
  [clojure.string :as cs]
  [clojure.spec.alpha :as s]
  #?(:clj [orchestra.core :refer [defn-spec]]
     :cljs [orchestra.core :refer-macros [defn-spec]])
  [dda.c4k-common.yaml :as yaml]
  [dda.c4k-common.postgres :as postgres]
  [dda.c4k-matomo.matomo :as matomo]))

(def config-defaults {:issuer :staging})

(def config? (s/keys :req-un [::matomo/fqdn]
                     :opt-un [::matomo/issuer ::postgres/postgres-data-volume-path]))

(def auth? (s/keys :req-un [::postgres/postgres-db-user ::postgres/postgres-db-password])) ;TODO add auth 

(defn k8s-objects [config]
  (into
   []
   (concat
    [(yaml/to-string (postgres/generate-config :postgres-size :2gb :db-name "shynet"))
     (yaml/to-string (postgres/generate-secret config))]
    (when (contains? config :postgres-data-volume-path)
      [(yaml/to-string (postgres/generate-persistent-volume config))])
    [(yaml/to-string (postgres/generate-pvc))
     (yaml/to-string (postgres/generate-deployment :postgres-image "postgres:14"))
     (yaml/to-string (postgres/generate-service))]
    [(yaml/to-string (matomo/generate-webserver-deployment))
     (yaml/to-string (matomo/generate-celeryworker-deployment))
     (yaml/to-string (matomo/generate-ingress config))
     (yaml/to-string (matomo/generate-certificate config))
     (yaml/to-string (matomo/generate-service-redis))
     (yaml/to-string (matomo/generate-service-webserver))
     (yaml/to-string (matomo/generate-statefulset))])))

(defn-spec generate any?
  [my-config config?
   my-auth auth?]
  (let [resulting-config (merge config-defaults my-config my-auth)]
    (cs/join
     "\n---\n"
     (k8s-objects resulting-config))))
