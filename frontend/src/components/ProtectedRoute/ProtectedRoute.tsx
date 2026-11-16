import { ReactNode } from "react";
import { observer } from "mobx-react-lite";
import { Navigate } from "react-router-dom";
import { authStore } from "../../store/auth";

export const ProtectedRoute = observer(
  ({ children }: { children: ReactNode }) => {
    if (!authStore.initialized) return null;
    return authStore.isAuthenticated ? children : <Navigate to="/" replace />;
  },
);
