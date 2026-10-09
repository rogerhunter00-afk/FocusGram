(() => {
  if (window.__focusgramDMs) return;
  window.__focusgramDMs = true;
  const allowed = p => /^\/(direct(?:\/|$)|accounts\/|challenge\/|checkpoint\/|two_factor\/|consent\/)/.test(p);
  const guard = () => {
    if (!allowed(location.pathname)) location.replace('/direct/inbox/');
  };
  // Install the route guard before modifying visual navigation. No chat content
  // or credentials are read or sent to a native JavaScript interface.
  for (const name of ['pushState', 'replaceState']) {
    const original = history[name];
    history[name] = function(state, unused, url) {
      if (url != null) {
        const dest = new URL(url, location.href);
        if (dest.origin === location.origin && !allowed(dest.pathname)) {
          location.replace('/direct/inbox/'); return;
        }
      }
      const result = original.apply(this, arguments);
      guard(); return result;
    };
  }
  addEventListener('popstate', guard);
  addEventListener('pageshow', guard);
  document.addEventListener('click', event => {
    const a = event.target.closest && event.target.closest('a[href]');
    if (!a) return;
    const url = new URL(a.href, location.href);
    if (url.origin === location.origin && !allowed(url.pathname)) {
      event.preventDefault(); event.stopImmediatePropagation();
    }
  }, true);
  const css = `
    a[href="/"], a[href^="/reels"], a[href^="/explore"],
    a[href^="/stories"], a[href^="/create"], a[href^="/p/"],
    a[href^="/reel/"], a[href^="/notifications"],
    [aria-label="Home"], [aria-label="Reels"], [aria-label="Explore"],
    [aria-label="Stories"] { display:none !important; }
    html { overscroll-behavior:none; }
  `;
  function hideNavigation() {
    if (!document.head) return;
    let style = document.getElementById('focusgram-dm-style');
    if (!style) {
      style = document.createElement('style'); style.id = 'focusgram-dm-style';
      style.textContent = css; document.head.appendChild(style);
    }
    // Only remove links outside messages/auth. Do not remove the inbox's
    // Search field, attachments, message images or audio controls.
    for (const a of document.querySelectorAll('a[href]')) {
      try {
        const u = new URL(a.href, location.href);
        if (u.origin === location.origin && !allowed(u.pathname)) a.hidden = true;
      } catch (_) {}
    }
    guard();
  }
  let scheduled = false;
  new MutationObserver(() => {
    if (scheduled) return;
    scheduled = true;
    setTimeout(() => { scheduled = false; hideNavigation(); }, 100);
  }).observe(document, {childList:true, subtree:true});
  hideNavigation();
})();
