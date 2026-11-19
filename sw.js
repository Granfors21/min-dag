/* Min dag – service worker genererad vid bygget (6e993bcddb). Omfattning: /min-dag/ */
const CACHE = "min-dag-6e993bcddb";
const BASE = "/min-dag/";
const INDEX = "/min-dag/index.html";
const PRECACHE = ["/min-dag/assets/index-CVZ-tcwx.css","/min-dag/assets/index-CWOWePDW.js","/min-dag/assets/nunito-latin-400-normal-DKg4f3fz.woff","/min-dag/assets/nunito-latin-400-normal-r8SDr6Up.woff2","/min-dag/assets/nunito-latin-600-normal-Br8yIETf.woff2","/min-dag/assets/nunito-latin-600-normal-Cd0eNu1l.woff","/min-dag/assets/nunito-latin-700-normal-Dort48En.woff2","/min-dag/assets/nunito-latin-700-normal-OcDqTBcA.woff","/min-dag/assets/nunito-latin-800-normal-D-J0wlBY.woff","/min-dag/assets/nunito-latin-800-normal-Dz8SOQK_.woff2","/min-dag/assets/nunito-latin-ext-400-normal-CjMJVfGn.woff","/min-dag/assets/nunito-latin-ext-400-normal-i-8OOpdj.woff2","/min-dag/assets/nunito-latin-ext-600-normal-BKUpuB78.woff","/min-dag/assets/nunito-latin-ext-600-normal-Dd9Zuxh1.woff2","/min-dag/assets/nunito-latin-ext-700-normal-BWeMsAzO.woff2","/min-dag/assets/nunito-latin-ext-700-normal-D4woHhbd.woff","/min-dag/assets/nunito-latin-ext-800-normal-CDcxIxx8.woff","/min-dag/assets/nunito-latin-ext-800-normal-CtU8tJOV.woff2","/min-dag/favicon.svg","/min-dag/icon-maskable.svg","/min-dag/icon.svg","/min-dag/icons/icon-180.png","/min-dag/icons/icon-192.png","/min-dag/icons/icon-512-maskable.png","/min-dag/icons/icon-512.png","/min-dag/index.html","/min-dag/manifest.webmanifest","/min-dag/robots.txt"];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE).then(async (cache) => {
      await cache.addAll(PRECACHE);
      const index = await cache.match(INDEX);
      if (index) await cache.put(BASE, index.clone());
    }).then(() => self.skipWaiting()),
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((key) => key !== CACHE).map((key) => caches.delete(key))))
      .then(() => self.clients.claim()),
  );
});

self.addEventListener("fetch", (event) => {
  const request = event.request;
  if (request.method !== "GET") return;
  let url;
  try {
    url = new URL(request.url);
  } catch {
    return;
  }
  if (url.origin !== self.location.origin) return;
  const inScope = url.pathname === BASE.slice(0, -1) || url.pathname.startsWith(BASE);
  if (!inScope) return;
  // Låt webbläsaren ladda ner APK-filen, cacha den inte i PWA:n.
  if (url.pathname.endsWith(".apk")) return;

  if (request.mode === "navigate") {
    event.respondWith(
      fetch(request)
        .then((response) => {
          const copy = response.clone();
          caches.open(CACHE).then((cache) => {
            cache.put(INDEX, copy);
            cache.put(BASE, response.clone());
          });
          return response;
        })
        .catch(async () => (await caches.match(INDEX)) || (await caches.match(BASE)) || Response.error()),
    );
    return;
  }

  event.respondWith(
    caches.match(request).then((hit) => {
      if (hit) return hit;
      return fetch(request)
        .then((response) => {
          if (response.ok) {
            const copy = response.clone();
            caches.open(CACHE).then((cache) => cache.put(request, copy));
          }
          return response;
        })
        .catch(() => caches.match(request));
    }),
  );
});
