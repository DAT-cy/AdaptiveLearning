import { computed, reactive } from 'vue'
import { defineStore } from 'pinia'
import { removeToken, setToken, getToken } from '@/utils/auth'
import { userService } from '@/services/userService.js'

function normalizeRole(role) {
  if (!role) return ''
  const str = typeof role === 'string' ? role : (role?.name || '')
  if (!str) return ''
  return str.startsWith('ROLE_') ? str : `ROLE_${str}`
}

export const useUserStore = defineStore('user', () => {
  const state = reactive({
    accessToken: getToken(),
    username: '',
    fullName: localStorage.getItem('adaptive-learning-fullname') || '',
    role: normalizeRole(localStorage.getItem('adaptive-learning-role') || ''),
    avatar: '',
    permissions: [],
  })

  const getterAccessToken = computed(() => state.accessToken)
  const getterUserName = computed(() => state.fullName || state.username)
  const getterAvatar = computed(() => state.avatar)
  const getterPermissions = computed(() => state.permissions)

  async function login({ username, password }) {
    const profile = await userService.login({ username: username.trim(), password })
    if (!profile?.token) throw new Error('Backend không trả về JWT token.')
    setToken(profile.token)
    state.accessToken = profile.token
    state.username = profile.username || username
    state.fullName = profile.fullName || profile.username?.split('@')[0] || ''
    state.role = normalizeRole(profile.role)
    localStorage.setItem('adaptive-learning-fullname', state.fullName)
    localStorage.setItem('adaptive-learning-role', state.role)
  }

  async function loginWithGoogle(idToken) {
    const profile = await userService.loginWithGoogle({ idToken })
    if (!profile?.token) throw new Error('Backend không trả về JWT token.')
    setToken(profile.token)
    state.accessToken = profile.token
    state.username = profile.username || ''
    state.fullName = profile.fullName || profile.username?.split('@')[0] || ''
    state.role = normalizeRole(profile.role)
    localStorage.setItem('adaptive-learning-fullname', state.fullName)
    localStorage.setItem('adaptive-learning-role', state.role)
  }

  async function getInfo() {
    const profile = await userService.getInfo()
    if (!profile?.username) throw new Error('Không xác minh được tài khoản. Vui lòng đăng nhập lại.')
    state.username = profile.username
    state.fullName = profile.fullName || profile.username?.split('@')[0] || ''
    state.role = normalizeRole(profile.role)
    localStorage.setItem('adaptive-learning-fullname', state.fullName)
    localStorage.setItem('adaptive-learning-role', state.role)
  }

  function resetToken() {
    removeToken()
    localStorage.removeItem('adaptive-learning-fullname')
    localStorage.removeItem('adaptive-learning-role')
    Object.assign(state, { accessToken: null, username: '', fullName: '', role: '', avatar: '', permissions: [] })
  }

  function logout() {
    resetToken()
  }

  return {
    state, getterAccessToken, getterUserName, getterAvatar, getterPermissions,
    login, loginWithGoogle, getInfo, logout, resetToken,
  }
})
