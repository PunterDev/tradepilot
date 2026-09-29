import { Link } from "react-router-dom";
import type { BreadcrumbItem } from "../../seo/structuredData";

export function Breadcrumbs({ items }: { items: BreadcrumbItem[] }) {
  return (
    <nav className="marketing-breadcrumbs" aria-label="Breadcrumb">
      <ol>
        {items.map((item, index) => {
          const current = index === items.length - 1;
          return (
            <li key={item.path}>
              {current ? <span aria-current="page">{item.label}</span> : <Link to={item.path}>{item.label}</Link>}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
