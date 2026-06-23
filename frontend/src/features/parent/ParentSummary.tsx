import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { fetchWithAuth } from '../../shared/api';
import {
  Button,
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
  Input,
  PageGuidance,
} from '../../components/shared';
import { Badge } from '../../components/ui/badge';
import { ParentSettlementPanel } from '../../components/ParentSettlementPanel';

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
  bookEarningBasis?: 'PER_CHAPTER' | 'PER_BOOK' | 'PER_PAGE_MILESTONE' | null;
  pageCount?: number | null;
  pageCountConfirmed?: boolean;
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

function formatMoney(amount: number) {
  return `$${amount.toFixed(2)}`;
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

function KidSummaryCard({
  kid,
  onViewDetails,
}: {
  kid: KidSummary;
  onViewDetails: () => void;
}) {
  return (
    <Card className="flex h-full flex-col">
      <CardHeader>
        <div className="flex items-start justify-between gap-2">
          <div>
            <CardTitle>{kid.firstName}</CardTitle>
            <CardDescription>@{kid.username}</CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent className="flex flex-1 flex-col gap-4">
        <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
          <div>
            <dt className="text-text-secondary">Books read</dt>
            <dd className="font-semibold text-text-primary">{kid.booksRead}</dd>
          </div>
          <div>
            <dt className="text-text-secondary">Chapters read</dt>
            <dd className="font-semibold text-text-primary">{kid.chaptersRead}</dd>
          </div>
          <div>
            <dt className="text-text-secondary">Total earned</dt>
            <dd className="font-semibold text-success">{formatMoney(kid.totalEarned)}</dd>
          </div>
          <div>
            <dt className="text-text-secondary">Balance</dt>
            <dd className="font-semibold text-text-primary">{formatMoney(kid.currentBalance)}</dd>
          </div>
        </dl>
        <Button
          variant="secondary"
          size="sm"
          className="mt-auto w-full sm:w-auto"
          onClick={onViewDetails}
          aria-label={`View details for ${kid.firstName}`}
        >
          View Details
        </Button>
      </CardContent>
    </Card>
  );
}

export function ParentSummary() {
  const { token } = useAuth();
  const navigate = useNavigate();
  const { childId } = useParams<{ childId: string }>();

  const [kids, setKids] = useState<KidSummary[]>([]);
  const [childDetail, setChildDetail] = useState<ChildDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [showConfirm, setShowConfirm] = useState<string | null>(null);
  const [pageOverrideInput, setPageOverrideInput] = useState<Record<string, string>>({});
  const [pageOverrideLoading, setPageOverrideLoading] = useState<Record<string, boolean>>({});

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
          setChildDetail(await r.json());
        }
      } catch (err) {
        console.error('Error loading child detail:', err);
      }
      setDetailLoading(false);
    };

    loadDetail();
  }, [childId, token]);

  const handlePageCountOverride = async (targetChildId: string, book: Book) => {
    const raw = pageOverrideInput[book.bookReadId] ?? '';
    const totalPages = parseInt(raw, 10);
    if (isNaN(totalPages) || totalPages < 1) {
      window.alert('Enter a valid page count (minimum 1).');
      return;
    }
    setPageOverrideLoading((prev) => ({ ...prev, [book.bookReadId]: true }));
    try {
      const res = await fetchWithAuth(
        `/children/${targetChildId}/books/${book.googleBookId}/page-count-confirmation`,
        token,
        {
          method: 'PUT',
          body: JSON.stringify({ totalPages, confirmed: true }),
        }
      );
      if (res.ok) {
        setPageOverrideInput((prev) => ({ ...prev, [book.bookReadId]: '' }));
        const detailR = await fetchWithAuth(`/parent/${targetChildId}/child-detail`, token);
        if (detailR.ok) {
          setChildDetail(await detailR.json());
        }
      } else {
        const msg = await res.text();
        window.alert(msg || 'Error updating page count.');
      }
    } finally {
      setPageOverrideLoading((prev) => ({ ...prev, [book.bookReadId]: false }));
    }
  };

  const handleReverse = async (chapterReadId: string) => {
    if (!childId) return;
    try {
      const r = await fetchWithAuth(`/parent/${childId}/chapter-reads/${chapterReadId}/reverse`, token, {
        method: 'POST',
      });
      if (r.ok) {
        setShowConfirm(null);
        const detailR = await fetchWithAuth(`/parent/${childId}/child-detail`, token);
        if (detailR.ok) {
          setChildDetail(await detailR.json());
        }
      } else {
        alert('Error reversing chapter read');
      }
    } catch (err) {
      console.error('Error reversing chapter read:', err);
      alert('Error reversing chapter read');
    }
  };

  const refreshChildDetail = async () => {
    if (!childId) return;
    const detailR = await fetchWithAuth(`/parent/${childId}/child-detail`, token);
    if (detailR.ok) {
      setChildDetail(await detailR.json());
    }
    const summaryR = await fetchWithAuth('/parent/kids/summary', token);
    if (summaryR.ok) {
      const data = await summaryR.json();
      setKids(data.kids ?? []);
    }
  };

  if (childId) {
    return (
      <div className="page space-y-6">
        <PageGuidance
          title="Child Account Details"
          description="Review a child's reading activity, chapter history, and reward transactions in detail."
          instructions="Use Reverse on chapter reads when corrections are required, then return to account management when finished."
          tone="parent"
        />

        {detailLoading ? (
          <p className="text-sm text-text-secondary">Loading…</p>
        ) : !childDetail ? (
          <p className="text-sm text-text-secondary">Child not found.</p>
        ) : (
          <div className="space-y-6">
            <div>
              <h2 className="text-2xl font-semibold text-text-primary">{childDetail.child.firstName}</h2>
              <p className="text-sm text-text-secondary">Username: {childDetail.child.username}</p>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <Card className="p-4">
                <p className="text-xs font-semibold uppercase tracking-wide text-text-secondary">Total Earned</p>
                <p className="mt-1 text-2xl font-bold text-success">{formatMoney(childDetail.totalEarned)}</p>
              </Card>
              <Card className="border-accent bg-accent-light p-4">
                <p className="text-xs font-semibold uppercase tracking-wide text-text-secondary">Balance</p>
                <p className="mt-1 text-2xl font-bold text-text-primary">{formatMoney(childDetail.currentBalance)}</p>
              </Card>
            </div>

            <ParentSettlementPanel
              token={token}
              targetChildId={childDetail.child.id}
              childName={childDetail.child.firstName}
              onSettlementChange={refreshChildDetail}
            />

            {childDetail.books.length > 0 && (
              <section className="space-y-4">
                <h3 className="text-lg font-semibold text-text-primary">Books</h3>
                {childDetail.books.map((book) => (
                  <Card key={book.bookReadId} className="p-4">
                    <CardHeader className="mb-3 space-y-1">
                      <CardTitle className="text-lg">{book.title}</CardTitle>
                      <CardDescription>By {book.authors.join(', ')}</CardDescription>
                      <p className="text-sm text-text-secondary">
                        {book.inProgress
                          ? 'In Progress'
                          : `Finished on ${new Date(book.endDate!).toLocaleDateString()}`}
                      </p>
                    </CardHeader>

                    {book.inProgress && book.bookEarningBasis === 'PER_PAGE_MILESTONE' && (
                      <Card className="mb-4 border-border bg-background-alt p-4">
                        <p className="mb-3 text-sm text-text-secondary">
                          <span className="font-medium text-text-primary">Page Milestones</span> — Current page
                          count: {book.pageCount ?? 'not set'}
                          {book.pageCountConfirmed ? ' ✓ Confirmed' : ' (not yet confirmed)'}
                        </p>
                        <div className="flex flex-col gap-2 sm:flex-row sm:items-end">
                          <div className="flex-1">
                            <label
                              htmlFor={`page-override-${book.bookReadId}`}
                              className="mb-1 block text-sm font-medium text-text-primary"
                            >
                              Override page count
                            </label>
                            <Input
                              id={`page-override-${book.bookReadId}`}
                              type="number"
                              min="1"
                              placeholder="Total pages"
                              value={pageOverrideInput[book.bookReadId] ?? ''}
                              onChange={(e) =>
                                setPageOverrideInput((prev) => ({ ...prev, [book.bookReadId]: e.target.value }))
                              }
                            />
                          </div>
                          <Button
                            variant="secondary"
                            size="sm"
                            onClick={() => handlePageCountOverride(childDetail.child.id, book)}
                            disabled={pageOverrideLoading[book.bookReadId] || !pageOverrideInput[book.bookReadId]}
                          >
                            {pageOverrideLoading[book.bookReadId] ? 'Saving…' : 'Set Page Count'}
                          </Button>
                        </div>
                      </Card>
                    )}

                    {book.chapters.length > 0 && (
                      <div className="space-y-2">
                        <h5 className="text-sm font-semibold text-text-primary">
                          Chapters ({book.chapters.filter((ch) => ch.isRead).length}/{book.chapters.length} read)
                        </h5>
                        <div className="overflow-x-auto rounded-lg border border-border">
                          <table className="w-full min-w-[32rem] text-sm">
                            <thead className="bg-background-alt text-left text-xs uppercase tracking-wide text-text-secondary">
                              <tr>
                                <th className="px-3 py-2 font-semibold">Chapter</th>
                                <th className="px-3 py-2 font-semibold">Status</th>
                                <th className="px-3 py-2 font-semibold">Reward</th>
                                <th className="px-3 py-2 font-semibold">Action</th>
                              </tr>
                            </thead>
                            <tbody>
                              {book.chapters.map((chapter) => (
                                <tr key={chapter.id} className="border-t border-border">
                                  <td className="px-3 py-2 text-text-primary">
                                    {chapter.index + 1}. {chapter.name}
                                  </td>
                                  <td className="px-3 py-2">
                                    {chapter.isRead ? (
                                      <Badge variant="success">Read</Badge>
                                    ) : (
                                      <Badge variant="secondary">Not read</Badge>
                                    )}
                                  </td>
                                  <td className="px-3 py-2 text-text-primary">
                                    {chapter.earnedReward ? formatMoney(chapter.earnedReward) : '—'}
                                  </td>
                                  <td className="px-3 py-2">
                                    {chapter.isRead && chapter.chapterReadId && (
                                      <>
                                        {showConfirm === chapter.chapterReadId ? (
                                          <div className="flex flex-wrap gap-2">
                                            <Button
                                              variant="secondary"
                                              size="sm"
                                              onClick={() => handleReverse(chapter.chapterReadId!)}
                                            >
                                              Confirm
                                            </Button>
                                            <Button
                                              variant="ghost"
                                              size="sm"
                                              onClick={() => setShowConfirm(null)}
                                            >
                                              Cancel
                                            </Button>
                                          </div>
                                        ) : (
                                          <Button
                                            variant="secondary"
                                            size="sm"
                                            onClick={() => setShowConfirm(chapter.chapterReadId!)}
                                          >
                                            Reverse
                                          </Button>
                                        )}
                                      </>
                                    )}
                                  </td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      </div>
                    )}
                  </Card>
                ))}
              </section>
            )}

            {childDetail.rewards.length > 0 && (
              <section className="space-y-3">
                <h3 className="text-lg font-semibold text-text-primary">Reward History</h3>
                <div className="overflow-x-auto rounded-lg border border-border">
                  <table className="w-full min-w-[28rem] text-sm">
                    <thead className="bg-background-alt text-left text-xs uppercase tracking-wide text-text-secondary">
                      <tr>
                        <th className="px-3 py-2 font-semibold">Type</th>
                        <th className="px-3 py-2 font-semibold">Amount</th>
                        <th className="px-3 py-2 font-semibold">Date</th>
                        <th className="px-3 py-2 font-semibold">Note</th>
                      </tr>
                    </thead>
                    <tbody>
                      {childDetail.rewards.map((reward) => (
                        <tr key={reward.id} className="border-t border-border">
                          <td className="px-3 py-2">
                            <Badge variant={rewardTypeBadgeVariant(reward.type)}>{reward.type}</Badge>
                          </td>
                          <td className="px-3 py-2 font-medium text-text-primary">{formatMoney(reward.amount)}</td>
                          <td className="px-3 py-2 text-text-secondary">
                            {new Date(reward.createdAt).toLocaleDateString()}
                          </td>
                          <td className="px-3 py-2 text-text-secondary">{reward.note || '—'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </section>
            )}
          </div>
        )}

        <Button variant="secondary" onClick={() => navigate('/parent/summary')}>
          Back to Summary
        </Button>
      </div>
    );
  }

  return (
    <div className="page space-y-6">
      <PageGuidance
        title="Manage Child Accounts"
        description="Create or manage accounts for each of your children. Review progress totals and open details for each child."
        instructions="Select View Details for account-level history, or return to the dashboard to add and update child accounts."
        tone="parent"
      />

      {loading ? (
        <p className="text-sm text-text-secondary">Loading…</p>
      ) : kids.length === 0 ? (
        <Card className="p-6">
          <p className="text-sm text-text-secondary">No children found.</p>
        </Card>
      ) : (
        <>
          <ParentSettlementPanel token={token} />
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {kids.map((kid) => (
              <KidSummaryCard
                key={kid.id}
                kid={kid}
                onViewDetails={() => navigate(`/parent/summary/${kid.id}`)}
              />
            ))}
          </div>
        </>
      )}
    </div>
  );
}
