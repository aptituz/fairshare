const CACHE_VERSION = "v1";
const ASSET_CACHE = `fairshare-assets-${CACHE_VERSION}`;
const RUNTIME_CACHE = `fairshare-runtime-${CACHE_VERSION}`;

const ASSET_DESTINATIONS = new Set(["style", "script", "image", "font"]);

self.addEventListener("install", (event) => {
  event.waitUntil(self.skipWaiting());
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    (async () => {
      const cacheNames = await caches.keys();
      await Promise.all(
        cacheNames
          .filter((name) =>
            name.startsWith("fairshare-") &&
            ![ASSET_CACHE, RUNTIME_CACHE].includes(name)
          )
          .map((name) => caches.delete(name))
      );
      await self.clients.claim();
    })()
  );
});

self.addEventListener("fetch", (event) => {
  const { request } = event;
  if (request.method !== "GET") {
    return;
  }

  const url = new URL(request.url);
  const isSameOrigin = url.origin === self.location.origin;
  const destination = request.destination;

  if (!isSameOrigin && !/fonts\.(googleapis|gstatic)\.com/.test(url.hostname)) {
    return;
  }

  if (destination === "document") {
    event.respondWith(networkFirst(request));
    return;
  }

  if (ASSET_DESTINATIONS.has(destination)) {
    event.respondWith(staleWhileRevalidate(request, ASSET_CACHE));
    return;
  }

  if (!isSameOrigin) {
    event.respondWith(staleWhileRevalidate(request, RUNTIME_CACHE));
  }
});

async function networkFirst(request) {
  const cache = await caches.open(RUNTIME_CACHE);
  try {
    const response = await fetch(request);
    if (response && response.status === 200) {
      cache.put(request, response.clone());
    }
    return response;
  } catch (error) {
    const cached = await cache.match(request);
    if (cached) {
      return cached;
    }
    throw error;
  }
}

async function staleWhileRevalidate(request, cacheName) {
  const cache = await caches.open(cacheName);
  const cachedResponse = await cache.match(request);
  const fetchPromise = fetch(request).then((response) => {
    if (response && response.status === 200) {
      cache.put(request, response.clone());
    }
    return response;
  });

  return cachedResponse || fetchPromise;
}
