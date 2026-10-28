import { Axios } from "./Axios";

const unwrap = (response) => response?.data?.data ?? response?.data ?? response;

function login(payload) {
  const url = "/api/v1/auth/login";
  return Axios.post(url, payload).then(unwrap);
}
function loginWithGoogle(payload) {
  return Axios.post("/api/v1/auth/google", payload).then(unwrap);
}
function getInfo() {
  const url = "/api/v1/auth/profile";
  return Axios.get(url).then(unwrap);
}

function googleConfig() {
  return Axios.get('/api/v1/auth/google/config').then(unwrap);
}

function signUp(payload) {
  return Axios.post('/api/v1/auth/signup', payload).then(unwrap);
}

function verifyEmail(payload) {
  return Axios.post('/api/v1/auth/signup/verify', payload).then(unwrap);
}

function resendVerification(payload) {
  return Axios.post('/api/v1/auth/signup/resend', payload).then(unwrap);
}

export const userService = {
  login, loginWithGoogle, getInfo, googleConfig, signUp, verifyEmail, resendVerification
};
