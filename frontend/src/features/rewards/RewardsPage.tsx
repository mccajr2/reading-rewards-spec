import { useEffect, useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import {
  fetchWithAuth,
  RewardHistoryItemDto,
  RewardOptionDto,
  RewardOptionsResponseDto,
  RewardUnitBalanceDto,
  RewardsPageResponseDto,
  RewardSummaryDto,
} from '../../shared/api';
import {
  Button,
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
  PageGuidance,
  Pagination,
} from '../../components/shared';
import { Badge } from '../../components/ui/badge';
import { ChildPayoutRequest } from '../../components/ChildPayoutRequest';
import { ChildNudgePanel } from '../../components/ChildNudgePanel';

function formatMoney(amount: number) {
  return `$${amount.toFixed(2)}`;
}

function formatUnitAmount(amount: number, unitType?: string, unitLabel?: string) {
  if (unitType === 'NON_MONEY') {
    return `${amount.toFixed(2)} ${unitLabel ?? 'units'}`;
  }
  return formatMoney(amount);
}

function formatOptionValue(option: RewardOptionDto) {
  if (option.valueType === 'NON_MONEY') {
    return `${option.nonMoneyQuantity ?? 0} ${option.nonMoneyUnitLabel ?? 'units'}`;
  }
  return formatMoney(option.moneyAmount ?? 0);
}

function rewardTypeBadgeVariant(type: string) {
  switch (type) {
    case 'EARN':
      return 'success' as const;
    case 'PAYOUT':
      return 'default' as const;
    case 'SPEND':
      return 'warning' as const;
    default:
      return 'secondary' as const;
  }
}

function SummaryStatCard({
  label,
  value,
  valueClassName,
  highlight,
}: {
  label: string;
  value: string;
  valueClassName?: string;
  highlight?: boolean;
}) {
  return (
    <Card className={highlight ? 'border-accent bg-accent-light p-4' : 'p-4'}>
      <p className="text-xs font-semibold uppercase tracking-wide text-text-secondary">{label}</p>
      <p className={`mt-1 text-2xl font-bold ${valueClassName ?? 'text-text-primary'}`}>{value}</p>
    </Card>
  );
}

function UnitBalanceCard({ unit }: { unit: RewardUnitBalanceDto }) {
  return (
    <Card className="p-4">
      <CardHeader className="mb-2 space-y-1">
        <CardTitle className="text-base">{unit.unitLabel}</CardTitle>
        <CardDescription>{unit.unitType === 'MONEY' ? 'Money balance' : 'Reward units'}</CardDescription>
      </CardHeader>
      <CardContent className="space-y-1 text-sm text-text-secondary">
        <p>
          Earned:{' '}
          <span className="font-medium text-text-primary">
            {formatUnitAmount(unit.totalEarned, unit.unitType, unit.unitLabel)}
          </span>
        </p>
        <p>
          Balance:{' '}
          <span className="font-semibold text-text-primary">
            {formatUnitAmount(unit.currentBalance, unit.unitType, unit.unitLabel)}
          </span>
        </p>
      </CardContent>
    </Card>
  );
}

export function RewardsPage() {
  const { token, user } = useAuth();
  const [summary, setSummary] = useState<RewardSummaryDto>({
    totalEarned: 0,
    totalPaidOut: 0,
    totalSpent: 0,
    currentBalance: 0,
  });
  const [rewards, setRewards] = useState<RewardHistoryItemDto[]>([]);
  const [rewardOptions, setRewardOptions] = useState<RewardOptionDto[]>([]);
  const [activeSelectionOptionId, setActiveSelectionOptionId] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  const pageSize = 10;
  const [totalCount, setTotalCount] = useState(0);
  const [loading, setLoading] = useState(false);

  const loadSummary = async () => {
    const r = await fetchWithAuth('/rewards/summary', token);
    if (r.ok) setSummary(await r.json());
  };

  const loadRewards = async (p = page) => {
    const r = await fetchWithAuth(`/rewards?page=${p}&pageSize=${pageSize}`, token);
    if (r.ok) {
      const data = (await r.json()) as RewardsPageResponseDto | RewardHistoryItemDto[];
      if (Array.isArray(data)) {
        setRewards(data);
        setTotalCount(data.length);
      } else {
        const rows = data.rewards ?? [];
        setRewards(rows);
        setTotalCount(data.totalCount ?? rows.length);
      }
    }
  };

  const loadRewardOptions = async () => {
    const r = await fetchWithAuth('/reward-options', token);
    if (r.ok) {
      const data = (await r.json()) as RewardOptionsResponseDto;
      setRewardOptions(data.options ?? []);
      setActiveSelectionOptionId(data.activeSelectionOptionId ?? null);
    }
  };

  useEffect(() => {
    loadSummary();
    loadRewardOptions();
    loadRewards(1);
  }, []);

  useEffect(() => {
    loadRewards(page);
  }, [page]);

  const refreshRewards = async () => {
    await loadSummary();
    await loadRewards(page);
    (window as { updateCredits?: () => void }).updateCredits?.();
  };

  const handleSelectRewardOption = async (optionId: string) => {
    setLoading(true);
    await fetchWithAuth(`/reward-options/${optionId}/select`, token, { method: 'POST' });
    await loadRewardOptions();
    await loadSummary();
    await loadRewards(page);
    (window as { updateCredits?: () => void }).updateCredits?.();
    setLoading(false);
  };

  const totalPages = Math.max(1, Math.ceil(totalCount / pageSize));
  const isChild = user?.role === 'CHILD';
  const activeOption = rewardOptions.find((option) => option.id === activeSelectionOptionId);

  return (
    <div className="page space-y-6">
      <PageGuidance
        title={isChild ? 'Your Rewards Shop 🎁' : 'Rewards Settings'}
        description={
          isChild
            ? 'Here are the rewards you can unlock with your reading points. The more you read, the more you can earn.'
            : 'Customize and manage the rewards your children can earn by tracking payouts, spending, and balances.'
        }
        instructions={
          isChild
            ? 'Check your balance, request payouts or spends for parent approval, and nudge your parent when needed.'
            : 'Review reward summaries and history. Approve child requests from Manage Kids.'
        }
        tone={isChild ? 'child' : 'parent'}
      />

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <SummaryStatCard label="All-Time Earned" value={formatMoney(summary.totalEarned)} valueClassName="text-success" />
        <SummaryStatCard label="Paid Out" value={formatMoney(summary.totalPaidOut)} valueClassName="text-primary" />
        <SummaryStatCard label="Spent" value={formatMoney(summary.totalSpent)} valueClassName="text-warning" />
        <SummaryStatCard
          label="Balance"
          value={formatMoney(summary.currentBalance)}
          valueClassName="text-warning"
          highlight
        />
      </div>

      {isChild && (summary.balancesByUnit?.length ?? 0) > 0 && (
        <section className="space-y-3">
          <h2 className="text-xl font-semibold text-text-primary">Balances By Reward Unit</h2>
          <div className="grid gap-4 md:grid-cols-2">
            {summary.balancesByUnit!.map((unit) => (
              <UnitBalanceCard key={`${unit.unitType}-${unit.unitLabel}`} unit={unit} />
            ))}
          </div>
        </section>
      )}

      {isChild && user?.id && (
        <section className="space-y-4">
          <ChildPayoutRequest
            token={token}
            childId={user.id}
            rewardOptionId={activeSelectionOptionId ?? undefined}
            balancesByUnit={summary.balancesByUnit}
            onSuccess={refreshRewards}
          />
          <ChildNudgePanel token={token} />
        </section>
      )}

      {isChild && (
        <section className="space-y-3">
          <Card>
            <CardHeader>
              <CardTitle>My Reward Options</CardTitle>
              <CardDescription>
                Choose which reward rule should apply to your future reading progress.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              {activeOption ? (
                <p className="text-sm text-text-secondary">
                  Active option:{' '}
                  <span className="font-medium text-text-primary">
                    {activeOption.name} ({activeOption.earningBasis}, {formatOptionValue(activeOption)})
                  </span>
                </p>
              ) : (
                <p className="text-sm text-text-secondary">No reward option selected yet.</p>
              )}

              <div className="grid gap-3">
                {rewardOptions.map((option) => {
                  const selected = option.id === activeSelectionOptionId;
                  return (
                    <Card
                      key={option.id}
                      className={selected ? 'border-primary bg-primary-light p-4' : 'p-4'}
                    >
                      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                        <div>
                          <h3 className="font-semibold text-text-primary">{option.name}</h3>
                          <p className="mt-1 text-sm text-text-secondary">
                            {option.scopeType === 'FAMILY' ? 'Family option' : 'Child option'} ·{' '}
                            {option.earningBasis} · {formatOptionValue(option)}
                          </p>
                          {option.description && (
                            <p className="mt-1 text-sm text-text-secondary">{option.description}</p>
                          )}
                        </div>
                        <Button
                          onClick={() => handleSelectRewardOption(option.id)}
                          disabled={loading || selected || !option.active}
                          variant={selected ? 'secondary' : 'default'}
                          size="sm"
                        >
                          {selected ? 'Selected' : 'Select'}
                        </Button>
                      </div>
                    </Card>
                  );
                })}
                {rewardOptions.length === 0 && (
                  <p className="text-sm text-text-secondary">No reward options have been shared with you yet.</p>
                )}
              </div>
            </CardContent>
          </Card>
        </section>
      )}

      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-text-primary">Reward History</h2>
        {rewards.length === 0 ? (
          <Card className="p-6">
            <p className="text-sm text-text-secondary">No reward activity yet.</p>
          </Card>
        ) : (
          <ul className="space-y-2">
            {rewards.map((r) => (
              <li key={r.id}>
                <Card
                  className={`p-4 ${
                    r.type === 'EARN'
                      ? 'border-l-4 border-l-success'
                      : r.type === 'PAYOUT'
                        ? 'border-l-4 border-l-primary'
                        : 'border-l-4 border-l-warning'
                  }`}
                >
                  <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                    <div className="space-y-1">
                      <Badge variant={rewardTypeBadgeVariant(r.type)}>{r.type}</Badge>
                      {r.type === 'EARN' && r.bookRead?.book?.title && r.chapter && (
                        <p className="text-sm text-text-primary">
                          {r.bookRead.book.title} — {r.chapter.name}
                        </p>
                      )}
                      {r.note && <p className="text-sm text-text-secondary">{r.note}</p>}
                    </div>
                    <div className="text-left sm:text-right">
                      <p className="font-semibold text-text-primary">
                        {r.type !== 'EARN' ? '−' : '+'}
                        {formatUnitAmount(r.amount, r.unitType, r.unitLabel)}
                      </p>
                      <p className="text-xs text-text-secondary">{new Date(r.createdAt).toLocaleString()}</p>
                    </div>
                  </div>
                </Card>
              </li>
            ))}
          </ul>
        )}
      </section>

      {totalPages > 1 && (
        <Pagination className="flex justify-center gap-4" page={page} totalPages={totalPages} onPageChange={setPage} />
      )}
    </div>
  );
}
