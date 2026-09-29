export const SITE_URL = "https://www.tradepilotcoach.com";
export const ORGANIZATION_ID = `${SITE_URL}/#organization`;
export const WEBSITE_ID = `${SITE_URL}/#website`;
export const LOGO_ID = `${SITE_URL}/#logo`;
export const LOGO_URL = `${SITE_URL}/assets/tradepilot-coach-logo.png`;

export type BreadcrumbItem = {
  label: string;
  path: string;
};

type PageSchemaInput = {
  path: string;
  title: string;
  description: string;
  breadcrumbs?: BreadcrumbItem[];
};

export function absoluteSiteUrl(path = "/") {
  return `${SITE_URL}${path === "/" ? "" : path}`;
}

export function organizationSchema() {
  return {
    "@type": "Organization",
    "@id": ORGANIZATION_ID,
    name: "TradePilot Coach",
    url: SITE_URL,
    description: "AI-powered trading performance and execution intelligence for futures traders.",
    logo: {
      "@type": "ImageObject",
      "@id": LOGO_ID,
      url: LOGO_URL,
      contentUrl: LOGO_URL,
      caption: "TradePilot Coach",
    },
  };
}

export function websiteSchema() {
  return {
    "@type": "WebSite",
    "@id": WEBSITE_ID,
    url: SITE_URL,
    name: "TradePilot Coach",
    publisher: { "@id": ORGANIZATION_ID },
  };
}

export function breadcrumbSchema(items: BreadcrumbItem[]) {
  const pageUrl = absoluteSiteUrl(items[items.length - 1]?.path ?? "/");
  return {
    "@type": "BreadcrumbList",
    "@id": `${pageUrl}#breadcrumb`,
    itemListElement: items.map((item, index) => ({
      "@type": "ListItem",
      position: index + 1,
      name: item.label,
      item: absoluteSiteUrl(item.path),
    })),
  };
}

export function webPageSchema({ path, title, description, breadcrumbs }: PageSchemaInput) {
  const pageUrl = absoluteSiteUrl(path);
  return {
    "@type": "WebPage",
    "@id": `${pageUrl}#webpage`,
    url: pageUrl,
    name: title,
    description,
    isPartOf: { "@id": WEBSITE_ID },
    publisher: { "@id": ORGANIZATION_ID },
    ...(breadcrumbs ? { breadcrumb: { "@id": `${pageUrl}#breadcrumb` } } : {}),
  };
}

export function pageStructuredData(input: PageSchemaInput) {
  const graph: Record<string, unknown>[] = [webPageSchema(input)];
  if (input.breadcrumbs) graph.push(breadcrumbSchema(input.breadcrumbs));
  return { "@context": "https://schema.org", "@graph": graph };
}

export function homepageStructuredData(input: Omit<PageSchemaInput, "path" | "breadcrumbs">) {
  return {
    "@context": "https://schema.org",
    "@graph": [organizationSchema(), websiteSchema(), webPageSchema({ ...input, path: "/" })],
  };
}
