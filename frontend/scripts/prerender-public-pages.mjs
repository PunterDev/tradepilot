import { mkdir, readFile, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const distDir = path.join(projectRoot, "dist");
const serverBundlePath = path.join(projectRoot, ".prerender-ssr", "prerender-entry.mjs");
const routes = JSON.parse(await readFile(path.join(projectRoot, "seo-public-routes.json"), "utf8"));
const { renderPublicRoute } = await import(serverBundlePath);
const appTemplate = await readFile(path.join(distDir, "index.html"), "utf8");
const template = appTemplate.includes("</head>")
  ? appTemplate
  : `<!doctype html><html lang="en"><head></head><body>${appTemplate}</body></html>`;

function routeOutputPath(route) {
  return route === "/"
    ? path.join(distDir, "index.html")
    : path.join(distDir, route.replace(/^\/+/, ""), "index.html");
}

function injectPrerenderedMarkup(html, route, rendered) {
  const rootMarkup = `<div id="root">${rendered.body}</div>`;
  const withHead = html.replace("</head>", `${rendered.head}</head>`);
  const withBody = withHead.replace('<div id="root"></div>', rootMarkup);

  if (withBody === html) {
    throw new Error(`${route} prerender could not find the Vite root container.`);
  }
  return withBody;
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

for (const route of routes) {
  const rendered = renderPublicRoute(route);
  const html = injectPrerenderedMarkup(template, route, rendered);
  ensurePrerenderedHtml(route, html);

  const outputPath = routeOutputPath(route);
  await mkdir(path.dirname(outputPath), { recursive: true });
  await writeFile(outputPath, html);
  console.log(`prerendered ${route}`);
}
