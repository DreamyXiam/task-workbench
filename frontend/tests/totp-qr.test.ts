import { describe, expect, it } from 'vitest'
import jsQR from 'jsqr'
import { createTotpQr } from '../src/totp-qr'

describe('authenticator QR code', () => {
  it('encodes the complete provisioning URI so an authenticator can scan it', () => {
    const uri = 'otpauth://totp/TaskWorkbench:1?secret=JBSWY3DPEHPK3PXP&issuer=TaskWorkbench&digits=6&period=30'
    const qr = createTotpQr(uri)
    expect(qr).not.toBeNull()
    const scale = 5, margin = 4, width = (qr!.size + margin * 2) * scale
    const pixels = new Uint8ClampedArray(width * width * 4).fill(255)
    for (let y = 0; y < qr!.size; y++) for (let x = 0; x < qr!.size; x++) {
      if (!qr!.modules[y][x]) continue
      for (let dy = 0; dy < scale; dy++) for (let dx = 0; dx < scale; dx++) {
        const index = (((y + margin) * scale + dy) * width + (x + margin) * scale + dx) * 4
        pixels[index] = pixels[index + 1] = pixels[index + 2] = 0
      }
    }
    expect(jsQR(pixels, width, width)?.data).toBe(uri)
  })

  it('does not render a QR code for absent or unrelated URLs', () => {
    expect(createTotpQr('')).toBeNull()
    expect(createTotpQr('https://example.com/')).toBeNull()
  })
})
