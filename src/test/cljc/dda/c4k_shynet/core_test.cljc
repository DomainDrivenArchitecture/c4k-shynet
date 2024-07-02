(ns dda.c4k-shynet.core-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   #?(:cljs [shadow.resource :as rc])
   [clojure.spec.alpha :as s]
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-shynet.core :as cut]))

#?(:cljs
   (defmethod yaml/load-resource :jitsi-test [resource-name]
     (case resource-name
       "valid-auth.yaml"   (rc/inline "valid-auth.yaml")
       "valid-config.yaml" (rc/inline "valid-config.yaml")
       (throw (js/Error. "Undefined Resource!")))))

(deftest validate-valid-resources
  (is (s/valid? cut/config? (yaml/load-as-edn "valid-config.yaml")))
  (is (s/valid? cut/auth? (yaml/load-as-edn "valid-auth.yaml"))))