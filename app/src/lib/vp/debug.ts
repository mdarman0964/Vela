export type DebugEntry = { t: string; level: "info" | "warn" | "error"; msg: string; detail?: string };
let entries: DebugEntry[] = [];
const subs = new Set<() => void>();
const notify = () => subs.forEach((f) => f());
function text(x: unknown): string | undefined {
  if (x === undefined) return undefined;
  if (x instanceof Error) return x.stack || x.message;
  if (typeof x === "string") return x;
  try {
    return JSON.stringify(x);
  } catch {
    return String(x);
  }
}
export function dlog(level: DebugEntry["level"], msg: string, detail?: unknown) {
  entries = [...entries.slice(-299), { t: new Date().toLocaleTimeString(), level, msg, detail: text(detail) }];
  notify();
}
export const getEntries = () => entries;
export const clearEntries = () => {
  entries = [];
  notify();
};
export const subscribe = (f: () => void) => {
  subs.add(f);
  return () => {
    subs.delete(f);
  };
};
let installed = false;
export function installGlobalHandlers() {
  if (installed || typeof window === "undefined") return;
  installed = true;
  window.addEventListener("error", (e) => dlog("error", `JS error: ${e.message}`, `${e.filename}:${e.lineno}:${e.colno}`));
  window.addEventListener("unhandledrejection", (e) =>
    dlog("error", `Unhandled promise: ${e.reason?.message ?? e.reason}`, e.reason),
  );
}
