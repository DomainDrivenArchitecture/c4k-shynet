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
                     :opt-un []))

(def auth? (s/keys :req-un []))

(defn k8s-objects [config]
  (into
   []
   (concat 
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
