import { createServer } from "node:http";
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

const contentTypes = new Map([
  [".css", "text/css; charset=utf-8"],
  [".html", "text/html; charset=utf-8"],
  [".ico", "image/x-icon"],
  [".js", "text/javascript; charset=utf-8"],
  [".json", "application/json; charset=utf-8"],
  [".map", "application/json; charset=utf-8"],
  [".png", "image/png"],
  [".svg", "image/svg+xml"],
  [".webp", "image/webp"],
  [".woff", "font/woff"],
  [".woff2", "font/woff2"],
]);

function safeStaticPath(pathname) {
  const relativePath = path.posix.normalize(pathname).replace(/^\/+/, "") || "index.html";
  const candidate = path.resolve(distDir, relativePath);
  if (candidate !== distDir && !candidate.startsWith(`${distDir}${path.sep}`)) {
    return null;
  }
  return { candidate, hasExtension: path.extname(relativePath).length > 0 };
}

async function serveStaticFile(request, response) {
  const requestUrl = new URL(request.url ?? "/", origin);
  const resolved = safeStaticPath(requestUrl.pathname);
  if (!resolved) {
    response.writeHead(400, { "Content-Type": "text/plain; charset=utf-8" });
    response.end("Invalid static path");
    return;
  }

  let filePath = resolved.candidate;
  try {
    const body = await readFile(filePath);
    response.writeHead(200, {
      "Content-Type": contentTypes.get(path.extname(filePath)) ?? "application/octet-stream",
      "Cache-Control": "no-store",
    });
    response.end(body);
  } catch (error) {
    if (resolved.hasExtension || error?.code !== "ENOENT") {
      response.writeHead(404, { "Content-Type": "text/plain; charset=utf-8" });
      response.end("Static file not found");
      return;
    }

    filePath = path.join(distDir, "index.html");
    try {
      const body = await readFile(filePath);
      response.writeHead(200, {
        "Content-Type": "text/html; charset=utf-8",
        "Cache-Control": "no-store",
      });
      response.end(body);
    } catch (fallbackError) {
      response.writeHead(500, { "Content-Type": "text/plain; charset=utf-8" });
      response.end(`Unable to serve prerender fallback: ${fallbackError?.code ?? "unknown error"}`);
    }
  }
}

function startStaticServer() {
  const server = createServer((request, response) => {
    serveStaticFile(request, response).catch((error) => {
      response.writeHead(500, { "Content-Type": "text/plain; charset=utf-8" });
      response.end(`Prerender static server error: ${error instanceof Error ? error.message : "unknown error"}`);
    });
  });

  return new Promise((resolve, reject) => {
    const onError = (error) => {
      reject(new Error(`Prerender static server failed to bind ${origin}: ${error.code ?? error.message}`));
    };
    server.once("error", onError);
    server.listen(port, "127.0.0.1", () => {
      server.off("error", onError);
      resolve(server);
    });
  });
}

async function waitForServer(server) {
  const deadline = Date.now() + 45_000;
  let lastError = "no response";

  while (Date.now() < deadline) {
    if (!server.listening) {
      throw new Error(`Prerender static server stopped before readiness: ${lastError}`);
    }
    try {
      const response = await fetch(origin, { signal: AbortSignal.timeout(2_000) });
      if (response.status >= 200 && response.status < 400) {
        return;
      }
      lastError = `HTTP ${response.status}`;
    } catch (error) {
      lastError = error instanceof Error ? error.message : "unknown request error";
    }
    await new Promise((resolve) => setTimeout(resolve, 250));
  }

  throw new Error(`Timed out waiting for prerender static server at ${origin}: ${lastError}`);
}

function closeServer(server) {
  if (!server?.listening) {
    return Promise.resolve();
  }
  return new Promise((resolve, reject) => {
    server.close((error) => (error ? reject(error) : resolve()));
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

let server;
let browser;
try {
  server = await startStaticServer();
  await waitForServer(server);

  browser = await chromium.launch({ headless: true });
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
} finally {
  await browser?.close();
  await closeServer(server);
}
