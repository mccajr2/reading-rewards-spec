import { useEffect, useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import { fetchWithAuth, RewardHistoryItemDto, RewardsPageResponseDto, RewardSummaryDto } from '../../shared/api';
import { Button, Card, CardContent, Input, PageGuidance, Pagination } from '../../components/shared';
import './RewardsPage.css';

export function RewardsPage() {
  const { token, user } = useAuth();
  const [summary, setSummary] = useState<RewardSummaryDto>({ totalEarned: 0, totalPaidOut: 0, totalSpent: 0, currentBalance: 0 });
  const [rewards, setRewards] = useState<RewardHistoryItemDto[]>([]);
  const [page, setPage] = useState(1);
  const pageSize = 10;
  const [totalCount, setTotalCount] = useState(0);
  const [spend, setSpend] = useState('');
  const [spendNote, setSpendNote] = useState('');
  const [payout, setPayout] = useState('');
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

  useEffect(() => {
    loadSummary();
    loadRewards(1);
  }, []);

  useEffect(() => { loadRewards(page); }, [page]);

  const handleSpend = async () => {
    if (!spend || !spendNote) return;
    setLoading(true);
    await fetchWithAuth(`/rewards/spend?amount=${encodeURIComponent(spend)}&note=${encodeURIComponent(spendNote)}`, token, { method: 'POST' });
    setSpend(''); setSpendNote('');
    await loadSummary(); await loadRewards(page);
    (window as any).updateCredits?.();
    setLoading(false);
  };

  const handlePayout = async () => {
    if (!payout) return;
    setLoading(true);
    await fetchWithAuth(`/rewards/payout?amount=${encodeURIComponent(payout)}`, token, { method: 'POST' });
    setPayout('');
    await loadSummary(); await loadRewards(page);
    (window as any).updateCredits?.();
    setLoading(false);
  };

  const totalPages = Math.max(1, Math.ceil(totalCount / pageSize));
  const isChild = user?.role === 'CHILD';

  return (
    <div className="page rewards-page">
      <PageGuidance
        title={isChild ? 'Your Rewards Shop 🎁' : 'Rewards Settings'}
        description={
          isChild
            ? 'Here are the rewards you can unlock with your reading points. The more you read, the more you can earn.'
            : 'Customize and manage the rewards your children can earn by tracking payouts, spending, and balances.'
        }
        instructions={
          isChild
            ? 'Check your balance, celebrate your progress, and use your points for fun rewards.'
            : 'Use payout and spend actions below to keep reward balances accurate and up to date.'
        }
        tone={isChild ? 'child' : 'parent'}
      />

      <div className="rewards-summary-grid">
        <Card className="summary-card">
          <span className="summary-label">All-Time Earned</span>
          <span className="summary-value earn">${summary.totalEarned.toFixed(2)}</span>
        </Card>
        <Card className="summary-card">
          <span className="summary-label">Paid Out</span>
          <span className="summary-value payout">${summary.totalPaidOut.toFixed(2)}</span>
        </Card>
        <Card className="summary-card">
          <span className="summary-label">Spent</span>
          <span className="summary-value spend">${summary.totalSpent.toFixed(2)}</span>
        </Card>
        <Card className="summary-card highlight">
          <span className="summary-label">Balance</span>
          <span className="summary-value balance">${summary.currentBalance.toFixed(2)}</span>
        </Card>
      </div>

      <div className="rewards-actions">
        <div className="action-group">
          <Input className="input" type="number" min="0" step="0.01" placeholder="Payout amount" value={payout} onChange={e => setPayout(e.target.value)} />
          <Button onClick={handlePayout} disabled={loading || !payout}>Mark Paid Out</Button>
        </div>
        <div className="action-group">
          <Input className="input" type="number" min="0" step="0.01" placeholder="Spend amount" value={spend} onChange={e => setSpend(e.target.value)} />
          <Input className="input" type="text" placeholder="What did you buy?" value={spendNote} onChange={e => setSpendNote(e.target.value)} />
          <Button variant="secondary" onClick={handleSpend} disabled={loading || !spend || !spendNote}>Spend</Button>
        </div>
      </div>

      <h2>Reward History</h2>
      <ul className="rewards-list">
        {rewards.map(r => (
          <Card key={r.id} className={`reward-item ${r.type.toLowerCase()}`}>
            <CardContent className="reward-detail">
              <span className="reward-type">{r.type}</span>
              {r.type === 'EARN' && r.bookRead?.book?.title && r.chapter && (
                <span className="reward-desc">{r.bookRead.book.title} — {r.chapter.name}</span>
              )}
              {r.note && <span className="reward-desc">{r.note}</span>}
            </CardContent>
            <div className="reward-right">
              <span className="reward-amount">{r.type !== 'EARN' ? '-' : '+'}${r.amount.toFixed(2)}</span>
              <span className="reward-date">{new Date(r.createdAt).toLocaleString()}</span>
            </div>
          </Card>
        ))}
      </ul>

      {totalPages > 1 && (
        <Pagination className="pagination" page={page} totalPages={totalPages} onPageChange={setPage} />
      )}
    </div>
  );
}
