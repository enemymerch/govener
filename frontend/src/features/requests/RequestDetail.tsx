import { useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "wouter";
import {
  ArrowLeft,
  Clock3,
  MessageSquare,
  Send,
  UserRound,
} from "lucide-react";
import { useFormat } from "../../shared/useFormat";
import { requestStatuses, type Request } from "./model";
import RequestPhoto from "./RequestPhoto";
import { useWorkspace } from "../../workspace/Workspace";
import { errorKey } from "../../api/server";

type Props = {
  request: Request | undefined;
  isManager: boolean;
  staff: { id: string; name: string }[];
  onStatusChange: (id: string, status: Request["status"]) => Promise<void>;
  onAssign: (id: string, assigneeId: string | null) => Promise<void>;
  onComment: (id: string, body: string) => Promise<void>;
};
export default function RequestDetail({
  request,
  isManager,
  staff: requestAssignees,
  onStatusChange,
  onAssign,
  onComment,
}: Props) {
  const { t } = useTranslation();
  const { date, dateTime } = useFormat();
  const { busy, refreshError } = useWorkspace();
  const [error, setError] = useState("");
  const [comment, setComment] = useState("");
  const [sent, setSent] = useState(false);
  if (!request)
    return (
      <section className="panel route-state">
        <MessageSquare size={28} />
        <h2>{t("requests.notFound")}</h2>
        <p>{t("requests.notFoundHelp")}</p>
        <Link href="/requests" className="primary">
          {t("requests.back")}
        </Link>
      </section>
    );
  const display = (text: string) => (request.custom ? text : t(text));
  async function submitComment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!request || !comment.trim()) return;
    try {
      await onComment(request.id, comment.trim());
      setComment("");
      setSent(true);
      setError("");
    } catch (failure) {
      setError(errorKey(failure));
    }
  }
  return (
    <div className="request-detail">
      <Link href="/requests" className="text-button unit-back">
        <ArrowLeft size={16} />
        {t("requests.back")}
      </Link>
      {error && (
        <p className="form-error" role="alert">
          {t(error)}
        </p>
      )}
      <section className="panel request-detail-header">
        <div>
          <span className="eyebrow">
            REQ-{request.displayNumber ?? request.id}
          </span>
          <h2>{display(request.title)}</h2>
          <p>
            {display(request.location)} · {date(request.date)}
          </p>
        </div>
        <span
          className={`status ${request.status.toLowerCase().replace(" ", "-")}`}
        >
          {t(request.status)}
        </span>
      </section>
      <div className="request-detail-grid">
        <div className="request-main-column">
          <section className="panel request-description">
            <h3>{t("requests.description")}</h3>
            <p>
              {request.description
                ? display(request.description)
                : t("requests.noDescription")}
            </p>
            {request.photos.length > 0 && (
              <>
                <h3>{t("requests.photos")}</h3>
                <div className="request-photo-grid">
                  {request.photos.map((file, index) => (
                    <RequestPhoto key={`${file.name}-${index}`} file={file} />
                  ))}
                </div>
              </>
            )}
          </section>
          <section className="panel request-discussion">
            <div className="section-heading">
              <div>
                <h2>{t("requests.conversation")}</h2>
                <p>{t("requests.conversationHelp")}</p>
              </div>
              <MessageSquare size={19} />
            </div>
            <div className="request-comments">
              {request.comments.length === 0 ? (
                <p className="empty-state">{t("requests.noComments")}</p>
              ) : (
                request.comments.map((item) => (
                  <article className="request-comment" key={item.id}>
                    <div>
                      <strong>{item.authorName}</strong>
                      <time dateTime={item.createdAt}>
                        {dateTime(item.createdAt)}
                      </time>
                    </div>
                    <p>{item.custom ? item.body : t(item.body)}</p>
                  </article>
                ))
              )}
            </div>
            <form className="comment-form" onSubmit={submitComment}>
              <label>
                {t("requests.commentLabel")}
                <textarea
                  disabled={busy}
                  rows={3}
                  maxLength={2000}
                  required
                  value={comment}
                  onChange={(event) => {
                    setComment(event.target.value);
                    setSent(false);
                  }}
                />
              </label>
              <div>
                {sent && (
                  <span className="inline-success" role="status">
                    {t("requests.commentSent")}
                  </span>
                )}
                <button
                  className="primary"
                  disabled={!comment.trim() || busy || !!refreshError}
                >
                  <Send size={15} />
                  {t("requests.sendComment")}
                </button>
              </div>
            </form>
          </section>
        </div>
        <aside className="request-side-column">
          <section className="panel request-management">
            <h3>{t("requests.details")}</h3>
            <div className="request-person">
              <UserRound size={17} />
              <span>
                <small>{t("requests.createdBy")}</small>
                <strong>{request.createdByName}</strong>
              </span>
            </div>
            {isManager ? (
              <>
                <label>
                  {t("requests.assignee")}
                  <select
                    disabled={busy || !!refreshError}
                    value={request.assigneeId ?? ""}
                    onChange={(event) => {
                      void onAssign(
                        request.id,
                        event.target.value || null,
                      ).catch((failure) => setError(errorKey(failure)));
                    }}
                  >
                    <option value="">{t("requests.unassigned")}</option>
                    {requestAssignees.map((person) => (
                      <option key={person.id} value={person.id}>
                        {person.name}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  {t("requests.changeStatus")}
                  <select
                    disabled={busy || !!refreshError}
                    value={request.status}
                    onChange={(event) => {
                      void onStatusChange(
                        request.id,
                        event.target.value as Request["status"],
                      ).catch((failure) => setError(errorKey(failure)));
                    }}
                  >
                    {requestStatuses.map((status) => (
                      <option key={status} value={status}>
                        {t(status)}
                      </option>
                    ))}
                  </select>
                </label>
              </>
            ) : (
              <div className="request-person">
                <UserRound size={17} />
                <span>
                  <small>{t("requests.assignee")}</small>
                  <strong>
                    {requestAssignees.find(
                      (person) => person.id === request.assigneeId,
                    )?.name ??
                      request.assigneeName ??
                      t("requests.unassigned")}
                  </strong>
                </span>
              </div>
            )}
          </section>
          <section className="panel request-history">
            <h3>
              <Clock3 size={17} />
              {t("requests.history")}
            </h3>
            <ol>
              {request.history.map((item) => (
                <li key={item.id}>
                  <strong>
                    {item.kind === "CREATED"
                      ? t("requests.eventCreated")
                      : item.kind === "STATUS_CHANGED"
                        ? t("requests.eventStatus", {
                            from: t(item.from ?? "Open"),
                            to: t(item.to ?? "Open"),
                          })
                        : t("requests.eventAssignment", {
                            name:
                              requestAssignees.find(
                                (person) => person.id === item.to,
                              )?.name ??
                              (item.to === request.assigneeId
                                ? request.assigneeName
                                : undefined) ??
                              (item.to
                                ? t("live.teamMember")
                                : t("requests.unassigned")),
                          })}
                  </strong>
                  <span>{item.actorName}</span>
                  <time dateTime={item.createdAt}>
                    {dateTime(item.createdAt)}
                  </time>
                </li>
              ))}
            </ol>
          </section>
        </aside>
      </div>
    </div>
  );
}
