<template>
  <Header />
  <slot></slot>
  <Footer />
</template>
<script setup>
import Header from "./components/default/Header.vue";
import Footer from "./components/default/Footer.vue";

</script>
