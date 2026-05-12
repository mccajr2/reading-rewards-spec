import { useMemo, useState } from 'react';
import type { ChildProgressTrackingDraft, RewardUnit, TrackingType } from '../../services/rewardApi';

type ProgressTrackerProps = {
  rewardUnit: RewardUnit;
  initialTrackingType?: TrackingType;
  initialTotalChapters?: number | null;
  initialCurrentChapter?: number | null;
  initialTotalPages?: number | null;
  initialCurrentPage?: number | null;
  suggestedPageCount?: number | null;
  onSave: (payload: ChildProgressTrackingDraft) => Promise<void> | void;
};

function parsePositiveInt(raw: string): number | null {
  if (!raw.trim()) {
    return null;
  }
  const parsed = parseInt(raw, 10);
  return Number.isNaN(parsed) ? null : Math.max(parsed, 0);
}

export function ProgressTracker({
  rewardUnit,
  initialTrackingType = 'NONE',
  initialTotalChapters = null,
  initialCurrentChapter = null,
  initialTotalPages = null,
  initialCurrentPage = null,
  suggestedPageCount = null,
  onSave,
}: ProgressTrackerProps) {
  const [trackingType, setTrackingType] = useState<TrackingType>(initialTrackingType);
  const [totalChapters, setTotalChapters] = useState(initialTotalChapters?.toString() ?? '');
  const [currentChapter, setCurrentChapter] = useState(initialCurrentChapter?.toString() ?? '');
  const [totalPages, setTotalPages] = useState(initialTotalPages?.toString() ?? (suggestedPageCount?.toString() ?? ''));
  const [currentPage, setCurrentPage] = useState(initialCurrentPage?.toString() ?? '');
  const [message, setMessage] = useState('');

  const completionError = useMemo(() => {
    if (rewardUnit !== 'PER_CHAPTER') {
      return '';
    }
    if (trackingType !== 'CHAPTERS') {
      return 'Chapter count required to complete this book';
    }
    if ((parsePositiveInt(totalChapters) ?? 0) < 1) {
      return 'Chapter count required to complete this book';
    }
    return '';
  }, [rewardUnit, trackingType, totalChapters]);

  const save = async () => {
    const payload: ChildProgressTrackingDraft = {
      trackingType,
      totalChapters: parsePositiveInt(totalChapters),
      currentChapter: parsePositiveInt(currentChapter),
      totalPages: parsePositiveInt(totalPages),
      currentPage: parsePositiveInt(currentPage),
    };
    await onSave(payload);
    setMessage('Progress tracking saved');
  };

  const validateCompletion = () => {
    setMessage(completionError || 'Ready to mark complete');
  };

  return (
    <section aria-label="Progress tracking" style={{ border: '1px solid #e0e0e0', borderRadius: 8, padding: 12 }}>
      <p style={{ marginTop: 0, fontWeight: 600 }}>Progress tracking</p>
      <label style={{ display: 'block' }}>
        <input
          type="radio"
          name="tracking-mode"
          value="CHAPTERS"
          checked={trackingType === 'CHAPTERS'}
          onChange={() => setTrackingType('CHAPTERS')}
        />{' '}
        Track by chapters
      </label>
      <label style={{ display: 'block' }}>
        <input
          type="radio"
          name="tracking-mode"
          value="PAGES"
          checked={trackingType === 'PAGES'}
          onChange={() => setTrackingType('PAGES')}
        />{' '}
        Track by pages
      </label>
      <label style={{ display: 'block' }}>
        <input
          type="radio"
          name="tracking-mode"
          value="NONE"
          checked={trackingType === 'NONE'}
          onChange={() => setTrackingType('NONE')}
        />{' '}
        Skip tracking
      </label>

      {trackingType === 'CHAPTERS' && (
        <div style={{ display: 'grid', gap: 8, marginTop: 8 }}>
          <label>
            Total chapters
            <input aria-label="Total chapters" type="number" min={1} value={totalChapters} onChange={(e) => setTotalChapters(e.target.value)} />
          </label>
          <label>
            Current chapter
            <input aria-label="Current chapter" type="number" min={0} value={currentChapter} onChange={(e) => setCurrentChapter(e.target.value)} />
          </label>
        </div>
      )}

      {trackingType === 'PAGES' && (
        <div style={{ display: 'grid', gap: 8, marginTop: 8 }}>
          <label>
            Total pages
            <input aria-label="Total pages" type="number" min={1} value={totalPages} onChange={(e) => setTotalPages(e.target.value)} />
          </label>
          <label>
            Current page
            <input aria-label="Current page" type="number" min={0} value={currentPage} onChange={(e) => setCurrentPage(e.target.value)} />
          </label>
        </div>
      )}

      <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
        <button type="button" onClick={() => void save()}>Save progress</button>
        <button type="button" onClick={validateCompletion}>Check completion requirements</button>
      </div>

      {message ? <p role="alert" style={{ marginBottom: 0 }}>{message}</p> : null}
    </section>
  );
}
