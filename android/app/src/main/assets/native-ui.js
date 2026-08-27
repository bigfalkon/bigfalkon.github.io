(function () {
  if (window.__androidNativeUiReady) return;
  window.__androidNativeUiReady = true;

  /* ── 1. Donanım geri tuşu: önce açık katmanı kapat ───────────────────────── */
  var LAYER_SELECTOR =
    '#enlarged-overlay, [id$="-overlay"], [id$="-modal"], [id$="-menu"], [role="dialog"]';

  function isVisible(el) {
    if (!el || el.classList.contains('hidden')) return false;
    var cs = getComputedStyle(el);
    if (cs.display === 'none' || cs.visibility === 'hidden' || cs.opacity === '0') return false;
    return el.getBoundingClientRect().height > 0;
  }

  window.__androidBack = function () {
    var layers = Array.prototype.filter.call(
      document.querySelectorAll(LAYER_SELECTOR),
      isVisible
    );
    if (!layers.length) return false;
    // En üstteki katmanı (en yüksek z-index / DOM'da en sondaki) kapat.
    var top = layers[layers.length - 1];
    var closeBtn = top.querySelector(
      '.icon-btn, [aria-label="Close"], [data-close], button[onclick*="close"]'
    );
    if (closeBtn) closeBtn.click();
    else top.classList.add('hidden');
    return true;
  };

  /* ── 2. blob:/data: indirmeleri native "İndirilenler" klasörüne yaz ──────── */
  document.addEventListener(
    'click',
    function (e) {
      var a = e.target && e.target.closest && e.target.closest('a[download]');
      if (!a || !a.href) return;
      if (!/^(blob:|data:)/.test(a.href)) return;
      if (!window.AndroidApp || !window.AndroidApp.saveBase64) return;
      e.preventDefault();
      var name = a.getAttribute('download') || '';
      fetch(a.href)
        .then(function (r) { return r.blob(); })
        .then(function (b) {
          var fr = new FileReader();
          fr.onloadend = function () {
            window.AndroidApp.saveBase64(fr.result, b.type || '', name);
          };
          fr.readAsDataURL(b);
        })
        .catch(function () { window.AndroidApp.downloadFailed(); });
    },
    true
  );

  /* ── 3. Çift dokunuşla istem dışı yakınlaştırmayı yumuşat ────────────────── */
  var lastTouch = 0;
  document.addEventListener(
    'touchend',
    function (e) {
      var now = Date.now();
      if (now - lastTouch <= 300 && !e.target.closest('img')) e.preventDefault();
      lastTouch = now;
    },
    { passive: false }
  );
})();
