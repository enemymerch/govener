import { useState, type FormEvent } from "react";
import { useWorkspace } from "../../workspace/Workspace";
import { errorKey } from "../../api/server";
import { useTranslation } from "react-i18next";
import { Link } from "wouter";
import { ArrowLeft, Check, Send } from "lucide-react";
import {
  validAudience,
  type Announcement,
  type AnnouncementDraft,
  type Audience,
} from "./model";

type Props = {
  targets: { id: string; label: string; block: string; blockLabel?: string }[];
  initial?: Announcement;
  onSave: (draft: AnnouncementDraft, id?: string) => Promise<void>;
};
export default function AnnouncementEditor({
  initial,
  onSave,
  targets,
}: Props) {
  const { t } = useTranslation();
  const { busy, refreshError } = useWorkspace();
  const [title, setTitle] = useState(
    initial ? (initial.custom ? initial.title : t(initial.title)) : "",
  );
  const [body, setBody] = useState(
    initial ? (initial.custom ? initial.body : t(initial.body)) : "",
  );
  const [category, setCategory] = useState<Announcement["category"]>(
    initial?.category ?? "COMMUNITY",
  );
  const [audienceType, setAudienceType] = useState<Audience["type"]>(
    initial?.audience.type ?? "ALL",
  );
  const [selectedIds, setSelectedIds] = useState<string[]>(
    initial?.audience.type === "ALL" ? [] : (initial?.audience.ids ?? []),
  );
  const [error, setError] = useState("");
  function toggle(id: string) {
    setSelectedIds((previous) =>
      previous.includes(id)
        ? previous.filter((item) => item !== id)
        : [...previous, id],
    );
    setError("");
  }
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const audience: Audience =
      audienceType === "ALL"
        ? { type: "ALL" }
        : { type: audienceType, ids: selectedIds };
    if (!title.trim() || !body.trim()) {
      setError("announcements.required");
      return;
    }
    if (!validAudience(audience, targets)) {
      setError("announcements.selectAudience");
      return;
    }
    const submitter = (event.nativeEvent as SubmitEvent).submitter;
    const status =
      submitter instanceof HTMLButtonElement && submitter.value === "PUBLISHED"
        ? "PUBLISHED"
        : "DRAFT";
    try {
      await onSave(
        { title: title.trim(), body: body.trim(), category, audience, status },
        initial?.id,
      );
    } catch (failure) {
      setError(errorKey(failure));
    }
  }
  return (
    <div className="announcement-editor">
      <Link
        href={initial ? `/announcements/${initial.id}` : "/announcements"}
        className="text-button unit-back"
      >
        <ArrowLeft size={16} />
        {t("announcements.back")}
      </Link>
      <section className="panel">
        <div className="section-heading">
          <div>
            <h2>{t(initial ? "announcements.edit" : "announcements.new")}</h2>
            <p>{t("announcements.editorHelp")}</p>
          </div>
        </div>
        <form className="announcement-form" onSubmit={submit}>
          <label>
            {t("announcements.title")}
            <input
              autoFocus
              required
              maxLength={160}
              value={title}
              onChange={(event) => setTitle(event.target.value)}
            />
          </label>
          <label>
            {t("announcements.body")}
            <textarea
              required
              rows={8}
              maxLength={10000}
              value={body}
              onChange={(event) => setBody(event.target.value)}
            />
          </label>
          <div className="announcement-form-row">
            <label>
              {t("announcements.category")}
              <select
                value={category}
                onChange={(event) =>
                  setCategory(event.target.value as Announcement["category"])
                }
              >
                <option value="COMMUNITY">{t("COMMUNITY")}</option>
                <option value="MAINTENANCE">{t("MAINTENANCE")}</option>
              </select>
            </label>
            <label>
              {t("announcements.audience")}
              <select
                value={audienceType}
                onChange={(event) => {
                  setAudienceType(event.target.value as Audience["type"]);
                  setSelectedIds([]);
                  setError("");
                }}
              >
                <option value="ALL">{t("announcements.allCommunity")}</option>
                <option value="BLOCKS">
                  {t("announcements.selectedBlocks")}
                </option>
                <option value="UNITS">
                  {t("announcements.selectedUnits")}
                </option>
              </select>
            </label>
          </div>
          {audienceType !== "ALL" && (
            <fieldset className="audience-picker">
              <legend>
                {t(
                  audienceType === "BLOCKS"
                    ? "announcements.chooseBlocks"
                    : "announcements.chooseUnits",
                )}
              </legend>
              {(audienceType === "BLOCKS"
                ? [...new Set(targets.map((unit) => unit.block))].map(
                    (block) => ({
                      id: block,
                      label:
                        targets.find((unit) => unit.block === block)
                          ?.blockLabel ?? t("units.blockName", { block }),
                    }),
                  )
                : targets
              ).map((item) => (
                <label key={item.id}>
                  <input
                    type="checkbox"
                    checked={selectedIds.includes(item.id)}
                    onChange={() => toggle(item.id)}
                  />
                  {item.label}
                </label>
              ))}
            </fieldset>
          )}
          {error && (
            <p className="form-error" role="alert">
              {t(error)}
            </p>
          )}
          <p className="demo-note">{t("live.serverSavedHelp")}</p>
          <div className="form-actions">
            <button
              className="secondary"
              disabled={busy || !!refreshError}
              type="submit"
              value="DRAFT"
            >
              <Check size={15} />
              {t("announcements.saveDraft")}
            </button>
            <button
              className="primary"
              disabled={busy || !!refreshError}
              type="submit"
              value="PUBLISHED"
            >
              <Send size={15} />
              {t(
                initial?.status === "PUBLISHED"
                  ? "announcements.savePublished"
                  : "announcements.publish",
              )}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}
