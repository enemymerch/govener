import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { useTranslation } from "react-i18next";
import { Link, useLocation, useRoute } from "wouter";
import {
  ArrowDownLeft,
  ArrowRight,
  Bell,
  Building2,
  Check,
  ClipboardList,
  LayoutDashboard,
  LogOut,
  Megaphone,
  Plus,
  RefreshCw,
  Wallet,
  X,
} from "lucide-react";
import Finance from "./features/finance/Finance";
import Profile from "./features/profile/Profile";
import Notifications from "./features/notifications/Notifications";
import AuthScreen from "./features/auth/AuthScreen";
import ContextSwitcher from "./workspace/ContextSwitcher";
import { WorkspaceProvider, useWorkspace } from "./workspace/Workspace";
import { useSession } from "./session/Session";
import { repositories } from "./api/repositories";
import { errorKey } from "./api/server";
import LanguageSwitcher from "./LanguageSwitcher";
import Overview from "./features/overview/Overview";
import Account from "./features/accounts/Account";
import Announcements from "./features/announcements/Announcements";
import RequestList from "./features/requests/RequestList";
import RequestRoute from "./features/requests/RequestRoute";
import CreateRequestForm from "./features/requests/CreateRequestForm";
import type { RequestDraft } from "./features/requests/model";
import type { Request } from "./features/requests/model";
import AnnouncementDetail from "./features/announcements/AnnouncementDetail";
import AnnouncementEditor from "./features/announcements/AnnouncementEditor";
import type { AnnouncementDraft } from "./features/announcements/model";
import Payments from "./features/payments/Payments";
import Units from "./features/units/Units";
import { Brand } from "./shared/components";
import Modal from "./shared/Modal";
import { useFormat } from "./shared/useFormat";
import { calendarDateInIstanbul } from "./shared/calendar";
import { pagePaths, resolvePage, type Page } from "./shared/navigation";

export default function App() {
  const { session } = useSession();
  const [path, navigate] = useLocation();
  const auth = ["/login", "/forgot-password", "/reset-password"].includes(path);
  if (!session || auth)
    return (
      <AuthScreen
        key={path}
        kind={
          path === "/forgot-password"
            ? "forgot"
            : path === "/reset-password"
              ? "reset"
              : "login"
        }
        onLogin={(returnTo) =>
          navigate(
            auth
              ? returnTo
              : path === "/"
                ? "/overview"
                : path + window.location.search,
            { replace: true },
          )
        }
      />
    );
  return (
    <WorkspaceProvider key={session.user.id}>
      <WorkspaceApp />
    </WorkspaceProvider>
  );
}
function WorkspaceApp() {
  const { t } = useTranslation();
  const { date } = useFormat();
  const [path, navigate] = useLocation();
  const { session, logout } = useSession();
  const {
    community,
    communityId,
    unitId,
    state,
    isManager,
    can,
    busy,
    refreshError,
    refresh,
    mutate,
  } = useWorkspace();
  const [accountMatch, accountParams] = useRoute("/account/:unitId");
  const [unitMatch, unitParams] = useRoute("/units/:unitId");
  const [requestMatch, requestParams] = useRoute("/requests/:requestId");
  const [noticeMatch, noticeParams] = useRoute(
    "/announcements/:announcementId",
  );
  const [editMatch, editParams] = useRoute(
    "/announcements/:announcementId/edit",
  );
  const page = resolvePage(path === "/" ? "/overview" : path);
  const isNewNotice = path === "/announcements/new";
  const profile = session!.user;
  const initials = profile.name
    .split(" ")
    .map((part) => part[0])
    .slice(0, 2)
    .join("");
  const [modal, setModal] = useState(false);
  const [notificationOpen, setNotificationOpen] = useState(false);
  const [feedback, setFeedback] = useState("");
  const [error, setError] = useState("");
  const heading = useRef<HTMLHeadingElement>(null);
  const closeModal = useCallback(() => {
    if (!busy) setModal(false);
  }, [busy]);
  const requests = state.requests.filter(
    (item) => isManager || !item.unitId || item.unitId === unitId,
  );
  const unread = state.notifications.filter((item) => !item.readAt).length;
  const selectedNotice = state.announcements.find(
    (item) =>
      item.id ===
      (editMatch
        ? editParams.announcementId
        : noticeMatch
          ? noticeParams.announcementId
          : undefined),
  );
  useEffect(() => {
    if (path === "/") navigate("/overview", { replace: true });
    setModal(false);
    setNotificationOpen(false);
    setError("");
    heading.current?.focus({ preventScroll: true });
    window.scrollTo(0, 0);
  }, [path, navigate]);
  const allowed = (value: Page) =>
    value === "Account" || value === "Overview"
      ? can("FINANCE_READ")
      : value === "Finance"
        ? isManager && can("FINANCE_READ")
        : value === "Payments"
          ? !isManager || can("PAYMENT_RECORD")
          : value === "Units"
            ? can("DIRECTORY_READ") || (isManager && can("UNIT_READ"))
            : true;
  const navigation: {
    page: Exclude<Page, "NotFound">;
    label: string;
    icon: ReactNode;
  }[] = [
    {
      page: "Overview",
      label: "Overview",
      icon: <LayoutDashboard size={19} />,
    },
    {
      page: "Account",
      label: isManager ? "Unit accounts" : "My account",
      icon: <Wallet size={19} />,
    },
    { page: "Payments", label: "Payments", icon: <ArrowDownLeft size={19} /> },
    {
      page: "Requests",
      label: "Service requests",
      icon: <ClipboardList size={19} />,
    },
    {
      page: "Announcements",
      label: "Announcements",
      icon: <Megaphone size={19} />,
    },
    {
      page: "Units",
      label: isManager ? "Units" : "directory.title",
      icon: <Building2 size={19} />,
    },
    { page: "Finance", label: "Finance", icon: <Wallet size={19} /> },
    { page: "Profile", label: "Profile", icon: <Building2 size={19} /> },
    { page: "Notifications", label: "Notifications", icon: <Bell size={19} /> },
  ];
  const goTo = (next: Page) => {
    if (next !== "NotFound") navigate(pagePaths[next]);
  };
  async function createRequest(draft: RequestDraft) {
    await mutate(() =>
      repositories.requests.create(communityId, unitId || undefined, draft),
    );
    setModal(false);
    navigate("/requests");
    setFeedback("live.requestCreated");
  }
  async function status(id: string, value: Request["status"]) {
    const request = state.requests.find((item) => item.id === id);
    if (!request || request.version === undefined) return;
    await mutate(() =>
      repositories.requests.changeStatus(
        communityId,
        id,
        value,
        request.version!,
      ),
    );
  }
  async function saveNotice(draft: AnnouncementDraft, id?: string) {
    const existing = state.announcements.find((item) => item.id === id);
    const result = await mutate(() =>
      repositories.announcements.save(
        communityId,
        draft,
        existing ? { id: existing.id, version: existing.version! } : undefined,
      ),
    );
    navigate("/announcements/" + result.id);
    setFeedback(
      draft.status === "DRAFT"
        ? "announcements.draftSaved"
        : "announcements.published",
    );
  }
  async function leave() {
    try {
      await logout();
      navigate("/login");
    } catch (failure) {
      setError(errorKey(failure));
    }
  }
  const title =
    page === "Overview"
      ? t("greeting", { name: profile.name.split(" ")[0] })
      : page === "Account"
        ? t(isManager ? "Unit accounts" : "My account")
        : page === "Requests"
          ? t("Service requests")
          : page === "Announcements"
            ? t("Community announcements")
            : page === "Units" && !isManager
              ? t("directory.title")
              : t(page);
  const routeAllowed = allowed(page);
  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">
        {t("navigation.skip")}
      </a>
      <aside className="sidebar">
        <Brand />
        <div className="workspace">
          <span className="workspace-icon">
            <Building2 size={22} />
          </span>
          <div>
            <strong>{community.name}</strong>
            <small>{community.address}</small>
          </div>
        </div>
        <ContextSwitcher
          isManager={isManager}
          onSwitch={() => {
            navigate("/overview");
            setFeedback("");
          }}
        />
        <span className="nav-caption">{t("WORKSPACE")}</span>
        <nav className="manager-navigation" aria-label={t("navigation.main")}>
          {navigation
            .filter((item) => allowed(item.page))
            .map((item) => (
              <Link
                key={item.page}
                href={pagePaths[item.page]}
                className={page === item.page ? "nav-item active" : "nav-item"}
                aria-current={page === item.page ? "page" : undefined}
              >
                {item.icon}
                {t(item.label)}
                {item.page === "Requests" && (
                  <span className="count">
                    {
                      requests.filter((item) => item.status !== "Resolved")
                        .length
                    }
                  </span>
                )}
              </Link>
            ))}
        </nav>
        <div className="sidebar-bottom">
          <div className="community-note">
            <span className="small-dot" /> {t("A little more connected.")}
            <p>{t("Everything your community needs, in one shared space.")}</p>
          </div>
          <button
            className="profile"
            disabled={busy}
            onClick={() => void leave()}
          >
            <span className="avatar">{initials}</span>
            <span>
              <strong>{profile.name}</strong>
              <small>
                {t(isManager ? "Manager" : "Resident")} · {t("live.signOut")}
              </small>
            </span>
            <LogOut size={17} />
          </button>
        </div>
      </aside>
      <div className="main-shell">
        <header className="topbar">
          <span>
            {t("Workspace")} <span className="breadcrumb">/</span>{" "}
            <strong>{t(page)}</strong>
          </span>
          <div className="topbar-actions">
            <LanguageSwitcher />
            <button
              className="icon-button"
              disabled={busy}
              aria-label={t("live.signOut")}
              onClick={() => void leave()}
            >
              <LogOut size={18} />
            </button>
            <button
              className="icon-button"
              disabled={busy}
              aria-label={t("live.refresh")}
              onClick={() => void refresh()}
            >
              <RefreshCw size={19} />
            </button>
            <div className="notification-wrap">
              <button
                className="icon-button"
                aria-label={t("Notifications")}
                aria-expanded={notificationOpen}
                onClick={() => setNotificationOpen(!notificationOpen)}
              >
                <Bell size={20} />
                {unread > 0 && (
                  <span className="notification-badge">{unread}</span>
                )}
              </button>
              {notificationOpen && (
                <div className="notification-panel">
                  <strong>
                    {t("notifications.unreadCount", { count: unread })}
                  </strong>
                  {state.notifications.slice(0, 3).map((item) => (
                    <p key={item.id}>{t("live.notification." + item.type)}</p>
                  ))}
                  <button
                    className="text-button"
                    onClick={() => goTo("Notifications")}
                  >
                    {t("Notifications")}
                    <ArrowRight size={15} />
                  </button>
                </div>
              )}
            </div>
            <span className="avatar small">{initials}</span>
          </div>
        </header>
        <main className="content" id="main-content">
          <div className="page-heading">
            <div>
              <span className="eyebrow">
                {community.name}
                {!isManager &&
                  " · " +
                    (community.units.find((unit) => unit.id === unitId)
                      ?.label ?? "")}
              </span>
              <h1 ref={heading} tabIndex={-1}>
                {title}
              </h1>
              <p>{t("live.workspaceHelp")}</p>
            </div>
            <div className="heading-action">
              <span className="date">
                {date(calendarDateInIstanbul(new Date()), {
                  dateStyle: "full",
                })}
              </span>
              {(["Overview", "Account"].includes(page) ||
                (page === "Requests" && !requestMatch)) && (
                <button
                  className="primary"
                  disabled={busy || !!refreshError}
                  onClick={() => setModal(true)}
                >
                  <Plus size={17} />
                  {t("New request")}
                </button>
              )}
            </div>
          </div>
          {refreshError && (
            <div className="feedback" role="alert">
              <p>{t("live.refreshFailed")}</p>
              <button className="secondary" onClick={() => void refresh()}>
                {t("live.refresh")}
              </button>
            </div>
          )}
          {busy && <p role="status">{t("live.saving")}</p>}
          {error && (
            <div className="form-error feedback" role="alert">
              {t(error)}
            </div>
          )}
          {feedback && (
            <div className="feedback" role="status">
              <Check size={17} />
              {t(feedback)}
              <button
                aria-label={t("Dismiss confirmation")}
                onClick={() => setFeedback("")}
              >
                <X size={16} />
              </button>
            </div>
          )}
          {!routeAllowed ? (
            <AccessDenied />
          ) : (
            <>
              {page === "Overview" && (
                <Overview isManager={isManager} onNavigate={goTo} />
              )}
              {(page === "Overview" ||
                (page === "Requests" && !requestMatch)) && (
                <RequestList
                  key={page}
                  isManager={can("REQUEST_MANAGE") && isManager}
                  requests={requests}
                  compact={page === "Overview"}
                  onViewAll={() => goTo("Requests")}
                  onStatusChange={(id, next) => {
                    void status(id, next).catch((failure) =>
                      setError(errorKey(failure)),
                    );
                  }}
                />
              )}
              {page === "Requests" && requestMatch && (
                <RequestRoute
                  key={communityId + requestParams.requestId}
                  requestId={requestParams.requestId}
                />
              )}
              {page === "Account" && (
                <Account
                  key={communityId}
                  isManager={isManager}
                  requestedUnitId={
                    accountMatch ? accountParams.unitId : undefined
                  }
                />
              )}
              {page === "Payments" && (
                <Payments
                  key={communityId + unitId}
                  isManager={isManager}
                  notifications={state.paymentNotifications}
                />
              )}
              {(page === "Overview" || path === "/announcements") && (
                <Announcements
                  notices={state.announcements}
                  isManager={isManager && can("ANNOUNCEMENT_WRITE")}
                  compact={page === "Overview"}
                  onSelect={(id) => navigate("/announcements/" + id)}
                />
              )}
              {page === "Announcements" &&
                (isNewNotice || editMatch) &&
                (isManager && can("ANNOUNCEMENT_WRITE") ? (
                  isNewNotice || selectedNotice ? (
                    <AnnouncementEditor
                      key={selectedNotice?.id ?? "new"}
                      targets={community.units}
                      initial={isNewNotice ? undefined : selectedNotice}
                      onSave={saveNotice}
                    />
                  ) : (
                    <AnnouncementDetail notice={undefined} isManager={false} />
                  )
                ) : (
                  <AccessDenied />
                ))}
              {page === "Announcements" && noticeMatch && !isNewNotice && (
                <AnnouncementDetail
                  notice={selectedNotice}
                  isManager={isManager && can("ANNOUNCEMENT_WRITE")}
                />
              )}
              {page === "Units" && (
                <Units
                  key={communityId}
                  isManager={isManager && can("FINANCE_READ")}
                  unitId={unitMatch ? unitParams.unitId : undefined}
                />
              )}
              {page === "Finance" && (
                <Finance key={communityId} isManager={isManager} />
              )}
              {page === "Profile" && <Profile />}
              {page === "Notifications" && <Notifications />}
              {page === "NotFound" && (
                <section className="panel route-state">
                  <h2>{t("NotFound")}</h2>
                  <Link href="/overview" className="primary">
                    {t("navigation.home")}
                  </Link>
                </section>
              )}
            </>
          )}
          <footer>
            yönetici <span>{t("A better everyday, together.")}</span>
          </footer>
        </main>
      </div>
      {modal && (
        <Modal onClose={closeModal}>
          <CreateRequestForm onSubmit={createRequest} />
        </Modal>
      )}
    </div>
  );
}
function AccessDenied() {
  const { t } = useTranslation();
  return (
    <section className="panel route-state">
      <h2>{t("navigation.accessDenied")}</h2>
      <p>{t("live.forbidden")}</p>
    </section>
  );
}
