(ns dda.c4k-shynet.core-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   #?(:cljs [shadow.resource :as rc])
   [clojure.spec.alpha :as s]
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-shynet.core :as cut]))

#?(:cljs
   (defmethod yaml/load-resource :shynet-test [resource-name]
     (case resource-name
       "shynet-test/valid-auth.yaml"   (rc/inline "shynet-test/valid-auth.yaml")
       "shynet-test/valid-config.yaml" (rc/inline "shynet-test/valid-config.yaml")
       (throw (js/Error. "Undefined Resource!")))))

(deftest validate-valid-resources
  (is (s/valid? cut/config? (yaml/load-as-edn "shynet-test/valid-config.yaml")))
  (is (s/valid? cut/auth? (yaml/load-as-edn "shynet-test/valid-auth.yaml"))))