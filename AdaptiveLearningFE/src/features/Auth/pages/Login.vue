<script setup>
import { useRouter } from 'vue-router'
import { toTypedSchema } from '@vee-validate/zod'
import { z } from 'zod'
import LoginUi from '../components/Login/LoginUi.vue'
import { useUserStore } from '@/stores/user'
import { userService } from '@/services/userService'

import { toast } from '@/components/ui/overlay'

const router = useRouter()
const userStore = useUserStore()
const formSchema = toTypedSchema(z.object({
  username: z.string().min(1, 'Vui lòng nhập tên đăng nhập.'),
  password: z.string().min(1, 'Vui lòng nhập mật khẩu.'),
}))

async function onSubmit(values) {
  try {
    await userStore.login(values)
    const role = userStore.state.role
    await router.push(role === 'ROLE_ADMIN' ? '/admin' : '/home')
  } catch (error) {
    if (error?.response?.data?.responseCode === 'AUTH-ERR-420') {
      let verification = null
      try {
        verification = await userService.resendVerification({ email: values.username })
      } catch {
        // Keep the verification route available even while the resend cooldown is active.
      }
      await router.push({
        path: '/verify-email',
        query: {
          email: values.username,
          expiresAt: verification?.expiresAt,
          resendAvailableAt: verification?.resendAvailableAt,
        },
      })
    }
  }
}

async function onGoogleSignIn(idToken) {
  try {
    await userStore.loginWithGoogle(idToken)
    toast.success('Đăng nhập Google thành công', { description: 'Chào mừng bạn đến với hệ thống!' })
    const role = userStore.state.role
    await router.push(role === 'ROLE_ADMIN' ? '/admin' : '/home')
  } catch (error) {
    console.error('Đăng nhập Google thất bại:', error)
    const errorMsg = error?.response?.data?.responseMessage || error?.message || 'Đăng nhập Google không thành công. Vui lòng thử lại.'
    toast.error('Đăng nhập Google thất bại', { description: errorMsg })
  }
}
</script>

<template>
  <LoginUi :schema="formSchema" @submit="onSubmit" @google-sign-in="onGoogleSignIn" />
</template>
