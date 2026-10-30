# Frontend image: builds the React app and serves it with nginx, which also forwards
# /api requests to the backend container. Built by `docker compose up --build`.

# Stage 1: build the static files (index.html, JavaScript, CSS) into dist/
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# Stage 2: serve them with nginx
FROM nginx:1.28-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
