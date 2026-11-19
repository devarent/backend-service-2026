# Kubernetes deployment

The manifests use Kustomize and assume these cluster add-ons:

- an ingress controller (`ingressClassName: nginx`)
- cert-manager with a `letsencrypt-prod` ClusterIssuer
- Prometheus Operator for the `ServiceMonitor`
- a CNI that enforces `NetworkPolicy`

## Render before applying

```bash
kubectl kustomize kubernetes/overlays/dev
kubectl kustomize kubernetes/overlays/prod
```

## First deploy

```bash
kubectl apply -k kubernetes/base        # namespace, service account, config
kubectl -n order-service create secret generic order-service-secrets \
  --from-literal=DB_PASSWORD='<strong-password>' \
  --from-literal=REDIS_PASSWORD='<redis-password>'
kubectl apply -k kubernetes/overlays/prod
kubectl -n order-service rollout status deployment/order-service --timeout=180s
```

`secret.example.yaml` is a template and is not included in the Kustomize
resource list. Never apply it with real values committed to Git.

## Before production

1. Replace `orders.example.com` in `base/ingress.yaml` and the TLS secret.
2. Replace the PostgreSQL and Redis hostnames in `base/configmap.yaml`; the
   example assumes in-cluster services. For managed services, add a CIDR egress
   rule to `base/networkpolicy.yaml` and enable TLS (`sslmode=require`).
3. Set the image in `overlays/prod/kustomization.yaml` to the release tag built
   by `.github/workflows/release.yml`.
4. Review resource requests against the JVM heap (`MaxRAMPercentage=75`).
5. Confirm the Prometheus Operator's `ServiceMonitor` CRD and the `release`
   label selector used by your Prometheus instance.
