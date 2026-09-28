import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const distDir = path.join(projectRoot, "dist");
const canonicalOrigin = "https://www.tradepilotcoach.com";
const lowPriorityRoutes = new Set(["/privacy", "/terms", "/contact"]);
const privateRoutePatterns = [
  /^\/(?:login|register|dashboard|admin|billing|settings|journal|analytics|calendar|coach|trades|verify-email|forgot-password|reset-password|dev-tools|playbook|session-review|sessions|trading-dna|leak-detector|journey)(?:\/|$)/,
];
const requiredLinks = new Map([
  ["/ai-trading-journal", ["/futures-trading-journal", "/trading-psychology"]],
  ["/futures-trading-journal", ["/ai-trading-journal", "/topstep-trading-journal", "/tradovate-trading-journal", "/prop-firm-trading-journal", "/overtrading"]],
  ["/topstep-trading-journal", ["/futures-trading-journal", "/ai-trading-journal", "/prop-firm-trading-journal", "/trading-after-losses"]],
  ["/tradovate-trading-journal", ["/futures-trading-journal", "/ai-trading-journal", "/trading-psychology"]],
  ["/prop-firm-trading-journal", ["/futures-trading-journal", "/topstep-trading-journal", "/ai-trading-journal", "/trading-discipline", "/breaking-trading-rules"]],
  ["/trading-psychology", ["/revenge-trading", "/overtrading", "/trading-discipline", "/breaking-trading-rules", "/trading-after-losses", "/ai-trading-journal"]],
  ["/revenge-trading", ["/trading-after-losses", "/tools/revenge-trading-calculator", "/trading-discipline", "/ai-trading-journal"]],
  ["/overtrading", ["/tools/overtrading-calculator", "/trading-discipline", "/trading-after-losses", "/futures-trading-journal"]],
  ["/breaking-trading-rules", ["/trading-discipline", "/revenge-trading", "/trading-after-losses", "/trading-psychology"]],
  ["/tools/trading-expectancy-calculator", ["/futures-trading-journal", "/ai-trading-journal", "/trading-psychology"]],
]);
const bidirectionalPairs = [
  ["/revenge-trading", "/tools/revenge-trading-calculator"],
  ["/overtrading", "/tools/overtrading-calculator"],
];
const priorityRoutes = [
  "/ai-trading-journal",
  "/futures-trading-journal",
  "/topstep-trading-journal",
  "/tradovate-trading-journal",
  "/prop-firm-trading-journal",
  "/trading-psychology",
];
const journalClusterSources = new Set([
  "/ai-trading-journal",
  "/futures-trading-journal",
  "/topstep-trading-journal",
  "/tradovate-trading-journal",
  "/prop-firm-trading-journal",
]);
const behaviorClusterSources = new Set([
  "/trading-psychology",
  "/revenge-trading",
  "/overtrading",
  "/breaking-trading-rules",
]);

const routes = JSON.parse(await readFile(path.join(projectRoot, "seo-public-routes.json"), "utf8"));
const routeSet = new Set(routes);
const outbound = new Map(routes.map((route) => [route, new Set()]));
const inbound = new Map(routes.map((route) => [route, new Set()]));
const errors = [];

function outputPath(route) {
  return route === "/" ? path.join(distDir, "index.html") : path.join(distDir, route.slice(1), "index.html");
}

function normalizeInternalHref(href) {
  if (!href || href.startsWith("#") || /^(?:mailto:|tel:|javascript:)/i.test(href)) return null;
  let url;
  try {
    url = new URL(href, canonicalOrigin);
  } catch {
    return null;
  }
  if (url.origin !== canonicalOrigin) return null;
  const pathname = url.pathname === "/" ? "/" : url.pathname.replace(/\/+$/, "");
  return pathname;
}

function isPrivateRoute(route) {
  return privateRoutePatterns.some((pattern) => pattern.test(route));
}

for (const source of routes) {
  let html;
  try {
    html = await readFile(outputPath(source), "utf8");
  } catch {
    errors.push(`Missing prerendered HTML for ${source}: ${outputPath(source)}`);
    continue;
  }

  const hrefs = [...html.matchAll(/<a\b[^>]*\bhref=["']([^"']+)["'][^>]*>/gi)].map((match) => match[1]);
  for (const href of hrefs) {
    const target = normalizeInternalHref(href);
    if (!target || isPrivateRoute(target)) continue;
    if (!routeSet.has(target)) {
      errors.push(`${source} links to an unknown public route: ${href}`);
      continue;
    }
    if (target === source) continue;
    outbound.get(source).add(target);
    inbound.get(target).add(source);
  }
}

const orphans = routes.filter((route) => !lowPriorityRoutes.has(route) && inbound.get(route).size === 0);
if (orphans.length) errors.push(`Orphan indexable routes: ${orphans.join(", ")}`);

for (const [source, targets] of requiredLinks) {
  for (const target of targets) {
    if (!outbound.get(source)?.has(target)) errors.push(`Required link missing: ${source} -> ${target}`);
  }
}

for (const [guide, tool] of bidirectionalPairs) {
  if (!outbound.get(guide)?.has(tool) || !outbound.get(tool)?.has(guide)) {
    errors.push(`Bidirectional guide/tool link missing: ${guide} <-> ${tool}`);
  }
}

for (const route of priorityRoutes) {
  if (inbound.get(route).size < 4) {
    errors.push(`Priority route has fewer than four distinct inbound sources: ${route} (${inbound.get(route).size})`);
  }
}

const missingRequiredLinks = [...requiredLinks].flatMap(([source, targets]) =>
  targets.filter((target) => !outbound.get(source)?.has(target)).map((target) => [source, target])
);
const journalMissing = missingRequiredLinks.filter(([source]) => journalClusterSources.has(source));
const behaviorMissing = missingRequiredLinks.filter(([source]) => behaviorClusterSources.has(source));

console.log("Internal link audit");
console.log(`  Total indexable routes: ${routes.length}`);
for (const route of routes) {
  console.log(`  ${route}: outbound=${outbound.get(route).size}, inbound=${inbound.get(route).size}`);
}
console.log(`  Orphan pages: ${orphans.length}`);
console.log(`  Broken internal links: ${errors.filter((error) => error.includes("unknown public route") || error.includes("Missing prerendered HTML")).length}`);
console.log(`  Journal-cluster connectivity: ${journalMissing.length === 0 ? "passed" : `missing=${journalMissing.length}`}`);
console.log(`  Behavior-cluster connectivity: ${behaviorMissing.length === 0 ? "passed" : `missing=${behaviorMissing.length}`}`);
console.log(`  Guide/calculator bidirectional relationships: ${bidirectionalPairs.length} passed`);

if (errors.length) {
  console.error("Internal link audit failed:");
  for (const error of errors) console.error(`  - ${error}`);
  process.exitCode = 1;
} else {
  console.log("Internal link audit passed.");
}
