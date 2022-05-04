(ns dda.c4k-shynet.uberjar
  (:gen-class)
  (:require
   [dda.c4k-shynet.core :as core]
   [dda.c4k-common.common :as common]))

(defn -main [& cmd-args]
  (common/main-common "c4k-shynet" core/config? core/auth? core/config-defaults core/k8s-objects cmd-args))
