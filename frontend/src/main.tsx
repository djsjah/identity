import { StrictMode, useEffect } from "react";
import { createRoot } from "react-dom/client";
import "./index.css";
import { createBrowserRouter, Outlet, RouterProvider } from "react-router-dom";
import { observer } from "mobx-react-lite";
import { Landing } from "./pages/Landing/Landing.tsx";
import { ResultPage } from "./pages/Result/Result.tsx";
import { ProtectedRoute } from "./components/ProtectedRoute/ProtectedRoute.tsx";
import { AuthModal } from "./components/AuthModal/AuthModal.tsx";
import { SettingsModal } from "./components/SettingsModal/SettingsModal.tsx";
import { authStore } from "./store/auth.ts";
import "./i18n";

localStorage.removeItem("token");

const AppLayout = observer(() => {
  useEffect(() => {
    void authStore.initialize();
  }, []);

  return (
    <>
      <Outlet />
      {authStore.authModalMode && (
        <AuthModal
          key={authStore.authModalMode}
          mode={authStore.authModalMode}
          onClose={() => authStore.setAuthModalMode(null)}
        />
      )}
      {authStore.settingsModalOpen && authStore.isAuthenticated && (
        <SettingsModal onClose={() => authStore.setSettingsModalOpen(false)} />
      )}
    </>
  );
});

const router = createBrowserRouter([
  {
    element: <AppLayout />,
    children: [
      { path: "/", element: <Landing /> },
      {
        path: "/result",
        element: (
          <ProtectedRoute>
            <ResultPage />
          </ProtectedRoute>
        ),
      },
      { path: "*", element: <Landing /> },
    ],
  },
]);

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>,
);
