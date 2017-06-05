(ns es-typeahead.wiremock
  (:require [cheshire.core :as json]
            [clojure.walk :refer [keywordize-keys]]
            [org.httpkit.client :as http])
  (:import com.github.tomakehurst.wiremock.core.WireMockConfiguration
           com.github.tomakehurst.wiremock.WireMockServer))

(defn server []
  (WireMockServer. (.dynamicPort (WireMockConfiguration.))))

(defn url
  ([server]
   (url server nil))
  ([server path]
   (str "http://localhost:" (.port server) path)))

(defn admin [server method path body]
  (-> {:method method
       :url (url server (str "/__admin" path))
       :body (json/generate-string body)}
      http/request
      deref
      :body
      json/parse-string
      keywordize-keys))

(defn new-mapping [server body]
  (admin server :post "/mappings/new" body))

(defn find-requests [server body]
  (admin server :post "/requests/find" body))

(defn unmatched-requests [server]
  (admin server :post "/requests/unmatched" nil))

(defn count-requests [server body]
  (admin server :post "/requests/count" body))

(defn once-fixture [server]
  (fn [f]
    (.start server)
    (f)
    (.stop server)))

(defn each-fixture [server]
  (fn [f]
    (.resetAll server)
    (f)))
