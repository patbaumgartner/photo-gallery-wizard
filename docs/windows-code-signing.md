# Windows Code Signing

The release workflow signs the Windows native executable using a code signing certificate stored in GitHub Actions secrets.

## How It Works

The `Sign Windows executable` step in `.github/workflows/release.yml`:

1. Decodes the base64-encoded PFX certificate from the `WINDOWS_SIGN_CERT` secret.
2. Writes it to a temporary file on the runner.
3. Signs the `.exe` with `signtool` using SHA-256 and a DigiCert timestamp server.
4. Removes the temporary certificate file.

The step is conditional — it only runs when `SIGN_CERT` is set, so builds in forks without secrets still succeed.

## Generating a Self-Signed Certificate

```bash
# 1. Generate a random password
PASSWORD=$(openssl rand -base64 15)
echo "Password: $PASSWORD"

# 2. Create an OpenSSL config with the codeSigning extended key usage
cat > codesign.cnf <<EOF
[req]
distinguished_name = req_dn
x509_extensions    = v3_cs
prompt             = no

[req_dn]
CN = Photo Gallery Wizard
O  = pabaumgartner

[v3_cs]
keyUsage         = digitalSignature
extendedKeyUsage = codeSigning
EOF

# 3. Generate a 2048-bit RSA key and self-signed certificate (valid 730 days)
openssl req -x509 -newkey rsa:2048 -keyout cs-key.pem -out cs-cert.pem \
  -days 730 -nodes -config codesign.cnf

# 4. Export to PFX (PKCS#12)
openssl pkcs12 -export -in cs-cert.pem -inkey cs-key.pem \
  -out sign-cert.pfx -passout "pass:$PASSWORD"

# 5. Verify the PFX
openssl pkcs12 -in sign-cert.pfx -info -nokeys -password "pass:$PASSWORD"
```

## Setting GitHub Secrets

```bash
# Base64-encode the PFX and set as secret
base64 -w 0 sign-cert.pfx | gh secret set WINDOWS_SIGN_CERT

# Set the password as secret
echo "$PASSWORD" | gh secret set WINDOWS_SIGN_CERT_PASSWORD
```

| Secret | Content |
|--------|---------|
| `WINDOWS_SIGN_CERT` | Base64-encoded `.pfx` file |
| `WINDOWS_SIGN_CERT_PASSWORD` | PFX export password |

## Renewing the Certificate

The self-signed certificate expires after 730 days. To renew:

1. Re-run the generation steps above.
2. Update both GitHub secrets with the new values.
3. Clean up old key/cert files.

## Using a Purchased Certificate

For trusted signatures (no SmartScreen warnings), replace the self-signed certificate with one from a certificate authority (e.g., DigiCert, Sectigo, SSL.com). The workflow does not change — only the PFX and password in the secrets need to be updated.
