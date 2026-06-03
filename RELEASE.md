# Release signing & GitHub secrets

### Create keystore

```bash
# Create the release keystore (prompts for a password)
keytool -genkeypair -v \
  -keystore release-key.jks \
  -alias key \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=CHANGEME, O=CHANGEME, C=CHANGEME"
```

### Upload to GitHub Secrets

```bash
# Encode the keystore and upload it as the KEYSTORE_BASE64 secret
base64 -w0 release-key.jks | gh secret set KEYSTORE_BASE64
```

```bash
# Key alias (must match the -alias used above)
gh secret set KEY_ALIAS --body "key"
```

```bash
# Passwords - gh prompts for the value.
gh secret set KEYSTORE_PASSWORD
```

```bash
# Enter the SAME password for both.
gh secret set KEY_PASSWORD
```

### Build a signed release locally

```bash
export KEYSTORE_FILE=/path/to/release-key.jks
export KEYSTORE_PASSWORD=...
export KEY_ALIAS=...
export KEY_PASSWORD=...

./gradlew :app:assembleRelease
```

### Add release tag and push

```bash
# Tag + push -> triggers the Release workflow (builds & publishes signed APKs)
git tag v0.1.0
git push origin v0.1.0
```
