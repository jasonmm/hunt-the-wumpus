(defproject wumpus-web "0.1.0-SNAPSHOT"
  :description "Hunt The Wumpus On The Web"
  :url ""

  :min-lein-version "2.7.1"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.229"]
                 [org.clojure/core.async "0.2.395"
                  :exclusions [org.clojure/tools.reader]]
                 [reagent "0.6.0"]
                 [metosin/compojure-api "1.1.10"]
                 [ring "1.5.1"]
                 [secretary "1.2.3"]
                 [reagent-utils "0.2.1"]
                 [environ "1.1.0"]
                 [clj-time "0.13.0"]
                 [cljs-ajax "0.5.8"]]

  :plugins [[lein-figwheel "0.5.9"]
            [lein-cljsbuild "1.1.5" :exclusions [[org.clojure/clojure]]]
            [lein-ring "0.8.8"]]

  :main wumpus-web.backend
  :ring {:handler wumpus-web.backend/app}
  :hooks [leiningen.cljsbuild]
  :source-paths ["src/clj" "src/cljs"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild {:builds
              [{:id           "dev"
                :source-paths ["src/cljs"]

                ;; the presence of a :figwheel configuration here
                ;; will cause figwheel to inject the figwheel client
                ;; into your build
                :figwheel {:on-jsload "wumpus-web.core/on-js-reload"
                           ;; :open-urls will pop open your application
                           ;; in the default browser once Figwheel has
                           ;; started and complied your application.
                           ;; Comment this out once it no longer serves you.
                           ;; :open-urls ["http://localhost:3449/index.html"]
}

                :compiler {:main                 wumpus-web.core
                           :asset-path           "js/compiled/out"
                           :output-to            "resources/public/js/compiled/wumpus_web.js"
                           :output-dir           "resources/public/js/compiled/out"
                           :source-map-timestamp true
                           ;; To console.log CLJS data-structures make sure you enable devtools in Chrome
                           ;; https://github.com/binaryage/cljs-devtools
                           :preloads             [devtools.preload dirac.runtime.preload]}}
               ;; This next build is an compressed minified build for
               ;; production. You can build this with:
               ;; lein cljsbuild once min
               {:id           "min"
                :source-paths ["src/cljs"]
                :compiler     {:output-to     "resources/public/js/compiled/wumpus_web.js"
                               :main          wumpus-web.core
                               :optimizations :advanced
                               :pretty-print  false}}]}

  :figwheel {;; :http-server-root "public" ;; default and assumes "resources"
             ;; :server-port 3449 ;; default
             ;; :server-ip "127.0.0.1"

             :css-dirs ["resources/public/css"] ;; watch and update CSS

             ;; Start an nREPL server into the running figwheel process
             ;; :nrepl-port 7888

             ;; Server Ring Handler (optional)
             ;; if you want to embed a ring handler into the figwheel http-kit
             ;; server, this is for simple ring servers, if this

             ;; doesn't work for you just run your own server :) (see lein-ring)

             ;; :ring-handler hello_world.server/handler

             ;; To be able to open files in your editor from the heads up display
             ;; you will need to put a script on your path.
             ;; that script will have to take a file path and a line number
             ;; ie. in  ~/bin/myfile-opener
             ;; #! /bin/sh
             ;; emacsclient -n +$2 $1
             ;;
             ;; :open-file-command "myfile-opener"

             ;; if you are using emacsclient you can just use
             ;; :open-file-command "emacsclient"

             ;; if you want to disable the REPL
             ;; :repl false

             ;; to configure a different figwheel logfile path
             ;; :server-logfile "tmp/logs/figwheel-logfile.log"
};; setting up nREPL for Figwheel and ClojureScript dev
  ;; Please see:
  ;; https://github.com/bhauman/lein-figwheel/wiki/Using-the-Figwheel-REPL-within-NRepl


  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.0"]
                                  [binaryage/dirac "RELEASE"]
                                  [figwheel-sidecar "0.5.9"]
                                  [com.cemerick/piggieback "0.2.1"]]
                   ;; need to add dev source path here to get user.clj loaded
                   :source-paths ["src" "dev"]
                   ;; for CIDER
                   ;; :plugins [[cider/cider-nrepl "0.12.0"]]
                   :repl-options {:nrepl-middleware [dirac.nrepl/middleware] ;; [cemerick.piggieback/wrap-cljs-repl]
                                            }}

             :ubjerjar {:cljsbuild {:builds
                                    [{:source-paths ["src/cljs"]
                                      :compiler     {:optimizations :advanced
                                                     :output-to     "resources/public/js/compiled/wumpus_web.js"
                                                     :pretty-print  false}}]}
                        :aot       :all}})