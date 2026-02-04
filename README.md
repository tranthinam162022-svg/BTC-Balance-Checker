# BTC Balance Checker

BTC Balance Checker is a web application that allows users to check the balance of their Bitcoin wallet addresses. The application retrieves the balance of the Bitcoin wallet address by querying the blockchain using a third-party API.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

The following software is required to be installed on your system:

- Java 17
- Maven
- Git

### Installing

Clone the repository using Git:

```shell
git clone https://github.com/freelkee/BTC-Balance-Checker.git
```

Navigate to the project directory and build the project using Maven:

```shell
cd BTC-Balance-Checker
mvn clean install
```

Run the application using Maven:

```shell
mvn spring-boot:run
```

The application should now be running on `http://localhost:8080`.


---

## Running locally (Windows) ⚙️

1) Install Java 17 (JDK) and Maven.
2) Set JAVA_HOME environment variable (PowerShell example):

```powershell
setx JAVA_HOME "C:\\Program Files\\Java\\jdk-17.0.x"
# then re-open your terminal
```

3) Build and run:

```powershell
./mvnw.cmd -DskipTests package
./mvnw.cmd spring-boot:run
```

4) Batch features:
- Web UI: `http://localhost:8080/batch` — paste addresses or upload CSV/TXT.
- REST batch: `POST /api/balance/batch` with JSON `{ "currency":"", "offset":0, "addresses":["addr1","addr2"] }`.
- SSE progress: connect to `GET /batch/stream/{jobId}` or use the web upload to see progress.
- Cancel job: POST `/batch/cancel/{jobId}` — the UI's progress page exposes a "Hủy job" button (which calls this endpoint).

Examples and helper scripts are available in the `scripts/` folder:
- `scripts/check-java.ps1` — verify JAVA_HOME and java version (Windows PowerShell).
- `scripts/run-batch-curl.sh` — curl examples to call REST batch and upload file.
- `scripts/run-batch-pwsh.ps1` — PowerShell example for upload/polling notes.
- `scripts/run-batch-cancel.sh` — example workflow showing how to start and where to cancel.

Note: the app queries `blockchain.info` and `blockchain.com` ticker; for bulk requests consider limiting concurrency to avoid rate limiting. If you expect to check many addresses, increase timeouts or reduce `batch.concurrency` in `application.properties`.  

Tip: to run tests locally after setting JAVA_HOME:

```powershell
./mvnw.cmd test
```


## Usage

To check the balance of a Bitcoin wallet address, enter the wallet address in the input field and click the "Check Balance" button. The application will retrieve the balance of the wallet address and display it on the page.

## Built With

- Spring Boot - The web framework used
- Thymeleaf - The template engine used
- Maven - Dependency management

## Contributing

Contributions are welcome. Please submit a pull request with your changes.
