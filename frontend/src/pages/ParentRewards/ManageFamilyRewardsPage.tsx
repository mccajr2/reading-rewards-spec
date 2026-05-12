import { useEffect, useState } from 'react';
import {
  confirmParentPayout,
  getParentChildAccumulation,
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
import { ParentPayoutsPanel, type ParentAccumulationRow } from '../../components/parent/ParentPayoutsPanel';
import { PerChildOverrides } from '../../components/parent/PerChildOverrides';
import { RewardTemplateBuilder } from '../../components/parent/RewardTemplateBuilder';

export function ManageFamilyRewardsPage() {
  const [state, setState] = useState<ParentRewardsResponse>({ familyRewards: [], perChildRewards: [] });
  const [cannedTemplates, setCannedTemplates] = useState<CannedRewardTemplate[]>([]);
  const [activeChildForPayouts, setActiveChildForPayouts] = useState<string | null>(null);
  const [payoutRows, setPayoutRows] = useState<ParentAccumulationRow[]>([]);
  const [payoutChildName, setPayoutChildName] = useState('');
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

  async function loadChildPayouts(childId: string) {
    try {
      const data = await getParentChildAccumulation(childId);
      setActiveChildForPayouts(childId);
      setPayoutChildName(data.childName);
      const rows = (data.accumulations ?? []).map((row) => ({
        accumulationId: row.accumulationId ?? row.id ?? `${row.createdAt}-${row.amountEarned}`,
        amountEarned: Number(row.amountEarned ?? 0),
        status: row.status,
        calculationNote: row.calculationNote,
      }));
      setPayoutRows(rows);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load child payouts');
    }
  }

  async function handleConfirmPayout(accumulationIds: string[]) {
    if (!activeChildForPayouts) return;
    try {
      await confirmParentPayout(activeChildForPayouts, accumulationIds);
      await loadChildPayouts(activeChildForPayouts);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to confirm payout');
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

      {state.perChildRewards.length > 0 && (
        <section>
          <h2>Payout Tracking</h2>
          <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
            {state.perChildRewards.map((group) => (
              <button key={group.childId} type="button" onClick={() => void loadChildPayouts(group.childId)}>
                View {group.childName} Payouts
              </button>
            ))}
          </div>
        </section>
      )}

      {activeChildForPayouts && (
        <ParentPayoutsPanel
          childName={payoutChildName}
          rows={payoutRows}
          onConfirmPayout={handleConfirmPayout}
        />
      )}
    </section>
  );
}
