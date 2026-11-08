// Generated from docs/openapi.json. Run npm run api:generate; do not edit by hand.
export type Problem = { "type": string; "title": string; "status": number; "detail"?: string; "instance"?: string; "code": string; "traceId"?: string; "fieldErrors"?: Array<{ "field": string; "code": string; "message": string }> };
export type Profile = { "id": string; "name": string; "email": string; "phone": string; "version": number };
export type ProfileUpdate = { "name": string; "email": string; "phone": string; "expectedVersion": number };
export type Preferences = { "language": "tr" | "en"; "inApp": boolean; "push": boolean; "emailNotifications": boolean; "version": number };
export type PreferencesUpdate = { "language": "tr" | "en"; "inApp": boolean; "push": boolean; "emailNotifications": boolean; "expectedVersion": number };
export type Session = { "user": Profile; "roles": Array<"MANAGER" | "RESIDENT">; "expiresAt": string; "csrfToken": string; "permissions": Array<string> };
export type CsrfToken = { "csrfToken": string };
export type LoginRequest = { "email": string; "password": string };
export type ResetRequest = { "email": string };
export type PasswordReset = { "token": string; "password": string };
export type Community = { "id": string; "name": string; "address": string; "timeZone": "Europe/Istanbul"; "currency": "TRY" };
export type Unit = { "id": string; "communityId": string; "label": string; "blockId": string; "blockLabel": string; "floor": number; "kind": "APARTMENT" | "SHOP" | "OFFICE"; "areaSqm": number; "version": number };
export type CommunityContext = { "community": Community; "roles": Array<"MANAGER" | "RESIDENT">; "ownUnits": Array<Unit>; "permissions": Array<string>; "profile": Profile };
export type DirectoryPerson = { "id": string; "name": string };
export type DirectoryUnit = { "id": string; "label": string; "blockId": string; "blockLabel": string; "isMyUnit": boolean; "residents": Array<DirectoryPerson> };
export type Relationship = { "id": string; "unitId": string; "person": DirectoryPerson; "type": "OWNER" | "TENANT"; "startsOn": string; "endsOn": string | null };
export type FinancialAccount = { "id": string; "name": string; "kind": "BANK" | "CASH"; "currency": "TRY"; "openingMinor": number; "balanceMinor": number; "version": number };
export type Allocation = { "chargeId": string; "amountMinor": number };
export type Charge = { "id": string; "unitId": string; "description": string; "periodStart": string; "periodEnd": string; "issuedOn": string; "dueOn": string; "amountMinor": number; "currency": "TRY"; "allocatedMinor": number; "outstandingMinor": number; "status": "OUTSTANDING" | "PARTIALLY_PAID" | "PAID" | "CANCELLED"; "cancelledAt"?: string | null; "cancellationReason"?: string; "version": number };
export type ChargeBatchRequest = { "items": Array<{ "unitId": string; "description": string; "periodStart": string; "periodEnd": string; "dueOn": string; "amountMinor": number; "currency": "TRY"; "expectedAccountVersion": number }> };
export type ChargeBatch = { "id": string; "status": "COMPLETED"; "charges": Array<Charge>; "createdAt": string };
export type ReasonCommand = { "reason": string; "expectedVersion": number };
export type PaymentCreate = { "unitId": string; "accountId": string; "amountMinor": number; "currency": "TRY"; "paidOn": string; "reference": string; "allocations": Array<Allocation>; "notificationId"?: string; "expectedAccountVersion": number };
export type Payment = { "id": string; "unitId": string; "accountId": string; "amountMinor": number; "currency": "TRY"; "paidOn": string; "reference": string; "allocations": Array<Allocation>; "unallocatedMinor": number; "notificationId"?: string | null; "status": "RECORDED" | "REVERSED"; "reversedAt"?: string | null; "reversalReason"?: string; "version": number };
export type UnitAccount = { "unitId": string; "currency": "TRY"; "chargedMinor": number; "paidMinor": number; "outstandingMinor": number; "creditMinor": number; "version": number };
export type AccountEntry = { "id": string; "unitId": string; "type": "CHARGE" | "PAYMENT" | "CHARGE_CANCELLATION" | "PAYMENT_REVERSAL"; "amountMinor": number; "occurredAt": string; "description": string; "sourceId": string; "balanceAfterMinor": number };
export type PaymentNotificationCreate = { "unitId": string; "amountMinor": number; "currency": "TRY"; "paymentDate": string; "reference": string; "note": string; "receiptId"?: string };
export type PaymentNotification = { "id": string; "unitId": string; "amountMinor": number; "currency": "TRY"; "paymentDate": string; "reference": string; "note": string; "receipt"?: Attachment; "status": "PENDING" | "REVIEWED" | "REJECTED" | "WITHDRAWN" | "RECORDED"; "reviewNote": string; "paymentId": string | null; "version": number };
export type NotificationReview = { "decision": "REVIEWED" | "REJECTED"; "note": string; "expectedVersion": number };
export type PaymentLink = { "paymentId": string; "expectedVersion": number; "expectedPaymentVersion": number };
export type VersionCommand = { "expectedVersion": number };
export type ExpenseCreate = { "description": string; "category": "MAINTENANCE" | "UTILITIES" | "CLEANING" | "OTHER"; "vendor": string; "incurredOn": string; "amountMinor": number; "currency": "TRY"; "documentId"?: string; "immediatePayment"?: { "accountId": string; "paidOn": string } };
export type Expense = { "id": string; "description": string; "category": "MAINTENANCE" | "UTILITIES" | "CLEANING" | "OTHER"; "vendor": string; "incurredOn": string; "amountMinor": number; "currency": "TRY"; "paidMinor": number; "document"?: Attachment; "version": number };
export type ExpensePaymentCreate = { "accountId": string; "paidOn": string; "amountMinor": number; "expectedVersion": number };
export type ExpensePayment = { "id": string; "expenseId": string; "accountId": string; "paidOn": string; "amountMinor": number; "status": "RECORDED" | "REVERSED"; "reversalReason": string; "version": number };
export type Attachment = { "id": string; "fileName": string; "mediaType": "application/pdf" | "image/png" | "image/jpeg" | "image/webp"; "sizeBytes": number; "purpose": "REQUEST_PHOTO" | "PAYMENT_RECEIPT" | "EXPENSE_DOCUMENT"; "contentUrl": string; "createdAt": string };
export type Upload = { "purpose": "REQUEST_PHOTO" | "PAYMENT_RECEIPT" | "EXPENSE_DOCUMENT"; "file": Blob };
export type RequestCreate = { "unitId"?: string; "title": string; "location": string; "description": string; "photoIds": Array<string> };
export type CommentCreate = { "body": string };
export type Comment = { "id": string; "author": DirectoryPerson; "body": string; "createdAt": string };
export type RequestEvent = { "id": string; "kind": "CREATED" | "STATUS_CHANGED" | "ASSIGNEE_CHANGED"; "actor": DirectoryPerson; "createdAt": string; "from"?: string; "to"?: string };
export type Request = { "id": string; "displayNumber": number; "communityId": string; "unitId": string | null; "title": string; "location": string; "description": string; "status": "OPEN" | "IN_PROGRESS" | "RESOLVED"; "creator": DirectoryPerson; "assignee"?: DirectoryPerson; "photos": Array<Attachment>; "comments": Array<Comment>; "history": Array<RequestEvent>; "createdAt": string; "version": number };
export type RequestStatusChange = { "status": "OPEN" | "IN_PROGRESS" | "RESOLVED"; "expectedVersion": number };
export type RequestAssignment = { "assigneeId": string | null; "expectedVersion": number };
export type Audience = { "type": "ALL" | "BLOCKS" | "UNITS"; "ids": Array<string> };
export type AnnouncementWrite = { "title": string; "body": string; "category": "MAINTENANCE" | "COMMUNITY"; "audience": Audience; "status": "DRAFT" | "PUBLISHED" };
export type AnnouncementUpdate = { "title": string; "body": string; "category": "MAINTENANCE" | "COMMUNITY"; "audience": Audience; "status": "DRAFT" | "PUBLISHED"; "expectedVersion": number };
export type Announcement = { "id": string; "title": string; "body": string; "category": "MAINTENANCE" | "COMMUNITY"; "audience": Audience; "status": "DRAFT" | "PUBLISHED"; "author": DirectoryPerson; "createdAt": string; "updatedAt": string; "publishedAt": string | null; "version": number };
export type Notification = { "id": string; "type": "ANNOUNCEMENT" | "CHARGE" | "PAYMENT" | "REQUEST" | "SYSTEM"; "title": string; "body": string; "target": { "kind": "OVERVIEW" | "ANNOUNCEMENT" | "ACCOUNT" | "REQUEST" | "PAYMENT_NOTIFICATION"; "id": string | null }; "createdAt": string; "readAt": string | null };
export type ReadAll = { "before": string };
export type Dashboard = { "communityId": string; "currency": "TRY"; "period": string; "nextDueOn": string | null; "collectedMinor": number; "outstandingMinor": number; "targetMinor": number; "paidUnitCount": number; "unitCount": number; "openRequestCount": number; "ownAccount"?: UnitAccount };
export type CommunityPage = { "items": Array<Community>; "nextCursor": string | null; "hasMore": boolean };
export type UnitPage = { "items": Array<Unit>; "nextCursor": string | null; "hasMore": boolean };
export type DirectoryUnitPage = { "items": Array<DirectoryUnit>; "nextCursor": string | null; "hasMore": boolean };
export type RelationshipPage = { "items": Array<Relationship>; "nextCursor": string | null; "hasMore": boolean };
export type FinancialAccountPage = { "items": Array<FinancialAccount>; "nextCursor": string | null; "hasMore": boolean };
export type ChargePage = { "items": Array<Charge>; "nextCursor": string | null; "hasMore": boolean };
export type PaymentPage = { "items": Array<Payment>; "nextCursor": string | null; "hasMore": boolean };
export type UnitAccountPage = { "items": Array<UnitAccount>; "nextCursor": string | null; "hasMore": boolean };
export type AccountEntryPage = { "items": Array<AccountEntry>; "nextCursor": string | null; "hasMore": boolean };
export type PaymentNotificationPage = { "items": Array<PaymentNotification>; "nextCursor": string | null; "hasMore": boolean };
export type ExpensePage = { "items": Array<Expense>; "nextCursor": string | null; "hasMore": boolean };
export type ExpensePaymentPage = { "items": Array<ExpensePayment>; "nextCursor": string | null; "hasMore": boolean };
export type RequestPage = { "items": Array<Request>; "nextCursor": string | null; "hasMore": boolean };
export type CommentPage = { "items": Array<Comment>; "nextCursor": string | null; "hasMore": boolean };
export type RequestEventPage = { "items": Array<RequestEvent>; "nextCursor": string | null; "hasMore": boolean };
export type AnnouncementPage = { "items": Array<Announcement>; "nextCursor": string | null; "hasMore": boolean };
export type NotificationPage = { "items": Array<Notification>; "nextCursor": string | null; "hasMore": boolean };
export type DirectoryPersonPage = { "items": Array<DirectoryPerson>; "nextCursor": string | null; "hasMore": boolean };

export interface Operations {
  getCsrf: { response: CsrfToken }
  login: { body: LoginRequest; response: Session }
  getSession: { response: Session }
  logout: { response: undefined }
  requestPasswordReset: { body: ResetRequest; response: undefined }
  resetPassword: { body: PasswordReset; response: undefined }
  getProfile: { response: Profile }
  updateProfile: { body: ProfileUpdate; response: Profile }
  getPreferences: { response: Preferences }
  updatePreferences: { body: PreferencesUpdate; response: Preferences }
  listCommunities: { query?: { cursor?: string; limit?: number }; response: CommunityPage }
  getContext: { params: { communityId: string }; response: CommunityContext }
  getDashboard: { params: { communityId: string }; query?: { unitId?: string; period?: string }; response: Dashboard }
  listUnits: { params: { communityId: string }; query?: { cursor?: string; limit?: number; q?: string; blockId?: string }; response: UnitPage }
  getUnit: { params: { communityId: string; unitId: string }; response: Unit }
  listRelationships: { params: { communityId: string; unitId: string }; query?: { cursor?: string; limit?: number }; response: RelationshipPage }
  listDirectory: { params: { communityId: string }; query?: { cursor?: string; limit?: number; q?: string; blockId?: string; mine?: boolean }; response: DirectoryUnitPage }
  getDirectoryUnit: { params: { communityId: string; unitId: string }; response: DirectoryUnit }
  listFinancialAccounts: { params: { communityId: string }; query?: { cursor?: string; limit?: number }; response: FinancialAccountPage }
  listUnitAccounts: { params: { communityId: string }; query?: { cursor?: string; limit?: number }; response: UnitAccountPage }
  getUnitAccount: { params: { communityId: string; unitId: string }; response: UnitAccount }
  listAccountEntries: { params: { communityId: string; unitId: string }; query?: { cursor?: string; limit?: number; from?: string; to?: string }; response: AccountEntryPage }
  listCharges: { params: { communityId: string }; query?: { cursor?: string; limit?: number; unitId?: string; status?: "OUTSTANDING" | "PARTIALLY_PAID" | "PAID" | "CANCELLED" }; response: ChargePage }
  createChargeBatch: { params: { communityId: string }; body: ChargeBatchRequest; idempotencyKey: string; response: ChargeBatch }
  getChargeBatch: { params: { communityId: string; batchId: string }; response: ChargeBatch }
  cancelCharge: { params: { communityId: string; chargeId: string }; body: ReasonCommand; idempotencyKey: string; response: Charge }
  listPayments: { params: { communityId: string }; query?: { cursor?: string; limit?: number; unitId?: string }; response: PaymentPage }
  recordPayment: { params: { communityId: string }; body: PaymentCreate; idempotencyKey: string; response: Payment }
  reversePayment: { params: { communityId: string; paymentId: string }; body: ReasonCommand; idempotencyKey: string; response: Payment }
  listPaymentNotifications: { params: { communityId: string }; query?: { cursor?: string; limit?: number; unitId?: string; status?: "PENDING" | "REVIEWED" | "REJECTED" | "WITHDRAWN" | "RECORDED" }; response: PaymentNotificationPage }
  createPaymentNotification: { params: { communityId: string }; body: PaymentNotificationCreate; response: PaymentNotification }
  reviewPaymentNotification: { params: { communityId: string; notificationId: string }; body: NotificationReview; response: PaymentNotification }
  withdrawPaymentNotification: { params: { communityId: string; notificationId: string }; body: VersionCommand; response: PaymentNotification }
  linkPaymentNotification: { params: { communityId: string; notificationId: string }; body: PaymentLink; idempotencyKey: string; response: PaymentNotification }
  listExpensePayments: { params: { communityId: string }; query?: { cursor?: string; limit?: number; expenseId?: string }; response: ExpensePaymentPage }
  listExpenses: { params: { communityId: string }; query?: { cursor?: string; limit?: number }; response: ExpensePage }
  createExpense: { params: { communityId: string }; body: ExpenseCreate; idempotencyKey: string; response: Expense }
  getExpense: { params: { communityId: string; expenseId: string }; response: Expense }
  payExpense: { params: { communityId: string; expenseId: string }; body: ExpensePaymentCreate; idempotencyKey: string; response: ExpensePayment }
  reverseExpensePayment: { params: { communityId: string; expensePaymentId: string }; body: ReasonCommand; idempotencyKey: string; response: ExpensePayment }
  listAssignableStaff: { params: { communityId: string }; query?: { cursor?: string; limit?: number }; response: DirectoryPersonPage }
  listRequests: { params: { communityId: string }; query?: { cursor?: string; limit?: number; unitId?: string; status?: "OPEN" | "IN_PROGRESS" | "RESOLVED"; q?: string }; response: RequestPage }
  createRequest: { params: { communityId: string }; body: RequestCreate; response: Request }
  getRequest: { params: { communityId: string; requestId: string }; response: Request }
  listRequestComments: { params: { communityId: string; requestId: string }; query?: { cursor?: string; limit?: number }; response: CommentPage }
  addRequestComment: { params: { communityId: string; requestId: string }; body: CommentCreate; response: Comment }
  assignRequest: { params: { communityId: string; requestId: string }; body: RequestAssignment; response: Request }
  changeRequestStatus: { params: { communityId: string; requestId: string }; body: RequestStatusChange; response: Request }
  listRequestEvents: { params: { communityId: string; requestId: string }; query?: { cursor?: string; limit?: number }; response: RequestEventPage }
  listAnnouncements: { params: { communityId: string }; query?: { cursor?: string; limit?: number; status?: "DRAFT" | "PUBLISHED"; q?: string }; response: AnnouncementPage }
  createAnnouncement: { params: { communityId: string }; body: AnnouncementWrite; response: Announcement }
  getAnnouncement: { params: { communityId: string; announcementId: string }; response: Announcement }
  updateAnnouncement: { params: { communityId: string; announcementId: string }; body: AnnouncementUpdate; response: Announcement }
  listNotifications: { params: { communityId: string }; query?: { cursor?: string; limit?: number; unread?: boolean }; response: NotificationPage }
  markNotificationRead: { params: { communityId: string; notificationId: string }; response: undefined }
  markAllNotificationsRead: { params: { communityId: string }; body: ReadAll; response: undefined }
  uploadAttachment: { params: { communityId: string }; body: FormData; response: Attachment }
  getAttachment: { params: { communityId: string; attachmentId: string }; response: Attachment }
  downloadAttachment: { params: { communityId: string; attachmentId: string }; response: Blob }
}
export const operationMetadata = {
  "getCsrf": {
    "method": "GET",
    "path": "/auth/csrf",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "login": {
    "method": "POST",
    "path": "/auth/login",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": true
  },
  "getSession": {
    "method": "GET",
    "path": "/auth/session",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "logout": {
    "method": "POST",
    "path": "/auth/logout",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": true
  },
  "requestPasswordReset": {
    "method": "POST",
    "path": "/auth/password-reset-requests",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "resetPassword": {
    "method": "POST",
    "path": "/auth/password-resets",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "getProfile": {
    "method": "GET",
    "path": "/me",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "updateProfile": {
    "method": "PATCH",
    "path": "/me",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": true
  },
  "getPreferences": {
    "method": "GET",
    "path": "/me/preferences",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "updatePreferences": {
    "method": "PATCH",
    "path": "/me/preferences",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": true
  },
  "listCommunities": {
    "method": "GET",
    "path": "/communities",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "getContext": {
    "method": "GET",
    "path": "/communities/{communityId}/context",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "getDashboard": {
    "method": "GET",
    "path": "/communities/{communityId}/dashboard",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "listUnits": {
    "method": "GET",
    "path": "/communities/{communityId}/units",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "getUnit": {
    "method": "GET",
    "path": "/communities/{communityId}/units/{unitId}",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "listRelationships": {
    "method": "GET",
    "path": "/communities/{communityId}/units/{unitId}/relationships",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "listDirectory": {
    "method": "GET",
    "path": "/communities/{communityId}/directory",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "getDirectoryUnit": {
    "method": "GET",
    "path": "/communities/{communityId}/directory/units/{unitId}",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "listFinancialAccounts": {
    "method": "GET",
    "path": "/communities/{communityId}/financial-accounts",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "listUnitAccounts": {
    "method": "GET",
    "path": "/communities/{communityId}/unit-accounts",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "getUnitAccount": {
    "method": "GET",
    "path": "/communities/{communityId}/unit-accounts/{unitId}",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "listAccountEntries": {
    "method": "GET",
    "path": "/communities/{communityId}/unit-accounts/{unitId}/entries",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "listCharges": {
    "method": "GET",
    "path": "/communities/{communityId}/charges",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "createChargeBatch": {
    "method": "POST",
    "path": "/communities/{communityId}/charge-batches",
    "multipart": false,
    "binary": false,
    "financial": true,
    "csrf": true
  },
  "getChargeBatch": {
    "method": "GET",
    "path": "/communities/{communityId}/charge-batches/{batchId}",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "cancelCharge": {
    "method": "POST",
    "path": "/communities/{communityId}/charges/{chargeId}/cancellations",
    "multipart": false,
    "binary": false,
    "financial": true,
    "csrf": true
  },
  "listPayments": {
    "method": "GET",
    "path": "/communities/{communityId}/payments",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "recordPayment": {
    "method": "POST",
    "path": "/communities/{communityId}/payments",
    "multipart": false,
    "binary": false,
    "financial": true,
    "csrf": true
  },
  "reversePayment": {
    "method": "POST",
    "path": "/communities/{communityId}/payments/{paymentId}/reversals",
    "multipart": false,
    "binary": false,
    "financial": true,
    "csrf": true
  },
  "listPaymentNotifications": {
    "method": "GET",
    "path": "/communities/{communityId}/payment-notifications",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "createPaymentNotification": {
    "method": "POST",
    "path": "/communities/{communityId}/payment-notifications",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": true
  },
  "reviewPaymentNotification": {
    "method": "POST",
    "path": "/communities/{communityId}/payment-notifications/{notificationId}/reviews",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": true
  },
  "withdrawPaymentNotification": {
    "method": "POST",
    "path": "/communities/{communityId}/payment-notifications/{notificationId}/withdrawals",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": true
  },
  "linkPaymentNotification": {
    "method": "POST",
    "path": "/communities/{communityId}/payment-notifications/{notificationId}/payment-links",
    "multipart": false,
    "binary": false,
    "financial": true,
    "csrf": true
  },
  "listExpensePayments": {
    "method": "GET",
    "path": "/communities/{communityId}/expense-payments",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "listExpenses": {
    "method": "GET",
    "path": "/communities/{communityId}/expenses",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "createExpense": {
    "method": "POST",
    "path": "/communities/{communityId}/expenses",
    "multipart": false,
    "binary": false,
    "financial": true,
    "csrf": true
  },
  "getExpense": {
    "method": "GET",
    "path": "/communities/{communityId}/expenses/{expenseId}",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "payExpense": {
    "method": "POST",
    "path": "/communities/{communityId}/expenses/{expenseId}/payments",
    "multipart": false,
    "binary": false,
    "financial": true,
    "csrf": true
  },
  "reverseExpensePayment": {
    "method": "POST",
    "path": "/communities/{communityId}/expense-payments/{expensePaymentId}/reversals",
    "multipart": false,
    "binary": false,
    "financial": true,
    "csrf": true
  },
  "listAssignableStaff": {
    "method": "GET",
    "path": "/communities/{communityId}/staff",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "listRequests": {
    "method": "GET",
    "path": "/communities/{communityId}/requests",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "createRequest": {
    "method": "POST",
    "path": "/communities/{communityId}/requests",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": true
  },
  "getRequest": {
    "method": "GET",
    "path": "/communities/{communityId}/requests/{requestId}",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "listRequestComments": {
    "method": "GET",
    "path": "/communities/{communityId}/requests/{requestId}/comments",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "addRequestComment": {
    "method": "POST",
    "path": "/communities/{communityId}/requests/{requestId}/comments",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": true
  },
  "assignRequest": {
    "method": "POST",
    "path": "/communities/{communityId}/requests/{requestId}/assignments",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": true
  },
  "changeRequestStatus": {
    "method": "POST",
    "path": "/communities/{communityId}/requests/{requestId}/status-changes",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": true
  },
  "listRequestEvents": {
    "method": "GET",
    "path": "/communities/{communityId}/requests/{requestId}/events",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "listAnnouncements": {
    "method": "GET",
    "path": "/communities/{communityId}/announcements",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "createAnnouncement": {
    "method": "POST",
    "path": "/communities/{communityId}/announcements",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": true
  },
  "getAnnouncement": {
    "method": "GET",
    "path": "/communities/{communityId}/announcements/{announcementId}",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "updateAnnouncement": {
    "method": "PUT",
    "path": "/communities/{communityId}/announcements/{announcementId}",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": true
  },
  "listNotifications": {
    "method": "GET",
    "path": "/communities/{communityId}/notifications",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "markNotificationRead": {
    "method": "PUT",
    "path": "/communities/{communityId}/notifications/{notificationId}/read",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": true
  },
  "markAllNotificationsRead": {
    "method": "POST",
    "path": "/communities/{communityId}/notifications/read-all",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": true
  },
  "uploadAttachment": {
    "method": "POST",
    "path": "/communities/{communityId}/attachments",
    "multipart": true,
    "binary": false,
    "financial": false,
    "csrf": true
  },
  "getAttachment": {
    "method": "GET",
    "path": "/communities/{communityId}/attachments/{attachmentId}",
    "multipart": false,
    "binary": false,
    "financial": false,
    "csrf": false
  },
  "downloadAttachment": {
    "method": "GET",
    "path": "/communities/{communityId}/attachments/{attachmentId}/content",
    "multipart": false,
    "binary": true,
    "financial": false,
    "csrf": false
  }
} as const;
