const ALLOWED_FORMATS = [
  'mp4',
  'mov',
  'avi',
  'mkv',
  'mp3',
  'wav',
  'm4a',
  'webm',
]

const ALLOWED_MIME_TYPES = [
  'video/mp4',
  'video/quicktime',
  'video/x-msvideo',
  'video/x-matroska',
  'video/webm',
  'audio/mpeg',
  'audio/wav',
  'audio/x-m4a',
  'audio/mp4',
]

const MAX_FILE_SIZE = 2 * 1024 * 1024 * 1024 // 2GB

export interface ValidationResult {
  valid: boolean
  error?: string
}

export function validateFile(file: File): ValidationResult {
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (!ext || !ALLOWED_FORMATS.includes(ext)) {
    return {
      valid: false,
      error: `不支持的文件格式 ".${ext || 'unknown'}"，支持: ${ALLOWED_FORMATS.join(', ')}`,
    }
  }

  if (file.size > MAX_FILE_SIZE) {
    return {
      valid: false,
      error: `文件大小超出限制，最大支持 2GB`,
    }
  }

  return { valid: true }
}

export function getAcceptedFormats(): Record<string, string[]> {
  return {
    'video/*': ['.mp4', '.mov', '.avi', '.mkv', '.webm'],
    'audio/*': ['.mp3', '.wav', '.m4a'],
  }
}

export function getAcceptString(): string {
  return ALLOWED_MIME_TYPES.join(',')
}
