# Phase 0 Research: Reward Customization Design Decisions

**Date**: 2026-05-11  
**Feature**: Customizable Family Rewards  
**Scope**: Resolve all NEEDS CLARIFICATION items from spec.md

---

## Research Question 1: Parent Deletes Reward While Child is Working Toward It

**Decision**: Archive (Soft-Delete) + In-App Notification + Let Child Finish

**Rationale**:
- Spec already proposes soft-deletion for global rewards (see edge case in spec.md)
- Children perceive deleted rewards as "work lost" → frustration → disengagement
- Loyalty programs (Starbucks, Target) never cancel mid-progress rewards; they "grandfather" active earners
- Soft-delete preserves audit trail and allows recovery for admin use cases

**Implementation**:
1. Parent can only "Archive" a reward, not permanently delete
2. Archives mark `is_deleted=true` in DB but keep historical records
3. Child continues earning against the reward until book completion
4. For new books after archive, reward is hidden from RewardSelector
5. Child receives in-app message: "The $X reward is no longer available for new books, but you can finish this one"

---

## Research Question 2: How to Handle Reward Changes Mid-Book

**Decision**: Freeze Current Book Rate / Apply Changes to New Books Only

**Rationale**:
- "Moving goalposts" is a known UX anti-pattern in gaming, loyalty, and incentive programs
- Children have psychological "expectation lock-in" after starting a task
- Changing the rate mid-progress feels unfair and erodes trust
- Gaming industry standard: "locked-in" rewards at mission start
- Example: DoorDash, Lyft locked rates once a trip starts; game battle passes lock tier rewards

**Implementation**:
1. Capture the `RewardTemplate.amount` and `RewardTemplate.unit` when child selects reward (store in `RewardSelection`)
2. At payout time, use the locked-in values, not the current template values
3. When parent updates `RewardTemplate.amount`, existing `RewardSelection` rows retain old locked-in amount
4. Only new `RewardSelection` rows created after the update use new rates
5. Rationale in code: "Rate locked at time of selection for fairness"

---

## Research Question 3: Email Notification Strategy for Payout Reminders

**Decision**: In-App Primary (Always), Email Secondary (Opt-In by Default, Opt-Out Available)

**Rationale**:
- Spec **FR-012** already states "email notifications enabled by default" for parents
- Family apps must balance transparency with privacy/inbox fatigue (e.g., Google Family Link, Apple Screen Time do in-app first)
- Parents need at least one channel to catch critical payout reminders (child waiting to get paid → retention risk)
- Email helps parents who don't check the app frequently
- Children never receive emails (per **FR-013**) to protect privacy and reduce notification fatigue

**Implementation**:
1. **Child-to-Parent Payout Reminder**: Always sends in-app; optionally sends email if parent has `notify_email_payouts=true` (default true)
2. **Parent Payout Acknowledgment**: Always in-app; no email needed (parent already on platform)
3. **Default Settings**: New parent accounts get `notify_email_payouts=true`; can toggle in Settings
4. **Email Content**: Subject: "{child_name} has earned ${amount} and is waiting for payout", includes link to Manage Rewards page
5. **Rate Limiting**: Max 1 email per day per child (even if child sends 10 reminders, only 1 email sent to parent)

---

## Research Question 4: Can Child Change Reward Selection Mid-Book?

**Decision**: Allowed, Changes Take Effect for Next Unit (Chapter/Book) or Immediately Without Recalculation

**Rationale**:
- Flexibility increases engagement; forcing children to stick with 1 reward per book is rigid
- BUT: Changing mid-chapter gets complex (e.g., "I've earned 1.5 chapters at $0.50, now switching to $1/chapter")
- Best practice: Allow change to take effect on the **next tracking unit** (next chapter, next page, or next book)
- Prevents recalculation complexity and avoids "gaming" the system

**Implementation**:
1. Child can call `PUT /api/child/reward-selection/{bookReadId}` to change reward at any time
2. Change takes effect **immediately** for future progress
3. Already-tracked chapters/pages for current book use the **old** reward rate
4. Example: 5 chapters tracked at $0.50/chapter ($2.50), child switches to $1/chapter, chapters 6+ earn $1/chapter
5. UI shows: "New reward applies to chapters starting from Ch 6"

---

## Research Question 5: Industry Best Practices for Accumulation & Payout Ledgers

**Decision**: Double-Entry Ledger with Transaction History, Status States, and Audit Trail

**Rationale**:
- Loyalty programs (Starbucks balance), gift cards (Apple, Amazon), and mobile gaming (gem systems) all use transaction ledgers
- Spec requires **SC-003** (100% calculation accuracy) and **SC-004** (95% payout reminders acknowledged within 48h)
- Transparent audit trails prevent disputes between parent and child
- Success criteria **SC-003** demands proof of accuracy → immutable transaction log is mandatory

**Implementation**:
1. **Reward Accumulation Entity** (immutable):
   - `accumulation_id`, `child_id`, `book_read_id`, `reward_id`, `amount_earned`, `unit_count`, `status` (EARNED / PENDING / PAID), `payout_date`, `created_at`
   - Append-only; never update amounts
2. **Payout History View**:
   - Run aggregation query: `SUM(amount_earned WHERE status=EARNED)` = Total Earned
   - `SUM(amount_earned WHERE status=PAID)` = Total Paid
   - Difference = Available Balance
3. **Status Transitions**:
   - **EARNED**: Created when book is completed and reward is calculated
   - **PENDING**: When child sends payout reminder (in-app message created)
   - **PAID**: When parent marks payout as complete
4. **Audit Trail**:
   - Message entity links payout reminder to accumulation records
   - Parent can view all reminders and confirmations (linked by `related_reward_id`)
   - Full history visible to both parent and child

---

## Dependencies & External APIs

### OpenLibrary API

- **Endpoint**: `GET https://openlibrary.org/api/books?isbn=<isbn>`
- **Response**: Returns `page_count` field (nullable)
- **Fallback**: If API unavailable or returns null, prompt user to enter page count manually
- **Caching**: Cache results for 30 days to reduce API calls
- **Library Choice**: Use existing `RestTemplate` in Spring or new `WebClient` for async calls

### Email Service

- **Current Integration**: Check if app already has email service (e.g., Brevo, SendGrid, AWS SES)
- **Configuration**: Externalize email template, sender address, and rate limits in `application.yml`
- **Opt-Out**: Store `notify_email_payouts` flag in User preferences table

---

## Architecture Patterns & Guidelines

### Reward Template Immutability

RewardTemplate represents a parent-configured option. Once a child selects it for a book, the selection captures a snapshot of the template's rate (amount, unit). Future updates to the template don't affect historical selections.

### Message-Driven Payout Flow

1. Child initiates `POST /api/child/payout-reminder` → creates Message (type: PAYOUT_REMINDER)
2. Parent receives in-app notification (and optionally email)
3. Parent calls `POST /api/parent/payout-confirm` → updates Accumulation.status → PAID
4. Child receives in-app confirmation

### Progress Tracking Flexibility

- **Per-Chapter Rewards** (mandatory tracking): Chapters are the unit; completion blocked if chapters not entered
- **Per-Book Rewards** (optional tracking): Book is the unit; child can skip chapter tracking and just mark complete
- **Page Tracking** (optional, alternative): Track pages instead of chapters; OpenLibrary API provides default, user can override

---

## Summary of Clarifications Resolved

| Clarification | Answer | Spec Impact |
|---|---|---|
| Delete reward mid-progress? | Soft-delete template; child finishes current book | Edge case in spec |
| Reward changes mid-book? | Freeze rate at selection time; new books get new rate | New design detail |
| Email on every payout reminder? | In-app always; email by default (opt-out) | FR-012 clarified |
| Child change reward mid-book? | Allowed; takes effect next chapter/book | User Story 4 clarified |
| Accumulation tracking? | Double-entry ledger, immutable transaction log | SC-003 & SC-004 support |

---

**Next Phase**: Phase 1 - Design & Contracts (data-model.md, contract definitions, quickstart.md)
