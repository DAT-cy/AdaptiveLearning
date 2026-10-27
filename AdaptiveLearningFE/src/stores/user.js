import { computed, reactive } from 'vue'
import { defineStore } from 'pinia'
import { removeToken, setToken, getToken } from '@/utils/auth'
import { userService } from '@/services/userService.js'

export const useUserStore = defineStore('user', () => {
  const state = reactive({
    accessToken: getToken(),
    username: '',
    role: '',
    avatar: '',
    permissions: [],
  })

  const getterAccessToken = computed(() => state.accessToken)
  const getterUserName = computed(() => state.username)
  const getterAvatar = computed(() => state.avatar)
  const getterPermissions = computed(() => state.permissions)

  async function login({ username, password }) {
    const response = await userService.login({ username: username.trim(), password })
    const profile = response?.data
    if (!profile?.token) throw new Error('Backend không trả về JWT token.')
    setToken(profile.token)
    state.accessToken = profile.token
    state.username = profile.username || username
    state.role = profile.role || ''
  }

  async function loginWithGoogle(idToken) {
    const response = await userService.loginWithGoogle({ idToken })
    const profile = response?.data
    if (!profile?.token) throw new Error('Backend không trả về JWT token.')
    setToken(profile.token)
    state.accessToken = profile.token
    state.username = profile.username || ''
    state.role = profile.role || ''
  }

  async function getInfo() {
    const response = await userService.getInfo()
    const profile = response?.data
    if (!profile?.username) throw new Error('Không xác minh được tài khoản. Vui lòng đăng nhập lại.')
    state.username = profile.username
    state.role = profile.role || ''
  }

  function resetToken() {
    removeToken()
    Object.assign(state, { accessToken: null, username: '', role: '', avatar: '', permissions: [] })
  }

  function logout() {
    resetToken()
  }

  return {
    state, getterAccessToken, getterUserName, getterAvatar, getterPermissions,
    login, loginWithGoogle, getInfo, logout, resetToken,
  }
})
