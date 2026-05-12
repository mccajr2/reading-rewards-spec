import { useEffect, useState } from 'react';
import {
  listCannedRewardTemplates,
  createParentPerChildReward,
  createParentFamilyReward,
  listParentRewards,
  type CannedRewardTemplate,
  type ParentRewardsResponse,
  type RewardTemplate,
  type RewardTemplateDraft,
} from '../../services/rewardApi';
import { CannedRewardCatalog } from '../../components/parent/CannedRewardCatalog';
import { PerChildOverrides } from '../../components/parent/PerChildOverrides';
import { RewardTemplateBuilder } from '../../components/parent/RewardTemplateBuilder';

export function ManageFamilyRewardsPage() {
  const [state, setState] = useState<ParentRewardsResponse>({ familyRewards: [], perChildRewards: [] });
  const [cannedTemplates, setCannedTemplates] = useState<CannedRewardTemplate[]>([]);
  const [error, setError] = useState<string | null>(null);

  async function loadRewards() {
    try {
      setError(null);
      const rewards = await listParentRewards();
      setState(rewards);
      const templates = await listCannedRewardTemplates();
      setCannedTemplates(templates);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load rewards');
    }
  }

  useEffect(() => {
    void loadRewards();
  }, []);

  async function handleCreate(reward: Omit<RewardTemplate, 'rewardTemplateId' | 'isDeleted' | 'createdAt'>) {
    try {
      setError(null);
      await createParentFamilyReward(reward);
      await loadRewards();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create reward');
    }
  }

  async function handleCreatePerChild(childId: string, reward: RewardTemplateDraft) {
    try {
      setError(null);
      await createParentPerChildReward(childId, reward);
      await loadRewards();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create child override');
    }
  }

  async function handleAddCanned(template: CannedRewardTemplate) {
    await handleCreate({
      childId: null,
      rewardType: template.rewardType,
      amount: template.amount,
      unit: template.unit,
      frequency: template.frequency,
      cannedTemplateId: template.cannedTemplateId,
      description: template.description,
    });
  }

  return (
    <section aria-label="Manage family rewards page">
      <h1>Manage Rewards</h1>
      <p>Configure family-level rewards that all children can choose from.</p>
      {error && <p role="alert">{error}</p>}

      <RewardTemplateBuilder onSubmit={handleCreate} />
      <CannedRewardCatalog templates={cannedTemplates} onAdd={handleAddCanned} />

      <h2>Family Rewards</h2>
      <ul>
        {state.familyRewards.map((reward) => (
          <li key={reward.rewardTemplateId}>{reward.description}</li>
        ))}
      </ul>

      <PerChildOverrides groups={state.perChildRewards} onCreate={handleCreatePerChild} />
    </section>
  );
}
