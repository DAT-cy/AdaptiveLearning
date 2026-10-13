import { Axios } from "./Axios";

function login(payload) {
  const url = "/api/v1/auth/login";
  return Axios.post(url, payload);
}
function getInfo(payload) {
  const url = "/api/v1/auth/profile";
  return Axios.get(url, payload);
}

export const userService = {
  login, getInfo
};
