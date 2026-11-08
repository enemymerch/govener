import { useWorkspace } from "../../workspace/Workspace";
import { useTranslation } from "react-i18next";
import {
  ArrowDownLeft,
  ArrowRight,
  Building2,
  ClipboardList,
  Wallet,
} from "lucide-react";
import { Stat } from "../../shared/components";
import { useFormat } from "../../shared/useFormat";
import type { Page } from "../../shared/navigation";
export default function Overview({
  isManager,
  onNavigate,
}: {
  isManager: boolean;
  onNavigate: (page: Page) => void;
}) {
  const { t } = useTranslation();
  const { moneyMinor, date, period, percent } = useFormat();
  const { state, community, unitId } = useWorkspace();
  const dashboard = state.dashboard;
  if (!dashboard)
    return (
      <section className="panel route-state">
        <p>{t("live.forbidden")}</p>
      </section>
    );
  const ratio = dashboard.targetMinor
    ? Math.min(1, dashboard.collectedMinor / dashboard.targetMinor)
    : 0;
  return (
    <>
      <section className="stats">
        <Stat
          label={t("Collected this month")}
          value={moneyMinor(dashboard.collectedMinor)}
          detail={period(dashboard.period)}
          icon={<ArrowDownLeft size={20} />}
        />
        <Stat
          label={t(
            isManager ? "Outstanding charges" : "My outstanding charges",
          )}
          value={moneyMinor(dashboard.outstandingMinor)}
          detail={
            dashboard.nextDueOn
              ? t("nextDue", { date: date(dashboard.nextDueOn) })
              : t("live.noDueDate")
          }
          icon={<Wallet size={20} />}
        />
        <Stat
          label={t("Open requests")}
          value={String(dashboard.openRequestCount)}
          detail={t("A little attention goes a long way")}
          icon={<ClipboardList size={20} />}
        />
        <Stat
          label={t(isManager ? "Community units" : "My unit")}
          value={
            isManager
              ? String(dashboard.unitCount)
              : (community.units.find((unit) => unit.id === unitId)?.label ??
                "")
          }
          detail={community.name}
          icon={<Building2 size={20} />}
        />
      </section>
      <div className="overview-grid">
        <section className="panel">
          <div className="section-heading">
            <div>
              <h2>
                {t(
                  isManager
                    ? "Collection overview"
                    : "Your account at a glance",
                )}
              </h2>
              <p>{period(dashboard.period)}</p>
            </div>
            <span className="tag">TRY</span>
          </div>
          {isManager ? (
            <>
              <div className="collection-number">
                {moneyMinor(dashboard.collectedMinor)}{" "}
                <span>
                  {t("collectedOf", {
                    amount: moneyMinor(dashboard.targetMinor),
                  })}
                </span>
              </div>
              <div
                className="progress"
                role="progressbar"
                aria-label={t("Monthly collection progress")}
                aria-valuenow={Math.round(ratio * 100)}
                aria-valuemin={0}
                aria-valuemax={100}
              >
                <span style={{ width: `${ratio * 100}%` }} />
              </div>
              <div className="progress-legend">
                <span>
                  <i />
                  {t("Collected")} <strong>{percent(ratio)}</strong>
                </span>
                <span>
                  <i />
                  {t("Outstanding")}{" "}
                  <strong>
                    {percent(dashboard.targetMinor ? 1 - ratio : 0)}
                  </strong>
                </span>
              </div>
              <div className="collection-footer">
                <span>
                  {t("paidUnits", {
                    paid: dashboard.paidUnitCount,
                    total: dashboard.unitCount,
                  })}
                </span>
                <button
                  className="text-button"
                  onClick={() => onNavigate("Account")}
                >
                  {t("View accounts")}
                  <ArrowRight size={15} />
                </button>
              </div>
            </>
          ) : (
            <>
              <div className="collection-number">
                {moneyMinor(
                  dashboard.ownAccount?.outstandingMinor ??
                    dashboard.outstandingMinor,
                )}{" "}
                <span>{t("outstanding")}</span>
              </div>
              <div className="collection-footer">
                <span>{t("Payments are made by bank transfer.")}</span>
                <button
                  className="text-button"
                  onClick={() => onNavigate("Account")}
                >
                  {t("View my account")}
                  <ArrowRight size={15} />
                </button>
              </div>
            </>
          )}
        </section>
        <section className="community-card">
          <span className="eyebrow">{t("OUR SHARED SPACE")}</span>
          <h2>
            {t("Home is more than")}
            <br />
            {t("your front door.")}
          </h2>
          <p>{t("Stay in the loop with the people and places around you.")}</p>
          <button onClick={() => onNavigate("Announcements")}>
            {t("Community updates")}
            <ArrowRight size={16} />
          </button>
          <Building2 className="card-building" size={130} strokeWidth={0.7} />
        </section>
      </div>
    </>
  );
}
