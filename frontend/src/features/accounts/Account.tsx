import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useWorkspace } from "../../workspace/Workspace";
import { useFormat } from "../../shared/useFormat";
import { allocatedTo } from "../finance/model";
import { all, errorKey } from "../../api/server";
import { calendarDateInIstanbul } from "../../shared/calendar";
import type { AccountEntry } from "../../api/generated";

export default function Account({
  isManager,
  requestedUnitId,
}: {
  isManager: boolean;
  requestedUnitId?: string;
}) {
  const { t } = useTranslation();
  const { moneyMinor, date, period } = useFormat();
  const { community, communityId, unitId, state, setUnitId } = useWorkspace();
  const finance = state.finance;
  const [selection, setSelection] = useState(
    requestedUnitId ?? (isManager ? "ALL" : unitId),
  );
  const [tab, setTab] = useState("charges");
  useEffect(() => {
    setSelection(requestedUnitId ?? (isManager ? "ALL" : unitId));
  }, [isManager, unitId, requestedUnitId, community.id]);
  const activeUnit = isManager
    ? selection === "ALL"
      ? undefined
      : selection
    : (requestedUnitId ?? unitId);
  useEffect(() => {
    if (
      !isManager &&
      requestedUnitId &&
      community.myUnitIds.includes(requestedUnitId) &&
      requestedUnitId !== unitId
    )
      setUnitId(requestedUnitId);
  }, [isManager, requestedUnitId, community.myUnitIds, unitId, setUnitId]);
  const [entries, setEntries] = useState<AccountEntry[]>([]);
  const [entryError, setEntryError] = useState("");
  const [entryLoading, setEntryLoading] = useState(false);
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    setEntries([]);
    setEntryError("");
    if (
      tab !== "history" ||
      (activeUnit &&
        !state.unitAccounts.some((item) => item.unitId === activeUnit))
    )
      return () => controller.abort();
    setEntryLoading(true);
    Promise.all(
      state.unitAccounts
        .filter((item) => !activeUnit || item.unitId === activeUnit)
        .map((item) =>
          all<"listAccountEntries", AccountEntry>(
            "listAccountEntries",
            communityId,
            controller.signal,
            {},
            { unitId: item.unitId },
          ),
        ),
    )
      .then((pages) => {
        if (!controller.signal.aborted) setEntries(pages.flat());
      })
      .catch((failure) => {
        if (!controller.signal.aborted) setEntryError(errorKey(failure));
      })
      .finally(() => {
        if (!controller.signal.aborted) setEntryLoading(false);
      });
    return () => controller.abort();
  }, [tab, activeUnit, communityId, state.unitAccounts, attempt]);
  if (
    requestedUnitId &&
    !isManager &&
    !community.myUnitIds.includes(requestedUnitId)
  )
    return (
      <section className="panel route-state">
        <h2>{t("navigation.accessDenied")}</h2>
      </section>
    );
  if (activeUnit && !community.units.some((unit) => unit.id === activeUnit))
    return (
      <section className="panel route-state">
        <h2>{t("units.notFound")}</h2>
      </section>
    );
  const charges = finance.charges.filter(
    (charge) => !activeUnit || charge.unitId === activeUnit,
  );
  const payments = finance.payments.filter(
    (payment) => !activeUnit || payment.unitId === activeUnit,
  );
  const unitLabel = (id: string) =>
    community.units.find((unit) => unit.id === id)?.label ?? id;
  const summaries = state.unitAccounts.filter(
    (item) => !activeUnit || item.unitId === activeUnit,
  );
  const movements = entries.map((item) => ({
    id: item.id,
    on: calendarDateInIstanbul(new Date(item.occurredAt)),
    label: item.description,
    unitId: item.unitId,
    amountMinor: item.amountMinor,
    balanceAfterMinor: item.balanceAfterMinor,
    kind:
      item.type === "CHARGE"
        ? "finance.charge"
        : item.type === "PAYMENT"
          ? "finance.recorded"
          : item.type === "CHARGE_CANCELLATION"
            ? "finance.cancelled"
            : "finance.reversed",
  }));
  return (
    <div className="account-feature">
      {isManager && (
        <label className="account-unit-select">
          {t("UNIT")}
          <select
            value={selection}
            onChange={(event) => setSelection(event.target.value)}
          >
            <option value="ALL">{t("payments.allUnits")}</option>
            {community.units.map((unit) => (
              <option key={unit.id} value={unit.id}>
                {unit.label}
              </option>
            ))}
          </select>
        </label>
      )}
      <section className="unit-summary-grid">
        <div>
          <span>{t("units.outstanding")}</span>
          <strong>
            {moneyMinor(
              summaries.reduce((sum, item) => sum + item.outstandingMinor, 0),
            )}
          </strong>
        </div>
        <div>
          <span>{t("units.recordedPayments")}</span>
          <strong>
            {moneyMinor(
              summaries.reduce((sum, item) => sum + item.paidMinor, 0),
            )}
          </strong>
        </div>
        <div>
          <span>{t("units.unallocatedCredit")}</span>
          <strong>
            {moneyMinor(
              summaries.reduce((sum, item) => sum + item.creditMinor, 0),
            )}
          </strong>
        </div>
      </section>
      <div className="unit-tabs">
        {["charges", "history", "receipts"].map((value) => (
          <button
            key={value}
            className={tab === value ? "active" : ""}
            aria-pressed={tab === value}
            onClick={() => setTab(value)}
          >
            {t(`finance.tab.${value}`)}
          </button>
        ))}
      </div>
      {tab === "history" && entryLoading && (
        <p role="status">{t("live.loading")}</p>
      )}
      {tab === "history" && entryError && (
        <div role="alert">
          <p>{t(entryError)}</p>
          <button
            className="secondary"
            onClick={() => setAttempt((value) => value + 1)}
          >
            {t("units.retry")}
          </button>
        </div>
      )}
      <section className="panel">
        <div className="section-heading">
          <div>
            <h2>{t(isManager ? "Community charges" : "My charges")}</h2>
            <p>{t("finance.accountHelp")}</p>
          </div>
          <span className="tag">TRY</span>
        </div>
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>{t("UNIT")}</th>
                <th>{t("DESCRIPTION")}</th>
                <th>{t("DATE")}</th>
                <th>{t("AMOUNT")}</th>
                <th>{t("STATUS")}</th>
              </tr>
            </thead>
            <tbody>
              {tab === "charges" &&
                charges.map((charge) => (
                  <tr key={charge.id}>
                    <td>
                      <strong>{unitLabel(charge.unitId)}</strong>
                    </td>
                    <td>
                      <strong>
                        {charge.custom
                          ? charge.description
                          : t(charge.description)}
                      </strong>
                      <small>{period(charge.period)}</small>
                    </td>
                    <td>{date(charge.dueOn)}</td>
                    <td>{moneyMinor(charge.amountMinor)}</td>
                    <td>
                      <span className="status">
                        {t(
                          charge.cancelledAt
                            ? "finance.cancelled"
                            : allocatedTo(finance, charge.id) ===
                                charge.amountMinor
                              ? "Paid"
                              : "Outstanding",
                        )}
                      </span>
                    </td>
                  </tr>
                ))}
              {tab === "receipts" &&
                payments.map((payment) => (
                  <tr key={payment.id}>
                    <td>{unitLabel(payment.unitId)}</td>
                    <td>
                      <strong>{payment.reference}</strong>
                      {payment.reason && <small>{payment.reason}</small>}
                    </td>
                    <td>{date(payment.paidOn)}</td>
                    <td>{moneyMinor(payment.amountMinor)}</td>
                    <td>
                      {t(
                        payment.reversedAt
                          ? "finance.reversed"
                          : "finance.recorded",
                      )}
                    </td>
                  </tr>
                ))}
              {tab === "history" &&
                movements.map((item) => (
                  <tr key={item.id}>
                    <td>{unitLabel(item.unitId)}</td>
                    <td>
                      <strong>{item.label}</strong>
                    </td>
                    <td>{date(item.on.slice(0, 10))}</td>
                    <td>{moneyMinor(item.amountMinor)}</td>
                    <td>{t(item.kind)}</td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
        {(tab === "charges"
          ? charges.length
          : tab === "receipts"
            ? payments.length
            : movements.length) === 0 && (
          <p className="empty-state">{t("finance.empty")}</p>
        )}
      </section>
    </div>
  );
}
