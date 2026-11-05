import QRCode from 'qrcode'

export function createTotpQr(uri: string): { size: number, modules: boolean[][] } | null {
  if (!uri.startsWith('otpauth://totp/')) return null
  try {
    const qr = QRCode.create(uri, { errorCorrectionLevel: 'M' })
    return {
      size: qr.modules.size,
      modules: Array.from({ length: qr.modules.size }, (_, y) =>
        Array.from({ length: qr.modules.size }, (_, x) => Boolean(qr.modules.get(y, x))))
    }
  } catch {
    return null
  }
}
