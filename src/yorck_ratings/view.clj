(ns yorck-ratings.view
  (:use [hiccup.page]))

(defn movie-item [movie]
  (let [{:keys [rating rating-count imdb-title yorck-title]} movie]
    (str rating " (" rating-count ") " imdb-title " " yorck-title)))

(defn markup [movies]
  (html5
    [:head
     [:title "Yorck movies with IMDB ratings"]
     [:meta {:charset "utf-8"}]]
    [:body
     [:ol
      (for [movie movies]
        [:li (movie-item movie)])]]))
