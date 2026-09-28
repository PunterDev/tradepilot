import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const canonicalOrigin = "https://www.tradepilotcoach.com";
const privateRoutePatterns = [
  /^\/(?:login|register|dashboard|admin|billing|settings|journal|analytics|calendar|coach|verify-email|forgot-password|reset-password)(?:\/|$)/,
  /^\/trades(?:\/|$)/,
];

const routes = JSON.parse(await readFile(path.join(projectRoot, "seo-public-routes.json"), "utf8"));
const sitemap = await readFile(path.join(projectRoot, "public", "sitemap.xml"), "utf8");
const vercel = JSON.parse(await readFile(path.join(projectRoot, "vercel.json"), "utf8"));
const errors = [];

function duplicateValues(values) {
  const seen = new Set();
  return [...new Set(values.filter((value) => (seen.has(value) ? true : (seen.add(value), false))))];
}

function isPrivateRoute(route) {
  return privateRoutePatterns.some((pattern) => pattern.test(route));
}

function outputDestination(route) {
  return `${route}/index.html`;
}

if (!Array.isArray(routes)) {
  throw new Error("seo-public-routes.json must contain a JSON array.");
}

const registryDuplicates = duplicateValues(routes);
if (registryDuplicates.length) {
  errors.push(`Duplicate registry routes: ${registryDuplicates.join(", ")}`);
}

for (const route of routes) {
  if (typeof route !== "string" || !route.startsWith("/") || route.includes("?") || route.includes("#")) {
    errors.push(`Invalid registry route: ${String(route)}`);
  }
}

const sitemapUrls = [...sitemap.matchAll(/<loc>\s*([^<]+?)\s*<\/loc>/gi)].map((match) => match[1].trim());
const sitemapDuplicates = duplicateValues(sitemapUrls);
if (sitemapDuplicates.length) {
  errors.push(`Duplicate sitemap URLs: ${sitemapDuplicates.join(", ")}`);
}

const sitemapPaths = [];
for (const value of sitemapUrls) {
  try {
    const url = new URL(value);
    if (url.protocol !== "https:" || url.origin !== canonicalOrigin || url.search || url.hash) {
      errors.push(`Non-canonical sitemap URL: ${value}`);
    }
    sitemapPaths.push(url.pathname);
  } catch {
    errors.push(`Invalid sitemap URL: ${value}`);
  }
}

const privateSitemapPaths = sitemapPaths.filter(isPrivateRoute);
if (privateSitemapPaths.length) {
  errors.push(`Private/auth routes in sitemap: ${privateSitemapPaths.join(", ")}`);
}

const missingSitemapRoutes = routes.filter((route) => !sitemapPaths.includes(route));
const unexpectedSitemapRoutes = sitemapPaths.filter((route) => !routes.includes(route));
if (missingSitemapRoutes.length) {
  errors.push(`Registry routes missing from sitemap: ${missingSitemapRoutes.join(", ")}`);
}
if (unexpectedSitemapRoutes.length) {
  errors.push(`Unexpected sitemap routes: ${unexpectedSitemapRoutes.join(", ")}`);
}

const rewrites = Array.isArray(vercel.rewrites) ? vercel.rewrites : [];
const missingRewrites = routes
  .filter((route) => route !== "/")
  .filter((route) => !rewrites.some((rewrite) => rewrite.source === route && rewrite.destination === outputDestination(route)));
if (missingRewrites.length) {
  errors.push(`Missing Vercel prerender rewrites: ${missingRewrites.join(", ")}`);
}

console.log("SEO route audit");
console.log(`  Indexable registry: ${routes.length}`);
console.log(`  Sitemap URLs: ${sitemapUrls.length}`);
console.log(`  Missing sitemap URLs: ${missingSitemapRoutes.length}`);
console.log(`  Unexpected sitemap URLs: ${unexpectedSitemapRoutes.length}`);
console.log(`  Missing Vercel prerender routes: ${missingRewrites.length}`);
console.log(`  Private sitemap URLs: ${privateSitemapPaths.length}`);
console.log(`  Duplicate registry routes: ${registryDuplicates.length}`);
console.log(`  Duplicate sitemap URLs: ${sitemapDuplicates.length}`);

if (errors.length) {
  console.error("SEO route audit failed:");
  for (const error of errors) console.error(`  - ${error}`);
  process.exitCode = 1;
} else {
  console.log("SEO route audit passed.");
}
