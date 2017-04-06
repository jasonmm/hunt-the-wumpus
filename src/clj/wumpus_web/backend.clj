(ns wumpus-web.backend
  (:require [compojure.api.sweet :refer :all]
            [ring.adapter.jetty :refer :all]
            [ring.util.http-response :refer :all]
            [ring.middleware.content-type :refer :all]
            [ring.middleware.resource :refer :all]
            [ring.middleware.not-modified :refer :all]
            [ring.middleware.params :refer :all]
            [clj-time.core :as t]
            [environ.core :refer [env]]))

;; The number of seconds before a game is considered abandoned.
(def game-ttl (or (:game-ttl env) 300))

;; List of adjoining rooms to the index room.
;; For example, room 0 adjoins rooms 1, 4, and 7.
(def cave
  [[1,4,7]
   [0,2,9]
   [1,3,11]
   [2,4,13]
   [0,3,5]
   [4,6,14]
   [5,7,16]
   [0,6,8]
   [7,9,17]
   [1,8,10]
   [9,11,18]
   [2,10,12]
   [11,13,19]
   [3,12,14]
   [5,13,15]
   [14,16,19]
   [6,15,17]
   [8,16,18]
   [10,17,19]
   [12,15,18]])

;; Defines a data type that holds the current state of the game.
(defrecord Game
           [wumpus-room bat-room-1 bat-room-2 pit-room-1 pit-room-2
            human-room game-over arrows last-modified id])

;; The data type returned to the client describing the current game state.
(defrecord GameState
           [human-room adjoining-dangers adjoining-rooms arrows
            id room-danger wumpus-dead])

;; A vector of active games.
(defonce active-games (atom {}))

(defn unique-rand-rooms
  "Returns `n` unique random room numbers."
  [n]
  (vec (take n (set (take 18 (repeatedly #(inc (rand-int 18))))))))

(defn uuid
  "https://gist.github.com/gorsuch/1418850"
  []
  (str (java.util.UUID/randomUUID)))

(defn adjoining-rooms?
  "Whether or not two rooms are adjoining."
  [r1 r2]
  (> (.indexOf (nth cave r1) r2) -1))

(defn adjoining-dangers
  "Return a list of dangers that are in adjoining rooms to the `human-room`."
  [game]
  (let [room (:human-room game)]
    (or (->> {:wumpus (adjoining-rooms? room (:wumpus-room game))
               :bats   (or (adjoining-rooms? room (:bat-room-1 game))
                           (adjoining-rooms? room (:bat-room-2 game)))
               :pit    (or (adjoining-rooms? room (:pit-room-1 game))
                           (adjoining-rooms? room (:pit-room-2 game)))}
              (filter (fn [[k v]] (true? v)))
              keys)
        [])))

(defn bat-move-human
  "Bats move humans to a random, non-bat, location in the cave."
  [game]
  (let [not-bat-room (fn [r] (and (not= (:bat-room-1 game) r) (not= (:bat-room-2 game) r)))]
    (assoc game :human-room (->> (unique-rand-rooms 18)
                                 (filter not-bat-room)
                                 first))))

(defn human-room-danger
  "Determine if the human is in the same room as a danger, and return which
  danger. Returns `nil` if the human is not in the same room as a danger."
  [game]
  (condp = (:human-room game)
    (:wumpus-room game) :wumpus
    (:bat-room-1 game)  :bat
    (:bat-room-2 game)  :bat
    (:pit-room-1 game)  :pit
    (:pit-room-2 game)  :pit
    nil))

(defn set-game-over
  "Sets what the human dies by if the human is in the same room as a
  deadly danger."
  [game]
  (assoc game :game-over (case (human-room-danger game)
                           :wumpus true
                           :pit    true
                           false)))

(defn current-game-state
  "Constructs a `GameState` record for `game`."
  [game]
  (let [human-room (:human-room game)]
    (map->GameState {:human-room        human-room
                     :arrows            (:arrows game)
                     :id                (:id game)
                     :room-danger       (human-room-danger game)
                     :wumpus-dead       (and (:game-over game)
                                             (nil? (human-room-danger game)))
                     :adjoining-dangers (adjoining-dangers game)
                     :adjoining-rooms   (get cave human-room)})))

(defn move-human
  "Move the human to the new room and returns the new game."
  [game new-room]
  (assoc game :human-room new-room))

(defn add-active-game
  "Adds `game` to the `active-games` vector."
  [game]
  (swap! active-games assoc (:id game) game))

(defn delete-old-games
  "Removes games with a `last-modified` time in the past."
  [])

(defn create-game
  "Handler for the `new-game` API route."
  []
  (delete-old-games)
  (let [danger-rooms (unique-rand-rooms 4)
        new-game     (map->Game {:wumpus-room   (int (inc (rand-int 19)))
                                 :bat-room-1    (nth danger-rooms 0)
                                 :bat-room-2    (nth danger-rooms 1)
                                 :pit-room-1    (nth danger-rooms 2)
                                 :pit-room-2    (nth danger-rooms 3)
                                 :human-room    0
                                 :game-over     false
                                 :arrows        3
                                 :last-modified (t/now)
                                 :id            (uuid)})]
    (add-active-game new-game)
    (ok (current-game-state new-game))))

(defn game-not-found
  "Returns a bad request stating the game was not found."
  []
  (not-found "Game Not Found"))

(defn invalid-game?
  "Whether or not the game is invalid."
  [game]
  (or (nil? game) (:game-over game)))

(comment
  (-> game
      (assoc :human-room)
      (assoc :last-modified)
      (bat-move-human)
      (set-game-over)))

(defn move
  "Moves the human and updates the game accordingly. Must be called from
  `move-handler` which does various checks to ensure the move can be made."
  [game new-room]
  (let [game (-> game
                 (move-human new-room)
                 (assoc :last-modified (t/now))
                 set-game-over)
        game (if (= :bat (human-room-danger game))
               (bat-move-human game)
               game)]
    (add-active-game game)
    (ok (current-game-state game))))

(defn shoot
  "Shoots into the `target-room`. Must be called from `shoot-handler` which
  ensures the shot can be taken."
  [game target-room]
  (let [game (-> game
                 (assoc :last-modified (t/now))
                 (assoc :game-over (= (:wumpus-room game) target-room))
                 (assoc :arrows (dec (:arrows game))))]
    (add-active-game game)
    (ok (current-game-state game))))

(defn move-handler
  "Handler for the `move` API route. Performs various checks to ensure
  the move can be made before calling `move`."
  [game new-room]
  (if (invalid-game? game)
    (game-not-found)
    (if (adjoining-rooms? (:human-room game) new-room)
      (move game new-room)
      (bad-request {:error (str "Room " new-room " is not an adjoining room to human room " (:human-room game) ".")}))))

(defn shoot-handler
  [game target-room]
  (if (invalid-game? game)
    (game-not-found)
    (if (adjoining-rooms? (:human-room game) target-room)
      (shoot game target-room)
      (bad-request {:error (str "Room " target-room " is not an adjoining room to human room " (:human-room game) " and cannot be shot into.")}))))

(defapi wumpus-api
  {:swagger {:ui   "/api/swagger"
             :spec "/swagger.json"
             :data {:info {:title "Hunt The Wumpus API"}}}}
  (context "/api/v1" []
    (GET "/new-game" []
      :summary "Create a new game."
      (create-game))
    (GET "/:game-id/move/:room" [game-id room]
      :summary "Move the human to `room`."
      (move-handler (get @active-games game-id) (Integer/parseInt room)))
    (GET "/:game-id/shoot/:room" [game-id room]
      :summary "Shoot an arrow to `room`."
      (shoot-handler (get @active-games game-id) (Integer/parseInt room)))))

(def app
  (-> wumpus-api
      (wrap-resource "public")
      (wrap-content-type)
      (wrap-not-modified)))

(defn -main
  [& args]
  (run-jetty #'app {:port (Integer/parseInt (first args))}))
