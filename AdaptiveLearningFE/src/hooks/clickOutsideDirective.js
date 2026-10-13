//TODO: <div v-click-outside="()=>click()"></div>

const clickOutsideDirective = {
  beforeMount(el, binding) {
    el.__clickOutsideHandler = (event) => {
      if (!el.contains(event.target)) {
        binding.value()
      }
    }
    document.addEventListener('click', el.__clickOutsideHandler)
  },
  unmounted(el) {
    document.removeEventListener('click', el.__clickOutsideHandler)
  }
}

export default clickOutsideDirective