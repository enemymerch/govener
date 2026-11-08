import { api } from "./index";
import {
  announcementFromWire,
  paymentNotificationFromWire,
  requestFromWire,
  requestStatusToWire,
} from "./adapters";
import type { AnnouncementDraft } from "../features/announcements/model";
import type { RequestDraft, RequestStatus } from "../features/requests/model";
import type {
  PaymentNotificationCreate,
  AnnouncementWrite,
  ProfileUpdate,
  PreferencesUpdate,
  PaymentCreate,
  ChargeBatchRequest,
  ExpenseCreate,
} from "./generated";

type Client = typeof api;
// HTTP repositories for the live workspace.
export function createRepositories(client: Client = api) {
  const params = (communityId: string) => ({ communityId });
  const uploads = new WeakMap<
    File,
    Map<string, Promise<import("./generated").Attachment>>
  >();
  async function upload(
    communityId: string,
    purpose: "REQUEST_PHOTO" | "PAYMENT_RECEIPT" | "EXPENSE_DOCUMENT",
    file: File,
  ) {
    let cached = uploads.get(file);
    if (!cached) {
      cached = new Map();
      uploads.set(file, cached);
    }
    const scope = communityId + ":" + purpose;
    const previous = cached.get(scope);
    if (previous) return previous;
    const body = new FormData();
    body.set("purpose", purpose);
    body.set("file", file);
    const pending = client.execute("uploadAttachment", {
      params: params(communityId),
      body,
    });
    cached.set(scope, pending);
    try {
      return await pending;
    } catch (failure) {
      cached.delete(scope);
      throw failure;
    }
  }
  return {
    auth: {
      csrf: () => client.execute("getCsrf", {}),
      session: () => client.execute("getSession", {}),
      login: (email: string, password: string) =>
        client.execute("login", { body: { email, password } }),
      logout: async () => {
        await client.execute("logout", {});
        client.clearSession();
      },
      requestReset: (email: string) =>
        client.execute("requestPasswordReset", { body: { email } }),
      reset: (token: string, password: string) =>
        client.execute("resetPassword", { body: { token, password } }),
    },
    profile: {
      get: () => client.execute("getProfile", {}),
      update: (body: ProfileUpdate) =>
        client.execute("updateProfile", { body }),
      preferences: () => client.execute("getPreferences", {}),
      updatePreferences: (body: PreferencesUpdate) =>
        client.execute("updatePreferences", { body }),
    },
    communities: {
      list: (cursor?: string) =>
        client.execute("listCommunities", { query: { cursor } }),
      context: (communityId: string, signal?: AbortSignal) =>
        client.execute("getContext", { params: params(communityId), signal }),
      dashboard: (communityId: string, unitId?: string) =>
        client.execute("getDashboard", {
          params: params(communityId),
          query: { unitId },
        }),
    },
    directory: {
      list: (
        communityId: string,
        query?: import("./generated").Operations["listDirectory"]["query"],
      ) =>
        client.execute("listDirectory", { params: params(communityId), query }),
      getUnit: (communityId: string, unitId: string) =>
        client.execute("getDirectoryUnit", { params: { communityId, unitId } }),
      units: (communityId: string, cursor?: string) =>
        client.execute("listUnits", {
          params: params(communityId),
          query: { cursor },
        }),
      unit: (communityId: string, unitId: string) =>
        client.execute("getUnit", { params: { communityId, unitId } }),
      relationships: (communityId: string, unitId: string, cursor?: string) =>
        client.execute("listRelationships", {
          params: { communityId, unitId },
          query: { cursor },
        }),
    },
    notifications: {
      list: (communityId: string, cursor?: string, unread?: boolean) =>
        client.execute("listNotifications", {
          params: params(communityId),
          query: { cursor, unread },
        }),
      markRead: (communityId: string, notificationId: string) =>
        client.execute("markNotificationRead", {
          params: { communityId, notificationId },
        }),
      markAllRead: (communityId: string, before: string) =>
        client.execute("markAllNotificationsRead", {
          params: params(communityId),
          body: { before },
        }),
    },
    requests: {
      list: (communityId: string, cursor?: string) =>
        client.execute("listRequests", {
          params: params(communityId),
          query: { cursor },
        }),
      get: async (communityId: string, requestId: string) =>
        requestFromWire(
          await client.execute("getRequest", {
            params: { communityId, requestId },
          }),
        ),
      create: async (
        communityId: string,
        unitId: string | undefined,
        draft: RequestDraft,
      ) => {
        const photos = await Promise.all(
          draft.photos.map((file) =>
            upload(communityId, "REQUEST_PHOTO", file),
          ),
        );
        return requestFromWire(
          await client.execute("createRequest", {
            params: params(communityId),
            body: {
              unitId,
              title: draft.title,
              location: draft.location,
              description: draft.description,
              photoIds: photos.map((photo) => photo.id),
            },
          }),
        );
      },
      comment: (communityId: string, requestId: string, body: string) =>
        client.execute("addRequestComment", {
          params: { communityId, requestId },
          body: { body },
        }),
      assign: async (
        communityId: string,
        requestId: string,
        assigneeId: string | null,
        expectedVersion: number,
      ) =>
        requestFromWire(
          await client.execute("assignRequest", {
            params: { communityId, requestId },
            body: { assigneeId, expectedVersion },
          }),
        ),
      changeStatus: async (
        communityId: string,
        requestId: string,
        status: RequestStatus,
        expectedVersion: number,
      ) =>
        requestFromWire(
          await client.execute("changeRequestStatus", {
            params: { communityId, requestId },
            body: { status: requestStatusToWire[status], expectedVersion },
          }),
        ),
    },
    announcements: {
      list: (communityId: string, cursor?: string) =>
        client.execute("listAnnouncements", {
          params: params(communityId),
          query: { cursor },
        }),
      get: async (communityId: string, announcementId: string) =>
        announcementFromWire(
          await client.execute("getAnnouncement", {
            params: { communityId, announcementId },
          }),
        ),
      save: async (
        communityId: string,
        draft: AnnouncementDraft,
        existing?: { id: string; version: number },
      ) => {
        const body: AnnouncementWrite = {
          ...draft,
          audience:
            draft.audience.type === "ALL"
              ? { type: "ALL", ids: [] }
              : draft.audience,
        };
        const result = existing
          ? await client.execute("updateAnnouncement", {
              params: { communityId, announcementId: existing.id },
              body: { ...body, expectedVersion: existing.version },
            })
          : await client.execute("createAnnouncement", {
              params: params(communityId),
              body,
            });
        return announcementFromWire(result);
      },
    },
    paymentNotifications: {
      list: (
        communityId: string,
        query?: import("./generated").Operations["listPaymentNotifications"]["query"],
      ) =>
        client.execute("listPaymentNotifications", {
          params: params(communityId),
          query,
        }),
      create: async (
        communityId: string,
        body: Omit<PaymentNotificationCreate, "receiptId">,
        receipt?: File,
      ) => {
        const attachment = receipt
          ? await upload(communityId, "PAYMENT_RECEIPT", receipt)
          : undefined;
        return paymentNotificationFromWire(
          await client.execute("createPaymentNotification", {
            params: params(communityId),
            body: { ...body, receiptId: attachment?.id },
          }),
        );
      },
      review: (
        communityId: string,
        notificationId: string,
        decision: "REVIEWED" | "REJECTED",
        note: string,
        expectedVersion: number,
      ) =>
        client.execute("reviewPaymentNotification", {
          params: { communityId, notificationId },
          body: { decision, note, expectedVersion },
        }),
      withdraw: (
        communityId: string,
        notificationId: string,
        expectedVersion: number,
      ) =>
        client.execute("withdrawPaymentNotification", {
          params: { communityId, notificationId },
          body: { expectedVersion },
        }),
      link: (
        communityId: string,
        notificationId: string,
        paymentId: string,
        expectedVersion: number,
        expectedPaymentVersion: number,
        idempotencyKey: string,
      ) =>
        client.execute("linkPaymentNotification", {
          params: { communityId, notificationId },
          body: { paymentId, expectedVersion, expectedPaymentVersion },
          idempotencyKey,
        }),
    },
    finance: {
      accounts: (communityId: string, cursor?: string) =>
        client.execute("listFinancialAccounts", {
          params: params(communityId),
          query: { cursor },
        }),
      charges: (
        communityId: string,
        query?: import("./generated").Operations["listCharges"]["query"],
      ) =>
        client.execute("listCharges", { params: params(communityId), query }),
      payments: (
        communityId: string,
        query?: import("./generated").Operations["listPayments"]["query"],
      ) =>
        client.execute("listPayments", { params: params(communityId), query }),
      expenses: (communityId: string, cursor?: string) =>
        client.execute("listExpenses", {
          params: params(communityId),
          query: { cursor },
        }),
      expensePayments: (
        communityId: string,
        expenseId?: string,
        cursor?: string,
      ) =>
        client.execute("listExpensePayments", {
          params: params(communityId),
          query: { expenseId, cursor },
        }),
      cancelCharge: (
        communityId: string,
        chargeId: string,
        reason: string,
        expectedVersion: number,
        idempotencyKey: string,
      ) =>
        client.execute("cancelCharge", {
          params: { communityId, chargeId },
          body: { reason, expectedVersion },
          idempotencyKey,
        }),
      payExpense: (
        communityId: string,
        expenseId: string,
        body: import("./generated").ExpensePaymentCreate,
        idempotencyKey: string,
      ) =>
        client.execute("payExpense", {
          params: { communityId, expenseId },
          body,
          idempotencyKey,
        }),
      reverseExpensePayment: (
        communityId: string,
        expensePaymentId: string,
        reason: string,
        expectedVersion: number,
        idempotencyKey: string,
      ) =>
        client.execute("reverseExpensePayment", {
          params: { communityId, expensePaymentId },
          body: { reason, expectedVersion },
          idempotencyKey,
        }),
      account: (communityId: string, unitId: string) =>
        client.execute("getUnitAccount", { params: { communityId, unitId } }),
      entries: (communityId: string, unitId: string, cursor?: string) =>
        client.execute("listAccountEntries", {
          params: { communityId, unitId },
          query: { cursor },
        }),
      createCharges: (
        communityId: string,
        body: ChargeBatchRequest,
        idempotencyKey: string,
      ) =>
        client.execute("createChargeBatch", {
          params: params(communityId),
          body,
          idempotencyKey,
        }),
      recordPayment: (
        communityId: string,
        body: PaymentCreate,
        idempotencyKey: string,
      ) =>
        client.execute("recordPayment", {
          params: params(communityId),
          body,
          idempotencyKey,
        }),
      reversePayment: (
        communityId: string,
        paymentId: string,
        reason: string,
        expectedVersion: number,
        idempotencyKey: string,
      ) =>
        client.execute("reversePayment", {
          params: { communityId, paymentId },
          body: { reason, expectedVersion },
          idempotencyKey,
        }),
      createExpense: async (
        communityId: string,
        body: Omit<ExpenseCreate, "documentId">,
        idempotencyKey: string,
        document?: File,
      ) => {
        const attachment = document
          ? await upload(communityId, "EXPENSE_DOCUMENT", document)
          : undefined;
        return client.execute("createExpense", {
          params: params(communityId),
          body: { ...body, documentId: attachment?.id },
          idempotencyKey,
        });
      },
    },
    upload,
    raw: client.execute,
  };
}
export const repositories = createRepositories();
