# Facilitator-only reference fix outline

Keep this file out of attendee copies until labs are complete. Equivalent implementations are acceptable if the contract and tests pass.

## Java reference design

Add a record such as:

```java
private record IdempotencyEntry(
        String sourceAccountId,
        String destinationAccountId,
        long amountMinor,
        TransferResponse response) {}
```

Maintain a `Map<String, IdempotencyEntry>` in `TransferService`. At the beginning of `transfer`, after rejecting a missing/blank key:

1. Look up the key.
2. If present, compare all three request fields. Return the stored response if identical; otherwise throw a `409` `TransferException` using `IDEMPOTENCY_KEY_REUSED`.
3. If absent, run all current validation and calculate both resulting balances without mutating state.
4. Create the response once, update both balances, then retain key, request tuple, and response.
5. Return the created response.

Use `synchronized` on the state-changing service method (and balance read if desired) if presenting atomic sequential operations to requests within the JVM. Explain that an in-memory map is still lost on restart and not shared across instances.

## Rust reference design

Add an entry struct containing the three request fields and a cloned `TransferResponse`, plus `idempotency: HashMap<String, IdempotencyEntry>` in `Ledger`.

Inside `Ledger::transfer`:

1. Reject a blank key.
2. Check the key and compare the complete request tuple. Return a cloned stored response for an exact match; return `409` on a mismatch.
3. Validate accounts, amount, funds, and checked destination addition.
4. Construct the response once; update balances and insert the entry only after all fallible validation is complete.
5. Return the response.

The HTTP handler already holds the mutex guard across `Ledger::transfer`, which keeps the local check-and-update under one lock in this single-process sample. It does not supply durability or cross-instance coordination.

## Minimum regression tests

For each language, add tests that:

- Send the same successful key and tuple twice.
- Assert equal transfer IDs.
- Assert source and destination balances show exactly one transfer.
- Reuse the key with a changed amount and assert conflict/error plus unchanged balances.
- Reuse the key with a changed source or destination and assert conflict/error.
- Make a request fail validation, then retry a corrected request with the same key and assert the corrected transfer succeeds.
- Retain the existing invalid, insufficient-funds, unknown-account, same-account, and overflow tests.

For an HTTP-level check, verify the `Idempotency-Key` header is required and response/error JSON uses camel-case API field names. In Rust, `serde(rename_all = "camelCase")` on request and response types provides the wire naming in the starter.

## Expected local checks

- Java: `mvn test`
- Rust: `cargo test`, `cargo fmt --check`, `cargo clippy --all-targets -- -D warnings`

Do not accept a fix that only caches the last request, ignores source/destination when comparing payloads, stores failed requests as completed, mutates balances before overflow checks, or logs keys or request payloads.
