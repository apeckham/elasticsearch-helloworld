(defproject es-typeahead "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [cc.qbits/spandex "0.3.11"]
                 [faker "0.2.2"]
                 [vc.inreach.aws/aws-signing-request-interceptor "0.0.16"]
                 [com.amazonaws/aws-java-sdk-core "1.11.138"]]
  :main ^:skip-aot es-typeahead.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
