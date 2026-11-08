import { useTranslation } from "react-i18next";
import {
  useAttachment,
  type AttachmentSource,
} from "../../shared/useAttachment";
export default function RequestPhoto({ file }: { file: AttachmentSource }) {
  const { t } = useTranslation();
  const { url, failed } = useAttachment(file);
  return (
    <figure className="request-photo">
      {url ? (
        <a href={url} download={file.name}>
          <img src={url} alt={file.name} />
          <figcaption>
            {t("requests.downloadPhoto")} · {file.name}
          </figcaption>
        </a>
      ) : (
        <figcaption>
          {t(failed ? "api.attachmentError" : "api.attachmentLoading")}
        </figcaption>
      )}
    </figure>
  );
}
