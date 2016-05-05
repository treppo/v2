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
                line-height: 1.5em;
              }
              ol { list-style: none; }
              a { color: black; }"]]
    [:body
     [:ol
      (for [movie movies]
        [:li (movie-item movie)])]]))
