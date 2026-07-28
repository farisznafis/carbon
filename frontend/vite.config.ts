import { defineConfig } from "vite";
import react from "@vitejs/plugin-react-swc";
import path from "path";

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => ({
  server: {
    host: "::",
    port: 8080,
    cors: true,
    allowedHosts: true,
    middlewareMode: false,
    proxy: {
      "/api": {
        target: "https://wannabe-kilowatt-overeater.ngrok-free.dev",
        // target: "https://acronym-epidemic-upstream.ngrok-free.dev/",
        // target: "https://d50c-2400-4152-4781-2a00-a59b-cb6e-3abd-3b89.ngrok-free.app/",
        // target: "http://localhost:8081",
        changeOrigin: true,
      },
    }
  },
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
    dedupe: ["react", "react-dom"],
  },
  test: {
    globals: true,
    environment: "jsdom", // simulate browser DOM
    setupFiles: "./src/setupTests.ts", // optional setup file
  }
}));
