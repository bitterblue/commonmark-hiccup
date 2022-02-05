# commonmark-hiccup

![build](https://github.com/bitterblue/commonmark-hiccup/actions/workflows/clojure.yml/badge.svg)

A small Clojure library for converting [CommonMark][1] markdown to HTML. It is
designed to make the HTML output as configurable as possible, relying
on [Hiccup][2] as an intermediary representation.

[1]: http://spec.commonmark.org/
[2]: https://github.com/weavejester/hiccup


## Installation

Add the following dependency to `project.clj`:

    [commonmark-hiccup "0.2.0"]


## Documentation

commonmark-hiccup uses the [commonmark-java][3] parser and implements its own
renderer, which transforms the CommonMark AST to Hiccup-compatible Clojure data
structures. It then uses Hiccup to render them to their final HTML
representation.

commonmark-hiccup is built for configurability, not performance. I use it to
render static content for different web sites, each with different requirements
for how paragraphs, code blocks etc. should look like.

[3]: https://github.com/atlassian/commonmark-java


### Usage

You can convert a markdown string to HTML using `markdown->html`:

```clojure
user=> (require '[commonmark-hiccup.core :refer [markdown->html]])
nil
user=> (markdown->html "This is a *test*.")
"<p>This is a <em>test</em>.</p>"

```

You can pass a configuration to the converter to tweak the output. This example
renders paragraphs without the surrounding `<p></p>` tags:

```clojure
user=> (let [config (update-in commonmark-hiccup.core/default-config
                               [:renderer :nodes org.commonmark.node.Paragraph]
                               (constantly :content))]
         (markdown->html config "This is a *test*."))
"This is a <em>test</em>."
```

### Configuration

The default configuration defines the Hiccup snippets to which the different
CommonMark AST nodes are rendered:

```clojure
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
                      org.commonmark.node.HardLineBreak     [:br]}}
   :parser   {:extensions nil}})
```

The `:nodes` map uses [commonmark-java][3] node classes as keys. The values are
just Clojure data structures. Some keywords and lists are replaced during
rendering:

* All keywords prefixed with `:node-` are replaced with the respective property
  of the rendered node (e.g. `:node-literal` for `org.commonmark.node.HtmlBlock`
  is replaced with the value returned by `HtmlBlock::getLiteral`).
* Some keywords are special: `:content` is replaced with the rendered content
  of the current node's children; `:text-content` is replaced with the
  concatenated content of all `org.commonmark.node.Text` child nodes.
* List elements are joined to strings. This is useful for rendering node
  properties as part of a longer string. `['(:h :node-level) :content]`
  uses the `level` property of the `Heading` node to render the appropriate HTML
  tag (e.g. `<h1></h1>` for level 1 headings).

For the available properties for each node type, refer to
the [commonmark-java][3] sources.


#### Extensions

CommonMark [extensions](https://github.com/commonmark/commonmark-java#extensions)
can be added to the parser by including them in the `[:parser :extensions]`
list.

For example, to add the support for GFM tables:

1. Add the dependency to your project
```clojure
[com.atlassian.commonmark/commonmark-ext-gfm-tables "..."]
```

2. Add the extension to your config along with the new renderers
```clojure
(require '[commonmark-hiccup.core :as md])
(def my-config
     (-> md/default-config
         (update-in [:parser :extensions] conj
                    (org.commonmark.ext.gfm.tables.TablesExtension/create))
         (update-in [:renderer :nodes] merge
                    {org.commonmark.ext.gfm.tables.TableBlock [:table :content]
                     org.commonmark.ext.gfm.tables.TableHead  [:thead :content]
                     org.commonmark.ext.gfm.tables.TableBody  [:tbody :content]
                     org.commonmark.ext.gfm.tables.TableRow   [:tr :content]
                     org.commonmark.ext.gfm.tables.TableCell  [:td :content]})))

(md/markdown->hiccup mt-config "|head1|head2|
|---|---|
|foo|bar|")
=> ([:table
     ([:thead ([:tr ([:td ("head1")] [:td ("head2")])])]
      [:tbody ([:tr ([:td ("foo")] [:td ("bar")])])])])
```


## License

Copyright © 2017 Axel Schüssler

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

