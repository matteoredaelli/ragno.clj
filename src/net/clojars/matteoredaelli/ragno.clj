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
  ([url as]
   (get-request url
                as
                {:follow-redirects :always
                 ;; :proxy {:host "proxyzscaler.group.pirelli.com"
                 ;;         :port 80}
                                      
                 ;; :connect-timeout  10000
                 ;; :throw-exceptions false
                 }
                ))
  ([url as http_options]
   (try
     (http/get url {:as as
                    :throw false
                    :client (http/client http_options)})
     (catch Exception e {:status -1
                         :url url
                         :error (str "caught exception: " e)}))))


(defn check-link [link]
  (let [resp (get-request link :stream)]
    {:url link
     :final-url (str (:uri resp))
     :status (:status resp)}
    )
  )

(defn check-links [links]
  (mapv check-link links))

(defn analyze-get-response
  "I don't do a whole lot."
  [url resp opts]
  (let [body (str (:body resp))
        soup (Jsoup/parse body)
        ;; body-headers (distinct (html-ext/extract-element-text soup "h1,h2"))
        body-links (distinct (html-ext/extract-links soup))
        emails (distinct (links-ext/filter-email-links body-links))
        body-web-links (->> body-links
                            links-ext/remove-empty-links
                            links-ext/remove-links-with-fragment
                            links-ext/remove-links-with-mailto)
        body-domain-links (distinct (links-ext/get-domain-links body-web-links))
        head-description [(html-ext/extract-head-meta-content soup "name" "description")
                          (html-ext/extract-head-meta-content soup "property" "og:description")]
        head-keywords (html-ext/extract-head-meta-content soup "name" "keywords")
        head-title [(.title soup)
                    (html-ext/extract-head-meta-content soup "property" "og:title")]]
    {:status (:status resp)
     :http-headers (:headers resp)
     :url url
     :final-url (str (:uri resp))
     :check-links (if (:check-links opts)
                    (check-links body-web-links)
                    [])
                                        ;:body-headers body-headers
     :links body-web-links
     :domain-links body-domain-links
     :head-title head-title
     :head-description head-description
     :head-keywords head-keywords
     :emails emails
     }))
  
(defn surf
  "I don't do a whole lot."
  [url opts]
  (let [resp (get-request url :string)
        status (:status resp)]
    (if (and (>= status 200)
             (< status 400))
      (
       ;; check if domain changes
       let [final-url (str (:uri resp))
            domain (uri-ext/get-domain-url (uri url))
            final-domain (uri-ext/get-domain-url (uri final-url))]
       (if (= domain final-domain)
         (analyze-get-response url resp opts)
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
        (surf surf_opts)
        (json/write-str)
        println)))
