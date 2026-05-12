import { AuthUser } from '../shared/api';

export type RewardRouteRole = 'PARENT' | 'CHILD';

export function canAccessRewardRoute(user: AuthUser | null, role: RewardRouteRole): boolean {
  return !!user && user.role === role;
}

export const rewardRoutes = {
  parent: '/parent/rewards',
  child: '/child/rewards',
} as const;
