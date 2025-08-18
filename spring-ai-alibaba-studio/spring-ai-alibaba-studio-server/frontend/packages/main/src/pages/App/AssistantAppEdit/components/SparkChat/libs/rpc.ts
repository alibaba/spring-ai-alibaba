import { baseURL, session } from '@/request/request';
import { fetchEventSource } from './fetchEventSource';

export class Rpc {
  baseApi = `${baseURL.get()}/console/v1/apps/chat/completions`;
  control = null as any;
  action = 'DialogForTestWindow';

  constructor(props?: { action?: string }) {
    if (props?.action) this.action = props.action;
  }

  close() {
    this.control?.abort?.();
    this.control = null;
  }

  destroy() {
    this.close();
  }

  async conversation(
    data: any,
    {
      onopen,
      onmessage,
      onerror,
      onclose,
    }: {
      onopen: () => void;
      onmessage: (event: any) => void;
      onclose: () => void;
      onerror: (err: any) => void;
    },
  ) {
    const ctrl = new AbortController();
    const token = await session.asyncGet();

    this.control = ctrl;
    try {
      await fetchEventSource(this.baseApi, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(data),
        credentials: 'include',
        // @ts-ignore
        onopen,
        onmessage,
        onerror,
        onclose,
        signal: ctrl.signal,
        openWhenHidden: true,
      });
    } catch (error) {
      onerror(error);
    }
  }
}
