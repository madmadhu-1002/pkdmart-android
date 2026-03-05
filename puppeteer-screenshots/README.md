# Puppeteer Screenshots - pkdmart.com

This utility crawls `https://pkdmart.com` and captures full-page screenshots for all discovered internal pages.

## Run

```bash
cd C:\Users\ih20162\AndroidStudioProjects\pkdmart\puppeteer-screenshots
npm install
npm run shot
```

Optional page cap:

```bash
MAX_PAGES=500 npm run shot
```

## Output

- `screenshots/*.png` (all page screenshots)
- `screenshots/visited-urls.json` (crawl list)
