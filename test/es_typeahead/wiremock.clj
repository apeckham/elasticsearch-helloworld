(ns es-typeahead.wiremock
  (:import com.github.tomakehurst.wiremock.core.WireMockConfiguration
           com.github.tomakehurst.wiremock.WireMockServer))

(defn server []
  (WireMockServer. (.dynamicPort (WireMockConfiguration.))))
