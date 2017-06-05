(ns es-typeahead.core-test
  (:require [cheshire.core :as json]
            [clojure.test :refer :all]
            [es-typeahead.wiremock :as wiremock]
            [org.httpkit.client :as http]
            [qbits.spandex :as s]))

(def server (wiremock/server))

(use-fixtures :once (wiremock/once-fixture server))
(use-fixtures :each (wiremock/each-fixture server))

(deftest a-test
  (testing "hello wiremock"
    (wiremock/new-mapping server {:request {:method "GET" :url "/hello"}
                                  :response {:status 200
                                             :body "{\"message\": \"Hello World\"}"
                                             :headers {:Content-Type "application/json"}}})
    (is (= {"message" "Hello World"} (-> (wiremock/url server "/hello")
                                         http/get
                                         deref
                                         :body
                                         json/parse-string))))

  (testing "spandex request"
    (wiremock/new-mapping server {:response {:status 200
                                             :body (json/generate-string {})
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

    (is (= ["/blog/user" "/blog/user"] (->> {:method "POST"}
                                            (wiremock/find-requests server)
                                            :requests
                                            (map :url))))
    (is (= 2 (:count (wiremock/count-requests server {:method "POST"}))))
    (is (empty? (:requests (wiremock/unmatched-requests server))))))
