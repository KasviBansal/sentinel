package com.sentinel;

import com.sentinel.database.Database;
import com.sentinel.exception.DatabaseException;
import com.sentinel.exception.InvalidTransactionException;
import com.sentinel.model.*;
import com.sentinel.service.AppServices;
import com.sentinel.util.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class PersistenceTest {
    @TempDir Path directory;
    private static final LocalDate DEMO = LocalDate.of(2026, 11, 18);
    private static final String A = "701809000001";
    private static final String B = "701809000002";
    private static final String BLACKLISTED = "701809000051";

    private AppConfig config() { return new AppConfig(directory, directory.resolve("test.db"), DEMO); }

    @Test void seedAndWorkflowPersistAcrossReopen() throws Exception {
        AppServices app = AppServices.initialize(config());
        assertTrue(app.auth().login(" ADMIN@SENTINEL.DEMO ", "Admin@123"));
        assertFalse(app.auth().login("admin@sentinel.demo", "incorrect"));
        assertFalse(app.auth().login("", ""));
        assertEquals(50, app.customers().findAll().size());
        assertEquals(60, app.customers().activeAccounts().size());
        assertEquals(4, app.monitoring().rules().size());

        var seeded = app.monitoring().snapshot(DEMO.minusDays(59), DEMO);
        assertEquals(500, seeded.transactions().size());
        assertEquals(500, seeded.daily().values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(500, seeded.types().values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(seeded.suspicious(), seeded.alerts().size());
        for (RuleType type : RuleType.values()) assertTrue(seeded.ruleCounts().get(type) > 0, type.name());

        Customer added = new Customer(0, "Aarya Das", "aarya.das@example.test", "9000012345", "654321009876", "Kolkata", true);
        app.customers().save(added);
        assertThrows(IllegalArgumentException.class, () -> app.customers().save(added));
        Customer saved = app.customers().findAll().stream().filter(c -> c.email().equals(added.email())).findFirst().orElseThrow();
        app.customers().save(new Customer(saved.id(), saved.name(), saved.email(), saved.phone(), saved.account(), "Pune", true));

        LocalDate day = DEMO.plusDays(1);
        LocalDateTime noon = day.atTime(12, 0);
        assertThrows(InvalidTransactionException.class, () -> create(app, A, A, "1", noon));
        assertThrows(InvalidTransactionException.class, () -> create(app, A, "unknown", "1", noon));
        assertThrows(InvalidTransactionException.class, () -> create(app, A, B, "-1", noon));
        assertThrows(InvalidTransactionException.class, () -> create(app, A, B, "1.001", noon));
        var normal = create(app, saved.account(), B, "17.62", noon);
        assertEquals(1762, normal.amountPaise());
        assertFalse(normal.evaluation().suspicious());
        var highAmount = create(app, A, B, "75000", noon);
        assertEquals(30, highAmount.evaluation().score());
        var overnight = create(app, A, B, "500", day.atTime(2, 0));
        assertEquals(20, overnight.evaluation().score());
        assertFalse(overnight.evaluation().suspicious());
        assertEquals(50, create(app, A, BLACKLISTED, "500", noon).evaluation().score());
        assertEquals(80, create(app, A, BLACKLISTED, "75000", noon).evaluation().score());

        // Exercise the exact ten-minute history boundary, also with a future row already inserted.
        create(app, B, A, "50", noon.plusHours(5));
        for (int i = 0; i < 5; i++) {
            assertEquals(0, create(app, B, A, "50", noon.plusMinutes(i)).evaluation().score());
        }
        create(app, B, A, "50", noon.minusSeconds(1));
        var burst = create(app, B, A, "50", noon.plusMinutes(10));
        assertEquals(30, burst.evaluation().score());
        for (int i = 0; i < 5; i++) create(app, BLACKLISTED, A, "50", day.atTime(1, i));
        assertEquals(100, create(app, BLACKLISTED, A, "75000", day.atTime(1, 5)).evaluation().score());

        var alert = app.monitoring().snapshot(day, day).alerts().stream()
                .filter(a -> a.row().transaction().id() == highAmount.id()).findFirst().orElseThrow();
        app.monitoring().review(alert.id(), AlertStatus.UNDER_REVIEW, "Verification pending.");
        assertTrue(app.monitoring().snapshot(day, day).alerts().stream().anyMatch(a -> a.id() == alert.id()
                && a.status() == AlertStatus.UNDER_REVIEW));
        app.monitoring().review(alert.id(), AlertStatus.RESOLVED, "Confirmed, \"manual\" review.\nAuthorised.");
        app.monitoring().saveRule(new RuleConfig(RuleType.HIGH_AMOUNT, 100_000, true, 30));
        assertEquals(0, create(app, A, B, "75000", noon.plusHours(3)).evaluation().score());
        app.monitoring().saveRule(new RuleConfig(RuleType.FREQUENCY, 5, false, 30));
        assertEquals(0, create(app, B, A, "50", noon.plusMinutes(10)).evaluation().score());
        app.customers().save(new Customer(saved.id(), saved.name(), saved.email(), saved.phone(), saved.account(), "Pune", false));
        assertThrows(InvalidTransactionException.class, () -> create(app, saved.account(), B, "50", noon));
        assertEquals(1, app.customers().history(saved).size());

        var all = app.monitoring().snapshot(null, null);
        var restarted = AppServices.initialize(config());
        var reopened = restarted.monitoring().snapshot(null, null);
        assertEquals(all.transactions().size(), reopened.transactions().size());
        assertEquals(all.totalValue(), reopened.totalValue());
        assertTrue(reopened.alerts().stream().anyMatch(a -> a.id() == alert.id() && a.score() == 30
                && a.status() == AlertStatus.RESOLVED && a.note().contains("Authorised.")));
        assertTrue(restarted.monitoring().rules().stream().anyMatch(rule -> rule.type() == RuleType.HIGH_AMOUNT
                && rule.threshold() == 100_000));
        assertTrue(restarted.customers().findAll().stream().anyMatch(c -> c.id() == saved.id() && !c.active() && c.city().equals("Pune")));

        var transfersFile = directory.resolve("transfers.csv");
        var alertsFile = directory.resolve("alerts.csv");
        CsvExport.transactions(transfersFile, reopened.transactions());
        CsvExport.alerts(alertsFile, reopened.alerts());
        assertTrue(Files.readString(transfersFile).contains(normal.displayId()));
        assertTrue(Files.readString(alertsFile).contains("Confirmed, \"\"manual\"\" review.\nAuthorised."));
        var settings = new SettingsBackup("2026-11-01", "2026-11-18");
        settings.save(directory);
        assertEquals(settings, SettingsBackup.load(directory));
    }

    @Test void rollsBackTheTransferIfAlertInsertFails() {
        AppServices app = AppServices.initialize(config());
        new Database(config().database()).write(connection -> {
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("""
                        CREATE TRIGGER reject_test_alert BEFORE INSERT ON fraud_alerts
                        BEGIN SELECT RAISE(ABORT, 'Test alert failure'); END
                        """);
            }
            return null;
        });
        assertThrows(DatabaseException.class, () -> create(app, A, B, "75000", DEMO.plusDays(1).atTime(12, 0)));
        assertEquals(500, app.transfers().findAll().size());
    }

    private Transaction create(AppServices app, String sender, String receiver, String amount, LocalDateTime time)
            throws InvalidTransactionException {
        return app.transfers().create(sender, receiver, new BigDecimal(amount), TransferType.UPI, time);
    }
}
