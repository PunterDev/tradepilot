import { Helmet } from "react-helmet-async";

const SITE_URL = "https://www.tradepilotcoach.com";
const DEFAULT_OG_IMAGE = `${SITE_URL}/assets/tradepilot-logo-homepage.png`;

type SeoProps = {
  title: string;
  description: string;
  path?: string;
  image?: string;
  structuredData?: Record<string, unknown>;
};

function absoluteUrl(path = "/") {
  return `${SITE_URL}${path === "/" ? "" : path}`;
}

export default function Seo({ title, description, path = "/", image = DEFAULT_OG_IMAGE, structuredData }: SeoProps) {
  const canonicalUrl = absoluteUrl(path);

  return (
    <Helmet>
      <title>{title}</title>
      <meta name="description" content={description} />
      <link rel="canonical" href={canonicalUrl} />

      <meta property="og:title" content={title} />
      <meta property="og:description" content={description} />
      <meta property="og:type" content="website" />
      <meta property="og:url" content={canonicalUrl} />
      <meta property="og:image" content={image} />

      <meta name="twitter:card" content="summary_large_image" />
      <meta name="twitter:title" content={title} />
      <meta name="twitter:description" content={description} />
      <meta name="twitter:image" content={image} />

      {structuredData && (
        <script type="application/ld+json">
          {JSON.stringify(structuredData)}
        </script>
      )}
    </Helmet>
  );
}
