(ns wumpus-web.pages.index
  (:require [wumpus-web.common :as c]
            [clojure.string :as str]))

(defn room-desc-game-over
  [game-state]
  (into
   [:h2]
   (cond
     (some? (:room-danger game-state)) (->> game-state
                                            :room-danger
                                            (get c/death-text))
     (:wumpus-dead game-state) "Hooray!! You have killed the Wumpus!")))

(defn room-desc-normal
  [game-state]
  [:div
   [:p "You are in room " (:human-room game-state)]
   [:p "Adjoning rooms are " (str/join ", " (:adjoining-rooms game-state))]
   [:p "You have " (:arrows game-state) " arrows left."]
   (into [:div] (->> game-state
                     :adjoining-dangers
                     (select-keys c/danger-text)
                     vals
                     (map (fn [v] [:p v]))))])

(defn room-description
  [game-state]
  [:div
   (if (c/game-over? game-state)
     [room-desc-game-over game-state]
     [room-desc-normal game-state])])

(defn game-controls
  [game-state]
  (if (not (c/game-over? game-state))
    [:span
     [:input {:type        "number"
              :id          "room"
              :placeholder "Room"
              :size        4
              :on-change   #(reset! c/room-input (-> % .-target .-value))}]
     [:button {:on-click #(c/move @c/room-input)} "Move"]
     [:button {:on-click #(c/shoot @c/room-input)} "Shoot"]
     [:strong {:style {:color "red" :margin-left "10px"}} @c/flash-message]]))

(defn index
  []
  [:div
   [:h1 "Hunt The Wumpus!"]
   [:p "The Wumpus lives in a cave of 20 rooms. Each room has 3 tunnels leading to other rooms."]
   [:p "There are two rooms with pits, two rooms with bats, and one room with the Wumpus. If you enter a room with a pit you will fall to your death. If you enter a room with a bat then the bat will pick you up and carry you to a random room (which my be troublesome). If you enter a room with the Wumpus then the Wumpus will eat you."]
   [:p "Each turn you may move or shoot an arrow into an adjoining room. If you hit the Wumpus with an arrow you win."]
   [:hr]
   [:div
    [:button {:on-click #(c/new-game)} "New Game"]
    (if (some? (:game-state @c/app-state))
      [:span
       [game-controls (:game-state @c/app-state)]
       [room-description (:game-state @c/app-state)]])]
   ;; [:pre (pr-str (:game-state @c/app-state))]
   ])
