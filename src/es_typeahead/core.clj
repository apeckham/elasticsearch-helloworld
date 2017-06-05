(ns es-typeahead.core
  (:gen-class)
  (:require [clojure.core.async :as async]
            [clojure.pprint :refer [pprint]]
            [faker.name :refer [names]]
            [qbits.spandex :as s])
  (:import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
           com.google.common.base.Supplier
           [java.time LocalDateTime ZoneOffset]
           org.apache.http.impl.nio.client.HttpAsyncClientBuilder
           [vc.inreach.aws.request AWSSigner AWSSigningRequestInterceptor]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, asdf World!"))

(def clock-supplier
  (reify Supplier
    (get [this]
      (LocalDateTime/now ZoneOffset/UTC))))

(defmethod qbits.spandex.client-options/set-http-client-option!
  :aws-signing-request-interceptor [_ ^HttpAsyncClientBuilder builder {:keys [service region]}]
  (.addInterceptorLast builder (-> (DefaultAWSCredentialsProviderChain.)
                                   (AWSSigner. region service clock-supplier)
                                   AWSSigningRequestInterceptor.)))

(def client (s/client {:hosts ["https://search-test-knkhdacrx5az5rzjsmmjip3ove.us-east-1.es.amazonaws.com"]
                       :http-client {:aws-signing-request-interceptor {:service "es" :region "us-east-1"}}}))

(defn req
  ([url method]
   (req url method nil))
  ([url method body]
   (->> {:url url
         :method method
         :body body
         :headers {:content-type "application/json"}}
        (s/request client)
        :body)))

(defn index [name]
  (pprint (req "/blog/user" :post {:name name})))

(defn count []
  (:count (req "/blog/user/_count" :post)))

(defn match-all []
  (->> {:query {:match_all {}}}
       (req "/blog/user/_search" :get)
       :hits
       :hits
       (map :_source)
       pprint))

(defn indices []
  (println (req "/_cat/indices" :get)))

(comment "easy indexing"
         (doall (map index (take 10 (names))))

         (count)

         (match-all))

(defn bulk-lines [name]
  [{:index {:_index "music" :_type "song"}}
   {:additional "payload2" :suggest {:input name}}])

(comment "completion suggester"

         (req "/music" :delete)

         (req "/_all" :get)

         (req "/_aliases" :post {:actions [{:remove {:index "_all"
                                                     :alias "music-alias"}}
                                           {:add {:index "music"
                                                  :alias "music-alias"}}]})

         (req "/music*" :get)

         (req "/music2" :put {:mappings
                             {:song
                              {:properties
                               {:suggest {:type "completion"}
                                :title {:type "keyword"}}}}})

         (time (doseq [part (partition-all 300 (mapcat bulk-lines (take 300000 (names))))]
                 (s/request client {:url "/_bulk"
                                    :method :put
                                    :body (s/chunks->body part)
                                    :headers {:content-type "application/x-ndjson"}})))

         (:count (req "/music/song/_count" :post nil))

         (->> {:query {:match_all {}}}
              (req "/suggest-199/_search" :post)
              :hits
              :hits
              (map :_source))

         (defn suggest [prefix]
           (->> {:suggest
                 {:song-suggest
                  {:prefix prefix
                   :completion {:field "suggest"
                                :size 20}}}}
                (req "/suggest/_search" :post)
                :suggest
                :song-suggest
                first
                :options
                (map :_source)))

         (suggest "aaron")

         )

(comment "index one doc at a time"
         (doseq [song (take 1000 (names))]
           (req "/music/song" :post {:additional "payload"
                                     :suggest
                                     {:input song}})))

(comment "bulk-chan indexing. blocks after first request, not sure why"
         (let [{:keys [input-ch output-ch]} (s/bulk-chan client {:max-concurrent-requests 1})
               bulk-lines (fn [name]
                            [{:index {:_index "music" :_type "song"}}
                             {:additional "payload2" :suggest {:input name}}])]
           (future
             (prn "started")
             (loop []
               (prn (async/<!! (second output-ch)))))
           (doseq [lines (map bulk-lines (take 1000 (names)))]
             (async/>!! input-ch lines))
           (async/close! input-ch)
           #_(loop []
               (when-let [[job response] (async/<!! output-ch)]
                 (if (-> response :body :errors)
                   (throw (Exception. "Bulk index failed")))
                 (recur)))))
