import fs from "node:fs";
import path from "node:path";
import process from "node:process";

const SITE_URL = "https://www.tradepilotcoach.com";
const ORGANIZATION_ID = `${SITE_URL}/#organization`;
const WEBSITE_ID = `${SITE_URL}/#website`;
const publicRoutes = JSON.parse(fs.readFileSync(new URL("../seo-public-routes.json", import.meta.url), "utf8"));
const calculatorRoutes = new Set([
  "/tools/trading-expectancy-calculator",
  "/tools/overtrading-calculator",
  "/tools/revenge-trading-calculator",
]);
const utilityRoutes = new Set(["/", "/privacy", "/terms", "/contact", ...calculatorRoutes]);
const failures = [];

function fail(route, message) {
  failures.push(`${route}: ${message}`);
}

function readAttribute(tag, name) {
  return tag.match(new RegExp(`\\b${name}=["']([^"']*)["']`, "i"))?.[1] ?? null;
}

function findTag(html, tagName, predicate) {
  return [...html.matchAll(new RegExp(`<${tagName}\\b[^>]*>`, "gi"))].find((match) => predicate(match[0]));
}

function decodeHtml(value) {
  return value?.replace(/&(?:amp|lt|gt|quot|#x27|#39);/gi, (entity) => ({
    "&amp;": "&",
    "&lt;": "<",
    "&gt;": ">",
    "&quot;": "\"",
    "&#x27;": "'",
    "&#39;": "'",
  })[entity.toLowerCase()] ?? entity);
}

function schemaNodes(documents) {
  return documents.flatMap((document) => Array.isArray(document?.["@graph"]) ? document["@graph"] : [document]);
}

function hasType(node, type) {
  const value = node?.["@type"];
  return Array.isArray(value) ? value.includes(type) : value === type;
}

function routeFile(route) {
  return route === "/" ? path.resolve("dist/index.html") : path.resolve(`dist${route}/index.html`);
}

function isCanonicalSiteUrl(value) {
  return typeof value === "string" && value.startsWith(SITE_URL) && !value.startsWith("http://") && !value.startsWith("https://tradepilotcoach.com");
}

function collectAbsoluteTradePilotUrls(value, results = []) {
  if (typeof value === "string") {
    for (const match of value.matchAll(/https?:\/\/(?:www\.)?tradepilotcoach\.com[^\s"']*/g)) results.push(match[0]);
  } else if (Array.isArray(value)) {
    value.forEach((item) => collectAbsoluteTradePilotUrls(item, results));
  } else if (value && typeof value === "object") {
    Object.values(value).forEach((item) => collectAbsoluteTradePilotUrls(item, results));
  }
  return results;
}

const globalDefinitions = new Map();

for (const route of publicRoutes) {
  const file = routeFile(route);
  if (!fs.existsSync(file)) {
    fail(route, `missing prerendered file ${path.relative(process.cwd(), file)}`);
    continue;
  }

  const html = fs.readFileSync(file, "utf8");
  const title = decodeHtml(html.match(/<title>([\s\S]*?)<\/title>/i)?.[1]?.trim());
  const descriptionTag = findTag(html, "meta", (tag) => readAttribute(tag, "name") === "description");
  const canonicalTag = findTag(html, "link", (tag) => readAttribute(tag, "rel") === "canonical");
  const description = descriptionTag ? decodeHtml(readAttribute(descriptionTag[0], "content")) : null;
  const canonical = canonicalTag ? readAttribute(canonicalTag[0], "href") : null;
  const expectedUrl = `${SITE_URL}${route === "/" ? "" : route}`;

  if (!title) fail(route, "missing title");
  if (!description) fail(route, "missing meta description");
  if (canonical !== expectedUrl) fail(route, `canonical URL must be ${expectedUrl}`);

  const documents = [];
  for (const match of html.matchAll(/<script\b[^>]*type=["']application\/ld\+json["'][^>]*>([\s\S]*?)<\/script>/gi)) {
    try {
      documents.push(JSON.parse(match[1]));
    } catch (error) {
      fail(route, `invalid JSON-LD: ${error.message}`);
    }
  }
  const nodes = schemaNodes(documents);

  for (const url of collectAbsoluteTradePilotUrls(documents)) {
    if (!isCanonicalSiteUrl(url)) fail(route, `non-canonical TradePilot URL in JSON-LD: ${url}`);
  }
  if (nodes.some((node) => hasType(node, "FAQPage"))) fail(route, "FAQPage JSON-LD must not be present");
  if (nodes.some((node) => Object.hasOwn(node, "aggregateRating") || Object.hasOwn(node, "review"))) {
    fail(route, "review or aggregateRating JSON-LD must not be present");
  }

  const ids = new Map();
  for (const node of nodes) {
    if (!node?.["@id"]) continue;
    const serialized = JSON.stringify(node);
    const existing = ids.get(node["@id"]);
    if (existing && existing !== serialized) fail(route, `conflicting duplicate JSON-LD @id ${node["@id"]}`);
    ids.set(node["@id"], serialized);
    if (node["@id"] === ORGANIZATION_ID || node["@id"] === WEBSITE_ID) {
      const globalExisting = globalDefinitions.get(node["@id"]);
      if (globalExisting && globalExisting !== serialized) fail(route, `conflicting global definition for ${node["@id"]}`);
      globalDefinitions.set(node["@id"], serialized);
    }
  }

  const page = nodes.find((node) => hasType(node, "WebPage"));
  const breadcrumb = nodes.find((node) => hasType(node, "BreadcrumbList"));
  if (route === "/") {
    const organization = nodes.find((node) => hasType(node, "Organization"));
    const website = nodes.find((node) => hasType(node, "WebSite"));
    if (!organization || organization["@id"] !== ORGANIZATION_ID) fail(route, "missing canonical Organization node");
    if (!website || website["@id"] !== WEBSITE_ID || website.publisher?.["@id"] !== ORGANIZATION_ID) {
      fail(route, "missing canonical WebSite publisher relationship");
    }
  }

  const requiresPageSchema = !["/privacy", "/terms", "/contact"].includes(route);
  if (requiresPageSchema && !page) fail(route, "missing WebPage JSON-LD");
  if (page) {
    if (page.url !== expectedUrl || page.name !== title || page.description !== description) {
      fail(route, "WebPage URL, name, or description does not match page metadata");
    }
    if (page.isPartOf?.["@id"] !== WEBSITE_ID || page.publisher?.["@id"] !== ORGANIZATION_ID) {
      fail(route, "WebPage must reference the canonical WebSite and Organization");
    }
  }

  const requiresBreadcrumb = !utilityRoutes.has(route);
  if (requiresBreadcrumb && !breadcrumb) fail(route, "missing BreadcrumbList JSON-LD");
  if (breadcrumb) {
    const items = breadcrumb.itemListElement ?? [];
    if (!items.length || items[0]?.name !== "Home" || items[0]?.item !== SITE_URL) fail(route, "breadcrumb must begin with Home");
    if (items.at(-1)?.item !== expectedUrl) fail(route, "breadcrumb must end at the canonical page URL");
    items.forEach((item, index) => {
      if (item.position !== index + 1) fail(route, "breadcrumb positions must be sequential");
      if (!isCanonicalSiteUrl(item.item)) fail(route, `breadcrumb contains a non-canonical URL: ${item.item}`);
      const itemPath = new URL(item.item).pathname;
      if (index > 0 && index < items.length - 1 && !publicRoutes.includes(itemPath)) {
        fail(route, `breadcrumb intermediate route is not public: ${itemPath}`);
      }
    });
    if (page?.breadcrumb?.["@id"] !== breadcrumb["@id"]) fail(route, "WebPage breadcrumb reference is missing or mismatched");
  }

  if (calculatorRoutes.has(route) && (!page || !breadcrumb)) fail(route, "calculator requires WebPage and BreadcrumbList");
}

if (!globalDefinitions.has(ORGANIZATION_ID) || !globalDefinitions.has(WEBSITE_ID)) {
  failures.push("/: Organization and WebSite must be defined on the homepage");
}

if (failures.length) {
  console.error("Structured-data audit failed:\n" + failures.map((failure) => `- ${failure}`).join("\n"));
  process.exit(1);
}

console.log(`Structured-data audit passed: ${publicRoutes.length} public routes, FAQPage nodes: 0.`);
