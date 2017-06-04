(ns es-typeahead.core-test
  (:require [cheshire.core :as json]
            [clojure.test :refer :all]
            [org.httpkit.client :as http]
            [qbits.spandex :as s])
  (:import com.github.tomakehurst.wiremock.core.WireMockConfiguration
           com.github.tomakehurst.wiremock.WireMockServer))

(def server (WireMockServer. (.dynamicPort (WireMockConfiguration.))))

(use-fixtures :once (fn [f]
                      (.start server)
                      (f)
                      (.stop server)))

(use-fixtures :each (fn [f]
                      (.resetAll server)
                      (f)))

(deftest a-test
  (testing "FIXME, I fail."
    @(http/post (str "http://localhost:" (.port server) "/__admin/mappings/new")
                {:body (json/generate-string {:request {:method "GET" :url "/hello"}
                                              :response {:status 200
                                                         :body "{ \"message\": \"Hello World\" }"
                                                         :headers {:Content-Type "application/json"}}})})
    (let [response @(http/get (str "http://localhost:" (.port server) "/hello"))]
      (is (= {"message" "Hello World"} (json/parse-string (:body response))))))

  (testing "spandex request"
    @(http/post (str "http://localhost:" (.port server) "/__admin/mappings/new")
                {:body (json/generate-string {:request {:method "POST" :url "/blog/user"}
                                              :response {:status 200
                                                         :body "{}"
                                                         :headers {:Content-Type "application/json"}}})})
    (let [client (s/client {:hosts [(str "http://localhost:" (.port server))]})]
      (s/request client {:url "/blog/user"
                         :method :post
                         :body {:name "hello"}
                         :headers {:content-type "application/json"}}))
    (is (not (= nil (json/parse-string (:body
                                        @(http/post (str "http://localhost:" (.port server) "/__admin/requests/find")
                                                    {:body (json/generate-string {:method "POST" :url "/blog/user"})}))))))))
