(ns yorck-ratings.view
  (:use [hiccup.page]))

(defn movie-item [movie]
  (let [{:keys [rating rating-count imdb-title imdb-url yorck-title yorck-url]} movie]
    (str rating " (" rating-count ") • <a href=\"" imdb-url "\">" imdb-title "</a> • <a href=\"" yorck-url "\">" yorck-title "</a>")))

(defn markup [movies]
  (html5
    [:head
     [:title "Yorck movies with IMDB ratings"]
     [:meta {:charset "utf-8"}]
     [:style "body {
                font-size: 16px;
                line-height: 1.4rem;
              }
              ol { list-style: none; }
              li {
                padding: .1rem;
              }
              a { color: black; }
              .highlighted {
                background-color: yellow;
                margin-left: -.4rem;
                margin-right: -.4rem;
                padding-left: .4rem;
                padding-right: .4rem;
               }"]]
    [:body
     [:ol
      (for [movie movies]
        [:li
         [:span {:class (if (< (:rating movie) 7) "" "highlighted")} (movie-item movie)]])]]))
