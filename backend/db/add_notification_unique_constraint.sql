-- Add unique constraint to prevent duplicate friend request notifications
-- Only applies to friend request notifications to the same user from the same requester

ALTER TABLE notifications 
ADD CONSTRAINT unique_friend_request_notification 
UNIQUE NULLS NOT DISTINCT (user_id, type, reference_id) 
WHERE type = 'FRIEND_REQUEST';

-- Create index for better performance
CREATE INDEX idx_notifications_user_type_ref 
ON notifications(user_id, type, reference_id) 
WHERE type = 'FRIEND_REQUEST';
