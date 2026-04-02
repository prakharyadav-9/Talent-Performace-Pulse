-- =============================================================================
--  docker/postgres/init/01_init.sql
--  Runs automatically when the postgres container starts for the first time.
--
--  LLD §4 – Database Schema & Indexing Strategy
--
--  This file is the placeholder for the full DDL that will be added as
--  each entity module is implemented per the LLD.
--
--  Execution order follows numeric prefix: 01_ → 02_ → 03_ …
--  Add per-module DDL files as:
--    02_employees.sql
--    03_review_cycles.sql
--    04_performance_reviews.sql
--    05_goals.sql
--    06_indexes.sql
-- =============================================================================

-- Ensure the database is clean on first run
-- (postgres container handles DB creation via POSTGRES_DB env var)

-- Enable UUID generation (required for UUID primary keys)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Placeholder: future tables will be created by Flyway / Liquibase
-- when added in the LLD extension phase.
-- For now, Spring Boot (ddl-auto=create-drop on dev, validate on prod)
-- manages the schema.

SELECT 'Performance Pulse DB initialised.' AS status;
