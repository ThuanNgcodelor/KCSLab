import { Icon } from '../../../shared/components/Icon';
import { UserAvatar } from '../../profile/components/UserAvatar';

const typeIcons = {
  INTAKE_RECORDED: 'inbox',
  TRANSFER_SUBMITTED: 'send',
  TRANSFER_ACCEPTED: 'checkCircle',
};

export function NotificationAvatar({ notification }) {
  if (notification.actor) {
    return <UserAvatar user={notification.actor} className="notification-type-icon notification-actor" />;
  }
  return <span className="notification-type-icon" aria-hidden="true">
    <Icon name={typeIcons[notification.type] ?? 'alert'} size={16} />
  </span>;
}
