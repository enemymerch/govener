import type { AttachmentSource } from "../../shared/useAttachment";
export type RequestStatus = "Open" | "In progress" | "Resolved";
export type RequestComment = {
  id: string;
  authorId: string;
  authorName: string;
  body: string;
  createdAt: string;
  custom: boolean;
};
export type RequestEvent = {
  id: string;
  kind: "CREATED" | "STATUS_CHANGED" | "ASSIGNEE_CHANGED";
  actorName: string;
  createdAt: string;
  from?: string;
  to?: string;
};
export type Request = {
  id: string;
  displayNumber?: number;
  version?: number;
  unitId?: string;
  title: string;
  location: string;
  description: string;
  status: RequestStatus;
  date: string;
  custom?: boolean;
  createdById: string;
  createdByName: string;
  assigneeId: string | null;
  assigneeName?: string;
  photos: AttachmentSource[];
  comments: RequestComment[];
  history: RequestEvent[];
};
export type RequestDraft = Pick<
  Request,
  "title" | "location" | "description"
> & { photos: File[] };
export const requestStatuses: RequestStatus[] = [
  "Open",
  "In progress",
  "Resolved",
];
export const maxRequestPhotos = 3;
export const maxPhotoBytes = 5 * 1024 * 1024;
export function validRequestPhotos(files: File[]) {
  const types: Record<string, RegExp> = {
    "image/png": /\.png$/i,
    "image/jpeg": /\.jpe?g$/i,
    "image/webp": /\.webp$/i,
  };
  return (
    files.length <= maxRequestPhotos &&
    files.every(
      (file) =>
        file.size > 0 &&
        file.size <= maxPhotoBytes &&
        types[file.type]?.test(file.name),
    )
  );
}
