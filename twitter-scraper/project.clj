(defproject twitter-scraper "1.0.0-SNAPSHOT"
  :description "A simple scraper for the Twitter public timeline"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/tools.cli "0.2.1"]
                 [org.clojure/tools.logging "0.2.3"]
                 [commons-logging "1.1.1"]
                 [log4j "1.2.16" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]
                 [net.htmlparser.jericho/jericho-html "3.2"]
                 [org.elasticsearch/elasticsearch "0.18.7"]
                 [clj-json "0.5.0"]]
  :dev-dependencies [[swank-clojure/swank-clojure "1.4.0-SNAPSHOT"]]
  :main twitter-scraper.core)