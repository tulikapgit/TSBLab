package com.northstar.bank;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class TransferService {
    private record IdempotencyEntry(
            String sourceAccountId,
            String destinationAccountId,
            long amountMinor,
            TransferResponse response) {}

    private final Map<String, Long> balances = new HashMap<>();
    private final Map<String, IdempotencyEntry> completedTransfers = new HashMap<>();

    public TransferService() {
        balances.put("checking-001", 50_000L);
        balances.put("savings-001", 10_000L);
        balances.put("overflow-target", Long.MAX_VALUE - 5);
    }

    public synchronized AccountBalance balance(String accountId) {
        Long current = balances.get(accountId);
        if (current == null) {
            throw new TransferException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND");
        }
        return new AccountBalance(accountId, current);
    }

    public synchronized TransferResponse transfer(
            String idempotencyKey,
            String sourceAccountId,
            String destinationAccountId,
            long amountMinor) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new TransferException(HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_REQUIRED");
        }

        IdempotencyEntry existing = completedTransfers.get(idempotencyKey);
        if (existing != null) {
            boolean samePayload = existing.sourceAccountId().equals(sourceAccountId)
                    && existing.destinationAccountId().equals(destinationAccountId)
                    && existing.amountMinor() == amountMinor;
            if (!samePayload) {
                throw new TransferException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED");
            }
            return existing.response();
        }

        if (amountMinor <= 0) {
            throw new TransferException(HttpStatus.BAD_REQUEST, "AMOUNT_MUST_BE_POSITIVE");
        }
        if (sourceAccountId == null || destinationAccountId == null
                || sourceAccountId.equals(destinationAccountId)) {
            throw new TransferException(HttpStatus.BAD_REQUEST, "ACCOUNTS_MUST_BE_DIFFERENT");
        }

        Long sourceBalance = balances.get(sourceAccountId);
        Long destinationBalance = balances.get(destinationAccountId);
        if (sourceBalance == null || destinationBalance == null) {
            throw new TransferException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND");
        }
        if (sourceBalance < amountMinor) {
            throw new TransferException(HttpStatus.CONFLICT, "INSUFFICIENT_FUNDS");
        }

        long newDestinationBalance;
        try {
            newDestinationBalance = Math.addExact(destinationBalance, amountMinor);
        } catch (ArithmeticException exception) {
            throw new TransferException(HttpStatus.UNPROCESSABLE_ENTITY, "BALANCE_OVERFLOW");
        }

        TransferResponse response = new TransferResponse(
                UUID.randomUUID().toString(),
                sourceAccountId,
                destinationAccountId,
                amountMinor,
                "COMPLETED");
        balances.put(sourceAccountId, sourceBalance - amountMinor);
        balances.put(destinationAccountId, newDestinationBalance);
        completedTransfers.put(
                idempotencyKey,
                new IdempotencyEntry(
                        sourceAccountId, destinationAccountId, amountMinor, response));
        return response;
    }
}
