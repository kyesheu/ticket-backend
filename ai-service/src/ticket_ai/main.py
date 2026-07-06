"""FastAPI 应用入口。"""

from fastapi import FastAPI

from ticket_ai.api import router

app = FastAPI(title="Ticket AI Service", version="0.1.0", docs_url=None, redoc_url=None)
app.include_router(router)
