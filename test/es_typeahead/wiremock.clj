(ns es-typeahead.wiremock
  (:require [cheshire.core :as json]
            [clojure.walk :refer [keywordize-keys]]
            [org.httpkit.client :as http])
  (:import com.github.tomakehurst.wiremock.core.WireMockConfiguration
           com.github.tomakehurst.wiremock.WireMockServer))

(defn server []
  (WireMockServer. (.dynamicPort (WireMockConfiguration.))))

(defn admin-request [server method path body]
  (-> {:method method
       :url (format "http://localhost:%d/__admin%s" (.port server) path)
       :body (json/generate-string body)}
      http/request
      deref
      :body
      json/parse-string
      keywordize-keys))

(defn new-mapping [server mapping]
  (admin-request server :post "/mappings/new" mapping))

(defn find-requests [server body]
  (admin-request server :post "/requests/find" body))

(defn unmatched-requests [server]
  (admin-request server :get "/requests/unmatched" nil))

(defn count-requests [server body]
  (:count (admin-request server :post "/requests/count" body)))

(defn once-fixture [server]
  (fn [f]
    (.start server)
    (f)
    (.stop server)))

(defn each-fixture [server]
  (fn [f]
    (.resetAll server)
    (f)))
