CREATE OR REPLACE FUNCTION prevent_movement_update_delete()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'movements records are immutable: % is not allowed', TG_OP;
RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_movement_update
    BEFORE UPDATE ON movements
    FOR EACH ROW EXECUTE FUNCTION prevent_movement_update_delete();

CREATE TRIGGER trg_prevent_movement_delete
    BEFORE DELETE ON movements
    FOR EACH ROW EXECUTE FUNCTION prevent_movement_update_delete();