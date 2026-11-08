import { calendarDateInIstanbul } from "../shared/calendar";
import type {
  Request as WireRequest,
  Announcement as WireAnnouncement,
  PaymentNotification as WireNotification,
  Attachment,
} from "./generated";
import type { Request } from "../features/requests/model";
import type { Announcement } from "../features/announcements/model";
import type { PaymentNotification } from "../features/payments/model";
import type { RemoteAttachment } from "../shared/useAttachment";
const statuses = {
  OPEN: "Open",
  IN_PROGRESS: "In progress",
  RESOLVED: "Resolved",
} as const;
export const requestStatusToWire = {
  Open: "OPEN",
  "In progress": "IN_PROGRESS",
  Resolved: "RESOLVED",
} as const;
export function attachmentFromWire(file: Attachment): RemoteAttachment {
  return { id: file.id, name: file.fileName, mediaType: file.mediaType };
}
export function requestFromWire(request: WireRequest): Request {
  return {
    id: request.id,
    version: request.version,
    displayNumber: request.displayNumber,
    unitId: request.unitId ?? undefined,
    title: request.title,
    location: request.location,
    description: request.description,
    status: statuses[request.status],
    date: calendarDateInIstanbul(new Date(request.createdAt)),
    custom: true,
    createdById: request.creator.id,
    createdByName: request.creator.name,
    assigneeId: request.assignee?.id ?? null,
    assigneeName: request.assignee?.name,
    photos: request.photos.map(attachmentFromWire),
    comments: request.comments.map((item) => ({
      id: item.id,
      authorId: item.author.id,
      authorName: item.author.name,
      body: item.body,
      createdAt: item.createdAt,
      custom: true,
    })),
    history: request.history.map((item) => ({
      id: item.id,
      kind: item.kind,
      actorName: item.actor.name,
      createdAt: item.createdAt,
      from:
        item.kind === "STATUS_CHANGED"
          ? statuses[item.from as keyof typeof statuses]
          : item.from,
      to:
        item.kind === "STATUS_CHANGED"
          ? statuses[item.to as keyof typeof statuses]
          : item.to,
    })),
  };
}
export function announcementFromWire(notice: WireAnnouncement): Announcement {
  return {
    id: notice.id,
    version: notice.version,
    title: notice.title,
    body: notice.body,
    custom: true,
    category: notice.category,
    date: calendarDateInIstanbul(
      new Date(notice.publishedAt ?? notice.createdAt),
    ),
    tone: notice.category === "MAINTENANCE" ? "green" : "sand",
    status: notice.status,
    audience:
      notice.audience.type === "ALL"
        ? { type: "ALL" }
        : { type: notice.audience.type, ids: notice.audience.ids },
    authorName: notice.author.name,
    updatedAt: notice.updatedAt,
  };
}
export function paymentNotificationFromWire(
  item: WireNotification,
): PaymentNotification {
  return {
    id: item.id,
    version: item.version,
    unitId: item.unitId,
    amountMinor: item.amountMinor,
    paymentDate: item.paymentDate,
    reference: item.reference,
    note: item.note,
    receipt: item.receipt ? attachmentFromWire(item.receipt) : undefined,
    status: item.status,
    reviewNote: item.reviewNote,
  };
}
