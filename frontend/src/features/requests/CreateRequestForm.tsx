import { useState, type FormEvent } from "react";
import { useWorkspace } from "../../workspace/Workspace";
import { errorKey } from "../../api/server";
import { useTranslation } from "react-i18next";
import { ArrowRight } from "lucide-react";
import {
  maxRequestPhotos,
  validRequestPhotos,
  type RequestDraft,
} from "./model";
import RequestPhoto from "./RequestPhoto";

export default function CreateRequestForm({
  onSubmit,
}: {
  onSubmit: (draft: RequestDraft) => Promise<void>;
}) {
  const { t } = useTranslation();
  const { busy, refreshError } = useWorkspace();
  const [photos, setPhotos] = useState<File[]>([]);
  const [error, setError] = useState("");
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const title = String(data.get("title") ?? "").trim();
    const location = String(data.get("location") ?? "").trim();
    const description = String(data.get("description") ?? "").trim();
    if (!title || !location) {
      setError("requests.required");
      return;
    }
    if (!validRequestPhotos(photos)) {
      setError("requests.photoError");
      return;
    }
    try {
      await onSubmit({ title, location, description, photos });
    } catch (failure) {
      setError(errorKey(failure));
    }
  }
  return (
    <>
      <span className="eyebrow">{t("A LITTLE HELP AT HOME")}</span>
      <h2 id="dialog-title">{t("Create a service request")}</h2>
      <p>{t("Let your community manager know what needs attention.")}</p>
      <form onSubmit={submit}>
        <label>
          {t("Request title")}
          <input
            autoFocus
            name="title"
            required
            maxLength={120}
            placeholder={t("e.g. Hallway light is not working")}
          />
        </label>
        <label>
          {t("Location")}
          <input
            name="location"
            required
            maxLength={100}
            placeholder={t("e.g. Block A · Third floor")}
          />
        </label>
        <label>
          {t("requests.descriptionOptional")}
          <textarea name="description" rows={4} maxLength={2000} />
        </label>
        <label>
          {t("requests.photos")}
          <input
            type="file"
            accept="image/png,image/jpeg,image/webp"
            multiple
            onChange={(event) => {
              const files = Array.from(event.target.files ?? []);
              setPhotos(files);
              setError(validRequestPhotos(files) ? "" : "requests.photoError");
            }}
          />
          <small>{t("requests.photoHelp", { count: maxRequestPhotos })}</small>
        </label>
        {validRequestPhotos(photos) && photos.length > 0 && (
          <div className="request-photo-grid">
            {photos.map((file, index) => (
              <RequestPhoto key={`${file.name}-${index}`} file={file} />
            ))}
          </div>
        )}
        {error && (
          <p className="form-error" role="alert">
            {t(error)}
          </p>
        )}
        <p className="demo-note">{t("live.serverSavedHelp")}</p>
        <button
          className="primary"
          disabled={busy || !!refreshError}
          type="submit"
        >
          {t("Submit request")}
          <ArrowRight size={16} />
        </button>
      </form>
    </>
  );
}
