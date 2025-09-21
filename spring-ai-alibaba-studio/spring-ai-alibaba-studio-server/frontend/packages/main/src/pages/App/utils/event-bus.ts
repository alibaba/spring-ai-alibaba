import EventEmitter from 'eventemitter3';

export interface IEventBus {
  removeListener: (
    event: string | symbol,
    listener: (...args: any[]) => void,
  ) => any;
  addListener: (
    event: string | symbol,
    listener: (...args: any[]) => void,
  ) => any;
  setMaxListeners: (n: number) => any;
  removeAllListeners: (event?: string | symbol) => any;
  setValue: (event?: string, data?: any) => any;
  getValue: (event?: string) => any;
  deleteValue: (event?: string) => boolean;
  emit: (event?: string, ...args: any[]) => void;
  on: (event: string | symbol, listener: (...args: any[]) => void) => any;
  off: (event: string | symbol, listener: (...args: any[]) => void) => any;
  getAllEvents: () => any;
}
let eventBus: IEventBus;

export class EventBus implements IEventBus {
  private readonly eventEmitter: EventEmitter;
  private readonly name?: string;
  private store?: any;
  constructor(emitter: EventEmitter, name?: string) {
    this.eventEmitter = emitter;
    this.name = name;
    this.store = {};
  }
  /**
   * listening event
   * @param event event name
   * @param listener event callback
   */
  // @ts-ignore
  on(event: string, listener: (...args: any[]) => void): () => void {
    this.eventEmitter.on(event, listener);
    return () => {
      this.off(event, listener);
    };
  }

  /**
   * unlisten event
   * @param event event name
   * @param listener event callback
   */
  // @ts-ignore
  off(event: string, listener: (...args: any[]) => void) {
    this.eventEmitter.off(event, listener);
  }

  /**
   * emit event
   * @param event event name
   * @param args event parameters
   * @returns
   */
  // @ts-ignore
  emit(event: string, ...args: any[]) {
    this.eventEmitter.emit(event, ...args);
  }

  removeListener(
    event: string | symbol,
    listener: (...args: any[]) => void,
  ): any {
    return this.eventEmitter.removeListener(event, listener);
  }

  addListener(event: string | symbol, listener: (...args: any[]) => void): any {
    return this.eventEmitter.addListener(event, listener);
  }

  setMaxListeners(n: number): any {
    // @ts-ignore
    return this.eventEmitter.setMaxListeners(n);
  }
  removeAllListeners(event?: string | symbol): any {
    return this.eventEmitter.removeAllListeners(event);
  }
  getAllEvents() {
    // @ts-ignore
    return this.eventEmitter._events;
  }
  /**
   * store the data
   * @param key key
   * @param data data
   */
  // @ts-ignore
  setValue(key: string, data: any): any {
    this.store[key] = data;
  }
  /**
   * get the data
   * @param key key
   */
  // @ts-ignore
  getValue(key: string): any {
    if (this.store.hasOwnProperty(key)) {
      return this.store[key];
    }
    return '';
  }
  /**
   * delete the data
   * @param key key
   */
  // @ts-ignore
  deleteValue(key: string): boolean {
    if (this.store.hasOwnProperty(key)) {
      delete this.store[key];
      return true;
    }
    return false;
  }
}
export const createEventBus = (
  moduleName?: string,
  maxListeners?: number,
): IEventBus => {
  const emitter = new EventEmitter();
  if (maxListeners) {
    // @ts-ignore
    emitter.setMaxListeners(maxListeners);
  }
  // @ts-ignore
  eventBus = new EventBus(emitter, moduleName || 'bailian-canvas');
  return eventBus;
};
