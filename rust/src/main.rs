use axum::routing::{get, post};
use axum::Router;
use std::net::SocketAddr;
use std::sync::{Arc, Mutex};

mod transfers;

use transfers::{account, create_transfer, AppState, Ledger};

#[tokio::main]
async fn main() {
    let state = AppState {
        ledger: Arc::new(Mutex::new(Ledger::new())),
    };
    let app = Router::new()
        .route("/accounts/{account_id}", get(account))
        .route("/transfers", post(create_transfer))
        .with_state(state);

    let address = SocketAddr::from(([127, 0, 0, 1], 8080));
    let listener = tokio::net::TcpListener::bind(address)
        .await
        .expect("bind local workshop server");
    println!("Synthetic workshop API listening on http://{address}");
    axum::serve(listener, app)
        .await
        .expect("serve local workshop API");
}
