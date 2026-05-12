import { useState } from 'react';
import type { RewardTemplate } from '../../services/rewardApi';

type RewardTemplateBuilderProps = {
  onSubmit: (reward: Omit<RewardTemplate, 'rewardTemplateId' | 'isDeleted' | 'createdAt'>) => Promise<void>;
};

export function RewardTemplateBuilder({ onSubmit }: RewardTemplateBuilderProps) {
  const [description, setDescription] = useState('');
  const [amount, setAmount] = useState('1.00');
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    try {
      await onSubmit({
        childId: null,
        rewardType: 'MONEY',
        amount: Number(amount),
        unit: 'PER_CHAPTER',
        frequency: 'IMMEDIATE',
        cannedTemplateId: null,
        description,
      });
      setDescription('');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} aria-label="reward template builder">
      <label>
        Description
        <input
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          placeholder="e.g. $0.50 per chapter"
          required
        />
      </label>
      <label>
        Amount
        <input
          type="number"
          step="0.01"
          min="0"
          value={amount}
          onChange={(event) => setAmount(event.target.value)}
          required
        />
      </label>
      <button type="submit" disabled={submitting}>
        {submitting ? 'Saving...' : 'Save Family Reward'}
      </button>
    </form>
  );
}
