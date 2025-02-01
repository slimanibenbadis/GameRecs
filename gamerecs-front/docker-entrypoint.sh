#!/bin/sh
# Replace environment variables in nginx config
envsubst '${BACKEND_URL} ${PORT}' < /etc/nginx/conf.d/default.conf > /etc/nginx/conf.d/default.conf.tmp
mv /etc/nginx/conf.d/default.conf.tmp /etc/nginx/conf.d/default.conf

# Replace environment variables in JS files
/replace-env-vars.sh

# Execute nginx
exec nginx -g 'daemon off;' 
