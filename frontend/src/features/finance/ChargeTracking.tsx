import { useState } from "react";
import { Link } from "wouter";
import { useTranslation } from "react-i18next";
import { useWorkspace } from "../../workspace/Workspace";
import { useFormat } from "../../shared/useFormat";
import { calendarDateInIstanbul } from "../../shared/calendar";
import { allocatedTo, type Charge } from "./model";

export default function ChargeTracking({
  category,
  onCancel,
}: {
  category: "DUES" | "OTHER";
  onCancel: (id: string) => void;
}) {
  const { t } = useTranslation();
  const { moneyMinor, period, locale } = useFormat();
  const { community, state, can, busy, refreshError } = useWorkspace();
  const [query, setQuery] = useState("");
  const [year, setYear] = useState("");
  const [month, setMonth] = useState("");
  const [status, setStatus] = useState("ALL");
  const currentMonth = calendarDateInIstanbul(new Date()).slice(0, 7);
  const records = state.finance.charges.filter((item) =>
    category === "DUES" ? item.category === "DUES" : item.category !== "DUES",
  );
  const outstanding = (item: Charge) =>
    item.cancelledAt
      ? 0
      : (item.outstandingMinor ??
        item.amountMinor - allocatedTo(state.finance, item.id));
  const unit = (id: string) => community.units.find((item) => item.id === id);
  const visible = records
    .filter(
      (item) =>
        `${unit(item.unitId)?.label ?? item.unitId} ${unit(item.unitId)?.blockLabel ?? ""} ${item.description}`
          .toLocaleLowerCase(locale)
          .includes(query.trim().toLocaleLowerCase(locale)) &&
        (!year || item.period.slice(0, 4) === year) &&
        (!month || item.period.slice(5, 7) === month) &&
        (status === "ALL" ||
          (status === "CANCELLED" && !!item.cancelledAt) ||
          (!item.cancelledAt &&
            (status === "PAID"
              ? outstanding(item) === 0
              : outstanding(item) > 0))),
    )
    .sort(
      (a, b) =>
        b.period.localeCompare(a.period) ||
        (unit(a.unitId)?.label ?? "").localeCompare(
          unit(b.unitId)?.label ?? "",
          locale,
          { numeric: true },
        ),
    );
  return (
    <>
      <section className="tracking-widgets" aria-label={t("manager.summary")}>
        <article className="panel">
          <span>
            {t(
              category === "DUES"
                ? "manager.totalUnpaidDues"
                : "manager.totalOtherDebt",
            )}
          </span>
          <strong>
            {moneyMinor(
              records.reduce((sum, item) => sum + outstanding(item), 0),
            )}
          </strong>
          <small>{t("manager.allPeriods")}</small>
        </article>
        <article className="panel">
          <span>
            {t(
              category === "DUES"
                ? "manager.monthUnpaidDues"
                : "manager.monthOtherDebt",
            )}
          </span>
          <strong>
            {moneyMinor(
              records
                .filter((item) => item.period.slice(0, 7) === currentMonth)
                .reduce((sum, item) => sum + outstanding(item), 0),
            )}
          </strong>
          <small>{period(currentMonth + "-01")}</small>
        </article>
      </section>
      {category === "OTHER" &&
        records.some(
          (item) => !item.category || item.category === "UNSPECIFIED",
        ) && <p className="section-help">{t("manager.legacyCharges")}</p>}
      <section className="panel">
        <div className="tracking-filters">
          <label>
            {t("manager.unitSearch")}
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder={t("manager.unitSearch")}
            />
          </label>
          <label>
            {t("manager.year")}
            <select
              aria-label={t("manager.year")}
              value={year}
              onChange={(event) => setYear(event.target.value)}
            >
              <option value="">{t("manager.allYears")}</option>
              {[...new Set(records.map((item) => item.period.slice(0, 4)))]
                .sort()
                .reverse()
                .map((value) => (
                  <option key={value}>{value}</option>
                ))}
            </select>
          </label>
          <label>
            {t("manager.month")}
            <select
              aria-label={t("manager.month")}
              value={month}
              onChange={(event) => setMonth(event.target.value)}
            >
              <option value="">{t("manager.allMonths")}</option>
              {Array.from({ length: 12 }, (_, i) =>
                String(i + 1).padStart(2, "0"),
              ).map((value) => (
                <option value={value} key={value}>
                  {new Intl.DateTimeFormat(locale, {
                    month: "long",
                    timeZone: "UTC",
                  }).format(new Date("2026-" + value + "-01T12:00:00Z"))}
                </option>
              ))}
            </select>
          </label>
          <label>
            {t("STATUS")}
            <select
              aria-label={t("STATUS")}
              value={status}
              onChange={(event) => setStatus(event.target.value)}
            >
              <option value="ALL">{t("manager.allStatuses")}</option>
              <option value="PAID">{t("Paid")}</option>
              <option value="OUTSTANDING">{t("Outstanding")}</option>
              <option value="CANCELLED">{t("finance.cancelled")}</option>
            </select>
          </label>
          <button
            className="text-button"
            onClick={() => {
              setQuery("");
              setYear("");
              setMonth("");
              setStatus("ALL");
            }}
          >
            {t("units.clearFilters")}
          </button>
        </div>
        <div className="table-scroll">
          <table className="charge-tracking-table">
            <thead>
              <tr>
                {[
                  "UNIT",
                  "finance.period",
                  "DESCRIPTION",
                  "AMOUNT",
                  "manager.paidAmount",
                  "manager.remaining",
                  "STATUS",
                  "ACTION",
                ].map((label) => (
                  <th key={label}>{t(label)}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {visible.map((item) => (
                <tr key={item.id}>
                  <td>
                    <Link href={"/units/" + encodeURIComponent(item.unitId)}>
                      {unit(item.unitId)?.label ?? item.unitId}
                    </Link>
                    <small>{unit(item.unitId)?.blockLabel}</small>
                  </td>
                  <td>{period(item.period)}</td>
                  <td>
                    {item.description}
                    {category === "OTHER" &&
                      (!item.category || item.category === "UNSPECIFIED") && (
                        <small>{t("manager.unspecified")}</small>
                      )}
                  </td>
                  <td>{moneyMinor(item.amountMinor)}</td>
                  <td>{moneyMinor(allocatedTo(state.finance, item.id))}</td>
                  <td>{moneyMinor(outstanding(item))}</td>
                  <td>
                    <span
                      className={
                        "status " +
                        (outstanding(item) === 0 ? "resolved" : "open")
                      }
                    >
                      {t(
                        item.cancelledAt
                          ? "finance.cancelled"
                          : outstanding(item) === 0
                            ? "Paid"
                            : allocatedTo(state.finance, item.id) > 0
                              ? "manager.partial"
                              : "Outstanding",
                      )}
                    </span>
                  </td>
                  <td>
                    {!item.cancelledAt && can("CHARGE_CREATE") && (
                      <button
                        className="text-button"
                        disabled={
                          busy ||
                          !!refreshError ||
                          allocatedTo(state.finance, item.id) > 0
                        }
                        onClick={() => onCancel(item.id)}
                      >
                        {t("finance.cancelCharge")}
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {!visible.length && (
          <p className="empty-state">{t("manager.noCharges")}</p>
        )}
      </section>
    </>
  );
}
