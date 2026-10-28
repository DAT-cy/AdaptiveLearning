<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { toTypedSchema } from '@vee-validate/zod'
import { z } from 'zod'
import LoginUi from '../components/Login/LoginUi.vue'
import { useUserStore } from '@/stores/user'
import { userService } from '@/services/userService'
import { EMAIL_RESEND_COOLDOWN_MS } from '../constants'

import { toast } from '@/components/ui/overlay'

const router = useRouter()
const userStore = useUserStore()
const loginError = ref('')
const loading = ref(false)
const googleLoading = ref(false)
const formSchema = toTypedSchema(z.object({
  username: z.string().min(1, 'Vui lòng nhập tên đăng nhập.'),
  password: z.string().min(1, 'Vui lòng nhập mật khẩu.'),
}))

function authErrorMessage(error) {
  const payload = error?.response?.data
  const validationMessage = payload && typeof payload === 'object'
    ? Object.values(payload).find((value) => typeof value === 'string')
    : null
  const code = payload?.responseCode
  if (code === 'AUTH-ERR-408') return 'Tên đăng nhập hoặc mật khẩu không chính xác.'
  if (code === 'AUTH-ERR-413') return 'Tài khoản đã bị vô hiệu hóa. Vui lòng liên hệ hỗ trợ.'
  if (code === 'AUTH-ERR-409') return 'Không xác minh được tài khoản Google. Vui lòng thử lại.'
  return payload?.responseMessage || payload?.message
    || validationMessage
    || 'Không thể kết nối dịch vụ xác thực. Vui lòng thử lại.'
}

async function onSubmit(values) {
  loginError.value = ''
  loading.value = true
  try {
    await userStore.login(values)
    const role = userStore.state.role
    await router.push(role === 'ROLE_ADMIN' ? '/admin' : '/home')
  } catch (error) {
    if (error?.response?.data?.responseCode === 'AUTH-ERR-420') {
      let verification = null
      let resendAvailableAt
      try {
        verification = await userService.resendVerification({ email: values.username })
        resendAvailableAt = verification?.resendAvailableAt
      } catch (resendError) {
        // Keep the verification route available even while the resend cooldown is active.
        if (resendError?.response?.data?.responseCode === 'AUTH-ERR-424'
          || resendError?.response?.status === 412
          || resendError?.response?.status === 429) {
          resendAvailableAt = new Date(Date.now() + EMAIL_RESEND_COOLDOWN_MS).toISOString()
        }
      }
      await router.push({
        path: '/verify-email',
        query: {
          email: values.username,
          expiresAt: verification?.expiresAt,
          resendAvailableAt,
        },
      })
      return
    }
    loginError.value = authErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function onGoogleSignIn(idToken) {
  loginError.value = ''
  googleLoading.value = true
  try {
    await userStore.loginWithGoogle(idToken)
    toast.success('Đăng nhập Google thành công', { description: 'Chào mừng bạn đến với hệ thống!' })
    const role = userStore.state.role
    await router.push(role === 'ROLE_ADMIN' ? '/admin' : '/home')
  } catch (error) {
    const errorMessage = authErrorMessage(error)
    loginError.value = errorMessage
    toast.error('Đăng nhập Google thất bại', { description: errorMessage })
  } finally {
    googleLoading.value = false
  }
}
</script>

<template>
  <LoginUi :schema="formSchema" :error-message="loginError" :loading="loading"
    :google-loading="googleLoading"
    @submit="onSubmit" @google-sign-in="onGoogleSignIn" />
</template>
