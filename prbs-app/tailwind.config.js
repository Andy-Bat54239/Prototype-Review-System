/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          navy: '#0F2755',
          blue: '#1D5BAF',
          lightBlue: '#E5EDF8',
          beige: '#F8F5F0',
          darkBeige: '#EDE9E2',
          charcoal: '#1C1814',
          gray: '#7A7069',
          lightGray: '#B8AFA2',
        }
      },
      fontFamily: {
        sans: ['"DM Sans"', 'sans-serif'],
        serif: ['"Playfair Display"', 'serif'],
      }
    },
  },
  plugins: [],
}
