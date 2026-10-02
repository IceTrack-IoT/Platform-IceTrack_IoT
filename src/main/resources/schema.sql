-- Schema bootstrap, executed by Spring Boot before JPA creates the EntityManagerFactory
-- (see spring.sql.init.mode=always in application.properties).
--
-- Hibernate's ddl-auto=update creates TABLES but never SCHEMAS: its generated DDL for
-- @Table(schema = "assets") is `create table assets.sites (...)`, which PostgreSQL rejects with
-- `ERROR: schema "assets" does not exist`. Hibernate logs that failure as a WARNING and boots
-- anyway, so the application starts healthy while every query against assets.sites fails with
-- `relation "assets.sites" does not exist`.
--
-- Creating the schemas here, once and idempotently, is what makes ddl-auto=update able to build
-- the tables. Every statement is CREATE SCHEMA IF NOT EXISTS, so this is safe to run on every
-- startup and against an existing database: it never drops nor alters anything.

-- Mapped explicitly by SitePersistenceEntity and EquipmentPersistenceEntity
-- (@Table(schema = "assets")). Bounded context namespace for sites and equipments.
CREATE SCHEMA IF NOT EXISTS assets;

-- Bounded context namespaces reserved for the rest of the platform. No entity maps to them yet
-- (every other table lives in the default schema, public); they are created here so a context can
-- be isolated by changing its @Table(schema = ...) without touching the database first.
CREATE SCHEMA IF NOT EXISTS iam;
CREATE SCHEMA IF NOT EXISTS profiles;
CREATE SCHEMA IF NOT EXISTS monitoring;
CREATE SCHEMA IF NOT EXISTS shared;