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
   {:re #"apache|httpd"
    :tags [:sw:apache] }
   {:re #"lightspeed"
    :tags [:sw:lightspeed] } 
   {:re #"nginx"
    :tags [:sw:nginx] }   
   {:re #"-amz"
    :tags [:cloud:aws] }
   {:re #"-aruba"
    :tags [:cloud:aruba] }
   {:re #"-azure"
    :tags [:cloud:azure] }
   {:re #"cloudflare"
    :tags [:cloud:aws :cdn:cloudflare] }
   {:re #"cloudfront"
    :tags [:cloud:aws :cdn:cloudfront] }
   {:re #"akamai"
    :tags [:cloud:aws :cdn:akamai] }
   {:re #"bigip"
    :tags [:sw:bigip]}
   {:re #"varnish"
    :tags [:sw:varnish]}
   {:re #"joomla"
    :tags [:sw:joomla :plang:php]}
   {:re #"drupal"
    :tags [:sw:drupal :plang:php]}   
   {:re #"x-node"
    :tags [:plang:nodejs]}
   {:re #"x-wp"
    :tags [:sw:wordpress :plang:php]}
  ])

(defn tag-headers
  [headers]
  (let [keys (keys headers)
        vals (vals headers)
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
