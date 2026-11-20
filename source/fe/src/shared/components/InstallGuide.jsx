export function InstallGuide() {
  if (window.matchMedia('(display-mode: standalone)').matches || navigator.standalone) return null;

  return <details className="install-guide">
    <summary>Cài CFC Lab trên điện thoại</summary>
    <div>
      <p><strong>iPhone / iPad.</strong> Mở bằng Safari, chọn Chia sẻ → Thêm vào MH chính. Bật Mở dưới dạng ứng dụng web nếu Safari có tùy chọn này rồi chọn Thêm.</p>
      <p><strong>Android.</strong> Mở bằng Chrome, vào menu ⋮ rồi chọn Cài đặt ứng dụng hoặc Thêm vào màn hình chính.</p>
    </div>
  </details>;
}
