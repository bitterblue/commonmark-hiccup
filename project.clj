(defproject commonmark-hiccup "0.3.0-SNAPSHOT"
  :description "Library to render CommonMark markdown to customizable HTML."
  :url "https://github.com/bitterblue/commonmark-hiccup"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [com.atlassian.commonmark/commonmark "0.17.0"]
                 [hiccup "1.0.5"]]
  :repositories [["releases" {:url "https://repo.clojars.org"
                              :creds :gpg}]])
