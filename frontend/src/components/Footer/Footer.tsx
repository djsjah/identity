import { useTranslation } from "react-i18next";
import css from "./Footer.module.scss";
import { StarsContainer } from "../StarsContainer/StarsContainer";

export const Footer = ({ withStars }: { withStars?: boolean }) => {
  const { i18n } = useTranslation();
  const lang = i18n.language.startsWith("en") ? "en" : "ru";

  const toggleLanguage = () => {
    const newLang = lang === "en" ? "ru" : "en";
    void i18n.changeLanguage(newLang);
    localStorage.setItem("lang", newLang);
  };

  return (
    <footer className={css.footer}>
      {withStars && <StarsContainer />}
      <div className={css.footerContent}>
        <p>© {new Date().getFullYear()} I-Dentity</p>
        <button className={css.langToggle} onClick={toggleLanguage}>
          {lang === "en" ? "🇷🇺 Русский" : "🇺🇸 English"}
        </button>
      </div>
    </footer>
  );
};
