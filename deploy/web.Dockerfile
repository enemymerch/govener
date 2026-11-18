FROM node:22-alpine AS build
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend ./
RUN npm run build
WORKDIR /workspace/admin-frontend
COPY admin-frontend/package.json admin-frontend/package-lock.json ./
RUN npm ci
COPY admin-frontend ./
ENV ADMIN_BASE_PATH=/admin/
RUN npm run build

FROM caddy:2-alpine
COPY --from=build /workspace/frontend/dist /srv/web
COPY --from=build /workspace/admin-frontend/dist /srv/admin
COPY deploy/Caddyfile /etc/caddy/Caddyfile
EXPOSE 80 443
