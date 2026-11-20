ALTER TABLE staff_notifications
    ADD COLUMN actor_user_id BIGINT NULL,
    ADD CONSTRAINT fk_staff_notifications_actor
        FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE SET NULL;
