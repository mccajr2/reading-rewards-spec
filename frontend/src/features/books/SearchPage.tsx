import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { fetchWithAuth } from '../../shared/api';
import { RewardSelector } from '../../components/rewards/RewardSelector';
import {
  listChildAvailableRewards,
  selectChildReward,
  type AvailableRewardTemplate,
} from '../../services/rewardApi';
import { Scanner } from './Scanner';
import { Button, Card, CardContent, FormField, Input, Modal, PageGuidance } from '../../components/shared';
import './SearchPage.css';

type BookResult = {
  googleBookId: string;
  title: string;
  authors: string[];
  description: string;
  thumbnailUrl: string;
};

type AddBookResponse = {
  id: string;
  googleBookId: string;
};

type PendingChapterSeed = {
  bookReadId: string;
  googleBookId: string;
  title: string;
};

type PendingRewardSelection = {
  bookReadId: string;
  googleBookId: string;
  title: string;
  shouldSeedChapters: boolean;
};

export function SearchPage() {
  const { token } = useAuth();
  const navigate = useNavigate();
  const [title, setTitle] = useState('');
  const [author, setAuthor] = useState('');
  const [isbn, setIsbn] = useState('');
  const [results, setResults] = useState<BookResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [adding, setAdding] = useState<string | null>(null);
  const [error, setError] = useState('');
  const [pendingChapterSeed, setPendingChapterSeed] = useState<PendingChapterSeed | null>(null);
  const [pendingRewardSelection, setPendingRewardSelection] = useState<PendingRewardSelection | null>(null);
  const [availableRewards, setAvailableRewards] = useState<AvailableRewardTemplate[]>([]);
  const [selectedRewardTemplateId, setSelectedRewardTemplateId] = useState('');
  const [savingRewardSelection, setSavingRewardSelection] = useState(false);
  const [rewardSelectionError, setRewardSelectionError] = useState('');
  const [chapterCount, setChapterCount] = useState('');
  const [chapterCountError, setChapterCountError] = useState('');
  const [savingChapters, setSavingChapters] = useState(false);

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setResults([]);
    if (!title && !author && !isbn) {
      setError('Enter a title, author, or ISBN to search.');
      return;
    }
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (title) params.set('title', title);
      if (author) params.set('author', author);
      if (isbn) params.set('isbn', isbn);
      const res = await fetchWithAuth(`/search?${params}`, token);
      if (!res.ok) throw new Error('Search failed');
      const books = await res.json();
      setResults(books);
      if (books.length === 0) setError('No results found. Try a different title or author.');
    } catch {
      setError('Search unavailable. Open Library may be temporarily unavailable — try again in a moment.');
    } finally {
      setLoading(false);
    }
  };

  const handleAdd = async (book: BookResult) => {
    setAdding(book.googleBookId);
    try {
      const res = await fetchWithAuth('/books', token, {
        method: 'POST',
        body: JSON.stringify(book),
      });
      if (!res.ok) return;

      const added: AddBookResponse = await res.json();

      // Shared chapter definitions are global per book. If chapters already exist,
      // reuse them; otherwise seed a default list from the user's chapter count.
      const existingChaptersRes = await fetchWithAuth(
        `/bookreads/${added.id}/chapters`,
        token
      );

      let shouldSeedChapters = true;
      if (existingChaptersRes.ok) {
        const existing = await existingChaptersRes.json();
        shouldSeedChapters = !Array.isArray(existing) || existing.length === 0;
      }

      if (shouldSeedChapters) {
        try {
          const rewardsResponse = await listChildAvailableRewards();
          const rewards = rewardsResponse.availableRewards ?? [];
          if (rewards.length > 0) {
            setAvailableRewards(rewards);
            setSelectedRewardTemplateId(rewardsResponse.defaultRewardId ?? rewards[0].rewardTemplateId);
            setRewardSelectionError('');
            setPendingRewardSelection({
              bookReadId: added.id,
              googleBookId: added.googleBookId,
              title: book.title,
              shouldSeedChapters,
            });
            return;
          }
        } catch {
          // If reward selection cannot be loaded, continue with existing chapter setup behavior.
        }

        setChapterCount('');
        setChapterCountError('');
        setPendingChapterSeed({
          bookReadId: added.id,
          googleBookId: added.googleBookId,
          title: book.title,
        });
        return;
      }

      navigate('/reading-list', { state: { newGoogleBookId: added.googleBookId } });
    } finally {
      setAdding(null);
    }
  };

  const closeRewardModal = (proceedToReadingList: boolean) => {
    if (proceedToReadingList && pendingRewardSelection) {
      navigate('/reading-list', { state: { newGoogleBookId: pendingRewardSelection.googleBookId } });
    }
    setPendingRewardSelection(null);
    setAvailableRewards([]);
    setSelectedRewardTemplateId('');
    setSavingRewardSelection(false);
    setRewardSelectionError('');
  };

  const submitRewardSelection = async () => {
    if (!pendingRewardSelection) return;
    if (!selectedRewardTemplateId) {
      setRewardSelectionError('Select a reward option to continue.');
      return;
    }

    setSavingRewardSelection(true);
    setRewardSelectionError('');
    try {
      await selectChildReward(pendingRewardSelection.bookReadId, selectedRewardTemplateId);
      const shouldSeedChapters = pendingRewardSelection.shouldSeedChapters;
      const next = {
        bookReadId: pendingRewardSelection.bookReadId,
        googleBookId: pendingRewardSelection.googleBookId,
        title: pendingRewardSelection.title,
      };
      setPendingRewardSelection(null);
      setAvailableRewards([]);
      setSelectedRewardTemplateId('');
      setSavingRewardSelection(false);
      if (shouldSeedChapters) {
        setChapterCount('');
        setChapterCountError('');
        setPendingChapterSeed(next);
      } else {
        navigate('/reading-list', { state: { newGoogleBookId: pendingRewardSelection.googleBookId } });
      }
    } catch {
      setSavingRewardSelection(false);
      setRewardSelectionError('Could not save reward selection right now.');
    }
  };

  const closeChapterSeedModal = (navigateToReadingList: boolean) => {
    if (navigateToReadingList && pendingChapterSeed) {
      navigate('/reading-list', { state: { newGoogleBookId: pendingChapterSeed.googleBookId } });
    }
    setPendingChapterSeed(null);
    setChapterCount('');
    setChapterCountError('');
    setSavingChapters(false);
  };

  const submitChapterSeed = async () => {
    if (!pendingChapterSeed) return;

    const parsedCount = parseInt(chapterCount, 10);
    if (Number.isNaN(parsedCount) || parsedCount < 1) {
      setChapterCountError('Enter a chapter count of 1 or more.');
      return;
    }

    setSavingChapters(true);
    setChapterCountError('');
    try {
      const chapters = Array.from({ length: parsedCount }, (_, idx) => ({
        chapterIndex: idx + 1,
        name: `Chapter ${idx + 1}`,
      }));
      const res = await fetchWithAuth(`/bookreads/${pendingChapterSeed.bookReadId}/chapters`, token, {
        method: 'POST',
        body: JSON.stringify(chapters),
      });
      if (!res.ok) {
        setChapterCountError('Could not save chapters right now. You can add them later from My Reading List.');
        setSavingChapters(false);
        return;
      }
      closeChapterSeedModal(true);
    } catch {
      setChapterCountError('Could not save chapters right now. You can add them later from My Reading List.');
      setSavingChapters(false);
    }
  };

  const handleScan = async (isbnOrUpc: string) => {
    setError('');
    setResults([]);
    setIsbn(isbnOrUpc);
    setLoading(true);
    try {
      const params = new URLSearchParams({ isbn: isbnOrUpc });
      const res = await fetchWithAuth(`/search?${params}`, token);
      if (!res.ok) throw new Error('Search failed');
      setResults(await res.json());
    } catch {
      setError('Could not find book for scanned barcode.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page search-page">
      <PageGuidance
        title="Find Your Next Book 🔍"
        description="Search for any book and add it to your reading list. You can search by title, author, or ISBN."
        instructions="Type in the search box to find a book, then pick Add to Reading List to save it for later."
        tone="child"
      />
      <Scanner onResult={handleScan} disabled={loading} />
      <form className="search-form" onSubmit={handleSearch}>
        <div className="search-fields">
          <Input
            type="text"
            placeholder="Title"
            value={title}
            onChange={e => setTitle(e.target.value)}
            className="input"
          />
          <Input
            type="text"
            placeholder="Author"
            value={author}
            onChange={e => setAuthor(e.target.value)}
            className="input"
          />
          <Input
            type="text"
            placeholder="ISBN"
            value={isbn}
            onChange={e => setIsbn(e.target.value)}
            className="input"
          />
        </div>
        <Button type="submit" disabled={loading}>
          {loading ? 'Searching…' : 'Search'}
        </Button>
      </form>
      {error && <p className="error-msg">{error}</p>}
      {!loading && results.length === 0 && !error && (
        <p className="muted">Search results will appear here.</p>
      )}
      <div className="book-results">
        {results.map(book => (
          <Card key={book.googleBookId} className="book-card">
            {book.thumbnailUrl && (
              <img src={book.thumbnailUrl} alt={book.title} className="book-thumb" />
            )}
            <CardContent className="book-info">
              <h3>{book.title}</h3>
              <p className="book-authors">{book.authors?.join(', ')}</p>
              {book.description && (
                <p className="book-desc">{book.description.slice(0, 200)}{book.description.length > 200 ? '…' : ''}</p>
              )}
            </CardContent>
            <Button
              variant="secondary"
              onClick={() => handleAdd(book)}
              disabled={adding === book.googleBookId}
            >
              {adding === book.googleBookId ? 'Adding…' : '+ Add to Reading List'}
            </Button>
          </Card>
        ))}
      </div>
      <Modal
        open={Boolean(pendingRewardSelection)}
        onOpenChange={open => {
          if (!open) {
            closeRewardModal(true);
          }
        }}
        title="Pick A Reward"
        description={pendingRewardSelection
          ? `Choose what you want to earn for reading "${pendingRewardSelection.title}".`
          : undefined}
        footer={(
          <>
            <Button variant="secondary" onClick={() => closeRewardModal(true)} disabled={savingRewardSelection}>
              Add Later
            </Button>
            <Button onClick={submitRewardSelection} disabled={savingRewardSelection}>
              {savingRewardSelection ? 'Saving…' : 'Save Reward'}
            </Button>
          </>
        )}
      >
        <div className="grid gap-4">
          <RewardSelector
            rewards={availableRewards}
            selectedRewardTemplateId={selectedRewardTemplateId}
            onSelect={setSelectedRewardTemplateId}
          />
          {rewardSelectionError && <p role="alert">{rewardSelectionError}</p>}
        </div>
      </Modal>
      <Modal
        open={Boolean(pendingChapterSeed)}
        onOpenChange={open => {
          if (!open) {
            closeChapterSeedModal(true);
          }
        }}
        title="Set Up Chapters"
        description={pendingChapterSeed
          ? `"${pendingChapterSeed.title}" is now in your reading list. Add the total number of chapters so we can create a starting checklist.`
          : undefined}
        footer={(
          <>
            <Button variant="secondary" onClick={() => closeChapterSeedModal(true)} disabled={savingChapters}>
              Add Later
            </Button>
            <Button onClick={submitChapterSeed} disabled={savingChapters}>
              {savingChapters ? 'Saving…' : 'Save Chapters'}
            </Button>
          </>
        )}
      >
        <div className="grid gap-4">
          <FormField
            label="Chapter count"
            type="number"
            inputMode="numeric"
            min={1}
            step={1}
            value={chapterCount}
            error={chapterCountError || undefined}
            helperText="We will create shared chapter names like Chapter 1, Chapter 2, and so on."
            onChange={e => {
              setChapterCount(e.target.value);
              if (chapterCountError) {
                setChapterCountError('');
              }
            }}
            onKeyDown={e => {
              if (e.key === 'Enter') {
                e.preventDefault();
                void submitChapterSeed();
              }
            }}
          />
        </div>
      </Modal>
    </div>
  );
}
