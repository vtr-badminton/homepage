(set-env!
 :source-paths #{"content"}
 :resource-paths #{"content", "ressources"}
 :dependencies '[[hiccup "1.0.5"]
                 [markdown-clj "0.9.91"]])



(require '[clojure.edn :as edn]
         '[clojure.string :as str]
         '[boot.core       :as boot]
         '[clojure.java.io :as io]
         '[hiccup.core :refer [h]]
         '[hiccup.page :refer [html5 include-css include-js]]
         '[markdown.core :as markdown])


(defn- load-md-file [file]
  (let [md (slurp (boot/tmp-file file))
        html (markdown/md-to-html-string md)
        name (boot/tmp-path file)
        name (.substring name 0 (.lastIndexOf name "."))
        title (.substring
               (first (str/split-lines md))
               2)
        lines (str/split-lines md)
        line-count (count lines)
        short-md (nth lines 2)
        short-html (markdown/md-to-html-string short-md)
        image-line (if (> line-count 3) (nth lines 4) nil)
        image (if (and image-line (.startsWith image-line "!["))
                (.substring
                  image-line
                  (inc (.indexOf image-line "("))
                  (dec (.length image-line)))
                nil)]
        ; image (if
        ;         (and (not (nil? image)) (.startsWith image "!["))
        ;         image
        ;         nil)]
        ; image (if
        ;         (.startsWith image "![")

        ;         nil)]

    {:name name
     :html-filename (str name ".html")
     :md md
     :html html
     :title title
     :short-html short-html
     :image image}))

(defn- load-md-files [fileset]
  (let [files (boot/input-files fileset)
        files (boot/by-ext [".md"] files)
        files (map load-md-file files)]
    {:artikels (vec files)}))

(defn- render-artikel [content artikel]
  [:div
   [:article
    [:div (:html artikel)]]
   [:br][:hr]
   [:i [:a {:href (:url content)} "Zurück zur Startseite..."]]])

(defn- render-main-content [content]
  [:div
   [:div.dm2020
    [:a {:href "dm2020/"
         :style "display: flex;
                 justify-content: center;
                 padding: 1em;
                 margin: 1em;
                 cursor: pointer;
                 border: 1px solid #999;
                 box-shadow: 0 10px 20px rgba(0,0,0,0.19), 0 6px 6px rgba(0,0,0,0.23);"}
     [:img {:src "dm2020.png"
            :style "width: 100%;"}]]]
   (for [artikel (reverse (sort-by :name (:artikels content)))]
     [:div
      [:div.post-preview
       [:a {:href (:html-filename artikel)}
        [:h3.post-title (h (:title artikel))]
        [:div
          (if (:image artikel)
            [:img {:src (:image artikel)
                   :style "width: 25%; float: left; margin-right: 1rem;"}])
          (:short-html artikel)
          [:div.clearfix]]]]
      [:hr]
      [:br]])])

(defn- render-sidebar-block [title content]
  [:div.post-preview {:style "margin-bottom: 8rem;"}
   [:h3.post-title title]
   [:div.post-meta
    content]])

(defn- render-sidebar [content]
  [:div

   (render-sidebar-block
    "Kontakt"
    [:p
     (get-in content [:kontakt :name])
     [:br]
     [:a {:href (str "tel:" (get-in content [:kontakt :telefon])) :target :_blank} (get-in content [:kontakt :telefon])]
     [:br]
     [:a {:href (str "mailto:" (get-in content [:kontakt :email])) :target :_blank} (get-in content [:kontakt :email])]])

   (render-sidebar-block
    "Training"
    [:div
     [:p
      [:a {:href (get-in content [:trainingsort :href])  :target :_blank}
       (get-in content [:trainingsort :adresse])]
      [:br]
      (get-in content [:trainingsort :halle])]

     [:p
      (for [zeit (:trainingszeiten content)]
        [:div {:style "margin-bottom: 1rem;"}
         (:zeit zeit)
         [:br]
         (:gruppe zeit)])]])

   (for [mansch (:mannschaften content)]
     (render-sidebar-block
      (:name mansch)
      [:div
       (if (:bild mansch) [:img {:src (:bild mansch) :style "width: 100%; margin-top: 2rem;"}])
       [:p [:a {:href (:turnierde-href mansch)  :target :_blank} (:liga mansch)]]
       [:p
        (for [spieler (:spieler mansch)]
          [:div (h spieler)])]]))])



(defn- render-page [content artikel]
  {:head [:head
          [:meta {:charset "utf-8"}]
          [:meta {:name "viewport"
                  :content "width=device-width, initial-scale=1"}]

          ;; (include-css "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css")
          ;; (include-css "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap-theme.min.css")

          (include-css "vendor/bootstrap/css/bootstrap.min.css")
          (include-css "css/clean-blog.min.css")
          (include-css "custom.css")
          (include-css "vendor/font-awesome/css/font-awesome.min.css")
          (include-css "https://fonts.googleapis.com/css?family=Lora:400,700,400italic,700italic")
          (include-css "https://fonts.googleapis.com/css?family=Open+Sans:300italic,400italic,600italic,700italic,800italic,400,300,600,700,800")
          "<!-- HTML5 Shim and Respond.js IE8 support of HTML5 elements and media queries -->
    <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
    <!--[if lt IE 9]>
        <script src=\"https://oss.maxcdn.com/libs/html5shiv/3.7.0/html5shiv.js\"></script>
        <script src=\"https://oss.maxcdn.com/libs/respond.js/1.4.2/respond.min.js\"></script>
    <![endif]-->"

          [:title (:title content)]]
   :body [:body {:class "body-container"}
          [:header.intro-header
           [:div.container
            [:div.row
             [:div.col-lg-8.col-lg-offset-2.col-md-10.col-md-offset-1
              [:div.site-heading
               [:h1 (:title content)]
               [:h5 (:subtitle content)]]]]]]
          [:div.container
           [:div.row

            [:div#main-content.col-sm-7
             (if artikel
               (render-artikel content artikel)
               (render-main-content content))]

            [:div.col-sm-1]

            [:div#sidebar.col-sm-4 (render-sidebar content)]]]



          ;; (include-js "https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js")
          ;; (include-js "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js")

          (include-js "vendor/jquery/jquery.min.js")
          (include-js "vendor/bootstrap/js/bootstrap.min.js")
          (include-js "js/clean-blog.min.js")]})



(boot/deftask homepage
  "Build static site"
  []
  (let [tmp (boot/tmp-dir!)]
    (fn middleware [next-handler]
      (fn handler [fileset]
        (boot/empty-dir! tmp)
        (let [content (-> (boot/tmp-get fileset "content.edn")
                          boot/tmp-file
                          slurp
                          edn/read-string)
              content (merge content (load-md-files fileset))
              index-page (render-page content nil)]
          (spit
           (io/file tmp "index.html")
           (html5 (:head index-page) (:body index-page)))
          (doall (for [artikel (:artikels content)]
                   (let [page (render-page content artikel)]
                     (spit
                      (io/file tmp (str (:html-filename artikel)))
                      (html5 (:head page) (:body page)))))))


        (-> fileset
            (boot/add-resource tmp)
            (boot/commit!)
            next-handler)))))
