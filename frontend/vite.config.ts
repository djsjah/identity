import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import svgr from "vite-plugin-svgr";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const backendUrl = env.BACKEND_URL || "http://localhost:8080";

  return {
    plugins: [react(), svgr()],
    server: {
      host: "localhost",
      proxy: {
        "/auth": { target: backendUrl, changeOrigin: true },
        "/users": { target: backendUrl, changeOrigin: true },
        "/images": { target: backendUrl, changeOrigin: true },
      },
    },
  };
});
