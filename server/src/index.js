import express from 'express';
import http from 'node:http';
import { Server } from 'socket.io';
import {
  applyMove, BOT, createGame, DISCONNECT_GRACE_MS, MATCHMAKING_TIMEOUT_MS,
  MAX_CONSECUTIVE_TIMEOUTS, randomBotMove, TURN_TIMEOUT_MS, winnerFor
} from './game.js';

const PORT = Number(process.env.PORT || 3000);
const app = express();
const httpServer = http.createServer(app);
const io = new Server(httpServer, { cors: { origin: '*' } });
const queue = [];
const matches = new Map();
const botNames = [
  'Ahsan Khan', 'Hamza Malik', 'Bilal Ahmed', 'Saad Raza',
  'Usman Ali', 'Danish Shah', 'Zain Abbas', 'Haris Iqbal'
];

function randomBotName() {
  return botNames[Math.floor(Math.random() * botNames.length)];
}

function safeDisplayName(value, userId) {
  const name = String(value || '').trim();
  if (!name || name === userId || name.startsWith('guest_') || name.length > 40) return 'Player';
  return name;
}

app.get('/health', (_req, res) => res.json({ ok: true, queue: queue.length, matches: matches.size }));

function publicState(match) {
  return {
    matchId: match.id,
    state: match.state,
    turnDeadline: match.turnDeadline,
    serverNow: Date.now(),
    opponent: match.opponent
  };
}

function emitState(match) {
  for (const socketId of Object.values(match.sockets)) {
    if (socketId) io.to(socketId).emit('game_state', publicState(match));
  }
}

function emitMatchSnapshot(match, userId) {
  const socketId = match.sockets[userId];
  if (!socketId) return;
  io.to(socketId).emit('match_found', {
    matchId: match.id,
    opponent: match.opponent[userId]
  });
  io.to(socketId).emit('game_state', publicState(match));
}

function findResumableMatch(userId) {
  for (const match of matches.values()) {
    if (match.state.status === 'FINISHED') continue;
    if (match.state.players.includes(userId) && match.sockets[userId] == null) return match;
  }
  return null;
}

function winnerForFinish(match, reason, forfeitingPlayer) {
  if (['rage_quit', 'disconnect_forfeit', 'timeout_forfeit'].includes(reason) && forfeitingPlayer) {
    return match.state.players.find(player => player !== forfeitingPlayer) || null;
  }
  return winnerFor(match.state);
}

function finish(match, reason = 'completed', forfeitingPlayer = null) {
  if (!matches.has(match.id)) return;
  clearTimeout(match.turnTimer);
  clearTimeout(match.disconnectTimer);
  match.state.status = 'FINISHED';
  const result = {
    matchId: match.id,
    reason,
    winner: winnerForFinish(match, reason, forfeitingPlayer),
    scores: match.state.scores,
    startedAt: match.state.startedAt,
    endedAt: Date.now()
  };
  for (const socketId of Object.values(match.sockets)) {
    if (socketId) io.to(socketId).emit('match_finished', result);
  }
  matches.delete(match.id);
}

function startTurnTimer(match) {
  clearTimeout(match.turnTimer);
  if (match.state.status !== 'ACTIVE') return finish(match);
  match.turnDeadline = Date.now() + TURN_TIMEOUT_MS;
  emitState(match);
  match.turnTimer = setTimeout(() => handleTimeout(match), TURN_TIMEOUT_MS);
}

function startMatch(match) {
  if (match.state.status !== 'READY') return;
  match.state.status = 'ACTIVE';
  scheduleBot(match);
}

function handleTimeout(match) {
  if (!matches.has(match.id) || match.state.status !== 'ACTIVE') return;
  const player = match.state.turn;
  match.state.consecutiveTimeouts[player] = (match.state.consecutiveTimeouts[player] || 0) + 1;
  if (match.state.consecutiveTimeouts[player] >= MAX_CONSECUTIVE_TIMEOUTS) {
    finish(match, 'timeout_forfeit', player);
    return;
  }
  const move = randomBotMove(match.state);
  if (move) {
    match.state = applyMove(match.state, player, move);
    emitState(match);
  }
  if (match.state.turn === BOT) scheduleBot(match);
  else startTurnTimer(match);
}

function scheduleBot(match) {
  if (match.state.turn !== BOT || match.state.status !== 'ACTIVE') return startTurnTimer(match);
  clearTimeout(match.turnTimer);
  match.turnTimer = setTimeout(() => {
    const move = randomBotMove(match.state);
    if (move) match.state = applyMove(match.state, BOT, move);
    emitState(match);
    if (match.state.status === 'FINISHED') finish(match, 'completed');
    else startTurnTimer(match);
  }, 700);
}

function createMatch(first, second = null) {
  const id = `match_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
  const p1 = first.userId;
  const p2 = second?.userId || BOT;
  const match = {
    id,
    state: createGame(p1, p2),
    sockets: { [p1]: first.socketId, ...(second ? { [p2]: second.socketId } : {}) },
    opponent: second
      ? {
          [p1]: { userId: p2, displayName: safeDisplayName(second.displayName, p2), isBot: false },
          [p2]: { userId: p1, displayName: safeDisplayName(first.displayName, p1), isBot: false }
        }
      : { [p1]: { userId: BOT, displayName: randomBotName(), isBot: true, label: 'Auto-matched opponent' } },
    ready: new Set(),
    turnTimer: null,
    disconnectTimer: null,
    turnDeadline: null,
    disconnectedUser: null
  };
  match.state.status = 'READY';
  matches.set(id, match);
  for (const socketId of Object.values(match.sockets)) {
    const viewer = Object.entries(match.sockets).find(([, id]) => id === socketId)?.[0];
    io.to(socketId).emit('match_found', { matchId: id, opponent: match.opponent[viewer] });
  }
  // Both human and bot matches start with an authoritative board snapshot.
  for (const socketId of Object.values(match.sockets)) {
    if (socketId) io.to(socketId).emit('game_state', publicState(match));
  }
  return match;
}

function removeFromQueue(socketId) {
  const index = queue.findIndex(entry => entry.socketId === socketId);
  if (index >= 0) queue.splice(index, 1);
}

function attemptMatch() {
  while (queue.length >= 2) createMatch(queue.shift(), queue.shift());
}

io.on('connection', socket => {
  const userId = String(socket.handshake.auth?.userId || socket.id);
  const displayName = safeDisplayName(socket.handshake.auth?.displayName, userId);
  socket.data.userId = userId;

  const resumableMatch = findResumableMatch(userId);
  if (resumableMatch) {
    resumableMatch.sockets[userId] = socket.id;
    resumableMatch.disconnectedUser = null;
    clearTimeout(resumableMatch.disconnectTimer);
    socket.data.matchId = resumableMatch.id;
    emitMatchSnapshot(resumableMatch, userId);
    const opponentId = resumableMatch.state.players.find(id => id !== userId);
    const opponentSocket = resumableMatch.sockets[opponentId];
    if (opponentSocket) io.to(opponentSocket).emit('opponent_reconnected');
  }

  socket.on('join_queue', () => {
    const activeMatch = [...matches.values()].find(match =>
      match.state.status !== 'FINISHED' && match.state.players.includes(userId) && match.sockets[userId]
    );
    if (activeMatch) {
      socket.emit('match_error', { code: 'MATCH_IN_PROGRESS' });
      return;
    }
    removeFromQueue(socket.id);
    const entry = { userId, displayName, socketId: socket.id, queuedAt: Date.now(), botTimer: null };
    queue.push(entry);
    entry.botTimer = setTimeout(() => {
      const index = queue.findIndex(item => item.socketId === socket.id);
      if (index >= 0) {
        queue.splice(index, 1);
        createMatch(entry);
      }
    }, MATCHMAKING_TIMEOUT_MS);
    socket.emit('queue_joined', { timeoutMs: MATCHMAKING_TIMEOUT_MS });
    attemptMatch();
  });

  socket.on('cancel_queue', () => {
    removeFromQueue(socket.id);
    socket.emit('queue_cancelled');
  });

  socket.on('match_ready', ({ matchId }) => {
    const match = matches.get(matchId);
    if (!match || !match.state.players.includes(userId)) return;
    socket.data.matchId = matchId;
    match.ready.add(userId);
    const requiredPlayers = match.state.players.filter(player => player !== BOT);
    if (requiredPlayers.every(player => match.ready.has(player))) startMatch(match);
  });

  socket.on('move', ({ matchId, line }) => {
    const match = matches.get(matchId);
    if (!match || match.state.status !== 'ACTIVE') return socket.emit('match_error', { code: 'MATCH_NOT_ACTIVE' });
    try {
      match.state = applyMove(match.state, userId, line);
      match.state.consecutiveTimeouts[userId] = 0;
      emitState(match);
      if (match.state.status === 'FINISHED') finish(match, 'completed');
      else scheduleBot(match);
    } catch (error) {
      socket.emit('match_error', { code: error.message });
    }
  });

  socket.on('leave_match', ({ matchId }) => {
    const match = matches.get(matchId);
    if (match) finish(match, 'rage_quit', userId);
  });

  socket.on('disconnect', () => {
    removeFromQueue(socket.id);
    for (const match of matches.values()) {
      if (!match.sockets[userId]) continue;
      match.sockets[userId] = null;
      match.disconnectedUser = userId;
      const opponentId = match.state.players.find(id => id !== userId);
      const opponentSocket = match.sockets[opponentId];
      if (opponentSocket) io.to(opponentSocket).emit('opponent_disconnected', { graceMs: DISCONNECT_GRACE_MS });
      clearTimeout(match.disconnectTimer);
      match.disconnectTimer = setTimeout(() => {
        if (matches.has(match.id) && match.sockets[userId] == null && match.disconnectedUser === userId) {
          finish(match, 'disconnect_forfeit', userId);
        }
      }, DISCONNECT_GRACE_MS);
    }
  });
});

httpServer.listen(PORT, () => console.log(`NabPrize match server listening on :${PORT}`));
