# Production deployment

The repository contains a single-host Docker Compose deployment for the Vue frontend, five Spring Boot applications, MySQL, Redis, Nacos, and RocketMQ. Only the frontend/Nginx container publishes a host port.

## Prerequisites

- Linux server with Docker Engine and Docker Compose v2
- At least 4 CPU cores, 8 GB RAM, and 80 GB disk for the complete stack
- An Aliyun OSS bucket and RAM user credentials
- Ports 80 and 443 allowed by the cloud firewall; infrastructure ports should remain private

## Configure

Create the local secrets file and replace every placeholder:

```bash
cp .env.example .env
nano .env
```

The `.env` file is ignored by Git. Use long random values and grant the OSS RAM user only the permissions required for the selected bucket.

Nacos authentication starts with its built-in `nacos` user. After the first successful deployment, change that password in the Nacos console and update `NACOS_PASSWORD` in `.env`. The console is not exposed publicly by this Compose file. If temporary access is needed, use an SSH tunnel:

```bash
ssh -L 8848:127.0.0.1:8848 user@server
```

Temporarily add `127.0.0.1:8848:8848` to the Nacos service ports while administering it, then remove that mapping.

XXL-Job Admin is deliberately not included because it needs its own database schema and authentication lifecycle. With `XXL_JOB_ADMIN_ADDRESSES` empty, executors do not register with an admin server. When an admin deployment is ready, set the variable to its internal URL, for example `http://xxl-job-admin:8080/xxl-job-admin`.

## Build and start

```bash
docker compose config
docker compose build
docker compose up -d
docker compose ps
```

The first startup initializes `teiba_x` from `docs/sql_design/teiba-x.sql`. MySQL only runs initialization scripts for an empty data volume. Later SQL changes must be applied as migrations or manually; restarting the container does not re-run the file.

Inspect startup logs:

```bash
docker compose logs -f --tail=200 gateway user-server post-server bar-server file-server
```

Open `http://SERVER_IP/` after every application has registered with Nacos. API traffic under `/api/` is forwarded by Nginx to the gateway without removing the `/api` prefix.

## Update

```bash
git pull --ff-only
docker compose build
docker compose up -d --remove-orphans
```

## Back up and stop

Back up MySQL before an upgrade. A normal stop preserves named volumes:

```bash
docker compose down
```

Do not use `docker compose down -v` in production because it deletes the database and other persistent volumes.

## HTTPS

For a public deployment, put a TLS reverse proxy such as Caddy, Traefik, or a host-level Nginx in front of port 80. Publish only ports 80/443 and do not expose MySQL, Redis, Nacos, RocketMQ, gateway, or application service ports to the internet.
