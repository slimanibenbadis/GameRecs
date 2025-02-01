#!/bin/sh
echo "Replacing environment variables in JS files"
for file in $(find /usr/share/nginx/html -type f -name "*.js"); do
  echo "Processing $file"
  sed -i "s|\\\${BACKEND_URL}|${BACKEND_URL}|g" $file
  sed -i "s|\\\${GOOGLE_OAUTH_CLIENT_ID}|${GOOGLE_OAUTH_CLIENT_ID}|g" $file
done
echo "Finished replacing environment variables" 
