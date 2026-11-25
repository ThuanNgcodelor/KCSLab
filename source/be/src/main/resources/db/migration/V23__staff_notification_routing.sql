-- Bring existing transfer notices in line with the same safe reference used for new events.
UPDATE staff_notifications n
JOIN sample_transfers t ON n.entity_type='TRANSFER' AND n.entity_id=t.id
JOIN receipts r ON r.id=t.receipt_id
JOIN users sender ON sender.id=t.delivered_by_user_id
SET n.message=CONCAT(sender.display_name, ' đã chuyển ',
        (SELECT COUNT(*) FROM sample_transfer_items i WHERE i.transfer_id=t.id),
        ' mẫu của ', IF(r.request_number IS NULL OR TRIM(r.request_number)='',
            CONCAT('phiếu ',t.transfer_code),CONCAT('Số/No. ',r.request_number)), ' đến KCS')
WHERE n.type='TRANSFER_SUBMITTED';

-- Retain each recipient's original read state; add only missing copies for currently enabled Admins.
INSERT INTO staff_notifications
    (recipient_user_id,actor_user_id,type,title,message,target_url,entity_type,entity_id,dedupe_key,read_at,created_at)
SELECT admin.id, n.actor_user_id, n.type, n.title, n.message,
       CASE n.entity_type
           WHEN 'TRANSFER' THEN CONCAT('/admin/transfers/',n.entity_id)
           WHEN 'RECEIPT' THEN CONCAT('/admin/intake/',n.entity_id)
           WHEN 'TEST_REQUEST' THEN CONCAT('/admin/requests/',n.entity_id)
           ELSE n.target_url END,
       n.entity_type,n.entity_id,CONCAT(n.type,':',n.entity_type,':',n.entity_id,':',admin.id),NULL,n.created_at
FROM staff_notifications n
JOIN (SELECT MIN(id) AS id FROM staff_notifications
      WHERE entity_type IS NOT NULL AND entity_id IS NOT NULL
      GROUP BY type,entity_type,entity_id) first_notice ON first_notice.id=n.id
CROSS JOIN users admin
WHERE admin.role='ADMIN' AND admin.enabled=true
ON DUPLICATE KEY UPDATE dedupe_key=VALUES(dedupe_key);
