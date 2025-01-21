;   Copyright (c) Matteo Redaelli at gmail dot com. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns net.clojars.matteoredaelli.tagger
  (:require [lambdaisland.uri :refer [uri]]
            [net.clojars.matteoredaelli.uri-ext :as uri-ext]))

(defn sw-regexs-links
  [prefix]
  [
   {:re (re-pattern (str prefix "[\\S]+\\.aspx"))
    :tags [:plang:aspnet]}
   {:re (re-pattern (str prefix "/?Main_Page"))
    :tags [:plang:php :sw:mediawiki]} 
   {:re (re-pattern (str prefix "/?wp-content"))
    :tags [:plang:php :sw:wordpress]}
   {:re (re-pattern "This website is like a Rocket")
    :tags [:plang:php :sw:wordpress :sw:wp-rocket]}
   {:re (re-pattern "jquery.*.js")
    :tags [:sw:jQuery]}
   {:re (re-pattern "react.*.js")
    :tags [:sw:reactjs]}
   {:re (re-pattern "angular.*.js")
    :tags [:sw:angularjs]}
   {:re (re-pattern "vue.*.js")
    :tags [:sw:vuejs]}
   {:re (re-pattern (str prefix "[\\S]+\\.php"))
    :tags [:plang:php]}
   ]
  )

(defn tag-text-by-regex
  [body regex-entry]
  (if (re-find (:re regex-entry) body)
    (:tags regex-entry)
    nil))

(defn tag-body
  [body prefix]
  (->> (map #(tag-text-by-regex body %)
            (sw-regexs-links prefix))
       (reduce concat)
       (filter some?)
       distinct
       ))

(def key-values-regexs
  [
   {:re #"(?i)apache|httpd"
    :tags [:sw:apache] }
   {:re #"(?i)lightspeed"
    :tags [:sw:lightspeed] } 
   {:re #"(?i)nginx"
    :tags [:sw:nginx] }
   {:re #"(?i)openresty"
    :tags [:sw:openresty] } 
   {:re #"(?i)-amz"
    :tags [:cloud:aws] }
   {:re #"(?i)-aruba"
    :tags [:cloud:aruba] }
   {:re #"(?i)aruba\.it"
    :tags [:cloud:aruba] }
   {:re #"(?i)gws"
    :tags [:cloud:google] }
   {:re #"(?i)x-goog"
    :tags [:cloud:google] }
   {:re #"(?i)x-ec-"
    :tags [:cloud:edgio] }
   {:re #"(?i)x-edg-"
    :tags [:cloud:edgio] } 
   {:re #"(?i)netlify"
    :tags [:cloud:netlify] }
   {:re #"(?i)cf-ray"
    :tags [:cdn:cloudflare] }
   {:re #"(?i)cloudflare"
    :tags [:cdn:cloudflare] }
   {:re #"(?i)cloudfront"
    :tags [:cloud:aws :cdn:cloudfront] }
   {:re #"(?i)akamai"
    :tags [:cdn:akamai] }
   {:re #"(?i)express"
    :tags [:sw:express :plang:nodejs]}
   {:re #"(?i)bigip"
    :tags [:sw:bigip]}   
   {:re #"(?i)varnish"
    :tags [:sw:varnish]}
   {:re #"(?i)joomla"
    :tags [:sw:joomla :plang:php]}
   {:re #"(?i)drupal"
    :tags [:sw:drupal :plang:php]}   
   {:re #"(?i)x-node"
    :tags [:plang:nodejs]}
   {:re #"(?i)x-wp"
    :tags [:sw:wordpress :plang:php]}])

(defn tag-headers
  [headers]
  (let [
        ;; remove key/value for "content-security-policy"  . es twitter does not use cloudfront
        cleaned_headers (apply dissoc headers ["content-security-policy-report-only" "content-security-policy"])
        keys (keys cleaned_headers)
        vals (vals cleaned_headers)
        keys_vals (concat keys vals)
        text (clojure.string/join " " keys_vals)]
    (->> (map #(tag-text-by-regex text %)
              key-values-regexs)
         (reduce concat)
         (filter some?)
         distinct
         )))

(defn tag
  [headers body prefix]
  (let [htags (tag-headers headers)
        btags (tag-body body prefix)]
    (->> (concat htags btags)
         distinct
         )))
