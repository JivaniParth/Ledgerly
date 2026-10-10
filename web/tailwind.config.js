/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        finance: {
          blue: '#1A56DB',
          blueDark: '#1240A8',
          blueLight: '#3D7FF5',
          blueContainer: '#DCEAFF',
          green: '#0D9E6A',
          greenDark: '#087A50',
          greenLight: '#1BBF82',
          greenContainer: '#D2F5E9',
          red: '#DE2027',
          redDark: '#B01018',
          redContainer: '#FFE4E4',
          gold: '#F5A623',
          goldDark: '#D48A0A',
          goldContainer: '#FFF1D0',
          teal: '#0097A7',
          tealDark: '#006978',
          tealContainer: '#D0F5F8',
        },
        darkBg: '#0D1117',
        darkSurface: '#161B22',
        darkCard: '#21262D',
        darkBorder: '#30363D',
        darkText: '#E6EDF3',
        darkMuted: '#8B949E',
        lightBg: '#F6F8FA',
        lightSurface: '#FFFFFF',
        lightCard: '#F0F3F6',
        lightBorder: '#D0D7DE',
        lightText: '#0D1117',
        lightMuted: '#57606A',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
      }
    },
  },
  plugins: [],
}
