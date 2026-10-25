# Ledger transaction settlement patch

This patch adds the Room entity/DAO and version 23 -> 24 migration for transaction settlements, repository/data-source operations, settlement-aware interest calculation, ViewModel save/load integration, a long-press settlement dialog on transaction cards, and unit tests for sequential principal reduction.

## Important verification note
This archive was created from the source ZIP supplied in chat, not from the full local Android project. The Gradle wrapper, complete dependency graph, and any files not included in the supplied ZIP are not present here, so `./gradlew test assembleDebug` has NOT been run. Before release, apply the changed files to the working project and run tests/build.

## Remaining integration to verify before release
- Pass settlement records through all interest summary and PDF report call sites (the included party card and ViewModel balance paths are settlement-aware; every PDF and separate Interest Account screen must also be checked).
- Confirm Room migration on a copy of a real v23 database and test backup/sync restore of settlement records.
- Confirm existing reports represent principal settlements separately from recorded interest; settlement amounts reduce principal only and do not write off accrued interest.
