import test from 'node:test';
import assert from 'node:assert/strict';
import { fieldQuantityForSample } from './intakeModel.js';

test('matches quantities by sample name rather than field row order', () => {
  const field = { items: [{ sampleName: 'Mẫu B', quantity: '3 quả' }, { sampleName: 'Mẫu A', quantity: '500 g' }] };
  assert.equal(fieldQuantityForSample(field, 'Mẫu A', 2), '500 g');
  assert.equal(fieldQuantityForSample(field, 'Mẫu B', 2), '3 quả');
});
test('never copies a combined field quantity to every sample', () => {
  assert.equal(fieldQuantityForSample({ sampleQuantity: '10 kg', items: [] }, 'Mẫu A', 2), '');
  assert.equal(fieldQuantityForSample({ items: [{ sampleName: 'Mẫu B', quantity: '3 quả' }] }, 'Mẫu A', 2), '');
});
test('supports legacy single-sample field forms without guessing or changing units', () => {
  assert.equal(fieldQuantityForSample({ sampleQuantity: '2', items: [] }, 'Mẫu A', 1), '2');
  assert.equal(fieldQuantityForSample({ items: [{ sampleName: 'Tên hiện trường', quantity: '500 g' }] }, 'Tên yêu cầu', 1), '500 g');
  assert.equal(fieldQuantityForSample(null, 'Mẫu A', 1), '');
});
test('leaves ambiguous matches and explicit empty values blank', () => {
  assert.equal(fieldQuantityForSample({ items: [{ sampleName: 'A', quantity: '1 kg' }, { sampleName: 'A', quantity: '2 kg' }] }, 'A', 1), '');
  assert.equal(fieldQuantityForSample({ sampleQuantity: '3 kg', items: [{ sampleName: 'A', quantity: '' }] }, 'A', 1), '');
});
