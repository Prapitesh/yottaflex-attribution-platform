# Run the YottaFlex Attribution Platform on Windows

## 1. Database connection
The application uses PostgreSQL and Flyway. In pgAdmin, connect to the PostgreSQL server that you choose to use, then create a database named `yottaflex_attribution` if it does not already exist:

```sql
CREATE DATABASE yottaflex_attribution;
```

If it already exists, do not drop it. Configure the backend with the same host, port, username and password used by that pgAdmin server. Set these in the terminal before starting Spring Boot; do not put the real password in Git:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/yottaflex_attribution"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="YOUR_REAL_POSTGRES_PASSWORD"
cd C:\Users\user\YottaFlex-Attribution-Platform\backend
mvn spring-boot:run
```

Change port 5432 to the selected pgAdmin server's port. Flyway applies versioned migrations automatically. Hibernate validates the mappings against the migrated schema.

## 2. Start frontend
In a second terminal:

```powershell
cd C:\Users\user\YottaFlex-Attribution-Platform\frontend
npm run dev
```

Open http://localhost:5173.

## 3. Initial workflow
1. Choose **Generate starter data** from the dashboard.
2. Wait for the dataset run ID response.
3. Choose **Run attribution**.
4. Run **Run data quality checks**.
5. Select the dataset run from the top-right selector to inspect one snapshot.
6. Use the attribution model selector to switch between first touch, last touch and linear.

Use the FULL generator only after validating the starter run. Full scale creates up to 500 campaigns, 50,000 contacts, 200,000 baseline touchpoints and 10,000 opportunities, plus labelled anomaly examples.

## 4. API smoke checks
```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod http://localhost:8080/api/v1/dataset-runs
Invoke-RestMethod http://localhost:8080/api/v1/dashboard/summary
```

Generation and calculation endpoints:
- POST /api/v1/admin/generate — body: {"seed":20261022,"scale":"STARTER"}
- POST /api/v1/admin/recalculate
- POST /api/v1/admin/recalculate/{runId}
- POST /api/v1/admin/validate?runId={id}
