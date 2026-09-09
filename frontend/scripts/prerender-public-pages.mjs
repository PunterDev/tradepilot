import { spawn } from "node:child_process";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { chromium } from "@playwright/test";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const distDir = path.join(projectRoot, "dist");
const routes = JSON.parse(await readFile(path.join(projectRoot, "seo-public-routes.json"), "utf8"));
const port = Number(process.env.PRERENDER_PORT ?? 4173);
const origin = `http://127.0.0.1:${port}`;
const viteBin = path.join(projectRoot, "node_modules", ".bin", process.platform === "win32" ? "vite.cmd" : "vite");

function waitForServer(processHandle) {
  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => {
      reject(new Error("Timed out waiting for Vite preview during prerender."));
    }, 30_000);

    processHandle.stdout.on("data", (chunk) => {
      const text = chunk.toString();
      if (text.includes(`:${port}`) || text.includes(`127.0.0.1:${port}`)) {
        clearTimeout(timeout);
        resolve();
      }
    });

    processHandle.once("exit", (code) => {
      clearTimeout(timeout);
      reject(new Error(`Vite preview exited before prerender could run. Code: ${code}`));
    });
  });
}

function routeOutputPath(route) {
  if (route === "/") {
    return path.join(distDir, "index.html");
  }
  return path.join(distDir, route.replace(/^\/+/, ""), "index.html");
}

function ensurePrerenderedHtml(route, html) {
  if (!/<title>[^<]+<\/title>/i.test(html)) {
    throw new Error(`${route} prerender is missing a title.`);
  }
  if (!/<meta\s+name="description"\s+content="[^"]+"/i.test(html)) {
    throw new Error(`${route} prerender is missing a meta description.`);
  }
  if (!/<link\s+rel="canonical"\s+href="https:\/\/www\.tradepilotcoach\.com/i.test(html)) {
    throw new Error(`${route} prerender is missing a www.tradepilotcoach.com canonical.`);
  }
  if (!/<h1[\s>]/i.test(html)) {
    throw new Error(`${route} prerender is missing an H1.`);
  }
  if (!/TradePilot/i.test(html)) {
    throw new Error(`${route} prerender is missing meaningful TradePilot body copy.`);
  }
  const expectsStructuredData = !["/privacy", "/terms", "/contact"].includes(route);
  if (expectsStructuredData && !/<script\s+type="application\/ld\+json"/i.test(html)) {
    throw new Error(`${route} prerender is missing structured data.`);
  }
  if (route !== "/" && html.includes('<link rel="canonical" href="https://www.tradepilotcoach.com">')) {
    throw new Error(`${route} prerender has a homepage canonical.`);
  }
}

async function normalizeHead(page, route) {
  const expectedCanonical = `https://www.tradepilotcoach.com${route === "/" ? "" : route}`;
  await page.evaluate((canonical) => {
    const titleText = document.title;
    document.querySelectorAll("title").forEach((element) => element.remove());
    const title = document.createElement("title");
    title.textContent = titleText;
    document.head.appendChild(title);

    for (const selector of [
      'meta[name="description"]',
      'meta[property="og:title"]',
      'meta[property="og:description"]',
      'meta[property="og:type"]',
      'meta[property="og:image"]',
      'meta[name="twitter:card"]',
      'meta[name="twitter:title"]',
      'meta[name="twitter:description"]',
      'meta[name="twitter:image"]',
    ]) {
      const elements = Array.from(document.querySelectorAll(selector));
      const last = elements.at(-1);
      elements.forEach((element) => element.remove());
      if (last) {
        document.head.appendChild(last);
      }
    }

    document.querySelectorAll('link[rel="canonical"]').forEach((element) => {
      if (element.getAttribute("href") !== canonical) {
        element.remove();
      }
    });
    document.querySelectorAll('meta[property="og:url"]').forEach((element) => {
      if (element.getAttribute("content") !== canonical) {
        element.remove();
      }
    });
  }, expectedCanonical);
}

const server = spawn(viteBin, ["preview", "--host", "127.0.0.1", "--port", String(port)], {
  cwd: projectRoot,
  env: {
    ...process.env,
    VITE_API_BASE_URL: process.env.VITE_API_BASE_URL ?? "http://127.0.0.1:8080/api",
  },
  stdio: ["ignore", "pipe", "pipe"],
});

server.stderr.on("data", (chunk) => {
  process.stderr.write(chunk);
});

try {
  await waitForServer(server);

  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();

  for (const route of routes) {
    const separator = route.includes("?") ? "&" : "?";
    await page.goto(`${origin}${route}${separator}__prerender=1`, { waitUntil: "networkidle" });
    await page.waitForSelector("#root h1");
    await page.waitForFunction(() => document.title.length > 0 && document.querySelector('link[rel="canonical"]'));
    await normalizeHead(page, route);

    const html = (await page.content()).replaceAll(origin, "");
    ensurePrerenderedHtml(route, html);

    const outputPath = routeOutputPath(route);
    await mkdir(path.dirname(outputPath), { recursive: true });
    await writeFile(outputPath, html);
    console.log(`prerendered ${route}`);
  }

  await browser.close();
} finally {
  server.kill();
}
