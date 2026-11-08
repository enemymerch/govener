import { useEffect, useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import LanguageSwitcher from "../../LanguageSwitcher";
import { api } from "../../api";
import type { Preferences, Profile as UserProfile } from "../../api/generated";
import { errorKey } from "../../api/server";
import { useSession } from "../../session/Session";
import { useWorkspace } from "../../workspace/Workspace";
export default function Profile() {
  const { t, i18n } = useTranslation();
  const { updateUser } = useSession();
  const { mutate, busy, refreshError } = useWorkspace();
  const [profile, setProfile] = useState<UserProfile>();
  const [preferences, setPreferences] = useState<Preferences>();
  const [error, setError] = useState("");
  const [saved, setSaved] = useState("");
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    setError("");
    Promise.all([
      api.execute("getProfile", { signal: controller.signal }),
      api.execute("getPreferences", { signal: controller.signal }),
    ])
      .then(([user, settings]) => {
        if (!controller.signal.aborted) {
          setProfile(user);
          setPreferences(settings);
        }
      })
      .catch((failure) => {
        if (!controller.signal.aborted) setError(errorKey(failure));
      });
    return () => controller.abort();
  }, [attempt]);
  async function saveProfile(event: FormEvent) {
    event.preventDefault();
    if (!profile) return;
    setError("");
    setSaved("");
    try {
      const user = await mutate(() =>
        api.execute("updateProfile", {
          body: {
            name: profile.name,
            email: profile.email,
            phone: profile.phone,
            expectedVersion: profile.version,
          },
        }),
      );
      setProfile(user);
      updateUser(user);
      setPreferences((previous) =>
        previous ? { ...previous, version: user.version } : previous,
      );
      setSaved("profile.saved");
    } catch (failure) {
      setError(errorKey(failure));
      if ((failure as { status?: number }).status === 409)
        setAttempt((value) => value + 1);
    }
  }
  async function savePreferences(event: FormEvent) {
    event.preventDefault();
    if (!preferences || !profile) return;
    setError("");
    setSaved("");
    try {
      const settings = await mutate(() =>
        api.execute("updatePreferences", {
          body: {
            language: i18n.resolvedLanguage === "tr" ? "tr" : "en",
            inApp: preferences.inApp,
            push: preferences.push,
            emailNotifications: preferences.emailNotifications,
            expectedVersion: preferences.version,
          },
        }),
      );
      setPreferences(settings);
      const user = { ...profile, version: settings.version };
      setProfile(user);
      updateUser(user);
      setSaved("profile.saved");
    } catch (failure) {
      setError(errorKey(failure));
      if ((failure as { status?: number }).status === 409)
        setAttempt((value) => value + 1);
    }
  }
  if (!profile || !preferences)
    return (
      <section className="panel route-state">
        {error ? (
          <>
            <p role="alert">{t(error)}</p>
            <button onClick={() => setAttempt((value) => value + 1)}>
              {t("units.retry")}
            </button>
          </>
        ) : (
          <p role="status">{t("live.loading")}</p>
        )}
      </section>
    );
  return (
    <section className="panel">
      <div className="section-heading">
        <div>
          <h2>{t("Profile")}</h2>
          <p>{t("profile.help")}</p>
        </div>
      </div>
      {error && (
        <p className="form-error" role="alert">
          {t(error)}
        </p>
      )}
      {saved && (
        <p className="inline-success" role="status">
          {t(saved)}
        </p>
      )}
      <form className="financial-form" onSubmit={saveProfile}>
        <label>
          {t("profile.name")}
          <input
            disabled={busy}
            required
            maxLength={100}
            value={profile.name}
            onChange={(event) => {
              setProfile({ ...profile, name: event.target.value });
              setSaved("");
            }}
          />
        </label>
        <label>
          {t("profile.email")}
          <input
            type="email"
            disabled={busy}
            required
            maxLength={200}
            value={profile.email}
            onChange={(event) =>
              setProfile({ ...profile, email: event.target.value })
            }
          />
        </label>
        <label>
          {t("profile.phone")}
          <input
            disabled={busy}
            type="tel"
            maxLength={30}
            value={profile.phone}
            onChange={(event) =>
              setProfile({ ...profile, phone: event.target.value })
            }
          />
        </label>
        <div className="form-actions full-width">
          <button className="primary" disabled={busy || !!refreshError}>
            {t("live.saveProfile")}
          </button>
        </div>
      </form>
      <form className="financial-form" onSubmit={savePreferences}>
        <div>
          <p>{t("language")}</p>
          <LanguageSwitcher />
        </div>
        <fieldset className="audience-picker full-width" disabled={busy}>
          <legend>{t("profile.preferences")}</legend>
          {(["inApp", "push", "emailNotifications"] as const).map((field) => (
            <label key={field}>
              <input
                type="checkbox"
                checked={preferences[field]}
                onChange={(event) => {
                  setPreferences({
                    ...preferences,
                    [field]: event.target.checked,
                  });
                  setSaved("");
                }}
              />
              {t(`profile.${field}`)}
            </label>
          ))}
        </fieldset>
        <p className="demo-note full-width">{t("profile.deliveryHelp")}</p>
        <div className="form-actions full-width">
          <button className="primary" disabled={busy || !!refreshError}>
            {t("live.savePreferences")}
          </button>
        </div>
      </form>
    </section>
  );
}
