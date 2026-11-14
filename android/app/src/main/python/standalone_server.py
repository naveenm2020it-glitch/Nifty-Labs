from __future__ import annotations

import os
from pathlib import Path


def start(project_root: str, database_path: str, host: str = "127.0.0.1", port: int = 8765):
    """Run the original NIFTY LAB FastAPI engine on Android loopback."""
    root = Path(project_root).resolve()
    (root / "static").mkdir(parents=True, exist_ok=True)
    (root / "frontend" / "dist").mkdir(parents=True, exist_ok=True)
    os.environ["NIFTYLAB_PROJECT_ROOT"] = str(root)
    os.environ["CACHE_DB"] = database_path

    import uvicorn
    from app.main import app

    config = uvicorn.Config(
        app,
        host=host,
        port=port,
        log_level="warning",
        access_log=False,
        loop="asyncio",
    )
    server = uvicorn.Server(config)
    server.run()
