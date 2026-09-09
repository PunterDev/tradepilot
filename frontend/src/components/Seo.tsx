import { Helmet } from "react-helmet-async";

export const SITE_URL = "https://www.tradepilotcoach.com";
const DEFAULT_OG_IMAGE = `${SITE_URL}/assets/tradepilot-logo-homepage.png`;

type SeoProps = {
  title: string;
  description: string;
  path?: string;
  canonical?: string;
  robots?: string;
  ogTitle?: string;
  ogDescription?: string;
  ogUrl?: string;
  image?: string;
  twitterCard?: "summary" | "summary_large_image";
  structuredData?: Record<string, unknown>;
};

function absoluteUrl(path = "/") {
  if (/^https?:\/\//i.test(path)) {
    return path;
  }
  return `${SITE_URL}${path === "/" ? "" : path}`;
}

export default function Seo({
  title,
  description,
  path = "/",
  canonical,
  robots,
  ogTitle,
  ogDescription,
  ogUrl,
  image = DEFAULT_OG_IMAGE,
  twitterCard = "summary_large_image",
  structuredData,
}: SeoProps) {
  const canonicalUrl = absoluteUrl(canonical ?? path);
  const resolvedOgTitle = ogTitle ?? title;
  const resolvedOgDescription = ogDescription ?? description;
  const resolvedOgUrl = absoluteUrl(ogUrl ?? canonical ?? path);
  const resolvedImage = absoluteUrl(image);

  return (
    <Helmet>
      <title>{title}</title>
      <meta name="description" content={description} />
      {robots && <meta name="robots" content={robots} />}
      <link rel="canonical" href={canonicalUrl} />

      <meta property="og:title" content={resolvedOgTitle} />
      <meta property="og:description" content={resolvedOgDescription} />
      <meta property="og:type" content="website" />
      <meta property="og:url" content={resolvedOgUrl} />
      <meta property="og:image" content={resolvedImage} />

      <meta name="twitter:card" content={twitterCard} />
      <meta name="twitter:title" content={resolvedOgTitle} />
      <meta name="twitter:description" content={resolvedOgDescription} />
      <meta name="twitter:image" content={resolvedImage} />

      {structuredData && (
        <script type="application/ld+json">
          {JSON.stringify(structuredData)}
        </script>
      )}
    </Helmet>
  );
}
