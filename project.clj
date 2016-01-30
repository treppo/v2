(defproject yorck-ratings "0.1.0-SNAPSHOT"
  :description "IMDB ratings for movies playing in Yorck cinemas Berlin"
  :url "https://yorck-ratings.treppo.org"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [http-kit "2.1.19"]
                 [ring/ring-devel "1.4.0"]
                 [ring/ring-core "1.4.0"]
                 [http-kit.fake "0.2.2"]
                 [luminus/config "0.3"]
                 [hickory "0.6.0"]
                 [org.clojure/core.async "0.2.374"]]
  :main ^:skip-aot yorck-ratings.web
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :min-lein-version "2.4.0"
  :uberjar-name "yorck-ratings-standalone.jar")
