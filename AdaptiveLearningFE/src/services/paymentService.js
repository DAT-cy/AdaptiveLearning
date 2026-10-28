import { Axios } from './Axios'

const data = (request) => request.then((response) => response.data?.data ?? response.data)

export const paymentService = {
  entitlements: () => data(Axios.get('/api/v1/me/entitlements')),
  checkout: (provider) => data(Axios.post('/api/v1/payments/checkout', { provider })),
  status: (paymentId) => data(Axios.get(`/api/v1/payments/${paymentId}`)),
  history: () => data(Axios.get('/api/v1/payments/history')),
  completeSandbox: (paymentId, success = true) =>
    data(Axios.post('/api/v1/payments/sandbox/complete', { paymentId, success })),
}
