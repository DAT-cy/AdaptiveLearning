import axios from "axios";
import { getToken } from '@/utils/auth';
import { isArray } from '@/utils/validate'
import { isLoading } from '@/hooks/isLoading'
import router from "@/router";
import { settings } from '@/config/settings'
import { useToast } from '@/components/ui/toast/use-toast'
import { useUserStore } from '@/stores/user'

const { toast } = useToast()
const { baseURL, contentType, apiLoading, debounce, invalidCode, loginInterception, noPermissionCode, requestTimeout, successCode } = settings
let axiosInstance = null;

const handleCode = (code, msg) => {
  const userStore = useUserStore()
  switch (code) {
    case invalidCode:
      toast({
        title: 'Uh oh! Something went wrong.',
        description: msg || `Backend interface ${code} exception`,
        variant: 'destructive',
      });
      userStore.resetToken()
      if (loginInterception) {
        router.push({ path: '/login' })
      }
      break
    case noPermissionCode:
      router.push({ path: '/403' })
      break
    default:
      toast({
        title: 'Uh oh! Something went wrong.',
        description: msg || `Backend interface ${code} exception`,
        variant: 'destructive',
      });
      break
  }
}
let pendingRequests = 0;
function getInstance() {
  if (axiosInstance != null) {
    return axiosInstance
  }
  axiosInstance = axios.create({
    baseURL: baseURL,
    timeout: requestTimeout,
    headers: {
      'Content-Type': contentType
    },
  })
  //hook interceptor cài ở đây
  axiosInstance.interceptors.request.use(
    config => {
      const token = getToken()
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      const shouldDebounce = debounce.some(url => config.url.includes(url));
      pendingRequests++; // Tăng biến đếm khi bắt đầu gọi API
      isLoading.value = (apiLoading || shouldDebounce) && pendingRequests > 0;
      return config;
    },
    error => {
      return Promise.reject(error)
    }
  )

  axiosInstance.interceptors.response.use(
    response => {
      pendingRequests--; // Giảm biến đếm khi nhận được phản hồi từ API
      isLoading.value = pendingRequests > 0
      const { data, config } = response
      // PATCH: ACCEPT EITHER .code OR .statusCode from backend
      const code = data.code !== undefined ? data.code : data.statusCode
      const codeVerificationArray = isArray(successCode) ? [...successCode] : [...[successCode]]
      if (codeVerificationArray.includes(code)) {
        return data
      } else {
        handleCode(code, data.msg || data.responseMessage || data.message)
        return Promise.reject(
          `Request exception interception:${JSON.stringify({
            url: config.url,
            code,
            msg: data.msg || data.responseMessage || data.message,
          })}` || 'Error'
        )
      }
    },
    error => {
      pendingRequests--; // Giảm biến đếm khi nhận được phản hồi từ API
      isLoading.value = pendingRequests > 0
      const { response, message } = error
      if (response && response.data) {
        const { status, data } = response
        handleCode(status, data.msg || message)
        return Promise.reject(error)
      } else {
        let msg
        if (message === 'Network Error') {
          msg = 'Backend interface connection abnormality'
        }
        if (message.includes('timeout')) {
          msg = 'Backend interface request timeout'
        }
        if (message.includes('Request failed with status code')) {
          const code = message.substr(message.length - 3)
          msg = `backend interface ${code} abnormal`
        }
        toast({
          title: 'Uh oh! Something went wrong.',
          description: msg || `Backend interface unknown exception`,
          variant: 'destructive',
        });
        return Promise.reject(error)
      }
    })
  return axiosInstance;
}


function get(endpointApiUrl, payload = {}, config = {}) {
  return getInstance().get(endpointApiUrl, {
    params: payload,
    ...config,
  });
}

function post(endpointApiUrl, payload = {}, config = {}) {
  return getInstance().post(endpointApiUrl, payload, config);
}

function put(endpointApiUrl, payload = {}, config = {}) {
  return getInstance().put(endpointApiUrl, payload, config);
}

function del(endpointApiUrl, payload = {}, config = {}) {
  return getInstance().delete(endpointApiUrl, payload, config);
}

function patch(endpointApiUrl, payload = {}, config = {}) {
  return getInstance().patch(endpointApiUrl, payload, config);
}

export const Axios = {
  axiosInstance,
  get,
  post,
  put,
  del,
  patch
}