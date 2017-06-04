(ns es-typeahead.core-test
  (:require [clojure.test :refer :all]
            [cheshire.core :as json]
            [org.httpkit.client :as http])
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
      (is (= {"message" "Hello World"} (json/parse-string (:body response)))))

    #_(let [client] (s/client {:hosts [(str "http://localhost:" (.port server))]})
           (s/request client {:url url
                              :method method
                              :body body
                              :headers {:content-type "application/json"}}))

    (is (= 1 1))))
