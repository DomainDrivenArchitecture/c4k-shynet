(ns dda.c4k-shynet.core
 (:require
  [clojure.string :as cs]
  [clojure.spec.alpha :as s]
  #?(:clj [orchestra.core :refer [defn-spec]]
     :cljs [orchestra.core :refer-macros [defn-spec]])
  [dda.c4k-common.common :as cm]
  [dda.c4k-common.yaml :as yaml]
  [dda.c4k-common.postgres :as postgres]
  [dda.c4k-shynet.shynet :as shynet]))

(def config-defaults {:issuer :staging})

(def config? (s/keys :req-un [::shynet/fqdn]
                     :opt-un [::shynet/issuer]))

(def auth? (s/keys :req-un [::shynet/django-secret-key
                            ::postgres/postgres-db-user ::postgres/postgres-db-password]))

(defn k8s-objects [config]
  (let [storage-class (if (contains? config :postgres-data-volume-path) :manual :local-path)]
  (cm/concat-vec
   [(yaml/to-string (postgres/generate-config {:postgres-size :2gb :db-name "shynet"}))
    (yaml/to-string (postgres/generate-secret config))]
   (when (contains? config :postgres-data-volume-path)
     [(yaml/to-string (postgres/generate-persistent-volume (select-keys config [:postgres-data-volume-path :pv-storage-size-gb])))])
   [(yaml/to-string (postgres/generate-pvc {:pv-storage-size-gb 20
                                            :pvc-storage-class-name storage-class}))
    (yaml/to-string (postgres/generate-deployment {:postgres-image "postgres:14"
                                                   :postgres-size :2gb}))
    (yaml/to-string (postgres/generate-service))
    (yaml/to-string (shynet/generate-secret config))
    (yaml/to-string (shynet/generate-webserver-deployment))
    (yaml/to-string (shynet/generate-celeryworker-deployment))
    (yaml/to-string (shynet/generate-ingress config))
    (yaml/to-string (shynet/generate-certificate config))
    (yaml/to-string (shynet/generate-service-redis))
    (yaml/to-string (shynet/generate-service-webserver))
    (yaml/to-string (shynet/generate-statefulset))])))

(defn-spec generate any?
  [my-config config?
   my-auth auth?]
  (let [resulting-config (merge config-defaults my-config my-auth)]
    (cs/join
     "\n---\n"
     (k8s-objects resulting-config))))
