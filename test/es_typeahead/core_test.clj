(ns es-typeahead.core-test
  (:require [cheshire.core :as json]
            [clojure.test :refer :all]
            [es-typeahead.wiremock :as wiremock]
            [org.httpkit.client :as http]
            [qbits.spandex :as s]))

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

(deftest a-test
  (testing "hello wiremock"
    (wiremock/new-mapping server {:request {:method "GET" :url "/hello"}
                         :response {:status 200
                                    :body "{\"message\": \"Hello World\"}"
                                    :headers {:Content-Type "application/json"}}})
    (let [response @(http/get (format "http://localhost:%d/hello" (.port server)))]
      (is (= {"message" "Hello World"} (json/parse-string (:body response))))))

  (testing "spandex request"
    (wiremock/new-mapping server {:request {:method "POST" :url "/blog/user"}
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
    (is (= ["/blog/user" "/blog/user"] (map :url (:requests (wiremock/find-requests server {:method "POST"})))))
    (is (= 2 (wiremock/count-requests server {:method "POST"})))
    (is (empty? (:requests (wiremock/unmatched-requests server))))))
