(ns mecca.view
  (:require [mecca.music :as music]
            [re-frame.core :as rf :refer [subscribe dispatch]]
            [reagent.core :as r]
            [mecca.castle :as castle]
            [mecca.transport :as transport]
            [mecca.editor :as editor :refer [svg-paths]]
            [mecca.mario :as mario]
            [sci.core :as sci]
            [mecca.sci]
            [mecca.sci-editor :as sci-editor :refer [!points points eval-result !result]]
            [nextjournal.clojure-mode.keymap :as keymap]
            [clojure.pprint :as pp]
            [goog.object :as o]
            [clojure.string :as str]
            [clojure.edn :as edn]))

(defn note-guides []
  (let [editor-x (subscribe [:editor-beat-start])]
    (fn []
      (into [:g]
            (for [beat (range 0 9 0.5)]
              (if (= 0
                     (mod (+ (dec @editor-x) beat) 4))
                [:g
                 ;[bar-number (/ beat 4) (+ 296 (* 120 (mod beat 8))) 64 0.05]
                 [:line {:x1 (+ 8 (* 6 beat)) :x2 (+ 8 (* 6 beat))
                         :y1 4 :y2 21 :stroke "orange"
                         :stroke-width 0.25
                         :stroke-dasharray 0.5}]]
                [:line {:x1 (+ 8 (* 6 beat)) :x2 (+ 8 (* 6 beat))
                        :y1 4 :y2 21 :stroke "black"
                        :stroke-width 0.075
                        :stroke-dasharray 0.5}]))))))

(defn eval-all [s]
  (try (sci/eval-string s {:classes {'js goog/global :allow :all}})
       (catch :default e
         (str e))))

(defn note-targets []
  (let [instrument (subscribe [:instrument])
        editor-x (subscribe [:editor-beat-start])
        sharp? (subscribe [:sharp?])]
    (fn []
      (into [:g]
            (for [time (range 0 9 0.5)
                  pitch (range 19)]
              ^{:key [time pitch]}
              [:rect {:transform "translate(6.5,4)"
                      :x (* 6 time)
                      :y (- pitch 1)
                      :height 1 :width 3
                      :stroke "black"
                      :stroke-width 0.2
                      :fill "gray"
                      :visibility "hidden"
                      :opacity 0.2
                      :pointer-events "all"
                      :on-mouse-over #(if-not (and 
                                               @sharp?
                                               (or (= pitch 12)
                                                   (= pitch 15)
                                                   (= pitch 8)
                                                   (= pitch 5)
                                                   (= pitch 1)
                                                   (= pitch 0)))
                                        (dispatch [:update-focus-note [time pitch]]))
                      :on-mouse-out #(dispatch [:update-focus-note [nil nil]])
                      :on-click (let [pitches [84 83 81 79 77 76 74 72 71 69 67 65 64 62 60 59 57 55 53]]
                                  (cond
                                    @(subscribe [:eraser?])
                                    #(do (music/play-sample 18 63)
                                       (dispatch [:remove-note (+ time (dec @editor-x))
                                                  (get pitches pitch)]))
                                    @(subscribe [:repeat?])
                                    #(dispatch [:set-loop-end time])
                                    :else
                                    #(do
                                      (dispatch [:add-note @instrument
                                                  (+ time (dec @editor-x))
                                                  (get pitches pitch)])
                                       (sci-editor/update-editor! 
                                        (str (conj @(subscribe [:notes]) 
                                                   {:instrument @instrument
                                                    :time       (+ time (dec @editor-x))
                                                    :pitch      (get pitches pitch)})))
                                       (sci-editor/update-result!
                                        (str "Output꞉ " (eval-all (str (some-> @!points .-state .-doc str))))))))}])))))

(defn note-cursor []
  (let [focused (subscribe [:focused-note-pos])
        instrument (subscribe [:instrument])
        sharp? (subscribe [:sharp?])]
       (when-not (= @focused [nil nil])
         (let [x (first @focused) y (last @focused)]
           [:g
            (if @sharp? (svg-paths [["black" "M15 46C15 47 14 47 13 47 13 47 12 47 12 46V37L7 39V49C7 49 6 50 6 50 5 50 5 49 5 49V40L3 40C3 40 2 40 2 40 2 40 1 40 1 39V35C1 35 1 34 2 34L5 33V23L3 24C3 24 2 24 2 24 2 24 1 23 1 23V19C1 19 1 18 2 18L5 17V7C5 6 5 6 6 6 6 6 7 6 7 7V16L12 14V4C12 4 13 3 13 3 14 3 15 4 15 4V13L17 13C17 12 17 12 17 12 18 12 18 13 18 14V17C18 18 18 18 17 19L15 20V30L17 29C17 29 17 29 17 29 18 29 18 29 18 30V34C18 34 18 35 17 35L15 36V46ZM7 22V32L12 31V21Z"]]
                                   (+ 68 (* 86 x)) (+ 18 (* 15 y)) 0.07))
            (cond
              @(subscribe [:eraser?])
              [editor/eraser-cursor (+ 36 (* 30 x)) (+ (* 5 y) 20) 0.2]
              @(subscribe [:repeat?])
              [editor/repeat-sign (+ 7 (* 6 x)) 8 0.13]
              :else
              (case @instrument
                1 [mario/mario-icon (+ 2 (* 30 x)) (+ (* 5 y) 9) 0.2]
                2 [mario/shroom (+ 32 (* 30 x)) (+ (* 5 y) 12) 0.2]
                3 [mario/yoshi (+ 32 (* 30 x)) (+ (* 5 y) 10) 0.2]
                4 [mario/star (+ 32 (* 30 x)) (+ (* 5 y) 10) 0.2]
                5 [mario/flower (+ 32 (* 30 x)) (+ (* 5 y) 10) 0.2]
                6 [mario/gb (+ 32 (* 30 x)) (+ (* 5 y) 10) 0.2]
                7 [mario/dog (+ 32 (* 30 x)) (+ (* 5 y) 10) 0.2]
                8 [mario/kitty (+ 32 (* 30 x)) (+ (* 5 y) 10) 0.2]
                9 [mario/pig (+ 32 (* 30 x)) (+ (* 5 y) 10) 0.2]
                10 [mario/swan (+ 32 (* 30 x)) (+ (* 5 y) 10) 0.2]
                11 [mario/face (+ 32 (* 30 x)) (+ (* 5 y) 10) 0.2]
                12 [mario/plane (+ 32 (* 30 x)) (+ (* 5 y) 15) 0.2]
                13 [mario/boat (+ 32  (* 30 x)) (+ (* 5 y) 12) 0.2]
                14 [mario/car (+ 32 (* 30 x)) (+ (* 5 y) 12) 0.2]
                15 [mario/heart (+ 32 (* 30 x)) (+ (* 5 y) 15) 0.2]))]))))

(defn score-notes []
   (let [notes (subscribe [:notes])
         editor-x (subscribe [:editor-beat-start])]
     (into [:g]
           (for [{:keys [time instrument pitch]} @notes
                 :when (<= (dec @editor-x) time (+ 16 (dec @editor-x)))]
                    ^{:key [instrument time pitch]}
                    (let [x (- time (dec @editor-x))
                          pitch-map (zipmap [84 83 81 79 77 76 74 72 71 69 67 65 64 62 60 59 57 55 53]
                                            (range 19))]
                      [:g
                       (if-not (get pitch-map pitch)
                        (svg-paths [["black" "M15 46C15 47 14 47 13 47 13 47 12 47 12 46V37L7 39V49C7 49 6 50 6 50 5 50 5 49 5 49V40L3 40C3 40 2 40 2 40 2 40 1 40 1 39V35C1 35 1 34 2 34L5 33V23L3 24C3 24 2 24 2 24 2 24 1 23 1 23V19C1 19 1 18 2 18L5 17V7C5 6 5 6 6 6 6 6 7 6 7 7V16L12 14V4C12 4 13 3 13 3 14 3 15 4 15 4V13L17 13C17 12 17 12 17 12 18 12 18 13 18 14V17C18 18 18 18 17 19L15 20V30L17 29C17 29 17 29 17 29 18 29 18 29 18 30V34C18 34 18 35 17 35L15 36V46ZM7 22V32L12 31V21Z"]]
                                   (+ 68 (* 86 x)) (+ 18 (* 15 (get pitch-map (dec pitch)))) 0.07))
                      (case instrument
                        1 [mario/mario-note (+ 2 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 9) 0.2]
                        2 [mario/shroom (+ 32 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 12) 0.2]
                        3 [mario/yoshi (+ 32 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 10) 0.2]
                        4 [mario/star (+ 32 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 10) 0.2]
                        5 [mario/flower (+ 32 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 10) 0.2]
                        6 [mario/gb (+ 32 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 10) 0.2]
                        7 [mario/dog (+ 32 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 10) 0.2]
                        8 [mario/kitty (+ 32 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 10) 0.2]
                        9 [mario/pig (+ 32 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 10) 0.2]
                        10 [mario/swan (+ 32 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 10) 0.2]
                        11 [mario/face (+ 32 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 10) 0.2]
                        12 [mario/plane (+ 32 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 15) 0.2]
                        13 [mario/boat (+ 32 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 12) 0.2]
                        14 [mario/car (+ 32 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 12) 0.2]
                        15 [mario/heart (+ 32 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 15) 0.2]
                        [mario/mario-note (+ 2 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 9) 0.2])])))))

(defn editor []
  (let [editor-x (subscribe [:editor-beat-start])
        mario-run (subscribe [:mario-run])]
    (fn []
      (when (= 20 @mario-run)
        (dispatch [:jump-reset]))
      [:svg {:width "100%"
             :view-box "0 0 64 36"
             :style {:cursor "url(images/hand.png),pointer"}}
       [mario/cloud 1 1]
       [mario/hill 40]
       [castle/brick-face 363 18 6]
       [castle/brick-face 348 48 10]
       [mario/mario-sm]
       [editor/current-note-display 47 0 0.22]
       [editor/note-blocks]
       [mario/floor-tile 16]
       [:rect#editorframe
          {:stroke "black"
           :stroke-width 0.2
           :fill "none"
           :height 20 :width 63.5 :x 0.25 :y 14.5}]
       [:g.staff {:transform "translate(0,13.5) scale(1)"}
        [editor/staff-lines]
        [editor/retract-editor 2]
         [editor/treble-clef
          (- 0.8 (* 6 (dec @editor-x)))
          6.3]
        [editor/advance-editor]
        [editor/advance-measure]
        [editor/advance-end]
        [note-targets]
        [note-guides]
        [note-cursor]
        [score-notes]
        (when @(subscribe [:loop-end])
          [editor/repeat-sign (+ 7 (* 6 @(subscribe [:loop-end]))) 8 0.13])]])))

(defn load-song []
  [:input#input
   {:type      "file"
    :on-change
    (fn [e]
      (let [dom    (o/get e "target")
            file   (o/getValueByKeys dom #js ["files" 0])
            reader (js/FileReader.)]
        (.readAsText reader file)
        (set! (.-onload reader)
              #(dispatch [:set-notes
                          (edn/read-string (-> % .-target .-result))]))))}])

(def demo "(defn scale [intervals]
  (reductions + (cycle intervals)))

(def double-harmonic-minor [1 3 1 2 1 3 1])
(def blues [3 2 1 1 3 2])

(defn scale-note [note]
  (nth (reductions + 60 (cycle blues)) note))

(let [notes (range 10)]
  (for [beat (range (count notes))]
    {:instrument 1
     :time beat
     :pitch (scale-note (nth notes beat))}))")

(defn linux? []
  (some? (re-find #"(Linux)|(X11)" js/navigator.userAgent)))

(defn mac? []
  (and (not (linux?))
       (some? (re-find #"(Mac)|(iPhone)|(iPad)|(iPod)" js/navigator.platform))))

(defn key-mapping []
  (cond-> {"ArrowUp" "↑"
           "ArrowDown" "↓"
           "ArrowRight" "→"
           "ArrowLeft" "←"
           "Mod" "Ctrl"}
    (mac?)
    (merge {"Alt" "⌥"
            "Shift" "⇧"
            "Enter" "⏎"
            "Ctrl" "⌃"
            "Mod" "⌘"})))

(defn render-key [key]
  (let [keys (into [] (map #(get ((memoize key-mapping)) % %) (str/split key #"-")))]
    (into [:span]
          (map-indexed (fn [i k]
                         [:<>
                          (when-not (zero? i) [:span " + "])
                          [:kbd.kbd k]]) keys))))

(defn key-bindings-table [keymap]
  [:table.w-full.text-sm
   [:thead
    [:tr.border-t
     [:th.px-3.py-1.align-top.text-left.text-xs.uppercase.font-normal.black-50 "Command"]
     [:th.px-3.py-1.align-top.text-left.text-xs.uppercase.font-normal.black-50 "Keybinding"]
     [:th.px-3.py-1.align-top.text-left.text-xs.uppercase.font-normal.black-50 "Alternate Binding"]
     [:th.px-3.py-1.align-top.text-left.text-xs.uppercase.font-normal.black-50 {:style {:min-width 290}} "Description"]]]
   (into [:tbody]
         (->> keymap
              (sort-by first)
              (map (fn [[command [{:keys [key shift doc]} & [{alternate-key :key}]]]]
                     [:<>
                      [:tr.border-t.hover:bg-gray-100
                       [:td.px-3.py-1.align-top.monospace.whitespace-nowrap [:b (name command)]]
                       [:td.px-3.py-1.align-top.text-right.text-sm.whitespace-nowrap (render-key key)]
                       [:td.px-3.py-1.align-top.text-right.text-sm.whitespace-nowrap (some-> alternate-key render-key)]
                       [:td.px-3.py-1.align-top doc]]
                      (when shift
                        [:tr.border-t.hover:bg-gray-100
                         [:td.px-3.py-1.align-top [:b (name shift)]]
                         [:td.px-3.py-1.align-top.text-sm.whitespace-nowrap.text-right
                          (render-key (str "Shift-" key))]
                         [:td.px-3.py-1.align-top.text-sm]
                         [:td.px-3.py-1.align-top]])]))))])

(defn mecca []
  [:div
   [editor]
   [:div.flex-container
    [:div.flex-item
     [sci-editor/editor demo !points {:eval? true}]]
    [:div.flex-item
     [transport/transport 20 -0.5 0.4]
    [editor/toolbar 0 0]
     [:center
      [:button
       {:on-click #(let [file-blob (js/Blob. [@(subscribe [:notes])] #js {"type" "text/plain"})
                         link (.createElement js/document "a")]
                     (set! (.-href link) (.createObjectURL js/URL file-blob))
                     (.setAttribute link "download" "mecca.txt")
                     (.appendChild (.-body js/document) link)
                     (.click link)
                     (.removeChild (.-body js/document) link))}
       "Download"]
      [load-song]]]]
   [:div
   [key-bindings-table (merge keymap/paredit-keymap* (mecca.sci/keymap* "Alt"))]]])
