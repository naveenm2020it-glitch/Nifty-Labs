/** @type {import('tailwindcss').Config} */
export default {
  darkMode: ['class', '[data-theme="dark"]'],
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        ink: 'rgb(var(--ink) / <alpha-value>)',
        muted: 'rgb(var(--muted) / <alpha-value>)',
        line: 'rgb(var(--line) / <alpha-value>)',
        panel: 'rgb(var(--panel) / <alpha-value>)',
        accent: { DEFAULT: 'rgb(var(--accent) / <alpha-value>)', foreground: 'rgb(var(--accent-foreground) / <alpha-value>)' },
      },
      boxShadow: { panel: '0 10px 32px rgba(18, 30, 55, .06)', float: '0 24px 70px rgba(18, 30, 55, .14)' },
      fontFamily: { sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'] },
      transitionTimingFunction: { terminal: 'cubic-bezier(.2,.75,.25,1)' },
    },
  },
  plugins: [],
};
