(ns twitter-scraper.core
  (:gen-class)
  (:use [clojure.tools.cli]
        [clojure.tools.logging])
  (:require [clojure.string :as string]
            [clj-json.core :as json])
  (:import [java.net URL]
           [java.util Date]
           [net.htmlparser.jericho Source]
           [net.htmlparser.jericho HTMLElementName]
           [org.elasticsearch.client.transport TransportClient]
           [org.elasticsearch.common.transport InetSocketTransportAddress]
           [org.elasticsearch.common.settings ImmutableSettings]
           [org.apache.commons.logging LogFactory]
           [org.apache.commons.logging Log]))

;; logger instance
(def LOGGER (. LogFactory getLog "twitter-scraper.core"))

;; twitter timeline seed URL
(def SEED-URL (URL. "https://twitter.com/public_timeline"))

(defn fetch-page
  "Fetches a page from network and parses it with Jericho. A Jericho
  Source object is returned."
  [url-in]
  (Source. url-in))

(defn extract-statuses
  "Returns a sequence of Jericho Element objects, each one containing
  a status."
  [source-page]
  (.getAllElementsByClass (.getFirstElementByClass source-page "statuses") "status"))

(defn extract-status-date
  "Extracts the String date from a String status timestamp."
  [date-in]
  (subs date-in 7 (- (count date-in) 2)))

(defn extract-status-id
  "Extracts an String id (user name/status id) from a String status
  URL."
  [status-url]
  (let [chunks (string/split status-url #"/")]
    (apply str (interpose "/" [(nth chunks 3) (last chunks)]))))

(defn extract-user-id
  "Extracts the user's id from the provided String status URL."
  [status-url]
  (let [chunks (string/split status-url #"/")]
    (nth chunks 3)))

(defn extract-status
  "Returns a hash-map containing the status for the provided Jericho
  Element."
  [source-status]

  ;; fetch the URL, pull out the vcard and status body
  (let [vcard (.getFirstElementByClass source-status "vcard")
        body (.getFirstElementByClass source-status "status-body")]

    (merge

     ;; extract vcard information
     (let [anchor (.getFirstElement vcard HTMLElementName/A)
           avatar (.getFirstElement (.getFirstElement vcard) HTMLElementName/IMG)]

       {:profile-url (.getValue (first (.getAttributes (.getFirstStartTag anchor))))
        :avatar {:alt-text (.getAttributeValue avatar "alt")
                 :url (.getAttributeValue avatar "src")
                 :height (.getAttributeValue avatar "height")
                 :width (.getAttributeValue avatar "width")}})

     ;; extract status information
     (let [status-content (.getFirstElementByClass body "status-content")
           status-meta (.getFirstElementByClass body "entry-meta")
           status-url (.getAttributeValue (.getFirstElementByClass status-meta "entry-date") "href")]

       {:content (str (.getTextExtractor status-content))
        :content-html (str status-content)
        :date (extract-status-date
               (.getAttributeValue (.getFirstElementByClass status-meta "published") "data"))
        :url status-url
        :id (extract-status-id status-url)
        :user-id (extract-user-id status-url)}))))

(defn status-maps
  "Returns a sequence of hash-maps, each map containing the data for a
  status message found on the provided page."
  [twitter-url-in]
  (for [status-in (extract-statuses (fetch-page twitter-url-in))]
    (extract-status status-in)))

(defn transform-status
  "Transforms a hash-map of status data into a String of JSON data
  suitable for loading."
  [status-map]
  (json/generate-string status-map))

(defn get-es-client
  "Returns a new Elasticsearch client that is all ready to load data
  into our cluster."
  []

  ;; create a new client
  (let [es-client (TransportClient.)]

    ;; point the client at our nodes
    (doto es-client
      (.addTransportAddress
       (InetSocketTransportAddress. "33.33.33.10" 9300))
      (.addTransportAddress
       (InetSocketTransportAddress. "33.33.33.11" 9300))
      (.addTransportAddress
       (InetSocketTransportAddress. "33.33.33.12" 9300)))))

(defn -main
  "Provides the bootstrapping function for the application."
  [& args-in]
  (info "Hello from Twitter Scraper!")

  ;; we setup our client, an atom to accumulate statud ids and we store
  ;; the start-time of our loading processs in another atom
  (let [es-client (get-es-client)
        ids (atom (hash-set))
        start-time (atom (.getTime (Date.)))]

    ;; we're going to keep reloading the seed forever
    (while true

      ;; scrape the seed URL
      (let [statuses (status-maps SEED-URL)]

        (info (str (count statuses) " Twitter status messages extracted"))

        ;; handle each status
        (doseq [status statuses]

          ;; check to see if this is a new status
          (if (not (@ids (:id status)))

            ;; load in this status
            (do (info (str "Loading status " (:id status)))

                ;; prepare the index to load our data, set the source
                ;; content to our JSON String and then execute the insert
                (.execute (.setSource (.prepareIndex es-client
                                                     "twitter"
                                                     "tweet"
                                                     (:id status))
                                      (transform-status status)))

                ;; add the id to our accumulator
                (swap! ids conj (:id status))))))

      ;; wait a minute before re-fetching
      (let [elapsed (- (.getTime (Date.)) @start-time)]
        (if (> 60000 elapsed)
          (Thread/sleep (- 60000 elapsed)))
        (reset! start-time (.getTime (Date.)))))))
