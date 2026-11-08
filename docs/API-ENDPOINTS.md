# API Endpoint Inventory

Generated from [openapi.json](openapi.json). Edit the contract builder and run `npm run api:generate`; do not edit this table by hand.

Base URL: `/api/v1`. Access is enforced server-side. See [Backend Handoff](BACKEND-HANDOFF.md) for conventions and open decisions.

| Operation | Method and path | Request | Success response | Roles / scope | Permissions |
|---|---|---|---|---|---|
| `getCsrf` | `GET /auth/csrf` | — | 200 CsrfToken | session/public · PUBLIC_RATE_LIMITED | — |
| `login` | `POST /auth/login` | LoginRequest | 200 Session | session/public · PUBLIC_RATE_LIMITED | — |
| `getSession` | `GET /auth/session` | — | 200 Session | session/public · AUTHENTICATED_SESSION | — |
| `logout` | `POST /auth/logout` | — | 204 no body | session/public · AUTHENTICATED_SESSION | — |
| `requestPasswordReset` | `POST /auth/password-reset-requests` | ResetRequest | 202 no body | session/public · PUBLIC_RATE_LIMITED | — |
| `resetPassword` | `POST /auth/password-resets` | PasswordReset | 204 no body | session/public · PUBLIC_RATE_LIMITED | — |
| `getProfile` | `GET /me` | — | 200 Profile | MANAGER, RESIDENT · CURRENT_USER | — |
| `updateProfile` | `PATCH /me` | ProfileUpdate | 200 Profile | MANAGER, RESIDENT · CURRENT_USER | — |
| `getPreferences` | `GET /me/preferences` | — | 200 Preferences | MANAGER, RESIDENT · CURRENT_USER | — |
| `updatePreferences` | `PATCH /me/preferences` | PreferencesUpdate | 200 Preferences | MANAGER, RESIDENT · CURRENT_USER | — |
| `listCommunities` | `GET /communities` | — | 200 CommunityPage | MANAGER, RESIDENT · CURRENT_USER_MEMBERSHIPS | — |
| `getContext` | `GET /communities/{communityId}/context` | — | 200 CommunityContext | MANAGER, RESIDENT · COMMUNITY_MEMBERSHIP | COMMUNITY_READ |
| `getDashboard` | `GET /communities/{communityId}/dashboard` | — | 200 Dashboard | MANAGER, RESIDENT · COMMUNITY_MEMBERSHIP | COMMUNITY_READ, FINANCE_READ |
| `listUnits` | `GET /communities/{communityId}/units` | — | 200 UnitPage | MANAGER, RESIDENT · COMMUNITY_MEMBERSHIP | COMMUNITY_READ |
| `getUnit` | `GET /communities/{communityId}/units/{unitId}` | — | 200 Unit | MANAGER, RESIDENT · COMMUNITY_MEMBERSHIP | COMMUNITY_READ |
| `listRelationships` | `GET /communities/{communityId}/units/{unitId}/relationships` | — | 200 RelationshipPage | MANAGER · COMMUNITY_MEMBERSHIP | FINANCE_READ |
| `listDirectory` | `GET /communities/{communityId}/directory` | — | 200 DirectoryUnitPage | MANAGER, RESIDENT · CURRENT_RESIDENTS_IN_OWN_COMMUNITY_ONLY | DIRECTORY_READ |
| `getDirectoryUnit` | `GET /communities/{communityId}/directory/units/{unitId}` | — | 200 DirectoryUnit | MANAGER, RESIDENT · CURRENT_RESIDENTS_IN_OWN_COMMUNITY_ONLY | DIRECTORY_READ |
| `listFinancialAccounts` | `GET /communities/{communityId}/financial-accounts` | — | 200 FinancialAccountPage | MANAGER · COMMUNITY_MEMBERSHIP | FINANCE_READ |
| `listUnitAccounts` | `GET /communities/{communityId}/unit-accounts` | — | 200 UnitAccountPage | MANAGER, RESIDENT · MANAGER_COMMUNITY_OR_RESIDENT_AUTHORIZED_ACCOUNT_PROJECTION | FINANCE_READ |
| `getUnitAccount` | `GET /communities/{communityId}/unit-accounts/{unitId}` | — | 200 UnitAccount | MANAGER, RESIDENT · MANAGER_COMMUNITY_OR_RESIDENT_AUTHORIZED_ACCOUNT_PROJECTION | FINANCE_READ |
| `listAccountEntries` | `GET /communities/{communityId}/unit-accounts/{unitId}/entries` | — | 200 AccountEntryPage | MANAGER, RESIDENT · MANAGER_COMMUNITY_OR_RESIDENT_AUTHORIZED_ACCOUNT_PROJECTION | FINANCE_READ |
| `listCharges` | `GET /communities/{communityId}/charges` | — | 200 ChargePage | MANAGER, RESIDENT · MANAGER_COMMUNITY_OR_RESIDENT_AUTHORIZED_ACCOUNT_PROJECTION | FINANCE_READ |
| `createChargeBatch` | `POST /communities/{communityId}/charge-batches` | ChargeBatchRequest | 201 ChargeBatch | MANAGER · COMMUNITY_MEMBERSHIP | CHARGE_CREATE |
| `getChargeBatch` | `GET /communities/{communityId}/charge-batches/{batchId}` | — | 200 ChargeBatch | MANAGER · COMMUNITY_MEMBERSHIP | FINANCE_READ |
| `cancelCharge` | `POST /communities/{communityId}/charges/{chargeId}/cancellations` | ReasonCommand | 201 Charge | MANAGER · COMMUNITY_MEMBERSHIP | CHARGE_CREATE |
| `listPayments` | `GET /communities/{communityId}/payments` | — | 200 PaymentPage | MANAGER, RESIDENT · MANAGER_COMMUNITY_OR_RESIDENT_AUTHORIZED_ACCOUNT_PROJECTION | FINANCE_READ |
| `recordPayment` | `POST /communities/{communityId}/payments` | PaymentCreate | 201 Payment | MANAGER · COMMUNITY_MEMBERSHIP | PAYMENT_RECORD |
| `reversePayment` | `POST /communities/{communityId}/payments/{paymentId}/reversals` | ReasonCommand | 201 Payment | MANAGER · COMMUNITY_MEMBERSHIP | PAYMENT_RECORD |
| `listPaymentNotifications` | `GET /communities/{communityId}/payment-notifications` | — | 200 PaymentNotificationPage | MANAGER, RESIDENT · MANAGER_COMMUNITY_OR_RESIDENT_OWN_UNIT | — |
| `createPaymentNotification` | `POST /communities/{communityId}/payment-notifications` | PaymentNotificationCreate | 201 PaymentNotification | RESIDENT · MANAGER_COMMUNITY_OR_RESIDENT_OWN_UNIT | — |
| `reviewPaymentNotification` | `POST /communities/{communityId}/payment-notifications/{notificationId}/reviews` | NotificationReview | 201 PaymentNotification | MANAGER · MANAGER_COMMUNITY_OR_RESIDENT_OWN_UNIT | PAYMENT_RECORD |
| `withdrawPaymentNotification` | `POST /communities/{communityId}/payment-notifications/{notificationId}/withdrawals` | VersionCommand | 201 PaymentNotification | RESIDENT · MANAGER_COMMUNITY_OR_RESIDENT_OWN_UNIT | — |
| `linkPaymentNotification` | `POST /communities/{communityId}/payment-notifications/{notificationId}/payment-links` | PaymentLink | 201 PaymentNotification | MANAGER · MANAGER_COMMUNITY_OR_RESIDENT_OWN_UNIT | PAYMENT_RECORD |
| `listExpensePayments` | `GET /communities/{communityId}/expense-payments` | — | 200 ExpensePaymentPage | MANAGER · COMMUNITY_MEMBERSHIP | FINANCE_READ |
| `listExpenses` | `GET /communities/{communityId}/expenses` | — | 200 ExpensePage | MANAGER · COMMUNITY_MEMBERSHIP | FINANCE_READ |
| `createExpense` | `POST /communities/{communityId}/expenses` | ExpenseCreate | 201 Expense | MANAGER · COMMUNITY_MEMBERSHIP | EXPENSE_WRITE |
| `getExpense` | `GET /communities/{communityId}/expenses/{expenseId}` | — | 200 Expense | MANAGER · COMMUNITY_MEMBERSHIP | FINANCE_READ |
| `payExpense` | `POST /communities/{communityId}/expenses/{expenseId}/payments` | ExpensePaymentCreate | 201 ExpensePayment | MANAGER · COMMUNITY_MEMBERSHIP | EXPENSE_WRITE |
| `reverseExpensePayment` | `POST /communities/{communityId}/expense-payments/{expensePaymentId}/reversals` | ReasonCommand | 201 ExpensePayment | MANAGER · COMMUNITY_MEMBERSHIP | EXPENSE_WRITE |
| `listAssignableStaff` | `GET /communities/{communityId}/staff` | — | 200 DirectoryPersonPage | MANAGER · COMMUNITY_MEMBERSHIP | REQUEST_MANAGE |
| `listRequests` | `GET /communities/{communityId}/requests` | — | 200 RequestPage | MANAGER, RESIDENT · MANAGER_COMMUNITY_OR_RESIDENT_CREATED_REQUEST | — |
| `createRequest` | `POST /communities/{communityId}/requests` | RequestCreate | 201 Request | MANAGER, RESIDENT · MANAGER_COMMUNITY_OR_RESIDENT_CREATED_REQUEST | — |
| `getRequest` | `GET /communities/{communityId}/requests/{requestId}` | — | 200 Request | MANAGER, RESIDENT · MANAGER_COMMUNITY_OR_RESIDENT_CREATED_REQUEST | — |
| `listRequestComments` | `GET /communities/{communityId}/requests/{requestId}/comments` | — | 200 CommentPage | MANAGER, RESIDENT · MANAGER_COMMUNITY_OR_RESIDENT_CREATED_REQUEST | — |
| `addRequestComment` | `POST /communities/{communityId}/requests/{requestId}/comments` | CommentCreate | 201 Comment | MANAGER, RESIDENT · MANAGER_COMMUNITY_OR_RESIDENT_CREATED_REQUEST | — |
| `assignRequest` | `POST /communities/{communityId}/requests/{requestId}/assignments` | RequestAssignment | 201 Request | MANAGER · MANAGER_COMMUNITY_OR_RESIDENT_CREATED_REQUEST | REQUEST_MANAGE |
| `changeRequestStatus` | `POST /communities/{communityId}/requests/{requestId}/status-changes` | RequestStatusChange | 201 Request | MANAGER · MANAGER_COMMUNITY_OR_RESIDENT_CREATED_REQUEST | REQUEST_MANAGE |
| `listRequestEvents` | `GET /communities/{communityId}/requests/{requestId}/events` | — | 200 RequestEventPage | MANAGER, RESIDENT · MANAGER_COMMUNITY_OR_RESIDENT_CREATED_REQUEST | — |
| `listAnnouncements` | `GET /communities/{communityId}/announcements` | — | 200 AnnouncementPage | MANAGER, RESIDENT · MANAGER_COMMUNITY_OR_PUBLISHED_MATCHING_AUDIENCE | — |
| `createAnnouncement` | `POST /communities/{communityId}/announcements` | AnnouncementWrite | 201 Announcement | MANAGER · COMMUNITY_MEMBERSHIP | ANNOUNCEMENT_WRITE |
| `getAnnouncement` | `GET /communities/{communityId}/announcements/{announcementId}` | — | 200 Announcement | MANAGER, RESIDENT · MANAGER_COMMUNITY_OR_PUBLISHED_MATCHING_AUDIENCE | — |
| `updateAnnouncement` | `PUT /communities/{communityId}/announcements/{announcementId}` | AnnouncementUpdate | 200 Announcement | MANAGER · COMMUNITY_MEMBERSHIP | ANNOUNCEMENT_WRITE |
| `listNotifications` | `GET /communities/{communityId}/notifications` | — | 200 NotificationPage | MANAGER, RESIDENT · CURRENT_USER_IN_COMMUNITY | — |
| `markNotificationRead` | `PUT /communities/{communityId}/notifications/{notificationId}/read` | — | 204 no body | MANAGER, RESIDENT · CURRENT_USER_IN_COMMUNITY | — |
| `markAllNotificationsRead` | `POST /communities/{communityId}/notifications/read-all` | ReadAll | 204 no body | MANAGER, RESIDENT · CURRENT_USER_IN_COMMUNITY | — |
| `uploadAttachment` | `POST /communities/{communityId}/attachments` | Upload | 201 Attachment | MANAGER, RESIDENT · OWNER_OR_AUTHORIZED_PARENT_RESOURCE_AND_PURPOSE | — |
| `getAttachment` | `GET /communities/{communityId}/attachments/{attachmentId}` | — | 200 Attachment | MANAGER, RESIDENT · OWNER_OR_AUTHORIZED_PARENT_RESOURCE_AND_PURPOSE | — |
| `downloadAttachment` | `GET /communities/{communityId}/attachments/{attachmentId}/content` | — | 200 binary | MANAGER, RESIDENT · OWNER_OR_AUTHORIZED_PARENT_RESOURCE_AND_PURPOSE | — |
