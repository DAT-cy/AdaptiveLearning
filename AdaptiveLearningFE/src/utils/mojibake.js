/**
 * Tiện ích phát hiện và tự động khôi phục lỗi font/encoding tiếng Việt (Mojibake).
 * Xảy ra khi chuỗi UTF-8 bị giải mã nhầm qua bảng mã Windows-1252 / ISO-8859-1.
 *
 * Ví dụ:
 * "Ä  á»— Anh Tuáº¥n" / "Ä\u0090á»— Anh Tuáº¥n" -> "Đỗ Anh Tuấn"
 * "Nguyá»…n VÄƒn A" -> "Nguyễn Văn A"
 */

const WIN1252_REVERSE_MAP = {
  0x20AC: 0x80, 0x201A: 0x82, 0x0192: 0x83, 0x201E: 0x84, 0x2026: 0x85, 0x2020: 0x86, 0x2021: 0x87,
  0x02C6: 0x88, 0x2030: 0x89, 0x0160: 0x8A, 0x2039: 0x8B, 0x0152: 0x8C, 0x017D: 0x8E,
  0x2018: 0x91, 0x2019: 0x92, 0x201C: 0x93, 0x201D: 0x94, 0x2022: 0x95, 0x2013: 0x96, 0x2014: 0x97,
  0x02DC: 0x98, 0x2122: 0x99, 0x0161: 0x9A, 0x203A: 0x9B, 0x0153: 0x9C, 0x017E: 0x9E, 0x0178: 0x9F
}

export function isMojibake(str) {
  if (!str || typeof str !== 'string') return false
  return /Ä[\s\u0090]|á»|áº|Ã¢|Ã£|Ã¡|Ã©|Ã­|Ã³|Ãº|Ã½|Ã |Ã¨|Ã²|Ã¹|á»—|Tuáº¥n/.test(str)
}

export function fixVietnameseMojibake(str) {
  if (!str || typeof str !== 'string') return str
  if (!isMojibake(str)) return str

  // Chuẩn hóa 'Ä ' hoặc 'Ä\u0090' (ký tự 'Đ' trong UTF-8 khi bị lỗi)
  const normalized = str.replace(/Ä[\s\u0090]+/g, 'Đ ')

  // Thử giải mã trực tiếp
  const tryDecode = (input) => {
    const bytes = []
    for (let i = 0; i < input.length; i++) {
      const code = input.charCodeAt(i)
      if (code in WIN1252_REVERSE_MAP) {
        bytes.push(WIN1252_REVERSE_MAP[code])
      } else if (code <= 0xFF) {
        bytes.push(code)
      } else {
        return null
      }
    }
    try {
      return new TextDecoder('utf-8', { fatal: true }).decode(new Uint8Array(bytes))
    } catch {
      return null
    }
  }

  const direct = tryDecode(str)
  if (direct && !direct.includes('\ufffd')) {
    return direct
  }

  // Thử giải mã kèm thay thế 'Đ' và 'đ'
  const tryDecodeWithCustomChars = (input) => {
    const bytes = []
    for (let i = 0; i < input.length; i++) {
      const ch = input[i]
      if (ch === 'Đ') {
        bytes.push(0xC4, 0x90)
        continue
      }
      if (ch === 'đ') {
        bytes.push(0xC4, 0x91)
        continue
      }
      const code = input.charCodeAt(i)
      if (code in WIN1252_REVERSE_MAP) {
        bytes.push(WIN1252_REVERSE_MAP[code])
      } else if (code <= 0xFF) {
        bytes.push(code)
      } else {
        return null
      }
    }
    try {
      return new TextDecoder('utf-8', { fatal: true }).decode(new Uint8Array(bytes))
    } catch {
      return null
    }
  }

  const decodedWithD = tryDecodeWithCustomChars(normalized)
  if (decodedWithD && !decodedWithD.includes('\ufffd')) {
    return decodedWithD.replace(/Đ\s+([A-Za-z\u00C0-\u024F\u1EA0-\u1EF9])/g, 'Đ$1')
  }

  return str
}
