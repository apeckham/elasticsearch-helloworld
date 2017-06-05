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
(defn wiremock-each-fixture [f]
  (.resetAll server)
  (f))
(defn wiremock-once-fixture [f]
  (.start server)
  (f)
  (.stop server))

(use-fixtures :once wiremock-once-fixture)
(use-fixtures :each wiremock-each-fixture)

(defn admin-request [server method path body]
  (->> (http/request {:method method
                      :url (format "http://localhost:%d/__admin%s" (.port server) path)
                      :body (json/generate-string body)})
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

(deftest a-test
  (testing "hello wiremock"
    (new-mapping server {:request {:method "GET" :url "/hello"}
                         :response {:status 200
                                    :body "{\"message\": \"Hello World\"}"
                                    :headers {:Content-Type "application/json"}}})
    (let [response @(http/get (format "http://localhost:%d/hello" (.port server)))]
      (is (= {"message" "Hello World"} (json/parse-string (:body response))))))

  (testing "spandex request"
    (new-mapping server {:request {:method "POST" :url "/blog/user"}
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
    (is (= ["/blog/user" "/blog/user"] (map :url (:requests (find-requests server {:method "POST"})))))
    (is (= 2 (count-requests server {:method "POST"})))
    (is (empty? (:requests (unmatched-requests server))))))
