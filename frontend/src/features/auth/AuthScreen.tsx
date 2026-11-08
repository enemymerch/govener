import { useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { Link, useSearchParams } from "wouter";
import LanguageSwitcher from "../../LanguageSwitcher";
import { Brand } from "../../shared/components";
import { useSession } from "../../session/Session";
import { repositories } from "../../api/repositories";
import { errorKey } from "../../api/server";
export default function AuthScreen({
  kind,
  onLogin,
}: {
  kind: "login" | "forgot" | "reset";
  onLogin: (returnTo: string) => void;
}) {
  const { t } = useTranslation();
  const { login, expired } = useSession();
  const [params] = useSearchParams();
  const [error, setError] = useState("");
  const [sent, setSent] = useState(false);
  const [pending, setPending] = useState(false);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (pending) return;
    const data = new FormData(event.currentTarget);
    setError("");
    setPending(true);
    try {
      if (kind === "login") {
        await login(
          String(data.get("email")).trim(),
          String(data.get("password")),
        );
        const raw = params.get("returnTo") ?? "/overview";
        const safe =
          raw.startsWith("/") &&
          !raw.startsWith("//") &&
          !raw.includes("\\") &&
          !/^\/(login|forgot-password|reset-password)/.test(raw)
            ? raw
            : "/overview";
        onLogin(safe);
      } else if (kind === "reset") {
        const token = params.get("token");
        if (!token) {
          setError("auth.missingToken");
          return;
        }
        if (
          String(data.get("password")).length < 8 ||
          data.get("password") !== data.get("confirm")
        ) {
          setError("auth.passwordMismatch");
          return;
        }
        await repositories.auth.reset(token, String(data.get("password")));
        window.dispatchEvent(new Event("govener:unauthorized"));
        setSent(true);
      } else {
        await repositories.auth.requestReset(String(data.get("email")).trim());
        setSent(true);
      }
    } catch (failure) {
      setError(errorKey(failure));
    } finally {
      setPending(false);
    }
  }
  return (
    <div className="auth-page">
      <section className="auth-card">
        <Brand />
        <LanguageSwitcher />
        <h1>{t(`auth.${kind}.title`)}</h1>
        <p>{t(`auth.${kind}.help`)}</p>
        {kind === "login" && expired && (
          <p className="feedback" role="alert">
            {t("auth.expired")}
          </p>
        )}
        {sent ? (
          <div role="status" className="auth-result">
            <p>
              {t(
                kind === "forgot"
                  ? "auth.resetRequested"
                  : "auth.passwordReset",
              )}
            </p>
            <Link href="/login" className="primary">
              {t("auth.login.title")}
            </Link>
          </div>
        ) : (
          <form className="auth-form" onSubmit={submit}>
            <fieldset disabled={pending}>
              {kind !== "reset" && (
                <label>
                  {t("profile.email")}
                  <input
                    type="email"
                    name="email"
                    autoComplete="username"
                    required
                    maxLength={200}
                  />
                </label>
              )}
              {kind !== "forgot" && (
                <label>
                  {t("auth.password")}
                  <input
                    name="password"
                    type="password"
                    required
                    minLength={kind === "reset" ? 8 : 1}
                    maxLength={128}
                    autoComplete={
                      kind === "reset" ? "new-password" : "current-password"
                    }
                  />
                </label>
              )}
              {kind === "reset" && (
                <label>
                  {t("auth.confirm")}
                  <input
                    name="confirm"
                    type="password"
                    required
                    minLength={8}
                    maxLength={128}
                    autoComplete="new-password"
                  />
                </label>
              )}
              {error && (
                <p className="form-error" role="alert">
                  {t(error)}
                </p>
              )}
              <button className="primary">
                {t(pending ? "live.saving" : `auth.${kind}.submit`)}
              </button>
            </fieldset>
          </form>
        )}
        {kind === "login" && (
          <Link href="/forgot-password" className="text-button">
            {t("auth.forgot.title")}
          </Link>
        )}
        {kind !== "login" && (
          <Link href="/login" className="text-button">
            {t("auth.back")}
          </Link>
        )}
      </section>
    </div>
  );
}
