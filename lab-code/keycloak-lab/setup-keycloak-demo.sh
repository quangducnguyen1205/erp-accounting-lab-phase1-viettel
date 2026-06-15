#!/usr/bin/env bash
set -euo pipefail

CONTAINER_NAME="${KEYCLOAK_CONTAINER_NAME:-viettel-keycloak}"
SERVER_URL="${KEYCLOAK_INTERNAL_URL:-http://localhost:8080}"
ADMIN_USER="${KEYCLOAK_ADMIN_USER:-admin}"
ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"

REALM="${KEYCLOAK_REALM:-viettel-lab}"
API_CLIENT="${KEYCLOAK_API_CLIENT_ID:-tenant-demo-api-client}"
WEB_CLIENT="${KEYCLOAK_WEB_CLIENT_ID:-tenant-demo-web}"
WEB_REDIRECT_URI="${KEYCLOAK_WEB_REDIRECT_URI:-http://localhost:5173/*}"
WEB_ORIGIN="${KEYCLOAK_WEB_ORIGIN:-http://localhost:5173}"
LOGIN_THEME="${KEYCLOAK_LOGIN_THEME:-master-data-portal}"

KC="/opt/keycloak/bin/kcadm.sh"

log() {
  echo "$@" >&2
}

run_kcadm() {
  docker exec "$CONTAINER_NAME" "$KC" "$@"
}

wait_for_keycloak() {
  log "Waiting for Keycloak admin API..."
  for _ in $(seq 1 60); do
    if run_kcadm config credentials \
      --server "$SERVER_URL" \
      --realm master \
      --user "$ADMIN_USER" \
      --password "$ADMIN_PASSWORD" >/dev/null 2>&1; then
      log "Keycloak admin API is ready."
      return 0
    fi
    sleep 2
  done

  echo "Keycloak did not become ready in time." >&2
  return 1
}

csv_first_id() {
  head -n 1 | cut -d, -f1 | tr -d '"\r'
}

get_client_uuid() {
  run_kcadm get clients -r "$REALM" -q clientId="$1" --fields id --format csv | csv_first_id
}

get_user_id() {
  run_kcadm get users -r "$REALM" -q username="$1" --fields id --format csv | csv_first_id
}

ensure_realm() {
  if run_kcadm get "realms/$REALM" >/dev/null 2>&1; then
    log "Realm exists: $REALM"
  else
    log "Creating realm: $REALM"
    run_kcadm create realms -s "realm=$REALM" -s enabled=true
  fi
}

configure_login_theme() {
  log "Configuring login theme and local HTTP realm setting: $LOGIN_THEME"
  run_kcadm update "realms/$REALM" \
    -s "loginTheme=$LOGIN_THEME" \
    -s "sslRequired=none"
}

ensure_realm_role() {
  local role="$1"
  if run_kcadm get "roles/$role" -r "$REALM" >/dev/null 2>&1; then
    log "Realm role exists: $role"
  else
    log "Creating realm role: $role"
    run_kcadm create roles -r "$REALM" -s "name=$role"
  fi
}

configure_user_profile() {
  log "Configuring User Profile attribute: tenant_id"
  docker exec -i "$CONTAINER_NAME" /bin/sh -c "cat > /tmp/viettel-user-profile.json" <<'JSON'
{
  "unmanagedAttributePolicy": "ENABLED",
  "attributes": [
    {
      "name": "username",
      "displayName": "${username}",
      "validations": {
        "length": { "min": 3, "max": 255 },
        "username-prohibited-characters": {},
        "up-username-not-idn-homograph": {}
      },
      "permissions": { "view": ["admin", "user"], "edit": ["admin", "user"] },
      "multivalued": false
    },
    {
      "name": "email",
      "displayName": "${email}",
      "validations": {
        "email": {},
        "length": { "max": 255 }
      },
      "required": { "roles": ["user"] },
      "permissions": { "view": ["admin", "user"], "edit": ["admin", "user"] },
      "multivalued": false
    },
    {
      "name": "firstName",
      "displayName": "${firstName}",
      "validations": {
        "length": { "max": 255 },
        "person-name-prohibited-characters": {}
      },
      "required": { "roles": ["user"] },
      "permissions": { "view": ["admin", "user"], "edit": ["admin", "user"] },
      "multivalued": false
    },
    {
      "name": "lastName",
      "displayName": "${lastName}",
      "validations": {
        "length": { "max": 255 },
        "person-name-prohibited-characters": {}
      },
      "required": { "roles": ["user"] },
      "permissions": { "view": ["admin", "user"], "edit": ["admin", "user"] },
      "multivalued": false
    },
    {
      "name": "tenant_id",
      "displayName": "tenant_id",
      "validations": {
        "integer": {}
      },
      "permissions": { "view": ["admin"], "edit": ["admin"] },
      "multivalued": false
    }
  ],
  "groups": [
    {
      "name": "user-metadata",
      "displayHeader": "User metadata",
      "displayDescription": "Attributes, which refer to user metadata"
    }
  ]
}
JSON
  run_kcadm update users/profile -r "$REALM" -f /tmp/viettel-user-profile.json
}

ensure_client() {
  local client_id="$1"
  local direct_grants="$2"
  local uuid
  uuid="$(get_client_uuid "$client_id")"

  if [ -z "$uuid" ]; then
    log "Creating client: $client_id"
    run_kcadm create clients -r "$REALM" \
      -s "clientId=$client_id" \
      -s protocol=openid-connect \
      -s enabled=true \
      -s publicClient=true \
      -s clientAuthenticatorType=client-secret \
      -s standardFlowEnabled=true \
      -s "directAccessGrantsEnabled=$direct_grants" \
      -s serviceAccountsEnabled=false \
      -s fullScopeAllowed=true
    uuid="$(get_client_uuid "$client_id")"
  else
    log "Updating client: $client_id"
    run_kcadm update "clients/$uuid" -r "$REALM" \
      -s enabled=true \
      -s publicClient=true \
      -s standardFlowEnabled=true \
      -s "directAccessGrantsEnabled=$direct_grants" \
      -s serviceAccountsEnabled=false \
      -s fullScopeAllowed=true
  fi

  if [ "$client_id" = "$WEB_CLIENT" ]; then
    run_kcadm update "clients/$uuid" -r "$REALM" \
      -s "redirectUris=[\"$WEB_REDIRECT_URI\"]" \
      -s "webOrigins=[\"$WEB_ORIGIN\"]" \
      -s 'attributes."pkce.code.challenge.method"=S256'
  fi

  echo "$uuid"
}

ensure_client_role() {
  local client_uuid="$1"
  local role="$2"

  if run_kcadm get "clients/$client_uuid/roles/$role" -r "$REALM" >/dev/null 2>&1; then
    log "Client role exists: $role"
  else
    log "Creating client role: $role"
    run_kcadm create "clients/$client_uuid/roles" -r "$REALM" -s "name=$role"
  fi
}

reset_tenant_mapper() {
  local client_uuid="$1"
  local client_id="$2"
  local mapper_ids

  mapper_ids="$(
    run_kcadm get "clients/$client_uuid/protocol-mappers/models" -r "$REALM" --fields id,name --format csv \
      | grep -E ',tenant_id$|,"tenant_id"$' \
      | cut -d, -f1 \
      | tr -d '"\r' || true
  )"

  for mapper_id in $mapper_ids; do
    log "Removing old tenant_id mapper from client $client_id: $mapper_id"
    run_kcadm delete "clients/$client_uuid/protocol-mappers/models/$mapper_id" -r "$REALM"
  done

  log "Creating tenant_id mapper for client: $client_id"
  run_kcadm create "clients/$client_uuid/protocol-mappers/models" -r "$REALM" \
    -s name=tenant_id \
    -s protocol=openid-connect \
    -s protocolMapper=oidc-usermodel-attribute-mapper \
    -s 'config."user.attribute"=tenant_id' \
    -s 'config."claim.name"=tenant_id' \
    -s 'config."jsonType.label"=long' \
    -s 'config."access.token.claim"=true' \
    -s 'config."id.token.claim"=false' \
    -s 'config."userinfo.token.claim"=true'
}

ensure_user() {
  local username="$1"
  local tenant_id="$2"
  local role="$3"
  local email="${username}@example.local"
  local first_name="${username%%-*}"
  local user_file="/tmp/viettel-${username}.json"
  local user_id

  docker exec -i "$CONTAINER_NAME" /bin/sh -c "cat > '$user_file'" <<JSON
{
  "username": "$username",
  "enabled": true,
  "email": "$email",
  "emailVerified": true,
  "firstName": "$first_name",
  "lastName": "User",
  "attributes": {
    "tenant_id": ["$tenant_id"]
  }
}
JSON

  user_id="$(get_user_id "$username")"
  if [ -z "$user_id" ]; then
    log "Creating user: $username"
    run_kcadm create users -r "$REALM" -f "$user_file"
    user_id="$(get_user_id "$username")"
  else
    log "Updating user: $username"
    run_kcadm update "users/$user_id" -r "$REALM" -f "$user_file"
  fi

  log "Setting local demo password for: $username"
  run_kcadm set-password -r "$REALM" --username "$username" --new-password password

  log "Assigning realm role $role to $username"
  run_kcadm add-roles -r "$REALM" --uusername "$username" --rolename "$role" >/dev/null 2>&1 || true

  log "Assigning API client role $role to $username"
  run_kcadm add-roles -r "$REALM" --uusername "$username" --cclientid "$API_CLIENT" --rolename "$role" >/dev/null 2>&1 || true
}

main() {
  wait_for_keycloak
  ensure_realm
  configure_login_theme
  configure_user_profile

  local api_client_uuid
  local web_client_uuid
  api_client_uuid="$(ensure_client "$API_CLIENT" true)"
  web_client_uuid="$(ensure_client "$WEB_CLIENT" false)"

  for role in ADMIN ACCOUNTANT VIEWER; do
    ensure_realm_role "$role"
    ensure_client_role "$api_client_uuid" "$role"
  done

  reset_tenant_mapper "$api_client_uuid" "$API_CLIENT"
  reset_tenant_mapper "$web_client_uuid" "$WEB_CLIENT"

  ensure_user tenant1-user 1 ACCOUNTANT
  ensure_user tenant2-user 2 VIEWER
  ensure_user platform-admin 1 ADMIN

  echo ""
  echo "Keycloak demo setup completed."
  echo "Realm: $REALM"
  echo "API client: $API_CLIENT"
  echo "Web client: $WEB_CLIENT"
  echo "Login theme: $LOGIN_THEME"
  echo "Users: tenant1-user/password, tenant2-user/password, platform-admin/password"
}

main "$@"
