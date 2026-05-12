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
  return request<{ availableRewards: RewardTemplate[] }>('/child/rewards/available');
}