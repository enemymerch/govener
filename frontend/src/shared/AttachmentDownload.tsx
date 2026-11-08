import { useTranslation } from "react-i18next";
import { useAttachment, type AttachmentSource } from "./useAttachment";
export default function AttachmentDownload({
  file,
}: {
  file: AttachmentSource;
}) {
  const { t } = useTranslation();
  const { url, failed } = useAttachment(file);
  return url ? (
    <a className="text-button" href={url} download={file.name}>
      {t("finance.download")} · {file.name}
    </a>
  ) : (
    <span className="demo-note">
      {t(failed ? "api.attachmentError" : "api.attachmentLoading")}
    </span>
  );
}
