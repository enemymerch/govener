import { Link } from "wouter";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { ArrowRight, Search } from "lucide-react";
import { useFormat } from "../../shared/useFormat";
import { useWorkspace } from "../../workspace/Workspace";
import type { Request } from "./model";
type Props = {
  isManager: boolean;
  requests: Request[];
  compact?: boolean;
  onViewAll: () => void;
  onStatusChange: (id: string, status: Request["status"]) => void;
};
export default function RequestList({
  isManager,
  requests,
  compact = false,
  onViewAll,
  onStatusChange,
}: Props) {
  const { t } = useTranslation();
  const { busy, refreshError } = useWorkspace();
  const { locale, date } = useFormat();
  const [query, setQuery] = useState("");
  const page = compact ? "Overview" : "Requests";
  const displayRequest = (request: Request, field: "title" | "location") =>
    request.custom ? request[field] : t(request[field]);
  const filteredRequests = requests.filter((request) =>
    `${displayRequest(request, "title")} ${displayRequest(request, "location")}`
      .toLocaleLowerCase(locale)
      .includes(query.toLocaleLowerCase(locale)),
  );
  return (
    <section className="panel requests-panel">
      <div className="section-heading">
        <div>
          <h2>
            {page === "Overview" ? t("Recent requests") : t("All requests")}
          </h2>
          <p>
            {isManager
              ? t("The things that need a helping hand.")
              : t("Follow the progress of your requests.")}
          </p>
        </div>
        {page === "Overview" ? (
          <button className="text-button" onClick={() => onViewAll()}>
            {t("View all")} <ArrowRight size={15} />
          </button>
        ) : (
          <label className="search">
            <Search size={16} />
            <input
              aria-label={t("Search requests")}
              placeholder={t("Search requests")}
              value={query}
              onChange={(event) => setQuery(event.target.value)}
            />
          </label>
        )}
      </div>
      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              <th>{t("REQUEST")}</th>
              <th>{t("LOCATION")}</th>
              <th>{t("DATE")}</th>
              <th>{t("STATUS")}</th>
              {isManager && page === "Requests" && <th>{t("ACTION")}</th>}
            </tr>
          </thead>
          <tbody>
            {filteredRequests.map((request) => (
              <tr key={request.id}>
                <td>
                  <Link
                    href={`/requests/${request.id}`}
                    className="request-title-link"
                  >
                    <strong>{displayRequest(request, "title")}</strong>
                  </Link>
                  <small>REQ-{request.displayNumber ?? request.id}</small>
                </td>
                <td>{displayRequest(request, "location")}</td>
                <td>{date(request.date)}</td>
                <td>
                  <span
                    className={`status ${request.status.toLowerCase().replace(" ", "-")}`}
                  >
                    <i />
                    {t(request.status)}
                  </span>
                </td>
                {isManager && page === "Requests" && (
                  <td>
                    <select
                      disabled={busy || !!refreshError}
                      aria-label={t("statusFor", {
                        title: displayRequest(request, "title"),
                      })}
                      value={request.status}
                      onChange={(event) =>
                        onStatusChange(
                          request.id,
                          event.target.value as Request["status"],
                        )
                      }
                    >
                      <option value="Open">{t("Open")}</option>
                      <option value="In progress">{t("In progress")}</option>
                      <option value="Resolved">{t("Resolved")}</option>
                    </select>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
        {filteredRequests.length === 0 && (
          <p className="empty-state">
            {t("No requests found. Try another search or create a request.")}
          </p>
        )}
      </div>
    </section>
  );
}
