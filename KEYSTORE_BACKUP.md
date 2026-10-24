# Keystore Backup & Signing Recovery

This document explains how the APK signing key is stored securely so updates can install over previous versions without requiring uninstall.

## Why this matters

Android requires that every APK update be signed with the **same key** as the installed version. If the key is lost:
- New APKs cannot be installed over the old version (users must uninstall first, losing all save data)
- Google Play (if ever published) cannot update the listing — would require a new package ID

## Current setup (Phase 7+)

The signing keystore is stored in **three independent locations** for redundancy:

### 1. GitHub Actions Secrets (primary, secure)
Stored as encrypted repository secrets — only CI can read them:

| Secret name | Value |
|---|---|
| `SIGNING_KEYSTORE_BASE64` | base64-encoded `.keystore` file (~3.7 KB) |
| `SIGNING_KEYSTORE_PASSWORD` | `android` |
| `SIGNING_KEY_ALIAS` | `tetris-prime` |
| `SIGNING_KEY_PASSWORD` | `android` |

The CI workflow (`.github/workflows/build.yml`) decodes `SIGNING_KEYSTORE_BASE64` and writes the `.keystore` file at build time. Credentials are passed via `System.getenv(...)` in `app/build.gradle`.

### 2. Committed copy in repo (fallback)
`debug.keystore` is committed at the repository root. This is the original copy used since Phase 0. It serves as a fallback if the secret is unavailable, and as a transparent artifact for anyone cloning the repo to build locally.

### 3. Local backup (recommended)
You should also keep a copy in:
- A password manager (1Password / Bitwarden / KeePassXC — store the binary as attachment)
- An encrypted USB stick in a safe location
- A cloud-encrypted volume (Veracrypt container in Google Drive / Dropbox)

## How to recover

If `debug.keystore` ever gets lost from the repo:

```bash
# 1. Pull the secret from GitHub (requires `gh` CLI with repo admin scope)
gh secret get SIGNING_KEYSTORE_BASE64 --repo saidjte/tetris-prime-kotlin | base64 -d > debug.keystore

# 2. Verify the SHA-256 matches the original
sha256sum debug.keystore
# expected: cf0d9d6cf2a2a3e6a1d4d2c2e2a3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1
```

## How to re-seal the keystore (if you ever rotate)

If you ever generate a new keystore (e.g., to migrate to Google Play App Signing), update the secrets:

```bash
# Generate new keystore (example)
keytool -genkeypair -v -keystore new.keystore -alias tetris-prime \
  -keyalg RSA -keysize 2048 -validity 36500 \
  -storepass android -keypass android \
  -dname "CN=Tetris Prime, O=tetrisprime, C=MA"

# Encode and push to GitHub
base64 -w 0 new.keystore > keystore.b64
gh secret set SIGNING_KEYSTORE_BASE64 < keystore.b64 --repo saidjte/tetris-prime-kotlin
```

⚠️ **Warning**: rotating the keystore means users must uninstall the previous version before installing a new-key APK. Avoid rotation unless absolutely necessary.

## Verification

To verify that two APKs are signed with the same key:

```bash
# Verify the v1 (JAR) signature key
keytool -printcert -jarfile old.apk
keytool -printcert -jarfile new.apk
# Compare the certificate fingerprints — they MUST match.
```

Or use apksigner (Android SDK build-tools):

```bash
apksigner verify --print-certs old.apk
apksigner verify --print-certs new.apk
```

## CI workflow integration

The CI workflow at `.github/workflows/build.yml`:

1. **`Restore keystore` step** — decodes `SIGNING_KEYSTORE_BASE64` to `debug.keystore` if the secret exists. Falls back to the committed copy if not.
2. **`Build Release APK` step** — passes `SIGNING_KEYSTORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD` as env vars. `app/build.gradle` reads these via `System.getenv(...)`, with literal `'android'` / `'tetris-prime'` fallbacks for local builds.

## Notes

- The keystore used here is the standard Android `debug.keystore`. This was a deliberate choice early in the migration to allow rapid iteration. For a production release on Google Play, you would generate a dedicated release keystore and store its credentials the same way.
- The keystore password (`android`) is the default debug keystore password. Even though it is committed to the repo, the keystore file itself is what matters for update compatibility — losing the file (not the password) is what causes problems.
