use axum::extract::{Path, State};
use axum::http::{HeaderMap, StatusCode};
use axum::response::{IntoResponse, Response};
use axum::Json;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use uuid::Uuid;

#[derive(Clone)]
pub struct AppState {
    pub ledger: Arc<Mutex<Ledger>>,
}

pub struct Ledger {
    balances: HashMap<String, i64>,
}

impl Ledger {
    pub fn new() -> Self {
        Self {
            balances: HashMap::from([
                ("checking-001".to_owned(), 50_000),
                ("savings-001".to_owned(), 10_000),
                ("overflow-target".to_owned(), i64::MAX - 5),
            ]),
        }
    }

    pub fn balance(&self, account_id: &str) -> Result<AccountBalance, AppError> {
        self.balances
            .get(account_id)
            .copied()
            .map(|balance_minor| AccountBalance {
                account_id: account_id.to_owned(),
                balance_minor,
            })
            .ok_or_else(|| AppError::new(StatusCode::NOT_FOUND, "ACCOUNT_NOT_FOUND"))
    }

    pub fn transfer(
        &mut self,
        idempotency_key: &str,
        request: TransferRequest,
    ) -> Result<TransferResponse, AppError> {
        if idempotency_key.trim().is_empty() {
            return Err(AppError::new(
                StatusCode::BAD_REQUEST,
                "IDEMPOTENCY_KEY_REQUIRED",
            ));
        }
        if request.amount_minor <= 0 {
            return Err(AppError::new(
                StatusCode::BAD_REQUEST,
                "AMOUNT_MUST_BE_POSITIVE",
            ));
        }
        if request.source_account_id == request.destination_account_id {
            return Err(AppError::new(
                StatusCode::BAD_REQUEST,
                "ACCOUNTS_MUST_BE_DIFFERENT",
            ));
        }

        let source_balance = *self
            .balances
            .get(&request.source_account_id)
            .ok_or_else(|| AppError::new(StatusCode::NOT_FOUND, "ACCOUNT_NOT_FOUND"))?;
        let destination_balance = *self
            .balances
            .get(&request.destination_account_id)
            .ok_or_else(|| AppError::new(StatusCode::NOT_FOUND, "ACCOUNT_NOT_FOUND"))?;
        if source_balance < request.amount_minor {
            return Err(AppError::new(
                StatusCode::CONFLICT,
                "INSUFFICIENT_FUNDS",
            ));
        }
        let new_destination_balance = destination_balance
            .checked_add(request.amount_minor)
            .ok_or_else(|| AppError::new(StatusCode::UNPROCESSABLE_ENTITY, "BALANCE_OVERFLOW"))?;

        let new_source_balance = source_balance - request.amount_minor;
        // Workshop defect: the required key is not recorded or checked.
        self.balances
            .insert(request.source_account_id.clone(), new_source_balance);
        self.balances.insert(
            request.destination_account_id.clone(),
            new_destination_balance,
        );

        Ok(TransferResponse {
            transfer_id: Uuid::new_v4().to_string(),
            source_account_id: request.source_account_id,
            destination_account_id: request.destination_account_id,
            amount_minor: request.amount_minor,
            status: "COMPLETED".to_owned(),
        })
    }
}

impl Default for Ledger {
    fn default() -> Self {
        Self::new()
    }
}

#[derive(Debug, Deserialize, Clone, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub struct TransferRequest {
    pub source_account_id: String,
    pub destination_account_id: String,
    pub amount_minor: i64,
}

#[derive(Debug, Serialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub struct AccountBalance {
    pub account_id: String,
    pub balance_minor: i64,
}

#[derive(Debug, Serialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub struct TransferResponse {
    pub transfer_id: String,
    pub source_account_id: String,
    pub destination_account_id: String,
    pub amount_minor: i64,
    pub status: String,
}

#[derive(Debug, Serialize)]
struct ErrorBody {
    error: String,
}

#[derive(Debug)]
pub struct AppError {
    status: StatusCode,
    code: &'static str,
}

impl AppError {
    fn new(status: StatusCode, code: &'static str) -> Self {
        Self { status, code }
    }
}

impl IntoResponse for AppError {
    fn into_response(self) -> Response {
        (
            self.status,
            Json(ErrorBody {
                error: self.code.to_owned(),
            }),
        )
            .into_response()
    }
}

pub async fn account(
    State(state): State<AppState>,
    Path(account_id): Path<String>,
) -> Result<Json<AccountBalance>, AppError> {
    let ledger = state
        .ledger
        .lock()
        .map_err(|_| AppError::new(StatusCode::INTERNAL_SERVER_ERROR, "STATE_UNAVAILABLE"))?;
    ledger.balance(&account_id).map(Json)
}

pub async fn create_transfer(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(request): Json<TransferRequest>,
) -> Result<Json<TransferResponse>, AppError> {
    let key = headers
        .get("Idempotency-Key")
        .and_then(|value| value.to_str().ok())
        .ok_or_else(|| AppError::new(StatusCode::BAD_REQUEST, "IDEMPOTENCY_KEY_REQUIRED"))?;
    let mut ledger = state
        .ledger
        .lock()
        .map_err(|_| AppError::new(StatusCode::INTERNAL_SERVER_ERROR, "STATE_UNAVAILABLE"))?;
    ledger.transfer(key, request).map(Json)
}

#[cfg(test)]
mod tests {
    use super::*;

    fn request(source: &str, destination: &str, amount_minor: i64) -> TransferRequest {
        TransferRequest {
            source_account_id: source.to_owned(),
            destination_account_id: destination.to_owned(),
            amount_minor,
        }
    }

    #[test]
    fn successful_transfer_moves_exact_amount() {
        let mut ledger = Ledger::new();
        let result = ledger
            .transfer("key-1", request("checking-001", "savings-001", 1200))
            .unwrap();

        assert_eq!(ledger.balance("checking-001").unwrap().balance_minor, 48_800);
        assert_eq!(ledger.balance("savings-001").unwrap().balance_minor, 11_200);
        assert_eq!(result.status, "COMPLETED");
    }

    #[test]
    fn insufficient_funds_leave_both_balances_unchanged() {
        let mut ledger = Ledger::new();
        assert!(ledger
            .transfer("key-2", request("checking-001", "savings-001", 50_001))
            .is_err());

        assert_eq!(ledger.balance("checking-001").unwrap().balance_minor, 50_000);
        assert_eq!(ledger.balance("savings-001").unwrap().balance_minor, 10_000);
    }

    #[test]
    fn invalid_amount_leaves_balances_unchanged() {
        let mut ledger = Ledger::new();
        assert!(ledger
            .transfer("key-3", request("checking-001", "savings-001", 0))
            .is_err());

        assert_eq!(ledger.balance("checking-001").unwrap().balance_minor, 50_000);
        assert_eq!(ledger.balance("savings-001").unwrap().balance_minor, 10_000);
    }

    #[test]
    fn overflow_leaves_both_balances_unchanged() {
        let mut ledger = Ledger::new();
        assert!(ledger
            .transfer("key-4", request("checking-001", "overflow-target", 10))
            .is_err());

        assert_eq!(ledger.balance("checking-001").unwrap().balance_minor, 50_000);
        assert_eq!(
            ledger.balance("overflow-target").unwrap().balance_minor,
            i64::MAX - 5
        );
    }

    #[test]
    fn same_account_is_rejected_without_mutation() {
        let mut ledger = Ledger::new();
        assert!(ledger
            .transfer("key-5", request("checking-001", "checking-001", 100))
            .is_err());
        assert_eq!(ledger.balance("checking-001").unwrap().balance_minor, 50_000);
    }

    #[test]
    fn unknown_account_is_rejected_without_mutation() {
        let mut ledger = Ledger::new();
        assert!(ledger
            .transfer("key-6", request("checking-001", "missing-001", 100))
            .is_err());
        assert_eq!(ledger.balance("checking-001").unwrap().balance_minor, 50_000);
    }

    #[test]
    fn missing_key_is_rejected_without_mutation() {
        let mut ledger = Ledger::new();
        assert!(ledger
            .transfer("  ", request("checking-001", "savings-001", 100))
            .is_err());
        assert_eq!(ledger.balance("checking-001").unwrap().balance_minor, 50_000);
        assert_eq!(ledger.balance("savings-001").unwrap().balance_minor, 10_000);
    }
}
