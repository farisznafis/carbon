type LogArgs = unknown[];

const canLog = () => import.meta.env.DEV;

export const logger = {
  debug(...args: LogArgs): void {
    if (canLog()) {
      console.debug(...args);
    }
  },

  info(...args: LogArgs): void {
    if (canLog()) {
      console.info(...args);
    }
  },

  warn(...args: LogArgs): void {
    if (canLog()) {
      console.warn(...args);
    }
  },

  error(...args: LogArgs): void {
    console.error(...args);
  },
};
