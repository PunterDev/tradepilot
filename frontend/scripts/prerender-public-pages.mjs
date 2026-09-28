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
  const titles = [...html.matchAll(/<title\b[^>]*>([\s\S]*?)<\/title>/gi)].map((match) => match[1].trim());
  const metaTags = html.match(/<meta\b[^>]*>/gi) ?? [];
  const linkTags = html.match(/<link\b[^>]*>/gi) ?? [];
  const h1s = [...html.matchAll(/<h1\b[^>]*>([\s\S]*?)<\/h1>/gi)].map((match) => match[1].replace(/<[^>]+>/g, " ").trim());
  const descriptionTags = metaTags.filter((tag) => /\bname=["']description["']/i.test(tag));
  const canonicalTags = linkTags.filter((tag) => /\brel=["']canonical["']/i.test(tag));
  const robotsTags = metaTags.filter((tag) => /\bname=["']robots["']/i.test(tag));
  const canonicalHref = canonicalTags[0]?.match(/\bhref=["']([^"']+)["']/i)?.[1];
  const expectedCanonical = new URL(`https://www.tradepilotcoach.com${route}`);
  const renderedText = html
    .replace(/<script\b[^>]*>[\s\S]*?<\/script>/gi, "")
    .replace(/<style\b[^>]*>[\s\S]*?<\/style>/gi, "")
    .replace(/<[^>]+>/g, " ")
    .replace(/\s+/g, " ")
    .trim();

  if (titles.length !== 1 || !titles[0]) {
    throw new Error(`${route} prerender must contain exactly one non-empty title.`);
  }
  if (descriptionTags.length !== 1 || !/\bcontent=["'][^"']+[^"']["']/i.test(descriptionTags[0])) {
    throw new Error(`${route} prerender must contain exactly one non-empty meta description.`);
  }
  const parsedCanonical = canonicalHref ? new URL(canonicalHref) : null;
  if (
    canonicalTags.length !== 1 ||
    !parsedCanonical ||
    parsedCanonical.origin !== expectedCanonical.origin ||
    parsedCanonical.pathname !== expectedCanonical.pathname ||
    parsedCanonical.search ||
    parsedCanonical.hash
  ) {
    throw new Error(`${route} prerender must contain exactly one canonical matching ${expectedCanonical.href}.`);
  }
  if (h1s.length !== 1 || !h1s[0]) {
    throw new Error(`${route} prerender must contain exactly one non-empty H1.`);
  }
  if (robotsTags.some((tag) => /\bcontent=["'][^"']*noindex/i.test(tag))) {
    throw new Error(`${route} prerender must not emit noindex.`);
  }
  if (renderedText.length < 160 || /(?:loading|checking your session)\.\.\./i.test(renderedText)) {
    throw new Error(`${route} prerender is missing useful rendered body content.`);
  }
  const expectsStructuredData = !["/privacy", "/terms", "/contact"].includes(route);
  if (expectsStructuredData && !/<script\s+type="application\/ld\+json"/i.test(html)) {
    throw new Error(`${route} prerender is missing structured data.`);
  }
}

const titles = new Set();
for (const route of routes) {
  const rendered = renderPublicRoute(route);
  const html = injectPrerenderedMarkup(template, route, rendered);
  ensurePrerenderedHtml(route, html);
  const title = html.match(/<title\b[^>]*>([\s\S]*?)<\/title>/i)?.[1].trim();
  if (titles.has(title)) {
    throw new Error(`${route} prerender duplicates the title: ${title}`);
  }
  titles.add(title);

  const outputPath = routeOutputPath(route);
  await mkdir(path.dirname(outputPath), { recursive: true });
  await writeFile(outputPath, html);
  console.log(`prerendered ${route}`);
}
