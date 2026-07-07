# Copy to smoke-env.local.ps1 and fill local-only values.
# smoke-env.local.ps1 is ignored by git. Do not commit real tokens.

$env:TICKET_AI_SMOKE_ENABLED = "true"
$env:TICKET_AI_ENABLED = "true"
$env:TICKET_AI_BASE_URL = "http://127.0.0.1:8090"
$env:TICKET_AI_SERVICE_TOKEN = "replace-with-local-token-at-least-16-chars"

$env:TICKET_AI_SMOKE_MODE = "true"
$env:TICKET_AI_KNOWLEDGE_INDEX = "ticket-knowledge-smoke"
$env:TICKET_AI_TICKET_HISTORY_INDEX = "ticket-history-smoke"
$env:TICKET_AI_ELASTICSEARCH_URL = "http://127.0.0.1:9200"
