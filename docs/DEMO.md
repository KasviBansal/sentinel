# SENTINEL demonstration

Use a freshly seeded database for predictable scores. Give all three students a turn explaining the flow.

1. Start the application with `mvn javafx:run`. Show empty-field validation and then sign in as `admin@sentinel.demo` with `Admin@123`.
2. Start on **Financial Security Overview**. Switch between **Last 7 days** and **All dates**. Point out how the cards, charts, and recent alerts change together. Open a recent alert to see its evaluation.
3. In **Customers**, add **Aarya Das**, `aarya.das@example.test`, `9000012345`, account `654321009876`, city **Kolkata**. Search for Aarya and edit the city if time allows.
4. In **Transactions**, use Aarya as sender and Priya Nair’s primary account `701809000002` as receiver. Use a date outside the seeded period, such as 19 November 2026, for the following examples. Enter the times shown and choose **UPI**.

   | Time | Amount (INR) | Expected result |
   |---|---:|---|
   | 11:00 | 1250.50 | 0, Low, Normal |
   | 11:30 | 75000 | 30, Medium, Suspicious |
   | 02:30 | 750 | 20, Low, Normal, with an Unusual Time explanation |

5. Enter a ₹100 transfer from Aarya at 12:00 to blacklisted account `701809000051` (Aarav Sharma’s secondary account). Expect 50 points and a Medium Suspicious result. To demonstrate frequency, add six ₹100 transfers from Aarya to Priya at 13:00, 13:01, 13:02, 13:03, 13:04, and 13:05 on that same date. The sixth transfer should score 30 for frequency.
6. In **Fraud Alerts**, search for the ID of the ₹75,000 transfer. Save **Under Review** with a note. Reopen it and save **Resolved**.
7. In **Fraud Rules**, edit **High Amount**, set the threshold to `100000`, and save. Submit another ₹75,000 transfer at 16:00 from Aarya to Priya. It should score zero. Look up the earlier ₹75,000 transfer and show its original score and explanation.
8. In **Reports**, include the demo transaction date in the selected range and refresh. Export both CSVs and open them in a text editor or spreadsheet application. Show the resolved alert and the rule trigger totals.
9. Return to **Customers**, view Aarya’s transactions, and deactivate the customer. Reopen the history, then restart SENTINEL and verify the customer, saved transfers, alert review, and threshold are still present.
