import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const rootDir = fileURLToPath(new URL('.', import.meta.url));

export default defineConfig({
  plugins: [react()],
  base: '/',
  resolve: { alias: { '@': path.resolve(rootDir, 'src') } },
  build: {
    outDir: 'dist', emptyOutDir: true, sourcemap: false,
    rollupOptions: {
      input: {
        index: path.resolve(rootDir, 'index.html'),
        'legacy-chart-bridge': path.resolve(rootDir, 'src/legacy-chart-bridge.ts'),
      },
      output: {
        entryFileNames: (chunk) => chunk.name === 'legacy-chart-bridge' ? 'assets/legacy-chart-bridge.js' : 'assets/[name]-[hash].js',
        chunkFileNames: 'assets/chunks/[name]-[hash].js',
        assetFileNames: 'assets/[name]-[hash][extname]',
      },
    },
  },
  server: {
    proxy: { '/api': 'http://127.0.0.1:8000', '/workspace': 'http://127.0.0.1:8000', '/assets': 'http://127.0.0.1:8000' },
  },
});
