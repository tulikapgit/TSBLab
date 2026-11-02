# Participant workbook: Bank Release Rescue

**Audience:** intermediate Java and Rust developers  
**Format:** individual work; choose one language track  
**Story:** Northstar Bank's support team reports duplicate internal transfers after a retry. Your job is to find the behavior, define what correct means, add evidence, and make a minimal fix.

## Half-day flow (3 hours 30 minutes)

| Time | Activity |
|---|---|
| 00:00–00:15 | Welcome, safe-use boundaries, toolchain check |
| 00:15–00:45 | Lab 1 — Investigate before editing |
| 00:45–01:10 | Lab 2 — Give Copilot context |
| 01:10–01:20 | Break |
| 01:20–02:10 | Lab 3 — Make the defect fail, then fix it |
| 02:10–02:20 | Break |
| 02:20–02:50 | Lab 4 — Challenge the tests |
| 02:50–03:20 | Lab 5 — Review locally |
| 03:20–03:30 | Reflection and takeaways |

Facilitators may extend the fix lab if attendees need more time. The labs do not depend on a GitHub website, remote repository, hosted checks, or cloud agent.

## Before the first lab

1. Open either `java/` or `rust/`; do not work in both tracks.
2. Read `shared/api-contract.md`.
3. Run the existing tests and record the result.
4. Start the server once and verify the two account routes and one transfer.
5. Stop the server before continuing if using the same port for the other track.
6. Keep your prompts and generated artifacts free of real personal or banking data.

If agent mode is unavailable or disallowed, ask Copilot Chat for a small patch and apply it yourself. If Copilot instructions are not discovered automatically, attach `copilot-guidance.md` explicitly in the chat. Always run commands yourself and inspect edits.

## Lab 1 — Investigate before editing (30 minutes)

**Mission:** Support reports that a retry can move the same funds twice. Determine what the code does before changing it.

### Tasks

1. Read `support-brief.txt`, `outdated-design-note.txt`, the API contract, the transfer endpoint, service, and tests.
2. Trace the endpoint-to-state-change path.
3. Mark each statement as a verified fact, hypothesis, or open question.
4. Write acceptance criteria in the worksheet below.
5. Check every explanation from Copilot against a source file or a test.

### Prompt

> Trace how an internal transfer changes account balances. Cite the relevant files and symbols. Explain existing validation and test coverage. Do not edit code. Separate verified behavior from assumptions, and call out any conflict between the design note and executable behavior.

### Checkpoint

- You can name the method that mutates balances.
- You can explain whether the idempotency header is currently used.
- Your acceptance criteria describe observable outcomes, not an implementation.

### Worksheet: facts and acceptance criteria

| Statement | Fact / hypothesis / question | Evidence or next check |
|---|---|---|
| | | |
| | | |

**Acceptance criteria (Given / When / Then):**

1. Given ______________________________________, when ______________________________________, then ______________________________________.
2. Given ______________________________________, when ______________________________________, then ______________________________________.
3. Given ______________________________________, when ______________________________________, then ______________________________________.

### Debrief

What did the code or tests prove that the outdated note did not?

## Lab 2 — Give Copilot context (25 minutes)

**Mission:** Shape Copilot's design discussion before asking it to edit.

### Tasks

1. Read `copilot-guidance.md`.
2. Use the supported IDE mechanism for project instructions if available. Otherwise, attach the guidance explicitly in chat.
3. Ask Copilot for two possible locations for the fix and compare the trade-offs.
4. Decide key scope, payload equivalence, missing-key behavior, and what happens after a failed transfer.
5. Write down what the design does not guarantee.

### Prompt

> Propose the smallest change that satisfies the acceptance criteria. Compare an API-layer approach with a service-layer approach using this project's architecture. Explain idempotency key scope and replay behavior. Identify tests and limitations. Do not edit code yet.

### Design decision worksheet

- Chosen layer and reason:
- Idempotency key scope:
- Payload fields used to detect key reuse:
- Behavior if a key is reused with changed payload:
- Behavior after a failed transfer:
- Missing key behavior:
- Restart/concurrency limitations:

### Checkpoint

Your decision is consistent with `shared/api-contract.md` and does not claim durable storage or production readiness.

## Lab 3 — Make the defect fail, then fix it (50 minutes)

**Mission:** Add an observable regression test before changing implementation.

### Tasks

1. Add a test that submits a successful request and then retries it with the same key and payload.
2. Assert both responses refer to the same transfer.
3. Assert the exact source and destination balances after both requests.
4. Run only that new test and preserve the failing output.
5. Ask Copilot to implement the smallest fix.
6. Inspect the diff, then run all project tests and quality checks.

### Prompt

> First add a regression test proving that replaying the same successful transfer moves money only once. Assert the same transfer ID and exact account balances after both requests. Run it and show the failure. Then implement the smallest fix. Preserve existing behavior; do not remove tests, weaken assertions, or add dependencies.

### Required behaviors to verify

- Valid transfer debits and credits the exact amount.
- An identical successful retry returns the original transfer ID and does not debit again.
- Same key with changed source, destination, or amount returns conflict and does not mutate balances.
- Non-positive amount, missing account, same account, insufficient funds, and overflow all reject without partial mutation.
- The key is required and whitespace-only keys reject.

### Checkpoint

Record:

| Evidence | Before fix | After fix |
|---|---|---|
| New replay test | | |
| Existing suite | | |
| Formatter/linter | | |

### Hint ladder

1. Is the idempotency key passed from the endpoint into the state-changing service?
2. What data must be retained to return the original result?
3. Is the key lookup and transfer mutation one logical operation?
4. Does the service store a key only after all validations succeed?

Do not jump straight to the final implementation. First identify what the failing test actually proves.

## Lab 4 — Challenge the tests (30 minutes)

**Mission:** Make sure your test suite would catch a plausible regression.

### Tasks

1. Ask Copilot to compare tests to the contract and suggest missing boundary cases.
2. Pick one useful gap and add a table-driven/parameterized test.
3. In a disposable copy, temporarily bypass the replay check or return a new transfer ID for a replay.
4. Run the targeted test and verify it fails.
5. Restore the deliberate mutation without discarding your real work.
6. Run the full suite and formatter/linter again.

### Prompt

> Compare these tests with the acceptance criteria. Identify gaps and rank them by business impact. Propose a small parameterized test. Do not broaden scope to persistent storage or distributed processing.

### Checkpoint

You have one test that demonstrably fails for a controlled incorrect behavior and passes for the intended behavior. Coverage percentage alone is not evidence that an assertion is strong.

## Lab 5 — Review locally (30 minutes)

**Mission:** Review your local changes as a skeptical teammate.

### Tasks

1. Inspect the complete local diff. If not using Git, compare the edited files to the provided starter.
2. Give Copilot the contract, diff, and test evidence.
3. Evaluate each finding; do not accept changes automatically.
4. Check for partial mutation, unchecked arithmetic, key/payload mismatch, unsafe logging, weak tests, and unrelated edits.
5. Apply only justified changes and rerun checks.

### Prompt

> Review these local changes against the attached acceptance criteria. Prioritize correctness, exact money arithmetic, unchanged balances on errors, and sensitive logging. Cite specific evidence. Separate confirmed findings from questions. Do not edit files until I select a finding.

### Review worksheet

| Finding | Evidence | Accept / reject / defer | Why |
|---|---|---|---|
| | | | |
| | | | |

### Local change summary

- Behavior changed:
- Tests/checks run and result:
- Limitations:
- Remaining questions:

## Closing reflection

Where did Copilot reduce effort, and where did your judgment prevent a mistake?

### Optional challenge

Sketch what would have to change to make idempotency survive a restart and work across multiple service instances. Do not implement persistence in this workshop.
