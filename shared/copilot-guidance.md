# Northstar workshop project guidance

- Treat this as a synthetic educational transfer service, never as production banking software.
- Use integer minor units. Do not use floating-point arithmetic for balances.
- Preserve balances exactly on every rejected request; validate both resulting balances before mutating either one.
- Use only synthetic identifiers and data. Do not add secrets or log request bodies, account identifiers, or idempotency keys.
- Keep changes focused on the stated acceptance criteria; do not add dependencies or persistence unless the exercise explicitly asks.
- Explain assumptions and cite relevant files. Do not claim concurrency, restart, or cross-instance guarantees beyond the demonstrated implementation.
- Add a regression test before changing behavior. Do not weaken or remove existing assertions.
- Run the repository's existing tests and formatter/linter after edits. Ask before running commands that mutate or delete data.
