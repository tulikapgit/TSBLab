# Northstar Bank internal-transfer API contract

## Scenario

Support received a synthetic report: an internal transfer may be submitted again after a timeout. The client retries the request; both submissions can currently move money. Investigate the implementation and agree on observable requirements before changing code.

All account names, balances, IDs, and requests in this kit are fictional.

## API

### `GET /accounts/{accountId}`

Returns the current synthetic account balance.

Success response:

```json
{
  "accountId": "checking-001",
  "balanceMinor": 50000
}
```

An unknown account returns `404` and an error object.

### `POST /transfers`

Requires a non-empty `Idempotency-Key` header. The starter validates that a key is present but intentionally ignores it during processing.

Request:

```http
POST /transfers
Idempotency-Key: workshop-transfer-001
Content-Type: application/json
```

```json
{
  "sourceAccountId": "checking-001",
  "destinationAccountId": "savings-001",
  "amountMinor": 1200
}
```

Successful response:

```json
{
  "transferId": "generated-identifier",
  "sourceAccountId": "checking-001",
  "destinationAccountId": "savings-001",
  "amountMinor": 1200,
  "status": "COMPLETED"
}
```

The generated transfer identifier is illustrative and will differ on each starter request.

## Required business rules

1. Amounts are positive signed 64-bit integer minor units. Do not use floating-point arithmetic.
2. Source and destination accounts must exist and must be different.
3. Insufficient funds rejects the transfer without changing either balance.
4. The destination balance addition must be checked for overflow before changing either balance.
5. A successful transfer debits and credits exactly the requested amount; the sum of the two balances is conserved.
6. For a successful request, replaying the same key with the same source, destination, and amount returns the original response and does not apply the transfer a second time.
7. Reusing a key with a different source, destination, or amount returns `409 Conflict` and does not change balances.
8. A failed transfer is not recorded as a successful idempotency result; a later corrected request can use the key.
9. A missing or whitespace-only key returns `400 Bad Request`.
10. The exercise only requires sequential retries while the process remains running. The in-memory record is lost on restart. Do not imply durable or cross-instance behavior.

The contract chooses the tuple `(sourceAccountId, destinationAccountId, amountMinor)` as payload equivalence. JSON property ordering and whitespace are irrelevant because the request is decoded into fields.

## Starter data

| Account | Initial balance (minor units) |
|---|---:|
| `checking-001` | 50,000 |
| `savings-001` | 10,000 |
| `overflow-target` | 9,223,372,036,854,775,802 |

Use the `overflow-target` only to exercise checked addition with a positive amount. Its near-limit balance is synthetic.

## Expected statuses

| Condition | Status |
|---|---:|
| Transfer completed or identical replay | 200 |
| Missing/blank key, non-positive amount, same account | 400 |
| Unknown account | 404 |
| Insufficient funds or key/payload conflict | 409 |
| Balance overflow | 422 |

## Deliberate limitations

The service is an educational in-memory model, not a financial ledger. It does not implement authentication, authorization, durable transactions, reconciliation, rate limiting, idempotency expiry, cross-process consistency, or production observability. The contract is sufficient for the local sequential exercises only.
