const dateTimeFormatter = new Intl.DateTimeFormat('vi-VN', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hour12: false
});

function partValue(parts, type) {
  return parts.find((part) => part.type === type)?.value;
}

export function formatDateTime(value, fallback = '—') {
  if (value === null || value === undefined || value === '') {
    return fallback;
  }

  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) {
    return fallback;
  }

  const parts = dateTimeFormatter.formatToParts(date);
  const day = partValue(parts, 'day');
  const month = partValue(parts, 'month');
  const year = partValue(parts, 'year');
  const hour = partValue(parts, 'hour');
  const minute = partValue(parts, 'minute');
  const second = partValue(parts, 'second');

  if (!day || !month || !year || !hour || !minute || !second) {
    return fallback;
  }

  return `${day}/${month}/${year} ${hour}:${minute}:${second}`;
}
