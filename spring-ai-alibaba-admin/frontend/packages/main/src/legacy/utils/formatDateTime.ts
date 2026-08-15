import $i18n from '@/i18n';

const LOCALE_MAP: Record<string, string> = { zh: 'zh-CN', en: 'en-US', ja: 'ja-JP' };

/**
 * Parse API datetime values.
 * Backend Jackson WRITE_DATES_AS_TIMESTAMPS serializes LocalDateTime as
 * [year, month(1-12), day, hour, minute, second, nano].
 */
export function parseApiDateTime(value: unknown): Date | null {
  if (value == null || value === '') return null;

  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value as number[];
    if (year == null || month == null || day == null) return null;
    const date = new Date(year, month - 1, day, hour, minute, second);
    return Number.isNaN(date.getTime()) ? null : date;
  }

  if (typeof value === 'number') {
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? null : date;
  }

  const date = new Date(value as string);
  return Number.isNaN(date.getTime()) ? null : date;
}

/** Format API datetime for UI using the active language locale. */
export function formatDateTime(value: unknown): string {
  const date = parseApiDateTime(value);
  if (!date) return '-';

  const locale = LOCALE_MAP[$i18n.getCurrentLanguage()] || 'en-US';
  return date.toLocaleString(locale, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}
