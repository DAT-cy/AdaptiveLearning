export function loadView(featureName, view) {
  return () => import(`@/features/${featureName}/pages/${view}.vue`)
}