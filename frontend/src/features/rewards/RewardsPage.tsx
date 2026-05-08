import { useEffect, useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import { fetchWithAuth } from '../../shared/api';
import './RewardsPage.css';

type RewardSummary = {
  totalEarned: number;
  totalPaidOut: number;
  totalSpent: number;
  currentBalance: number;
};

type Reward = {
  id: string;
  type: 'EARN' | 'PAYOUT' | 'SPEND';
  amount: number;
  note?: string;
  createdAt: string;
  chapter?: { id: string; name: string };
  bookRead?: { book?: { title?: string } };
};

export function RewardsPage() {
  const { token } = useAuth();
  const [summary, setSummary] = useState<RewardSummary>({ totalEarned: 0, totalPaidOut: 0, totalSpent: 0, currentBalance: 0 });
  const [rewards, setRewards] = useState<Reward[]>([]);
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
      const data = await r.json();
      setRewards(data.rewards ?? data);
      setTotalCount(data.totalCount ?? (data.rewards ? data.rewards.length : data.length));
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

  return (
    <div className="page rewards-page">
      <h1>Rewards</h1>

      <div className="rewards-summary-grid">
        <div className="summary-card">
          <span className="summary-label">All-Time Earned</span>
          <span className="summary-value earn">${summary.totalEarned.toFixed(2)}</span>
        </div>
        <div className="summary-card">
          <span className="summary-label">Paid Out</span>
          <span className="summary-value payout">${summary.totalPaidOut.toFixed(2)}</span>
        </div>
        <div className="summary-card">
          <span className="summary-label">Spent</span>
          <span className="summary-value spend">${summary.totalSpent.toFixed(2)}</span>
        </div>
        <div className="summary-card highlight">
          <span className="summary-label">Balance</span>
          <span className="summary-value balance">${summary.currentBalance.toFixed(2)}</span>
        </div>
      </div>

      <div className="rewards-actions">
        <div className="action-group">
          <input className="input" type="number" min="0" step="0.01" placeholder="Payout amount" value={payout} onChange={e => setPayout(e.target.value)} />
          <button className="btn btn-primary" onClick={handlePayout} disabled={loading || !payout}>Mark Paid Out</button>
        </div>
        <div className="action-group">
          <input className="input" type="number" min="0" step="0.01" placeholder="Spend amount" value={spend} onChange={e => setSpend(e.target.value)} />
          <input className="input" type="text" placeholder="What did you buy?" value={spendNote} onChange={e => setSpendNote(e.target.value)} />
          <button className="btn btn-secondary" onClick={handleSpend} disabled={loading || !spend || !spendNote}>Spend</button>
        </div>
      </div>

      <h2>Reward History</h2>
      <ul className="rewards-list">
        {rewards.map(r => (
          <li key={r.id} className={`reward-item ${r.type.toLowerCase()}`}>
            <div className="reward-detail">
              <span className="reward-type">{r.type}</span>
              {r.type === 'EARN' && r.bookRead?.book?.title && r.chapter && (
                <span className="reward-desc">{r.bookRead.book.title} — {r.chapter.name}</span>
              )}
              {r.note && <span className="reward-desc">{r.note}</span>}
            </div>
            <div className="reward-right">
              <span className="reward-amount">{r.type !== 'EARN' ? '-' : '+'}${r.amount.toFixed(2)}</span>
              <span className="reward-date">{new Date(r.createdAt).toLocaleString()}</span>
            </div>
          </li>
        ))}
      </ul>

      {totalPages > 1 && (
        <div className="pagination">
          <button className="btn btn-secondary" onClick={() => setPage(p => Math.max(1, p - 1))} disabled={page === 1}>← Prev</button>
          <span>{page} / {totalPages}</span>
          <button className="btn btn-secondary" onClick={() => setPage(p => Math.min(totalPages, p + 1))} disabled={page === totalPages}>Next →</button>
        </div>
      )}
    </div>
  );
}
