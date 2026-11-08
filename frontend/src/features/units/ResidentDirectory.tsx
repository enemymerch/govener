import { useWorkspace } from "../../workspace/Workspace";
import { useTranslation } from "react-i18next";
import { Link, useSearchParams } from "wouter";
import {
  ArrowLeft,
  ArrowRight,
  Building2,
  Home,
  Search,
  Users,
} from "lucide-react";
import { calendarDateInIstanbul } from "../../shared/calendar";
import { useFormat } from "../../shared/useFormat";
import type { ResidentDirectorySnapshot } from "./residentModel";

export default function ResidentDirectory({ unitId }: { unitId?: string }) {
  const { state, community } = useWorkspace();
  const directory = state.directory;
  const snapshot: ResidentDirectorySnapshot = {
    asOf: calendarDateInIstanbul(new Date()),
    currentPersonId: "",
    ownUnitIds: community.myUnitIds,
    units: directory.map((item) => ({
      id: item.id,
      label: item.label,
      block: item.blockId,
      blockLabel: item.blockLabel,
    })),
    people: [
      ...new Map(
        directory.flatMap((item) =>
          item.residents.map((person) => [person.id, person] as const),
        ),
      ).values(),
    ],
    residences: directory.flatMap((item) =>
      item.residents.map((person) => ({
        id: item.id + ":" + person.id,
        unitId: item.id,
        personId: person.id,
      })),
    ),
  };
  return <DirectoryContent snapshot={snapshot} unitId={unitId} />;
}

function DirectoryContent({
  snapshot,
  unitId,
}: {
  snapshot: ResidentDirectorySnapshot;
  unitId?: string;
}) {
  const { community } = useWorkspace();
  const { t } = useTranslation();
  const { locale, date } = useFormat();
  const [params, setParams] = useSearchParams();
  const query = params.get("q") ?? "";
  const block = params.get("block") ?? "ALL";
  const scope = params.get("scope") ?? "ALL";
  const peopleFor = (id: string) =>
    snapshot.residences
      .filter((item) => item.unitId === id)
      .map((item) =>
        snapshot.people.find((person) => person.id === item.personId),
      )
      .filter((person) => person !== undefined);
  const own = (id: string) => snapshot.ownUnitIds.includes(id);
  const suffix = params.size ? `?${params.toString()}` : "";
  const backHref = `/units${suffix}`;
  function update(key: string, value: string) {
    const next = new URLSearchParams(params);
    if (value && value !== "ALL") next.set(key, value);
    else next.delete(key);
    setParams(next, { replace: true });
  }
  if (unitId) {
    const unit = snapshot.units.find((item) => item.id === unitId);
    if (!unit)
      return (
        <section className="panel route-state">
          <Building2 size={28} />
          <h2>{t("units.notFound")}</h2>
          <p>{t("units.notFoundHelp")}</p>
          <Link className="primary" href={backHref}>
            {t("units.back")}
          </Link>
        </section>
      );
    const residents = peopleFor(unit.id);
    return (
      <div className="unit-detail resident-unit-detail">
        <Link href={backHref} className="text-button unit-back">
          <ArrowLeft size={16} />
          {t("directory.back")}
        </Link>
        <section className="panel unit-identity">
          <span className="unit-building-icon">
            <Home size={32} />
          </span>
          <div>
            <span className="eyebrow">{community.name}</span>
            <h2>{t("units.unitTitle", { unit: unit.label })}</h2>
            <p>
              {unit.blockLabel ?? t("units.blockName", { block: unit.block })}
            </p>
          </div>
          {own(unit.id) && <span className="tag">{t("directory.myUnit")}</span>}
        </section>
        <section className="panel resident-household">
          <div className="section-heading">
            <div>
              <h2>
                {t(
                  own(unit.id)
                    ? "directory.myHousehold"
                    : "directory.currentResidents",
                )}
              </h2>
              <p>{t("directory.asOf", { date: date(snapshot.asOf) })}</p>
            </div>
            <span className="tag">
              {t("directory.peopleCount", { count: residents.length })}
            </span>
          </div>
          <div className="relationship-list">
            {residents.length === 0 ? (
              <p className="empty-state">{t("directory.noResidents")}</p>
            ) : (
              residents.map((person) => (
                <article className="relationship-card" key={person.id}>
                  <span className="avatar">
                    {person.name
                      .split(" ")
                      .map((part) => part[0])
                      .slice(0, 2)
                      .join("")}
                  </span>
                  <div>
                    <h3>{person.name}</h3>
                    <span>{t("Resident")}</span>
                  </div>
                  {person.id === snapshot.currentPersonId && (
                    <span className="tag">{t("directory.you")}</span>
                  )}
                </article>
              ))
            )}
          </div>
        </section>
      </div>
    );
  }
  const blocks = [...new Set(snapshot.units.map((unit) => unit.block))].sort();
  const visible = snapshot.units.filter(
    (unit) =>
      (block === "ALL" || unit.block === block) &&
      (scope !== "MINE" || own(unit.id)) &&
      `${unit.label} ${peopleFor(unit.id)
        .map((person) => person.name)
        .join(" ")}`
        .toLocaleLowerCase(locale)
        .includes(query.trim().toLocaleLowerCase(locale)),
  );
  return (
    <div className="resident-directory">
      <div className="unit-context">
        <span className="tag">{t("directory.currentOnly")}</span>
        <p>{t("directory.asOf", { date: date(snapshot.asOf) })}</p>
      </div>
      <section className="unit-summary-grid">
        <div>
          <Building2 size={20} />
          <span>{t("units.sampleUnits")}</span>
          <strong>{snapshot.units.length}</strong>
        </div>
        <div>
          <Users size={20} />
          <span>{t("directory.people")}</span>
          <strong>
            {new Set(snapshot.residences.map((item) => item.personId)).size}
          </strong>
        </div>
        <div>
          <Home size={20} />
          <span>{t("directory.myHome")}</span>
          {snapshot.units
            .filter((unit) => own(unit.id))
            .map((unit) => (
              <Link
                key={unit.id}
                href={`/units/${unit.id}`}
                className="text-button household-shortcut"
              >
                {unit.label}
                <ArrowRight size={16} />
              </Link>
            ))}
        </div>
      </section>
      <section className="panel unit-directory">
        <div className="section-heading">
          <div>
            <h2>{t("directory.title")}</h2>
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
            {t("directory.scope")}
            <select
              value={scope}
              onChange={(event) => update("scope", event.target.value)}
            >
              <option value="ALL">{t("directory.wholeCommunity")}</option>
              <option value="MINE">{t("directory.myUnits")}</option>
            </select>
          </label>
          {(query || block !== "ALL" || scope !== "ALL") && (
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
          <div className="resident-unit-grid">
            {visible.map((unit) => (
              <article className="resident-unit-card" key={unit.id}>
                <div className="resident-unit-heading">
                  <span className="icon-box">
                    <Home size={21} />
                  </span>
                  <div>
                    <h3>{unit.label}</h3>
                    <p>
                      {unit.blockLabel ??
                        t("units.blockName", { block: unit.block })}
                    </p>
                  </div>
                  {own(unit.id) && (
                    <span className="tag">{t("directory.myUnit")}</span>
                  )}
                </div>
                <div className="resident-names">
                  {peopleFor(unit.id).length ? (
                    peopleFor(unit.id).map((person) => (
                      <span key={person.id}>{person.name}</span>
                    ))
                  ) : (
                    <p>{t("directory.noResidents")}</p>
                  )}
                </div>
                <Link
                  className="text-button"
                  href={`/units/${unit.id}${suffix}`}
                >
                  {t("directory.viewResidents")}
                  <ArrowRight size={15} />
                </Link>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
