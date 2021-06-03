(ns commonmark-hiccup.core
  "Library for converting markdown to HTML. Uses commonmark-java
  (https://github.com/atlassian/commonmark-java) for parsing and
  renders to hiccup (https://github.com/weavejester/hiccup) data
  structures. The renderer itself is quite configurable."
  (:require [hiccup.core :as hiccup]
            [clojure.walk :as walk])
  (:import org.commonmark.parser.Parser))

(def default-config
  {:renderer {:nodes {org.commonmark.node.Document          :content
                      org.commonmark.node.Heading           ['(:h :node-level) :content]
                      org.commonmark.node.Paragraph         [:p :content]
                      org.commonmark.node.Text              :node-literal
                      org.commonmark.node.BulletList        [:ul :content]
                      org.commonmark.node.OrderedList       [:ol {:start :node-startNumber} :content]
                      org.commonmark.node.ListItem          [:li :content]
                      org.commonmark.node.BlockQuote        [:blockquote :content]
                      org.commonmark.node.HtmlBlock         :node-literal
                      org.commonmark.node.HtmlInline        :node-literal
                      org.commonmark.node.FencedCodeBlock   [:pre [:code {:class :node-info} :node-literal]]
                      org.commonmark.node.IndentedCodeBlock [:pre [:code :node-literal]]
                      org.commonmark.node.Code              [:code :node-literal]
                      org.commonmark.node.Link              [:a {:href :node-destination} :content]
                      org.commonmark.node.Image             [:img {:src   :node-destination
                                                                   :alt   :text-content
                                                                   :title :node-title}]
                      org.commonmark.node.Emphasis          [:em :content]
                      org.commonmark.node.StrongEmphasis    [:strong :content]
                      org.commonmark.node.ThematicBreak     [:hr]
                      org.commonmark.node.SoftLineBreak     " "
                      org.commonmark.node.HardLineBreak     [:br]}}})

(defn- children
  "Returns a seq of the children of a commonmark-java AST node."
  [node]
  (take-while some? (iterate #(.getNext %) (.getFirstChild node))))

(defn- text-content
  "Recursively walks over the given commonmark-java AST node depth-first,
  extracting and concatenating literals from any text nodes it visits."
  [node]
  (->> (tree-seq (constantly true) children node)
       (filter #(instance? org.commonmark.node.Text %))
       (map #(.getLiteral %))
       (apply str)))

(defn property-map [node]
  (into {} (for [[k v] (dissoc (bean node) :class)]
             [(keyword (str "node-" (name k))) v])))

(defmulti node-properties
  "Returns the map representation of a commonmark-java AST node. Property names
  are prefixed with \"node-\"."
  class)
(defmethod node-properties :default [node] (property-map node))
(defmethod node-properties org.commonmark.node.FencedCodeBlock [node]
  (-> (property-map node)
      (update :node-literal hiccup.util/escape-html)
      (update :node-info not-empty)))
(defmethod node-properties org.commonmark.node.IndentedCodeBlock [node]
  (update (property-map node) :node-literal hiccup.util/escape-html))
(defmethod node-properties org.commonmark.node.Code [node]
  (update (property-map node) :node-literal hiccup.util/escape-html))
(defmethod node-properties org.commonmark.node.OrderedList [node]
  (update (property-map node) :node-startNumber #(when (< 1 %) %)))
(defmethod node-properties org.commonmark.node.ListItem [node]
  (let [parent (.getParent node)
        tight? (and (instance? org.commonmark.node.ListBlock parent)
                    (.isTight parent))]
    (assoc (property-map node) :content (if tight? :content-tight :content))))

(defn- string-fuse
  "Takes a seq and joins its elements into a single string. If a keyword
  is in the first position, its name is used instead of the keyword itself."
  [s]
  (apply str (cons (name (first s)) (rest s))))

(defn- render-node [config node]
  (let [html-config     (get-in config [:renderer :nodes (class node)])
        render-children (fn [n] (map (partial render-node config) (children n)))]
    (->> html-config
         (walk/postwalk-replace (node-properties node))
         (walk/postwalk #(if (= :content %) (render-children node) %))
         (walk/postwalk #(if (= :content-tight %) (render-children (first (children node))) %))
         (walk/postwalk #(if (= :text-content %) (text-content node) %))
         (walk/postwalk #(if (list? %) (string-fuse %) %)))))

(defn- parse-markdown [s]
  (let [parser (.build (Parser/builder))]
    (.parse parser s)))

(defn markdown->hiccup
  "Takes a string of markdown and converts it to HTML. Optionally takes a configuration
  map, allowing customization of the Hiccup output."
  ([s]        (markdown->hiccup default-config s))
  ([config s] (render-node config (parse-markdown s))))

(defn markdown->html
  "Takes a string of markdown and converts it to HTML. Optionally takes a configuration
  map, allowing customization of the HTML output."
  ([s]        (markdown->html default-config s))
  ([config s] (hiccup/html (markdown->hiccup config s))))

