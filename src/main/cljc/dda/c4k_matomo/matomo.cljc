(ns dda.c4k-matomo.matomo
 (:require
  [clojure.spec.alpha :as s]
  #?(:cljs [shadow.resource :as rc])
  [dda.c4k-common.yaml :as yaml]
  [dda.c4k-common.common :as cm]))

(s/def ::fqdn cm/fqdn-string?)
(s/def ::issuer cm/letsencrypt-issuer?)
(s/def ::matomo-data-volume-path string?)

#?(:cljs
   (defmethod yaml/load-resource :matomo [resource-name]
     (case resource-name
       "matomo/certificate.yaml" (rc/inline "matomo/certificate.yaml")
       ;"matomo/deployments.yaml" (rc/inline "matomo/deployments.yaml")
       ;"matomo/ingress.yaml" (rc/inline "matomo/ingress.yaml")  
       ;"matomo/services.yaml" (rc/inline "matomo/services.yaml")
       ;"matomo/statefulset.yaml" (rc/inline "matomo/statefulset.yaml")
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
    (-> (yaml/from-string (yaml/load-resource "matomo/deployments-template.yaml"))
        (cm/replace-all-matching-values-by-new-value "shynet-application" shynet-application)
        (update-in [:spec :template :spec :containers 0] dissoc :command))))

(defn generate-ingress [config]
  (let [{:keys [fqdn issuer]
         :or {issuer :staging}} config
        letsencrypt-issuer (str "letsencrypt-" (name issuer) "-issuer")]
    (->
     (yaml/from-string (yaml/load-resource "matomo/ingress.yaml"))
     (assoc-in [:metadata :annotations :cert-manager.io/cluster-issuer] letsencrypt-issuer)
     (cm/replace-all-matching-values-by-new-value "fqdn" fqdn))))

(defn generate-persistent-volume [config]
  (let [{:keys [matomo-data-volume-path]} config]
    (-> 
     (yaml/from-string (yaml/load-resource "matomo/persistent-volume.yaml"))
     (assoc-in [:spec :hostPath :path] matomo-data-volume-path))))

(defn generate-pvc []
  (yaml/from-string (yaml/load-resource "matomo/pvc.yaml")))

(defn generate-service []
  (yaml/from-string (yaml/load-resource "matomo/service.yaml")))
