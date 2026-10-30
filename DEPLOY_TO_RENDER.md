# Deploy NIFTY LAB to Render

The project uses a multi-stage Docker build. The first stage installs Node.js frontend dependencies and builds the React terminal UI; the second stage installs Python/FastAPI dependencies and serves the built UI plus the existing feature/API layer.

## Steps

1. Extract this ZIP and upload the **contents of the project folder** to a GitHub repository. The repository root should contain `Dockerfile`, `render.yaml`, `requirements.txt`, `frontend/`, `app/`, `static/`, and `tests/`.
2. In Render, create a new Blueprint from that repository.
3. Let Render build and deploy the Docker image. The frontend build needs access to the npm registry during the image build.
4. When Render reports the service is live, open the HTTPS URL. The `/api/health` endpoint is the configured health check.

## Notes

- This ZIP is source code and does not include a pre-deployed public URL.
- A free instance may sleep after inactivity and its local cache may be ephemeral.
- NSE public endpoints can rate-limit requests or change format. The application reports failures instead of inventing market data.
- Before sharing publicly, add authentication and request/upload limits as appropriate.
