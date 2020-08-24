(ns webapp.core
  (:require [net.cgrand.enlive-html :as enlive]
            [compojure.core :refer [defroutes GET POST]]
            [ring.adapter.jetty :as jetty]))

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
  [:section.tweeds] (enlive/content (map tweed-tpl tweeds)))

(defroutes app
  (GET "/" [] (index-tpl (get-tweeds store))))

(def server (jetty/run-jetty app {:port 3000 :join? false}))