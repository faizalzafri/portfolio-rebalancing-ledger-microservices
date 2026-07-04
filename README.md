# Spring Stock Microservices

Modernized microservices application built with **Java 21 (LTS)**, **Spring Boot 3.3.0**, **Spring Cloud (2023.0.1)**, **Spring Cloud Gateway**, and **PostgreSQL**.

---

## 1. Local Development (Docker Compose)

The entire microservices stack (Eureka, Gateway, database, and backend services) is fully containerized.

### Build and Run
Prune legacy database volumes and run the entire stack in the background:
```bash
# Tear down and purge old volumes
docker compose down -v

# Build and start services
docker compose up --build -d

# Check service health status
docker compose ps

# Monitor service logs
docker compose logs -f
```

### Exposed Endpoints
- **Service Registry (Eureka)**: `http://localhost:8761`
- **API Gateway (Spring Cloud Gateway)**: `http://localhost:8302`

---

## 2. API Verification

Requests route through the API Gateway on port `8302`.

### Add stock quotes for a user
```bash
curl -H "Content-Type: application/json" \
     -d '{"username":"sam", "quotes":["AAPL", "GOOG"]}' \
     http://localhost:8302/api/db-service/rest/db/add
```

### Retrieve stock price valuations
```bash
curl http://localhost:8302/api/stock-service/rest/stock/sam
```

---

## 3. Resilience Testing (Circuit Breaker)

Verify the Resilience4j Circuit Breaker fallback mechanism.

### Simulate outage
Stop the database service:
```bash
docker compose stop db-service
```

### Fetch quotes (Triggers Fallback)
```bash
curl http://localhost:8302/api/stock-service/rest/stock/sam
```
*Expected response (graceful mock cached fallback):*
```json
[
  {"quote":"FALLBACK-MSFT","price":420.59},
  {"quote":"FALLBACK-AAPL","price":185.75}
]
```

---

## 4. Continuous Integration (CI)

A GitHub Actions workflow is defined in `.github/workflows/ci.yml` to compile and verify all services.

### Local Workflow testing (via Act CLI)
To test the workflow on your local machine using Docker:
```bash
# Install Act CLI (Windows)
winget install nektos.act

# Run the workflow locally
act
```

### Trigger GitHub Build
Push commits to the remote repository to trigger the cloud CI run:
```bash
git add .
git commit -m "ci: modernize stock microservices"
git push origin master
```

---

## 5. Kubernetes Orchestration

Production manifests are located in the [k8s/](file:///E:/Dev/spring-stock-microservices/k8s) folder.

### Dry-run validation
```bash
kubectl apply -f k8s/ --dry-run=client --validate=false
```