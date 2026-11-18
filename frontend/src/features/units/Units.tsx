import { useWorkspace } from "../../workspace/Workspace";
import ResidentDirectory from "./ResidentDirectory";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useSearchParams } from "wouter";
import { ArrowLeft, Building2, RefreshCw, Users } from "lucide-react";
import { all } from "../../api/server";
import type { Relationship } from "../../api/generated";
import { calendarDateInIstanbul } from "../../shared/calendar";
import { useFormat } from "../../shared/useFormat";
import { isCurrent, type CommunitySnapshot, type Unit } from "./model";

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
  const { locale } = useFormat();
  const [params, setParams] = useSearchParams();
  const query = params.get("q") ?? "";
  const block = params.get("block") ?? "";
  const names = (id: string, type: "OWNER" | "TENANT") =>
    snapshot.relationships
      .filter(
        (item) =>
          item.unitId === id &&
          item.type === type &&
          isCurrent(item, snapshot.asOf),
      )
      .map(
        (item) =>
          snapshot.people.find((person) => person.id === item.personId)?.name ??
          t("units.unknownPerson"),
      )
      .join(", ") || t("units.notRecorded");
  const visible = snapshot.units.filter(
    (unit) =>
      (!block || unit.block === block) &&
      `${unit.label} ${unit.blockLabel} ${names(unit.id, "OWNER")} ${names(unit.id, "TENANT")}`
        .toLocaleLowerCase(locale)
        .includes(query.trim().toLocaleLowerCase(locale)),
  );
  const update = (key: string, value: string) => {
    const next = new URLSearchParams(params);
    if (value) next.set(key, value);
    else next.delete(key);
    setParams(next, { replace: true });
  };
  return (
    <section className="panel unit-directory">
      <div className="section-heading">
        <div>
          <h2>{t("manager.directory")}</h2>
          <p>{t("manager.directoryHelp")}</p>
        </div>
      </div>
      <div className="tracking-filters">
        <label>
          {t("units.search")}
          <input
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
            <option value="">{t("units.allBlocks")}</option>
            {[
              ...new Map(
                snapshot.units.map((unit) => [
                  unit.block,
                  unit.blockLabel ?? unit.block,
                ]),
              ).entries(),
            ].map(([id, name]) => (
              <option key={id} value={id}>
                {name}
              </option>
            ))}
          </select>
        </label>
        <button
          className="text-button"
          onClick={() => setParams(new URLSearchParams(), { replace: true })}
        >
          {t("units.clearFilters")}
        </button>
      </div>
      <div className="table-scroll">
        <table className="unit-table">
          <thead>
            <tr>
              <th>{t("UNIT")}</th>
              <th>{t("units.block")}</th>
              <th>{t("units.owner")}</th>
              <th>{t("units.tenant")}</th>
              <th>{t("ACTION")}</th>
            </tr>
          </thead>
          <tbody>
            {visible.map((unit) => (
              <tr key={unit.id}>
                <td>
                  <Link href={"/units/" + encodeURIComponent(unit.id)}>
                    {unit.label}
                  </Link>
                </td>
                <td>{unit.blockLabel ?? unit.block}</td>
                <td>{names(unit.id, "OWNER")}</td>
                <td>{names(unit.id, "TENANT")}</td>
                <td>
                  <Link
                    href={"/units/" + encodeURIComponent(unit.id)}
                    className="text-button"
                  >
                    {t("units.viewDetail", { unit: unit.label })}
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {!visible.length && <p className="empty-state">{t("units.empty")}</p>}
    </section>
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
  const { date } = useFormat();
  const relationships = snapshot.relationships
    .filter((item) => item.unitId === unit.id)
    .sort((a, b) => b.startsOn.localeCompare(a.startsOn));
  const current = relationships.filter((item) =>
    isCurrent(item, snapshot.asOf),
  );
  const past = relationships.filter((item) => !isCurrent(item, snapshot.asOf));
  function people(items: typeof relationships) {
    return items.map((item) => (
      <article className="relationship-card" key={item.id}>
        <Users size={22} />
        <div>
          <h3>
            {snapshot.people.find((person) => person.id === item.personId)
              ?.name ?? t("units.unknownPerson")}
          </h3>
          <strong>{t("units.relation." + item.type)}</strong>
          <p>
            {date(item.startsOn)} -{" "}
            {item.endsOn ? date(item.endsOn) : t("units.ongoing")}
          </p>
        </div>
      </article>
    ));
  }
  return (
    <div className="unit-detail">
      <Link href="/units" className="text-button unit-back">
        <ArrowLeft size={16} />
        {t("manager.directory")}
      </Link>
      <section className="panel unit-identity">
        <Building2 size={30} />
        <div>
          <h2>{t("units.unitTitle", { unit: unit.label })}</h2>
          <p>
            {unit.blockLabel ?? unit.block} ?{" "}
            {t("units.floorValue", { floor: unit.floor })} ?{" "}
            {t("units.area", { area: unit.areaSqm })}
          </p>
        </div>
      </section>
      <section className="panel">
        <div className="section-heading">
          <h2>{t("manager.currentPeople")}</h2>
        </div>
        <div className="relationship-list">
          {current.length ? (
            people(current)
          ) : (
            <p className="empty-state">{t("units.notRecorded")}</p>
          )}
        </div>
      </section>
      {past.length > 0 && (
        <details className="panel directory-history">
          <summary>{t("manager.pastPeople")}</summary>
          <div className="relationship-list">{people(past)}</div>
        </details>
      )}
    </div>
  );
}
