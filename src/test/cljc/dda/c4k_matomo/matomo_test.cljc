(ns dda.c4k-matomo.matomo-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [dda.c4k-matomo.matomo :as cut]))


(deftest should-generate-webserver-deployment
  (is (= {:apiVersion "apps/v1"
          :kind "Deployment"
          :metadata
          {:name "shynet-webserver"
           :namespace "default"
           :labels {:app "shynet-webserver"}}
          :spec
          {:selector {:matchLabels {:app "shynet-webserver"}}
           :strategy {:type "Recreate"}
           :replicas 1
           :template
           {:metadata {:labels {:app "shynet-webserver"}}
            :spec
            {:containers
             [{:name "shynet-webserver"
               :image "milesmcc/shynet:edge"
               :imagePullPolicy "IfNotPresent"
               :envFrom [{:secretRef {:name "shynet-settings"}}]}]}}}}
         (cut/generate-webserver-deployment))))

(deftest should-generate-celeryworker-deployment
  (is (= {:apiVersion "apps/v1"
          :kind "Deployment"
          :metadata
          {:name "shynet-celeryworker"
           :namespace "default"
           :labels {:app "shynet-celeryworker"}}
          :spec
          {:selector {:matchLabels {:app "shynet-celeryworker"}}
           :strategy {:type "Recreate"}
           :replicas 1
           :template
           {:metadata {:labels {:app "shynet-celeryworker"}}
            :spec
            {:containers
             [{:name "shynet-celeryworker"
               :image "milesmcc/shynet:edge"
               :imagePullPolicy "IfNotPresent"
               :command ["./celeryworker.sh"]
               :envFrom [{:secretRef {:name "shynet-settings"}}]}]}}}}
         (cut/generate-celeryworker-deployment))))

(deftest should-generate-certificate
  (is (= {:apiVersion "cert-manager.io/v1"
          :kind "Certificate"
          :metadata {:name "shynet-cert", :namespace "default"}
          :spec
          {:secretName "shynet-secret"
           :commonName "test.com"
           :dnsNames ["test.com"]
           :issuerRef {:name "letsencrypt-staging-issuer", :kind "ClusterIssuer"}}}
         (cut/generate-certificate {:fqdn "test.com" :issuer :staging}))))

(deftest should-generate-ingress
  (is (= {:apiVersion "networking.k8s.io/v1"
          :kind "Ingress"
          :metadata
          {:name "shynet-webserver-ingress"
           :annotations
           {:cert-manager.io/cluster-issuer "letsencrypt-staging-issuer"
            :kubernetes.io/ingress.class "addon-http-application-routing"
            :nginx.ingress.kubernetes.io/proxy-body-size "256m"
            :nginx.ingress.kubernetes.io/ssl-redirect "true"
            :nginx.ingress.kubernetes.io/rewrite-target "/"
            :nginx.ingress.kubernetes.io/proxy-connect-timeout "300"
            :nginx.ingress.kubernetes.io/proxy-send-timeout "300"
            :nginx.ingress.kubernetes.io/proxy-read-timeout "300"}}
          :spec
          {:tls [{:hosts ["test.com"], :secretName "shynet-secret"}]
           :rules
           [{:host "test.com"
             :http {:paths [{:backend {:serviceName "shynet-webserver-service", :servicePort 8080}, :path "/"}]}}]}}
         (cut/generate-ingress {:fqdn "test.com" :issuer :staging}))))