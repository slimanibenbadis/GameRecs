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
        // Dark theme colors (now default)
        'text': '#d7f9f4',
        'background': '#020d0b',
        'primary': '#7beadb',
        'secondary': '#155b89',
        'accent': '#4684e2',
        surface: {
          0: '#020d0b',    // Darkest
          100: '#051714',
          200: '#082420',
          300: '#0b312c',
          400: '#0e3e38',
          500: '#114b44',
          600: '#145850',
          700: '#17655c',
          800: '#1a7268',
          900: '#1d7f74'   // Lightest
        }
      },
    },
  },
  plugins: [require('tailwindcss-primeui')],
  // Remove darkMode config since we're enforcing dark theme
};

