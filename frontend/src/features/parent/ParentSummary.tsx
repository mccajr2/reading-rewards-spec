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

export function ParentSummary() {
  const { token } = useAuth();
  const navigate = useNavigate();
  const { childId } = useParams<{ childId: string }>();

  const [kids, setKids] = useState<KidSummary[]>([]);
  const [loading, setLoading] = useState(true);

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

  const selectedKid = childId ? kids.find(kid => kid.id === childId) : null;

  if (childId) {
    return (
      <div className="page">
        <h1>Child Detail</h1>
        {loading ? (
          <p>Loading…</p>
        ) : !selectedKid ? (
          <p className="muted">Child not found.</p>
        ) : (
          <div>
            <h2>{selectedKid.firstName}</h2>
            <p className="muted">Username: {selectedKid.username}</p>
            <table className="kids-table">
              <thead>
                <tr>
                  <th>Books Read</th>
                  <th>Chapters Read</th>
                  <th>Total Earned</th>
                  <th>Balance</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>{selectedKid.booksRead}</td>
                  <td>{selectedKid.chaptersRead}</td>
                  <td>${selectedKid.totalEarned.toFixed(2)}</td>
                  <td>${selectedKid.currentBalance.toFixed(2)}</td>
                </tr>
              </tbody>
            </table>
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
