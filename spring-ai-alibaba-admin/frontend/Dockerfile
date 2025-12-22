# Stage 1: Build the frontend application
FROM node:18-alpine AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./
COPY packages/main/package.json ./packages/main/
COPY packages/spark-flow/package.json ./packages/spark-flow/
COPY packages/spark-i18n/package.json ./packages/spark-i18n/

# Install dependencies
RUN npm install

# Copy source code
COPY . .

# Build spark-flow first (required dependency)
RUN npm run build:flow

# Build the main application
RUN npm run build:app

# Stage 2: Serve with nginx
FROM nginx:alpine

# Copy built files from builder stage
COPY --from=builder /app/packages/main/dist /usr/share/nginx/html

# Copy nginx configuration (optional, if you have custom nginx config)
# COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 80
EXPOSE 80

# Start nginx
CMD ["nginx", "-g", "daemon off;"]

