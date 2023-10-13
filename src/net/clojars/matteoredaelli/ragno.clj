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

(defn ping
  "I don't do a whole lot."
  [args]
  (prn (get args :url) "Hello, World!"))

(defn foo
  "I don't do a whole lot."
  [x]
  (prn x "Hello, World!"))

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


(defn check-link [link]
  (let [resp (get-request link :stream)]
    {:url link
     :final-url (str (:uri resp))
     :status (:status resp)}
    )
  )

(defn check-links [links]
  (log/debug (str "check-links " links " - begin"))
  (def resp (mapv check-link links))
  (log/debug (str "check-links " links " - end"))
  resp
  )

(defn analyze-get-response
  "I don't do a whole lot."
  [url resp opts]
  (log/debug (str "analyze-get-response " url " - begin"))
  (let [body (str (:body resp))
        location (get-in resp [:headers :location] url)
        soup (Jsoup/parse body)
        ;; body-headers (distinct (html-ext/extract-element-text soup "h1,h2"))
        body-links (distinct (html-ext/extract-links soup))
        emails (distinct (links-ext/filter-email-links body-links))
        body-web-links (->> ;; (conj body-links location)
                        body-links
                        links-ext/remove-empty-links
                            links-ext/remove-links-with-fragment
                            links-ext/remove-links-with-mailto)
        ;; TODO SOme websites have too many links
        ;; https://as.com has too many links and 
        check-links (check-links (vec (take  (:check-links opts) body-web-links)))
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
     ;; :body-headers body-headers
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
  (log/debug (str "surf " url " - begin"))
  (let [resp (get-request url :string http-options)
        status (:status resp)]
    (if (and (>= status 200)
             (< status 400))
      (
       ;; check if domain changes
       let [final-url (str (:uri resp))
            domain (uri-ext/get-domain-url (uri url))
            final-domain (uri-ext/get-domain-url (uri final-url))]
       (if (= domain final-domain)
         (analyze-get-response url resp ragno-options)
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
      [surf_opts {:check-links false}]
    (-> (:url opts)
        (surf surf_opts {:follow-redirects :always})
        (json/write-str)
        println)))
