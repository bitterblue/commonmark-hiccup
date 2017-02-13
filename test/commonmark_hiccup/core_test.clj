(ns commonmark-hiccup.core-test
  (:require [clojure.test :refer :all]
            [commonmark-hiccup.core :refer :all]))

(deftest markdown->html-test
  (testing "converts a simple string of markdown to html"
    (are [md html] (= html (markdown->html md))
      "Test. Test."              "<p>Test. Test.</p>"
      "# Test\nTesting test."    "<h1>Test</h1><p>Testing test.</p>"
      "## Second Level Heading"  "<h2>Second Level Heading</h2>"
      "### Third Level Heading"  "<h3>Third Level Heading</h3>"
      "#### 4th Level Heading"   "<h4>4th Level Heading</h4>"
      "##### 5th Level Heading"  "<h5>5th Level Heading</h5>"
      "###### 6th Level Heading" "<h6>6th Level Heading</h6>"))
  (testing "replaces line breaks within text contents with spaces by default"
    (is (= (markdown->html "This is a\nparagraph with line\nbreaks.")
           "<p>This is a paragraph with line breaks.</p>")))
  (testing "renders reference links"
    (is (= (markdown->html "This is a [test][1].\n\n[1]: www.test.tst")
           "<p>This is a <a href=\"www.test.tst\">test</a>.</p>")))
  (testing "renders images"
    (are [md html] (= html (markdown->html md))
      "![A pretty picture](./picture.png)" "<p><img alt=\"A pretty picture\" src=\"./picture.png\" /></p>"
      "![A pretty picture](./picture.png \"A Title\")" "<p><img alt=\"A pretty picture\" src=\"./picture.png\" title=\"A Title\" /></p>"
      "![](./picture.png)" "<p><img alt=\"\" src=\"./picture.png\" /></p>"))
  (testing "renders fenced code blocks"
    (is (= (markdown->html "```\n(def foo \"bar\")\n```")
           "<pre><code>(def foo &quot;bar&quot;)\n</code></pre>"))
    (is (= (markdown->html "```clojure\n(def foo \"bar\")\n```")
           "<pre><code class=\"clojure\">(def foo &quot;bar&quot;)\n</code></pre>"))
    (let [config (update-in default-config
                            [:renderer :nodes org.commonmark.node.FencedCodeBlock]
                            (constantly [:pre {:class '("lang:" :node-info " decode:true")} :node-literal]))]
      (is (= (markdown->html config "```clojure\n(def foo \"bar\")\n```")
             "<pre class=\"lang:clojure decode:true\">(def foo &quot;bar&quot;)\n</pre>"))))
  (testing "renders indented code blocks"
    (is (= (markdown->html "    (def foo \"bar\")")
           "<pre><code>(def foo &quot;bar&quot;)\n</code></pre>")))
  (testing "renders code spans"
    (is (= (markdown->html "Avoid throwing `NullPointerException` explicitly!")
           "<p>Avoid throwing <code>NullPointerException</code> explicitly!</p>"))
    (is (= (markdown->html "Inline code, e.g. `<script>alert('Test');</script>`, should be escaped.")
           "<p>Inline code, e.g. <code>&lt;script&gt;alert(&apos;Test&apos;);&lt;/script&gt;</code>, should be escaped.</p>")))
  (testing "renders emphasis and strong emphasis"
    (is (= (markdown->html "This is _emphasized_ text.")
           "<p>This is <em>emphasized</em> text.</p>"))
    (is (= (markdown->html "This is *emphasized* text.")
           "<p>This is <em>emphasized</em> text.</p>"))
    (is (= (markdown->html "This is **strongly emphasized** text.")
           "<p>This is <strong>strongly emphasized</strong> text.</p>")))
  (testing "renders blockquotes"
    (is (= (markdown->html "> This is a\n> blockquote!")
           "<blockquote><p>This is a blockquote!</p></blockquote>")))
  (testing "renders tight lists"
    (is (= (markdown->html "- foo\n- bar\n- baz")
           "<ul><li>foo</li><li>bar</li><li>baz</li></ul>"))
    (is (= (markdown->html "1. foo\n2. bar")
           "<ol><li>foo</li><li>bar</li></ol>"))
    (is (= (markdown->html "1. foo\n2. bar\n3) baz")
           "<ol><li>foo</li><li>bar</li></ol><ol start=\"3\"><li>baz</li></ol>")))
  (testing "renders loose lists"
    (is (= (markdown->html "- foo\n\n- bar\n\n- baz")
           "<ul><li><p>foo</p></li><li><p>bar</p></li><li><p>baz</p></li></ul>")))
  (testing "renders HTML blocks"
    (is (= (markdown->html "<script>alert('Test');</script>")
           "<script>alert('Test');</script>")))
  (testing "renders inline HTML"
    (is (= (markdown->html "This is <foo>text</bar> surrounded by tags.")
           "<p>This is <foo>text</bar> surrounded by tags.</p>")))
  (testing "renders thematic breaks"
    (is (= (markdown->html "---")
           "<hr />")))
  (testing "renders hard line breaks"
    (is (= (markdown->html "This is a\\\nline with\\\nhard line breaks.")
           "<p>This is a<br />line with<br />hard line breaks.</p>"))))

