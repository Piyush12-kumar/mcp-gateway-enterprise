# Kubernetes Deployment Guide

This directory contains Kubernetes manifests for deploying MCP Gateway to a Kubernetes cluster.

## Prerequisites

- Kubernetes cluster (v1.25+)
- `kubectl` configured to access your cluster
- Docker image built and pushed to a registry

## Files

| File | Description |
|------|-------------|
| `00-namespace-config.yaml` | Namespace, ConfigMap, Secrets, ServiceAccount |
| `10-mysql.yaml` | MySQL StatefulSet with persistent storage |
| `20-backend.yaml` | Backend Deployment, Service, HPA, Ingress |

## Quick Start

### 1. Build and Push Docker Image

```bash
# Build the image
docker build -t your-registry/mcp-gateway-backend:latest .

# Push to registry
docker push your-registry/mcp-gateway-backend:latest
```

### 2. Update Image Reference

Edit `20-backend.yaml` and update the image:
```yaml
image: your-registry/mcp-gateway-backend:latest
```

### 3. Deploy to Kubernetes

```bash
# Apply in order
kubectl apply -f k8s/00-namespace-config.yaml
kubectl apply -f k8s/10-mysql.yaml
kubectl apply -f k8s/20-backend.yaml

# Or apply all at once
kubectl apply -f k8s/
```

### 4. Verify Deployment

```bash
# Check namespace
kubectl get all -n mcp-gateway

# Check pods are running
kubectl get pods -n mcp-gateway

# Check logs
kubectl logs -n mcp-gateway -l app=mcp-gateway-backend --tail=100

# Check HPA status
kubectl get hpa -n mcp-gateway
```

### 5. Access the Application

**Option A: Port Forward (Development)**
```bash
kubectl port-forward -n mcp-gateway svc/mcp-gateway-backend 8080:8080
# Access at http://localhost:8080
```

**Option B: NodePort (Testing)**
```bash
# Access at http://<node-ip>:30080
kubectl get nodes -o wide  # Get node IP
```

**Option C: Ingress (Production)**
```bash
# Add to /etc/hosts (or configure DNS)
echo "$(minikube ip) mcp-gateway.local" | sudo tee -a /etc/hosts

# Access at http://mcp-gateway.local
```

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    mcp-gateway namespace                     │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   ┌─────────────┐     ┌──────────────────────────────┐      │
│   │   Ingress   │────▶│  mcp-gateway-backend (2-6)   │      │
│   │  :80/:443   │     │        Deployment            │      │
│   └─────────────┘     │    ┌────┐ ┌────┐ ┌────┐     │      │
│                       │    │Pod1│ │Pod2│ │... │     │      │
│                       │    └────┘ └────┘ └────┘     │      │
│                       └───────────────┬──────────────┘      │
│                                       │                      │
│                                       ▼                      │
│                       ┌──────────────────────────────┐      │
│                       │      mysql (StatefulSet)     │      │
│                       │    ┌──────────────────┐      │      │
│                       │    │   MySQL 8.0      │      │      │
│                       │    │   PVC: 5Gi       │      │      │
│                       │    └──────────────────┘      │      │
│                       └──────────────────────────────┘      │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Configuration

### Environment Variables

Stored in `00-namespace-config.yaml`:

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active profile | `prod` |
| `RATE_LIMIT_CALLS` | Max calls per window | `100` |
| `RATE_LIMIT_WINDOW_SECONDS` | Rate limit window | `60` |
| `FEATURES_REDIS_ENABLED` | Enable Redis | `false` |
| `FEATURES_KAFKA_ENABLED` | Enable Kafka | `false` |

### Secrets

**⚠️ Important:** Update secrets before production deployment!

```bash
# Generate base64 encoded values
echo -n 'your-password' | base64

# Update secrets in 00-namespace-config.yaml
```

For production, consider using:
- HashiCorp Vault
- AWS Secrets Manager
- Azure Key Vault
- Sealed Secrets

## Scaling

### Horizontal Pod Autoscaler (HPA)

The HPA automatically scales pods based on:
- CPU utilization > 70%
- Memory utilization > 80%

```bash
# Check HPA status
kubectl get hpa -n mcp-gateway

# Manual scaling
kubectl scale deployment mcp-gateway-backend -n mcp-gateway --replicas=4
```

### Resource Limits

| Component | CPU Request | CPU Limit | Memory Request | Memory Limit |
|-----------|-------------|-----------|----------------|--------------|
| Backend | 250m | 1000m | 512Mi | 1Gi |
| MySQL | 250m | 500m | 512Mi | 1Gi |

## Monitoring

### Health Checks

```bash
# Liveness probe
curl http://<pod-ip>:8080/actuator/health/liveness

# Readiness probe
curl http://<pod-ip>:8080/actuator/health/readiness

# Prometheus metrics
curl http://<pod-ip>:8080/actuator/prometheus
```

### Prometheus Integration

Pods are annotated for Prometheus scraping:
```yaml
annotations:
  prometheus.io/scrape: "true"
  prometheus.io/path: "/actuator/prometheus"
  prometheus.io/port: "8080"
```

## Troubleshooting

### Pod Not Starting

```bash
# Check pod events
kubectl describe pod -n mcp-gateway <pod-name>

# Check logs
kubectl logs -n mcp-gateway <pod-name> --previous
```

### Database Connection Issues

```bash
# Check MySQL is running
kubectl get pods -n mcp-gateway -l app=mysql

# Test connection from backend pod
kubectl exec -it -n mcp-gateway <backend-pod> -- nc -zv mysql 3306
```

### Common Issues

| Issue | Solution |
|-------|----------|
| ImagePullBackOff | Check image name and registry credentials |
| CrashLoopBackOff | Check logs for application errors |
| Pending pods | Check if PVC can be bound, check node resources |
| Connection refused | Wait for readiness probe, check service selectors |

## Cleanup

```bash
# Delete all resources
kubectl delete -f k8s/

# Or delete namespace (removes everything)
kubectl delete namespace mcp-gateway
```

## Production Considerations

1. **Secrets Management**: Use external secret managers
2. **TLS**: Configure TLS termination at Ingress
3. **Backup**: Set up MySQL backup to object storage
4. **Logging**: Configure centralized logging (ELK/Loki)
5. **Monitoring**: Deploy Prometheus + Grafana
6. **Network Policies**: Already included, review and adjust

