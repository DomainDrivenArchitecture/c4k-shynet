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

