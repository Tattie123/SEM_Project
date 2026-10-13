# SEM Project

Java 25 command-line reports backed by the supplied MySQL world dataset.

## Run with Docker

Install Docker Desktop (or Docker Engine with Compose). From the repository root:

    docker compose up -d --wait mysql
    docker compose build app
    docker compose run --rm app --all-cities

The standalone report lists all 4,079 cities, with name, country, district and population in descending population order. Equal populations are ordered by city ID.

To run all currently integrated reports interactively:

    docker compose run --rm app

Enter a continent, region and positive whole-number country limit when prompted. The application also displays world population and Chinese/English speaker statistics. Missing or invalid input returns exit code 2; database failures return exit code 1.

    docker compose down

This stops containers and preserves the database volume. The bundled SQL is imported automatically only when initializing a fresh database volume. It recreates the world database, so use a separate Compose project for testing rather than importing it into an existing database.

## Build and test locally

Requirements: JDK 25, Maven 3.9+, and the initialized MySQL dataset.

    docker compose up -d --wait mysql
    mvn --batch-mode clean verify
    java -jar target/SoftwareEngineeringMethods-1.0-SNAPSHOT.jar --all-cities

Database configuration uses environment variables:

| Variable | Default | Purpose |
| --- | --- | --- |
| DB_URL | jdbc:mysql://localhost:3306/world | JDBC address for the Java app and tests |
| DB_USER | software_app | Database user |
| DB_PASSWORD | MyAppPassword2026! | Database password |
| DB_PORT | 3306 | Host port exposed by Compose |

Compose connects the application to the mysql service internally. The included passwords are development defaults.

For an isolated local test database when port 3306 is already in use (PowerShell):

    $env:DB_PORT = '3307'
    $env:DB_URL = 'jdbc:mysql://localhost:3307/world'
    docker compose -p sem-assigned-check up -d --wait mysql
    mvn --batch-mode clean verify
    docker compose -p sem-assigned-check down

## Integration and submission checks

GitHub Actions runs the real MySQL integration suite, packages the executable JAR, builds the Docker image, checks the standalone city report, runs the interactive reports and verifies invalid input fails. Test reports, executable JAR and deployment output are uploaded as workflow artifacts.

See [testing and deployment handover](docs/testing-and-deployment.md) for coverage, problem records and the remaining team submission checks.
