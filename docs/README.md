# Loadstone website

The public site for Loadstone, served by GitHub Pages straight out of this folder.

| File | URL |
|---|---|
| `index.html` | https://prempoddar.github.io/Loadstone/ |
| `privacy/index.html` | https://prempoddar.github.io/Loadstone/privacy |
| `open/index.html` | https://prempoddar.github.io/Loadstone/open?lat=…&lng=…&n=…&d=… |

## Enabling Pages

In this repository: **Settings → Pages → Source: Deploy from a branch → Branch: `main`, folder: `/docs`**.
Give it a minute, then open https://prempoddar.github.io/Loadstone/.

`.nojekyll` is present so files are published as-is, without Jekyll processing.

## Shared links

Sharing a pin sends a link like:

```
https://prempoddar.github.io/Loadstone/open?lat=37.42&lng=-122.08&n=Camp&d=by%20the%20gate
```

The link opens `open/index.html`, which shows the pin's name, note and coordinates and offers an
**Open in Loadstone** button. That button uses the `loadstone://open?…` custom scheme, which needs no
domain verification, so it reaches the app whenever it is installed.

Photos are never included in a shared link — only the name, note and coordinates.

### Why links do not open the app automatically

Android only hands an `https` link straight to an app if it can verify the domain, which requires

```
https://prempoddar.github.io/.well-known/assetlinks.json
```

Only a **user site** (a repository named `prempoddar.github.io`) can serve files at the domain root.
This is a project site, so the file cannot be published where Android looks for it — `assetlinks.json`
is kept in `.well-known/` here for reference only.

Two ways to get automatic opening, if you want it later:

1. **Create a `prempoddar.github.io` repository** and put `.well-known/assetlinks.json` in its root,
   with the app's signing SHA-256 from *Play Console → Test and release → App integrity → App signing*.
   The app already declares the matching `autoVerify` intent filter, so it starts working on install.
2. **Per-device, no hosting:** Settings → Apps → Loadstone → *Open by default* → add the
   `prempoddar.github.io` link.

Check the current state on a connected device:

```bash
adb shell pm get-app-links com.prempoddar.loadstone
```

## Google Play

Set the privacy policy URL in **Play Console → Policy → App content** to:

```
https://prempoddar.github.io/Loadstone/privacy
```

Then complete the Data safety form to match the app: no data collected, no data shared. Location is
used only in the foreground and photos stay on the device.

## Editing

Plain HTML and CSS with no build step — edit and push. Colours and type match the app: deep indigo
`#0B0D16`, accent `#5B6BF0`, Space Grotesk for display, IBM Plex Sans for text, IBM Plex Mono for data.
If the app's behaviour changes, update `privacy/index.html` and the Play Data safety form together.
