import { defineConfig, loadEnv } from "vite";
import process from "node:process";
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  return {
    server: {
      port: 5173,
      strictPort: true,
      proxy: {
        "/api": {
          target:
            process.env.GOVENER_API_TARGET ||
            env.GOVENER_API_TARGET ||
            "http://127.0.0.1:8080",
          changeOrigin: true,
        },
      },
    },
  };
});
