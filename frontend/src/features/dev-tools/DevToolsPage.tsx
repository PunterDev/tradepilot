import DevToolsCard from "../../components/DevToolsCard";
import PageHeader from "../../layout/PageHeader";

type Props = {
  onChanged: () => void;
};

export default function DevToolsPage({ onChanged }: Props) {
  return (
    <>
      <PageHeader
        label="Development"
        title="Dev Tools"
        description="Seed demo data, clear demo data, and mock subscription states in local development."
      />
      <DevToolsCard onChanged={onChanged} />
    </>
  );
}
