import { ref } from 'vue'

export const lastDataUpdatedAt=ref<number|null>(null)
export function markDataUpdated(){lastDataUpdatedAt.value=Date.now()}
export function clearDataUpdated(){lastDataUpdatedAt.value=null}
