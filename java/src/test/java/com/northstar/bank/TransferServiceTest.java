package com.northstar.bank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransferServiceTest {
    private TransferService service;

    @BeforeEach
    void setUp() {
        service = new TransferService();
    }

    @Test
    void successfulTransferMovesExactAmount() {
        TransferResponse result = service.transfer("key-1", "checking-001", "savings-001", 1200);

        assertEquals(48800, service.balance("checking-001").balanceMinor());
        assertEquals(11200, service.balance("savings-001").balanceMinor());
        assertEquals("COMPLETED", result.status());
    }

    @Test
    void insufficientFundsLeaveBothBalancesUnchanged() {
        assertThrows(TransferException.class,
                () -> service.transfer("key-2", "checking-001", "savings-001", 50001));

        assertEquals(50000, service.balance("checking-001").balanceMinor());
        assertEquals(10000, service.balance("savings-001").balanceMinor());
    }

    @Test
    void invalidAmountLeavesBalancesUnchanged() {
        assertThrows(TransferException.class,
                () -> service.transfer("key-3", "checking-001", "savings-001", 0));

        assertEquals(50000, service.balance("checking-001").balanceMinor());
        assertEquals(10000, service.balance("savings-001").balanceMinor());
    }

    @Test
    void overflowLeavesBothBalancesUnchanged() {
        assertThrows(TransferException.class,
                () -> service.transfer("key-4", "checking-001", "overflow-target", 10));

        assertEquals(50000, service.balance("checking-001").balanceMinor());
        assertEquals(Long.MAX_VALUE - 5, service.balance("overflow-target").balanceMinor());
    }

    @Test
    void missingOrBlankIdempotencyKeyIsRejected() {
        assertThrows(TransferException.class,
                () -> service.transfer(" ", "checking-001", "savings-001", 100));
        assertThrows(TransferException.class,
                () -> service.transfer(null, "checking-001", "savings-001", 100));
    }

    @Test
    void sameAccountIsRejectedWithoutMutation() {
        assertThrows(TransferException.class,
                () -> service.transfer("key-5", "checking-001", "checking-001", 100));

        assertEquals(50000, service.balance("checking-001").balanceMinor());
    }

    @Test
    void unknownAccountIsRejectedWithoutMutation() {
        assertThrows(TransferException.class,
                () -> service.transfer("key-6", "checking-001", "missing-001", 100));

        assertEquals(50000, service.balance("checking-001").balanceMinor());
    }
}
