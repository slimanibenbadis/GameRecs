# Stage 1: Install dependencies
FROM node:20-alpine AS dependencies
WORKDIR /app

# Copy package files for dependency installation
COPY package*.json ./

# Install dependencies with cache mounting
RUN --mount=type=cache,target=/root/.npm \
    npm ci --only=production

# Stage 2: Build the application
FROM node:20-alpine AS builder
WORKDIR /app

# Copy dependencies from previous stage
COPY --from=dependencies /app/node_modules ./node_modules
COPY . .

# Build with production optimization
RUN npm run build

# Stage 3: Production image
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Add non-root user
RUN adduser -D -u 1001 gamerecs && \
    chown -R gamerecs:gamerecs /usr/share/nginx/html && \
    chown -R gamerecs:gamerecs /var/cache/nginx && \
    chown -R gamerecs:gamerecs /var/log/nginx && \
    chown -R gamerecs:gamerecs /etc/nginx/conf.d && \
    touch /var/run/nginx.pid && \
    chown -R gamerecs:gamerecs /var/run/nginx.pid

# Copy the build output and nginx config
COPY --from=builder --chown=gamerecs:gamerecs /app/dist/gamerecs-front/browser .
COPY --chown=gamerecs:gamerecs nginx.conf /etc/nginx/conf.d/default.conf

# Switch to non-root user
USER 1001

# Add health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:80/health || exit 1

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"] 