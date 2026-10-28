<script setup>
import { Primitive } from "reka-ui";
import { Loader2 } from "lucide-vue-next";
import { buttonVariants } from ".";
import { cn } from "@/lib/utils";

const props = defineProps({
  variant: { type: null, required: false },
  size: { type: null, required: false },
  as: { type: String, required: false, default: "button" },
  class: { type: null, required: false },
  asChild: { type: Boolean, required: false },
  disabled: { type: Boolean, required: false, default: false },
  loading: { type: Boolean, required: false, default: false },
});
</script>

<template>
  <Primitive
    :as="as"
    :as-child="asChild"
    :disabled="disabled || loading"
    :class="cn(buttonVariants({ variant, size }), props.class)"
  >
    <Loader2
      v-if="loading"
      class="animate-spin"
      :class="cn(size === 'sm' ? 'w-3.5 h-3.5' : 'w-4 h-4', $slots.default ? 'mr-2' : '')"
    />
    <slot />
  </Primitive>
</template>
