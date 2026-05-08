import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { fetchWithAuth } from '../../shared/api';
import { Scanner } from './Scanner';
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
        const chapterCountStr = window.prompt('How many chapters are in this book?');
        const chapterCount = chapterCountStr ? parseInt(chapterCountStr, 10) : 0;

        if (Number.isNaN(chapterCount) || chapterCount < 1) {
          window.alert('Book added. Add chapters from My Reading List when ready.');
        } else {
          const chapters = Array.from({ length: chapterCount }, (_, idx) => ({
            chapterIndex: idx + 1,
            name: `Chapter ${idx + 1}`,
          }));
          await fetchWithAuth(`/bookreads/${added.id}/chapters`, token, {
            method: 'POST',
            body: JSON.stringify(chapters),
          });
        }
      }

      navigate('/reading-list', { state: { newGoogleBookId: added.googleBookId } });
    } finally {
      setAdding(null);
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
      <h1>Search Books</h1>
      <Scanner onResult={handleScan} disabled={loading} />
      <form className="search-form" onSubmit={handleSearch}>
        <div className="search-fields">
          <input
            type="text"
            placeholder="Title"
            value={title}
            onChange={e => setTitle(e.target.value)}
            className="input"
          />
          <input
            type="text"
            placeholder="Author"
            value={author}
            onChange={e => setAuthor(e.target.value)}
            className="input"
          />
          <input
            type="text"
            placeholder="ISBN"
            value={isbn}
            onChange={e => setIsbn(e.target.value)}
            className="input"
          />
        </div>
        <button type="submit" className="btn btn-primary" disabled={loading}>
          {loading ? 'Searching…' : 'Search'}
        </button>
      </form>
      {error && <p className="error-msg">{error}</p>}
      {!loading && results.length === 0 && !error && (
        <p className="muted">Search results will appear here.</p>
      )}
      <div className="book-results">
        {results.map(book => (
          <div key={book.googleBookId} className="book-card">
            {book.thumbnailUrl && (
              <img src={book.thumbnailUrl} alt={book.title} className="book-thumb" />
            )}
            <div className="book-info">
              <h3>{book.title}</h3>
              <p className="book-authors">{book.authors?.join(', ')}</p>
              {book.description && (
                <p className="book-desc">{book.description.slice(0, 200)}{book.description.length > 200 ? '…' : ''}</p>
              )}
            </div>
            <button
              className="btn btn-secondary"
              onClick={() => handleAdd(book)}
              disabled={adding === book.googleBookId}
            >
              {adding === book.googleBookId ? 'Adding…' : '+ Add to Reading List'}
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}
