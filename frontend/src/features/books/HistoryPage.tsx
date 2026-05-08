import { useState, useEffect } from 'react';
import { useAuth } from '../auth/AuthContext';
import { fetchWithAuth } from '../../shared/api';
import './HistoryPage.css';

type ChapterRead = {
  id: string;
  chapterId: string;
  chapterName: string;
  bookTitle: string;
  completionDate: string;
};

export function HistoryPage() {
  const { token } = useAuth();
  const [history, setHistory] = useState<ChapterRead[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      const res = await fetchWithAuth('/history', token);
      if (res.ok) setHistory(await res.json());
      setLoading(false);
    };
    load();
  }, []);

  const byDate = history.reduce<Record<string, ChapterRead[]>>((acc, item) => {
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
    </div>
  );
}
