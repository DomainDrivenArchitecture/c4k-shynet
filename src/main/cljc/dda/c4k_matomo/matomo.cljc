(ns dda.c4k-matomo.matomo
 (:require
  [clojure.spec.alpha :as s]
  #?(:cljs [shadow.resource :as rc])
  [dda.c4k-common.yaml :as yaml]
  [dda.c4k-common.common :as cm]
  [dda.c4k-common.predicate :as pred]))

(s/def ::fqdn pred/fqdn-string?)
(s/def ::issuer pred/letsencrypt-issuer?)

#?(:cljs
   (defmethod yaml/load-resource :matomo [resource-name]
     (case resource-name
       "matomo/certificate.yaml" (rc/inline "matomo/certificate.yaml")
       "matomo/deployments.yaml" (rc/inline "matomo/deployments.yaml")
       "matomo/ingress.yaml" (rc/inline "matomo/ingress.yaml")  
       "matomo/service-redis.yaml" (rc/inline "matomo/service-redis.yaml")
       "matomo/service-webserver.yaml" (rc/inline "matomo/service-webserver.yaml")
       "matomo/statefulset.yaml" (rc/inline "matomo/statefulset.yaml")
       (throw (js/Error. "Undefined Resource!")))))
 
(defn generate-certificate [config]
  (let [{:keys [fqdn issuer]} config
        letsencrypt-issuer (str "letsencrypt-" (name issuer) "-issuer")]
    (->
     (yaml/from-string (yaml/load-resource "matomo/certificate.yaml"))
     (assoc-in [:spec :commonName] fqdn)
     (assoc-in [:spec :dnsNames] [fqdn])
     (assoc-in [:spec :issuerRef :name] letsencrypt-issuer))))

(defn generate-webserver-deployment []
  (let [shynet-application "shynet-webserver"]
    (-> (yaml/from-string (yaml/load-resource "matomo/deployments.yaml"))
        (cm/replace-all-matching-values-by-new-value "shynet-application" shynet-application)
        (update-in [:spec :template :spec :containers 0] dissoc :command))))

(defn generate-celeryworker-deployment []
  (let [shynet-application "shynet-celeryworker"]
    (-> (yaml/from-string (yaml/load-resource "matomo/deployments.yaml"))
        (cm/replace-all-matching-values-by-new-value "shynet-application" shynet-application))))

(defn generate-ingress [config]
  (let [{:keys [fqdn issuer]
         :or {issuer :staging}} config
        letsencrypt-issuer (str "letsencrypt-" (name issuer) "-issuer")]
    (->
     (yaml/from-string (yaml/load-resource "matomo/ingress.yaml"))
     (assoc-in [:metadata :annotations :cert-manager.io/cluster-issuer] letsencrypt-issuer)
     (cm/replace-all-matching-values-by-new-value "fqdn" fqdn))))

(defn generate-statefulset []
  (yaml/from-string (yaml/load-resource "matomo/statefulset.yaml")))

(defn generate-service-redis []
  (yaml/from-string (yaml/load-resource "matomo/service-redis.yaml")))

(defn generate-service-webserver []
  (yaml/from-string (yaml/load-resource "matomo/service-webserver.yaml")))
