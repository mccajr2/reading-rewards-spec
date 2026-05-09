import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { fetchWithAuth } from '../../shared/api';
import './ParentDashboard.css';

type KidSummary = {
  id: string;
  firstName: string;
  username: string;
  booksRead: number;
  chaptersRead: number;
  totalEarned: number;
  currentBalance: number;
};

type Chapter = {
  id: string;
  index: number;
  name: string;
  isRead: boolean;
  chapterReadId?: string;
  earnedReward?: number;
};

type Book = {
  bookReadId: string;
  googleBookId: string;
  title: string;
  authors: string[];
  thumbnailUrl: string;
  startDate: string;
  endDate?: string;
  inProgress: boolean;
  chapters: Chapter[];
};

type Reward = {
  id: string;
  type: string;
  amount: number;
  chapterReadId?: string;
  note?: string;
  createdAt: string;
};

type ChildDetail = {
  child: {
    id: string;
    firstName: string;
    username: string;
  };
  books: Book[];
  rewards: Reward[];
  totalEarned: number;
  currentBalance: number;
};

export function ParentSummary() {
  const { token } = useAuth();
  const navigate = useNavigate();
  const { childId } = useParams<{ childId: string }>();

  const [kids, setKids] = useState<KidSummary[]>([]);
  const [childDetail, setChildDetail] = useState<ChildDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [showConfirm, setShowConfirm] = useState<string | null>(null);

  useEffect(() => {
    const load = async () => {
      const r = await fetchWithAuth('/parent/kids/summary', token);
      if (r.ok) {
        const data = await r.json();
        setKids(data.kids ?? []);
      }
      setLoading(false);
    };
    load();
  }, [token]);

  useEffect(() => {
    if (!childId) {
      setChildDetail(null);
      return;
    }

    const loadDetail = async () => {
      setDetailLoading(true);
      try {
        const r = await fetchWithAuth(`/parent/${childId}/child-detail`, token);
        if (r.ok) {
          const data = await r.json();
          setChildDetail(data);
        }
      } catch (err) {
        console.error('Error loading child detail:', err);
      }
      setDetailLoading(false);
    };

    loadDetail();
  }, [childId, token]);

  const handleReverse = async (chapterReadId: string) => {
    if (!childId) return;
    try {
      const r = await fetchWithAuth(`/parent/${childId}/chapter-reads/${chapterReadId}/reverse`, token, {
        method: 'POST',
      });
      if (r.ok) {
        setShowConfirm(null);
        // Refresh child detail
        const detailR = await fetchWithAuth(`/parent/${childId}/child-detail`, token);
        if (detailR.ok) {
          const data = await detailR.json();
          setChildDetail(data);
        }
      } else {
        alert('Error reversing chapter read');
      }
    } catch (err) {
      console.error('Error reversing chapter read:', err);
      alert('Error reversing chapter read');
    }
  };

  if (childId) {
    return (
      <div className="page">
        <h1>Child Detail</h1>
        {detailLoading ? (
          <p>Loading…</p>
        ) : !childDetail ? (
          <p className="muted">Child not found.</p>
        ) : (
          <div>
            <h2>{childDetail.child.firstName}</h2>
            <p className="muted">Username: {childDetail.child.username}</p>

            <div>
              <h3>Summary</h3>
              <table className="kids-table">
                <thead>
                  <tr>
                    <th>Total Earned</th>
                    <th>Balance</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>${childDetail.totalEarned.toFixed(2)}</td>
                    <td>${childDetail.currentBalance.toFixed(2)}</td>
                  </tr>
                </tbody>
              </table>
            </div>

            {childDetail.books.length > 0 && (
              <div>
                <h3>Books</h3>
                {childDetail.books.map((book) => (
                  <div key={book.bookReadId} style={{ marginBottom: '1.5rem', borderBottom: '1px solid #ddd', paddingBottom: '1rem' }}>
                    <h4>{book.title}</h4>
                    <p className="muted">By {book.authors.join(', ')}</p>
                    <p className="muted">
                      {book.inProgress ? 'In Progress' : `Finished on ${new Date(book.endDate!).toLocaleDateString()}`}
                    </p>
                    {book.chapters.length > 0 && (
                      <div>
                        <h5>Chapters ({book.chapters.filter(ch => ch.isRead).length}/{book.chapters.length} read)</h5>
                        <table className="kids-table" style={{ fontSize: '0.9rem' }}>
                          <thead>
                            <tr>
                              <th>Chapter</th>
                              <th>Status</th>
                              <th>Reward</th>
                              <th>Action</th>
                            </tr>
                          </thead>
                          <tbody>
                            {book.chapters.map((chapter) => (
                              <tr key={chapter.id}>
                                <td>{chapter.index + 1}. {chapter.name}</td>
                                <td>{chapter.isRead ? '✓ Read' : 'Not read'}</td>
                                <td>{chapter.earnedReward ? `$${chapter.earnedReward.toFixed(2)}` : '—'}</td>
                                <td>
                                  {chapter.isRead && chapter.chapterReadId && (
                                    <>
                                      {showConfirm === chapter.chapterReadId ? (
                                        <div style={{ fontSize: '0.8rem' }}>
                                          <button
                                            className="btn btn-danger btn-sm"
                                            onClick={() => handleReverse(chapter.chapterReadId!)}
                                            style={{ marginRight: '0.5rem' }}
                                          >
                                            Confirm
                                          </button>
                                          <button
                                            className="btn btn-secondary btn-sm"
                                            onClick={() => setShowConfirm(null)}
                                          >
                                            Cancel
                                          </button>
                                        </div>
                                      ) : (
                                        <button
                                          className="btn btn-warning btn-sm"
                                          onClick={() => setShowConfirm(chapter.chapterReadId!)}
                                        >
                                          Reverse
                                        </button>
                                      )}
                                    </>
                                  )}
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}

            {childDetail.rewards.length > 0 && (
              <div>
                <h3>Reward History</h3>
                <table className="kids-table" style={{ fontSize: '0.9rem' }}>
                  <thead>
                    <tr>
                      <th>Type</th>
                      <th>Amount</th>
                      <th>Date</th>
                      <th>Note</th>
                    </tr>
                  </thead>
                  <tbody>
                    {childDetail.rewards.map((reward) => (
                      <tr key={reward.id}>
                        <td>{reward.type}</td>
                        <td>${reward.amount.toFixed(2)}</td>
                        <td>{new Date(reward.createdAt).toLocaleDateString()}</td>
                        <td className="muted">{reward.note || '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}
        <button className="btn btn-secondary" onClick={() => navigate('/parent/summary')}>
          Back to Summary
        </button>
      </div>
    );
  }

  return (
    <div className="page">
      <h1>Children's Summary</h1>
      {loading ? (
        <p>Loading…</p>
      ) : kids.length === 0 ? (
        <p className="muted">No children found.</p>
      ) : (
        <table className="kids-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Username</th>
              <th>Books Read</th>
              <th>Chapters Read</th>
              <th>Total Earned</th>
              <th>Balance</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {kids.map(kid => (
              <tr key={kid.id}>
                <td>{kid.firstName}</td>
                <td>{kid.username}</td>
                <td>{kid.booksRead}</td>
                <td>{kid.chaptersRead}</td>
                <td>${kid.totalEarned.toFixed(2)}</td>
                <td>${kid.currentBalance.toFixed(2)}</td>
                <td>
                  <button
                    className="btn btn-secondary btn-sm"
                    onClick={() => navigate(`/parent/summary/${kid.id}`)}
                    aria-label={`View details for ${kid.firstName}`}
                  >
                    View Details
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
