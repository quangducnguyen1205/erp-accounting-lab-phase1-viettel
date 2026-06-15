# Keycloak Custom Login Theme Walkthrough

## 1. General Theme Anatomy

A Keycloak login theme is server-side UI customization for Keycloak pages. It is not a React route.

Common files:

| File | Meaning |
|---|---|
| `theme.properties` | Declares parent theme, imported resources and CSS files. |
| `login.ftl` | FreeMarker template for the login form page. |
| `resources/css/*.css` | Static CSS used by the theme. |
| Docker mount | Makes the theme directory visible inside the Keycloak container. |
| Realm setting | Selects which login theme the realm uses. |

Keycloak still owns authentication. A theme only changes presentation.

## 2. Repo-Specific Mapping

| Concept | Repo file |
|---|---|
| Theme root | `lab-code/keycloak-lab/themes/master-data-portal/` |
| Theme descriptor | `login/theme.properties` |
| Login template | `login/login.ftl` |
| CSS | `login/resources/css/styles.css` |
| Docker mount | `lab-code/keycloak-lab/docker-compose.yml` |
| Realm theme bootstrap | `lab-code/keycloak-lab/setup-keycloak-demo.sh` |

Theme name:

```text
master-data-portal
```

The theme matches the React product direction:

```text
Master Data Portal
Cổng quản lý danh mục dùng chung theo tenant
```

## 3. Runtime Flow

```text
React Web UI
  -> redirects browser to Keycloak login
  -> Keycloak realm viettel-lab renders master-data-portal login theme
  -> user submits username/password
  -> Keycloak issues OIDC tokens
  -> browser redirects back to React Web UI
  -> backend services validate JWT as Resource Servers
```

The theme does not change tokens, claims, clients, PKCE, redirect URI, roles, or backend authorization.

## 4. What The Theme Shows

The local login page shows:

- `Master Data Portal` brand;
- Vietnamese-friendly product copy;
- username/password form;
- local demo accounts:
  - `tenant1-user / password` — Accountant;
  - `tenant2-user / password` — Viewer;
- local-only credential warning;
- error message area.

No token or secret is displayed.

## 5. Common Mistakes

- Implementing login as a React route instead of a Keycloak theme.
- Forgetting to mount the theme directory into the Keycloak container.
- Updating the theme files but not rerunning/restarting Keycloak when needed.
- Changing auth logic while trying to change only UI.
- Forgetting `make keycloak-setup`, so the realm still uses the default theme.
- Treating local demo credentials as production credentials.

## 6. Verification

Validate Compose:

```bash
docker compose -f lab-code/keycloak-lab/docker-compose.yml config
```

Start and bootstrap:

```bash
cd lab-code
make keycloak-up
make keycloak-setup
```

Expected setup output includes:

```text
Login theme: master-data-portal
```

Then open the Web UI and click login:

```text
http://localhost:5173
```

Expected:

- Keycloak login page uses `Master Data Portal` visual styling;
- `tenant1-user/password` can log in;
- `tenant2-user/password` can log in;
- backend token claims and roles are unchanged.

## 7. Giới hạn production

- Real production Keycloak theme should go through accessibility, browser and security review.
- Demo account hints must not be shown in production.
- Production should harden TLS, redirect URI, token lifetime, password policies and client settings.
- Theme customization does not replace backend authorization or tenant-aware queries.
