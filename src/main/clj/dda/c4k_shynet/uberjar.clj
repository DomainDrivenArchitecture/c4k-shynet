(ns dda.c4k-shynet.uberjar
  (:gen-class)
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as cs]
   [clojure.tools.reader.edn :as edn]
   [expound.alpha :as expound]
   [dda.c4k-shynet.core :as core]))

(def usage
  "usage:
  
  c4k-shynet {your configuraton file} {your authorization file}")

(s/def ::options (s/* #{"-h"}))
(s/def ::filename (s/and string?
                              #(not (cs/starts-with? % "-"))))
(s/def ::cmd-args (s/cat :options ::options
                         :args (s/?
                                (s/cat :config ::filename
                                       :auth ::filename))))

(defn expound-config
  [config]
  (expound/expound ::core/config config))

(defn invalid-args-msg 
  [spec args]
  (s/explain spec args)
  (println (str "Bad commandline arguments\n" usage)))

(defn generate-common [my-config my-auth config-defaults k8s-objects]
  (let [resulting-config (merge config-defaults my-config my-auth)]
    (cs/join
     "\n---\n"
     (k8s-objects resulting-config))))

(defn main-common [config-spec? auth-spec? config-defaults k8s-objects cmd-args]
  (let [parsed-args-cmd (s/conform ::cmd-args cmd-args)]
    (if (= ::s/invalid parsed-args-cmd)
      (invalid-args-msg ::cmd-args cmd-args)
      (let [{:keys [options args]} parsed-args-cmd
            {:keys [config auth]} args]
          (cond
            (some #(= "-h" %) options)
            (println usage)
            :default
            (let [config-str (slurp config)
                  auth-str (slurp auth)
                  config-edn (edn/read-string config-str)
                  auth-edn (edn/read-string auth-str)
                  config-valid? (s/valid? config-spec? config-edn)
                  auth-valid? (s/valid? auth-spec? auth-edn)]
              (if (and config-valid? auth-valid?)
                (println (generate-common config-edn auth-edn config-defaults k8s-objects))
                (do
                  (when (not config-valid?) 
                    (println 
                     (expound/expound-str config-spec? config-edn {:print-specs? false})))
                  (when (not auth-valid?) 
                    (println 
                     (expound/expound-str auth-spec? auth-edn {:print-specs? false})))))))))))

(defn -main [& cmd-args]
  (main-common core/config? core/auth? core/config-defaults core/k8s-objects cmd-args))
