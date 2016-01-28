(ns yorck-ratings.view
  (:use [hiccup.page]))

(defn movie-item [movie]
  (let [props (vals movie)]
    (apply str (interpose " " props))))

(defn markup [movies]
  (html5
    [:head
     [:title "Yorck movies with IMDB ratings"]]
    [:body
     [:ol
      (for [movie movies]
        [:li (movie-item movie)])]]))
