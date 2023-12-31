(ns dda.c4k-shynet.shynet
 (:require
  [clojure.spec.alpha :as s]
  #?(:cljs [shadow.resource :as rc])
  [dda.c4k-common.yaml :as yaml]
  [dda.c4k-common.common :as cm]
  [dda.c4k-common.predicate :as pred]))

(s/def ::fqdn pred/fqdn-string?)
(s/def ::issuer pred/letsencrypt-issuer?)
(s/def ::django-secret-key pred/bash-env-string?)

#?(:cljs
   (defmethod yaml/load-resource :shynet [resource-name]
     (case resource-name
       "shynet/secret.yaml" (rc/inline "shynet/secret.yaml")
       "shynet/certificate.yaml" (rc/inline "shynet/certificate.yaml")
       "shynet/deployments.yaml" (rc/inline "shynet/deployments.yaml")
       "shynet/ingress.yaml" (rc/inline "shynet/ingress.yaml")  
       "shynet/service-redis.yaml" (rc/inline "shynet/service-redis.yaml")
       "shynet/service-webserver.yaml" (rc/inline "shynet/service-webserver.yaml")
       "shynet/statefulset.yaml" (rc/inline "shynet/statefulset.yaml")
       (throw (js/Error. "Undefined Resource!")))))

#?(:cljs
   (defmethod yaml/load-as-edn :shynet [resource-name]
     (yaml/from-string (yaml/load-resource resource-name))))
 
(defn generate-secret [config]
  (let [{:keys [fqdn django-secret-key postgres-db-user postgres-db-password]} config]
    (->
     (yaml/load-as-edn "shynet/secret.yaml")
     ; TODO: See comment in secret.yaml
     ;(assoc-in [:stringData :ALLOWED_HOSTS] fqdn)
     (assoc-in [:stringData :DJANGO_SECRET_KEY] django-secret-key)
     (assoc-in [:stringData :DB_USER] postgres-db-user)
     (assoc-in [:stringData :DB_PASSWORD] postgres-db-password))))

(defn generate-certificate [config]
  (let [{:keys [fqdn issuer]} config
        letsencrypt-issuer (name issuer)]
    (->
     (yaml/load-as-edn "shynet/certificate.yaml")
     (assoc-in [:spec :commonName] fqdn)
     (assoc-in [:spec :dnsNames] [fqdn])
     (assoc-in [:spec :issuerRef :name] letsencrypt-issuer))))

(defn generate-webserver-deployment []
  (let [shynet-application "shynet-webserver"]
    (-> (yaml/load-as-edn "shynet/deployments.yaml")
        (cm/replace-all-matching-values-by-new-value "shynet-application" shynet-application)
        (update-in [:spec :template :spec :containers 0] dissoc :command))))

(defn generate-celeryworker-deployment []
  (let [shynet-application "shynet-celeryworker"]
    (-> (yaml/load-as-edn "shynet/deployments.yaml")
        (cm/replace-all-matching-values-by-new-value "shynet-application" shynet-application))))

(defn generate-ingress [config]
  (let [{:keys [fqdn issuer]
         :or {issuer :staging}} config
        letsencrypt-issuer (name issuer)]
    (->
     (yaml/load-as-edn "shynet/ingress.yaml")
     (assoc-in [:metadata :annotations :cert-manager.io/cluster-issuer] letsencrypt-issuer)
     (cm/replace-all-matching-values-by-new-value "fqdn" fqdn))))

(defn generate-statefulset []
  (yaml/load-as-edn "shynet/statefulset.yaml"))

(defn generate-service-redis []
  (yaml/load-as-edn "shynet/service-redis.yaml"))

(defn generate-service-webserver []
  (yaml/load-as-edn "shynet/service-webserver.yaml"))
