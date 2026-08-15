# NabPrize Match Server

Local development:

```powershell
npm install
npm test
npm start
```

The health endpoint is available at `http://localhost:3000/health`.

The server is authoritative for the 5x4 board, validates turns and lines,
keeps the 15-second turn timer, applies timeout forfeits after three
consecutive timeouts, matches two queued players, and falls back to a bot
after 20 seconds. A disconnected player has a 35-second reconnect grace
period.

When the 20-second queue window expires, the fallback bot receives a random
Pakistani-style display name and is marked in the protocol as an
`Auto-matched opponent`; the client must not present it as a real human.

Before production use, the settlement adapter must be connected to Firebase
Admin so completed matches can atomically update tickets, NP-Coins and stats.
Never ship a Firebase service-account JSON file inside the Android app.
