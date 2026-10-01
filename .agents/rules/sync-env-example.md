# Synchronize Environment Templates (.env -> .env.example)

Whenever any `.env` file (e.g., `.env`, `.env.local`, `.env.*`) is created, modified, or updated with new variables or keys:

1. **Mandatory update**: Update the corresponding `.env.example` file alongside the `.env` change.
2. **Workable placeholders**: Never commit real secrets, private keys, or actual credentials to `.env.example`.
3. **Informative hints**: Every entry in `.env.example` must provide a clear, workable placeholder or hint that explains what kind of value to use (e.g. `your_postgres_password_here`, `http://localhost:8080`, `dev`).
4. **Key parity**: Maintain identical key names, order, and comment categories between `.env` and `.env.example`.
