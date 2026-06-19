/**
 * services/settlementService.ts
 *
 * Re-exports settlement and messaging API helpers from the shared API module
 * so that they can be imported from a consistent `services/` path.
 *
 * Tasks that reference `frontend/src/services/` should import from here.
 */
export {
  approveSettlementRequest,
  cancelSettlementRequest,
  createSettlementRequest,
  getAllMessages,
  getMessageInbox,
  listParentPendingRequests,
  listSettlementRequests,
  rejectSettlementRequest,
  sendEncouragement,
  sendNudge,
} from '../shared/api';

export type {
  FamilyMessageDto,
  MessageType,
  SettlementRequestDto,
  SettlementRequestStatus,
  SettlementRequestType,
} from '../shared/api';
