# Repository Guidelines

## Project Structure & Module Organization

This repository contains three application areas. `Adaptive Learning/src/main/java` holds the Spring Boot backend, organized by feature under `adaptivelearning/module`; its tests are in `Adaptive Learning/src/test/java`. `Adaptive Learning/pybkt` contains the Python BKT service. `AdaptiveLearningFE/src` contains the Vue 3 frontend, with feature pages under `features`, API clients under `services`, and shared routing, state, and utilities nearby. Static frontend assets live in `AdaptiveLearningFE/public` and `src/assets`. Project notes and API mocks/simulations are in `docs` and `postman`.

## Build, Test, and Development Commands

Run backend commands from `Adaptive Learning`:

- `./gradlew bootRun` (Windows: `gradlew.bat bootRun`) starts the Spring Boot app.
- `./gradlew test` runs the JUnit 5 test suite.
- `./gradlew build` compiles, tests, and packages the backend.

Run frontend commands from `AdaptiveLearningFE`:

- `npm install` installs dependencies; `npm run dev` starts Vite locally.
- `npm run build` creates the production bundle; `npm run preview` serves it locally.

The frontend currently defines no test script. Run Python service checks from `Adaptive Learning/pybkt` using the environment and dependencies in `requirements.txt`.

## Coding Style & Naming Conventions

Follow surrounding code. Java uses package paths under `adaptivelearning`, PascalCase classes, and camelCase members. Vue components use `.vue` files and follow the existing feature-oriented organization; JavaScript uses camelCase. Keep changes scoped, preserve API contracts, and avoid committing secrets from local environment files. Use the existing Prettier setup for frontend formatting; pre-commit may format staged files with `pretty-quick`.

## Testing Guidelines

Add or update backend JUnit tests under the matching `src/test/java` package, using `*Test.java` names. Cover relevant behavior and regressions, then run `./gradlew test`. For frontend changes, run `npm run build` and manually verify affected flows because no automated frontend test command is configured.

## Commit & Pull Request Guidelines

## Reporting Module

- Admin reports are available at `/admin/reports`; APIs live under `/api/v1/reports` and require `ROLE_ADMIN`.
- Report dates use `Asia/Ho_Chi_Minh` and a half-open interval from local midnight through midnight after `to`.
- Report KPIs must be calculated in database aggregates, not from loaded raw records.
- Run checks with `cd "Adaptive Learning"; .\gradlew.bat test` and `cd AdaptiveLearningFE; npm run build`.
- Optional demo seed: `Adaptive Learning/src/main/resources/db/report-demo-seed.sql`. Use only in a disposable DB with at least one user and exam.
- Keep local secrets in ignored `.env` files; never commit credentials.

Recent commits use short imperative summaries, often with `feat:` (formatting is not fully consistent). Prefer Conventional Commit prefixes, for example `feat: add payment status view` or `fix: validate checkout request`. Pull requests should describe behavior and rationale, link related issues, list tests/builds run, and include screenshots for visible UI changes. Call out configuration or database changes explicitly.
