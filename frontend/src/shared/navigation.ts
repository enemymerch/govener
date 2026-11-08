export type Role = "Manager" | "Resident";
export type Page =
  | "Overview"
  | "Account"
  | "Requests"
  | "Announcements"
  | "Payments"
  | "Units"
  | "Finance"
  | "Profile"
  | "Notifications"
  | "NotFound";

export const pagePaths: Record<Exclude<Page, "NotFound">, string> = {
  Overview: "/overview",
  Account: "/account",
  Requests: "/requests",
  Announcements: "/announcements",
  Payments: "/payments",
  Units: "/units",
  Finance: "/finance",
  Profile: "/profile",
  Notifications: "/notifications",
};

export function resolvePage(path: string): Page {
  if (/^\/account\/[^/]+$/.test(path)) return "Account";
  if (/^\/units\/[^/]+$/.test(path)) return "Units";
  if (/^\/requests\/[^/]+$/.test(path)) return "Requests";
  if (/^\/announcements\/[^/]+(?:\/edit)?$/.test(path)) return "Announcements";
  return (
    (Object.entries(pagePaths).find(([, value]) => value === path)?.[0] as
      Page | undefined) ?? "NotFound"
  );
}
