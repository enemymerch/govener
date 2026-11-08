import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { api } from "../api";
import { repositories } from "../api/repositories";
import { errorKey } from "../api/server";
import type { Profile, Session } from "../api/generated";
import { useTranslation } from "react-i18next";
type Value = {
  session: Session | null;
  expired: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  updateUser: (user: Profile) => void;
};
const Context = createContext<Value | null>(null);
export function SessionProvider({ children }: { children: ReactNode }) {
  const { t } = useTranslation();
  const [session, setSession] = useState<Session | null>(null);
  const current = useRef<Session | null>(null);
  const epoch = useRef(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [attempt, setAttempt] = useState(0);
  const [expired, setExpired] = useState(false);
  const put = useCallback((value: Session | null) => {
    current.current = value;
    setSession(value);
  }, []);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    api
      .execute("getSession", { signal: controller.signal })
      .then((value) => {
        if (!controller.signal.aborted) put(value);
      })
      .catch((failure) => {
        if (!controller.signal.aborted) {
          if (failure.status === 401) put(null);
          else setError(errorKey(failure));
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [attempt, put]);
  useEffect(() => {
    const unauthorized = () => {
      if (current.current) setExpired(true);
      epoch.current++;
      api.clearSession();
      put(null);
    };
    window.addEventListener("govener:unauthorized", unauthorized);
    const resume = () => {
      if (document.visibilityState !== "visible" || !current.current) return;
      const version = epoch.current;
      void repositories.auth
        .session()
        .then((value) => {
          if (epoch.current === version && current.current) put(value);
        })
        .catch((failure) => {
          if (epoch.current === version && failure.status !== 401)
            setError(errorKey(failure));
        });
    };
    document.addEventListener("visibilitychange", resume);
    return () => {
      window.removeEventListener("govener:unauthorized", unauthorized);
      document.removeEventListener("visibilitychange", resume);
    };
  }, [put]);
  async function login(email: string, password: string) {
    await repositories.auth.csrf();
    const value = await repositories.auth.login(email, password);
    epoch.current++;
    setExpired(false);
    setError("");
    put(value);
  }
  async function logout() {
    await repositories.auth.logout();
    epoch.current++;
    setExpired(false);
    put(null);
  }
  if (loading)
    return (
      <main className="auth-page">
        <p role="status">{t("live.loadingSession")}</p>
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
  return (
    <Context.Provider
      value={{
        session,
        expired,
        login,
        logout,
        updateUser: (user) => {
          if (current.current) put({ ...current.current, user });
        },
      }}
    >
      {children}
    </Context.Provider>
  );
}
export function useSession() {
  const value = useContext(Context);
  if (!value) throw new Error("SessionProvider is required");
  return value;
}
