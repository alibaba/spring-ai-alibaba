import { request } from '@/request';
import { session } from '@/request/session';

export function authLogin(data: { username: string; password: string }) {
  return request({
    method: 'POST',
    url: '/console/v1/auth/login',
    data,
  }).then((res) => {
    session.set(res.data.data);
    return res;
  });
}

export function authGithubLogin() {
  return request({
    method: 'GET',
    url: '/oauth2/login/github',
  }).then((res) => res.data);
}

export function authLogout() {
  session.clear();
  location.reload();
}
