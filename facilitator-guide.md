# Facilitator guide: Bank Release Rescue

## Purpose and boundaries

This 3.5-hour IDE-only workshop teaches experienced developers to use GitHub Copilot across discovery, design, implementation, testing, and local review. Attendees work individually in a Java or Rust track. It is intentionally not a GitHub-platform, CI/CD, deployment, release, or operations workshop.

The service simulates a narrow internal-transfer workflow using fictional accounts and process-local state. It is not suitable for real funds or production use.

## Before class

### Prepare machines

- Java track: Java 21, Maven, IDE Java support, and approved Copilot integration.
- Rust track: stable Rust/Cargo, Rust Analyzer or equivalent IDE support, and approved Copilot integration.
- Pre-fetch Maven and Cargo dependencies; verify in a clean attendee-like environment.
- Confirm each IDE supports chat and local file/terminal use. Validate instruction-file discovery per IDE version; do not assume parity.
- Confirm Copilot entitlement, organization policy, permitted models, and network access. No GitHub website access, repository permissions, Actions, or cloud-agent setup is necessary.
- Pre-open the chosen project and warm dependency caches before the workshop.
- Keep reference fixes and facilitator-only answer notes outside participant view.

### Smoke test

From `java/`, run `mvn test`; from `rust/`, run `cargo test`. Both starter suites should pass. Then run one service, check a balance, and submit a transfer. Repeat with the same key and amount: the starter should create two different transfer IDs and debit twice. Reset by restarting the process; data is in memory.

The authoring environment for this kit did not have Java/Maven or Rust/Cargo installed, so those commands were not executed here. Run this smoke test before class and resolve any toolchain or dependency issue before attendees arrive.

## Run of show

| Segment | Duration | Facilitation notes |
|---|---:|---|
| Welcome / preflight | 15m | Set safety boundary; have everyone run baseline tests |
| Lab 1 | 30m | Require code/test evidence; allow multiple hypotheses |
| Lab 2 | 25m | Compare designs; emphasize explicit context |
| Break | 10m | |
| Lab 3 | 50m | Keep test-first order; capture before/after failure |
| Break | 10m | |
| Lab 4 | 30m | Mutation is local and reversible; provide faulty snapshot as fallback |
| Lab 5 | 30m | Treat Copilot findings as proposals, not an oracle |
| Reflection | 10m | Ask where human review mattered |

## Answer key

### Lab 1

The HTTP handler accepts an idempotency key but starter code passes it only as a required argument / header check. The transfer service currently does not record or look up a key. Each successful call creates a new UUID and moves balances again. This is the defect.

Expected criteria: replaying a successful key+same payload returns same transfer ID and changes balances only once; reusing a key with changed payload returns 409 and changes nothing; failed transfers do not reserve the key; missing/blank key rejects; the existing validation cases remain correct.

The outdated design note says all transfer retries are deduplicated. That is not supported by the current service or tests. Participants should label it outdated/unsupported rather than assume intent from prose.

### Lab 2

Preferred simple design: keep an in-memory map in the transfer service from key to original request tuple plus completed response. Validate key and compare existing record before applying a transfer. For a new key, validate accounts, amount, funds, and overflow; compute both new balances before mutation; mutate balances; then store the idempotency record. A changed tuple is a conflict. A failed request should not be recorded as completed.

The contract requires sequential process-local behavior. In Java, a synchronized service method can keep check-and-mutate atomic within this single process. In Rust, keep lookup and mutation under the same mutex guard. Explain that this does not provide restart or multi-instance durability. If using different code structure, accept it if required behavior and tests pass.

### Lab 3

A good regression test uses a unique key, submits the same request twice, compares transfer IDs, and asserts balances after both calls. It should fail on the original code because IDs differ and balances move twice.

Changed payload tests should assert `409` and unchanged balances. Overflow must be checked before either account changes. Do not accept tests that assert only a status code or tests that silently remove previous validation.

### Lab 4

Good candidate edge cases include blank idempotency key, changed destination, changed source, overflow, unknown source/destination, same account, and a failed request followed by a corrected request with the same key. The controlled mutation should make at least one assertion fail. Ensure participants restore only the deliberate mutation; use a supplied copy if they are unsure.

### Lab 5

The correct review result may include rejecting a suggestion that broadens scope to database persistence or claims cross-instance idempotency. Look for:

- Key record stored before validation / before transfer commits.
- Source debit occurs before destination overflow check.
- Key compared only to amount, ignoring account IDs.
- Retry returns a new ID.
- Failure responses mutate state.
- Request body, account identifiers, or idempotency key are logged unnecessarily.
- Tests use shared keys and leak state across test cases.

Do not grade on whether Copilot independently finds the planted issue. Grade on evidence and reasoning.

## Facilitator-only twists and recovery

- **Discovery:** outdated note conflicts with implementation and test evidence.
- **Lab 3:** a weak example test checks only status. Ask the attendee to strengthen it.
- **Lab 4:** use a disposable broken snapshot with replay lookup bypassed. Avoid asking learners to use destructive Git commands.
- **Lab 5:** optional local patch contains a deterministic unsafe log statement or misses one validation edge. Give it only after the learner completes their own review.
- **Feature fallback:** if agent mode is unavailable, have Copilot Chat propose a patch and let the attendee apply it manually. If instruction discovery differs, explicitly attach the guidance file. If IDE diff support is weak, compare with the prepared baseline.
- **Toolchain failure:** do not spend workshop time installing compilers. Switch to a verified prepared machine/project; the functional objective matters more than setup.

## Assessment rubric (0–2 each; 10 total)

| Dimension | 0 | 1 | 2 |
|---|---|---|---|
| Requirements | Restates the prompt without outcomes | Some observable criteria | Separates facts/questions and defines observable behaviors |
| Context/design | Unbounded prompt / no design reasoning | Uses project context | Uses explicit constraints and explains scope/trade-offs |
| Correctness | Fix breaks rules or partial mutation | Main replay case works | Replay, conflict, failed requests, and overflow are addressed |
| Tests | Status-only or no regression evidence | Regression test exists | Strong assertions plus a demonstrated failing mutation |
| Review judgment | Accepts or rejects AI blindly | Some evidence cited | Findings are classified with precise evidence and reasoning |

Suggested recognition: Evidence Detective (Lab 1), Context Engineer (Lab 2), Test Skeptic (Labs 3–4). Do not rank by speed, prompt volume, or number of generated lines.

## Debrief prompts

1. Which Copilot answer did you verify against code or tests?
2. What made the regression test convincing?
3. Which suggestion did you reject, and why?
4. What guarantee would require a durable shared store?
5. What would you change in your own team's IDE workflow tomorrow?

## Facilitator follow-up checklist

- [ ] Both starter suites passed in the prepared environments.
- [ ] The duplicate transfer was reproduced from a clean process.
- [ ] Each track's attendee can run focused and full checks.
- [ ] Instruction and agent-mode fallbacks were tested.
- [ ] No real banking/customer data or credentials are present.
- [ ] Participants understand that the sample is not production software.
