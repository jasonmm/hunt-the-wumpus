(ns wumpus-web.common
  (:require [reagent.core :as reagent :refer [atom]]
            [ajax.core :as ajax]
            [clojure.string :as str]))

(defonce app-state (atom {:game-state nil}))
(defonce room-input (atom ""))
(defonce room-description (atom ""))
(defonce flash-message (atom ""))
(defonce danger-text {:pit    "I feel a breeze!"
                      :bats   "I hear bats!"
                      :wumpus "I smell a Wumpus!"})
(defonce death-text {:pit    "AAAAHHHH! You have fallen into a bottomless pit!"
                     :wumpus "CHOMP! The Wumpus has eaten you!"})

(def api-url "/api/v1/")

(defn game-over?
  [game-state]
  (or (nil? game-state)
      (:wumpus-dead game-state)
      (some? (:room-danger game-state))))

(defn clear-flash
  "Sets `flash-message` to a blank strings."
  []
  (reset! flash-message ""))

(defn new-game
  []
  (ajax/GET
    (str api-url "new-game")
    {:handler       (fn [response]
                      (swap! app-state assoc :game-state response))
     :error-handler (fn [response]
                      (reset! room-description "An error occurred creating the new game."))}))

(defn move
  [new-room]
  (clear-flash)
  (ajax/GET
    (str api-url (get-in @app-state [:game-state :id]) "/move/" new-room)
    {:handler       (fn [response]
                      (swap! app-state assoc :game-state response))
     :error-handler (fn [response]
                      (reset! flash-message (get-in response [:response :error])))}))

(defn shoot
  [target-room]
  (clear-flash)
  (ajax/GET
    (str api-url (get-in @app-state [:game-state :id]) "/shoot/" target-room)
    {:handler       (fn [response]
                      (swap! app-state assoc :game-state response))
     :error-handler (fn [response]
                      (reset! flash-message (:error response)))}))
