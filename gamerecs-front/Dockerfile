# Stage 1: Install dependencies and build
FROM node:22 AS builder
WORKDIR /app

# Copy package files
COPY package*.json ./

# Clean install dependencies, forcing platform-specific rebuild
RUN npm cache clean --force && \
    rm -rf node_modules && \
    npm install

# Copy source code
COPY . .

# Build the application
RUN npm run build

# Stage 2: Production image
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Install wget for health checks
RUN apk add --no-cache wget

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
