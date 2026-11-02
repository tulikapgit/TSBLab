package com.northstar.bank;

public record TransferResponse(
        String transferId,
        String sourceAccountId,
        String destinationAccountId,
        long amountMinor,
        String status) {}
