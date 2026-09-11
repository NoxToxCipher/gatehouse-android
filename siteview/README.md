# Gatehouse — Site View (photorealistic 3D "God's Eye")

A photorealistic, flyable 3D view of **Post 01 — Hume Doors & Timber, Kingston QLD**
(`-27.6350, 153.1160`), rendered with **CesiumJS + Google Photorealistic 3D Tiles**.
This is the prototype seed for the in-app **SITE** tab, the 3D companion to the 2D
tactical **SCOPE**.

> Coverage confirmed 2026-09-11: Google's photoreal mesh renders the Hume timber
> sheds (sawtooth roofs, rooftop solar) at street resolution. Green light.

## Attribution / licence

Adapted from **[God's Eye View](https://github.com/bilawalsidhu/gods-eye-view)**
by Bilawal Sidhu, used under the **MIT License** (© 2026 Bilawal Sidhu). Reused
patterns: the Google 3D Tiles load (`src/mapStartup.js`), the circular "scope
mask" (`src/scopeMask.js`), and the CRT sensor shader (`src/styles/retro.js`).
Keep this notice if you redistribute.

Google Photorealistic 3D Tiles are shown under the **Google Maps Platform Terms
of Service** — the on-screen Google credit line **must stay visible** whenever the
tiles are displayed (it is, bottom-left; do not hide `#cesium-credits`).

## Run it locally

The Google key is validated by **HTTP referrer**, so serve over `http://localhost`
— don't open the file via `file://` (the referrer won't match and tiles 401).

```bash
cd siteview
python3 -m http.server 8000
# then open http://localhost:8000
```

On first load it asks for a **Google Maps Platform API key** with the **Map Tiles
API** enabled. The key is stored only in your browser's `localStorage`
(`gatehouse_gmaps_key`) — it is never committed and never sent anywhere but Google.

### Getting the key (free for testing)

1. console.cloud.google.com → new project → **APIs & Services → Enable APIs →
   Map Tiles API**.
2. **Credentials → Create credentials → API key.**
3. **Restrict it:** *API restriction* → Map Tiles API only; *Application
   restriction* → HTTP referrers → `http://localhost:*`.
4. **Billing → Budgets & alerts** → set a low cap. The monthly free allotment
   covers low-volume testing at $0, but a card is required and overage bills.

## Controls

- **Drag** to orbit · **scroll** to zoom · **right-drag** to tilt (Cesium defaults).
- **Scope mask (eye)** — the circular vignette. On by default.
- **Auto-orbit** — slow rotation around Post 01; any manual grab cancels it.
  (Off automatically under `prefers-reduced-motion`.)
- **CRT skin** + Pixelation / Distortion / Instability — the God's Eye sensor look.
- **Reframe Post 01** — snap back to the yard.

## How this reaches the Android app

Gatehouse builds without Gradle and already loads HTML in a `WebView`, so the path
is the same as every other view here:

1. **Bundle:** ship this page (and a pinned local copy of Cesium, rather than the
   CDN) as an Android asset, loaded into a `WebView` as the **SITE** tab beside the
   2D **SCOPE**.
2. **Key-proxy (the one fiddly bit):** a `file://`/`https://appassets` WebView
   origin can't satisfy Google's HTTP-referrer restriction, so the key must not sit
   in client code. Front it with a tiny proxy (as GEV does with its dev server) —
   or Gatehouse's existing remote-endpoint pattern — and hold the key in the **site
   book**, alongside the other tokens. The WebView calls the proxy; the proxy holds
   the key.
3. **Overlays:** feed the same **GeoJSON** the Fire / Airspace / Freight engines
   already produce as Cesium entities, so the 3D SITE view and the 2D SCOPE read
   from one source.

## Known limits of this seed

- Not yet wired to live Gatehouse data — it's the basemap + camera + look only.
- Cesium is loaded from its CDN for convenience; pin a local copy before shipping.
- Untested against a live key inside this repo's build environment (Google egress
  is blocked there); validated by mirroring GEV's working load path.
