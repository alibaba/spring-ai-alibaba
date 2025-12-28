import axios from 'axios';
import { baseURL } from './request';

/**
 * Interface representing session data structure
 */
interface ISession {
  /** Access token for authentication */
  access_token: string;
  /** Expiration time in seconds */
  expires_in: number;
  /** Refresh token for obtaining new access token */
  refresh_token: string;
}

/** Local storage key for session data */
const lskey = 'data-prefers-session';

/**
 * Session management utilities
 */
export const session = {
  /**
   * Store session data in local storage
   * @param param0 Session data to store
   */
  set({ access_token, expires_in, refresh_token }: ISession) {
    const data = {
      access_token,
      refresh_token,
      expires_time: expires_in * 1000,
    };

    localStorage.setItem(lskey, JSON.stringify(data));
  },

  /**
   * Clear session data from local storage
   */
  clear() {
    localStorage.removeItem(lskey);
  },

  /**
   * Get access token from local storage
   * @returns Access token if exists, otherwise empty string
   */
  get() {
    const resultString = localStorage.getItem(lskey);
    if (!resultString) return '';
    if (resultString) {
      const result = JSON.parse(resultString) as ISession;
      return result.access_token;
    }
  },

  /**
   * Asynchronously get access token with automatic refresh
   * @returns Promise resolving to access token
   */
  async asyncGet() {
    const resultString = localStorage.getItem(lskey);
    if (!resultString) return '';

    if (resultString) {
      const result = JSON.parse(resultString) as ISession & {
        expires_time: number;
      };
      if (result.expires_time > Date.now()) {
        return result.access_token;
      } else {
        const refreshToken = result.refresh_token;

        return axios
          .request({
            baseURL: baseURL.get(),
            method: 'POST',
            url: '/console/v1/auth/refresh-token',
            data: {
              refresh_token: refreshToken,
            },
          })
          .then((res) => {
            session.set(res.data.data);
            return res.data.data.access_token;
          })
          .catch(() => {
            session.clear();
            return '';
          });
      }
    }
  },
};
