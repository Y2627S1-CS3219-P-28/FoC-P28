# Shared penalty policy library

Follow root AGENTS.md. This ordinary Maven JAR currently shares only
sg.edu.nus.foc.common.penalty.PenaltySystemPolicy, per Order ADR-043 / CHANGE-130.

- CHANGE-132 renames the old CourierPenaltyCase Java type. Keep static queries:
  getSuccessfulDeliveryReward returns SUCCESSFUL_DELIVERY; getCourierPenaltyCases
  excludes ACCEPTED_TASK_CANCELLATION, OVERDUE_DELIVERY, SUCCESSFUL_DELIVERY;
  getFullPenaltyPolicy includes all six. Returned lists are immutable and ordered.
  Do not confuse the report-case subset with all automatic addition reasons.

- Keep approved names/amounts stable: accepted cancellation 2, overdue delivery 1,
  successful delivery 1, wrong-location delivery 4, mishandling/damage 4,
  deliberate retention/theft 10 (CHANGE-131, D1 NTH2 PDF37-39).
  Amounts are positive; only successful delivery deducts. Report misconduct needs
  administrator confirmation. The mapping does not enforce suspension, select
  the highest incident or implement report resolution; do not claim otherwise.
- No authentication, service entities/repositories, persistence, HTTP endpoints,
  Spring runtime configuration or application service logic in this module.
- No Dockerfile, database or deployment. Services bundle this dependency.
- Verify with root `bash ./mvnw -pl foc-common verify` or PowerShell
  `.\mvnw.cmd -pl foc-common verify`; retain 80% line/branch coverage gates.
- Policy/wire changes require team agreement and consumer compatibility review.
