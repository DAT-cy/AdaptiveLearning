<script setup>
import { inject } from 'vue'
import { LogOut } from 'lucide-vue-next'
import { homeContextKey } from '../../composables/homeContext'

const home = inject(homeContextKey)
</script>

<template>
  <section class="page-view"><div class="view-heading"><div><p class="view-kicker">TÀI KHOẢN</p><h1>Cài đặt</h1><p>Thông tin đăng nhập và quyền truy cập của bạn.</p></div></div><section class="account-panel"><div class="account-avatar">{{ home.initials }}</div><div class="account-details"><h2>{{ home.userName }}</h2><p>{{ home.userStore.state.role === 'ROLE_ADMIN' ? 'Quản trị viên' : 'Học viên' }}</p></div><button class="secondary-button" @click="home.logout"><LogOut :size="16" /> Đăng xuất</button></section></section>
</template>
