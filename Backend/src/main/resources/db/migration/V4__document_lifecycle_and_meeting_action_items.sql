ALTER TABLE documents ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE meetings ADD COLUMN status VARCHAR(24) NOT NULL DEFAULT 'SCHEDULED';
CREATE TABLE meeting_action_items (
    id UUID PRIMARY KEY,
    meeting_id UUID NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    assignee_id UUID REFERENCES users(id),
    due_date DATE,
    status VARCHAR(24) NOT NULL DEFAULT 'OPEN',
    task_id UUID REFERENCES tasks(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_meeting_action_items_meeting ON meeting_action_items(meeting_id);
