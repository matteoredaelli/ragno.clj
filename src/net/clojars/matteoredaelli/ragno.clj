;   Copyright (c) Matteo Redaelli at gmail dot com. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns net.clojars.matteoredaelli.ragno
  (:import (org.jsoup Jsoup))
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [net.clojars.matteoredaelli.uri-ext :as uri-ext]
            [net.clojars.matteoredaelli.html-ext :as html-ext]
            [net.clojars.matteoredaelli.links-ext :as links-ext]
            [clojure.java.io :as io]
            [babashka.http-client :as http]
            [lambdaisland.uri :refer [uri join]]))

(defn read-edn-file [source]
  (try
    (with-open [r (io/reader source)]
      (edn/read (java.io.PushbackReader. r)))

    (catch java.io.IOException e
      (log/error "Couldn't open '%s': %s\n" source (.getMessage e))
      (throw (Exception.))
      )
    (catch RuntimeException e
      (log/error "Error parsing edn file '%s': %s\n" source (.getMessage e))
      (throw (Exception.))
      )))
  
(defn lazy-contains? [coll key]
  (boolean (some #(= % key) coll)))

(defn validate-edn-config-or-exit [config]
  (assert (not= config {} "Empty / Missing config file. Bye"))
  (let [keys (keys config)]
    (assert (lazy-contains? keys :http-options) "Missing key ':http-option'. Bye")
    (assert (lazy-contains? keys :ragno-options) "Missing key ':ragno-option'. Bye")
    (assert (lazy-contains? keys :redis) "Missing key ':redis'. Bye")
    ))
  
(defn get-request
  "Doing raw http requests"
  [url as http-options]
   (log/debug (str "get-request " url " - begin"))
   (try
     (http/get url {:as as
                    :throw false
                    :client (http/client http-options)})
     (catch Exception e {:status -1
                         :url url
                         :error (str "caught exception: " e)})))


(defn check-link
  [link http-options]
  (let [resp (get-request link :stream http-options)]
    {:url link
     :final-url (str (:uri resp))
     :status (:status resp)}
    )
  )

(defn check-links
  [links http-options]
  (log/debug (str "check-links " links " - begin"))
  (def resp (mapv #(check-link % http-options)
                  links))
  (log/debug (str "check-links " links " - end"))
  resp
  )

(defn analyze-get-response
  "I don't do a whole lot."
  [url resp ragno-options http-options]
  (log/debug (str "analyze-get-response " url " - begin"))
  (let [body (str (get resp :body ""))
        location (get-in resp [:headers :location] url)
        soup (Jsoup/parse body)
        body-headers (distinct (html-ext/extract-element-text soup "h1,h2"))
        body-links (distinct (html-ext/extract-links soup))
        emails (distinct (links-ext/filter-email-links body-links))
        body-web-links (->> ;; (conj body-links location)
                        body-links
                        links-ext/remove-empty-links
                        links-ext/remove-links-with-fragment
                        links-ext/remove-links-with-mailto)
        ;; TODO SOme websites have too many links
        ;; https://as.com has too many links and 
        check-links (check-links (vec (take  (:check-links ragno-options)
                                             body-web-links))
                                 http-options)
        corrupted-links (->> (filterv #( = -1 (:status %)) check-links)
                             (map #(:url %)))
        good-links (->> (map #(:final-url %) check-links)
                        links-ext/remove-empty-links
                        distinct)
        domain-links (->> (concat body-web-links good-links)
                          distinct
                          links-ext/remove-empty-links
                          links-ext/get-domain-links
                          distinct)
        head-description [(html-ext/extract-head-meta-content soup "name" "description")
                          (html-ext/extract-head-meta-content soup "property" "og:description")]
        head-keywords (html-ext/extract-head-meta-content soup "name" "keywords")
        head-title [(.title soup)
                    (html-ext/extract-head-meta-content soup "property" "og:title")]]
    (log/debug (str "analyze-get-response " url " - end"))
    {:status (:status resp)
     :http-headers (:headers resp)
     :url url
     :final-url (str (:uri resp))
     ;;:body-headers body-headers
     :links body-web-links
     :corrupted-links corrupted-links
     :good-links good-links
     :domain-links domain-links
     :head-title head-title
     :head-description head-description
     :head-keywords head-keywords
     :emails emails
     }))
  
(defn surf
  "I don't do a whole lot."
  [url ragno-options http-options]
  (log/debug (str "surf "
                  url
                  "with options"
                  http-options
                  " - begin"))
  (let [resp (get-request url :string http-options)
        status (:status resp)]
    (log/debug resp)
    (if (and (>= status 200)
             (< status 600))
      (
       ;; check if domain changes
       let [final-url (str (:uri resp))
            domain (uri-ext/get-domain-url (uri url))
            final-domain (uri-ext/get-domain-url (uri final-url))]
       (log/debug (str "domain:" domain " , final-domain:" final-domain))
       (if (= domain final-domain)
         (analyze-get-response url resp ragno-options http-options)
         ;; redirect to a new domain. nothing to do, just adding the final domain for a further crawling 
         {:status 301
          :real-status status
          :url url
          :final-url final-url
          :link-domains [final-domain]}
         ))
      ;; status not ok
      resp
      )))

(defn cli
  [opts]
  (let
      [urls (:urls opts)
       urls-list (clojure.string/split urls #",")
       config-file (:config-file opts)
       config (read-edn-file config-file)
       http-options (:http-options config)
       ragno-options (:ragno-options config)]
    (log/info config)
    (validate-edn-config-or-exit config) 
    (mapv #(-> (surf % ragno-options http-options)
               (json/write-str)
               println)
          urls-list)))
