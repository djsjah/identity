import { useEffect, useState } from "react";
import { observer } from "mobx-react-lite";
import { useTranslation } from "react-i18next";
import { authStore } from "../../store/auth";
import css from "../AuthModal/AuthModal.module.scss";

type SettingsModalProps = {
  onClose: () => void;
};

export const SettingsModal = observer(({ onClose }: SettingsModalProps) => {
  const { t } = useTranslation();
  const [mode, setMode] = useState<"email" | "password">("email");
  const [email, setEmail] = useState(authStore.user?.email || "");
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmationCode, setConfirmationCode] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape" && !submitting) onClose();
    };
    window.addEventListener("keydown", handleEscape);
    return () => window.removeEventListener("keydown", handleEscape);
  }, [onClose, submitting]);

  const changeMode = (next: "email" | "password") => {
    setMode(next);
    setCurrentPassword("");
    setNewPassword("");
    setConfirmationCode("");
    setError("");
    setSuccess("");
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (submitting) return;
    setSubmitting(true);
    setError("");
    setSuccess("");

    try {
      if (mode === "email") {
        await authStore.changeEmail(email, currentPassword);
        setCurrentPassword("");
        setSuccess(t("Settings.emailUpdated"));
      } else {
        if (
          [...newPassword].length < 8 ||
          new TextEncoder().encode(newPassword).length > 72
        ) {
          throw new Error(t("Settings.passwordConstraints"));
        }
        await authStore.changePassword(newPassword, confirmationCode);
        setNewPassword("");
        setConfirmationCode("");
        setSuccess(t("Settings.passwordUpdated"));
      }
    } catch (error) {
      setError(
        error instanceof Error ? error.message : t("Settings.updateError"),
      );
    } finally {
      setSubmitting(false);
    }
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
        aria-labelledby="settings-title"
        onClick={(event) => event.stopPropagation()}
      >
        <h2 id="settings-title" className={css.modalTitle}>
          {t("Settings.title")}
        </h2>
        <div className={css.tabs}>
          <button
            type="button"
            className={mode === "email" ? css.activeTab : ""}
            onClick={() => changeMode("email")}
            disabled={submitting}
          >
            {t("Settings.changeEmail")}
          </button>
          <button
            type="button"
            className={mode === "password" ? css.activeTab : ""}
            onClick={() => changeMode("password")}
            disabled={submitting}
          >
            {t("Settings.changePassword")}
          </button>
        </div>
        <form onSubmit={handleSubmit}>
          {mode === "email" ? (
            <>
              <input
                type="email"
                className={css.input}
                aria-label={t("Settings.newEmail")}
                placeholder={t("Settings.newEmail")}
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                autoComplete="email"
                maxLength={254}
                autoFocus
                required
              />
              <input
                type="password"
                className={css.input}
                aria-label={t("Settings.currentPassword")}
                placeholder={t("Settings.currentPassword")}
                value={currentPassword}
                onChange={(event) => setCurrentPassword(event.target.value)}
                autoComplete="current-password"
                maxLength={72}
                required
              />
            </>
          ) : (
            <>
              <p className={css.hint}>{t("Settings.confirmationHint")}</p>
              <input
                type="password"
                className={css.input}
                aria-label={t("Settings.newPassword")}
                placeholder={t("Settings.newPassword")}
                value={newPassword}
                onChange={(event) => setNewPassword(event.target.value)}
                autoComplete="new-password"
                minLength={8}
                maxLength={72}
                autoFocus
                required
              />
              <input
                type="password"
                className={css.input}
                aria-label={t("Settings.confirmationCode")}
                placeholder={t("Settings.confirmationCode")}
                value={confirmationCode}
                onChange={(event) => setConfirmationCode(event.target.value)}
                autoComplete="off"
                required
              />
            </>
          )}
          <button className={css.button} type="submit" disabled={submitting}>
            {t("Settings.save")}
          </button>
          {error && (
            <p className={css.error} role="alert">
              {error}
            </p>
          )}
          {success && (
            <p className={css.success} role="status">
              {success}
            </p>
          )}
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
});
