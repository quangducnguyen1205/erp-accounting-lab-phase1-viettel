# Final Web UI Figma Screen Handoff

## Status

Figma file:

[AI Knowledge Workspace - Phase 1.5 Ops Console](https://www.figma.com/design/kyPvlljW1KhQhoUvqLcjco)

Current status on 2026-06-13:

- `00 Design System` exists and was generated in Figma in the previous design pass.
- `01 Web App Screens + Demo Flow` exists as a page, but the full screen set could not be generated in this pass.
- `02 Keycloak Theme + Handoff Notes` exists as a page, but the Keycloak frames could not be generated in this pass.
- The Figma Starter MCP tool-call limit is currently blocking metadata inspection, screen creation and screenshot export.
- React implementation now follows this handoff directly in `lab-code/web-ui-demo/src/` as a multi-screen console. Full Figma frames/screenshots are still useful for visual review, but no longer block the first React implementation.

This document records the exact intended Figma frames and screenshot handoff so the next Figma pass can complete the screen set without rethinking the design.

## Pages And Frames

### `00 Design System`

Existing page from the previous pass.

Contains:

- color tokens;
- typography and spacing examples;
- controls and state examples;
- application component patterns.

### `01 Web App Screens + Demo Flow`

Frames to create:

1. `01 Welcome - Logged Out`
2. `02 Dashboard - Architecture Overview`
3. `03 Master Data - ACCOUNTANT Success`
4. `04 Master Data - Error States`
5. `05 Audit Events - Tenant 1`
6. `06 Audit Events - Tenant 2 Empty`
7. `07 Observability`
8. `08 Demo Flow Map`

### `02 Keycloak Theme + Handoff Notes`

Frames to create:

1. `01 Keycloak Login - Default`
2. `02 Keycloak Login - Error`
3. `03 Keycloak Theme Handoff`

## Screenshot Gallery

Screenshots were not exported in this pass because the Figma MCP tool-call limit blocked `get_metadata`, `use_figma` and screenshot operations.

Expected screenshot directory:

```text
docs/06-frontend/images/final-ui-design/
```

Expected screenshot filenames:

```text
01-welcome-logged-out.png
02-dashboard-architecture-overview.png
03-master-data-accountant-success.png
04-master-data-error-states.png
05-audit-events-tenant1.png
06-audit-events-tenant2-empty.png
07-observability.png
08-demo-flow-map.png
09-keycloak-login-default.png
10-keycloak-login-error.png
11-keycloak-theme-handoff.png
```

The folder is kept in Git with `.gitkeep`; generated PNGs should be added only after they are exported from the actual Figma frames and checked for secrets/tokens.

## Design Decisions

### Sidebar And Topbar Layout

The final UI should move away from a single technical test page and become a small SaaS-style console:

- left sidebar for route-level navigation;
- topbar for user, tenant, role and API base URL;
- dense content panels for backend demo workflows;
- no oversized marketing hero after login.

Recommended nav items:

- Dashboard.
- Master Data.
- Audit Events.
- Observability.

### Role And Tenant Visibility

The authenticated state should be visible without exposing tokens:

- username;
- `tenant_id`;
- role badge, for example `ACCOUNTANT` or `VIEWER`;
- token availability shown only as `available (hidden)`.

The UI can display these claims for learning, but backend services remain responsible for JWT validation, role checks and tenant isolation.

### RequestId And Status Visibility

Every API action should show:

- HTTP status;
- endpoint;
- requestId;
- short explanation for `401`, `403`, `409`, `500` and service-unavailable states.

This supports the core demo behavior: take a requestId from the UI and search it in Loki/Grafana.

### Observability Is Links And Recipes

The Observability screen should not call Grafana, Loki or Kafka UI APIs directly.

It should provide:

- local tool links;
- practical LogQL recipes;
- explanation that `service` and `source` are labels;
- explanation that requestId/code/status are searched as text.

### Keycloak Theme Is Separate From React UI

The Keycloak login theme is a later implementation task. It should visually match the React console, but it must not change authentication logic.

Future theme path:

```text
lab-code/keycloak-lab/themes/<theme-name>/login/theme.properties
lab-code/keycloak-lab/themes/<theme-name>/login/login.ftl
lab-code/keycloak-lab/themes/<theme-name>/login/resources/css/styles.css
```

## Implementation Mapping

Potential React structure for the later implementation:

```text
lab-code/web-ui-demo/src/
  App.jsx
  api.js
  config.js
  keycloak.js
  styles.css
  components/
    AppShell.jsx
    Sidebar.jsx
    Topbar.jsx
    StatusCard.jsx
    Badge.jsx
    RequestStatus.jsx
    DataTable.jsx
    EmptyState.jsx
    Alert.jsx
  screens/
    WelcomeScreen.jsx
    DashboardScreen.jsx
    MasterDataScreen.jsx
    AuditEventsScreen.jsx
    ObservabilityScreen.jsx
```

The current app may stay single-route if that keeps the demo simpler, but the component boundaries above make the redesign easier to read.

Current API helpers already map cleanly:

| UI screen | Existing helper |
|---|---|
| Master Data list | `loadMasterData(apiBaseUrl)` |
| Load by code | `loadMasterDataByCode(code, apiBaseUrl)` |
| Create master data | `createMasterData(payload, apiBaseUrl)` |
| Audit Events | `loadAuditEvents(apiBaseUrl, limit)` |

No backend API contract change is required.

## Approved For Coding Next

The first React implementation has been completed from this handoff. The next coding/refinement pass should review the running UI and adjust visuals/states against the approved design direction:

1. Review app shell: sidebar, topbar, route state and layout tokens.
2. Review Dashboard screen: architecture cards and demo checklist.
3. Review Master Data screen: list/create/load-by-code and API state handling.
4. Review Audit Events screen: table, flow panel and tenant2 empty success state.
5. Review Observability screen: local tool links and LogQL recipes.
6. Run final QA: ACCOUNTANT flow, VIEWER flow, duplicate `409`, requestId log search.

## Future Work

- Keycloak custom login theme implementation.
- Optional tool health probes.
- Optional metrics dashboard link polish.
- Optional screenshots regeneration after visual review.

## Remaining Manual/Figma Work

The next Figma pass should:

1. Open `01 Web App Screens + Demo Flow`.
2. Create the eight web app frames listed above.
3. Open `02 Keycloak Theme + Handoff Notes`.
4. Create the three Keycloak/handoff frames listed above.
5. Export PNG screenshots into `docs/06-frontend/images/final-ui-design/`.
6. Update this handoff doc from pending screenshots to an embedded screenshot gallery.
