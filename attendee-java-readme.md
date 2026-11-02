# Northstar Bank workshop — Java participant package

This package contains the Java starter and participant materials for the IDE-only Bank Release Rescue workshop. Work individually in this language track.

## Start

Requirements: Java 21, Maven, an approved IDE, and GitHub Copilot enabled under your organization's policy.

```powershell
cd java
mvn test
mvn spring-boot:run
```

The service listens on `127.0.0.1:8080`. Dependencies should be pre-fetched before the workshop.

```powershell
$body = '{"sourceAccountId":"checking-001","destinationAccountId":"savings-001","amountMinor":1200}'
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8080/transfers -Headers @{'Idempotency-Key'='workshop-transfer-001'} -ContentType 'application/json' -Body $body
Invoke-RestMethod -Uri http://127.0.0.1:8080/accounts/checking-001
```

The starter is designed for its baseline tests to pass, but replaying a successful request with the same `Idempotency-Key` currently moves funds twice. Lab 3 asks you to expose and fix that defect.

The application is a synthetic, in-memory teaching sample—not production banking software. Do not enter real customer information, credentials, or payment data. Copilot may require account access and network connectivity even though all coding exercises run locally.

Read `shared/api-contract.md` and `attendee-workbook.md` before beginning.
