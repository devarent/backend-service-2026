# Security policy

## Supported versions

| Version | Supported |
| --- | --- |
| `main` | Yes |
| Tagged releases | Yes, latest minor |
| Older snapshots | No |

## Reporting a vulnerability

Please do **not** open a public GitHub issue for a security problem. Report it
privately to the maintainer through GitHub's "Report a vulnerability" flow
(Security tab -> Advisories) or by email to the address on the maintainer's
GitHub profile.

Include:

- a description of the issue and its impact;
- the affected version or commit;
- reproduction steps or a proof of concept;
- any suggested mitigation.

You can expect an acknowledgement within 3 business days and a status update
within 10 business days. Please allow time for a fix before public disclosure.

## Security model of this demo

This repository is a portfolio service, not a hardened multi-tenant product.
Known limitations are documented deliberately:

- **No authentication or authorization.** All endpoints are public in the demo.
  Production must add an OIDC resource server and per-customer authorization.
- **No transport security in local development.** The container listens on HTTP;
  TLS terminates at the ingress or load balancer.
- **Single configured currency.** Currency is validated against configuration,
  not against a market-data feed.
- **Secrets are environment variables.** Local defaults are placeholders. Never
  commit real credentials; use a secret manager in production.
- **No rate limiting.** Add it at the ingress or API gateway for public traffic.

## Hardening already present

- Non-root container user and `readOnlyRootFilesystem` in Kubernetes.
- Dropped Linux capabilities, `allowPrivilegeEscalation: false`, and the
  `RuntimeDefault` seccomp profile.
- Kubernetes `NetworkPolicy` restricting ingress and egress.
- Bean Validation on all request bodies and query parameters.
- Parameterised JPQL; no string-concatenated SQL.
- Correlation ids on errors, with no stack traces or SQL details returned to
  clients.
- Dependencies are scanned by Trivy in CI; the scan is non-blocking in this
  portfolio so a new advisory does not break unrelated pull requests.

## Dependency updates

Dependabot is not enabled by default because the repository is not hosted yet.
When it is, enable weekly updates for Maven, GitHub Actions, and Docker. Review
Quarkus platform upgrades together because the BOM coordinates extensions.
