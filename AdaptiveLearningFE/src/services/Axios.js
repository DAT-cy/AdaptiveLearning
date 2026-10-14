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

const statusMessages = {
  400: 'Dữ liệu gửi lên không hợp lệ. Vui lòng kiểm tra và thử lại.',
  401: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
  403: 'Bạn không có quyền thực hiện thao tác này.',
  404: 'Không tìm thấy dữ liệu hoặc địa chỉ yêu cầu.',
  409: 'Dữ liệu bị xung đột. Vui lòng tải lại và thử lại.',
  422: 'Dữ liệu chưa đáp ứng yêu cầu. Vui lòng kiểm tra lại.',
  500: 'Máy chủ gặp sự cố. Vui lòng thử lại sau.',
  502: 'Máy chủ chưa phản hồi. Vui lòng thử lại sau.',
  503: 'Dịch vụ đang tạm thời không khả dụng.',
  504: 'Máy chủ phản hồi quá thời gian chờ.',
}

function showError(description) {
  toast({
    title: 'Đã xảy ra lỗi',
    description,
    variant: 'destructive',
  })
}

function getStatusMessage(code) {
  return statusMessages[code] || `Yêu cầu thất bại (mã lỗi ${code}).`
}

const handleCode = (code) => {
  const userStore = useUserStore()
  switch (code) {
    case invalidCode:
      showError(getStatusMessage(code))
      userStore.resetToken()
      if (loginInterception) {
        router.push({ path: '/login' })
      }
      break
    case noPermissionCode:
      router.push({ path: '/403' })
      break
    default:
      showError(getStatusMessage(code))
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
        handleCode(code)
        return Promise.reject(
          `Request exception interception:${JSON.stringify({
            url: config.url,
            code,
            msg: getStatusMessage(code),
          })}` || 'Error'
        )
      }
    },
    error => {
      pendingRequests--; // Giảm biến đếm khi nhận được phản hồi từ API
      isLoading.value = pendingRequests > 0
      const { response, message } = error
      if (response && response.data) {
        const { status } = response
        handleCode(status)
        return Promise.reject(error)
      } else {
        const timedOut = error.code === 'ECONNABORTED' || message?.toLowerCase().includes('timeout')
        showError(timedOut
          ? 'Máy chủ phản hồi quá thời gian chờ. Vui lòng thử lại.'
          : 'Không thể kết nối tới máy chủ. Hãy kiểm tra backend và thử lại.')
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
