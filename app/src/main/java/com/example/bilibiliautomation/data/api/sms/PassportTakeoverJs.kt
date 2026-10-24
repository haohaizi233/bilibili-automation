package com.example.bilibiliautomation.data.api.sms

/**
 * Official passport scripts. Humans drag the GeeTest slider; we only
 * locate DOM, fill fields, and raise the captcha node.
 */
internal object PassportTakeoverJs {
    val hook: String = """
(function(){
  if (window.__biliHook) return;
  window.__biliHook = true;
  function tap(kind, payload) {
    try { if (window.BiliGee) BiliGee.onPageEvent(kind, payload || ''); } catch (e) {}
  }
  var open = XMLHttpRequest.prototype.open;
  var send = XMLHttpRequest.prototype.send;
  XMLHttpRequest.prototype.open = function(method, url) {
    this.__biliUrl = String(url || '');
    return open.apply(this, arguments);
  };
  XMLHttpRequest.prototype.send = function(body) {
    var xhr = this;
    xhr.addEventListener('load', function() {
      var u = xhr.__biliUrl || '';
      var text = xhr.responseText || '';
      if (u.indexOf('sms/send') !== -1) tap('sms_send', text);
      if (u.indexOf('login/sms') !== -1) tap('sms_login', text);
      if (u.indexOf('captcha') !== -1 || u.indexOf('geetest') !== -1) tap('captcha_net', (u + ' ' + String(xhr.status)));
    });
    return send.apply(this, arguments);
  };
  if (window.fetch) {
    var orig = window.fetch;
    window.fetch = function() {
      var args = arguments;
      var input = args[0];
      var url = (typeof input === 'string') ? input : ((input && input.url) || '');
      var p = orig.apply(this, args);
      p.then(function(res) {
        try {
          if (url.indexOf('sms/send') !== -1 || url.indexOf('login/sms') !== -1) {
            res.clone().text().then(function(text) {
              if (url.indexOf('sms/send') !== -1) tap('sms_send', text);
              if (url.indexOf('login/sms') !== -1) tap('sms_login', text);
            });
          }
        } catch (e) {}
      });
      return p;
    };
  }
})();
""".trimIndent()

    fun fillPhone(phone: String): String {
        val phoneJs = org.json.JSONObject.quote(phone)
        return """
(function(){
  var phone = $phoneJs;
  if (!phone) return 'empty';
  var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value');
  var n = 0;
  document.querySelectorAll('input').forEach(function(el){
    var hint = ((el.placeholder || '') + (el.name || '') + (el.type || '') + (el.id || '')).toLowerCase();
    if (!(el.type === 'tel' || hint.indexOf('phone') !== -1 || hint.indexOf('tel') !== -1 || (el.placeholder || '').indexOf('手机') !== -1)) return;
    el.focus();
    if (setter && setter.set) setter.set.call(el, phone);
    else el.value = phone;
    try {
      el.dispatchEvent(new InputEvent('input', {bubbles:true, composed:true, data: phone, inputType:'insertFromPaste'}));
    } catch (e) {
      el.dispatchEvent(new Event('input', {bubbles:true, composed:true}));
    }
    el.dispatchEvent(new Event('change', {bubbles:true, composed:true}));
    n += 1;
  });
  return String(n);
})();
""".trimIndent()
    }

    fun queryTargets(focus: String = "none"): String {
        val focusJs = org.json.JSONObject.quote(focus)
        return """
(function(){
  var focus = $focusJs;
  function textOf(el){ return ((el.innerText || el.textContent || '') + '').replace(/\s+/g,''); }
  function shown(el){
    if (!el) return false;
    var r = el.getBoundingClientRect();
    if (r.width < 4 || r.height < 4) return false;
    var cs = window.getComputedStyle(el);
    if (cs.display === 'none' || cs.visibility === 'hidden' || cs.opacity === '0' || cs.pointerEvents === 'none') return false;
    return true;
  }
  function pack(el){
    if (!el) return null;
    var r = el.getBoundingClientRect();
    var iw = window.innerWidth || 1;
    var ih = window.innerHeight || 1;
    return {
      nx: (r.left + r.width/2) / iw,
      ny: (r.top + r.height/2) / ih,
      w: Math.round(r.width),
      h: Math.round(r.height),
      t: textOf(el).slice(0, 40),
      tag: el.tagName,
      cls: (el.className && el.className.toString) ? el.className.toString().slice(0, 80) : ''
    };
  }
  function findText(substr, maxLen){
    var best = null, bestLen = 1e9;
    var nodes = document.querySelectorAll('a,button,div,span,p,label,i,em,li');
    for (var i = 0; i < nodes.length; i++) {
      var el = nodes[i];
      if (!shown(el)) continue;
      var t = textOf(el);
      if (!t || t.indexOf(substr) === -1 || t.length > maxLen) continue;
      if (t.length < bestLen) { best = el; bestLen = t.length; }
    }
    return best;
  }
  function isChecked(el){
    if (!el) return false;
    if (el.checked === true) return true;
    if ((el.getAttribute && el.getAttribute('aria-checked')) === 'true') return true;
    var cls = (el.className && el.className.toString) ? el.className.toString() : '';
    return cls.indexOf('checked') !== -1;
  }
  function findAgreeBox(){
    var label = findText('我已阅读', 120);
    if (!label) return null;
    var root = label.tagName === 'A' ? label.parentElement : label;
    for (var up = 0; up < 6 && root; up++) {
      var kids = root.querySelectorAll('input[type=checkbox],i,svg,img,em,span,div,b');
      for (var k = 0; k < kids.length; k++) {
        var n = kids[k];
        if (n.tagName === 'A') continue;
        if (!shown(n)) continue;
        if (textOf(n).length > 4) continue;
        var r = n.getBoundingClientRect();
        if (r.width >= 8 && r.width <= 44 && r.height >= 8 && r.height <= 44) return n;
      }
      root = root.parentElement;
    }
    return label;
  }
  function findCaptcha(){
    var sel = '[class*="geetest"],[id*="geetest"],iframe[src*="geetest"],iframe[src*="captcha"]';
    var nodes = document.querySelectorAll(sel);
    for (var i = 0; i < nodes.length; i++) {
      var r = nodes[i].getBoundingClientRect();
      if (r.width > 60 && r.height > 40) return nodes[i];
    }
    return findText('向右滑动', 40) || findText('请完成验证', 40) || findText('请完成滑块', 24);
  }
  var captcha = findCaptcha();
  if (captcha) {
    document.documentElement.classList.add('bili-slider-only');
    if (!document.getElementById('bili-takeover-style')) {
      var style = document.createElement('style');
      style.id = 'bili-takeover-style';
      style.textContent = 'html.bili-slider-only,html.bili-slider-only body{background:#f4f7fb!important;overflow:hidden!important;}html.bili-slider-only body>*{visibility:hidden!important;}html.bili-slider-only [class*="geetest"],html.bili-slider-only [class*="geetest"] *,html.bili-slider-only iframe[src*="geetest"],html.bili-slider-only iframe[src*="captcha"]{visibility:visible!important;opacity:1!important;pointer-events:auto!important;position:relative!important;z-index:2147483647!important;}';
      document.documentElement.appendChild(style);
    }
    try { if (window.BiliGee) BiliGee.onPageEvent('captcha_visible', ''); } catch (e) {}
  }
  var agreeEl = findAgreeBox();
  var sendEl = findText('获取验证码', 16);
  if (!captcha && focus === 'agree' && agreeEl) agreeEl.scrollIntoView({block:'center', inline:'nearest'});
  if (!captcha && focus === 'send' && sendEl) sendEl.scrollIntoView({block:'center', inline:'nearest'});
  return JSON.stringify({
    send: pack(sendEl),
    agree: pack(agreeEl),
    agreed: isChecked(agreeEl),
    captcha: pack(captcha)
  });
})();
""".trimIndent()
    }

    fun injectCodeAndSubmit(code: String): String {
        val codeJs = org.json.JSONObject.quote(code)
        return """
(function(){
  var code = $codeJs;
  document.documentElement.classList.remove('bili-slider-only');
  var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value');
  document.querySelectorAll('input').forEach(function(el){
    var ph = el.placeholder || '';
    if (ph.indexOf('验证码') === -1 || ph.indexOf('手机') !== -1) return;
    el.focus();
    if (setter && setter.set) setter.set.call(el, code);
    else el.value = code;
    el.dispatchEvent(new Event('input', {bubbles:true, composed:true}));
    el.dispatchEvent(new Event('change', {bubbles:true, composed:true}));
  });
  var nodes = document.querySelectorAll('a,button,div,span');
  for (var j = 0; j < nodes.length; j++) {
    var t = ((nodes[j].innerText || nodes[j].textContent || '') + '').replace(/\s+/g,'');
    if (t === '登录') { nodes[j].click(); break; }
  }
})();
""".trimIndent()
    }
}
