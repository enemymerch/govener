import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { api } from "../../api";
import { all, errorKey } from "../../api/server";
import type { Comment, RequestEvent } from "../../api/generated";
import { requestFromWire } from "../../api/adapters";
import { repositories } from "../../api/repositories";
import { useWorkspace } from "../../workspace/Workspace";
import RequestDetail from "./RequestDetail";
import type { Request } from "./model";
export default function RequestRoute({ requestId }: { requestId: string }) {
  const { t } = useTranslation();
  const { communityId, state, isManager, can, mutate } = useWorkspace();
  const [request, setRequest] = useState<Request>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    Promise.all([
      api.execute("getRequest", {
        params: { communityId, requestId },
        signal: controller.signal,
      }),
      all<"listRequestComments", Comment>(
        "listRequestComments",
        communityId,
        controller.signal,
        {},
        { requestId },
      ),
      all<"listRequestEvents", RequestEvent>(
        "listRequestEvents",
        communityId,
        controller.signal,
        {},
        { requestId },
      ),
    ])
      .then(([item, comments, history]) => {
        if (!controller.signal.aborted)
          setRequest(requestFromWire({ ...item, comments, history }));
      })
      .catch((failure) => {
        if (!controller.signal.aborted) setError(errorKey(failure));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [communityId, requestId, state.requests, attempt]);
  if (loading)
    return (
      <section className="panel route-state" role="status">
        {t("live.loading")}
      </section>
    );
  if (error)
    return (
      <section className="panel route-state" role="alert">
        <p>{t(error)}</p>
        <button
          className="primary"
          onClick={() => setAttempt((value) => value + 1)}
        >
          {t("units.retry")}
        </button>
      </section>
    );
  return (
    <RequestDetail
      request={request}
      isManager={isManager && can("REQUEST_MANAGE")}
      staff={state.staff}
      onStatusChange={async (id, status) => {
        await mutate(() =>
          repositories.requests.changeStatus(
            communityId,
            id,
            status,
            request!.version!,
          ),
        );
      }}
      onAssign={async (id, assigneeId) => {
        await mutate(() =>
          repositories.requests.assign(
            communityId,
            id,
            assigneeId,
            request!.version!,
          ),
        );
      }}
      onComment={async (id, body) => {
        await mutate(() =>
          repositories.requests.comment(communityId, id, body),
        );
      }}
    />
  );
}
