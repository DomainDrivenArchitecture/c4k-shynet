(ns dda.c4k-shynet.browser
  (:require
   [clojure.tools.reader.edn :as edn]
   [dda.c4k-shynet.core :as core]
   [dda.c4k-shynet.shynet :as shynet]
   [dda.c4k-common.browser :as br]
   [dda.c4k-common.postgres :as pgc]))

;; TODO: Move these fkt up to common
(defn concat-vec [& vs]
  (into []
        (apply concat vs)))

(defn generate-group
  [name
   content]
  [{:type :element
    :tag :div
    :attrs {:class "rounded border border-3  m-3 p-2"}
    :content [{:type :element
               :tag :b
               :attrs {:style "z-index: 1; position: relative; top: -1.3rem;"}
               :content name}
              {:type :element
               :tag :fieldset
               :content content}]}])

(defn generate-content []
  (concat-vec
   [(assoc
     (br/generate-needs-validation) :content
     (concat-vec
      (generate-group
       "domain"
       (concat-vec
        (br/generate-input-field "fqdn" "Your fqdn:" "shynet.prod.meissa-gmbh.de")
        (br/generate-input-field "issuer" "(Optional) Your issuer prod/staging:" "")))
      (generate-group
       "provider"
       (concat-vec
        (br/generate-input-field "ingress-type" "(Optional) Your ingress type (traefik/ngingx):" "traefik")
        (br/generate-input-field "postgres-data-volume-path" "(Optional) Your postgres-data-volume-path:" "/var/postgres")))
      (generate-group
       "credentials"
       (br/generate-text-area
        "auth" "Your auth.edn:"
        "{:postgres-db-user \"shynet\"
:postgres-db-password \"shynet-db-password\"
:django-secret-key \"djangosecretkey\"}"
        "5"))
      [(br/generate-br)]
      (br/generate-button "generate-button" "Generate c4k yaml")))]
   (br/generate-output "c4k-shynet-output" "Your c4k deployment.yaml:" "25")))

(defn generate-content-div
  []
  {:type :element
   :tag :div
   :content
   (generate-content)})

(defn config-from-document []
  (let [postgres-data-volume-path (br/get-content-from-element "postgres-data-volume-path" :optional true)
        issuer (br/get-content-from-element "issuer" :optional true :deserializer keyword)]
    (merge
     {:fqdn (br/get-content-from-element "fqdn")}
     (when (some? postgres-data-volume-path)
       {:postgres-data-volume-path postgres-data-volume-path})
     (when (some? issuer)
       {:issuer issuer})
     )))

(defn validate-all! []
  (br/validate! "fqdn" ::shynet/fqdn)
  (br/validate! "postgres-data-volume-path" ::pgc/postgres-data-volume-path :optional true)
  (br/validate! "issuer" ::shynet/issuer :optional true :deserializer keyword)
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
  (add-validate-listener "postgres-data-volume-path")
  (add-validate-listener "issuer")
  (add-validate-listener "auth"))