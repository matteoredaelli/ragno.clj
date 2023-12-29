;   Copyright (c) Matteo Redaelli at gmail dot com. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns net.clojars.matteoredaelli.social-tagger
  (:require [lambdaisland.uri :refer [uri]]
            [net.clojars.matteoredaelli.uri-ext :as uri-ext]))

(def social-regexs
  [
   ;; thanks to https://github.com/lorey/socials/blob/master/socials/socials.py
   {:re #"^http[s]?://(www\.)?facebook\.com/([A-Za-z0-9_\-\.]+)/?$"
    :social :facebook} 
   {:re #"^http[s]?://(www\.)?facebook\.com/profile\.php\?id=(\d+)$"
    :social :facebook}

   {:re #"^http[s]?://(www\.)?github\.com/([A-z0-9_.-]+)/?$"
    :social :github}

   {:re #"^http[s]?://(www\.)?instagram\.com/([A-z0-9_.-]+)/?$"
    :social :instagram}

   {:re #"^http[s]?://(www\.)?instagr\.am/([A-z0-9_.-]+)/?$"
    :social :instagram}

   {:re #"^http[s]?://(www\.)?linkedin\.com/(company/)?([A-z0-9_.-@]+)/?"
    :social :linkedin}
   
   {:re #"^http[s]?://(www\.)?youtube\.com/((c|user)/)?([A-z0-9_.-@]+)/?"
    :social :youtube}
   
   {:re #"^http[s]?://(www\.)?tiktok\.com/@([A-z0-9_.-]+)/?$"
    :social :tiktok}
   
   {:re #"^http[s]?://(www\.)?twitter\.com/([A-z0-9_.-]+)/?$"
    :social :twitter}

   ]
  )

(defn tag-link
  ([link regex-entry]
   (let [name (last (re-matches (:re regex-entry) link))]
     (if (= name nil)
       nil
       {(:social regex-entry) name})))
  ([link]
   (->> (map #(tag-link link %) social-regexs)
        (filter some?)
        )))

(defn tag-links
  ([links]
   (->> (map #(tag-link %) links)
        flatten
        (reduce merge)
        )))

