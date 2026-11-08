import { useWorkspace } from "../../workspace/Workspace";
import ResidentDirectory from "./ResidentDirectory";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useSearchParams } from "wouter";
import {
  ArrowLeft,
  ArrowRight,
  Building2,
  RefreshCw,
  Search,
  Users,
  Wallet,
} from "lucide-react";
import { all } from "../../api/server";
import type { Relationship } from "../../api/generated";
import { calendarDateInIstanbul } from "../../shared/calendar";
import { useFormat } from "../../shared/useFormat";
import {
  isCurrent,
  unitSummary,
  type CommunitySnapshot,
  type Unit,
} from "./model";

type LoadState =
  | { status: "loading" }
  | { status: "error" }
  | { status: "ready"; data: CommunitySnapshot };

export default function Units({
  unitId,
  isManager,
}: {
  unitId?: string;
  isManager: boolean;
}) {
  return isManager ? (
    <ManagerUnits unitId={unitId} />
  ) : (
    <ResidentDirectory unitId={unitId} />
  );
}

function ManagerUnits({ unitId }: { unitId?: string }) {
  const { community, communityId, state: workspaceState } = useWorkspace();
  const { t } = useTranslation();
  const [state, setState] = useState<LoadState>({ status: "loading" });
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    setState({ status: "loading" });
    Promise.all(
      community.units.map((unit) =>
        all<"listRelationships", Relationship>(
          "listRelationships",
          communityId,
          controller.signal,
          {},
          { unitId: unit.id },
        ),
      ),
    )
      .then((pages) => {
        if (!controller.signal.aborted) {
          const relationships = pages.flat();
          setState({
            status: "ready",
            data: {
              communityName: community.name,
              asOf: calendarDateInIstanbul(new Date()),
              units: community.units,
              people: [
                ...new Map(
                  relationships.map((item) => [item.person.id, item.person]),
                ).values(),
              ],
              relationships: relationships.map((item) => ({
                ...item,
                personId: item.person.id,
              })),
              charges: [],
              payments: [],
              accounts: workspaceState.unitAccounts,
            },
          });
        }
      })
      .catch(() => {
        if (!controller.signal.aborted) setState({ status: "error" });
      });
    return () => controller.abort();
  }, [attempt, communityId, community.units, workspaceState.unitAccounts]);
  if (state.status === "loading")
    return (
      <section className="panel route-state" role="status">
        <RefreshCw className="loading-icon" size={24} />
        <h2>{t("units.loading")}</h2>
      </section>
    );
  if (state.status === "error")
    return (
      <section className="panel route-state" role="alert">
        <h2>{t("units.loadError")}</h2>
        <p>{t("units.loadErrorHelp")}</p>
        <button
          className="primary"
          onClick={() => setAttempt((previous) => previous + 1)}
        >
          <RefreshCw size={16} />
          {t("units.retry")}
        </button>
      </section>
    );
  const snapshot = {
    ...state.data,
    accounts: workspaceState.unitAccounts,
    charges: workspaceState.finance.charges
      .filter((item) => !item.cancelledAt)
      .map((item) => ({
        id: item.id,
        unitId: item.unitId,
        descriptionKey: item.description,
        period: item.period,
        dueOn: item.dueOn,
        amountMinor: item.amountMinor,
      })),
    payments: workspaceState.finance.payments
      .filter((item) => !item.reversedAt)
      .map((item) => ({
        id: item.id,
        unitId: item.unitId,
        paidOn: item.paidOn,
        amountMinor: item.amountMinor,
        allocations: item.allocations,
      })),
  };
  if (!unitId) return <UnitDirectory snapshot={snapshot} />;
  const unit = state.data.units.find((item) => item.id === unitId);
  if (!unit)
    return (
      <section className="panel route-state">
        <Building2 size={28} />
        <h2>{t("units.notFound")}</h2>
        <p>{t("units.notFoundHelp")}</p>
        <Link className="primary" href="/units">
          {t("units.back")}
        </Link>
      </section>
    );
  return <UnitDetail snapshot={snapshot} unit={unit} />;
}

function UnitDirectory({ snapshot }: { snapshot: CommunitySnapshot }) {
  const { t } = useTranslation();
  const { locale, moneyMinor, date } = useFormat();
  const [params, setParams] = useSearchParams();
  const query = params.get("q") ?? "";
  const block = params.get("block") ?? "ALL";
  const relationship = params.get("relationship") ?? "ALL";
  const current = snapshot.relationships.filter((item) =>
    isCurrent(item, snapshot.asOf),
  );
  const peopleFor = (unitId: string, type: "OWNER" | "TENANT") =>
    current
      .filter((item) => item.unitId === unitId && item.type === type)
      .map(
        (item) =>
          snapshot.people.find((person) => person.id === item.personId)?.name ??
          t("units.unknownPerson"),
      );
  const blocks = [...new Set(snapshot.units.map((unit) => unit.block))].sort();
  function update(key: string, value: string) {
    const next = new URLSearchParams(params);
    if (value && value !== "ALL") next.set(key, value);
    else next.delete(key);
    setParams(next, { replace: true });
  }
  const visible = snapshot.units.filter((unit) => {
    const owners = peopleFor(unit.id, "OWNER");
    const tenants = peopleFor(unit.id, "TENANT");
    const hasRelation = owners.length + tenants.length > 0;
    const relationshipMatch =
      relationship === "ALL" ||
      (relationship === "TENANT"
        ? tenants.length > 0
        : relationship === "OWNER_ONLY"
          ? owners.length > 0 && tenants.length === 0
          : relationship === "NONE"
            ? !hasRelation
            : false);
    const haystack =
      `${unit.label} ${unit.block} ${owners.join(" ")} ${tenants.join(" ")}`.toLocaleLowerCase(
        locale,
      );
    return (
      (block === "ALL" || unit.block === block) &&
      relationshipMatch &&
      haystack.includes(query.trim().toLocaleLowerCase(locale))
    );
  });
  const totalOutstanding = snapshot.units.reduce(
    (total, unit) => total + unitSummary(snapshot, unit.id).outstandingMinor,
    0,
  );
  const listHref = `/units${params.size ? `?${params.toString()}` : ""}`;
  return (
    <>
      <div className="unit-context">
        <span className="tag">{t("units.readOnly")}</span>
        <p>{t("units.snapshot", { date: date(snapshot.asOf) })}</p>
      </div>
      <section className="unit-summary-grid">
        <div>
          <Building2 size={20} />
          <span>{t("units.sampleUnits")}</span>
          <strong>{snapshot.units.length}</strong>
        </div>
        <div>
          <Users size={20} />
          <span>{t("units.currentRelationships")}</span>
          <strong>{current.length}</strong>
        </div>
        <div>
          <Wallet size={20} />
          <span>{t("units.sampleOutstanding")}</span>
          <strong>{moneyMinor(totalOutstanding)}</strong>
        </div>
      </section>
      <section className="panel unit-directory">
        <div className="section-heading">
          <div>
            <h2>{t("Units")}</h2>
            <p role="status">
              {t("units.resultCount", {
                count: visible.length,
                total: snapshot.units.length,
              })}
            </p>
          </div>
        </div>
        <div className="unit-filters">
          <label className="search">
            <Search size={17} />
            <input
              aria-label={t("units.search")}
              placeholder={t("units.search")}
              value={query}
              onChange={(event) => update("q", event.target.value)}
            />
          </label>
          <label>
            {t("units.block")}
            <select
              value={block}
              onChange={(event) => update("block", event.target.value)}
            >
              <option value="ALL">{t("units.allBlocks")}</option>
              {blocks.map((value) => (
                <option key={value} value={value}>
                  {snapshot.units.find((unit) => unit.block === value)
                    ?.blockLabel ?? t("units.blockName", { block: value })}
                </option>
              ))}
            </select>
          </label>
          <label>
            {t("units.relationshipFilter")}
            <select
              value={relationship}
              onChange={(event) => update("relationship", event.target.value)}
            >
              <option value="ALL">{t("units.allRelationships")}</option>
              <option value="TENANT">{t("units.withTenant")}</option>
              <option value="OWNER_ONLY">{t("units.ownerOnly")}</option>
              <option value="NONE">{t("units.noRelationship")}</option>
            </select>
          </label>
          {(query || block !== "ALL" || relationship !== "ALL") && (
            <button
              className="text-button"
              onClick={() =>
                setParams(new URLSearchParams(), { replace: true })
              }
            >
              {t("units.clearFilters")}
            </button>
          )}
        </div>
        {visible.length === 0 ? (
          <div className="route-state">
            <Search size={26} />
            <h2>{t("units.empty")}</h2>
            <p>{t("units.emptyHelp")}</p>
            <button
              className="secondary"
              onClick={() =>
                setParams(new URLSearchParams(), { replace: true })
              }
            >
              {t("units.clearFilters")}
            </button>
          </div>
        ) : (
          <div className="table-scroll">
            <table className="unit-table">
              <thead>
                <tr>
                  <th>{t("UNIT")}</th>
                  <th>{t("units.owner")}</th>
                  <th>{t("units.tenant")}</th>
                  <th>{t("units.outstanding")}</th>
                  <th>{t("ACTION")}</th>
                </tr>
              </thead>
              <tbody>
                {visible.map((unit) => (
                  <tr key={unit.id}>
                    <td>
                      <strong>{unit.label}</strong>
                      <small>
                        {t(`units.kind.${unit.kind}`)} ·{" "}
                        {t("units.floorValue", { floor: unit.floor })}
                      </small>
                    </td>
                    <td>
                      {peopleFor(unit.id, "OWNER").join(", ") ||
                        t("units.notRecorded")}
                    </td>
                    <td>
                      {peopleFor(unit.id, "TENANT").join(", ") ||
                        t("units.notRecorded")}
                    </td>
                    <td>
                      <strong>
                        {moneyMinor(
                          unitSummary(snapshot, unit.id).outstandingMinor,
                        )}
                      </strong>
                    </td>
                    <td>
                      <Link
                        className="text-button"
                        href={`/units/${encodeURIComponent(unit.id)}${params.size ? `?${params.toString()}` : ""}`}
                        state={{ listHref }}
                      >
                        {t("units.viewDetail", { unit: unit.label })}
                        <ArrowRight size={15} />
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  );
}

function UnitDetail({
  snapshot,
  unit,
}: {
  snapshot: CommunitySnapshot;
  unit: Unit;
}) {
  const { t } = useTranslation();
  const { moneyMinor, date, period } = useFormat();
  const [params] = useSearchParams();
  const [tab, setTab] = useState<"relationships" | "account">("relationships");
  useEffect(() => {
    setTab("relationships");
  }, [unit.id]);
  const summary = unitSummary(snapshot, unit.id);
  const relationships = snapshot.relationships
    .filter((item) => item.unitId === unit.id)
    .sort((a, b) => b.startsOn.localeCompare(a.startsOn));
  const backHref = `/units${params.size ? `?${params.toString()}` : ""}`;
  return (
    <div className="unit-detail">
      <Link href={backHref} className="text-button unit-back">
        <ArrowLeft size={16} />
        {t("units.back")}
      </Link>
      <section className="panel unit-identity">
        <span className="unit-building-icon">
          <Building2 size={32} />
        </span>
        <div>
          <span className="eyebrow">{snapshot.communityName ?? ""}</span>
          <h2>{t("units.unitTitle", { unit: unit.label })}</h2>
          <p>
            {t(`units.kind.${unit.kind}`)} ·{" "}
            {unit.blockLabel ?? t("units.blockName", { block: unit.block })} ·{" "}
            {t("units.floorValue", { floor: unit.floor })} ·{" "}
            {t("units.area", { area: unit.areaSqm })}
          </p>
        </div>
        <span className="tag">{t("units.readOnly")}</span>
      </section>
      <section className="unit-summary-grid">
        <div>
          <span>{t("units.totalCharged")}</span>
          <strong>{moneyMinor(summary.chargedMinor)}</strong>
        </div>
        <div>
          <span>{t("units.recordedPayments")}</span>
          <strong>{moneyMinor(summary.paidMinor)}</strong>
        </div>
        <div>
          <span>{t("units.outstanding")}</span>
          <strong>{moneyMinor(summary.outstandingMinor)}</strong>
        </div>
      </section>
      <div className="unit-tabs" aria-label={t("units.detailSections")}>
        <button
          className={tab === "relationships" ? "active" : ""}
          aria-pressed={tab === "relationships"}
          onClick={() => setTab("relationships")}
        >
          <Users size={17} />
          {t("units.relationshipHistory")}
        </button>
        <button
          className={tab === "account" ? "active" : ""}
          aria-pressed={tab === "account"}
          onClick={() => setTab("account")}
        >
          <Wallet size={17} />
          {t("units.accountSummary")}
        </button>
      </div>
      {tab === "relationships" ? (
        <section className="panel">
          <div className="section-heading">
            <div>
              <h2>{t("units.relationshipHistory")}</h2>
              <p>{t("units.historyHelp", { date: date(snapshot.asOf) })}</p>
            </div>
          </div>
          <div className="relationship-list">
            {relationships.length === 0 ? (
              <p className="empty-state">{t("units.noHistory")}</p>
            ) : (
              relationships.map((item) => (
                <article className="relationship-card" key={item.id}>
                  <span className="avatar">
                    {snapshot.people
                      .find((person) => person.id === item.personId)
                      ?.name.split(" ")
                      .map((part) => part[0])
                      .slice(0, 2)
                      .join("") ?? "—"}
                  </span>
                  <div>
                    <h3>
                      {snapshot.people.find(
                        (person) => person.id === item.personId,
                      )?.name ?? t("units.unknownPerson")}
                    </h3>
                    <span>{t(`units.relation.${item.type}`)}</span>
                    <p>
                      {date(item.startsOn)} —{" "}
                      {item.endsOn ? date(item.endsOn) : t("units.ongoing")}
                    </p>
                  </div>
                  <span
                    className={`status ${isCurrent(item, snapshot.asOf) ? "resolved" : "payment-status-withdrawn"}`}
                  >
                    {t(
                      isCurrent(item, snapshot.asOf)
                        ? "units.current"
                        : "units.past",
                    )}
                  </span>
                </article>
              ))
            )}
          </div>
        </section>
      ) : (
        <section className="panel">
          <div className="section-heading">
            <div>
              <h2>{t("units.accountSummary")}</h2>
              <p>{t("units.accountHelp")}</p>
            </div>
          </div>
          <div className="unit-account-section">
            <h3>{t("units.charges")}</h3>
            {summary.charges.length === 0 ? (
              <p className="empty-state">{t("units.noCharges")}</p>
            ) : (
              <div className="table-scroll">
                <table>
                  <thead>
                    <tr>
                      <th>{t("DESCRIPTION")}</th>
                      <th>{t("DUE DATE")}</th>
                      <th>{t("AMOUNT")}</th>
                      <th>{t("STATUS")}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {summary.charges.map((charge) => {
                      const allocated = summary.payments
                        .flatMap((payment) => payment.allocations)
                        .filter(
                          (allocation) => allocation.chargeId === charge.id,
                        )
                        .reduce(
                          (sum, allocation) => sum + allocation.amountMinor,
                          0,
                        );
                      return (
                        <tr key={charge.id}>
                          <td>
                            <strong>{charge.descriptionKey}</strong>
                            <small>{period(charge.period)}</small>
                          </td>
                          <td>{date(charge.dueOn)}</td>
                          <td>{moneyMinor(charge.amountMinor)}</td>
                          <td>
                            <span
                              className={`status ${allocated === charge.amountMinor ? "resolved" : "open"}`}
                            >
                              {t(
                                allocated === charge.amountMinor
                                  ? "Paid"
                                  : "Outstanding",
                              )}
                            </span>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
            <h3>{t("units.recordedPayments")}</h3>
            {summary.payments.length === 0 ? (
              <p className="empty-state">{t("units.noPayments")}</p>
            ) : (
              <div className="unit-payment-list">
                {summary.payments.map((payment) => (
                  <div key={payment.id}>
                    <span>
                      <strong>{t("units.bankTransfer")}</strong>
                      <small>{date(payment.paidOn)}</small>
                    </span>
                    <strong>{moneyMinor(payment.amountMinor)}</strong>
                  </div>
                ))}
              </div>
            )}
            <div className="unit-credit">
              <span>{t("units.unallocatedCredit")}</span>
              <strong>{moneyMinor(summary.creditMinor)}</strong>
            </div>
          </div>
        </section>
      )}
    </div>
  );
}
