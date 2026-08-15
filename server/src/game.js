export const GRID_SIZE = 5;
export const TOTAL_BOXES = (GRID_SIZE - 1) * (GRID_SIZE - 1);
export const TURN_TIMEOUT_MS = 15_000;
export const MATCHMAKING_TIMEOUT_MS = 20_000;
export const DISCONNECT_GRACE_MS = 35_000;
export const MAX_CONSECUTIVE_TIMEOUTS = 3;

export const PLAYER = 'player';
export const BOT = 'bot';

export function lineKey(line) {
  const a = `${line.r1}:${line.c1}`;
  const b = `${line.r2}:${line.c2}`;
  return a < b ? `${a}|${b}` : `${b}|${a}`;
}

export function allLines(gridSize = GRID_SIZE) {
  const lines = [];
  for (let r = 0; r < gridSize; r++) {
    for (let c = 0; c < gridSize - 1; c++) lines.push({ r1: r, c1: c, r2: r, c2: c + 1 });
  }
  for (let r = 0; r < gridSize - 1; r++) {
    for (let c = 0; c < gridSize; c++) lines.push({ r1: r, c1: c, r2: r + 1, c2: c });
  }
  return lines;
}

function boxSides(row, col) {
  return [
    { r1: row, c1: col, r2: row, c2: col + 1 },
    { r1: row + 1, c1: col, r2: row + 1, c2: col + 1 },
    { r1: row, c1: col, r2: row + 1, c2: col },
    { r1: row, c1: col + 1, r2: row + 1, c2: col + 1 }
  ];
}

function completedBoxes(line, lines, gridSize = GRID_SIZE) {
  const horizontal = line.r1 === line.r2;
  const candidates = horizontal
    ? [[line.r1 - 1, line.c1], [line.r1, line.c1]]
    : [[line.r1, line.c1 - 1], [line.r1, line.c1]];
  return candidates.filter(([r, c]) =>
    r >= 0 && r < gridSize - 1 && c >= 0 && c < gridSize - 1 &&
    boxSides(r, c).every(side => lines.has(lineKey(side)))
  ).map(([r, c]) => `${r}:${c}`);
}

export function createGame(playerOne, playerTwo = BOT) {
  return {
    gridSize: GRID_SIZE,
    lines: {},
    boxes: {},
    scores: { [playerOne]: 0, [playerTwo]: 0 },
    players: [playerOne, playerTwo],
    turn: playerOne,
    consecutiveTimeouts: { [playerOne]: 0, [playerTwo]: 0 },
    startedAt: Date.now(),
    endedAt: null,
    status: 'ACTIVE'
  };
}

export function availableLines(state) {
  const used = new Set(Object.keys(state.lines));
  return allLines(state.gridSize).filter(line => !used.has(lineKey(line)));
}

export function applyMove(state, player, line) {
  if (state.status !== 'ACTIVE') throw new Error('MATCH_FINISHED');
  if (state.turn !== player) throw new Error('NOT_YOUR_TURN');
  const key = lineKey(line);
  if (!availableLines(state).some(candidate => lineKey(candidate) === key)) throw new Error('INVALID_LINE');

  const nextLines = { ...state.lines, [key]: player };
  const claimed = completedBoxes(line, new Set(Object.keys(nextLines)), state.gridSize)
    .filter(box => !state.boxes[box]);
  const nextBoxes = { ...state.boxes };
  claimed.forEach(box => { nextBoxes[box] = player; });
  const nextScores = { ...state.scores, [player]: (state.scores[player] || 0) + claimed.length };
  const isFinished = Object.keys(nextLines).length === allLines(state.gridSize).length;
  const nextTurn = isFinished ? player : (claimed.length > 0 ? player : state.players.find(id => id !== player));

  return {
    ...state,
    lines: nextLines,
    boxes: nextBoxes,
    scores: nextScores,
    turn: nextTurn,
    status: isFinished ? 'FINISHED' : 'ACTIVE',
    endedAt: isFinished ? Date.now() : null
  };
}

export function randomBotMove(state) {
  const moves = availableLines(state);
  if (!moves.length) return null;
  return moves[Math.floor(Math.random() * moves.length)];
}

export function winnerFor(state) {
  const [one, two] = state.players;
  if ((state.scores[one] || 0) === (state.scores[two] || 0)) return null;
  return (state.scores[one] || 0) > (state.scores[two] || 0) ? one : two;
}
