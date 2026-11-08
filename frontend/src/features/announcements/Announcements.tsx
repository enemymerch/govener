import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "wouter";
import { ArrowRight, Megaphone, Plus, SquarePen } from "lucide-react";
import { useFormat } from "../../shared/useFormat";
import type { Announcement } from "./model";
import AudienceLabel from "./AudienceLabel";

type Props = {
  notices: Announcement[];
  isManager: boolean;
  compact?: boolean;
  onSelect: (id: string) => void;
};
export default function Announcements({
  notices,
  isManager,
  compact = false,
  onSelect,
}: Props) {
  const { t } = useTranslation();
  const { date } = useFormat();
  const [statusFilter, setStatusFilter] = useState("ALL");
  const visible = notices.filter((notice) =>
    compact
      ? notice.status === "PUBLISHED"
      : statusFilter === "ALL" || notice.status === statusFilter,
  );
  return (
    <section className="announcements">
      <div className="section-heading">
        <div>
          <h2>{t("On the community board")}</h2>
          <p>{t("A few things worth knowing.")}</p>
        </div>
        {isManager && !compact ? (
          <div className="announcement-toolbar">
            <label>
              <span className="sr-only">{t("announcements.statusFilter")}</span>
              <select
                value={statusFilter}
                onChange={(event) => setStatusFilter(event.target.value)}
              >
                <option value="ALL">{t("announcements.allStatuses")}</option>
                <option value="DRAFT">{t("announcements.status.DRAFT")}</option>
                <option value="PUBLISHED">
                  {t("announcements.status.PUBLISHED")}
                </option>
              </select>
            </label>
            <Link href="/announcements/new" className="primary">
              <Plus size={16} />
              {t("announcements.new")}
            </Link>
          </div>
        ) : (
          <Megaphone size={20} />
        )}
      </div>
      {visible.length === 0 ? (
        <section className="panel route-state">
          <Megaphone size={28} />
          <h2>{t("announcements.empty")}</h2>
          <p>{t("announcements.emptyHelp")}</p>
        </section>
      ) : (
        <div className="notice-grid">
          {visible.map((notice) => (
            <article className={`notice ${notice.tone}`} key={notice.id}>
              <button
                className="notice-open"
                onClick={() => onSelect(notice.id)}
              >
                <div>
                  <span className="eyebrow">{t(notice.category)}</span>
                  <time dateTime={notice.date}>
                    {date(notice.date, { day: "numeric", month: "short" })}
                  </time>
                </div>
                <h3>{notice.custom ? notice.title : t(notice.title)}</h3>
                <p>{notice.custom ? notice.body : t(notice.body)}</p>
                <span className="notice-link">
                  {t("Read update")}
                  <ArrowRight size={15} />
                </span>
              </button>
              <div className="notice-meta">
                <span
                  className={`status ${notice.status === "PUBLISHED" ? "resolved" : "open"}`}
                >
                  {t(`announcements.status.${notice.status}`)}
                </span>
                <AudienceLabel audience={notice.audience} />
                {isManager && !compact && (
                  <Link
                    className="text-button"
                    href={`/announcements/${notice.id}/edit`}
                  >
                    <SquarePen size={14} />
                    {t("announcements.edit")}
                  </Link>
                )}
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
