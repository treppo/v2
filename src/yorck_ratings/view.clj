(ns yorck-ratings.view
  (:use [hiccup.page])
  (:require [yorck-ratings.rated-movie :as rated-movie]))

(defn- rating [rated-movie]
  (if (rated-movie/has-imdb-rating? rated-movie)
    [:span (str (rated-movie/rating rated-movie) " (" (rated-movie/rating-count rated-movie) ")")]
    "∅"))

(defn- imdb-info [rated-movie]
  (if (rated-movie/has-imdb-info? rated-movie)
    [:a {:href (rated-movie/imdb-url rated-movie)} (rated-movie/imdb-title rated-movie)]
    "Not found on IMDB"))

(def separator " • ")

(defn movie-item [rated-movie]
  (let [class (cond
                (rated-movie/hot? rated-movie) " hot"
                (rated-movie/considerable? rated-movie) " considerable"
                :else "")]
    [:li {:class (str "rated-movie" class)}
     (rating rated-movie)
     separator
     (imdb-info rated-movie)
     separator
     [:a {:href (rated-movie/yorck-url rated-movie)} (rated-movie/yorck-title rated-movie)]]))

(defn markup [movies]
  (html5
    [:head
     [:title "Yorck movies with IMDB ratings"]
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     [:style "* + * { margin-top: 1.5em; }
              body {
                font-size: 16px;
                font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", \"Roboto\", \"Oxygen\", \"Ubuntu\", \"Cantarell\", \"Fira Sans\", \"Droid Sans\", \"Helvetica Neue\", sans-serif;
                line-height: 2rem;
                margin-left: 5%;
                margin-right: 5%;
              }
              h1 {
                font-size: 1rem;
                font-weight: normal;
              }
              ol {
                list-style: none;
                padding: 0;
              }
              li {
                margin-top: 0;
              }
              a { color: black; }
              .hot {
                background-color: #fff7a1;
                margin-left: -.4rem;
                margin-right: -.4rem;
                padding-left: .4rem;
                padding-right: .4rem;
               }
              .considerable {
                background-color: #ffffe6;
                margin-left: -.5rem;
                margin-right: -.5rem;
                padding-left: .5rem;
                padding-right: .5rem;
               }"]]
    [:body
     [:h1 "IMDB rated Yorck movies"]
     [:ol
      (for [movie movies]
        (do
          (movie-item movie)))]]))
