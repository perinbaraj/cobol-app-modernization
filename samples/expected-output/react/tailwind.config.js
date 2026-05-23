/** @type {import('tailwindcss').Config} */
export default {
  // Scan all TypeScript and TSX source files for class names
  content: [
    './index.html',
    './src/**/*.{ts,tsx}',
  ],
  theme: {
    extend: {
      // Custom colour tokens matching CUSTINQ BMS screen attributes:
      // BRT (bright) → font-bold already in Tailwind base
      // UNPROT (unprotected) → standard input styling
      // ASKIP (autoskip/protected) → read-only span styling
    },
  },
  plugins: [],
}
