# Integration testing and deployment handover

For the current integration audit, outstanding feature list and final verification
procedure, see [submission readiness — 30 October 2026](submission-readiness.md).
The validation record below describes the earlier 12 October revision.

## Scope

This change completes the worldwide city output (#13) and expands integration testing (#25) for the reports currently present on develop. The test/CI process and problem record support coordination (#58). Final submission coordination (#36) remains dependent on the team completing and integrating the other required features.

## Coverage

Tests run against a fresh instance of the supplied MySQL world dataset, rather than mocks:

- World population and all seven continent totals, including empty/unknown results.
- All 4,079 cities, descending population, joined country names and visible report fields.
- Scotland top-N results, limits above available rows, unknown districts, invalid limits and SQL-like input.
- Chinese and English speaker totals and percentages of the entire world population.
- Region report output, top-N limit and unknown/SQL-like region input.
- The combined application flow from user input through JDBC queries to printed output.
- Missing, nonnumeric and nonpositive user input.
- Empty city data, closed database connection and city-only mode without stdin.

The deployment workflow additionally checks the packaged container's report line count, integrated interactive output and invalid-input exit status.

## Problems found and addressed

| Problem | Resolution |
| --- | --- |
| Worldwide query existed but was never displayed | Add the report to the normal flow and a standalone --all-cities mode |
| Compose initialized a different database from the JDBC URL | Initialize the supplied world dump automatically in a fresh volume |
| JDBC localhost prevented service-to-service Docker deployment | Configure JDBC via environment; Compose uses the mysql service |
| Database failure printed a stack trace and returned success | Display the failure and return exit code 1 |
| Missing stdin could crash the interactive application | Validate required input and return exit code 2 |
| Container smoke check relied on host networking | Run the app through Compose on its service network |
| Equal-population city ordering was nondeterministic | Use city ID as a stable tie breaker |
| Test runner/version was implicit | Pin Surefire and fail if no tests are discovered |

## Team testing process

1. Merge/review report changes into develop and run the integration workflow.
2. Extend this suite for each newly integrated report using independently checked dataset totals and expected output.
3. Record failures using the repository bug issue template, with input, expected/actual output, branch and CI evidence.
4. Have the relevant module owner resolve the problem; rerun the affected checks and then the full workflow.
5. Retain test-results and deployment-evidence workflow artifacts for the submission record.

## Final submission gate

The current integrated app has worldwide cities, district top-N query, world/continent population, region top-N countries and Chinese/English reports. This is not the complete backlog.

Before closing #36:

- Confirm all required country, city and capital reports are integrated (#5–9, #16–23, #27–32).
- Confirm region/country/district/city population and urban/non-urban statistics are integrated (#39–45).
- Confirm Hindi, Spanish, Arabic and five-language ranking reports are integrated (#50–53).
- Confirm the integration owner has exposed all required reports in the application (#57), including district top-N.
- Review the final acceptance criteria and team-owned verification/deployment tasks (#12, #34).
- Agree the submission environment with the team, deploy that final revision, and retain the successful workflow and runtime output.
- Update the project/sprint board (#54) and confirm submission materials.

Open issue status alone does not prove a feature is absent. This list must be reconciled against subsequent merged code and the agreed assessment requirements. This PR establishes the deployment path and verifies currently integrated features; it does not certify the whole project ready for submission.

## Validation record — 12 October 2026

- Java 25 / Maven integration run: 32 tests, 0 failures, 0 errors, 0 skipped.
- Fresh isolated mysql:8.4 database on host port 3307, initialized automatically from the supplied world.sql.
- Docker multi-stage image built successfully.
- Container standalone report: 4,081 lines (two headers plus 4,079 cities).
- Container integrated run: Europe / Western Europe / 5 completed with world, continent, country and language output.
- Container invalid input: exit code 2.
- Container missing database: exit code 1.
- GitHub Actions repeats these checks on the proposed revision; its run is the shared evidence for review.
