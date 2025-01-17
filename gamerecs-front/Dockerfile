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

# Final stage
FROM nginx:alpine

# Copy nginx config template
COPY nginx.conf /etc/nginx/conf.d/default.conf.template

# Copy entrypoint script
COPY docker-entrypoint.sh /
RUN chmod +x /docker-entrypoint.sh

# Copy built app
COPY --from=builder /app/dist/gamerecs-front /usr/share/nginx/html

# Set entrypoint
ENTRYPOINT ["/docker-entrypoint.sh"]

# Install required tools
RUN apk add --no-cache wget gettext

# Add non-root user
RUN adduser -D -u 1001 gamerecs && \
    chown -R gamerecs:gamerecs /usr/share/nginx/html && \
    chown -R gamerecs:gamerecs /var/cache/nginx && \
    chown -R gamerecs:gamerecs /var/log/nginx && \
    chown -R gamerecs:gamerecs /etc/nginx/conf.d && \
    touch /var/run/nginx.pid && \
    chown -R gamerecs:gamerecs /var/run/nginx.pid

# Health check using assigned port
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT}/health || exit 1

# Use custom entrypoint
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"] 
