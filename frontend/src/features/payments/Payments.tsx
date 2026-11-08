import {
  useAttachment,
  type AttachmentSource,
} from "../../shared/useAttachment";
import { Link } from "wouter";
import { useWorkspace } from "../../workspace/Workspace";
import { repositories } from "../../api/repositories";
import { errorKey } from "../../api/server";
import { useEffect, useId, useRef, useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import {
  ArrowRight,
  Check,
  FileDown,
  Plus,
  Search,
  Wallet,
  X,
} from "lucide-react";
import {
  maxReceiptBytes,
  parseAmountMinor,
  validReceipt,
  type PaymentNotification,
  type PaymentNotificationStatus,
} from "./model";

type Props = {
  isManager: boolean;
  notifications: PaymentNotification[];
};

export default function Payments({ isManager, notifications }: Props) {
  const { t, i18n } = useTranslation();
  const {
    community,
    communityId,
    unitId: residentUnitId,
    mutate,
    busy,
    refreshError,
  } = useWorkspace();
  const unitLabel = (id: string) =>
    community.units.find((unit) => unit.id === id)?.label ?? id;
  const linked = (id: string) =>
    notifications.find((item) => item.id === id)?.status === "RECORDED";
  const displayStatus = (item: PaymentNotification) =>
    linked(item.id) ? "RECORDED" : item.status;
  const locale = i18n.resolvedLanguage === "tr" ? "tr-TR" : "en-GB";
  const [showForm, setShowForm] = useState(false);
  const [filter, setFilter] = useState("ALL");
  const [search, setSearch] = useState("");
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [reviewId, setReviewId] = useState<string | null>(null);
  const [reviewNote, setReviewNote] = useState("");
  const [reviewError, setReviewError] = useState("");
  const titleInput = useRef<HTMLInputElement>(null);
  const newButton = useRef<HTMLButtonElement>(null);
  const idPrefix = useId();
  const today = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Europe/Istanbul",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(new Date());
  const formatMoney = (minor: number) =>
    new Intl.NumberFormat(locale, {
      style: "currency",
      currency: "TRY",
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(minor / 100);
  const formatDate = (date: string) =>
    new Intl.DateTimeFormat(locale, {
      day: "numeric",
      month: "short",
      year: "numeric",
      timeZone: "Europe/Istanbul",
    }).format(new Date(`${date}T12:00:00Z`));
  const statuses: PaymentNotificationStatus[] = [
    "PENDING",
    "REVIEWED",
    "REJECTED",
    "WITHDRAWN",
    "RECORDED",
  ] as const;
  const scoped = isManager
    ? notifications
    : notifications.filter((item) => item.unitId === residentUnitId);
  const visible = scoped.filter(
    (item) =>
      (filter === "ALL" || displayStatus(item) === filter) &&
      `${item.id} ${item.unitId} ${item.reference} ${item.note}`
        .toLocaleLowerCase(locale)
        .includes(search.toLocaleLowerCase(locale)),
  );
  const pending = scoped.filter(
    (item) => item.status === "PENDING" && !linked(item.id),
  );
  const reviewing = scoped.find(
    (item) => item.id === reviewId && item.status === "PENDING",
  );
  useEffect(() => {
    if (showForm) titleInput.current?.focus();
  }, [showForm]);
  useEffect(() => {
    setShowForm(false);
    setReviewId(null);
    setMessage("");
    setError("");
    setSearch("");
    setFilter("ALL");
  }, [isManager]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (isManager) return;
    const data = new FormData(event.currentTarget);
    const amountMinor = parseAmountMinor(String(data.get("amount") ?? ""));
    const paymentDate = String(data.get("date") ?? "");
    const reference = String(data.get("reference") ?? "").trim();
    const note = String(data.get("note") ?? "").trim();
    const receiptEntry = data.get("receipt");
    const receipt =
      receiptEntry instanceof File && receiptEntry.name
        ? receiptEntry
        : undefined;
    if (amountMinor === null) {
      setError("payments.errorAmount");
      return;
    }
    if (
      !/^\d{4}-\d{2}-\d{2}$/.test(paymentDate) ||
      !Number.isFinite(Date.parse(paymentDate)) ||
      paymentDate > today
    ) {
      setError("payments.errorDate");
      return;
    }
    if (!reference || reference.length > 100 || note.length > 500) {
      setError("payments.errorReference");
      return;
    }
    if (receipt && !validReceipt(receipt)) {
      setError("payments.errorReceipt");
      return;
    }
    try {
      await mutate(() =>
        repositories.paymentNotifications.create(
          communityId,
          {
            unitId: residentUnitId,
            amountMinor,
            currency: "TRY",
            paymentDate,
            reference,
            note,
          },
          receipt,
        ),
      );
      setShowForm(false);
      setError("");
      setFilter("ALL");
      setSearch("");
      setMessage("payments.submitted");
    } catch (failure) {
      setError(errorKey(failure));
      return;
    }
    newButton.current?.focus();
  }

  async function transition(
    id: string,
    status: PaymentNotificationStatus,
    reason = "",
  ) {
    const current = notifications.find((item) => item.id === id);
    if (!current || current.status !== "PENDING" || linked(current.id)) return;
    if (
      isManager
        ? status !== "REVIEWED" && status !== "REJECTED"
        : status !== "WITHDRAWN" || current.unitId !== residentUnitId
    )
      return;
    if (status === "REJECTED" && !reason.trim()) {
      setReviewError("payments.reasonRequired");
      return;
    }
    try {
      await mutate(() =>
        isManager
          ? repositories.paymentNotifications.review(
              communityId,
              id,
              status as "REVIEWED" | "REJECTED",
              reason.trim(),
              current.version!,
            )
          : repositories.paymentNotifications.withdraw(
              communityId,
              id,
              current.version!,
            ),
      );
      setReviewId(null);
      setReviewNote("");
      setReviewError("");
      setMessage(
        status === "WITHDRAWN" ? "payments.withdrawn" : "payments.updated",
      );
    } catch (failure) {
      setReviewError(errorKey(failure));
      setError(errorKey(failure));
    }
  }

  return (
    <div className="payments-feature">
      {error && !showForm && (
        <p className="form-error feedback" role="alert">
          {t(error)}
        </p>
      )}
      <div className="payment-info">
        <Wallet size={21} aria-hidden="true" />
        <div>
          <strong>{t("payments.noticeTitle")}</strong>
          <p>{t("payments.noticeBody")}</p>
        </div>
      </div>
      {message && (
        <div className="feedback" role="status">
          <Check size={17} />
          {t(message)}
          <button
            aria-label={t("Dismiss confirmation")}
            onClick={() => setMessage("")}
          >
            <X size={16} />
          </button>
        </div>
      )}
      <div className="payment-summary">
        <div>
          <span>{t("payments.pendingCount")}</span>
          <strong>{pending.length}</strong>
        </div>
        <div>
          <span>{t("payments.pendingAmount")}</span>
          <strong>
            {formatMoney(
              pending.reduce((total, item) => total + item.amountMinor, 0),
            )}
          </strong>
        </div>
        <div>
          <span>{t("payments.scope")}</span>
          <strong>
            {isManager ? t("payments.allUnits") : unitLabel(residentUnitId)}
          </strong>
        </div>
      </div>
      {!isManager && (
        <div className="payment-create-bar">
          <p>{t("payments.transferFirst")}</p>
          <button
            ref={newButton}
            className="primary"
            aria-expanded={showForm}
            onClick={() => {
              setShowForm(!showForm);
              setError("");
              setMessage("");
            }}
          >
            <Plus size={17} />
            {t("payments.new")}
          </button>
        </div>
      )}
      {showForm && !isManager && (
        <section
          className="panel payment-form-panel"
          aria-labelledby={`${idPrefix}-form-title`}
        >
          <div className="section-heading">
            <div>
              <h2 id={`${idPrefix}-form-title`}>{t("payments.new")}</h2>
              <p>{t("payments.formIntro")}</p>
            </div>
          </div>
          <form className="payment-form" onSubmit={submit}>
            <label>
              {t("payments.unit")}
              <input value={unitLabel(residentUnitId)} readOnly />
            </label>
            <label>
              {t("payments.amount")}
              <input
                ref={titleInput}
                name="amount"
                inputMode="decimal"
                required
                maxLength={10}
                placeholder={
                  i18n.resolvedLanguage === "tr" ? "2400,00" : "2400.00"
                }
                aria-invalid={error === "payments.errorAmount"}
                aria-describedby={error ? `${idPrefix}-error` : undefined}
              />
            </label>
            <label>
              {t("payments.date")}
              <input
                name="date"
                type="date"
                required
                max={today}
                defaultValue={today}
              />
            </label>
            <label>
              {t("payments.reference")}
              <input name="reference" required maxLength={100} />
            </label>
            <label className="full-width">
              {t("payments.note")}
              <textarea name="note" maxLength={500} rows={3} />
            </label>
            <label className="full-width">
              {t("payments.receipt")}
              <input
                name="receipt"
                type="file"
                accept="application/pdf,image/png,image/jpeg"
              />
              <small>
                {t("payments.receiptHelp", {
                  size: maxReceiptBytes / 1024 / 1024,
                })}
              </small>
            </label>
            {error && (
              <p
                className="form-error full-width"
                id={`${idPrefix}-error`}
                role="alert"
              >
                {t(error)}
              </p>
            )}
            <div className="form-actions full-width">
              <button
                type="button"
                className="secondary"
                onClick={() => {
                  setShowForm(false);
                  setError("");
                  newButton.current?.focus();
                }}
              >
                {t("payments.cancel")}
              </button>
              <button
                className="primary"
                disabled={busy || !!refreshError}
                type="submit"
              >
                {t("payments.submit")}
                <ArrowRight size={16} />
              </button>
            </div>
          </form>
        </section>
      )}
      <section className="panel">
        <div className="section-heading payment-list-heading">
          <div>
            <h2>{t(isManager ? "payments.inbox" : "payments.mine")}</h2>
            <p>{t("live.serverSavedHelp")}</p>
          </div>
          <div className="payment-filters">
            <label className="search">
              <Search size={16} />
              <input
                aria-label={t("payments.search")}
                placeholder={t("payments.search")}
                value={search}
                onChange={(event) => setSearch(event.target.value)}
              />
            </label>
            <label>
              <span className="sr-only">{t("payments.filter")}</span>
              <select
                value={filter}
                onChange={(event) => setFilter(event.target.value)}
              >
                <option value="ALL">{t("payments.allStatuses")}</option>
                {statuses.map((status) => (
                  <option key={status} value={status}>
                    {t(`payments.status.${status}`)}
                  </option>
                ))}
              </select>
            </label>
          </div>
        </div>
        <div className="payment-list">
          {visible.length === 0 ? (
            <p className="empty-state">{t("payments.empty")}</p>
          ) : (
            visible.map((item) => (
              <article key={item.id} className="payment-row">
                <div className="payment-row-heading">
                  <div>
                    <strong>{formatMoney(item.amountMinor)}</strong>
                    <span>
                      {t("payments.unit")} {unitLabel(item.unitId)} ·{" "}
                      {formatDate(item.paymentDate)}
                    </span>
                  </div>
                  <span
                    className={`status payment-status-${item.status.toLowerCase()}`}
                  >
                    {t(`payments.status.${displayStatus(item)}`)}
                  </span>
                </div>
                <dl className="payment-details">
                  <div>
                    <dt>{t("payments.reference")}</dt>
                    <dd>{item.reference}</dd>
                  </div>
                  <div>
                    <dt>{t("payments.notificationId")}</dt>
                    <dd>{item.id}</dd>
                  </div>
                  {item.note && (
                    <div className="full-width">
                      <dt>{t("payments.note")}</dt>
                      <dd>{item.note}</dd>
                    </div>
                  )}
                  {item.reviewNote && (
                    <div className="full-width">
                      <dt>{t("payments.reviewNote")}</dt>
                      <dd>{item.reviewNote}</dd>
                    </div>
                  )}
                </dl>
                <div className="payment-row-actions">
                  {item.receipt ? (
                    <ReceiptLink file={item.receipt} />
                  ) : (
                    <span className="payment-no-receipt">
                      {t("payments.noReceipt")}
                    </span>
                  )}
                  {!linked(item.id) &&
                    item.status === "PENDING" &&
                    (isManager ? (
                      <button
                        className="secondary"
                        onClick={() => {
                          setReviewId(reviewId === item.id ? null : item.id);
                          setReviewNote("");
                          setReviewError("");
                        }}
                      >
                        {t("payments.review")}
                      </button>
                    ) : (
                      <button
                        className="text-button"
                        disabled={busy || !!refreshError}
                        onClick={() => transition(item.id, "WITHDRAWN")}
                      >
                        {t("payments.withdraw")}
                      </button>
                    ))}
                  {isManager &&
                    !linked(item.id) &&
                    ["PENDING", "REVIEWED"].includes(item.status) && (
                      <Link
                        className="primary"
                        href={
                          "/finance?notification=" + encodeURIComponent(item.id)
                        }
                      >
                        {t("finance.recordFromNotification")}
                      </Link>
                    )}
                </div>
                {reviewing?.id === item.id && isManager && (
                  <form
                    className="payment-review"
                    onSubmit={(event) => {
                      event.preventDefault();
                      transition(item.id, "REVIEWED", reviewNote);
                    }}
                  >
                    <label>
                      {t("payments.reviewNote")}
                      <textarea
                        autoFocus
                        rows={2}
                        maxLength={500}
                        value={reviewNote}
                        onChange={(event) => {
                          setReviewNote(event.target.value);
                          setReviewError("");
                        }}
                      />
                    </label>
                    <p>{t("payments.reviewHelp")}</p>
                    {reviewError && (
                      <p className="form-error" role="alert">
                        {t(reviewError)}
                      </p>
                    )}
                    <div className="form-actions">
                      <button
                        type="button"
                        disabled={busy || !!refreshError}
                        className="secondary"
                        onClick={() =>
                          transition(item.id, "REJECTED", reviewNote)
                        }
                      >
                        {t("payments.reject")}
                      </button>
                      <button
                        type="submit"
                        disabled={busy || !!refreshError}
                        className="primary"
                      >
                        <Check size={16} />
                        {t("payments.markReviewed")}
                      </button>
                    </div>
                  </form>
                )}
              </article>
            ))
          )}
        </div>
      </section>
    </div>
  );
}

function ReceiptLink({ file }: { file: AttachmentSource }) {
  const { t } = useTranslation();
  const { url, failed } = useAttachment(file);
  return url ? (
    <a className="receipt-link" href={url} download={file.name}>
      <FileDown size={16} />
      {t("payments.downloadReceipt")}
      <span>{file.name}</span>
    </a>
  ) : (
    <span className="demo-note">
      {t(failed ? "api.attachmentError" : "api.attachmentLoading")}
    </span>
  );
}
