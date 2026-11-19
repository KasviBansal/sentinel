# SENTINEL

**Rule-Based Fraud Detection System**  
*Monitor. Detect. Protect.*

SENTINEL is a JavaFX desktop application for the UCS553 Enterprise Web Application mini-project. Administrators can record transfers, understand how each risk score was calculated, review fraudulent activity, manage customer profiles and export reports. The demo contains only synthetic data.

## Run locally

Install **JDK 21** and **Maven 3.9+**, and make sure `mvn -version` shows Java 21. Extract the project, open a terminal in the `sentinel` directory, then run these commands. Maven requires internet access on the first build to download dependencies.

```text
mvn clean verify
mvn javafx:run
```

| Demo sign-in | Value |
|---|---|
| Email | `admin@sentinel.demo` |
| Password | `Admin@123` |

The first launch creates `data/sentinel.db` with 50 customer profiles, 60 account references, 500 transfers, four rules, and ten blacklist entries. The sample timeline spans 60 calendar days ending on 18 November 2026, using Indian Standard Time. First-run seeding uses the same evaluator as manual transfers. Later launches reopen the existing database.

## What each screen does

| Screen | Workflows |
|---|---|
| Dashboard | Date-filtered KPIs, daily transfer volume, fraud distribution, payment-channel counts, and recent alert details |
| Transactions | Search, risk and type filters, sortable ledger, transfer submission, and saved rule explanations |
| Fraud Alerts | Search, status filter, review notes, and New / Under Review / Resolved workflow |
| Fraud Rules | Enable or disable each rule, and change the amount threshold or outgoing-transfer limit |
| Customers | Add or edit profiles, open account history, and deactivate accounts while retaining history |
| Reports | Inclusive date-range statistics, rule trigger counts, and separate UTF-8 CSV exports |

Double-click a ledger row or use the detail button. Table headers sort the results. Dashboard periods end at the later of the demo date and the newest transfer date, so newly recorded transfers can appear in the default period. Reports use the selected dates.

## How it works

```text
JavaFX view → Controller → Service → JDBC DAO → SQLite
                                ↓
                             FraudRule
```

| Rule | Default match condition | Points |
|---|---|---:|
| High Amount | Amount strictly above ₹50,000 | 30 |
| Frequent Transactions | More than five outgoing transfers within ten minutes, including this transfer | 30 |
| Blacklisted Account | Sender or receiver is listed | 50 |
| Unusual Time | At or after 01:00 and before 05:00 IST | 20 |

The total is capped at 100. Risk bands are Low (0–29), Medium (30–59), High (60–79) and Critical (80–100). A score of 30 creates an alert and marks the transaction Suspicious. Frequencies include stored outgoing transfers at both ends of the ten-minute window and count the submitted transfer once. If you enter a backdated transfer, the engine uses the history present at submission time. Saved scores and explanations remain associated with the rule configuration used when they were evaluated.

SQLite stores integer paise and the application uses `BigDecimal` for exact rupee values. Ten demo customers have a secondary account. The customer grid shows the primary account. Transaction forms and customer history use all accounts linked to the profile. Primary account numbers are fixed after creation to preserve references.

Application logs go to `data/sentinel.log`. Bundled defaults are in `src/main/resources/config.properties`. The report date preference is saved as a small, type-filtered `settings.ser` under the app data directory. The app loads that file only from its own configured data directory. SQLite is the source for customer, transaction, rule and alert data.

To run with a separate demo database, launch `mvn javafx:run` from a separate copy of the project or set the `sentinel.data.dir` JVM system property in your IDE run configuration.

## Team walkthrough

| Member | Owns the walkthrough | Source |
|---|---|---|
| 1 | Database, tables, JDBC, seeding, and CRUD | `database`, `dao`, `DemoData` |
| 2 | Models, validation, fraud rules, scoring, and exceptions | `model`, `rules`, `service`, `exception` |
| 3 | JavaFX navigation, charts, controls, styling, and integration | `controller`, `resources/fxml`, `resources/css` |

- [Viva guide, class diagram, ER diagram, and flowchart](docs/VIVA_GUIDE.md)
- [Demo script](docs/DEMO.md)
- [Build and verification record](PROGRESS.md)
