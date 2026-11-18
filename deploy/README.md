# Govener single-server pilot

This deployment runs the Spring Boot monolith, an authenticated single-node MongoDB replica set and two built static frontends behind Caddy HTTPS. Only ports 80/443 are published. MongoDB and the backend are reachable only inside the Docker network. This is one server with durable volumes, not a highly available cluster.

## Prerequisites

- An ARM64 or x86-64 Linux host with Docker Engine and Docker Compose.
- A hostname resolving to its public IP and inbound TCP 80/443 allowed by both the cloud network and host firewall.
- A protected secrets directory outside the checkout. Never commit actual `.env` files, database credentials, root passwords, SSH keys or backups.
- No conflicting web server on ports 80/443. Development MongoDB volumes and users are not copied into this installation.

## Configuration

Copy `.env.example` to `/home/opc/govener-secrets/pilot.env`. Set directory permissions to 700 and files to 600. Supply independent random database root/app passwords, a random cursor secret of at least 32 characters, the root login email and an initial random root password. Create a random MongoDB replica-set keyfile and set its path in `GOVENER_MONGO_KEYFILE`. Hexadecimal secrets avoid URI escaping problems in the connection string.

`GOVENER_HOST` is the hostname only, without `https://` or a path. The URLs will be:

- `https://<hostname>/`: manager/resident application.
- `https://<hostname>/admin/`: root/admin application.

The administrative frontend is built with `ADMIN_BASE_PATH=/admin/`; both applications call `/api/v1` on the same origin. Sessions are therefore shared between the two frontends, including sign-out. TLS certificates and private keys are retained in the Caddy data volume.

## Build and first start

From this directory:

```sh
docker compose --env-file /home/opc/govener-secrets/pilot.env config --quiet
docker compose --env-file /home/opc/govener-secrets/pilot.env build
# Set GOVENER_ROOT_BOOTSTRAP_ENABLED=true for this first start only.
docker compose --env-file /home/opc/govener-secrets/pilot.env up -d --wait
```

The `prod` profile keeps secure HttpOnly cookies and disables fixtures. A production configuration guard rejects development profiles/secrets, anonymous MongoDB connections and non-HTTPS origins. Database collections and indexes are created before startup runners, allowing the first root account transaction to run against an empty database. MongoDB root credentials are used for initialization and backups; the application has a separate database-scoped read/write user.

After verifying the initial root login, set `GOVENER_ROOT_BOOTSTRAP_ENABLED=false` and clear `GOVENER_ROOT_PASSWORD` in the runtime env file. Retain the original login password only in the operator's password manager or protected local access file. Recreate the backend:

```sh
docker compose --env-file /home/opc/govener-secrets/pilot.env up -d --no-deps backend
```

The bootstrap does not overwrite an existing root account. Recreating the backend invalidates existing in-memory sessions. Use root to create communities and administrators, then provision units and manager/resident accounts. Demo passwords are not valid in this deployment.

## Operations and updates

```sh
docker compose --env-file /home/opc/govener-secrets/pilot.env ps
docker compose --env-file /home/opc/govener-secrets/pilot.env logs --tail=100 backend
docker compose --env-file /home/opc/govener-secrets/pilot.env logs --tail=100 web
```

Build/test the exact source revision before uploading it. Keep previous application images/source revisions for rollback and back up MongoDB before updates. Do not run `down -v`; that removes persistent database/certificate volumes. Container restart policies restore processes after reboot, but sessions do not survive backend restarts. Logs are size-limited.

Email delivery is initially disabled. Password-reset email is unavailable until an SMTP provider, sender and public reset URL are configured. This pilot has no external mail account, payment gateway, automatic off-server backup schedule or uptime SLA.

## Backups

Create `backups/` with operator ownership and permissions 700; the backup container runs as UID/GID 1000. Change this service user if the host operator has a different ID.

```sh
docker compose --env-file /home/opc/govener-secrets/pilot.env --profile maintenance run --rm backup
```

The resulting compressed archive includes the oplog for a consistent restore and contains sensitive application data and credential hashes. Copy backups to protected off-server storage; a backup only on this VM does not protect against VM loss. Test restore in a separate disposable MongoDB instance, never against the live database as a smoke check.

## Pilot hostname

A public DNS mapping service such as sslip.io can provide a temporary hostname without purchasing a domain. This hostname depends on a third party and on the VM's public IP. An ephemeral IP may be lost when unassigned or the instance is terminated. Move to an owned domain before relying on the address commercially.
