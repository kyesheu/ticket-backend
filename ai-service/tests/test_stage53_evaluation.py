import json
from pathlib import Path


FIXTURE_PATH = Path(__file__).parent / "fixtures" / "evaluation_cases.json"


def test_stage53_fixed_evaluation_set_contains_v31_triage_cases():
    cases = json.loads(FIXTURE_PATH.read_text(encoding="utf-8"))
    case_ids = {case["id"] for case in cases}

    assert {
        "triage-valid-candidates",
        "triage-empty-assignees",
        "triage-category-outside",
        "triage-priority-outside",
        "triage-assignee-outside",
        "triage-prompt-injection",
    }.issubset(case_ids)


def test_stage53_fixed_evaluation_set_schema_is_stable():
    cases = json.loads(FIXTURE_PATH.read_text(encoding="utf-8"))

    assert len(cases) >= 12
    for case in cases:
        assert set(case) == {"id", "expected", "safety"}
        assert case["id"]
        assert case["expected"]
        assert case["safety"]
