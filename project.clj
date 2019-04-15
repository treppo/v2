(defproject yorck-ratings "2.0.0"
  :description "IMDB ratings for movies playing in Yorck cinemas Berlin"
  :url "https://yorck-ratings.treppo.org"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [clj-http "3.9.1"]
                 [ring/ring-core "1.7.1"]
                 [ring/ring-jetty-adapter "1.7.1"]
                 [hickory "0.7.1"]
                 [hiccup "1.0.5"]
                 [org.clojure/core.async "0.4.490"]]
  :aot [yorck-ratings.web]
  :main yorck-ratings.web
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev     {:dependencies   [[clj-http-fake "1.0.3"]
                                        [ring/ring-mock "0.3.2"]]
                       :resource-paths ["test/resources"]
                       :plugins        [[lein-jlink "0.2.1"]
                                        [lein-ancient "0.6.15"]
                                        [lein-cljfmt "0.6.4"]]
                       :jlink-modules  ["java.sql" "java.naming"]}}
  :min-lein-version "2.8.0"
  :uberjar-name "yorck-ratings-standalone.jar")
