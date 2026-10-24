/*
 * Tetris Prime v4.46.0 — PWA service worker.
 *
 * Strategy:
 *  - navigations (HTML): network-first, fall back to the cached shell so the
 *    game still opens offline (it is 100% client-side, no API needed to play).
 *  - immutable build assets (/_next/static/**): cache-first (hashed filenames).
 *  - game media (/sfx/**, /voices/**) + icons/fonts: cache-first with a soft
 *    cap per bucket so a long play session cannot fill the disk unbounded.
 *  - /api/**: network only (downloads, ads endpoints) — never cached.
 *
 * Bump CACHE on every release so stale shells refresh immediately.
 */
const CACHE = 'tetris-prime-v4.58.0';
const SHELL = '/'; // '/' serves the game HTML
const MEDIA_CAP = 160; // max entries per media bucket (sfx ~25, voices ~100+)

self.addEventListener('install', event => {
  event.waitUntil(
    caches
      .open(CACHE)
      .then(c => c.addAll([SHELL]))
      .then(() => self.skipWaiting()),
  );
});

self.addEventListener('activate', event => {
  event.waitUntil(
    caches
      .keys()
      .then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k))))
      .then(() => self.clients.claim()),
  );
});

async function trimBucket(name, cap) {
  const cache = await caches.open(name);
  const keys = await cache.keys();
  if (keys.length <= cap) return;
  // delete the oldest entries first
  for (let i = 0; i < keys.length - cap; i++) await cache.delete(keys[i]);
}

function bucketFor(url) {
  if (url.pathname.startsWith('/sfx/')) return `${CACHE}-sfx`;
  if (url.pathname.startsWith('/voices/')) return `${CACHE}-voices`;
  return CACHE;
}

self.addEventListener('fetch', event => {
  const req = event.request;
  if (req.method !== 'GET') return;
  const url = new URL(req.url);
  if (url.origin !== self.location.origin) return;
  if (url.pathname.startsWith('/api/')) return; // network only

  // navigations: network-first, cached shell offline
  if (req.mode === 'navigate') {
    event.respondWith(
      fetch(req)
        .then(res => {
          const copy = res.clone();
          caches.open(CACHE).then(c => c.put(SHELL, copy));
          return res;
        })
        .catch(() => caches.match(SHELL).then(r => r || Response.error())),
    );
    return;
  }

  // everything else: cache-first (immutable assets + media), then network
  const bucket = bucketFor(url);
  event.respondWith(
    caches.match(req, { ignoreSearch: url.pathname.startsWith('/sfx/') || url.pathname.startsWith('/voices/') }).then(
      hit =>
        hit ||
        fetch(req).then(res => {
          if (res && res.status === 200 && (res.type === 'basic' || res.type === 'default')) {
            const copy = res.clone();
            caches.open(bucket).then(c => c.put(req, copy));
            if (bucket !== CACHE) trimBucket(bucket, MEDIA_CAP);
          }
          return res;
        }),
    ),
  );
});
