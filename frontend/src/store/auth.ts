import { makeAutoObservable, runInAction } from "mobx";
import { ApiError, apiRequest } from "../api/client";
import i18n from "../i18n";

export type AuthUser = {
  username: string;
  email: string;
  role: "USER" | "ADMIN";
};

type ModalMode = "login" | "register" | null;

let restorePromise: Promise<void> | null = null;
let sessionRevision = 0;

const readUser = (data: AuthUser): AuthUser => {
  if (
    !data ||
    typeof data.username !== "string" ||
    typeof data.email !== "string" ||
    (data.role !== "USER" && data.role !== "ADMIN")
  ) {
    throw new Error(i18n.t("Auth.invalidResponse"));
  }
  return data;
};

const getSignInErrorMessage = (error: unknown): string => {
  if (error instanceof ApiError && error.status === 401)
    return i18n.t("Auth.badCredentials");
  return getAuthErrorMessage(error, "Auth.signInError");
};

const getAuthErrorMessage = (error: unknown, fallback: string): string => {
  if (error instanceof ApiError) {
    if (error.message === "Username already exists")
      return i18n.t("Auth.usernameIsTaken");
    if (error.message === "Email already exists")
      return i18n.t("Auth.emailIsTaken");
    if (error.message === "Invalid current password")
      return i18n.t("Settings.badPassword");
    if (error.message === "Invalid confirmation code")
      return i18n.t("Settings.badCode");
    if (error.status === 429) return i18n.t("Auth.tooManyRequests");
    if (error.status === 401) return i18n.t("Auth.sessionExpired");
    if (error.status === 403) return i18n.t("Auth.accessDenied");
    if (
      error.status === 400 &&
      error.message &&
      error.message !== "Request failed"
    )
      return error.message;
  }
  return i18n.t(fallback);
};

class AuthStore {
  user: AuthUser | null = null;
  initialized = false;
  authModalMode: ModalMode = null;
  settingsModalOpen = false;

  constructor() {
    makeAutoObservable(this, {}, { autoBind: true });
  }

  get isAuthenticated() {
    return this.user !== null;
  }

  setAuthModalMode(mode: ModalMode) {
    this.authModalMode = mode;
    if (mode) this.settingsModalOpen = false;
  }

  setSettingsModalOpen(open: boolean) {
    this.settingsModalOpen = open;
    if (open) this.authModalMode = null;
  }

  async initialize() {
    if (this.initialized) return;
    if (restorePromise) return restorePromise;

    const revision = sessionRevision;
    restorePromise = (async () => {
      let user: AuthUser | null = null;
      try {
        user = readUser(await apiRequest<AuthUser>("/auth/me"));
      } catch {
        user = null;
      }

      if (revision === sessionRevision) {
        runInAction(() => {
          this.user = user;
          this.initialized = true;
        });
      }
    })();

    try {
      await restorePromise;
    } finally {
      restorePromise = null;
    }
  }

  async login(username: string, password: string, rememberMe: boolean) {
    try {
      const user = readUser(
        await apiRequest<AuthUser>("/auth/sign-in", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ username, password, rememberMe }),
        }),
      );

      sessionRevision++;
      runInAction(() => {
        this.user = user;
        this.initialized = true;
        this.authModalMode = null;
      });
    } catch (error) {
      throw new Error(getSignInErrorMessage(error), { cause: error });
    }
  }

  async register(username: string, email: string, password: string) {
    try {
      const user = readUser(
        await apiRequest<AuthUser>("/auth/sign-up", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ username, email, password }),
        }),
      );

      sessionRevision++;
      runInAction(() => {
        this.user = user;
        this.initialized = true;
        this.authModalMode = null;
      });
    } catch (error) {
      throw new Error(getAuthErrorMessage(error, "Auth.signUpError"), {
        cause: error,
      });
    }
  }

  async logout() {
    try {
      await apiRequest("/auth/logout", { method: "POST" });
    } catch (error) {
      if (!(error instanceof ApiError && error.status === 401)) {
        throw new Error(getAuthErrorMessage(error, "Auth.signOutError"), {
          cause: error,
        });
      }
    }

    this.clearSession();
  }

  clearSession() {
    sessionRevision++;
    this.user = null;
    this.initialized = true;
    this.settingsModalOpen = false;
  }

  async changeEmail(email: string, currentPassword: string) {
    try {
      await apiRequest<void>("/users/me/email", {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, currentPassword }),
      });

      const user = readUser(await apiRequest<AuthUser>("/auth/me"));
      runInAction(() => {
        this.user = user;
      });
    } catch (error) {
      if (error instanceof ApiError && error.status === 401)
        this.clearSession();
      throw new Error(getAuthErrorMessage(error, "Settings.emailError"), {
        cause: error,
      });
    }
  }

  async changePassword(newPassword: string, confirmationCode: string) {
    if (!this.user) throw new Error(i18n.t("Auth.sessionExpired"));

    const username = this.user.username;
    try {
      const user = readUser(
        await apiRequest<AuthUser>("/auth/reset-password", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            username,
            newPassword,
            confirmationCode,
          }),
        }),
      );

      sessionRevision++;
      runInAction(() => {
        this.user = user;
        this.initialized = true;
      });
    } catch (error) {
      if (error instanceof ApiError && error.status === 401)
        this.clearSession();
      throw new Error(getAuthErrorMessage(error, "Settings.passwordError"), {
        cause: error,
      });
    }
  }
}

export const authStore = new AuthStore();
