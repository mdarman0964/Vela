export type Side = "buy" | "sell";

export type Candle = {
  time: number;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
  spread: number;
};

export type Settings = {
  enableTrading: boolean;
  allowBuy: boolean;
  allowSell: boolean;
  manageExistingTrades: boolean;
  riskPercent: number;
  fixedLot: number;
  useRiskPercent: boolean;
  magicNumber: number;
  slippagePoints: number;
  maxPositions: number;
  maxSpreadPrice: number;
  vpLookbackBars: number;
  vpNumBins: number;
  vpValueAreaPct: number;
  vpBounceTolPoints: number;
  useM5Trend: boolean;
  m5SmaPeriod: number;
  useH1Trend: boolean;
  h1SmaPeriod: number;
  stopLossPoints: number;
  takeProfitPoints: number;
  useAtrTrailing: boolean;
  atrPeriod: number;
  trailAtrMult: number;
  trailStartAtr: number;
  trailMinPoints: number;
  pointSize: number;
  spreadSafetyMult: number;
  spreadPrice: number;
};

export type ProfileBin = {
  low: number;
  high: number;
  price: number;
  volume: number;
};

export type Profile = {
  valid: boolean;
  poc: number;
  vah: number;
  val: number;
  bins: ProfileBin[];
  low: number;
  high: number;
};

export type Position = {
  side: Side;
  volume: number;
  openPrice: number;
  sl: number;
  tp: number;
  openTime: number;
  trailed: boolean;
};

export type Fill = {
  id: string;
  side: Side;
  volume: number;
  openPrice: number;
  closePrice: number;
  openTime: number;
  closeTime: number;
  pnl: number;
  reason: "sl" | "tp";
  trailed: boolean;
};

export type Decision = {
  index: number;
  spreadOk: boolean;
  spread: number;
  profileOk: boolean;
  bullish: boolean;
  bearish: boolean;
  trendReady: boolean;
  buyVal: boolean;
  buyPoc: boolean;
  sellVah: boolean;
  sellPoc: boolean;
  buySignal: boolean;
  sellSignal: boolean;
  ambiguous: boolean;
  action: "buy" | "sell" | "flat" | "blocked" | "disarmed";
  note: string;
};

export type Session = {
  balance: number;
  position: Position | null;
  fills: Fill[];
  logs: string[];
  profile: Profile;
  decision: Decision;
  equity: number;
  usedMargin: number;
  lastPrice: number;
  spread: number;
  processedIndex: number;
};

export const TAPE_SEED = 20261031;
export const PAPER_BALANCE = 10_000;
export const CONTRACT_SIZE = 100;
export const TICK = 0.01;
export const LOT_STEP = 0.01;
export const LOT_MIN = 0.01;
export const LOT_MAX = 50;
export const LEVERAGE = 100;
export const M5_MS = 5 * 60 * 1000;
export const H1_MS = 60 * 60 * 1000;
const TAPE_START = Date.UTC(2026, 9, 26, 0, 0, 0);

export const DEFAULT_SETTINGS: Settings = {
  enableTrading: true,
  allowBuy: true,
  allowSell: true,
  manageExistingTrades: true,
  riskPercent: 1,
  fixedLot: 0.01,
  useRiskPercent: false,
  magicNumber: 20260918,
  slippagePoints: 10,
  maxPositions: 1,
  maxSpreadPrice: 5,
  vpLookbackBars: 200,
  vpNumBins: 24,
  vpValueAreaPct: 70,
  vpBounceTolPoints: 30,
  useM5Trend: true,
  m5SmaPeriod: 50,
  useH1Trend: false,
  h1SmaPeriod: 50,
  stopLossPoints: 300,
  takeProfitPoints: 600,
  useAtrTrailing: true,
  atrPeriod: 14,
  trailAtrMult: 0.05,
  trailStartAtr: 1,
  trailMinPoints: 50,
  pointSize: 0.01,
  spreadSafetyMult: 1.5,
  spreadPrice: 0.2,
};

const EMPTY_PROFILE: Profile = {
  valid: false,
  poc: 0,
  vah: 0,
  val: 0,
  bins: [],
  low: 0,
  high: 0,
};

function emptyDecision(index: number): Decision {
  return {
    index,
    spreadOk: true,
    spread: 0,
    profileOk: false,
    bullish: false,
    bearish: false,
    trendReady: false,
    buyVal: false,
    buyPoc: false,
    sellVah: false,
    sellPoc: false,
    buySignal: false,
    sellSignal: false,
    ambiguous: false,
    action: "flat",
    note: "Warming up",
  };
}

export function normalizePrice(price: number): number {
  return Math.round(price / TICK) * TICK;
}

export function round2(price: number): number {
  return Math.round(price * 100) / 100;
}

function hash(n: number, seed: number): number {
  let x = Math.imul(n ^ seed, 0x9e3779b1);
  x = Math.imul(x ^ (x >>> 16), 0x85ebca6b);
  x = Math.imul(x ^ (x >>> 13), 0xc2b2ae35);
  return (x ^ (x >>> 16)) >>> 0;
}

function hash01(n: number, seed: number): number {
  return hash(n, seed) / 4294967296;
}

function centerAt(i: number): number {
  const base = 4060;
  if (i < 180) return base + Math.sin(i / 30) * 1.2;
  if (i < 860) return base + 1.2 - (i - 180) * 0.018;
  return base + 1.2 - (860 - 180) * 0.018 + (i - 860) * 0.02;
}

function synthCandle(i: number, _prevClose: number, seed: number): Candle {
  const osc = (n: number) => Math.sin(n / 5.15) * 2.85 + Math.sin(n / 14) * 0.45;
  const open = round2(centerAt(i - 1) + osc(i - 1));
  let close = round2(centerAt(i) + osc(i));
  if (close === open) close = round2(open + (osc(i) >= osc(i - 1) ? 0.01 : -0.01));
  const wick = 0.08 + hash01(i + 9, seed) * 0.42;
  const high = round2(Math.max(open, close) + wick);
  const low = round2(Math.min(open, close) - wick * (0.85 + hash01(i + 4, seed) * 0.4));
  const span = Math.max(0.2, high - low);
  const pullback = osc(i) < -1.2 ? 1.8 : 1;
  const volume = Math.round(80 + span * 160 * pullback + hash01(i + 29, seed) * 40);
  const spread = round2(0.09 + hash01(i + 41, seed) * 0.08);
  return {
    time: TAPE_START + i * M5_MS,
    open,
    high,
    low,
    close,
    volume,
    spread,
  };
}

export const TAPE_BARS = 1880;

function withBuyWicks(candles: Candle[]): Candle[] {
  const next = candles.slice();
  const averages = smaSeries(next, 50);
  for (let i = 640; i < next.length; i++) {
    const candle = next[i];
    const average = averages[i];
    const prevAverage = averages[i - 1];
    if (average == null || prevAverage == null) continue;
    if (!(candle.close > candle.open) || !(candle.close > average)) continue;
    if (!(next[i - 1].close <= prevAverage)) continue;
    const hours = completedHourCloses(next, i, 50);
    if (!hours) continue;
    const h1Close = hours[0];
    const h1Average = hours.reduce((sum, value) => sum + value, 0) / hours.length;
    if (!(h1Close > h1Average)) continue;
    const profile = buildVolumeProfile(next, i, 200, 32, 70);
    if (!profile.valid || !(candle.close > profile.poc)) continue;
    const gap = candle.low - profile.poc;
    if (!(gap > 0.08) || gap > 2.6) continue;
    next[i] = {
      ...candle,
      low: round2(profile.poc - 0.04),
      volume: candle.volume + 80,
    };
  }
  return next;
}

export function buildTape(count: number, seed = TAPE_SEED): Candle[] {
  const candles: Candle[] = [];
  for (let i = 0; i < count; i++) candles.push(synthCandle(i, 0, seed));
  return withBuyWicks(candles);
}

export function extendTape(candles: Candle[], count: number, seed = TAPE_SEED): Candle[] {
  if (candles.length >= count) return candles;
  return buildTape(count, seed);
}

export function readyIndex(settings: Settings): number {
  return Math.max(
    settings.vpLookbackBars,
    settings.m5SmaPeriod,
    settings.atrPeriod + 1,
    settings.useH1Trend ? settings.h1SmaPeriod * 12 : 0,
  );
}

export function warmIndex(settings: Settings, length: number): number {
  const ready = readyIndex(settings);
  return Math.min(length - 1, Math.max(ready + 80, length - 140));
}

export function buildVolumeProfile(
  candles: Candle[],
  signalIndex: number,
  lookback: number,
  numBins: number,
  valueAreaPct: number,
): Profile {
  const start = signalIndex - lookback;
  const end = signalIndex - 1;
  if (start < 0 || end < start || numBins < 3) return EMPTY_PROFILE;

  let highest = -Infinity;
  let lowest = Infinity;
  for (let i = start; i <= end; i++) {
    const high = candles[i].high;
    const low = candles[i].low;
    if (high <= 0 || low <= 0 || high < low) return EMPTY_PROFILE;
    if (high > highest) highest = high;
    if (low < lowest) lowest = low;
  }
  if (!(highest > lowest)) return EMPTY_PROFILE;

  const binSize = (highest - lowest) / numBins;
  if (!(binSize > 0)) return EMPTY_PROFILE;

  const binVolume = new Array<number>(numBins).fill(0);
  for (let i = start; i <= end; i++) {
    const high = candles[i].high;
    const low = candles[i].low;
    const volume = candles[i].volume;
    if (volume <= 0) continue;
    let firstBin = Math.floor((low - lowest) / binSize);
    let lastBin = Math.floor((high - lowest) / binSize);
    firstBin = Math.max(0, Math.min(numBins - 1, firstBin));
    lastBin = Math.max(0, Math.min(numBins - 1, lastBin));
    const span = lastBin - firstBin + 1;
    if (span <= 0) continue;
    const each = volume / span;
    for (let b = firstBin; b <= lastBin; b++) binVolume[b] += each;
  }

  let pocBin = 0;
  let maxVolume = -1;
  let totalVolume = 0;
  for (let b = 0; b < numBins; b++) {
    totalVolume += binVolume[b];
    if (binVolume[b] > maxVolume) {
      maxVolume = binVolume[b];
      pocBin = b;
    }
  }
  if (!(totalVolume > 0)) return EMPTY_PROFILE;

  const target = (totalVolume * valueAreaPct) / 100;
  let covered = binVolume[pocBin];
  let lowBin = pocBin;
  let highBin = pocBin;
  while (covered < target && (lowBin > 0 || highBin < numBins - 1)) {
    const below = lowBin > 0 ? binVolume[lowBin - 1] : -1;
    const above = highBin < numBins - 1 ? binVolume[highBin + 1] : -1;
    if (above >= below && highBin < numBins - 1) {
      highBin += 1;
      covered += binVolume[highBin];
    } else if (lowBin > 0) {
      lowBin -= 1;
      covered += binVolume[lowBin];
    } else {
      break;
    }
  }

  return {
    valid: true,
    poc: lowest + (pocBin + 0.5) * binSize,
    val: lowest + lowBin * binSize,
    vah: lowest + (highBin + 1) * binSize,
    low: lowest,
    high: highest,
    bins: binVolume.map((volume, b) => ({
      volume,
      low: lowest + b * binSize,
      high: lowest + (b + 1) * binSize,
      price: lowest + (b + 0.5) * binSize,
    })),
  };
}

function sma(candles: Candle[], end: number, period: number): number | null {
  if (period < 1 || end - period + 1 < 0) return null;
  let sum = 0;
  for (let i = end - period + 1; i <= end; i++) sum += candles[i].close;
  return sum / period;
}

export function smaSeries(candles: Candle[], period: number): Array<number | null> {
  const out: Array<number | null> = new Array(candles.length).fill(null);
  if (period < 1) return out;
  let sum = 0;
  for (let i = 0; i < candles.length; i++) {
    sum += candles[i].close;
    if (i >= period) sum -= candles[i - period].close;
    if (i >= period - 1) out[i] = sum / period;
  }
  return out;
}

function completedHourCloses(candles: Candle[], signalIndex: number, need: number): number[] | null {
  const aligned =
    candles.length > 1 &&
    candles[1].time - candles[0].time === M5_MS &&
    candles[0].time % H1_MS === 0;
  const closes: number[] = [];
  if (aligned) {
    const hoursCompleted = Math.floor((signalIndex + 1) / 12);
    if (hoursCompleted < need) return null;
    const lastHour = hoursCompleted - 1;
    for (let k = 0; k < need; k++) {
      const bar = (lastHour - k + 1) * 12 - 1;
      if (bar < 0 || bar > signalIndex) return null;
      closes.push(candles[bar].close);
    }
    return closes;
  }

  const byTime = new Map<number, number>();
  for (let i = 0; i <= signalIndex; i++) byTime.set(candles[i].time, candles[i].close);
  const evalTime = candles[signalIndex].time + M5_MS;
  const hourEnd = evalTime % H1_MS === 0 ? evalTime : Math.floor(evalTime / H1_MS) * H1_MS;
  for (let k = 0; k < need; k++) {
    const end = hourEnd - k * H1_MS;
    const lastOpen = end - M5_MS;
    const firstOpen = end - H1_MS;
    const close = byTime.get(lastOpen);
    if (close == null || !byTime.has(firstOpen)) return null;
    closes.push(close);
  }
  return closes;
}

function atr(candles: Candle[], end: number, period: number): number | null {
  if (end < period || period < 1) return null;
  let sum = 0;
  for (let i = end - period + 1; i <= end; i++) {
    const prev = candles[i - 1].close;
    sum += Math.max(candles[i].high - candles[i].low, Math.abs(candles[i].high - prev), Math.abs(candles[i].low - prev));
  }
  return sum / period;
}

function floorLot(volume: number): number {
  const steps = Math.floor((volume + 1e-9) / LOT_STEP);
  return Math.round(steps * LOT_STEP * 100) / 100;
}

function calculateLot(balance: number, settings: Settings, entry: number, stop: number): number {
  if (!settings.useRiskPercent) {
    const fixed = floorLot(settings.fixedLot);
    if (fixed < LOT_MIN || fixed > LOT_MAX) return 0;
    return fixed;
  }
  const riskMoney = (balance * settings.riskPercent) / 100;
  const lossPerLot = Math.abs(entry - stop) * CONTRACT_SIZE;
  if (!(lossPerLot > 0) || !(riskMoney > 0)) return 0;
  let volume = floorLot(riskMoney / lossPerLot);
  if (volume < LOT_MIN) return 0;
  if (volume > LOT_MAX) volume = LOT_MAX;
  return volume;
}

function bullishBounce(candle: Candle, level: number, tolerance: number): boolean {
  const touched = candle.low <= level + tolerance && candle.high >= level - tolerance;
  return touched && candle.close > level && candle.close > candle.open;
}

function bearishBounce(candle: Candle, level: number, tolerance: number): boolean {
  const touched = candle.high >= level - tolerance && candle.low <= level + tolerance;
  return touched && candle.close < level && candle.close < candle.open;
}

function markEquity(balance: number, position: Position | null, candle: Candle): number {
  if (!position) return balance;
  const bid = candle.close;
  const ask = bid + candle.spread;
  const diff = position.side === "buy" ? bid - position.openPrice : position.openPrice - ask;
  return balance + diff * CONTRACT_SIZE * position.volume;
}

function marginOf(position: Position | null): number {
  if (!position) return 0;
  return (position.openPrice * CONTRACT_SIZE * position.volume) / LEVERAGE;
}

function pushLog(logs: string[], line: string): string[] {
  const next = logs.length >= 10 ? logs.slice(logs.length - 9) : logs.slice();
  next.push(line);
  return next;
}

function exitPrice(
  side: Side,
  candle: Candle,
  sl: number,
  tp: number,
): { price: number; reason: "sl" | "tp" } | null {
  const { open, high, low, spread } = candle;
  if (side === "buy") {
    if (open <= sl) return { price: open, reason: "sl" };
    if (tp > 0 && open >= tp) return { price: open, reason: "tp" };
    const hitSl = low <= sl;
    const hitTp = tp > 0 && high >= tp;
    if (hitSl) return { price: sl, reason: "sl" };
    if (hitTp) return { price: tp, reason: "tp" };
    return null;
  }
  // Sells exit on the ask, which is the candle bid plus the spread.
  const askOpen = open + spread;
  if (askOpen >= sl) return { price: askOpen, reason: "sl" };
  if (tp > 0 && askOpen <= tp) return { price: askOpen, reason: "tp" };
  const hitSl = high + spread >= sl;
  const hitTp = tp > 0 && low + spread <= tp;
  if (hitSl) return { price: sl, reason: "sl" };
  if (hitTp) return { price: tp, reason: "tp" };
  return null;
}

function trailStop(
  position: Position,
  candle: Candle,
  settings: Settings,
  candles: Candle[],
  index: number,
): Position {
  if (!settings.manageExistingTrades || !settings.useAtrTrailing) return position;
  const value = atr(candles, index, settings.atrPeriod);
  if (value == null) return position;
  const pt = settings.pointSize;
  const trailPoints = Math.max((value / pt) * settings.trailAtrMult, (candle.spread / pt) * settings.spreadSafetyMult, settings.trailMinPoints);
  const dist = trailPoints * pt;
  const start = value * settings.trailStartAtr;
  if (position.side === "buy") {
    const bid = candle.close;
    if (bid - position.openPrice < start) return position;
    const nextSl = normalizePrice(bid - dist);
    if (nextSl >= position.sl + TICK && nextSl <= bid - TICK) {
      return { ...position, sl: nextSl, trailed: true };
    }
    return position;
  }
  const ask = candle.close + candle.spread;
  if (position.openPrice - ask < start) return position;
  const nextSl = normalizePrice(ask + dist);
  if (nextSl <= position.sl - TICK && nextSl >= ask + TICK) {
    return { ...position, sl: nextSl, trailed: true };
  }
  return position;
}

type Hit = { price: number; reason: "sl" | "tp" };

/**
 * Walks one candle along an assumed intrabar path (bullish: open, low, high, close;
 * bearish: open, high, low, close). Stops and targets are checked on every leg and the
 * ATR trail ratchets at each turning point, so a trail that is raised and touched
 * inside the same candle closes the trade there.
 */
function walkCandle(
  position: Position,
  candle: Candle,
  settings: Settings,
  candles: Candle[],
  index: number,
): { position: Position; hit: Hit | null } {
  const buy = position.side === "buy";
  const off = buy ? 0 : candle.spread;
  const path =
    candle.close >= candle.open
      ? [candle.open, candle.low, candle.high, candle.close]
      : [candle.open, candle.high, candle.low, candle.close];
  let pos = position;
  let prev = path[0] + off;
  if (buy ? prev <= pos.sl : prev >= pos.sl) return { position: pos, hit: { price: prev, reason: "sl" } };
  if (pos.tp > 0 && (buy ? prev >= pos.tp : prev <= pos.tp)) return { position: pos, hit: { price: prev, reason: "tp" } };
  for (let k = 1; k < path.length; k++) {
    const bid = path[k];
    const cur = bid + off;
    const lo = Math.min(prev, cur);
    const hi = Math.max(prev, cur);
    let hit: Hit | null = null;
    let dist = Infinity;
    if (pos.sl >= lo && pos.sl <= hi) {
      hit = { price: pos.sl, reason: "sl" };
      dist = Math.abs(pos.sl - prev);
    }
    if (pos.tp > 0 && pos.tp >= lo && pos.tp <= hi && Math.abs(pos.tp - prev) < dist) {
      hit = { price: pos.tp, reason: "tp" };
    }
    if (hit) return { position: pos, hit };
    pos = trailStop(pos, { ...candle, close: bid }, settings, candles, index);
    prev = cur;
  }
  return { position: pos, hit: null };
}

export function createSession(balance = PAPER_BALANCE): Session {
  return {
    balance,
    position: null,
    fills: [],
    logs: [],
    profile: EMPTY_PROFILE,
    decision: emptyDecision(-1),
    equity: balance,
    usedMargin: 0,
    lastPrice: 0,
    spread: 0,
    processedIndex: -1,
  };
}

export function step(session: Session, candles: Candle[], index: number, settings: Settings): Session {
  if (index < 0 || index >= candles.length || index <= session.processedIndex) return session;
  const candle = candles[index];
  let balance = session.balance;
  let position = session.position;
  let fills = session.fills;
  let logs = session.logs;

  const heldAtOpen = position !== null;
  const settle = (): void => {
    if (!position) return;
    const walked = walkCandle(position, candle, settings, candles, index);
    position = walked.position;
    const hit = walked.hit;
    if (hit) {
      const diff =
        position.side === "buy" ? hit.price - position.openPrice : position.openPrice - hit.price;
      const pnl = diff * CONTRACT_SIZE * position.volume;
      balance = round2(balance + pnl);
      const fill: Fill = {
        id: `${position.openTime}-${position.side}`,
        side: position.side,
        volume: position.volume,
        openPrice: position.openPrice,
        closePrice: round2(hit.price),
        openTime: position.openTime,
        closeTime: candle.time,
        pnl: round2(pnl),
        reason: hit.reason,
        trailed: position.trailed,
      };
      fills = fills.concat(fill);
      logs = pushLog(
        logs,
        `${fill.side === "buy" ? "Long" : "Short"} closed at ${fill.closePrice.toFixed(2)} · ${hit.reason.toUpperCase()} ${pnl >= 0 ? "+" : ""}${fill.pnl.toFixed(2)}`,
      );
      position = null;
    }
  };
  settle();

  const profile = buildVolumeProfile(
    candles,
    index,
    settings.vpLookbackBars,
    settings.vpNumBins,
    settings.vpValueAreaPct,
  );
  const decision = emptyDecision(index);
  decision.spread = candle.spread;
  decision.spreadOk = true;
  decision.profileOk = profile.valid;

  let note = "No bounce";
  let action: Decision["action"] = "flat";

  // v3 evaluates on the first tick of a new bar. The bar open stands in for that tick.
  const pt = settings.pointSize;
  const bid = candle.open;
  const ask = candle.open + candle.spread;
  const price = (bid + ask) / 2;
  let smaValue: number | null = price;
  if (settings.useM5Trend) {
    smaValue = null;
    if (index - settings.m5SmaPeriod + 1 >= 0) {
      let sum = price;
      for (let i = index - settings.m5SmaPeriod + 1; i < index; i++) sum += candles[i].close;
      smaValue = sum / settings.m5SmaPeriod;
    }
  }
  decision.trendReady = smaValue != null;

  if (!settings.enableTrading) {
    action = "disarmed";
    note = "Strategy disarmed";
  } else if (heldAtOpen) {
    action = "blocked";
    note = "Position already open";
  } else if (!profile.valid || smaValue == null) {
    action = "blocked";
    note = "Profile or trend still warming";
  } else {
    const bullish = !settings.useM5Trend || price > smaValue;
    const bearish = !settings.useM5Trend || price < smaValue;
    decision.bullish = bullish;
    decision.bearish = bearish;
    const tolerance = settings.vpBounceTolPoints * pt;
    const near = (level: number) => Math.abs(price - level) <= tolerance;
    decision.buyVal = settings.allowBuy && bullish && near(profile.val);
    decision.buyPoc = settings.allowBuy && bullish && near(profile.poc) && price >= profile.poc;
    decision.buySignal = decision.buyVal || decision.buyPoc;
    if (!decision.buySignal) {
      decision.sellVah = settings.allowSell && bearish && near(profile.vah);
      decision.sellPoc = settings.allowSell && bearish && near(profile.poc) && price <= profile.poc;
    }
    decision.sellSignal = decision.sellVah || decision.sellPoc;
    if (decision.buySignal || decision.sellSignal) {
      const side: Side = decision.buySignal ? "buy" : "sell";
      const dir = side === "buy" ? 1 : -1;
      const entry = normalizePrice(side === "buy" ? ask : bid);
      const sl = normalizePrice(entry - dir * settings.stopLossPoints * pt);
      const tp = settings.takeProfitPoints > 0 ? normalizePrice(entry + dir * settings.takeProfitPoints * pt) : 0;
      const volume = calculateLot(balance, settings, entry, sl);
      if (!(volume > 0)) {
        action = "blocked";
        note = "Lot size is invalid. Trade skipped.";
        logs = pushLog(logs, note);
      } else {
        position = { side, volume, openPrice: entry, sl, tp, openTime: candle.time, trailed: false };
        action = side;
        note = side === "buy" ? "BUY signal confirmed." : "SELL signal confirmed.";
        logs = pushLog(logs, `${note} ${volume.toFixed(2)} @ ${entry.toFixed(2)}`);
      }
    } else if (bullish) {
      note = "Bullish — waiting for a bounce at VAL or POC";
    } else if (bearish) {
      note = "Bearish — waiting for a bounce at VAH or POC";
    } else {
      note = "Trend filter is mixed";
    }
  }

  if (action === "buy" || action === "sell") settle();

  decision.action = action;
  decision.note = note;
  const equity = round2(markEquity(balance, position, candle));
  return {
    balance,
    position,
    fills,
    logs,
    profile,
    decision,
    equity,
    usedMargin: round2(marginOf(position)),
    lastPrice: candle.close,
    spread: candle.spread,
    processedIndex: index,
  };
}

export function replay(candles: Candle[], settings: Settings, endIndex: number, balance = PAPER_BALANCE): Session {
  let session = createSession(balance);
  const last = Math.min(endIndex, candles.length - 1);
  const start = Math.max(0, readyIndex(settings) > 0 ? Math.min(last, settings.atrPeriod) : 0);
  for (let i = start; i <= last; i++) session = step(session, candles, i, settings);
  return session;
}

export function clock(ms: number): string {
  const d = new Date(ms);
  const month = d.toISOString().slice(5, 10);
  const hm = d.toISOString().slice(11, 16);
  return `${month} ${hm}`;
}

export function money(value: number): string {
  const abs = Math.abs(value).toLocaleString("en-US", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
  return `${value < 0 ? "-" : ""}$${abs}`;
}

export function px(value: number): string {
  return value.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

export function validateSettings(input: Settings): string | null {
  if (input.vpLookbackBars < 20 || input.vpLookbackBars > 500) return "Lookback must be 20 to 500.";
  if (input.vpNumBins < 3 || input.vpNumBins > 80) return "Bins must be 3 to 80.";
  if (input.vpValueAreaPct <= 0 || input.vpValueAreaPct > 100) return "Value area must be between 0 and 100.";
  if (!(input.stopLossPoints > 0)) return "Stop loss must be above zero.";
  if (input.takeProfitPoints < 0) return "Take profit cannot be negative.";
  if (!(input.maxSpreadPrice > 0)) return "Max spread must be above zero.";
  if (input.m5SmaPeriod < 2 || input.h1SmaPeriod < 2) return "SMA periods must be at least 2.";
  if (input.atrPeriod < 2) return "ATR period must be at least 2.";
  if (!(input.riskPercent > 0) || input.riskPercent > 20) return "Risk percent must be between 0 and 20.";
  if (!(input.fixedLot > 0)) return "Fixed lot must be above zero.";
  if (!(input.spreadPrice >= 0)) return "Spread cannot be negative.";
  if (input.maxPositions < 1) return "Max positions must be at least 1.";
  return null;
}
