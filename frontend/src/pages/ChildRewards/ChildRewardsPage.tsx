import { useEffect, useState } from 'react';
import { RewardsPage } from '../../features/rewards/RewardsPage';
import { RewardsTypeCards } from '../../components/rewards/RewardsTypeCards';
import { getChildRewardBalance, type ChildRewardBalanceResponse } from '../../services/rewardApi';

export function ChildRewardsPage() {
  const [byType, setByType] = useState<ChildRewardBalanceResponse['byRewardType']>([]);

  useEffect(() => {
    const load = async () => {
      try {
        const response = await getChildRewardBalance();
        setByType(response.byRewardType ?? []);
      } catch {
        setByType([]);
      }
    };
    void load();
  }, []);

  return (
    <>
      <RewardsTypeCards byType={byType} />
      <RewardsPage />
    </>
  );
}
