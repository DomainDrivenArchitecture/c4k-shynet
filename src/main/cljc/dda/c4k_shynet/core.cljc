(ns dda.c4k-shynet.core
 (:require
  [clojure.string :as cs]
  [clojure.spec.alpha :as s]
  #?(:clj [orchestra.core :refer [defn-spec]]
     :cljs [orchestra.core :refer-macros [defn-spec]])
  [dda.c4k-common.yaml :as yaml]
  [dda.c4k-common.postgres :as postgres]
  [dda.c4k-common.monitoring :as mon]
  [dda.c4k-shynet.shynet :as shynet]))

(def config-defaults {:issuer :staging})

(s/def ::mon-cfg ::mon/mon-cfg)
(s/def ::mon-auth ::mon/mon-auth)

(def config? (s/keys :req-un [::shynet/fqdn]
                     :opt-un [::shynet/issuer
                              ::mon-cfg]))

(def auth? (s/keys :req-un [::shynet/django-secret-key
                            ::postgres/postgres-db-user ::postgres/postgres-db-password
                            ::mon-auth]))

(defn config-objects [config]
  (let [storage-class (if (contains? config :postgres-data-volume-path) :manual :local-path)]
    (map yaml/to-string
         [(postgres/generate-config {:postgres-size :2gb :db-name "shynet"})
          (when (contains? config :postgres-data-volume-path)
            (postgres/generate-persistent-volume (select-keys config [:postgres-data-volume-path :pv-storage-size-gb])))
          (postgres/generate-pvc {:pv-storage-size-gb 20
                                  :pvc-storage-class-name storage-class})
          (postgres/generate-deployment {:postgres-image "postgres:14"
                                         :postgres-size :2gb})
          (postgres/generate-service config)
          (shynet/generate-webserver-deployment)
          (shynet/generate-celeryworker-deployment)
          (shynet/generate-ingress config)
          (shynet/generate-service-redis)
          (shynet/generate-service-webserver)
          (shynet/generate-statefulset)
          (when (:contains? config :mon-cfg)
            (mon/generate (:mon-cfg config) (:mon-auth config)))])))

(defn auth-objects [config]
  (map yaml/to-string
       [(postgres/generate-secret config)
        (shynet/generate-secret config)]))