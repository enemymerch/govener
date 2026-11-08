import { useTranslation } from "react-i18next";

export function useFormat() {
  const { i18n } = useTranslation();
  const locale = i18n.resolvedLanguage === "tr" ? "tr-TR" : "en-GB";
  const money = (value: number) =>
    new Intl.NumberFormat(locale, {
      style: "currency",
      currency: "TRY",
      maximumFractionDigits: 2,
    }).format(value);
  const moneyMinor = (value: number) => money(value / 100);
  const date = (
    value: string,
    options: Intl.DateTimeFormatOptions = {
      day: "numeric",
      month: "short",
      year: "numeric",
    },
  ) =>
    new Intl.DateTimeFormat(locale, {
      ...options,
      timeZone: "Europe/Istanbul",
    }).format(new Date(`${value}T12:00:00Z`));
  const period = (value: string) =>
    date(value, { month: "long", year: "numeric" });
  const percent = (value: number) =>
    new Intl.NumberFormat(locale, {
      style: "percent",
      maximumFractionDigits: 0,
    }).format(value);
  const dateTime = (value: string) =>
    new Intl.DateTimeFormat(locale, {
      dateStyle: "medium",
      timeStyle: "short",
      timeZone: "Europe/Istanbul",
    }).format(new Date(value));
  return { locale, money, moneyMinor, date, dateTime, period, percent };
}
