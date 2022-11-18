(ns irsa-error-repro
  (:import (software.amazon.awssdk.core.internal.http.loader ClasspathSdkHttpServiceProvider))
  (:gen-class))

(defn invoke-private-static-method [obj fn-name-string & args]
  (let [m (first (filter (fn [x] (.. x getName (equals fn-name-string)))
                         (.getDeclaredMethods obj)))]
    (. m (setAccessible true))
    (. m (invoke obj args))))

(defn invoke-private-method [obj fn-name-string & args]
  (let [m (first (filter (fn [x] (.. x getName (equals fn-name-string)))
                         (.. obj getClass getDeclaredMethods)))]
    (. m (setAccessible true))
    (. m (invoke obj args))))

(defn get-http-client
  []
  (-> (invoke-private-static-method ClasspathSdkHttpServiceProvider "syncProvider")
      (invoke-private-method "loadService")))

(defn -main
  [& _]
  (get-http-client))