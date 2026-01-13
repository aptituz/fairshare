import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig(({ command }) => ({
  base: command === "serve" ? "/" : (process.env.VITE_BASE || "/fairshare/"),
  plugins: [vue()],
  server: {
    port: 5173
  }
}));
