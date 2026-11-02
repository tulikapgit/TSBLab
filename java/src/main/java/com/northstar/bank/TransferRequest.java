package com.northstar.bank;

public record TransferRequest(String sourceAccountId, String destinationAccountId, long amountMinor) {}
