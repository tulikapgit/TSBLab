package com.northstar.bank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TransferIdempotencyTest {
    @Test
    void identicalRetryReturnsOriginalTransferAndMovesFundsOnce() {
        TransferService service = new TransferService();

        TransferResponse first =
                service.transfer("retry-key", "checking-001", "savings-001", 1200);
        TransferResponse retry =
                service.transfer("retry-key", "checking-001", "savings-001", 1200);

        assertEquals(first.transferId(), retry.transferId());
        assertEquals(48800, service.balance("checking-001").balanceMinor());
        assertEquals(11200, service.balance("savings-001").balanceMinor());
    }

    @Test
    void changedPayloadWithSameKeyConflictsWithoutMutation() {
        TransferService service = new TransferService();
        service.transfer("retry-key", "checking-001", "savings-001", 1200);

        assertThrows(TransferException.class,
                () -> service.transfer("retry-key", "checking-001", "savings-001", 1300));
        assertEquals(48800, service.balance("checking-001").balanceMinor());
        assertEquals(11200, service.balance("savings-001").balanceMinor());
    }

    @Test
    void failedRequestDoesNotReserveKey() {
        TransferService service = new TransferService();
        assertThrows(TransferException.class,
                () -> service.transfer("retry-key", "checking-001", "savings-001", 50001));

        TransferResponse corrected =
                service.transfer("retry-key", "checking-001", "savings-001", 1200);
        assertEquals("COMPLETED", corrected.status());
        assertEquals(48800, service.balance("checking-001").balanceMinor());
    }
}
