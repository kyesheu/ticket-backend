# Ticket AI Service

v3.0 的 Python FastAPI + LangChain 服务。当前阶段只提供 v1 HTTP 契约、服务认证、健康检查和后续功能路由骨架。

```powershell
python -m venv .venv
.venv\Scripts\python -m pip install -e ".[test]"
.venv\Scripts\python -m pytest
.venv\Scripts\python -m uvicorn ticket_ai.main:app --host 127.0.0.1 --port 8090
```

运行前通过环境变量设置 `TICKET_AI_SERVICE_TOKEN`。不要把真实凭据写入仓库。
