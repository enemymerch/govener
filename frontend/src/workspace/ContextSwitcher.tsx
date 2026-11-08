import { useTranslation } from "react-i18next";
import { useWorkspace } from "./Workspace";
export default function ContextSwitcher({
  isManager,
  onSwitch,
}: {
  isManager: boolean;
  onSwitch: () => void;
}) {
  const { t } = useTranslation();
  const {
    communityId,
    community,
    communities,
    unitId,
    setCommunityId,
    setUnitId,
    busy,
  } = useWorkspace();
  return (
    <div className="context-switcher">
      <label>
        <span>{t("workspace.community")}</span>
        <select
          disabled={busy}
          value={communityId}
          onChange={(event) => {
            setCommunityId(event.target.value);
            onSwitch();
          }}
        >
          {communities.map((item) => (
            <option key={item.id} value={item.id}>
              {item.name}
            </option>
          ))}
        </select>
      </label>
      {!isManager && (
        <label>
          <span>{t("workspace.myUnit")}</span>
          <select
            disabled={busy}
            value={unitId}
            onChange={(event) => {
              setUnitId(event.target.value);
              onSwitch();
            }}
          >
            {community.units
              .filter((unit) => community.myUnitIds.includes(unit.id))
              .map((unit) => (
                <option key={unit.id} value={unit.id}>
                  {unit.label}
                </option>
              ))}
          </select>
        </label>
      )}
    </div>
  );
}
