# Bank Release Rescue

An IDE-only, intermediate GitHub Copilot workshop for individual Java or Rust learners.

Participants investigate and fix a duplicate-transfer defect in a fictional bank's internal-transfer API. The kit contains equivalent local starter projects, a participant workbook, facilitator notes, and synthetic acceptance fixtures.

## Workshop kit

- `attendee-workbook.md` — agenda, five labs, prompts, checkpoints, and worksheets.
- `facilitator-guide.md` — setup, answer key, exercise twists, fallback plans, and scoring rubric.
- `shared/api-contract.md` — API and business rules shared by both tracks.
- `shared/fixtures/acceptance-cases.json` — language-neutral synthetic cases.
- `java/` — Java 21 + Spring Boot starter and JUnit tests.
- `rust/` — Rust + Axum starter and Cargo tests.

The attendee distribution should contain `README.md`, `attendee-workbook.md`, `shared/`, and exactly one of `java/` or `rust/`. Keep `facilitator-guide.md` and `facilitator/` facilitator-only; they include the reference fix.

## Start here

1. Choose one track; the exercises are individual.
2. Read `shared/api-contract.md`, then the attendee workbook.
3. Use a clean copy of the starter project. The current starter tests should pass.
4. The starter intentionally ignores transfer idempotency keys. Do not add a replay test until Lab 3; that test should fail before the fix.
5. Keep all data synthetic. Do not use production source, account information, secrets, or real banking integrations.

The sample service is an educational, process-local simulation. It is not production-ready financial software.

## Toolchain and IDE preflight

The Java track requires Java 21 and Maven. Open `java/` as a Maven project.

```powershell
cd java
mvn test
mvn spring-boot:run
```

The Rust track requires a current stable Rust toolchain with Cargo. Open `rust/` as a Cargo project.

```powershell
cd rust
cargo test
cargo run
```

Both servers listen on `127.0.0.1:8080`. Use separate runs or stop the first server before starting the other.

The generated kit does not include Maven Wrapper binaries. Install Maven on the workshop machine or pre-provision it before class. Pre-fetch dependencies and verify builds in the exact attendee environment.

GitHub Copilot must be enabled in the IDE and permitted by the attendee's organization. IDE-only means no GitHub website or hosted workflow is needed; Copilot itself may still require an account, approved model access, and network connectivity. IDE surfaces differ: use explicit chat context and terminal commands if automatic instructions or agent mode are unavailable.

## API smoke check

In another PowerShell terminal, with the service running:

```powershell
$body = '{"sourceAccountId":"checking-001","destinationAccountId":"savings-001","amountMinor":1200}'
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8080/transfers -Headers @{'Idempotency-Key'='workshop-transfer-001'} -ContentType 'application/json' -Body $body
Invoke-RestMethod -Uri http://127.0.0.1:8080/accounts/checking-001
```

Repeat the POST with the same key. In the starter, both requests succeed but create distinct transfers and debit the source twice. That is the workshop defect.

## Boundaries

- One currency; amounts are integer minor units.
- The lab's target is sequential duplicate-request handling in one running process.
- The starter is in-memory and resets on restart.
- No production authentication, durable ledger, cross-instance guarantees, payment integration, deployment, CI exercise, or operations lab is included.
- Avoid printing idempotency keys or request bodies in logs.
