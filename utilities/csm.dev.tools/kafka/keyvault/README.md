# Copy secrets between key vaults

Script to copy secrets from kafka key vault to environment key vault
e.g. from `ptcsmsandbox1kafkaakskv` to `ptcsmsandbox1envakskv`

**_NOTE:_**  Secrets in Kafka KV are colorized (`secret-name-blue`), secrets in Env KV are not (`secret-name`)

## 📦 Prerequisites

```bash
pip install -r ../utils/requirements.txt
```

## 🚀 How to use

```bash
./copy-secrets.py
```
