# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Two Versions

This repo contains two parallel implementations of the same prototype:

| | `prbs/` | `prbs-app/` |
|---|---|---|
| Runtime | Babel Standalone (browser) | Vite + @vitejs/plugin-react |
| Modules | None (global scope, load order) | ES modules (`import`/`export`) |
| Run | Open `PRBS.html` in browser | `npm run dev` inside `prbs-app/` |

**Active development happens in `prbs-app/`.** The `prbs/` directory is the original prototype and should be kept in sync but is not the primary target.

## Running `prbs-app`

```bash
cd prbs-app
npm install   # first time only
npm run dev   # dev server with HMR
npm run build # production build
npm run preview # preview the build
```

## Architecture

PRBS is a **client-side-only prototype** for a capstone project review booking system at AUCA (Adventist University of Central Africa). No backend, no persistence — state resets on every page reload.

### State management

All state lives in `App.jsx` and flows down via props. No Context, Redux, or other library.

- `bookings` and `availability` are owned by `App.jsx`, passed as props to `StudentDashboard` and `SupervisorDashboard`
- `AdminPanel` manages its own `users` and `settings` state locally (initialized from `data.js` constants)
- `notifCount` is derived in `App.jsx` as the count of `confirmed` bookings

### Role-based routing

Role is detected at login time in `Login.jsx` by email string matching:
- contains `"supervisor"` → `SupervisorDashboard`
- contains `"admin"` → `AdminPanel`
- anything else → `StudentDashboard`

`switchRole()` in `App.jsx` maps role strings to hardcoded demo emails and bypasses the login flow.

### Data (`prbs-app/src/data.js`)

All mock data and shared utilities are exported from `data.js`:

```js
MOCK_USERS          // { id, name, email, role, status }
MOCK_BOOKINGS_INIT  // { id, studentId, name, group, project, date, time, status, meetUrl }
MOCK_AVAILABILITY   // { date, start, end, duration, meetUrl }  — start/end as "HH:MM"
MOCK_SETTINGS_INIT  // { otpExpiry, cancelWindow, reminderTime }  — all in minutes
ANALYTICS_DATA      // [{ day, sessions }] — used by the Recharts chart in AdminPanel
TODAY               // hardcoded reference date: new Date('2026-04-23')

generateSlots(avail) // takes an availability object, returns ["HH:MM", ...] slot array
fmt12(t)             // "HH:MM" → "H:MM AM/PM"
fmtDate(dateStr)     // "YYYY-MM-DD" → "Monday, 25 April 2026"
```

`generateSlots` replaces time slot arrays that were previously inline in `StudentDashboard`.

### Styling

All styles are inline CSS-in-JS objects — no CSS files, no Tailwind (except `index.css` which only has a global `* { box-sizing: border-box }` reset). Fonts (DM Sans, Playfair Display) are loaded from Google Fonts in `index.html`.

- Primary color: `#1D5BAF`
- Background: `#F8F5F0`
- Sidebar gradient: `#0F2755 → #0D1F45`
- Text hierarchy: `#1C1814` (primary), `#7A7069` (secondary), `#B8AFA2` (muted)

### OTP login

OTP verification is simulated — any 6 digits typed into the OTP inputs will pass. The 6-digit inputs auto-advance focus and support Backspace to go back.

## Scope & Integration Points

When extending toward production:
- Replace email-string role detection with a real user lookup
- Implement real OTP delivery (email/SMS)
- Replace `MOCK_*` constants with API calls; add session/database persistence
