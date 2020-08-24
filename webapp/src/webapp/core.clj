(ns webapp.core
  (:require [net.cgrand.enlive-html :as enlive]
            [compojure.core :refer [defroutes GET POST]]
            [ring.adapter.jetty :as jetty]
            [compojure.route :refer [resources]]
            [ring.middleware.params :refer [wrap-params]]))

(defrecord Tweed [title content])

(defprotocol TweedStore
  (get-tweeds [store])
  (put-tweed! [store tweed]))

(defrecord AtomStore [data])

(extend-protocol TweedStore
  AtomStore
  (get-tweeds [store]
    (get @(:data store) :tweeds))
  (put-tweed! [store tweed]
    (swap! (:data store)
           update-in [:tweeds] conj tweed)))

(def store (->AtomStore (atom {:tweeds '()})))

(enlive/defsnippet tweed-tpl  "../resources/tweedler/index.html" [[:article.tweed enlive/first-of-type]]
  [tweed]
  [:.title] (enlive/html-content (:title tweed))
  [:.content] (enlive/html-content (:content tweed)))

(enlive/deftemplate index-tpl "../resources/tweedler/index.html"
  [tweeds]
  [:section.tweeds] (enlive/content (map tweed-tpl tweeds))
  [:form] (enlive/set-attr :method "post" :action "/"))

(put-tweed! store (->Tweed "Teste Title 2" "content"))

(defn handle-create [{{title "title" content "content"} :params}]
  (put-tweed! store (->Tweed title content))
  {:body "" :status 302 :headers {"Location" "/"}})

(defroutes app-routes
  (GET "/" [] (index-tpl (get-tweeds store)))
  (POST "/" req (handle-create req))
  (resources "/css" {:root "tweedler/css"})
  (resources "/img" {:root "tweedler/img"}))

(def app (-> app-routes
             (wrap-params)))

(def server (jetty/run-jetty (var app) {:port 7000 :join? false}))