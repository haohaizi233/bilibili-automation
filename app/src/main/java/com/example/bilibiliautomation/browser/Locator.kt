package com.example.bilibiliautomation.browser

import com.example.bilibiliautomation.util.JsUtils

sealed class Locator {
    abstract val description: String

    data class Css(val selector: String) : Locator() {
        override val description: String = selector
    }

    data class Text(val text: String) : Locator() {
        override val description: String = "text=$text"
    }

    data class Attribute(val name: String, val value: String) : Locator() {
        override val description: String = "[$name=$value]"
    }

    companion object {
        fun css(selector: String): Locator = Css(selector)

        fun text(text: String): Locator = Text(text)

        fun attribute(name: String, value: String): Locator = Attribute(name, value)
    }

    fun toFinderJs(): String = when (this) {
        is Css -> "document.querySelector(${JsUtils.quote(selector)})"
        is Text -> {
            val quoted = JsUtils.quote(text)
            """(function(){
                var nodes = document.querySelectorAll('button, a, label, input, textarea, div, span, p, h1, h2, h3, li');
                for (var i = 0; i < nodes.length; i++) {
                    var n = nodes[i];
                    var t = ((n.innerText || n.textContent || n.value || '') + '').trim();
                    if (t === $quoted) return n;
                }
                return null;
            })()"""
        }
        is Attribute -> {
            "document.querySelector(${JsUtils.quote("[$name=\"$value\"]")})"
        }
    }
}
