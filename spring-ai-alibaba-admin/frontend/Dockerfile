# Stage 1: Build the frontend application
FROM node:18-alpine AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./
COPY packages/main/package.json ./packages/main/
COPY packages/spark-flow/package.json ./packages/spark-flow/
COPY packages/spark-i18n/package.json ./packages/spark-i18n/

# Install dependencies. HUSKY=0 avoids husky prepare failing (.husky is dockerignored).
ENV HUSKY=0
RUN npm install --legacy-peer-deps

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

COPY nginx.conf /etc/nginx/conf.d/default.conf
RUN apk add --no-cache curl

# Expose port 80
EXPOSE 80

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
