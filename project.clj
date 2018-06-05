(defproject yorck-ratings "2.0.0"
  :description "IMDB ratings for movies playing in Yorck cinemas Berlin"
  :url "https://yorck-ratings.treppo.org"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [http-kit "2.3.0"]
                 [ring/ring-devel "1.6.3"]
                 [ring/ring-core "1.6.3"]
                 [http-kit.fake "0.2.2"]
                 [luminus/config "0.8"]
                 [hickory "0.7.1"]
                 [org.clojure/core.async "0.4.474"]]
  :main ^:skip-aot yorck-ratings.web
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev     {:resource-paths ["config/dev"]
                       :dependencies   [[midje "1.9.1"] [midje-notifier "0.2.0"]]
                       :plugins        [[lein-midje "3.2.1"]]}
             :midje    {:resource-paths ["test/resources" "config/test"]}}
  :min-lein-version "2.8.0"
  :uberjar-name "yorck-ratings-standalone.jar")
