<script setup>
import { inject } from 'vue'
import { ArrowRight, GraduationCap, Search } from 'lucide-vue-next'
import { homeContextKey } from '../../composables/homeContext'

const home = inject(homeContextKey)
</script>

<template>
  <section class="page-view">
    <div class="view-heading"><div><p class="view-kicker">NỘI DUNG HỌC TẬP</p><h1>Khóa học và chứng chỉ</h1><p>Chọn lĩnh vực học tập để xem đề luyện tập tương ứng.</p></div><label class="search-field"><Search :size="17" /><input v-model="home.searchQuery" placeholder="Tìm khóa học" aria-label="Tìm khóa học"></label></div>
    <div v-if="home.certificates.length" class="certificate-grid"><article v-for="(certificate, index) in home.certificates.filter((item) => !home.searchQuery || `${item.name} ${item.description || ''}`.toLowerCase().includes(home.searchQuery.toLowerCase()))" :key="certificate.certificateId" class="certificate-card"><span class="certificate-icon" :class="`certificate-color-${index % 4}`"><GraduationCap :size="21" /></span><h2>{{ certificate.name }}</h2><p>{{ certificate.description || 'Chương trình học tập và đánh giá theo chứng chỉ.' }}</p><div class="certificate-stats"><span>{{ home.certificateProgress(certificate).total }} bài học</span><span v-if="certificate.passingScore">Mục tiêu {{ certificate.passingScore }}</span></div><div class="course-progress"><span>{{ home.certificateProgress(certificate).completed }}/{{ home.certificateProgress(certificate).total }} bài hoàn thành</span><strong>{{ home.certificateProgress(certificate).percent }}%</strong><div><i :style="{ width: `${home.certificateProgress(certificate).percent}%` }"></i></div></div><button class="outline-button" @click="home.showView('tests'); home.searchQuery = ''">{{ home.certificateProgress(certificate).completed ? 'Tiếp tục học' : 'Bắt đầu học' }} <ArrowRight :size="15" /></button></article></div>
    <div v-else class="empty-page"><GraduationCap :size="28" /><h2>Chưa có chứng chỉ học tập</h2><p>Các chứng chỉ được quản trị viên cấu hình sẽ hiển thị tại đây.</p></div>
  </section>
</template>
