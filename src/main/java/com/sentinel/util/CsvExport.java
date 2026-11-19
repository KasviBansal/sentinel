package com.sentinel.util;

import com.sentinel.model.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class CsvExport {
    private CsvExport() { }

    public static void transactions(Path file, List<TransactionRow> rows) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            row(writer, "Transaction ID", "Date and Time IST", "Sender", "Sender Account", "Receiver", "Receiver Account",
                    "Amount INR", "Type", "Risk Score", "Risk Level", "Status");
            for (TransactionRow entry : rows) {
                Transaction t = entry.transaction();
                row(writer, t.displayId(), Formats.SQL_TIME.format(t.time()), entry.senderName(), t.sender(), entry.receiverName(),
                        t.receiver(), t.amount().toPlainString(), t.type(), t.evaluation().score(), t.evaluation().level(),
                        t.evaluation().status());
            }
        }
    }

    public static void alerts(Path file, List<FraudAlert> alerts) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            row(writer, "Alert ID", "Transaction ID", "Date and Time IST", "Customer", "Amount INR", "Risk Score",
                    "Reason", "Status", "Review Note");
            for (FraudAlert alert : alerts) {
                Transaction t = alert.row().transaction();
                row(writer, alert.displayId(), t.displayId(), Formats.SQL_TIME.format(t.time()), alert.row().senderName(),
                        t.amount().toPlainString(), alert.score(), alert.reason(), alert.status(), alert.note());
            }
        }
    }

    private static void row(BufferedWriter writer, Object... fields) throws IOException {
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) writer.write(',');
            String value = fields[i].toString();
            // Keep user-entered text from being interpreted as a formula in spreadsheet apps.
            String stripped = value.stripLeading();
            if (!stripped.isEmpty() && "=+-@".indexOf(stripped.charAt(0)) >= 0) value = "'" + value;
            writer.write("\"" + value.replace("\"", "\"\"") + "\"");
        }
        writer.write("\r\n");
    }
}
