# NabPrize (Dots & Boxes) — Product Requirements Document (V1)

**Market:** Pakistan
**Status:** Pre-development, blueprint finalized
**Author:** Hassan

---

## 1. Executive Summary

A skill-based 1v1 mobile game built on the classic Dots & Boxes mechanic. Unlike typical "watch-ads-earn" apps in the Pakistani market (spin-wheel, scratch-card, luck-based), this app rewards actual gameplay skill. Revenue is 100% ad-driven (AppLovin mediation); users never pay cash. Winning players convert NP-Coins into real rewards (mobile balance, vouchers, physical prizes at higher tiers).

V1 scope is intentionally tight: **Bot Practice + 1v1 Live Matches only.** Tournament mode (4-player, Sunday events) and referral system are V2.

---

## 2. Core Modes (V1)

### 2.1 Bot Practice
- Free, unlimited matches against AI.
- No ticket required to play.
- Purpose: (a) free skill-building for new/casual users, (b) the funnel through which tickets are earned.

### 2.2 1v1 Live Ranked Match
- Requires 1 ticket to enter.
- Matchmaking: **random** (V1) — queue-based, no friend-challenge or online-presence list (deferred to V2 for simplicity).
- **Matchmaking fallback:** if no human opponent found within **20 seconds**, auto-match with a bot instead.

### 2.3 (V2 — Deferred) 4-Player Knockout Tournament
- Runs on Sundays.
- 6x6 grid.
- Entry: 1 ticket/player. Winner gets accelerated NP-Coin boost.
- Not part of V1 build.

---

## 3. Ticket Economy

### 3.1 Earning Tickets (Bot Practice)
- 10 rewarded ads watched (post bot-practice matches) = 1 ticket.
- Scales linearly: 20 ads = 2 tickets, 30 = 3, etc.
- **Ad fallback:** if a rewarded ad fails to load, show an interstitial instead and still count it toward ticket progress (so the funnel never blocks on ad availability).

### 3.2 Daily Cap
- **Max 8 tickets/day** (80 rewarded ads/day) per user.
- Purpose: lets power-users earn meaningfully faster while staying bounded against ad-fraud pattern detection (AppLovin/network risk) and burnout.

### 3.3 Ticket Consumption Rules
- **Loser:** ticket is consumed on loss. A new ticket is required for the next match.
- **Winner:** ticket is **not consumed** — stays valid and reusable for the rest of that calendar day.
- **Per-ticket match cap:** a single ticket can be reused for a max of **15 matches/day** (prevents unbounded free play on one ticket).
- **Intentional leave/rage-quit:** treated as a loss — ticket consumed, opponent gets full winner NP-Coins.

### 3.4 Midnight Reset
- At 12:00 AM, any ticket still valid/held (e.g., from a win streak) **expires** — does not carry to the next day.
- Before expiring, each leftover ticket converts to a **20-NP-Coin bonus** (partial value return, since the ticket wasn't fully utilized).
- After a ticket is used for its full 15-match cap, it's cut with **no bonus** — full revenue value already went to platform (the ticket already delivered its user-facing value across 15 potential wins).
- Every new day, users must watch ads again for new tickets — no exceptions for streaks.

### 3.5 Post-Cap Bot Practice (Beyond 8 Tickets)
- Once the daily 8-ticket cap is hit, user can still play **unlimited bot practice.**
- Each practice win beyond the cap shows an **interstitial ad**, and that ad's revenue self-funds a small NP-Coin reward for the win (no new ticket, no new revenue risk — fully self-contained).
- Keeps engagement alive after the ticket cap without threatening core economics.

### 3.5.1 Optional Ad-Funded Practice Bonus (Pre-Cap, Any Practice Win)
- Separate from the ticket-progress ad: after **any** practice win (whether the user is still under the daily ticket cap or not), show an optional, opt-in "Watch ad for +10 bonus NP-Coins" prompt.
- Purely additive — no forced ad, no impact on the 10-ads-per-ticket ticket-earning flow. If the user skips it, nothing changes.
- Self-funded (the bonus NP-Coins are backed by that ad's own revenue) — extra revenue for the platform and faster NP-Coins progress for the user, with zero core-economics risk.

### 3.6 Daily Check-in
- A 7-day recurring NP-Coins schedule, resets after day 7 completes (or on a missed day — exact reset condition to be defined in logic pass):
  | Day | NP-Coins |
  |---|---|
  | 1 | 10 |
  | 2 | 30 |
  | 3 | 50 |
  | 4 | 70 |
  | 5 | 80 |
  | 6 | 100 |
  | 7 | 150 |
- An interstitial ad is shown when the user claims their check-in for the day (see Section 5, once/day frequency).

---

## 4. NP-Coins & Rewards

### 4.1 NP-Coins Model (Flat, Not Box-Based)
- **Winner:** flat **40 NP-Coins** per win, regardless of match margin.
- **Loser:** flat **2 NP-Coins** per loss (5% of winner's NP-Coins, as a consolation/retention mechanic).
- Box-count-based scoring (NP-Coins per box captured) was considered and explicitly rejected — too slow for winners to progress toward rewards.

### 4.2 NP-Coins-to-PKR Conversion
- **Rate: 20 NP-Coins = Re 1**
- Winner payout: 40 NP-Coins = **Rs 2.00/match**
- Loser payout: 2 pts = **Rs 0.10/match**
- Total payout/match: **Rs 2.10**

### 4.3 Reward Tiers
| Tier | Reward | NP-Coins Threshold |
|---|---|---|
| 1 | Rs 100 Mobile Load | 1,500 NP-Coins |
| 2 | 30 PUBG UC | 3,000 NP-Coins |
| 3 | 100 FreeFire Diamonds | 3,000 NP-Coins |
| 4 | Earbuds (~Rs 800 value) | 20,000 NP-Coins |

- Tier 1 threshold was lowered from an initial 2,500 to **1,500 NP-Coins** to make it achievable same-day for a casual/new user (trust-building) — verified still profitable: at ~Rs 0.088 revenue-backing per NP-Coin, 1,500 NP-Coins represents ~Rs 132 of already-collected revenue against a Rs 100 payout, leaving a ~Rs 32 margin. The breakeven floor (where margin hits zero) is ~1,136 NP-Coins — 1,500 stays safely above it.

- Thresholds are intentionally kept generous relative to strict ticket-revenue backing (funded partly by banner-ad revenue, which isn't otherwise counted in the core ticket/interstitial economics) — this lets users reach achievements faster without compromising the core per-match margin.
- Claiming a tier **subtracts that tier's threshold from the user's NP-Coins balance** (not a reset to 0) — e.g. a user with 2,000 NP-Coins claiming the 1,500-NP-Coin tier is left with 500 NP-Coins, which carry forward toward the next tier.

### 4.4 Reward Fulfillment
- **Manual, via admin panel** — a separate web dashboard where redemption requests are reviewed and processed by hand (mobile-load top-up, voucher code delivery, etc.).
- User submits redemption request once threshold hit; admin processes manually (mobile load top-up, voucher code, etc.)

---

## 5. Monetization & Ad Placement

| Placement | Ad Type | Revenue Purpose |
|---|---|---|
| Bot practice (during) | Banner | Supplemental — funds faster achievement progress |
| Bot practice (end, ticket-earning) | Rewarded | Core ticket-funding mechanism |
| Live match (during, bottom of screen) | Banner | Supplemental |
| Matchmaking-wait screen | Native | Supplemental — fills otherwise-idle wait time |
| Match Result screen | Native | Supplemental |
| Daily Check-in screen/feature | Interstitial | Low-frequency (once/day) — celebratory/routine moment |
| Post-cap bot practice win | Interstitial | Self-funds the small bonus NP-Coins for that win |

**Note:** The match-end interstitial (previously planned for both winner and loser after every live match) was deliberately **removed**. Product philosophy for V1: prioritize user experience over maximizing ad revenue while the app is new and building its user base — even if it means a smaller per-match margin, retention matters more early on. Ad intensity can be revisited once the app has an established, larger user base.

**Ad network:** AppLovin (mediation) — not AdMob alone. Provides better eCPM via bidding competition and built-in fraud filtering. AdMob may still be one network within the mediation stack.

---

### 5.2 Ad Policy Compliance (Critical — Account/App-Ban Risk)

Ad-network policy violations can lead to AdMob/AppLovin account suspension — which would kill the entire revenue source. These are non-negotiable implementation requirements for the dev pass:

- **Native ads:** Must carry a clear "Ad" / "Sponsored" label. Tap-area must be clearly bounded — no placing native ads close enough to real UI buttons that accidental taps are likely (especially relevant on the Matchmaking and Result screens).
- **Banner ads:** Keep reasonable distance from interactive buttons to avoid accidental clicks. Don't force an unusually fast auto-refresh rate — use standard network default (~30-60 sec).
- **Interstitials (Daily Check-in):** Must show a clear close ("X") button after a reasonable delay (5+ sec, per network policy). Never trigger an interstitial immediately on app-open with no user action preceding it — this is an explicitly banned pattern.
- **Rewarded ads:** Must always be opt-in — user explicitly chooses to watch for a reward, never forced. If the user closes/skips before the ad fully completes, no reward should be granted (partial-watch rewarding is also a policy violation).
- **Frequency discipline:** Respect the caps already defined in this document (e.g. 1x/day interstitial on Check-in) — self-imposed limits reduce fraud-pattern risk on top of what the ad network already enforces.

### 5.3 Per-Match Unit Economics (Updated)
- Revenue/match: ~Rs 3.6–3.8 (loser's fresh ticket ~Rs 3.5 + banner ~Rs 0.1 + partial native-ad allocation)
- Payout/match: Rs 2.10 (fixed — 40+2 NP-Coins)
- **Net profit/match: ~Rs 1.5–1.7** (reduced from an earlier ~Rs 2.00 estimate after removing the match-end interstitial — a deliberate trade-off favoring user experience over maximum margin in V1; see Section 5 note above)
- This holds **regardless of win/loss distribution** across users — every match is individually profitable because the loser's ticket is always freshly earned revenue. (Verified via multiple stress-test scenarios: symmetric mix, concentrated-winner scenarios, etc. — system-level profit only depends on total match volume, not skill distribution.)

### 5.4 Revenue Projections (Reference)
| DAU | Avg Tickets/User/Day | Est. Monthly Net Profit |
|---|---|---|
| 85 (early, existing user base) | ~3 | ~Rs 13,000 (~$45-50) |
| 1,000 | 3.5–5 | ~Rs 1.9L–2.7L (~$680–970) |
| 10,000 | 3.5–5 | ~Rs 15L–25L (~$5,400–8,900) |

- Pakistan rewarded-video eCPM assumption: **$1.00–$3.00** (based on real practitioner production data, not generic global benchmarks which skew heavily toward US/UK rates).
- Target: Rs 300K–500K/month is achievable at roughly 1,000–2,000 DAU — well below full 10K-scale projections.

---

## 6. Game Rules & Match Logic

- **Board grid:** 5x4 (20 boxes) for 1v1. *(6x6 reserved for V2 tournament mode.)*
- **Minimum match duration:** 2 minutes (anti-farming safeguard).
- **Turn timer:** 10 seconds per turn. If timer expires, server auto-draws a random available line.
- **AFK penalty:** 3 consecutive timeouts = automatic forfeit.

### 6.1 Edge Cases & Connection Handling
| Scenario | Behavior |
|---|---|
| Player disconnects (internet loss) | Grace period (~30-45 sec) for reconnect; opponent sees "waiting" state. No reconnect within window → automatic forfeit for disconnected player. |
| Intentional leave / rage-quit while losing | Ticket consumed (normal loss); opponent gets full winner NP-Coins (40 NP-Coins) — closes the "leave to avoid loss" exploit. |
| Both players disconnect simultaneously | Match voided; both tickets refunded (no user fault). |
| App backgrounded (call, notification, etc.) | Turn timer keeps running — standard auto-draw/timeout logic applies, no special-casing. |
| Opponent cancels before match start | Do NOT bot-fallback — show remaining player an "Oops, opponent left" message and return both to matchmaking. (Bot-fallback only applies to the initial 20-second no-opponent-found case, not a mid-matchmaking cancellation.) |
| Server crash/downtime mid-match | No ticket consumed (system fault, not user fault) — refund/retry. |
| Reconnect mid-match | Full board-state resync from server (server-authoritative state is source of truth). |

---

## 7. Anti-Fraud & Abuse Prevention

- **Minimum match duration (2 min)** — prevents rapid-fire farming.
- **Daily ticket cap (8/day, 80 ads)** — bounds exposure to ad-network fraud-pattern detection.
- **Per-ticket match cap (15/day)** — bounds unlimited free-play exploitation of a single earned ticket.
- **Device/IP fingerprint checks** — planned, to detect multi-account farming/collusion.
- **Rage-quit closes the ticket-preservation exploit** (see 6.1).
- Note: per-match economics are self-balancing even under adversarial win/loss patterns (verified via scenario modeling) — the loser's ticket is always freshly-earned revenue regardless of how concentrated wins/losses are among users.

---

## 8. Technical Stack

- **Frontend:** Android Native (Kotlin), custom Canvas View for board rendering (60 FPS target). Package name: `com.nabprize.play`.
- **Backend:** Node.js + Socket.io — server-authoritative match state, turn handling, timer sync.
- **Database:** Firebase Firestore — user accounts, ticket balances, NP-Coins, reward requests. *(Live match state itself stays in Socket.io server memory for latency — not written to Firestore in real time.)*
- **Monetization:** AppLovin (mediation), AdMob as one of the mediated networks.
- **Admin Panel:** Separate web panel (own build, not in V1 mobile app scope) — tracks:
  - Total NP-Coins issued (liability) vs. actual AppLovin revenue (via dashboard or Reporting API)
  - Real-time net margin health check
  - Manual reward-fulfillment processing

---

## 9. Roadmap

**V1 (Current Build)**
- Bot Practice + 1v1 Live Matches
- AdMob/AppLovin-only monetization
- Manual admin-panel reward fulfillment

**V2 (Later)**
- 4-Player Tournament mode (Sundays, 6x6 grid)
- Referral system (1 successful referral = 1 free ticket)
- Friend-challenge / online-players list matchmaking option
- Offerwall monetization layer (once traffic/credibility established — avoids the approval bottleneck that stalled the NabPrize project)

---

## 10. Build Timeline

~15 days total:
- 2 days — UI
- 1 day — Firebase setup
- 5 days — Core logic
- 5 days — Testing
- 2 days — Play Store launch

**Existing advantage:** 50+ interested/loyal users already following the project — solves cold-start, gives an early real-world signal on retention before broader marketing spend.

---

## 11. Open Items for Post-Launch Validation

- Real AppLovin eCPM data (current model uses $1-3 estimate from practitioner data — needs live confirmation).
- Actual DAU retention curve and average tickets/user/day (model assumes 3-5; real number could be lower).
- Dots & Boxes cultural appeal in Pakistan vs. more familiar formats (Ludo, Carrom) — unknown, first real signal will come from the existing 50+ user base.
- Matchmaking liquidity in early low-DAU weeks (bot-fallback should mitigate, but worth monitoring).
