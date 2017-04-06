(ns wumpus-web.core
  (:import goog.History)
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary]
            [ajax.core :as ajax]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [wumpus-web.common :as c]
            [wumpus-web.pages.index :refer [index] :rename {index index-page}]))

(enable-console-print!)

(defn current-page
  "Used by Reagent to get the correct component to render."
  []
  [:div [(session/get :current-page)]])

(defn index
  "Handler for the '/' route."
  []
  (session/put! :current-page #'index-page))

(defn hook-browser-navigation
  "Make the browser's back button work correctly."
  []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event] (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defn app-routes
  "Define routes for the app."
  []
  (secretary/set-config! :prefix "#")
  (secretary/defroute "/" [] (index))
  (hook-browser-navigation))

(defn on-js-reload [])

(defn main
  []
  (app-routes)
  (reagent/render-component [current-page] (. js/document (getElementById "app"))))

(add-watch c/flash-message :flash
           (fn [key atom old new]
             (if (not= new "")
               (js/setTimeout #(reset! c/flash-message "") 8000))))

(main)
