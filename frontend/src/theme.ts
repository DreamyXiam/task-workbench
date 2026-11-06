export type Theme = 'light' | 'dark'
export const themeKey = 'task-workbench:theme'

export function resolveTheme(saved: string | null, systemDark: boolean): Theme {
  return saved === 'light' || saved === 'dark' ? saved : systemDark ? 'dark' : 'light'
}

export function saveTheme(storage: Pick<Storage, 'setItem'>, theme: Theme): void {
  try { storage.setItem(themeKey, theme) } catch { /* Private browsing can block storage. */ }
}

export function initialTheme(): Theme {
  let saved: string | null = null
  try { saved = window.localStorage.getItem(themeKey) } catch { /* Use system preference. */ }
  return resolveTheme(saved, window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false)
}

export function applyTheme(theme: Theme): void {
  document.documentElement.dataset.theme = theme
  document.documentElement.classList.toggle('dark', theme === 'dark')
  document.documentElement.style.colorScheme = theme
  try { saveTheme(window.localStorage, theme) } catch { /* Access itself can be blocked. */ }
}
