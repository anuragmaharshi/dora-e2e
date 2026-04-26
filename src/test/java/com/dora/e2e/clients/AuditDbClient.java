package com.dora.e2e.clients;

import com.dora.e2e.support.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Direct JDBC client used exclusively by the immutability scenario (AC-2 / AC-3).
 *
 * <p>This client attempts a raw {@code UPDATE audit_log SET action='TAMPERED' WHERE entity_id=?}
 * and reports whether the database trigger rejected it.  All application-layer paths
 * are bypassed deliberately — this proves the DB-level constraint, not the app layer.
 *
 * <p>Connection parameters:
 * <ul>
 *   <li>JDBC URL — system property {@code db.url}, default {@code jdbc:postgresql://localhost:5432/dora}</li>
 *   <li>User     — system property {@code db.user}, default {@code dora}</li>
 *   <li>Password — system property {@code db.password}, default {@code dora}</li>
 * </ul>
 *
 * <p>If the database is unreachable (connection refused), {@link #tryTamperByEntityId}
 * throws {@link DbUnreachableException}, which the step definition catches and uses to
 * mark the scenario as pending rather than failing the suite.
 */
public class AuditDbClient {

    /**
     * Marker exception thrown when the DB is unreachable.
     * Step definitions catch this and pend the scenario.
     */
    public static final class DbUnreachableException extends RuntimeException {
        public DbUnreachableException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** Result of a tamper attempt. */
    public enum TamperResult {
        /** The trigger raised an exception — immutability confirmed. */
        TRIGGER_REJECTED,
        /** The UPDATE executed but affected 0 rows — row absent or no permissions. */
        ZERO_ROWS_AFFECTED,
        /** The UPDATE unexpectedly succeeded and modified rows — immutability BROKEN. */
        ROWS_AFFECTED
    }

    /**
     * Attempts {@code UPDATE audit_log SET action='TAMPERED' WHERE entity_id=?} directly
     * via JDBC, bypassing all application-layer constraints.
     *
     * @param entityId the UUID string of the probe entity
     * @return {@link TamperResult} indicating whether the trigger rejected the attempt
     * @throws DbUnreachableException if the database cannot be reached
     */
    public TamperResult tryTamperByEntityId(String entityId) {
        String url      = System.getProperty("db.url",      Config.DB_JDBC_URL);
        String user     = System.getProperty("db.user",     Config.DB_USER);
        String password = System.getProperty("db.password", Config.DB_PASSWORD);

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE audit_log SET action = 'TAMPERED' WHERE entity_id = ?::uuid")) {
                ps.setString(1, entityId);
                int rows = ps.executeUpdate();
                conn.rollback(); // safety rollback even if it somehow committed
                return rows > 0 ? TamperResult.ROWS_AFFECTED : TamperResult.ZERO_ROWS_AFFECTED;
            } catch (SQLException sqlEx) {
                // Postgres error code 'P0001' = RAISE EXCEPTION from a trigger
                conn.rollback();
                return TamperResult.TRIGGER_REJECTED;
            }
        } catch (SQLException connEx) {
            // Connection-level failure — DB is not reachable
            throw new DbUnreachableException(
                    "Cannot connect to the database at " + url +
                    ". Ensure the PostgreSQL container is running. " +
                    "Root cause: " + connEx.getMessage(), connEx);
        }
    }
}
