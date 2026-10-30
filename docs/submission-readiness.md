# Submission readiness — 30 October 2026

## Revision reviewed

Integration baseline: `develop` at `3d96c56ee1ebf204f0423024e828251c929875dc`.
This check supports integration testing #58 and final deployment #36.

## Integrated reports

Source inspection of `Main.java` confirms that the interactive application runs:

- All cities worldwide (#13), also available through `--all-cities`.
- World and continent population (#37, #38).
- Cities by continent (#16), including an explicit empty-result message.
- Top N countries by region (#10).
- Chinese and English speaker counts and world percentages (#46, #47).

Top N cities by district (#24) has a query and integration tests, but is not
called by the interactive application. Integration task #57 must expose it.

## Features still requiring integration or acceptance evidence

The current source tree contains `Main`, `City`, and `DatabaseConnectionManager`.
The application does not expose the following required reports:

- Countries worldwide/by continent/by region (#5–7) and top N countries
  worldwide/by continent (#8–9).
- Cities by region/country/district (#17–19), top N cities
  worldwide/by continent/by region/by country (#20–23).
- Capitals worldwide/by continent/by region and their top N variants
  (#27–32).
- Region/country/district/city population and urban/non-urban statistics
  (#39–45).
- Hindi, Spanish, Arabic, and the five-language ranking (#50–53).

These are source-level observations, not an inference from issue state.
Work in unmerged branches is not part of the deployed application.
At this check, PR #79 (cities by region) and PR #80 (Cameron to develop)
are open; their designated owners/reviewers must complete review and integration.

## Verification and deployment procedure

1. Review and merge the remaining report modules through #57.
2. Reconcile the application against the report list above and the assessment
   requirements. Add database-backed acceptance tests for newly exposed reports.
3. Run **SEM integration and deployment checks** on the final revision. Pushes
   and PRs trigger it automatically; `workflow_dispatch` also allows a manual
   run from the Actions tab after this workflow update is merged.
4. Require the integration tests, executable JAR, Docker build, worldwide and
   continent reports, empty-result handling, invalid input (exit 2), and database
   failure (exit 1) to pass.
5. Download `application-jar`, `test-results`, and `deployment-evidence` from
   the successful final run. The evidence includes `revision.txt` with the exact
   tested commit and workflow URL. Retain the artifacts with submission materials
   before GitHub's artifact retention period expires.
6. Record the final workflow URL in #58 and #36, reconcile #54 with the team,
   and close those tasks only after the complete integrated application passes.

GitHub Actions and downloadable build artifacts remain the recorded deployment
destination; no permanently running service is implied.

## Local verification limitations

Java 25 and Maven successfully compiled the baseline application and test suite
on 30 October. Local Docker startup failed while initializing its Secrets Engine
(`engine.sock` could not be renamed). This is an environment failure, not a
successful integration test run. The GitHub Actions run on this PR is the
database and container verification evidence for this revision.

Whole-project final sign-off remains pending the missing integrations above.
