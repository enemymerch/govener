import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import en from "./locales/en.json";
import tr from "./locales/tr.json";

export const languageStorageKey = "govener.language";
function storedLanguage(): "en" | "tr" {
  try {
    return localStorage.getItem(languageStorageKey) === "en" ? "en" : "tr";
  } catch {
    return "tr";
  }
}
function applyLanguage(language: string) {
  document.documentElement.lang = language === "en" ? "en" : "tr";
  document.title = i18n.t("appTitle");
  try {
    localStorage.setItem(languageStorageKey, language);
  } catch {
    /* Language switching still works without storage. */
  }
}
void i18n.use(initReactI18next).init({
  resources: { en: { translation: en }, tr: { translation: tr } },
  lng: storedLanguage(),
  supportedLngs: ["tr", "en"],
  fallbackLng: "en",
  keySeparator: false,
  nsSeparator: false,
  interpolation: { escapeValue: false },
});
i18n.on("languageChanged", applyLanguage);
applyLanguage(i18n.resolvedLanguage ?? "tr");
export default i18n;
