import { useState, useEffect } from 'react';
import { useAuth } from '../auth/AuthContext';
import { fetchWithAuth, HistoryItemDto } from '../../shared/api';
import './HistoryPage.css';

export function HistoryPage() {
  const { token } = useAuth();
  const [history, setHistory] = useState<HistoryItemDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const pageSize = 12;

  useEffect(() => {
    const load = async () => {
      const res = await fetchWithAuth('/history', token);
      if (res.ok) {
        const rows = (await res.json()) as HistoryItemDto[];
        rows.sort((a, b) => new Date(b.completionDate).getTime() - new Date(a.completionDate).getTime());
        setHistory(rows);
      }
      setLoading(false);
    };
    load();
  }, []);

  const totalPages = Math.max(1, Math.ceil(history.length / pageSize));
  const start = (page - 1) * pageSize;
  const pageHistory = history.slice(start, start + pageSize);

  const byDate = pageHistory.reduce<Record<string, HistoryItemDto[]>>((acc, item) => {
    const d = item.completionDate ? new Date(item.completionDate).toLocaleDateString() : 'Unknown';
    if (!acc[d]) acc[d] = [];
    acc[d].push(item);
    return acc;
  }, {});

  const sortedDates = Object.keys(byDate).sort((a, b) => new Date(b).getTime() - new Date(a).getTime());

  if (loading) return <div className="page"><p>Loading…</p></div>;

  return (
    <div className="page history-page">
      <h1>Reading History</h1>
      {history.length === 0 && <p className="muted">No history yet. Start reading!</p>}
      {sortedDates.map(date => (
        <div key={date} className="history-group">
          <h3 className="history-date">{date}</h3>
          <ul className="history-list">
            {byDate[date].map(item => (
              <li key={item.id} className="history-item">
                <span className="history-book">{item.bookTitle}</span>
                <span className="history-chapter">{item.chapterName}</span>
              </li>
            ))}
          </ul>
        </div>
      ))}

      {history.length > pageSize && (
        <div className="history-pagination">
          <button className="btn btn-secondary" onClick={() => setPage(p => Math.max(1, p - 1))} disabled={page === 1}>
            ← Prev
          </button>
          <span>{page} / {totalPages}</span>
          <button className="btn btn-secondary" onClick={() => setPage(p => Math.min(totalPages, p + 1))} disabled={page === totalPages}>
            Next →
          </button>
        </div>
      )}
    </div>
  );
}
