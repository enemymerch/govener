export type Audience =
  { type: "ALL" } | { type: "BLOCKS" | "UNITS"; ids: string[] };
export type Announcement = {
  id: string;
  version?: number;
  category: "MAINTENANCE" | "COMMUNITY";
  title: string;
  body: string;
  date: string;
  tone: "green" | "sand";
  custom?: boolean;
  status: "DRAFT" | "PUBLISHED";
  audience: Audience;
  authorName: string;
  updatedAt: string;
};
export type AnnouncementDraft = Pick<
  Announcement,
  "title" | "body" | "category" | "status" | "audience"
>;
export function validAudience(
  audience: Audience,
  units: { id: string; block: string }[],
) {
  if (audience.type === "ALL") return true;
  const allowed =
    audience.type === "BLOCKS"
      ? [...new Set(units.map((unit) => unit.block))]
      : units.map((unit) => unit.id);
  return (
    audience.ids.length > 0 && audience.ids.every((id) => allowed.includes(id))
  );
}
