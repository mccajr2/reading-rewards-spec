import type { CannedRewardTemplate } from '../../services/rewardApi';

type CannedRewardCatalogProps = {
  templates: CannedRewardTemplate[];
  onAdd: (template: CannedRewardTemplate) => Promise<void>;
};

export function CannedRewardCatalog({ templates, onAdd }: CannedRewardCatalogProps) {
  return (
    <section aria-label="Canned reward catalog">
      <h2>Suggested Rewards</h2>
      <ul>
        {templates.map((template) => (
          <li key={template.cannedTemplateId}>
            <span>{template.description}</span>{' '}
            <button type="button" onClick={() => void onAdd(template)}>Add</button>
          </li>
        ))}
      </ul>
    </section>
  );
}
