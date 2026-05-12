import { ReadingListPage } from '../features/books/ReadingListPage';
import {
  getChildProgressTracking,
  selectChildReward,
  updateChildProgressTracking,
  type ChildProgressTrackingDraft,
} from '../services/rewardApi';

// Compatibility wrapper used by feature task paths.
// Reward selection is integrated during add-book flow before users return to the reading list.
export function ReadingList() {
  return <ReadingListPage />;
}

export async function attachRewardSelectionToBookRead(bookReadId: string, rewardTemplateId: string) {
  return selectChildReward(bookReadId, rewardTemplateId);
}

export async function loadProgressTrackingForBookRead(bookReadId: string) {
  return getChildProgressTracking(bookReadId);
}

export async function saveProgressTrackingForBookRead(bookReadId: string, payload: ChildProgressTrackingDraft) {
  return updateChildProgressTracking(bookReadId, payload);
}
