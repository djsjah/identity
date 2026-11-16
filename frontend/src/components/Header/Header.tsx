import { useState, useEffect, RefObject } from "react";
import Logo from "./Logo.svg?react";
import css from "./Header.module.scss";
import { observer } from "mobx-react-lite";
import { authStore } from "../../store/auth";
import { validationStore } from "../../store/validation";
import { Link, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";

type HeaderProps = {
  alwaysVisible: boolean;
  dependencyBlock?: RefObject<HTMLElement | null>;
};

export const Header = observer(function Header({
  alwaysVisible,
  dependencyBlock,
}: HeaderProps) {
  const [scrolledPastHero, setScrolledPastHero] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);
  const [error, setError] = useState("");
  const navigate = useNavigate();
  const { t } = useTranslation();

  useEffect(() => {
    if (alwaysVisible) return;

    const handleScroll = () => {
      const heroHeight = dependencyBlock?.current?.clientHeight || 300;
      setScrolledPastHero(window.scrollY > heroHeight - 50);
    };

    const frame = window.requestAnimationFrame(handleScroll);
    window.addEventListener("scroll", handleScroll, { passive: true });
    window.addEventListener("resize", handleScroll);
    return () => {
      window.cancelAnimationFrame(frame);
      window.removeEventListener("scroll", handleScroll);
      window.removeEventListener("resize", handleScroll);
    };
  }, [alwaysVisible, dependencyBlock]);

  const logOut = async () => {
    if (loggingOut) return;
    setLoggingOut(true);
    setError("");
    try {
      await authStore.logout();
      validationStore.reset();
      navigate("/");
    } catch (error) {
      setError(error instanceof Error ? error.message : t("Auth.signOutError"));
    } finally {
      setLoggingOut(false);
    }
  };

  if (!alwaysVisible && !scrolledPastHero) return null;

  return (
    <header className={css.Header}>
      <Link className={css.logo} to="/" aria-label="I-Dentity">
        <Logo className={css.logoSvg} />
      </Link>
      <div className={css.actions}>
        {authStore.isAuthenticated ? (
          <>
            <span className={css.welcome}>{t("Header.loggedIn")}</span>
            <button onClick={() => authStore.setSettingsModalOpen(true)}>
              {t("Header.settings")}
            </button>
            <button
              className={css.logout}
              onClick={() => void logOut()}
              disabled={loggingOut}
            >
              {t("Header.signOut")}
            </button>
          </>
        ) : (
          <>
            <button
              className={css.login}
              onClick={() => authStore.setAuthModalMode("login")}
              disabled={!authStore.initialized}
            >
              {t("Header.signIn")}
            </button>
            <button
              className={css.register}
              onClick={() => authStore.setAuthModalMode("register")}
              disabled={!authStore.initialized}
            >
              {t("Header.signUp")}
            </button>
          </>
        )}
        {error && (
          <span className={css.actionError} role="alert">
            {error}
          </span>
        )}
      </div>
    </header>
  );
});
