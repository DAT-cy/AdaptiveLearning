<script setup>
import { cn } from "@/lib/utils";

const props = defineProps({
  class: { type: null, required: false },
  hoverable: { type: Boolean, required: false, default: false },
});
</script>

<template>
  <div
    :class="
      cn(
        'rounded-xl border border-border-subtle bg-white text-text-primary shadow-card transition-all duration-150',
        hoverable && 'hover:shadow-card-hover hover:border-slate-300',
        props.class
      )
    "
  >
    <slot />
  </div>
</template>
