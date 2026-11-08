import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { useTranslation } from "react-i18next";
import { api } from "../api";
import { all, errorKey } from "../api/server";
import {
  announcementFromWire,
  attachmentFromWire,
  paymentNotificationFromWire,
  requestFromWire,
} from "../api/adapters";
import type * as Wire from "../api/generated";
import type { FinanceState } from "../features/finance/model";
import type { Request } from "../features/requests/model";
import type { Announcement } from "../features/announcements/model";
import type { PaymentNotification } from "../features/payments/model";
import { useSession } from "../session/Session";

export type Community = Wire.Community & {
  units: (Wire.Unit & { block: string })[];
  myUnitIds: string[];
};
export type CommunityState = {
  requests: Request[];
  announcements: Announcement[];
  paymentNotifications: PaymentNotification[];
  finance: FinanceState;
  notifications: Wire.Notification[];
  unitAccounts: Wire.UnitAccount[];
  directory: Wire.DirectoryUnit[];
  staff: Wire.DirectoryPerson[];
  dashboard?: Wire.Dashboard;
};
type Snapshot = {
  community: Community;
  state: CommunityState;
  context: Wire.CommunityContext;
  unitId: string;
};
type Value = Snapshot & {
  communityId: string;
  communities: Wire.Community[];
  isManager: boolean;
  can: (permission: string) => boolean;
  setCommunityId: (id: string) => void;
  setUnitId: (id: string) => void;
  refresh: () => Promise<void>;
  mutate: <T>(work: () => Promise<T>) => Promise<T>;
  busy: boolean;
  refreshError: string;
};
const Context = createContext<Value | null>(null);
function saved(userId: string): { communityId?: string; unitId?: string } {
  try {
    return JSON.parse(
      sessionStorage.getItem("govener.context." + userId) ?? "{}",
    );
  } catch {
    return {};
  }
}
function persist(userId: string, communityId: string, unitId: string) {
  try {
    sessionStorage.setItem(
      "govener.context." + userId,
      JSON.stringify({ communityId, unitId }),
    );
  } catch {
    /* Optional selection persistence. */
  }
}

async function loadSnapshot(
  userId: string,
  communityId: string,
  requestedUnit: string | undefined,
  signal: AbortSignal,
): Promise<Snapshot> {
  const context = await api.execute("getContext", {
    params: { communityId },
    signal,
  });
  const can = (permission: string) => context.permissions.includes(permission);
  const manager = context.roles.includes("MANAGER");
  const units = can("UNIT_READ")
    ? await all<"listUnits", Wire.Unit>("listUnits", communityId, signal)
    : context.ownUnits;
  const unitId =
    context.ownUnits.find((unit) => unit.id === requestedUnit)?.id ??
    context.ownUnits[0]?.id ??
    "";
  const [
    requests,
    notices,
    notifications,
    directory,
    staff,
    charges,
    payments,
    accounts,
    summaries,
    expenses,
    settlements,
    paymentNotifications,
    dashboard,
  ] = await Promise.all([
    all<"listRequests", Wire.Request>("listRequests", communityId, signal),
    all<"listAnnouncements", Wire.Announcement>(
      "listAnnouncements",
      communityId,
      signal,
    ),
    all<"listNotifications", Wire.Notification>(
      "listNotifications",
      communityId,
      signal,
    ),
    can("DIRECTORY_READ")
      ? all<"listDirectory", Wire.DirectoryUnit>(
          "listDirectory",
          communityId,
          signal,
        )
      : [],
    manager && can("REQUEST_MANAGE")
      ? all<"listAssignableStaff", Wire.DirectoryPerson>(
          "listAssignableStaff",
          communityId,
          signal,
        )
      : [],
    can("FINANCE_READ")
      ? all<"listCharges", Wire.Charge>("listCharges", communityId, signal)
      : [],
    can("FINANCE_READ")
      ? all<"listPayments", Wire.Payment>("listPayments", communityId, signal)
      : [],
    manager && can("FINANCE_READ")
      ? all<"listFinancialAccounts", Wire.FinancialAccount>(
          "listFinancialAccounts",
          communityId,
          signal,
        )
      : [],
    can("FINANCE_READ")
      ? all<"listUnitAccounts", Wire.UnitAccount>(
          "listUnitAccounts",
          communityId,
          signal,
        )
      : [],
    manager && can("FINANCE_READ")
      ? all<"listExpenses", Wire.Expense>("listExpenses", communityId, signal)
      : [],
    manager && can("FINANCE_READ")
      ? all<"listExpensePayments", Wire.ExpensePayment>(
          "listExpensePayments",
          communityId,
          signal,
        )
      : [],
    !manager || can("PAYMENT_RECORD")
      ? all<"listPaymentNotifications", Wire.PaymentNotification>(
          "listPaymentNotifications",
          communityId,
          signal,
        )
      : [],
    can("FINANCE_READ")
      ? api.execute("getDashboard", {
          params: { communityId },
          query: { unitId: manager ? undefined : unitId || undefined },
          signal,
        })
      : undefined,
  ]);
  if (signal.aborted) throw new DOMException("Aborted", "AbortError");
  persist(userId, communityId, unitId);
  return {
    context,
    community: {
      ...context.community,
      units: units.map((unit) => ({ ...unit, block: unit.blockId })),
      myUnitIds: context.ownUnits.map((unit) => unit.id),
    },
    unitId,
    state: {
      requests: requests.map(requestFromWire),
      announcements: notices.map(announcementFromWire),
      notifications,
      directory,
      staff,
      dashboard,
      paymentNotifications: paymentNotifications.map(
        paymentNotificationFromWire,
      ),
      unitAccounts: summaries,
      finance: {
        charges: charges.map((item) => ({
          ...item,
          custom: true,
          period: item.periodStart,
          cancelledAt: item.cancelledAt ?? undefined,
          reason: item.cancellationReason,
        })),
        payments: payments.map((item) => ({
          ...item,
          notificationId: item.notificationId ?? undefined,
          reversedAt: item.reversedAt ?? undefined,
          reason: item.reversalReason,
        })),
        accounts,
        expenses: expenses.map((item) => ({
          ...item,
          document: item.document
            ? attachmentFromWire(item.document)
            : undefined,
        })),
        expensePayments: settlements.map((item) => ({
          ...item,
          reversedAt: item.status === "REVERSED" ? "REVERSED" : undefined,
          reason: item.reversalReason,
        })),
      },
    },
  };
}

export function WorkspaceProvider({ children }: { children: ReactNode }) {
  const { session } = useSession();
  const { t } = useTranslation();
  const userId = session!.user.id;
  const initial = useRef(saved(userId));
  const [communities, setCommunities] = useState<Wire.Community[]>([]);
  const [communityId, changeCommunity] = useState(
    initial.current.communityId ?? "",
  );
  const [unitId, changeUnit] = useState(initial.current.unitId ?? "");
  const [snapshot, setSnapshot] = useState<Snapshot | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [refreshError, setRefreshError] = useState("");
  const [attempt, setAttempt] = useState(0);
  const [busy, setBusy] = useState(false);
  const pending = useRef(false);
  const currentLoad = useRef<AbortController | null>(null);
  const generation = useRef(0);
  const listRef = useRef<Wire.Community[]>([]);
  useEffect(() => {
    const controller = new AbortController();
    currentLoad.current?.abort();
    currentLoad.current = controller;
    const version = ++generation.current;
    setLoading(true);
    setError("");
    setSnapshot(null);
    setRefreshError("");
    (async () => {
      const list = await all<"listCommunities", Wire.Community>(
        "listCommunities",
        undefined,
        controller.signal,
      );
      const selected =
        list.find((item) => item.id === communityId)?.id ?? list[0]?.id;
      if (!selected) {
        if (!controller.signal.aborted) {
          listRef.current = [];
          setCommunities([]);
          setLoading(false);
        }
        return;
      }
      const next = await loadSnapshot(
        userId,
        selected,
        unitId,
        controller.signal,
      );
      if (!controller.signal.aborted && generation.current === version) {
        listRef.current = list;
        setCommunities(list);
        setSnapshot(next);
      }
    })()
      .catch((failure) => {
        if (!controller.signal.aborted) setError(errorKey(failure));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => {
      controller.abort();
      generation.current++;
    };
  }, [userId, communityId, unitId, attempt]);
  const refresh = useCallback(async () => {
    if (!snapshot) return;
    const version = generation.current;
    const controller = new AbortController();
    currentLoad.current?.abort();
    currentLoad.current = controller;
    try {
      const next = await loadSnapshot(
        userId,
        snapshot.community.id,
        snapshot.unitId,
        controller.signal,
      );
      if (version === generation.current && !controller.signal.aborted) {
        setSnapshot(next);
        setRefreshError("");
      }
    } catch (failure) {
      if (!controller.signal.aborted && version === generation.current)
        setRefreshError(errorKey(failure));
    }
  }, [snapshot, userId]);
  async function mutate<T>(work: () => Promise<T>): Promise<T> {
    if (pending.current || refreshError)
      throw new Error("Workspace is busy or needs refreshing");
    pending.current = true;
    setBusy(true);
    const version = generation.current;
    try {
      const result = await work();
      if (generation.current === version) await refresh();
      return result;
    } catch (failure) {
      if (
        (failure as { status?: number }).status === 409 &&
        generation.current === version
      )
        await refresh();
      throw failure;
    } finally {
      pending.current = false;
      setBusy(false);
    }
  }
  if (loading)
    return (
      <main className="auth-page">
        <p role="status">{t("live.loadingWorkspace")}</p>
      </main>
    );
  if (error)
    return (
      <main className="auth-page">
        <section className="panel route-state" role="alert">
          <p>{t(error)}</p>
          <button
            className="primary"
            onClick={() => setAttempt((value) => value + 1)}
          >
            {t("units.retry")}
          </button>
        </section>
      </main>
    );
  if (!snapshot) return <EmptyMembership />;
  const can = (permission: string) =>
    snapshot.context.permissions.includes(permission);
  return (
    <Context.Provider
      value={{
        ...snapshot,
        communityId: snapshot.community.id,
        communities,
        isManager: snapshot.context.roles.includes("MANAGER"),
        can,
        busy,
        refreshError,
        refresh,
        mutate,
        setCommunityId: (id) => {
          if (
            !pending.current &&
            listRef.current.some((item) => item.id === id)
          ) {
            changeCommunity(id);
            changeUnit("");
          }
        },
        setUnitId: (id) => {
          if (!pending.current && snapshot.community.myUnitIds.includes(id)) {
            changeCommunity(snapshot.community.id);
            changeUnit(id);
          }
        },
      }}
    >
      {children}
    </Context.Provider>
  );
}
function EmptyMembership() {
  const { t } = useTranslation();
  const { logout } = useSession();
  const [error, setError] = useState("");
  return (
    <main className="auth-page">
      <section className="panel route-state">
        <h1>{t("live.noMembership")}</h1>
        <p>{t("live.noMembershipHelp")}</p>
        {error && <p role="alert">{t(error)}</p>}
        <button
          className="primary"
          onClick={() =>
            void logout().catch((error) => setError(errorKey(error)))
          }
        >
          {t("live.signOut")}
        </button>
      </section>
    </main>
  );
}
export function useWorkspace() {
  const value = useContext(Context);
  if (!value) throw new Error("WorkspaceProvider is required");
  return value;
}
