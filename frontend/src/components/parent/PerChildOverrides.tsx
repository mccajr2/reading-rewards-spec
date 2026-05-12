import { useState } from 'react';
import type { PerChildRewards, RewardTemplateDraft } from '../../services/rewardApi';

type PerChildOverridesProps = {
  groups: PerChildRewards[];
  onCreate: (childId: string, draft: RewardTemplateDraft) => Promise<void>;
};

export function PerChildOverrides({ groups, onCreate }: PerChildOverridesProps) {
  const [descriptionByChild, setDescriptionByChild] = useState<Record<string, string>>({});

  return (
    <section aria-label="Per-child reward overrides">
      <h2>Per-Child Rewards</h2>
      {groups.map((group) => (
        <article key={group.childId}>
          <h3>{group.childName}</h3>
          <ul>
            {group.rewards.map((reward) => (
              <li key={reward.rewardTemplateId}>{reward.description}</li>
            ))}
          </ul>
          <form
            onSubmit={async (event) => {
              event.preventDefault();
              const description = descriptionByChild[group.childId] ?? '';
              if (!description.trim()) {
                return;
              }
              await onCreate(group.childId, {
                childId: group.childId,
                rewardType: 'MONEY',
                amount: 1,
                unit: 'PER_CHAPTER',
                frequency: 'IMMEDIATE',
                cannedTemplateId: null,
                description,
              });
              setDescriptionByChild((prev) => ({ ...prev, [group.childId]: '' }));
            }}
          >
            <label>
              New reward for {group.childName}
              <input
                value={descriptionByChild[group.childId] ?? ''}
                onChange={(event) =>
                  setDescriptionByChild((prev) => ({
                    ...prev,
                    [group.childId]: event.target.value,
                  }))
                }
                placeholder="e.g. $1 per chapter"
              />
            </label>
            <button type="submit">Add Override</button>
          </form>
        </article>
      ))}
    </section>
  );
}
