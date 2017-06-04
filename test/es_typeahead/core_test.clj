(ns es-typeahead.core-test
  (:require [cheshire.core :as json]
            [clojure
             [test :refer :all]
             [walk :refer [keywordize-keys]]]
            [es-typeahead.wiremock :as wiremock]
            [org.httpkit.client :as http]
            [qbits.spandex :as s])
  (:import com.github.tomakehurst.wiremock.core.WireMockConfiguration
           com.github.tomakehurst.wiremock.WireMockServer))

(def server (wiremock/server))

(use-fixtures :once (fn [f]
                      (.start server)
                      (f)
                      (.stop server)))

(use-fixtures :each (fn [f]
                      (.resetAll server)
                      (f)))

(defn admin-url [path]
  (format "http://localhost:%d/__admin%s" (.port server) path))

(defn admin-request [method path body]
  (->> (http/request {:method method
                      :url (admin-url path)
                      :body (json/generate-string body)})
       deref
       :body
       json/parse-string
       keywordize-keys))

(defn new-mapping [mapping]
  (admin-request :post "/mappings/new" mapping))

(defn find-requests [body]
  (admin-request :post "/requests/find" body))

(defn unmatched-requests []
  (admin-request :get "/requests/unmatched" nil))

(defn count-requests [body]
  (:count (admin-request :post "/requests/count" body)))

(deftest a-test
  (testing "hello wiremock"
    (new-mapping {:request {:method "GET" :url "/hello"}
                  :response {:status 200
                             :body "{\"message\": \"Hello World\"}"
                             :headers {:Content-Type "application/json"}}})
    (let [response @(http/get (format "http://localhost:%d/hello" (.port server)))]
      (is (= {"message" "Hello World"} (json/parse-string (:body response))))))

  (testing "spandex request"
    (new-mapping {:request {:method "POST" :url "/blog/user"}
                  :response {:status 200
                             :body "{}"
                             :headers {:Content-Type "application/json"}}})
    (let [client (s/client {:hosts [(str "http://localhost:" (.port server))]})]
      (s/request client {:url "/blog/user"
                         :method :post
                         :body {:name "world"}
                         :headers {:content-type "application/json"}})
      (s/request client {:url "/blog/user"
                         :method :post
                         :body {:name "hello"}
                         :headers {:content-type "application/json"}}))
    (is (= ["/blog/user" "/blog/user"] (map :url (:requests (find-requests {:method "POST"})))))
    (is (= 2 (count-requests {:method "POST"})))
    (is (empty? (:requests (unmatched-requests))))))
