export const notificationTypesFor = (role) => {
  switch (role) {
    case 'ADMIN': return ['TEST_REQUEST_CREATED', 'INTAKE_RECORDED', 'TRANSFER_SUBMITTED', 'TRANSFER_ACCEPTED', 'TRANSFER_REJECTED'];
    case 'RECEPTIONIST': return ['TEST_REQUEST_CREATED', 'TRANSFER_ACCEPTED', 'TRANSFER_REJECTED'];
    case 'TECHNICIAN': return ['TRANSFER_SUBMITTED'];
    default: return [];
  }
};
