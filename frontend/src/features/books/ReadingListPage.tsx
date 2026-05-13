import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { fetchWithAuth } from '../../shared/api';
import { Button, Input, PageGuidance } from '../../components/shared';
import './ReadingListPage.css';

type BookReadProgress = {
  bookReadId: string;
  googleBookId: string;
  title: string;
  description: string;
  thumbnailUrl: string;
  authors: string[];
  startDate: string;
  bookEarningBasis?: 'PER_CHAPTER' | 'PER_BOOK' | 'PER_PAGE_MILESTONE' | null;
  pageCount?: number | null;
  pageCountConfirmed?: boolean;
  readCount: number;
  readChapterIds: string[];
};

type Chapter = {
  id: string;
  name: string;
  chapterIndex: number;
};

type ChapterRead = {
  chapterId: string;
  completionDate: string;
};

type CompletionOption = {
  id: string;
  name: string;
};

export function ReadingListPage() {
  const { token } = useAuth();
  const navigate = useNavigate();

  const [bookReads, setBookReads] = useState<BookReadProgress[]>([]);
  const [chapters, setChapters] = useState<Record<string, Chapter[]>>({});
  const [readDates, setReadDates] = useState<Record<string, Record<string, string>>>({});
  const [editName, setEditName] = useState<Record<string, { editing: boolean; value: string }>>({});
  const [chapterInput, setChapterInput] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    try {
      const res = await fetchWithAuth('/bookreads/in-progress', token);
      if (!res.ok) return;
      const brs: BookReadProgress[] = await res.json();
      setBookReads(brs);

      const chaptersMap: Record<string, Chapter[]> = {};
      const datesMap: Record<string, Record<string, string>> = {};

      for (const br of brs) {
        const cr = await fetchWithAuth(`/bookreads/${br.bookReadId}/chapters`, token);
        chaptersMap[br.googleBookId] = cr.ok ? await cr.json() : [];

        const drRes = await fetchWithAuth(`/bookreads/${br.bookReadId}/chapterreads`, token);
        if (drRes.ok) {
          const drArr: ChapterRead[] = await drRes.json();
          datesMap[br.bookReadId] = {};
          for (const dr of drArr) {
            if (dr.chapterId && dr.completionDate) {
              datesMap[br.bookReadId][dr.chapterId] = new Date(dr.completionDate).toLocaleDateString();
            }
          }
        }
      }
      setChapters(chaptersMap);
      setReadDates(datesMap);
    } catch {
      // Keep page responsive when API is unavailable (e.g., isolated frontend tests).
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const promptForRewardChoice = async (message: string, options: CompletionOption[]) => {
    const choice = window.prompt(
      `${message}\n${options.map((o, i) => `${i + 1}. ${o.name}`).join('\n')}`,
      '1'
    );
    if (!choice) return null;
    const selected = options[Number(choice) - 1];
    if (!selected) {
      window.alert('Invalid option number.');
      return null;
    }
    return selected;
  };

  const handleMarkComplete = async (br: BookReadProgress) => {
    let res = await fetchWithAuth(`/books/${br.googleBookId}/finish`, token, { method: 'POST' });

    if (res.status === 409) {
      const conflictBody = await res.json().catch(() => null) as { availableOptions?: CompletionOption[] } | null;
      const options = conflictBody?.availableOptions ?? [];
      if (options.length > 0) {
        const selected = await promptForRewardChoice('Choose a reward option for this book:', options);
        if (!selected) return;
        res = await fetchWithAuth(`/books/${br.googleBookId}/finish`, token, {
          method: 'POST',
          body: JSON.stringify({ rewardOptionId: selected.id }),
        });
      }
    }

    if (res.ok) {
      navigate('/history');
      return;
    }

    const msg = await res.text();
    if (msg) window.alert(msg);
  };

  const handleCheck = async (br: BookReadProgress, chapter: Chapter, isRead: boolean) => {
    const chapList = chapters[br.googleBookId] ?? [];
    if (!isRead) {
      let res = await fetchWithAuth(`/bookreads/${br.bookReadId}/chapters/${chapter.id}/read`, token, { method: 'POST' });

      if (res.status === 409) {
        const conflictBody = await res.json().catch(() => null) as { availableOptions?: CompletionOption[] } | null;
        const options = conflictBody?.availableOptions ?? [];
        if (options.length > 0) {
          const selected = await promptForRewardChoice('Choose a reward option for this chapter:', options);
          if (!selected) return;
          res = await fetchWithAuth(`/bookreads/${br.bookReadId}/chapters/${chapter.id}/read`, token, {
            method: 'POST',
            body: JSON.stringify({ rewardOptionId: selected.id }),
          });
        }
      }

      if (res.ok) {
        const date = new Date().toLocaleDateString();
        setReadDates(prev => ({
          ...prev,
          [br.bookReadId]: { ...(prev[br.bookReadId] ?? {}), [chapter.id]: date },
        }));
        setBookReads(prev => prev.map(b =>
          b.bookReadId === br.bookReadId
            ? { ...b, readChapterIds: [...b.readChapterIds, chapter.id] }
            : b
        ));
        (window as any).updateCredits?.();

        // check if all done
        const newReadIds = new Set([...br.readChapterIds, chapter.id]);
        if (br.bookEarningBasis !== 'PER_BOOK' && chapList.length > 0 && chapList.every(c => newReadIds.has(c.id))) {
          if (window.confirm('You finished the book! Mark as finished?')) {
            await fetchWithAuth(`/books/${br.googleBookId}/finish`, token, { method: 'POST' });
            navigate('/history');
          }
        }
      } else if (res.status >= 400) {
        const msg = await res.text();
        if (msg) window.alert(msg);
      }
    } else {
      // Only allow unchecking the most recently read chapter
      const readIds = chapList.filter(c => br.readChapterIds.includes(c.id)).map(c => c.id);
      const mostRecent = readIds[readIds.length - 1];
      if (chapter.id !== mostRecent) return;
      if (!window.confirm('Undo this chapter read?')) return;
      const res = await fetchWithAuth(`/books/${br.googleBookId}/chapters/${chapter.id}/read`, token, { method: 'DELETE' });
      if (res.ok) {
        setBookReads(prev => prev.map(b =>
          b.bookReadId === br.bookReadId
            ? { ...b, readChapterIds: b.readChapterIds.filter(id => id !== chapter.id) }
            : b
        ));
        setReadDates(prev => {
          const updated = { ...prev };
          const bmap = { ...(updated[br.bookReadId] ?? {}) };
          delete bmap[chapter.id];
          updated[br.bookReadId] = bmap;
          return updated;
        });
        (window as any).updateCredits?.();
      }
    }
  };

  const handleDeleteBookRead = async (br: BookReadProgress) => {
    if (!window.confirm(`Remove "${br.title}" from your reading list?`)) return;
    const res = await fetchWithAuth(`/bookreads/${br.bookReadId}`, token, { method: 'DELETE' });
    if (res.ok) {
      setBookReads(prev => prev.filter(b => b.bookReadId !== br.bookReadId));
    }
  };

  const saveChapterName = async (chapterId: string, googleBookId: string, name: string) => {
    const res = await fetchWithAuth(`/chapters/${chapterId}`, token, {
      method: 'PUT',
      body: JSON.stringify({ name }),
    });
    if (res.ok) {
      setChapters(prev => ({
        ...prev,
        [googleBookId]: prev[googleBookId].map(c => c.id === chapterId ? { ...c, name } : c),
      }));
      setEditName(prev => ({ ...prev, [chapterId]: { editing: false, value: name } }));
    }
  };

  const handleAddChapters = async (googleBookId: string, raw: string) => {
    const lines = raw.split('\n').map(l => l.trim()).filter(Boolean);
    let payload = lines.map((name, i) => ({ name, chapterIndex: i + 1 }));

    // Support entering a single number (e.g. "12") to generate Chapter 1..N.
    if (lines.length === 1 && /^\d+$/.test(lines[0])) {
      const count = parseInt(lines[0], 10);
      payload = Array.from({ length: count }, (_, idx) => ({
        name: `Chapter ${idx + 1}`,
        chapterIndex: idx + 1,
      }));
    }

    if (payload.length === 0) {
      window.alert('Enter chapter names (one per line) or a chapter count.');
      return;
    }

    const res = await fetchWithAuth(`/books/${googleBookId}/chapters`, token, {
      method: 'POST',
      body: JSON.stringify(payload),
    });
    if (res.ok) {
      const updated: Chapter[] = await res.json();
      setChapters(prev => ({ ...prev, [googleBookId]: updated }));
      setChapterInput(prev => ({ ...prev, [googleBookId]: '' }));
    }
  };

  if (loading) return <div className="page"><p>Loading…</p></div>;

  return (
    <div className="page reading-list-page">
      <PageGuidance
        title="Your Reading List 📚"
        description="This is your personal library. Keep track of books you are reading now and books you have started."
        instructions="Check off chapters as you finish them and watch your progress grow. The more you read, the more rewards you can earn."
        tone="child"
      />
      {bookReads.length === 0 && (
        <p className="muted">No books in progress. <a href="/search">Search for a book</a> to get started.</p>
      )}
      {bookReads.map(br => {
        const chapList = chapters[br.googleBookId] ?? [];
        const dateMap = readDates[br.bookReadId] ?? {};
        const readSet = new Set(br.readChapterIds);
        const readCount = readSet.size;
        const total = chapList.length;
        const pct = total > 0 ? Math.round((readCount / total) * 100) : 0;
        const readIds = chapList.filter(c => readSet.has(c.id)).map(c => c.id);
        const mostRecentReadId = readIds.length > 0 ? readIds[readIds.length - 1] : null;

        return (
          <div key={br.bookReadId} className="book-section">
            <div className="book-header">
              {br.thumbnailUrl && <img src={br.thumbnailUrl} alt={br.title} className="book-thumb-sm" />}
              <div className="book-meta">
                <h2>{br.title}</h2>
                <p className="book-authors">{br.authors?.join(', ')}</p>
                {br.bookEarningBasis === 'PER_BOOK' && (
                  <p className="muted">Reward tracks per book. Use Mark as Complete when you finish.</p>
                )}
                {br.bookEarningBasis === 'PER_PAGE_MILESTONE' && (
                  <p className="muted">
                    Page milestones use {br.pageCountConfirmed ? `${br.pageCount} pages` : 'the confirmed page count'}.
                  </p>
                )}
                {total > 0 && (
                  <div className="progress-row">
                    <div className="progress-bar">
                      <div className="progress-fill" style={{ width: `${pct}%` }} />
                    </div>
                    <span className="progress-label">{pct}%</span>
                  </div>
                )}
              </div>
              {br.bookEarningBasis === 'PER_BOOK' && (
                <Button variant="secondary" size="sm" onClick={() => handleMarkComplete(br)}>
                  Mark as Complete
                </Button>
              )}
              <Button variant="secondary" size="sm" className="btn-danger-sm" onClick={() => handleDeleteBookRead(br)} title="Remove book">
                ✕
              </Button>
            </div>

            {chapList.length === 0 && (
              <div className="chapter-add">
                <p className="muted">No chapters yet. Enter a chapter count (e.g. 12) or add chapter names one per line:</p>
                <textarea
                  className="input chapter-textarea"
                  placeholder="12&#10;or&#10;Chapter 1&#10;Chapter 2&#10;Chapter 3"
                  value={(chapterInput as any)[br.googleBookId] ?? ''}
                  onChange={e => setChapterInput(prev => ({ ...prev, [br.googleBookId]: e.target.value }))}
                  rows={5}
                />
                <Button
                  onClick={() => handleAddChapters(br.googleBookId, (chapterInput as any)[br.googleBookId] ?? '')}
                >
                  Save Chapters
                </Button>
              </div>
            )}

            <ul className="chapter-list">
              {chapList.map(ch => {
                const isRead = readSet.has(ch.id);
                const isMostRecent = ch.id === mostRecentReadId;
                const ed = editName[ch.id];
                return (
                  <li key={ch.id} className={`chapter-item ${isRead ? 'read' : ''}`} data-chapter={ch.chapterIndex}>
                    <input
                      type="checkbox"
                      checked={isRead}
                      onChange={() => handleCheck(br, ch, isRead)}
                      disabled={isRead && !isMostRecent}
                    />
                    {ed?.editing ? (
                      <Input
                        className="input input-inline"
                        value={ed.value}
                        autoFocus
                        onChange={e => setEditName(prev => ({ ...prev, [ch.id]: { editing: true, value: e.target.value } }))}
                        onBlur={() => saveChapterName(ch.id, br.googleBookId, ed.value)}
                        onKeyDown={e => e.key === 'Enter' && saveChapterName(ch.id, br.googleBookId, ed.value)}
                      />
                    ) : (
                      <span
                        className="chapter-name"
                        onDoubleClick={() => setEditName(prev => ({ ...prev, [ch.id]: { editing: true, value: ch.name } }))}
                      >
                        {ch.name}
                      </span>
                    )}
                    {isRead && dateMap[ch.id] && (
                      <span className="chapter-date">{dateMap[ch.id]}</span>
                    )}
                  </li>
                );
              })}
            </ul>
          </div>
        );
      })}
    </div>
  );
}
