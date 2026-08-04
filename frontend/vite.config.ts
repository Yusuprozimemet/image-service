import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // In dev the app runs here and the API runs on :8080, so /api is proxied
    // across. It has to look like one origin: the session cookie is SameSite=Strict
    // (ports do not split a site, but schemes and hosts do), and going through the
    // proxy also keeps the app free of any base-URL config.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: false,
      },
    },
  },
  build: {
    // Copied into the JAR under /static by the Maven build; see pom.xml.
    outDir: 'dist',
  },
})
