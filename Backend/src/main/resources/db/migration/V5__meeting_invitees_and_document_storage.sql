ALTER TABLE meetings ADD COLUMN timezone VARCHAR(80);
ALTER TABLE meetings ADD COLUMN external_link VARCHAR(1000);

CREATE TABLE meeting_invitees (
    meeting_id UUID NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (meeting_id, user_id)
);
CREATE INDEX idx_meeting_invitees_user ON meeting_invitees(user_id);
