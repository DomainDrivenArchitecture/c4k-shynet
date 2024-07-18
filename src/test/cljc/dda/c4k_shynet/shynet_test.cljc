(ns dda.c4k-shynet.shynet-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [dda.c4k-shynet.shynet :as cut]))


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
               :image "milesmcc/shynet:v0.12.0"
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
               :image "milesmcc/shynet:v0.12.0"
               :imagePullPolicy "IfNotPresent"
               :command ["./celeryworker.sh"]
               :envFrom [{:secretRef {:name "shynet-settings"}}]}]}}}}
         (cut/generate-celeryworker-deployment))))

(deftest should-generate-ingress-and-cert
  (is (= [{:apiVersion "cert-manager.io/v1",
           :kind "Certificate",
           :metadata
           {:name nil,
            :labels {:app.kubernetes.part-of nil},
            :namespace "default"},
           :spec
           {:secretName nil,
            :commonName nil,
            :duration "2160h",
            :renewBefore "720h",
            :dnsNames nil,
            :issuerRef {:name "staging", :kind "ClusterIssuer"}}}
          {:apiVersion "networking.k8s.io/v1",
           :kind "Ingress",
           :metadata
           {:namespace "default",
            :annotations
            {:traefik.ingress.kubernetes.io/router.entrypoints
             "web, websecure",
             :traefik.ingress.kubernetes.io/router.middlewares
             "default-redirect-https@kubernetescrd",
             :metallb.universe.tf/address-pool "public"},
            :name nil,
            :labels {:app.kubernetes.part-of nil}},
           :spec {:tls [{:hosts nil, :secretName nil}], :rules []}}]
         (cut/generate-ingress-and-cert {:fqdn "test.com" :issuer :staging}))))

(deftest should-generate-secret
  (is (= {:apiVersion "v1"
          :kind "Secret"
          :metadata {:name "shynet-settings"}
          :type "Opaque"
          :stringData
          {:DEBUG "False"
           :ALLOWED_HOSTS "*"
           :DJANGO_SECRET_KEY "django-pw"
           :ACCOUNT_SIGNUPS_ENABLED "False"
           :TIME_ZONE "America/New_York"
           :REDIS_CACHE_LOCATION
           "redis://shynet-redis.default.svc.cluster.local/0"
           :CELERY_BROKER_URL
           "redis://shynet-redis.default.svc.cluster.local/1"
           :DB_NAME "shynet"
           :DB_USER "postgres-user"
           :DB_PASSWORD "postgres-pw"
           :DB_HOST "postgresql-service"
           :EMAIL_HOST_USER ""
           :EMAIL_HOST_PASSWORD ""
           :EMAIL_HOST ""
           :SERVER_EMAIL "Shynet <noreply@shynet.example.com>"}}
         (cut/generate-secret {:fqdn "test.com"}
                              {:django-secret-key "django-pw"
                               :postgres-db-user "postgres-user" :postgres-db-password "postgres-pw"}))))