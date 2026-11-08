import { useWorkspace } from "../../workspace/Workspace";
import { useTranslation } from "react-i18next";
import type { Audience } from "./model";

export default function AudienceLabel({ audience }: { audience: Audience }) {
  const { t } = useTranslation();
  const { community } = useWorkspace();
  const label =
    audience.type === "ALL"
      ? t("announcements.allCommunity")
      : audience.type === "BLOCKS"
        ? audience.ids
            .map(
              (block) =>
                community.units.find((unit) => unit.block === block)
                  ?.blockLabel ?? t("units.blockName", { block }),
            )
            .join(", ")
        : audience.ids
            .map(
              (id) =>
                community.units.find((unit) => unit.id === id)?.label ?? id,
            )
            .join(", ");
  return <span>{label}</span>;
}
