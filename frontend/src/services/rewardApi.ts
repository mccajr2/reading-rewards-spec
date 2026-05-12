const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api';

export type RewardType = 'MONEY' | 'TIME' | 'CUSTOM_TEXT';
export type RewardUnit = 'PER_CHAPTER' | 'PER_BOOK';
export type RewardFrequency = 'IMMEDIATE' | 'ON_COMPLETION';

export type RewardTemplate = {
  rewardTemplateId: string;
  childId: string | null;
  rewardType: RewardType;
  amount: number;
  unit: RewardUnit;
  frequency: RewardFrequency;
  cannedTemplateId: string | null;
  description: string;
  isDeleted: boolean;
  createdAt: string;
};

export type PerChildRewards = {
  childId: string;
  childName: string;
  rewards: RewardTemplate[];
};

export type ParentRewardsResponse = {
  familyRewards: RewardTemplate[];
  perChildRewards: PerChildRewards[];
};

export type RewardTemplateDraft = Omit<RewardTemplate, 'rewardTemplateId' | 'isDeleted' | 'createdAt'>;

export type CannedRewardTemplate = {
  cannedTemplateId: string;
  rewardType: RewardType;
  amount: number;
  unit: RewardUnit;
  frequency: RewardFrequency;
  description: string;
};

export type AvailableRewardTemplate = {
  rewardTemplateId: string;
  rewardType: RewardType;
  amount: number;
  unit: RewardUnit;
  frequency: RewardFrequency;
  description: string;
  cannedTemplateId: string | null;
  scope: 'FAMILY' | 'PER_CHILD';
};

export type ChildAvailableRewardsResponse = {
  childId: string;
  availableRewards: AvailableRewardTemplate[];
  defaultRewardId: string | null;
};

export type RewardAccumulationStatus = 'EARNED' | 'PENDING_PAYOUT' | 'PAID';
export type TrackingType = 'CHAPTERS' | 'PAGES' | 'NONE';

export type RewardAccumulation = {
  id?: string;
  accumulationId?: string;
  amountEarned: number;
  unitCount?: number;
  calculationNote?: string;
  status: RewardAccumulationStatus;
  payoutDate?: string | null;
  createdAt: string;
};

export type ChildRewardBalanceResponse = {
  childId: string;
  balance: {
    totalEarned: number;
    totalPaid: number;
    availableBalance: number;
  };
  byRewardType: Array<{
    rewardType: RewardType;
    description: string;
    totalEarned: number;
    totalPaid: number;
    availableBalance: number;
    unitLabel: string;
    accent: 'money' | 'time' | 'custom';
  }>;
};

export type ChildRewardHistoryResponse = {
  childId: string;
  accumulations: RewardAccumulation[];
};

export type ParentChildAccumulationResponse = {
  childId: string;
  childName: string;
  summary: {
    totalEarned: number;
    totalPaid: number;
    availableBalance: number;
  };
  accumulations: RewardAccumulation[];
};

export type ChildProgressTracking = {
  bookReadId: string;
  trackingType: TrackingType;
  totalChapters: number | null;
  currentChapter: number | null;
  totalPages: number | null;
  currentPage: number | null;
  suggestedPageCount: number | null;
  rewardUnit: RewardUnit;
  completionAllowed: boolean;
};

export type ChildProgressTrackingDraft = {
  trackingType: TrackingType;
  totalChapters?: number | null;
  currentChapter?: number | null;
  totalPages?: number | null;
  currentPage?: number | null;
};

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, init);
  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `Request failed with status ${response.status}`);
  }

  const text = await response.text();
  return text ? (JSON.parse(text) as T) : ({} as T);
}

export async function listParentRewards(): Promise<ParentRewardsResponse> {
  return request<ParentRewardsResponse>('/parent/rewards');
}

export async function listCannedRewardTemplates(): Promise<CannedRewardTemplate[]> {
  return request<CannedRewardTemplate[]>('/parent/rewards/canned');
}

export async function createParentFamilyReward(payload: RewardTemplateDraft): Promise<RewardTemplate> {
  return request<RewardTemplate>('/parent/rewards', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });
}

export async function createParentPerChildReward(childId: string, payload: RewardTemplateDraft): Promise<RewardTemplate> {
  return request<RewardTemplate>(`/parent/rewards/child/${childId}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });
}

export async function updateParentFamilyReward(rewardTemplateId: string, payload: RewardTemplateDraft): Promise<RewardTemplate> {
  return request<RewardTemplate>(`/parent/rewards/${rewardTemplateId}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });
}

export async function archiveParentFamilyReward(rewardTemplateId: string): Promise<RewardTemplate> {
  return request<RewardTemplate>(`/parent/rewards/${rewardTemplateId}`, {
    method: 'DELETE',
  });
}

export async function listChildAvailableRewards(): Promise<{ availableRewards: RewardTemplate[] }> {
  return request<ChildAvailableRewardsResponse>('/child/rewards/available');
}

export async function selectChildReward(bookReadId: string, rewardTemplateId: string): Promise<{ selectionId: string }> {
  return request<{ selectionId: string }>(`/child/rewards/select/${bookReadId}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ rewardTemplateId }),
  });
}

export async function changeChildReward(bookReadId: string, rewardTemplateId: string): Promise<{ selectionId: string }> {
  return request<{ selectionId: string }>(`/child/rewards/select/${bookReadId}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ rewardTemplateId }),
  });
}

export async function getChildRewardBalance(): Promise<ChildRewardBalanceResponse> {
  return request<ChildRewardBalanceResponse>('/child/rewards/balance');
}

export async function getChildRewardHistory(status?: RewardAccumulationStatus): Promise<ChildRewardHistoryResponse> {
  const query = status ? `?status=${encodeURIComponent(status)}` : '';
  return request<ChildRewardHistoryResponse>(`/child/rewards/history${query}`);
}

export async function getParentChildAccumulation(childId: string, status?: RewardAccumulationStatus): Promise<ParentChildAccumulationResponse> {
  const query = status ? `?status=${encodeURIComponent(status)}` : '';
  return request<ParentChildAccumulationResponse>(`/parent/rewards/child/${childId}/accumulation${query}`);
}

export async function confirmParentPayout(childId: string, accumulationIds: string[], payoutMethod = ''): Promise<Record<string, unknown>> {
  return request<Record<string, unknown>>(`/parent/rewards/child/${childId}/payout-confirm`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ accumulationIds, payoutMethod }),
  });
}

export async function getChildProgressTracking(bookReadId: string): Promise<ChildProgressTracking> {
  return request<ChildProgressTracking>(`/child/rewards/progress/${bookReadId}`);
}

export async function updateChildProgressTracking(bookReadId: string, payload: ChildProgressTrackingDraft): Promise<ChildProgressTracking> {
  return request<ChildProgressTracking>(`/child/rewards/progress/${bookReadId}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });
}

export async function validateChildCompletion(bookReadId: string): Promise<{ allowed: boolean; message?: string }> {
  return request<{ allowed: boolean; message?: string }>(`/child/rewards/progress/${bookReadId}/validate-complete`, {
    method: 'POST',
  });
}