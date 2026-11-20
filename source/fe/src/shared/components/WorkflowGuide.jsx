const stages = [
  ['request', 'Yêu cầu kiểm nghiệm'],
  ['sampling', 'Lấy mẫu'],
  ['intake', 'Tiếp nhận'],
  ['handover', 'Bàn giao KCS'],
  ['testing', 'Thử nghiệm'],
  ['results', 'Kết quả'],
];

export function WorkflowGuide({ stage }) {
  return (
    <ol className="workspace-workflow" aria-label="Các công đoạn xử lý mẫu">
      {stages.map(([key, label], index) => (
        <li key={key} aria-current={key === stage ? 'step' : undefined}>
          <span aria-hidden="true">{index + 1}</span>{label}
        </li>
      ))}
    </ol>
  );
}
