import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { fetchWithAuth, RewardOptionDto, RewardOptionsResponseDto, RewardValueType } from '../../shared/api';
import { Button, Input, PageGuidance } from '../../components/shared';
import './ParentRewardsPage.css';

type Kid = {
  id: string;
  firstName: string;
  username: string;
};

export function ParentRewardsPage() {
  const { user, token } = useAuth();
  const navigate = useNavigate();

  const [kids, setKids] = useState<Kid[]>([]);
  const [rewardOptions, setRewardOptions] = useState<RewardOptionDto[]>([]);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [rewardForm, setRewardForm] = useState({
    name: 'Default $1 per chapter',
    description: 'Starter option for chapter-based earnings',
    valueType: 'MONEY' as RewardValueType,
    scopeType: 'FAMILY',
    childUserId: '',
    earningBasis: 'PER_CHAPTER',
    moneyAmount: '1.00',
    nonMoneyQuantity: '',
    nonMoneyUnitLabel: '',
    pageMilestoneSize: '',
  });
  const [editingRewardOptionId, setEditingRewardOptionId] = useState<string | null>(null);

  useEffect(() => {
    if (!user || user.role !== 'PARENT') {
      navigate('/');
      return;
    }
    loadKids();
    loadRewardOptions();
  }, []);

  async function loadKids() {
    const r = await fetchWithAuth('/parent/kids', token);
    if (r.ok) {
      setKids(await r.json());
    }
  }

  async function loadRewardOptions() {
    const r = await fetchWithAuth('/reward-options', token);
    if (!r.ok) {
      setRewardOptions([]);
      return;
    }
    const payload = await r.json() as RewardOptionsResponseDto;
    setRewardOptions(Array.isArray(payload?.options) ? payload.options : []);
  }

  const handleRewardFormChange = (field: string, value: string) => {
    setRewardForm(current => ({ ...current, [field]: value }));
  };

  const resetRewardForm = () => {
    setEditingRewardOptionId(null);
    setRewardForm({
      name: 'Default $1 per chapter',
      description: 'Starter option for chapter-based earnings',
      valueType: 'MONEY',
      scopeType: 'FAMILY',
      childUserId: '',
      earningBasis: 'PER_CHAPTER',
      moneyAmount: '1.00',
      nonMoneyQuantity: '',
      nonMoneyUnitLabel: '',
      pageMilestoneSize: '',
    });
  };

  const handleSaveRewardOption = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    const payload: Record<string, unknown> = {
      name: rewardForm.name,
      description: rewardForm.description,
      valueType: rewardForm.valueType,
      scopeType: rewardForm.scopeType,
      earningBasis: rewardForm.earningBasis,
      active: true,
    };

    if (rewardForm.valueType === 'MONEY') {
      payload.moneyAmount = Number(rewardForm.moneyAmount);
    } else {
      payload.nonMoneyQuantity = Number(rewardForm.nonMoneyQuantity);
      payload.nonMoneyUnitLabel = rewardForm.nonMoneyUnitLabel;
    }

    if (rewardForm.scopeType === 'CHILD') {
      payload.childUserId = rewardForm.childUserId;
    }
    if (rewardForm.earningBasis === 'PER_PAGE_MILESTONE') {
      payload.pageMilestoneSize = Number(rewardForm.pageMilestoneSize);
    }

    const path = editingRewardOptionId ? `/reward-options/${editingRewardOptionId}` : '/reward-options';
    const method = editingRewardOptionId ? 'PUT' : 'POST';
    const res = await fetchWithAuth(path, token, { method, body: JSON.stringify(payload) });
    if (!res.ok) {
      setError(await res.text() || 'Failed to save reward option');
      return;
    }

    setSuccess(editingRewardOptionId ? 'Reward option updated!' : 'Reward option created!');
    resetRewardForm();
    loadRewardOptions();
  };

  const handleDeactivateRewardOption = async (id: string) => {
    setError('');
    setSuccess('');
    const res = await fetchWithAuth(`/reward-options/${id}`, token, { method: 'DELETE' });
    if (!res.ok && res.status !== 204) {
      setError(await res.text() || 'Failed to deactivate reward option');
      return;
    }
    setSuccess('Reward option deactivated');
    loadRewardOptions();
  };

  const startEditingRewardOption = (option: RewardOptionDto) => {
    setEditingRewardOptionId(option.id);
    setRewardForm({
      name: option.name,
      description: option.description ?? '',
      valueType: option.valueType ?? 'MONEY',
      scopeType: option.scopeType,
      childUserId: option.childUserId ?? '',
      earningBasis: option.earningBasis,
      moneyAmount: option.moneyAmount != null ? option.moneyAmount.toFixed(2) : '',
      nonMoneyQuantity: option.nonMoneyQuantity != null ? String(option.nonMoneyQuantity) : '',
      nonMoneyUnitLabel: option.nonMoneyUnitLabel ?? '',
      pageMilestoneSize: option.pageMilestoneSize ? String(option.pageMilestoneSize) : '',
    });
  };

  const familyRewardOptions = rewardOptions.filter(option => option.scopeType === 'FAMILY');
  const childRewardOptions = rewardOptions.filter(option => option.scopeType === 'CHILD');
  const childNameById = new Map(kids.map(kid => [kid.id, kid.firstName]));

  return (
    <div className="page parent-rewards-page">
      <PageGuidance
        title="Manage Rewards"
        description="Create family-wide and child-specific reward options so each child has motivating ways to earn."
        instructions="Set scope and earning basis, then keep options current with edits or deactivation."
        tone="parent"
      />

      <section className="reward-options-section">
        <h2>Reward Options</h2>
        <p className="muted">Create family-wide or child-specific rewards so kids can choose how they earn.</p>
        <form className="reward-option-form" onSubmit={handleSaveRewardOption}>
          <Input className="input" type="text" placeholder="Reward name" value={rewardForm.name} onChange={e => handleRewardFormChange('name', e.target.value)} required />
          <Input className="input" type="text" placeholder="Description" value={rewardForm.description} onChange={e => handleRewardFormChange('description', e.target.value)} />

          <div className="reward-form-row">
            <label className="reward-select-field">
              <span>Scope</span>
              <select className="input" value={rewardForm.scopeType} onChange={e => handleRewardFormChange('scopeType', e.target.value)}>
                <option value="FAMILY">Family-wide</option>
                <option value="CHILD">Child-specific</option>
              </select>
            </label>

            <label className="reward-select-field">
              <span>Earning basis</span>
              <select className="input" value={rewardForm.earningBasis} onChange={e => handleRewardFormChange('earningBasis', e.target.value)}>
                <option value="PER_CHAPTER">Per chapter</option>
                <option value="PER_BOOK">Per book</option>
                <option value="PER_PAGE_MILESTONE">Per page milestone</option>
              </select>
            </label>

            <label className="reward-select-field">
              <span>Reward type</span>
              <select className="input" value={rewardForm.valueType} onChange={e => handleRewardFormChange('valueType', e.target.value)}>
                <option value="MONEY">Money (e.g. $1.00)</option>
                <option value="NON_MONEY">Non-money (e.g. 30 minutes screen time)</option>
              </select>
            </label>
          </div>

          {rewardForm.scopeType === 'CHILD' && (
            <label className="reward-select-field">
              <span>Child</span>
              <select className="input" value={rewardForm.childUserId} onChange={e => handleRewardFormChange('childUserId', e.target.value)} required>
                <option value="">Select a child</option>
                {kids.map(kid => <option key={kid.id} value={kid.id}>{kid.firstName}</option>)}
              </select>
            </label>
          )}

          <div className="reward-form-row">
            {rewardForm.valueType === 'MONEY' ? (
              <Input className="input" type="number" min="0" step="0.01" placeholder="Dollar amount" value={rewardForm.moneyAmount} onChange={e => handleRewardFormChange('moneyAmount', e.target.value)} required />
            ) : (
              <>
                <Input className="input" type="number" min="0" step="any" placeholder="Quantity (e.g. 30)" value={rewardForm.nonMoneyQuantity} onChange={e => handleRewardFormChange('nonMoneyQuantity', e.target.value)} required />
                <Input className="input" type="text" placeholder="Unit label (e.g. minutes screen time)" value={rewardForm.nonMoneyUnitLabel} onChange={e => handleRewardFormChange('nonMoneyUnitLabel', e.target.value)} required />
              </>
            )}
            <Input className="input" type="number" min="1" step="1" placeholder="Page milestone size" value={rewardForm.pageMilestoneSize} onChange={e => handleRewardFormChange('pageMilestoneSize', e.target.value)} disabled={rewardForm.earningBasis !== 'PER_PAGE_MILESTONE'} />
          </div>

          <div className="reward-form-actions">
            <Button type="submit">{editingRewardOptionId ? 'Update Reward Option' : 'Save Reward Option'}</Button>
            {editingRewardOptionId && <Button type="button" variant="secondary" onClick={resetRewardForm}>Cancel</Button>}
          </div>
        </form>

        {error && <p className="error-msg">{error}</p>}
        {success && <p className="success-msg">{success}</p>}

        <div className="reward-options-grid">
          <div>
            <h3>Family Options</h3>
            {familyRewardOptions.length === 0 ? (
              <p className="muted">No family reward options yet.</p>
            ) : (
              <div className="reward-option-card-list">
                {familyRewardOptions.map(option => (
                  <article className="reward-option-card" key={option.id}>
                    <div>
                      <strong>{option.name}</strong>
                      <p className="muted">{option.earningBasis} · {option.valueType === 'MONEY' ? `$${(option.moneyAmount ?? 0).toFixed(2)}` : `${option.nonMoneyQuantity} ${option.nonMoneyUnitLabel}`}</p>
                      {option.description && <p className="muted">{option.description}</p>}
                    </div>
                    <div className="table-actions">
                      <Button type="button" variant="secondary" size="sm" onClick={() => startEditingRewardOption(option)}>Edit</Button>
                      <Button type="button" variant="secondary" size="sm" onClick={() => handleDeactivateRewardOption(option.id)} disabled={!option.active}>Deactivate</Button>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </div>

          <div>
            <h3>Child-Specific Options</h3>
            {childRewardOptions.length === 0 ? (
              <p className="muted">No child-specific reward options yet.</p>
            ) : (
              <div className="reward-option-card-list">
                {childRewardOptions.map(option => (
                  <article className="reward-option-card" key={option.id}>
                    <div>
                      <strong>{option.name}</strong>
                      <p className="muted">{childNameById.get(option.childUserId ?? '') ?? 'Unknown child'} · {option.earningBasis} · {option.valueType === 'MONEY' ? `$${(option.moneyAmount ?? 0).toFixed(2)}` : `${option.nonMoneyQuantity} ${option.nonMoneyUnitLabel}`}</p>
                      {option.description && <p className="muted">{option.description}</p>}
                    </div>
                    <div className="table-actions">
                      <Button type="button" variant="secondary" size="sm" onClick={() => startEditingRewardOption(option)}>Edit</Button>
                      <Button type="button" variant="secondary" size="sm" onClick={() => handleDeactivateRewardOption(option.id)} disabled={!option.active}>Deactivate</Button>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </div>
        </div>
      </section>
    </div>
  );
}
