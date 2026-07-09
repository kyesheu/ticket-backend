# Ticket AI Service

v3.0 的 Python FastAPI + LangChain 服务。当前阶段只提供 v1 HTTP 契约、服务认证、健康检查和后续功能路由骨架。

```powershell
python -m venv .venv
.venv\Scripts\python -m pip install -e ".[test]"
.venv\Scripts\python -m pytest
```

本地联调不要手动逐个设置环境变量。后端根目录的 `.env` 会同时被 Java 后端和 Python AI 服务读取：

```powershell
Copy-Item .env.example .env
```

把 `.env` 里的 `TICKET_AI_*` 按本地环境改好后，启动完整本地栈：

```powershell
..\start.bat
```

脚本会复用 `.env` 里的配置；如果 `.env` 没配置 AI 项，则使用本地 smoke 默认值，并启动：

- Java 后端：`http://localhost:8080`
- Python AI 服务：`http://127.0.0.1:8090`

生产环境不要使用本地脚本，必须由部署平台、容器编排或 Secret Manager 注入 `TICKET_AI_SERVICE_TOKEN`、模型和检索服务配置。不要把真实凭据写入仓库。
