const MAX_SOURCE_BYTES = 10 * 1024 * 1024;
const CLIENT_SIZE = 640;

export async function prepareAvatar(file) {
  if (!file || !['image/jpeg', 'image/png'].includes(file.type)) {
    throw new Error('Chỉ hỗ trợ ảnh JPG hoặc PNG.');
  }
  if (file.size > MAX_SOURCE_BYTES) {
    throw new Error('Ảnh gốc tối đa 10 MB.');
  }

  const bitmap = await createImageBitmap(file);
  try {
    const side = Math.min(bitmap.width, bitmap.height);
    if (side < 32) throw new Error('Ảnh phải có kích thước tối thiểu 32×32 px.');
    const canvas = document.createElement('canvas');
    canvas.width = CLIENT_SIZE;
    canvas.height = CLIENT_SIZE;
    const context = canvas.getContext('2d', { alpha: false });
    context.fillStyle = '#ffffff';
    context.fillRect(0, 0, CLIENT_SIZE, CLIENT_SIZE);
    context.drawImage(
      bitmap,
      (bitmap.width - side) / 2,
      (bitmap.height - side) / 2,
      side,
      side,
      0,
      0,
      CLIENT_SIZE,
      CLIENT_SIZE,
    );
    const blob = await new Promise((resolve, reject) => canvas.toBlob(
      (result) => result ? resolve(result) : reject(new Error('Không thể nén ảnh đã chọn.')),
      'image/jpeg',
      0.82,
    ));
    return new File([blob], 'avatar.jpg', { type: 'image/jpeg' });
  } finally {
    bitmap.close();
  }
}

export function formatBytes(bytes) {
  if (!Number.isFinite(bytes)) return '';
  if (bytes < 1024) return `${bytes} B`;
  return `${(bytes / 1024).toFixed(bytes < 10240 ? 1 : 0)} KB`;
}
