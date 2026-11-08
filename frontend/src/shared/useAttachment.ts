import { useEffect, useState } from "react";
import { api } from "../api";
import { useWorkspace } from "../workspace/Workspace";
export type RemoteAttachment = { id: string; name: string; mediaType: string };
export type AttachmentSource = File | RemoteAttachment;
export function useAttachment(source: AttachmentSource) {
  const { communityId } = useWorkspace();
  const [url, setUrl] = useState("");
  const [failed, setFailed] = useState(false);
  useEffect(() => {
    const controller = new AbortController();
    let objectUrl = "";
    setUrl("");
    setFailed(false);
    const load =
      source instanceof File
        ? Promise.resolve(source)
        : api.execute("downloadAttachment", {
            params: { communityId, attachmentId: source.id },
            signal: controller.signal,
          });
    load
      .then((blob) => {
        if (controller.signal.aborted) return;
        objectUrl = URL.createObjectURL(blob);
        setUrl(objectUrl);
      })
      .catch(() => {
        if (!controller.signal.aborted) setFailed(true);
      });
    return () => {
      controller.abort();
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [source, communityId]);
  return { url, failed };
}
