-- =========================================================
-- V4__add_crew_configured_to_events.sql
-- Allow explicit crew assignment on events to override client defaults
-- =========================================================

ALTER TABLE events ADD COLUMN crew_configured BOOLEAN NOT NULL DEFAULT FALSE;

-- Mark existing events that already have event-specific crew assigned as configured
UPDATE events e SET crew_configured = TRUE 
WHERE EXISTS (
    SELECT 1 FROM employee_client_access eca 
    WHERE eca.event_id = e.id
);
