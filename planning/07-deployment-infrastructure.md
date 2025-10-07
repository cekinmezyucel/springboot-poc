# Deployment & Infrastructure Enhancement Plan

## Current State Analysis
- ✅ Docker Compose for local development
- ✅ Basic GitHub Actions CI/CD
- ❌ No production-ready container images
- ❌ No Kubernetes deployment manifests
- ❌ No environment-specific configurations
- ❌ No infrastructure as code
- ❌ No blue-green deployment strategy
- ❌ No monitoring and alerting in production

## Target State
- ✅ Production-optimized Docker images
- ✅ Kubernetes deployment manifests
- ✅ Helm charts for package management
- ✅ Multi-environment configuration management
- ✅ Infrastructure as Code (Terraform)
- ✅ Advanced CI/CD pipeline with stages
- ✅ Blue-green/rolling deployment strategies
- ✅ Production monitoring and alerting

## Implementation Steps

### Step 1: Production Docker Images

#### 1.1 Multi-stage Production Dockerfile
Create `Dockerfile`:
```dockerfile
# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build

# Install dependencies for building
RUN apk add --no-cache curl

# Set working directory
WORKDIR /app

# Copy Gradle files
COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
COPY gradle.properties ./

# Download dependencies (for better caching)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src src

# Build application
RUN ./gradlew build --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine AS runtime

# Install necessary packages
RUN apk add --no-cache \
    curl \
    jq \
    dumb-init

# Create non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Create necessary directories
RUN mkdir -p /app/logs /app/config /app/data && \
    chown -R appuser:appgroup /app

# Copy built application
COPY --from=build --chown=appuser:appgroup /app/build/libs/*.jar app.jar

# Copy configuration files
COPY --chown=appuser:appgroup docker/entrypoint.sh /app/
COPY --chown=appuser:appgroup docker/healthcheck.sh /app/

# Make scripts executable
RUN chmod +x /app/entrypoint.sh /app/healthcheck.sh

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD /app/healthcheck.sh

# Use dumb-init for proper signal handling
ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD ["/app/entrypoint.sh"]

# Labels for metadata
LABEL maintainer="springboot-poc-team@company.com" \
      version="1.0.0" \
      description="SpringBoot POC Application" \
      org.opencontainers.image.title="SpringBoot POC" \
      org.opencontainers.image.description="Enterprise-grade SpringBoot application" \
      org.opencontainers.image.version="1.0.0" \
      org.opencontainers.image.created="$(date -u +'%Y-%m-%dT%H:%M:%SZ')" \
      org.opencontainers.image.source="https://github.com/company/springboot-poc"
```

#### 1.2 Docker Scripts
Create `docker/entrypoint.sh`:
```bash
#!/bin/sh
set -e

# Wait for dependencies to be ready
echo "Checking dependencies..."
if [ -n "$DATABASE_HOST" ]; then
    echo "Waiting for database at $DATABASE_HOST:${DATABASE_PORT:-5432}..."
    until nc -z "$DATABASE_HOST" "${DATABASE_PORT:-5432}"; do
        echo "Database not ready, waiting..."
        sleep 2
    done
    echo "Database is ready!"
fi

if [ -n "$REDIS_HOST" ]; then
    echo "Waiting for Redis at $REDIS_HOST:${REDIS_PORT:-6379}..."
    until nc -z "$REDIS_HOST" "${REDIS_PORT:-6379}"; do
        echo "Redis not ready, waiting..."
        sleep 2
    done
    echo "Redis is ready!"
fi

# Set JVM options for containerized environment
export JAVA_OPTS="${JAVA_OPTS} \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-production}"

# Add observability options
export JAVA_OPTS="${JAVA_OPTS} \
    -javaagent:/app/opentelemetry-javaagent.jar \
    -Dotel.service.name=springboot-poc \
    -Dotel.traces.exporter=jaeger \
    -Dotel.metrics.exporter=prometheus"

echo "Starting SpringBoot POC application..."
echo "Active profiles: ${SPRING_PROFILES_ACTIVE:-production}"
echo "Java options: $JAVA_OPTS"

exec java $JAVA_OPTS -jar /app/app.jar "$@"
```

Create `docker/healthcheck.sh`:
```bash
#!/bin/sh

# Health check endpoint
HEALTH_URL="http://localhost:8080/actuator/health"

# Perform health check
response=$(curl -f -s -o /dev/null -w "%{http_code}" "$HEALTH_URL" 2>/dev/null)

if [ "$response" = "200" ]; then
    echo "Health check passed"
    exit 0
else
    echo "Health check failed with status: $response"
    exit 1
fi
```

#### 1.3 Docker Compose for Production Testing
Create `docker-compose.prod.yml`:
```yaml
version: '3.8'

services:
  app:
    build: 
      context: .
      dockerfile: Dockerfile
      target: runtime
    container_name: springboot-poc-app
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - DATABASE_HOST=postgres
      - REDIS_HOST=redis
      - DATABASE_URL=jdbc:postgresql://postgres:5432/pocdb
      - DATABASE_USERNAME=pocuser
      - DATABASE_PASSWORD=pocpass
      - REDIS_URL=redis://redis:6379
      - JWT_ISSUER_URI=https://auth.springboot-poc.com
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "/app/healthcheck.sh"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    restart: unless-stopped
    mem_limit: 1g
    cpus: 0.5

  postgres:
    image: postgres:16-alpine
    container_name: springboot-poc-postgres-prod
    environment:
      POSTGRES_DB: pocdb
      POSTGRES_USER: pocuser
      POSTGRES_PASSWORD: pocpass
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U pocuser -d pocdb"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    container_name: springboot-poc-redis-prod
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  prometheus:
    image: prom/prometheus:v2.45.0
    container_name: springboot-poc-prometheus-prod
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--web.enable-lifecycle'
      - '--web.enable-admin-api'
    restart: unless-stopped

  grafana:
    image: grafana/grafana:10.0.0
    container_name: springboot-poc-grafana-prod
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - GF_USERS_ALLOW_SIGN_UP=false
    volumes:
      - grafana_data:/var/lib/grafana
      - ./monitoring/grafana/dashboards:/etc/grafana/provisioning/dashboards:ro
      - ./monitoring/grafana/datasources:/etc/grafana/provisioning/datasources:ro
    restart: unless-stopped

volumes:
  postgres_data:
  redis_data:
  prometheus_data:
  grafana_data:

networks:
  default:
    name: springboot-poc-network
```

### Step 2: Kubernetes Deployment Manifests

#### 2.1 Namespace and Configuration
Create `k8s/namespace.yaml`:
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: springboot-poc
  labels:
    name: springboot-poc
    environment: production
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: springboot-poc-config
  namespace: springboot-poc
data:
  application.yaml: |
    spring:
      profiles:
        active: production
      datasource:
        url: jdbc:postgresql://postgres-service:5432/pocdb
        username: ${DATABASE_USERNAME}
        password: ${DATABASE_PASSWORD}
      data:
        redis:
          host: redis-service
          port: 6379
    
    management:
      endpoints:
        web:
          exposure:
            include: health,info,metrics,prometheus
      endpoint:
        health:
          show-details: always
    
    logging:
      level:
        root: INFO
        com.cekinmezyucel.springboot.poc: DEBUG
---
apiVersion: v1
kind: Secret
metadata:
  name: springboot-poc-secrets
  namespace: springboot-poc
type: Opaque
data:
  database-username: cG9jdXNlcg==  # pocuser base64 encoded
  database-password: cG9jcGFzcw==  # pocpass base64 encoded
  jwt-secret: bXktand0LXNlY3JldC1rZXk=  # jwt secret base64 encoded
```

#### 2.2 PostgreSQL Deployment
Create `k8s/postgres.yaml`:
```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
  namespace: springboot-poc
spec:
  serviceName: postgres-service
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:16-alpine
        ports:
        - containerPort: 5432
        env:
        - name: POSTGRES_DB
          value: pocdb
        - name: POSTGRES_USER
          valueFrom:
            secretKeyRef:
              name: springboot-poc-secrets
              key: database-username
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: springboot-poc-secrets
              key: database-password
        - name: PGDATA
          value: /var/lib/postgresql/data/pgdata
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
        livenessProbe:
          exec:
            command:
            - pg_isready
            - -U
            - $(POSTGRES_USER)
            - -d
            - $(POSTGRES_DB)
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          exec:
            command:
            - pg_isready
            - -U
            - $(POSTGRES_USER)
            - -d
            - $(POSTGRES_DB)
          initialDelaySeconds: 5
          periodSeconds: 5
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
  volumeClaimTemplates:
  - metadata:
      name: postgres-storage
    spec:
      accessModes: [ "ReadWriteOnce" ]
      resources:
        requests:
          storage: 10Gi
---
apiVersion: v1
kind: Service
metadata:
  name: postgres-service
  namespace: springboot-poc
spec:
  selector:
    app: postgres
  ports:
  - port: 5432
    targetPort: 5432
  type: ClusterIP
```

#### 2.3 Application Deployment
Create `k8s/deployment.yaml`:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: springboot-poc-app
  namespace: springboot-poc
  labels:
    app: springboot-poc
    version: v1
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: springboot-poc
  template:
    metadata:
      labels:
        app: springboot-poc
        version: v1
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      serviceAccountName: springboot-poc-sa
      securityContext:
        runAsNonRoot: true
        runAsUser: 1001
        runAsGroup: 1001
        fsGroup: 1001
      containers:
      - name: app
        image: springboot-poc:latest
        imagePullPolicy: Always
        ports:
        - containerPort: 8080
          name: http
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: DATABASE_USERNAME
          valueFrom:
            secretKeyRef:
              name: springboot-poc-secrets
              key: database-username
        - name: DATABASE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: springboot-poc-secrets
              key: database-password
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: springboot-poc-secrets
              key: jwt-secret
        - name: JAVA_OPTS
          value: "-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
        volumeMounts:
        - name: config-volume
          mountPath: /app/config
        - name: logs-volume
          mountPath: /app/logs
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 10
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        startupProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 30
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        securityContext:
          allowPrivilegeEscalation: false
          capabilities:
            drop:
            - ALL
          readOnlyRootFilesystem: true
      volumes:
      - name: config-volume
        configMap:
          name: springboot-poc-config
      - name: logs-volume
        emptyDir: {}
      terminationGracePeriodSeconds: 30
---
apiVersion: v1
kind: Service
metadata:
  name: springboot-poc-service
  namespace: springboot-poc
  labels:
    app: springboot-poc
spec:
  selector:
    app: springboot-poc
  ports:
  - port: 80
    targetPort: 8080
    name: http
  type: ClusterIP
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: springboot-poc-sa
  namespace: springboot-poc
```

#### 2.4 Ingress Configuration
Create `k8s/ingress.yaml`:
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: springboot-poc-ingress
  namespace: springboot-poc
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
    nginx.ingress.kubernetes.io/rate-limit: "100"
    nginx.ingress.kubernetes.io/rate-limit-window: "1m"
spec:
  tls:
  - hosts:
    - api.springboot-poc.com
    secretName: springboot-poc-tls
  rules:
  - host: api.springboot-poc.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: springboot-poc-service
            port:
              number: 80
```

### Step 3: Helm Charts

#### 3.1 Helm Chart Structure
Create `helm/springboot-poc/Chart.yaml`:
```yaml
apiVersion: v2
name: springboot-poc
description: A Helm chart for SpringBoot POC application
type: application
version: 1.0.0
appVersion: "1.0.0"
keywords:
  - springboot
  - java
  - api
  - microservice
home: https://github.com/company/springboot-poc
sources:
  - https://github.com/company/springboot-poc
maintainers:
  - name: SpringBoot POC Team
    email: springboot-poc-team@company.com
dependencies:
  - name: postgresql
    version: 11.9.13
    repository: https://charts.bitnami.com/bitnami
    condition: postgresql.enabled
  - name: redis
    version: 17.11.3
    repository: https://charts.bitnami.com/bitnami
    condition: redis.enabled
```

#### 3.2 Values Configuration
Create `helm/springboot-poc/values.yaml`:
```yaml
# Default values for springboot-poc
replicaCount: 3

image:
  repository: springboot-poc
  pullPolicy: Always
  tag: "latest"

imagePullSecrets: []
nameOverride: ""
fullnameOverride: ""

serviceAccount:
  create: true
  annotations: {}
  name: ""

podAnnotations:
  prometheus.io/scrape: "true"
  prometheus.io/port: "8080"
  prometheus.io/path: "/actuator/prometheus"

podSecurityContext:
  runAsNonRoot: true
  runAsUser: 1001
  runAsGroup: 1001
  fsGroup: 1001

securityContext:
  allowPrivilegeEscalation: false
  capabilities:
    drop:
    - ALL
  readOnlyRootFilesystem: true

service:
  type: ClusterIP
  port: 80
  targetPort: 8080

ingress:
  enabled: true
  className: "nginx"
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/rate-limit: "100"
  hosts:
    - host: api.springboot-poc.com
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: springboot-poc-tls
      hosts:
        - api.springboot-poc.com

resources:
  limits:
    cpu: 500m
    memory: 1Gi
  requests:
    cpu: 250m
    memory: 512Mi

autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 10
  targetCPUUtilizationPercentage: 80
  targetMemoryUtilizationPercentage: 80

nodeSelector: {}

tolerations: []

affinity:
  podAntiAffinity:
    preferredDuringSchedulingIgnoredDuringExecution:
    - weight: 100
      podAffinityTerm:
        labelSelector:
          matchExpressions:
          - key: app.kubernetes.io/name
            operator: In
            values:
            - springboot-poc
        topologyKey: kubernetes.io/hostname

# Application configuration
application:
  profiles:
    active: production
  
  database:
    host: ""  # Will be set by dependency
    port: 5432
    name: pocdb
    username: pocuser
    password: pocpass
  
  redis:
    host: ""  # Will be set by dependency
    port: 6379
  
  jwt:
    issuerUri: https://auth.springboot-poc.com

# PostgreSQL dependency configuration
postgresql:
  enabled: true
  auth:
    postgresPassword: pocpass
    username: pocuser
    password: pocpass
    database: pocdb
  primary:
    persistence:
      enabled: true
      size: 10Gi

# Redis dependency configuration
redis:
  enabled: true
  auth:
    enabled: false
  master:
    persistence:
      enabled: true
      size: 5Gi

# Monitoring
monitoring:
  serviceMonitor:
    enabled: true
    namespace: monitoring
    interval: 30s
    path: /actuator/prometheus
```

### Step 4: Advanced CI/CD Pipeline

#### 4.1 Enhanced GitHub Actions Workflow
Create `.github/workflows/cd-pipeline.yml`:
```yaml
name: CD Pipeline

on:
  push:
    branches: [main]
    tags: ['v*']
  workflow_dispatch:
    inputs:
      environment:
        description: 'Deployment environment'
        required: true
        default: 'staging'
        type: choice
        options:
        - staging
        - production

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    outputs:
      version: ${{ steps.version.outputs.version }}
      image-tag: ${{ steps.meta.outputs.tags }}
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      with:
        fetch-depth: 0

    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'

    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-

    - name: Grant execute permission for gradlew
      run: chmod +x gradlew

    - name: Run tests
      run: ./gradlew test

    - name: Generate test report
      uses: dorny/test-reporter@v1
      if: success() || failure()
      with:
        name: Gradle Tests
        path: build/test-results/test/*.xml
        reporter: java-junit

    - name: Build application
      run: ./gradlew build -x test

    - name: Generate version
      id: version
      run: |
        if [[ $GITHUB_REF == refs/tags/* ]]; then
          VERSION=${GITHUB_REF#refs/tags/}
        else
          VERSION=${{ github.sha }}
        fi
        echo "version=$VERSION" >> $GITHUB_OUTPUT

    - name: Upload build artifacts
      uses: actions/upload-artifact@v3
      with:
        name: jar-artifact
        path: build/libs/*.jar

  build-image:
    needs: build-and-test
    runs-on: ubuntu-latest
    outputs:
      image: ${{ steps.image.outputs.image }}
    steps:
    - name: Checkout code
      uses: actions/checkout@v4

    - name: Download build artifacts
      uses: actions/download-artifact@v3
      with:
        name: jar-artifact
        path: build/libs/

    - name: Log in to Container Registry
      uses: docker/login-action@v3
      with:
        registry: ${{ env.REGISTRY }}
        username: ${{ github.actor }}
        password: ${{ secrets.GITHUB_TOKEN }}

    - name: Extract metadata
      id: meta
      uses: docker/metadata-action@v5
      with:
        images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
        tags: |
          type=ref,event=branch
          type=ref,event=pr
          type=semver,pattern={{version}}
          type=semver,pattern={{major}}.{{minor}}
          type=sha,prefix={{branch}}-

    - name: Build and push Docker image
      id: build
      uses: docker/build-push-action@v5
      with:
        context: .
        push: true
        tags: ${{ steps.meta.outputs.tags }}
        labels: ${{ steps.meta.outputs.labels }}
        cache-from: type=gha
        cache-to: type=gha,mode=max

    - name: Output image
      id: image
      run: echo "image=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ needs.build-and-test.outputs.version }}" >> $GITHUB_OUTPUT

  security-scan:
    needs: build-image
    runs-on: ubuntu-latest
    steps:
    - name: Run Trivy vulnerability scanner
      uses: aquasecurity/trivy-action@master
      with:
        image-ref: ${{ needs.build-image.outputs.image }}
        format: 'sarif'
        output: 'trivy-results.sarif'

    - name: Upload Trivy scan results
      uses: github/codeql-action/upload-sarif@v2
      if: always()
      with:
        sarif_file: 'trivy-results.sarif'

  deploy-staging:
    needs: [build-and-test, build-image, security-scan]
    runs-on: ubuntu-latest
    environment: staging
    if: github.ref == 'refs/heads/main' || github.event.inputs.environment == 'staging'
    steps:
    - name: Checkout code
      uses: actions/checkout@v4

    - name: Set up Helm
      uses: azure/setup-helm@v3
      with:
        version: '3.12.0'

    - name: Set up kubectl
      uses: azure/setup-kubectl@v3
      with:
        version: '1.27.0'

    - name: Configure kubectl
      run: |
        echo "${{ secrets.KUBECONFIG_STAGING }}" | base64 -d > kubeconfig
        export KUBECONFIG=kubeconfig

    - name: Deploy to staging
      run: |
        export KUBECONFIG=kubeconfig
        helm upgrade --install springboot-poc-staging ./helm/springboot-poc \
          --namespace springboot-poc-staging \
          --create-namespace \
          --set image.tag=${{ needs.build-and-test.outputs.version }} \
          --set ingress.hosts[0].host=staging-api.springboot-poc.com \
          --set application.profiles.active=staging \
          --wait --timeout=10m

    - name: Run smoke tests
      run: |
        # Wait for deployment to be ready
        sleep 60
        # Run basic health check
        curl -f https://staging-api.springboot-poc.com/actuator/health

  deploy-production:
    needs: [build-and-test, build-image, security-scan, deploy-staging]
    runs-on: ubuntu-latest
    environment: production
    if: startsWith(github.ref, 'refs/tags/') || github.event.inputs.environment == 'production'
    steps:
    - name: Checkout code
      uses: actions/checkout@v4

    - name: Set up Helm
      uses: azure/setup-helm@v3
      with:
        version: '3.12.0'

    - name: Set up kubectl
      uses: azure/setup-kubectl@v3
      with:
        version: '1.27.0'

    - name: Configure kubectl
      run: |
        echo "${{ secrets.KUBECONFIG_PRODUCTION }}" | base64 -d > kubeconfig
        export KUBECONFIG=kubeconfig

    - name: Deploy to production
      run: |
        export KUBECONFIG=kubeconfig
        helm upgrade --install springboot-poc ./helm/springboot-poc \
          --namespace springboot-poc-production \
          --create-namespace \
          --set image.tag=${{ needs.build-and-test.outputs.version }} \
          --set replicaCount=5 \
          --set resources.limits.cpu=1000m \
          --set resources.limits.memory=2Gi \
          --wait --timeout=15m

    - name: Run production smoke tests
      run: |
        # Wait for deployment to be ready
        sleep 120
        # Run comprehensive health checks
        curl -f https://api.springboot-poc.com/actuator/health
        curl -f https://api.springboot-poc.com/api/v2/health

    - name: Notify deployment success
      if: success()
      run: |
        echo "Production deployment successful!"
        # Add notification logic (Slack, email, etc.)

  rollback:
    if: failure() && github.ref == 'refs/heads/main'
    needs: [deploy-production]
    runs-on: ubuntu-latest
    environment: production
    steps:
    - name: Rollback production deployment
      run: |
        echo "${{ secrets.KUBECONFIG_PRODUCTION }}" | base64 -d > kubeconfig
        export KUBECONFIG=kubeconfig
        helm rollback springboot-poc --namespace springboot-poc-production
```

### Step 5: Infrastructure as Code

#### 5.1 Terraform Configuration
Create `terraform/main.tf`:
```hcl
terraform {
  required_version = ">= 1.0"
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.20"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.10"
    }
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  backend "s3" {
    bucket = "springboot-poc-terraform-state"
    key    = "infrastructure/terraform.tfstate"
    region = "us-west-2"
  }
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "production"
}

variable "cluster_name" {
  description = "EKS cluster name"
  type        = string
  default     = "springboot-poc-cluster"
}

variable "node_groups" {
  description = "EKS node groups configuration"
  type = map(object({
    instance_types = list(string)
    min_size      = number
    max_size      = number
    desired_size  = number
  }))
  default = {
    general = {
      instance_types = ["t3.medium"]
      min_size      = 2
      max_size      = 10
      desired_size  = 3
    }
  }
}

# EKS Cluster
module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 19.0"

  cluster_name    = var.cluster_name
  cluster_version = "1.27"

  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets

  cluster_endpoint_public_access = true
  cluster_endpoint_private_access = true

  cluster_addons = {
    coredns = {
      most_recent = true
    }
    kube-proxy = {
      most_recent = true
    }
    vpc-cni = {
      most_recent = true
    }
    aws-ebs-csi-driver = {
      most_recent = true
    }
  }

  eks_managed_node_groups = {
    for name, config in var.node_groups : name => {
      min_size       = config.min_size
      max_size       = config.max_size
      desired_size   = config.desired_size
      instance_types = config.instance_types

      k8s_labels = {
        Environment = var.environment
        NodeGroup   = name
      }
    }
  }

  tags = {
    Environment = var.environment
    Project     = "springboot-poc"
  }
}

# VPC
module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 5.0"

  name = "${var.cluster_name}-vpc"
  cidr = "10.0.0.0/16"

  azs             = ["us-west-2a", "us-west-2b", "us-west-2c"]
  private_subnets = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
  public_subnets  = ["10.0.101.0/24", "10.0.102.0/24", "10.0.103.0/24"]

  enable_nat_gateway = true
  enable_vpn_gateway = false
  enable_dns_hostnames = true
  enable_dns_support = true

  public_subnet_tags = {
    "kubernetes.io/role/elb" = "1"
  }

  private_subnet_tags = {
    "kubernetes.io/role/internal-elb" = "1"
  }

  tags = {
    Environment = var.environment
    Project     = "springboot-poc"
  }
}

# RDS for PostgreSQL
resource "aws_db_instance" "postgres" {
  identifier = "springboot-poc-${var.environment}"

  engine         = "postgres"
  engine_version = "16.1"
  instance_class = "db.t3.micro"

  allocated_storage     = 20
  max_allocated_storage = 100
  storage_type         = "gp2"
  storage_encrypted    = true

  db_name  = "pocdb"
  username = "pocuser"
  password = random_password.db_password.result

  vpc_security_group_ids = [aws_security_group.rds.id]
  db_subnet_group_name   = aws_db_subnet_group.postgres.name

  backup_retention_period = 7
  backup_window          = "03:00-04:00"
  maintenance_window     = "sun:04:00-sun:05:00"

  skip_final_snapshot = var.environment != "production"
  deletion_protection = var.environment == "production"

  tags = {
    Environment = var.environment
    Project     = "springboot-poc"
  }
}

resource "random_password" "db_password" {
  length  = 32
  special = true
}

# ElastiCache for Redis
resource "aws_elasticache_subnet_group" "redis" {
  name       = "springboot-poc-${var.environment}-redis"
  subnet_ids = module.vpc.private_subnets
}

resource "aws_elasticache_replication_group" "redis" {
  description          = "Redis cluster for springboot-poc ${var.environment}"
  replication_group_id = "springboot-poc-${var.environment}"
  
  node_type            = "cache.t3.micro"
  port                 = 6379
  parameter_group_name = "default.redis7"
  
  num_cache_clusters = 2
  
  subnet_group_name  = aws_elasticache_subnet_group.redis.name
  security_group_ids = [aws_security_group.redis.id]
  
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
  
  tags = {
    Environment = var.environment
    Project     = "springboot-poc"
  }
}

# Outputs
output "cluster_endpoint" {
  value = module.eks.cluster_endpoint
}

output "cluster_security_group_id" {
  value = module.eks.cluster_security_group_id
}

output "database_endpoint" {
  value = aws_db_instance.postgres.endpoint
}

output "redis_endpoint" {
  value = aws_elasticache_replication_group.redis.primary_endpoint_address
}
```

## Validation Checklist

### ✅ Docker Images
- [ ] Multi-stage build optimized
- [ ] Non-root user configured
- [ ] Health checks implemented
- [ ] Security scanning passed
- [ ] Image size optimized

### ✅ Kubernetes Deployment
- [ ] Namespace isolation configured
- [ ] Resource limits set
- [ ] Health probes configured
- [ ] Security context applied
- [ ] ConfigMaps and Secrets used

### ✅ Helm Charts
- [ ] Dependencies configured
- [ ] Values parameterized
- [ ] Templates validated
- [ ] Chart versioning implemented
- [ ] Deployment strategies configured

### ✅ CI/CD Pipeline
- [ ] Multi-stage pipeline implemented
- [ ] Security scanning integrated
- [ ] Environment-specific deployments
- [ ] Rollback strategy configured
- [ ] Monitoring and notifications

### ✅ Infrastructure as Code
- [ ] Terraform state management
- [ ] Resource tagging consistent
- [ ] Security groups configured
- [ ] Backup strategies implemented
- [ ] Cost optimization applied

## Troubleshooting

### Common Issues
1. **Image pull errors**: Check registry authentication and image tags
2. **Pod startup failures**: Review resource limits and health check timeouts
3. **Network connectivity**: Verify security groups and network policies
4. **Persistent volume issues**: Check storage class and volume claims
5. **Helm deployment failures**: Validate templates and dependency requirements

### Best Practices
- Use immutable infrastructure principles
- Implement proper resource tagging
- Monitor deployment metrics and logs
- Regular backup and disaster recovery testing
- Implement proper secret management

## Next Steps
After implementing deployment infrastructure:
1. Set up monitoring and alerting in production
2. Implement disaster recovery procedures
3. Add automated performance testing
4. Set up log aggregation and analysis
5. Implement chaos engineering practices

## AI Agent Notes
- Always test deployments in staging before production
- Verify resource limits and scaling configurations
- Ensure proper secret management and rotation
- Monitor deployment metrics and set up alerting
- Implement proper backup and recovery procedures