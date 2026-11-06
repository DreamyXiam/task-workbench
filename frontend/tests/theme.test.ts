import { describe, expect, it } from 'vitest'
import { resolveTheme, saveTheme } from '../src/theme'

describe('appearance preference', () => {
  it('uses a saved preference before the system preference', () => {
    expect(resolveTheme('light', true)).toBe('light')
    expect(resolveTheme('dark', false)).toBe('dark')
  })
  it('falls back to the system preference for missing or invalid values', () => {
    expect(resolveTheme(null, true)).toBe('dark')
    expect(resolveTheme('invalid', false)).toBe('light')
  })
  it('keeps the chosen theme usable when storage is unavailable', () => {
    expect(() => saveTheme({ setItem: () => { throw new Error('blocked') } }, 'dark')).not.toThrow()
  })
})
