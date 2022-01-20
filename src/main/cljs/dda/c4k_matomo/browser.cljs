(ns dda.c4k-matomo.browser
  (:require
   [clojure.tools.reader.edn :as edn]
   [dda.c4k-matomo.core :as core]
   [dda.c4k-matomo.matomo :as matomo]
   [dda.c4k-common.browser :as br]
   [dda.c4k-common.postgres :as pgc]))

(defn generate-content
  []
  (into [] (concat [(assoc (br/generate-needs-validation) :content
                           (into [] (concat (br/generate-input-field "fqdn" "Your fqdn:" "matomo-neu.prod.meissa-gmbh.de")
                                            (br/generate-input-field "matomo-data-volume-path" "(Optional) Your matomo-data-volume-path:" "/var/matomo")
                                            (br/generate-input-field "postgres-data-volume-path" "(Optional) Your postgres-data-volume-path:" "/var/postgres")
                                            (br/generate-input-field "restic-repository" "(Optional) Your restic-repository:" "restic-repository")
                                            (br/generate-input-field "issuer" "(Optional) Your issuer prod/staging:" "")
                                            [(br/generate-br)]
                                            (br/generate-text-area "auth" "Your auth.edn:" "{:postgres-db-user \"matomo\"
         :postgres-db-password \"matomo-db-password\"
         :aws-access-key-id \"aws-id\"
         :aws-secret-access-key \"aws-secret\"
         :restic-password \"restic-password\"}"
                                                                   "5")
                                            [(br/generate-br)]
                                            (br/generate-button "generate-button" "Generate c4k yaml"))))]
                   (br/generate-output "c4k-nextcloud-output" "Your c4k deployment.yaml:" "25"))))

(defn generate-content-div
  []
  {:type :element
   :tag :div
   :content
   (generate-content)})

(defn config-from-document []
  (let [matomo-data-volume-path (br/get-content-from-element "matomo-data-volume-path" :optional true)
        postgres-data-volume-path (br/get-content-from-element "postgres-data-volume-path" :optional true)
        restic-repository (br/get-content-from-element "restic-repository" :optional true)
        issuer (br/get-content-from-element "issuer" :optional true :deserializer keyword)]
    (merge
     {:fqdn (br/get-content-from-element "fqdn")}
     (when (some? matomo-data-volume-path)
       {:matomo-data-volume-path matomo-data-volume-path})
     (when (some? postgres-data-volume-path)
       {:postgres-data-volume-path postgres-data-volume-path})
     (when (some? restic-repository)
       {:restic-repository restic-repository})
     (when (some? issuer)
       {:issuer issuer})
     )))

(defn validate-all! []
  (br/validate! "fqdn" ::matomo/fqdn)
  (br/validate! "matomo-data-volume-path" ::matomo/matomo-data-volume-path :optional true)
  (br/validate! "postgres-data-volume-path" ::pgc/postgres-data-volume-path :optional true)
  (br/validate! "issuer" ::matomo/issuer :optional true :deserializer keyword)
  (br/validate! "auth" core/auth? :deserializer edn/read-string)
  (br/set-validated!))

(defn add-validate-listener [name]
  (-> (br/get-element-by-id name)
      (.addEventListener "blur" #(do (validate-all!)))))


(defn init []
  (br/append-hickory (generate-content-div))
  (-> js/document
      (.getElementById "generate-button")
      (.addEventListener "click"
                         #(do (validate-all!)
                              (-> (core/generate
                                   (config-from-document)
                                   (br/get-content-from-element "auth" :deserializer edn/read-string))
                                  (br/set-output!)))))
  (add-validate-listener "fqdn")
  (add-validate-listener "matomo-data-volume-path")
  (add-validate-listener "postgres-data-volume-path")
  (add-validate-listener "restic-repository")
  (add-validate-listener "issuer")
  (add-validate-listener "auth"))