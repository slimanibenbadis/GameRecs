/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      fontFamily: {
        'rubik': ['Rubik', 'sans-serif'],
        'fira': ['Fira Sans', 'sans-serif'],
      },
      colors: {
        'primary-light': 'var(--primary-light)',
        'secondary-light': 'var(--secondary-light)',
        'accent-light': 'var(--accent-light)',
        'text-light': 'var(--text-light)',
        'bg-light': 'var(--bg-light)',
        'primary-dark': 'var(--primary-dark)',
        'secondary-dark': 'var(--secondary-dark)',
        'accent-dark': 'var(--accent-dark)',
        'text-dark': 'var(--text-dark)',
        'bg-dark': 'var(--bg-dark)',
        surface: {
          0: 'var(--surface-0)',
          100: 'var(--surface-100)',
          200: 'var(--surface-200)',
          300: 'var(--surface-300)',
          400: 'var(--surface-400)',
          500: 'var(--surface-500)',
          600: 'var(--surface-600)',
          700: 'var(--surface-700)',
          800: 'var(--surface-800)',
          900: 'var(--surface-900)'
        }
      },
    },
  },
  plugins: [require('tailwindcss-primeui')],
  darkMode: 'class'
};

