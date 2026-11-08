import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "wouter";
import { Bell, Check } from "lucide-react";
import { useWorkspace } from "../../workspace/Workspace";
import { useFormat } from "../../shared/useFormat";
import { api } from "../../api";
import { errorKey } from "../../api/server";
import type { Notification } from "../../api/generated";
export function notificationHref(item: Notification): string {
  const id = item.target.id ? encodeURIComponent(item.target.id) : "";
  switch (item.target.kind) {
    case "ACCOUNT":
      return id ? "/account/" + id : "/account";
    case "REQUEST":
      return id ? "/requests/" + id : "/requests";
    case "ANNOUNCEMENT":
      return id ? "/announcements/" + id : "/announcements";
    case "PAYMENT_NOTIFICATION":
      return "/payments";
    default:
      return "/overview";
  }
}
export default function Notifications() {
  const { t } = useTranslation();
  const { dateTime } = useFormat();
  const { state, communityId, mutate, busy, refreshError } = useWorkspace();
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [error, setError] = useState("");
  const items = state.notifications.filter(
    (item) => !unreadOnly || !item.readAt,
  );
  async function mark(id?: string) {
    setError("");
    try {
      await mutate(() =>
        id
          ? api.execute("markNotificationRead", {
              params: { communityId, notificationId: id },
            })
          : api.execute("markAllNotificationsRead", {
              params: { communityId },
              body: { before: new Date().toISOString() },
            }),
      );
    } catch (failure) {
      setError(errorKey(failure));
    }
  }
  return (
    <section className="panel">
      <div className="section-heading">
        <div>
          <h2>{t("Notifications")}</h2>
          <p>
            {t("notifications.unreadCount", {
              count: state.notifications.filter((item) => !item.readAt).length,
            })}
          </p>
        </div>
        <button
          className="secondary"
          disabled={
            busy ||
            !!refreshError ||
            !state.notifications.some((item) => !item.readAt)
          }
          onClick={() => void mark()}
        >
          <Check size={15} />
          {t("notifications.markAll")}
        </button>
      </div>
      {error && (
        <p className="form-error" role="alert">
          {t(error)}
        </p>
      )}
      <label className="checkbox-label notification-filter">
        <input
          type="checkbox"
          checked={unreadOnly}
          onChange={(event) => setUnreadOnly(event.target.checked)}
        />
        {t("notifications.unreadOnly")}
      </label>
      {items.length ? (
        <div className="notification-list">
          {items.map((item) => (
            <article key={item.id} className={item.readAt ? "read" : "unread"}>
              <Bell size={19} />
              <div>
                <Link href={notificationHref(item)}>
                  {t("live.notification." + item.type)}
                </Link>
                <time>{dateTime(item.createdAt)}</time>
              </div>
              {!item.readAt && (
                <button
                  className="text-button"
                  disabled={busy || !!refreshError}
                  onClick={() => void mark(item.id)}
                >
                  {t("notifications.markRead")}
                </button>
              )}
            </article>
          ))}
        </div>
      ) : (
        <p className="empty-state">{t("notifications.empty")}</p>
      )}
    </section>
  );
}
