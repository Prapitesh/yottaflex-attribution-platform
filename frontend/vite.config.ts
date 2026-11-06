import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: { port: 5173 },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes("/node_modules/recharts/")) return "recharts-vendor";
          if (id.includes("/node_modules/d3-") || id.includes("/node_modules/internmap/")) return "d3-vendor";
          if (id.includes("/node_modules/lucide-react/")) return "icons-vendor";
          return undefined;
        },
      },
    },
  },
});
