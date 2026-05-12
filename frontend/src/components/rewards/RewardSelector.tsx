import type { AvailableRewardTemplate } from '../../services/rewardApi';

type RewardSelectorProps = {
  rewards: AvailableRewardTemplate[];
  selectedRewardTemplateId: string;
  onSelect: (rewardTemplateId: string) => void;
};

export function RewardSelector({ rewards, selectedRewardTemplateId, onSelect }: RewardSelectorProps) {
  return (
    <div aria-label="Reward selector">
      {rewards.map((reward) => {
        const checked = reward.rewardTemplateId === selectedRewardTemplateId;
        return (
          <label key={reward.rewardTemplateId} style={{ display: 'block', marginBottom: '0.5rem' }}>
            <input
              type="radio"
              name="reward-template"
              value={reward.rewardTemplateId}
              checked={checked}
              onChange={() => onSelect(reward.rewardTemplateId)}
            />{' '}
            {reward.description}
          </label>
        );
      })}
    </div>
  );
}
