# Pilot deployment

Verified on 2026-11-14:

- Application: https://129-159-1-166.sslip.io
- Administration: https://129-159-1-166.sslip.io/admin/
- Oracle Linux ARM host, Docker Compose, authenticated MongoDB replica set, Spring Boot production profile and Caddy HTTPS.
- Public trusted TLS, root login, empty community list, secure HttpOnly cookies and rejection of development credentials verified through Chromium.
- Initial root bootstrap disabled after creation; login verified again after backend recreation.
- Only web ports are published. Backend and MongoDB remain on the internal Docker network.
- Initial database backup completed and copied to protected operator storage outside the repository. Backup restore was separately verified using a synthetic database before deployment.

The pilot starts without communities, apartments or demo users. Provision them through the root administration interface. Credentials are stored outside this repository and must not be committed.

This is a single-host pilot. Email delivery and scheduled off-server backups are not configured. The temporary hostname depends on the current public IP and a third-party DNS service. See README.md for operations and limitations.

## Joining-month visibility update — 2026-11-15

Resident charge visibility now starts on the first day of the active relationship's joining month. Earlier billing periods stay hidden. Relationship end dates and payment-date checks remain unchanged; charge amounts are not prorated. Existing relationship and debt records were preserved. A database backup and previous backend image were retained before the backend update. Public HTTPS and root login were verified after deployment.

## Readable theme — 2026-11-15

Both frontends use 18px base text, darker supporting text, system fonts, larger form controls and visible keyboard focus. All 34 existing frontend browser tests passed. Resident desktop/mobile layouts were visually inspected locally; public administration layouts were checked at 320px and 390px without document overflow. The previous web image is retained as `govener-pilot-web:before-readable-theme`.

The subsequent theme adjustment reduces base text to 16px at the owner's request and adds soft green, blue, peach and lavender accents. Larger touch controls and dark text remain in place.
