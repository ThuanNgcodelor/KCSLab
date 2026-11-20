import { splitMethods, splitParameters } from '../parameters';

/** Một mẫu có nhiều chỉ tiêu: mỗi chỉ tiêu một hàng, các cột của mẫu gộp theo chiều dọc. */
export function ParameterRows({ item, columns }) {
  const parameters = splitParameters(item.requestedParameters);
  const methods = splitMethods(item.testMethod);
  const rows = parameters.length ? parameters : ['—'];
  const paired = methods.length === rows.length;
  return rows.map((parameter, index) => <tr key={parameter}>
    {index === 0 && columns(rows.length)}
    <td data-label="Chỉ tiêu">{parameter}</td>
    {paired ? <td data-label="Phương pháp">{methods[index]}</td> : index === 0 && <td data-label="Phương pháp" rowSpan={rows.length}>{methods.join(', ') || '—'}</td>}
  </tr>);
}

/** Danh sách chỉ tiêu, mỗi chỉ tiêu một dòng. */
export function ParameterList({ value }) {
  const parameters = splitParameters(value);
  if (!parameters.length) return <small>Chưa ghi chỉ tiêu</small>;
  return <ul className="parameter-list">{parameters.map((parameter) => <li key={parameter}>{parameter}</li>)}</ul>;
}
