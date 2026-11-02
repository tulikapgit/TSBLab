package com.northstar.bank;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping
public class TransferController {
    private final TransferService service;

    public TransferController(TransferService service) {
        this.service = service;
    }

    @GetMapping("/accounts/{accountId}")
    public AccountBalance balance(@PathVariable String accountId) {
        return service.balance(accountId);
    }

    @PostMapping("/transfers")
    public TransferResponse transfer(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody TransferRequest request) {
        return service.transfer(
                idempotencyKey,
                request.sourceAccountId(),
                request.destinationAccountId(),
                request.amountMinor());
    }
}

@RestControllerAdvice
class TransferErrorHandler {
    @ExceptionHandler(TransferException.class)
    ResponseEntity<ErrorResponse> transferError(TransferException exception) {
        return ResponseEntity.status(exception.status())
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse invalidJson() {
        return new ErrorResponse("INVALID_REQUEST");
    }
}
