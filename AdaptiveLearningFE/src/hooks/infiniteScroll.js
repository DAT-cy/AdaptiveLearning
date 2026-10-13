//TODO: <div v-infinite-scroll="callback"></div>

const infiniteScrollDirective = {
  beforeMount(el, binding) {
    const handleScroll = () => {
      const scrollHeight = el.scrollHeight
      const scrollTop = el.scrollTop
      const clientHeight = el.clientHeight
      const isBottom = scrollHeight - scrollTop - clientHeight < 1

      if (isBottom) {
        binding.value()
      }
    }

    el.addEventListener('scroll', handleScroll)
  },
  unmounted(el) {
    el.removeEventListener('scroll', handleScroll)
  }
}

export default infiniteScrollDirective