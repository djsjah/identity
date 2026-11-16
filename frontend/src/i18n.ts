import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import HttpBackend from "i18next-http-backend";

const savedLanguage = localStorage.getItem("lang");
const browserLanguage = navigator.language.toLowerCase().startsWith("ru")
  ? "ru"
  : "en";
const language =
  savedLanguage === "ru" || savedLanguage === "en"
    ? savedLanguage
    : browserLanguage;

i18n.on("languageChanged", (current) => {
  document.documentElement.lang = current.startsWith("en") ? "en" : "ru";
});

i18n
  .use(HttpBackend)
  .use(initReactI18next)
  .init({
    backend: { loadPath: "/locales/{{lng}}/{{ns}}.json" },
    lng: language,
    supportedLngs: ["ru", "en"],
    fallbackLng: "ru",
    ns: ["translation"],
    defaultNS: "translation",
    debug: false,
    interpolation: { escapeValue: false },
  });

export default i18n;
