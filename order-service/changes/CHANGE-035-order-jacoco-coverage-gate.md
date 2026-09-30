# CHANGE-035 — Order Service JaCoCo coverage gate

- Date: 2026-09-30
- Status: Configured; coverage threshold not yet met
- Scope: Order Service Maven verification
- Approved by: Project workflow requirement NFR3.1.1

## Change

Added \`jacoco-maven-plugin\` 0.8.15 to \`order-service/pom.xml\`. The build now:

- instruments tests during the Maven test phase;
- writes an HTML/XML report during \`verify\`;
- excludes only the Spring Boot application bootstrap class; and
- enforces at least 80% line and 80% branch coverage.

## Verification

\`./mvnw verify\` under Java 21 executed all 12 Order Service tests successfully,
then failed the JaCoCo check because the current test suite covers:

- 128/318 lines: 40.25%;
- 60/209 branches: 28.71%.

The threshold was not lowered. Additional unit, controller, integration, and
contract tests are required before the Order verification gate can pass.

## Remaining work

Use the generated report at \`order-service/target/site/jacoco/index.html\` to
prioritize uncovered Order application, adapter, API, and persistence paths.
Add tests without weakening the 80% line/branch requirement, then rerun
\`./mvnw verify\`.
