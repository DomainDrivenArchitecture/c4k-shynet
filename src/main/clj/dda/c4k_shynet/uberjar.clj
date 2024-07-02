(ns dda.c4k-shynet.uberjar
  (:gen-class)
  (:require
   [dda.c4k-shynet.core :as core]
   [dda.c4k-common.uberjar :as uberjar]))

(defn -main [& cmd-args]
  (uberjar/main-cm "c4k-shynet" core/config? core/auth? core/config-defaults core/config-objects core/auth-objects cmd-args))
