<script setup>
import { useRouter } from 'vue-router'
import { toTypedSchema } from '@vee-validate/zod'
import { z } from 'zod'
import LoginUi from '../components/Login/LoginUi.vue'
import { useUserStore } from '@/stores/user'

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
    console.error('Đăng nhập thất bại:', error)
  }
}
</script>

<template>
  <LoginUi :schema="formSchema" @submit="onSubmit" />
</template>
