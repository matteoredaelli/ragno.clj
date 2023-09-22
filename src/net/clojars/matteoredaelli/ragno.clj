(ns net.clojars.matteoredaelli.ragno
  (:import (org.jsoup Jsoup))
  (:require [clojure.string :as str]
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

(defn analyze-get-response
  "I don't do a whole lot."
  [url resp]
  (let [body (str (:body resp))
        soup (Jsoup/parse body)
        ;body-headers (distinct (html-ext/extract-element-text soup "h1,h2"))
        body-links (distinct (html-ext/extract-links soup))
        emails (distinct (links-ext/filter-email-links body-links))
        body-web-links (->> body-links
                            links-ext/remove-empty-links
                            links-ext/remove-links-with-fragment
                            links-ext/remove-links-with-mailto)
        body-external-links (links-ext/filter-external-links body-web-links url)
        body-internal-links (links-ext/filter-internal-links body-web-links url)
        body-domain-links (distinct (links-ext/get-domain-links body-web-links))
        head-description [(html-ext/extract-head-meta-content soup "name" "description")
                          (html-ext/extract-head-meta-content soup "property" "og:description")]
        head-keywords (html-ext/extract-head-meta-content soup "name" "keywords")
        head-title [(.title soup)
                    (html-ext/extract-head-meta-content soup "property" "og:title")]]
    {:status (:status resp)
     :http-headers (:headers resp)
     :url url
     :final_url (str (:uri resp))
     ;:body-headers body-headers
     :body-external-links body-external-links
     :body-domain-links body-domain-links
     :body-internal-links body-internal-links
     :head-title head-title
     :head-description head-description
     :head-keywords head-keywords
     :emails emails
     }))
  
(defn surf
  "I don't do a whole lot."
  [url]
  (let [resp (get-request url :string)
        status (:status resp)]
    (if (and (>= status 200)
             (< status 400))
      (analyze-get-response url resp)
      resp
      )))

(defn cli
  [opts]
  (->> (get opts :url)
       surf
       (json/write-str)
       println
       )
  )
