CREATE OR REPLACE FUNCTION impedir_alteracao_movimento()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Movimento é imutável';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_movimento_immutavel_update
    BEFORE UPDATE ON movimento
    FOR EACH ROW
    EXECUTE FUNCTION impedir_alteracao_movimento();

CREATE TRIGGER trg_movimento_immutavel_delete
    BEFORE DELETE ON movimento
    FOR EACH ROW
    EXECUTE FUNCTION impedir_alteracao_movimento();