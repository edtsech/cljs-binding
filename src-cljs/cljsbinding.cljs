(ns cljsbinding
  (:use [jayq.core :only [$ attr val change]])
)

(def BindMonitor (atom false))
(def BindDependencies (atom {}))
(def BindFn (atom nil))

(defn translate [data]
  (if (map? data) (make-js-map data) data)
)

(defn bind-elem [elem data]  
  (let [f #(.call (aget elem (first data)) elem (translate(js/eval (second data))))]
    (reset! BindMonitor true)
    (reset! BindFn f)
    (f)
    (reset! BindMonitor false)
))

(defn make-js-map
  "makes a javascript map from a clojure one"
  [cljmap]
  (let [out (js-obj)]
    (doall (map #(aset out (name (first %)) (second %)) cljmap))
    out))

(defn bind [elem]
 (doseq [data (.split (attr elem "bind") ";")] (bind-elem elem (.split data ":")))
)

(defn bindatom [elem]
  (bind-elem elem ["val" (str "cljs.core.deref.call(null," (attr elem "bindatom") ")") ])
  (.change elem #(
    (reset! (js/eval (attr elem "bindatom")) (.val elem))
    :false
  ))
)

(defn ^:export init []
  (doseq [elem ($ "*[bind]")] (bind elem))
  (doseq [elem ($ "*[bindatom]")] (bindatom elem))
  )

(defn seq-contains?
  "Determine whether a sequence contains a given item"
  [sequence item]
  (if (empty? sequence)
    false
    (reduce #(or %1 %2) (map #(= %1 item) sequence))))  

(defn ^:export register [atom]
  (reset! BindMonitor false)
  (swap! BindDependencies
    #(assoc % atom (if (contains? % atom) 
      (cons @BindFn (% atom))
      [@BindFn]))
    )  
  (add-watch atom :binding-watch
          (fn [key a old-val new-val] 
            (doseq [f (@BindDependencies a)] (f))
          )
        )
  (reset! BindMonitor true)
)


