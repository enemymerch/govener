import { useTranslation } from "react-i18next";
import { Link } from "wouter";
import { ArrowLeft, Megaphone, SquarePen } from "lucide-react";
import { useFormat } from "../../shared/useFormat";
import type { Announcement } from "./model";
import AudienceLabel from "./AudienceLabel";

export default function AnnouncementDetail({
  notice,
  isManager,
}: {
  notice: Announcement | undefined;
  isManager: boolean;
}) {
  const { t } = useTranslation();
  const { dateTime } = useFormat();
  if (!notice)
    return (
      <section className="panel route-state">
        <Megaphone size={28} />
        <h2>{t("announcements.notFound")}</h2>
        <p>{t("announcements.notFoundHelp")}</p>
        <Link className="primary" href="/announcements">
          {t("announcements.back")}
        </Link>
      </section>
    );
  return (
    <div className="announcement-detail" data-testid="announcement-detail">
      <Link href="/announcements" className="text-button unit-back">
        <ArrowLeft size={16} />
        {t("announcements.back")}
      </Link>
      <article className="panel announcement-article">
        <div className="announcement-article-heading">
          <span className="eyebrow">{t(notice.category)}</span>
          <span
            className={`status ${notice.status === "PUBLISHED" ? "resolved" : "open"}`}
          >
            {t(`announcements.status.${notice.status}`)}
          </span>
        </div>
        <h2>{notice.custom ? notice.title : t(notice.title)}</h2>
        <p className="announcement-body">
          {notice.custom ? notice.body : t(notice.body)}
        </p>
        <dl className="announcement-facts">
          <div>
            <dt>{t("announcements.author")}</dt>
            <dd>{notice.authorName}</dd>
          </div>
          <div>
            <dt>{t("announcements.updated")}</dt>
            <dd>{dateTime(notice.updatedAt)}</dd>
          </div>
          <div>
            <dt>{t("announcements.audience")}</dt>
            <dd>
              <AudienceLabel audience={notice.audience} />
            </dd>
          </div>
        </dl>
        {isManager && (
          <Link href={`/announcements/${notice.id}/edit`} className="secondary">
            <SquarePen size={16} />
            {t("announcements.edit")}
          </Link>
        )}
      </article>
    </div>
  );
}
