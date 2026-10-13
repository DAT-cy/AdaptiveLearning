// TODO: <input type="text" v-debounce="[handleInput, 500]" />

import { debounce } from "@/utils/performance";

export const debounceDirective = {
  mounted(el, binding) {
    const [func, delay] = binding.value;
    el.addEventListener('input', debounce(func, delay));
  },
  beforeUnmount(el) {
    el.removeEventListener('input', el.__debounceHandler__);
  },
};