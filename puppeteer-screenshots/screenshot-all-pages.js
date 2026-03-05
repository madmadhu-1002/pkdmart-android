import fs from "node:fs/promises";
import path from "node:path";
import puppeteer from "puppeteer";

const BASE_URL = "https://pkdmart.com";
const ORIGIN = new URL(BASE_URL).origin;
const MAX_PAGES = Number(process.env.MAX_PAGES || 200);
const OUT_DIR = path.resolve("./screenshots");
const URLS_FILE = path.join(OUT_DIR, "visited-urls.json");

const visited = new Set();
const queued = new Set([BASE_URL]);
const queue = [BASE_URL];

const sanitize = (s) => s.replace(/[^a-zA-Z0-9-_]/g, "_");

function fileNameFromUrl(urlString, index) {
  const u = new URL(urlString);
  const pathname = u.pathname === "/" ? "home" : u.pathname.replace(/^\//, "");
  const query = u.search ? `__q_${u.searchParams.toString()}` : "";
  const raw = `${String(index).padStart(4, "0")}_${pathname}${query}`;
  return `${sanitize(raw)}.png`;
}

function normalizeUrl(href) {
  try {
    const u = new URL(href, BASE_URL);
    u.hash = "";
    if (u.origin !== ORIGIN) return null;
    return u.toString();
  } catch {
    return null;
  }
}

async function ensureDir() {
  await fs.mkdir(OUT_DIR, { recursive: true });
}

async function run() {
  await ensureDir();

  const browser = await puppeteer.launch({ headless: true });
  const page = await browser.newPage();
  await page.setViewport({ width: 1440, height: 2400 });

  let count = 0;

  while (queue.length && visited.size < MAX_PAGES) {
    const current = queue.shift();
    if (!current || visited.has(current)) continue;

    visited.add(current);
    count += 1;

    try {
      console.log(`[${count}] Opening ${current}`);
      await page.goto(current, { waitUntil: "networkidle2", timeout: 60000 });

      const fileName = fileNameFromUrl(current, count);
      const fullPath = path.join(OUT_DIR, fileName);
      await page.screenshot({ path: fullPath, fullPage: true });
      console.log(`Saved screenshot: ${fullPath}`);

      const hrefs = await page.$$eval("a[href]", (anchors) =>
        anchors.map((a) => a.getAttribute("href")).filter(Boolean)
      );

      for (const href of hrefs) {
        const normalized = normalizeUrl(href);
        if (!normalized) continue;
        if (!visited.has(normalized) && !queued.has(normalized)) {
          queued.add(normalized);
          queue.push(normalized);
        }
      }
    } catch (err) {
      console.log(`Failed ${current}: ${err.message}`);
    }
  }

  await fs.writeFile(URLS_FILE, JSON.stringify([...visited], null, 2), "utf8");
  await browser.close();

  console.log(`Done. Screenshots: ${visited.size}`);
  console.log(`Output dir: ${OUT_DIR}`);
}

run().catch((err) => {
  console.error(err);
  process.exit(1);
});
