
# Pharmacy-Management-Backend

## Release Process

1. Ensure all code changes are committed and pushed to the main branch.
2. Tag the release in git:
	```sh
	git tag vX.Y.Z
	git push origin vX.Y.Z
	```
3. Build the backend JAR:
	```sh
	./gradlew clean build
	```
4. The release artifact will be available in `build/libs/`.

## Running the Backend

1. Make sure PostgreSQL (or your configured DB) is running and accessible.
2. Update `src/main/resources/application.properties` with your DB credentials if needed.
3. Start the backend service:
	```sh
	./gradlew bootRun
	```
	or run the JAR directly:
	```sh
	java -jar build/libs/Phamarcy-Management-Backend-0.0.1-SNAPSHOT.jar
	```
4. The backend will be available at `http://localhost:8080`.

## Branches

- `main`: Production and release branch.
- `dev`: Development branch for new features and bug fixes.
- `feature/*`: Feature-specific branches (e.g., `feature/auth`, `feature/grn`).

## Frontend

See the frontend project in `pharmacy-management/` for ReactJS setup and instructions.
