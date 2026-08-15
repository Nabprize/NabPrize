import test from 'node:test';
import assert from 'node:assert/strict';
import { applyMove, createGame, lineKey, PLAYER, BOT } from '../src/game.js';

test('a completed box is awarded to the player making the fourth move', () => {
  let state = createGame(PLAYER, BOT);
  const moves = [
    [{ r1: 0, c1: 0, r2: 0, c2: 1 }, PLAYER],
    [{ r1: 0, c1: 0, r2: 1, c2: 0 }, BOT],
    [{ r1: 1, c1: 0, r2: 1, c2: 1 }, PLAYER],
    [{ r1: 0, c1: 1, r2: 1, c2: 1 }, BOT]
  ];
  for (const [line, owner] of moves) state = applyMove(state, owner, line);
  assert.equal(state.boxes['0:0'], BOT);
  assert.equal(state.scores[BOT], 1);
});

test('the same line cannot be played twice and turns are enforced', () => {
  const state = createGame(PLAYER, BOT);
  const line = { r1: 0, c1: 0, r2: 0, c2: 1 };
  const next = applyMove(state, PLAYER, line);
  assert.equal(next.lines[lineKey(line)], PLAYER);
  assert.throws(() => applyMove(next, PLAYER, { r1: 0, c1: 0, r2: 1, c2: 0 }), /NOT_YOUR_TURN/);
  assert.throws(() => applyMove(next, BOT, line), /INVALID_LINE/);
});
