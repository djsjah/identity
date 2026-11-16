import { useEffect, useState } from "react";
import css from "./AuthModal.module.scss";
import { authStore } from "../../store/auth";
import { useTranslation } from "react-i18next";

type AuthModalProps = {
  mode: "login" | "register";
  onClose: () => void;
};

export const AuthModal = ({ mode, onClose }: AuthModalProps) => {
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [rememberMe, setRememberMe] = useState(false);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const { t } = useTranslation();
  const isLogin = mode === "login";

  useEffect(() => {
    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape" && !submitting) onClose();
    };
    window.addEventListener("keydown", handleEscape);
    return () => window.removeEventListener("keydown", handleEscape);
  }, [onClose, submitting]);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (submitting) return;
    setSubmitting(true);
    setError("");

    try {
      if (isLogin) {
        await authStore.login(username, password, rememberMe);
      } else {
        if (new TextEncoder().encode(password).length > 72) {
          throw new Error(t("Auth.passwordTooLong"));
        }
        await authStore.register(username, email, password);
      }
      setPassword("");
      onClose();
    } catch (error) {
      setError(error instanceof Error ? error.message : t("Auth.signInError"));
    } finally {
      setSubmitting(false);
    }
  };

  const toggleMode = () => {
    authStore.setAuthModalMode(isLogin ? "register" : "login");
  };

  return (
    <div
      className={css.overlay}
      onClick={() => {
        if (!submitting) onClose();
      }}
    >
      <div
        className={css.modal}
        role="dialog"
        aria-modal="true"
        aria-labelledby="auth-modal-title"
        onClick={(event) => event.stopPropagation()}
      >
        <h2 id="auth-modal-title" className={css.modalTitle}>
          {isLogin ? t("Auth.signInTitle") : t("Auth.signUpTitle")}
        </h2>
        <form onSubmit={handleSubmit}>
          {!isLogin && (
            <input
              type="email"
              className={css.input}
              placeholder={t("Auth.email")}
              aria-label={t("Auth.email")}
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              autoComplete="email"
              maxLength={254}
              required
            />
          )}
          <input
            type="text"
            className={css.input}
            placeholder={t("Auth.login")}
            aria-label={t("Auth.login")}
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            autoComplete="username"
            minLength={5}
            maxLength={50}
            autoFocus
            required
          />
          <input
            type="password"
            minLength={isLogin ? undefined : 8}
            maxLength={72}
            className={css.input}
            placeholder={t("Auth.password")}
            aria-label={t("Auth.password")}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete={isLogin ? "current-password" : "new-password"}
            required
          />
          {isLogin && (
            <label className={css.rememberMe}>
              <input
                type="checkbox"
                checked={rememberMe}
                onChange={(event) => setRememberMe(event.target.checked)}
              />
              {t("Auth.rememberMe")}
            </label>
          )}
          <button type="submit" className={css.button} disabled={submitting}>
            {isLogin ? t("Auth.signIn") : t("Auth.signUp")}
          </button>
          {error && (
            <p role="alert" className={css.error}>
              {error}
            </p>
          )}
          <p className={css.modalBottom}>
            {isLogin ? t("Auth.notRegistered") : t("Auth.haveAccount")}
            <button
              type="button"
              className={css.actionCall}
              onClick={toggleMode}
              disabled={submitting}
            >
              {isLogin ? t("Auth.signUp") : t("Auth.signIn")}
            </button>
          </p>
        </form>
        <button
          type="button"
          className={css.close}
          aria-label={t("Auth.close")}
          onClick={onClose}
          disabled={submitting}
        >
          ×
        </button>
      </div>
    </div>
  );
};
