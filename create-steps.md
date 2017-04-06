1. `lein new figwheel <name> -- --reagent`
2. Edit `project.clj` as follows:
  * Add `[lein-ring "0.8.8"]` to `:plugins` in `project.clj`
  * Add
    :main wumpus-web.backend
    :ring {:handler wumpus-web.backend/app}
    :hooks [leiningen.cljsbuild]
    :main wumpus-web.backend
    :ring {:handler wumpus-web.backend/app}
    :hooks [leiningen.cljsbuild]
  * Change the root `:source-paths` to `["src/clj" "src/cljs"]`
  * Change all the `:source-paths` in `:cljsbuild :builds` to `["src/cljs"]`
  * Comment out the `:cljsbuild :builds :figwheel :open-urls` line
  * Add an `:uberjar` profile:
    :ubjerjar {:cljsbuild {:builds
                           [{:source-paths ["src/cljs"]
                             :compiler {:optimizations :advanced
                                        :output-to "resources/public/js/compiled/wumpus_web.js"
                                        :pretty-print false}}]}
               :aot :all}})
