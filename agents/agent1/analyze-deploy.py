#!/usr/bin/env python3
import argparse
import json
import os
import re
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, List, Optional, Tuple
from urllib import error as urlerror
from urllib import request as urlrequest


MAX_EVIDENCE_LINE_LEN = 240
DEFAULT_OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1"
DEFAULT_OPENROUTER_MODEL = "openai/gpt-4o-mini"


@dataclass
class CauseMatch:
    cause: str
    likelihood: float
    evidence: List[str]
    actions: List[Dict[str, str]]


PATTERN_LIBRARY = [
    {
        "cause": "DB unreachable / wrong DB host",
        "patterns": [
            r"connection .*5432.*refused",
            r"postgres.*connection.*refused",
            r"connection refused",
            r"could not connect to server",
        ],
        "base_likelihood": 0.88,
        "actions": [
            {
                "action": "Verify DB service is reachable from staging backend container",
                "reason": "Application logs suggest database connections are refused.",
                "priority": "P0",
            },
            {
                "action": "Check staging datasource host (avoid localhost inside containers)",
                "reason": "Containerized apps often fail when DB host is set to localhost.",
                "priority": "P0",
            },
        ],
    },
    {
        "cause": "DNS / networking issue",
        "patterns": [
            r"unknownhostexception",
            r"name or service not known",
            r"temporary failure in name resolution",
            r"getaddrinfo.*(enotfound|eai_again)",
        ],
        "base_likelihood": 0.82,
        "actions": [
            {
                "action": "Validate service DNS names and network aliases in staging",
                "reason": "Host resolution failures indicate name/network wiring issues.",
                "priority": "P0",
            }
        ],
    },
    {
        "cause": "Service dependency timeout",
        "patterns": [
            r"timed out",
            r"timeout",
            r"connecttimeoutexception",
            r"readtimeoutexception",
        ],
        "base_likelihood": 0.74,
        "actions": [
            {
                "action": "Check dependency latency/availability and timeout settings",
                "reason": "Timeout signatures indicate an upstream dependency is too slow or down.",
                "priority": "P1",
            }
        ],
    },
    {
        "cause": "Port binding conflict",
        "patterns": [
            r"bindexception",
            r"address already in use",
            r"port is already allocated",
        ],
        "base_likelihood": 0.9,
        "actions": [
            {
                "action": "Confirm exposed ports are not already occupied on staging host",
                "reason": "Binding conflicts prevent service startup.",
                "priority": "P0",
            }
        ],
    },
    {
        "cause": "Memory pressure / OOM",
        "patterns": [
            r"outofmemoryerror",
            r"oomkilled",
            r"killed process",
            r"cannot allocate memory",
        ],
        "base_likelihood": 0.86,
        "actions": [
            {
                "action": "Check memory limits and recent resource spikes in staging",
                "reason": "OOM patterns indicate memory saturation or limits too low.",
                "priority": "P0",
            }
        ],
    },
    {
        "cause": "Crash loop",
        "patterns": [
            r"restarting",
            r"crashloop",
            r"exception",
        ],
        "base_likelihood": 0.7,
        "actions": [
            {
                "action": "Inspect startup sequence and first fatal exception after boot",
                "reason": "Repeated restarts usually come from an unhandled startup/runtime error.",
                "priority": "P0",
            }
        ],
    },
    {
        "cause": "Missing configuration",
        "patterns": [
            r"could not resolve placeholder",
            r"missing required.*(env|environment)",
            r"environment variable .* not set",
            r"no value present",
        ],
        "base_likelihood": 0.88,
        "actions": [
            {
                "action": "Verify required staging environment variables are defined",
                "reason": "Startup errors indicate unresolved configuration placeholders.",
                "priority": "P0",
            }
        ],
    },
    {
        "cause": "Auth/config issue on staging endpoint",
        "patterns": [
            r"\b401\b",
            r"\b403\b",
            r"unauthorized",
            r"forbidden",
        ],
        "base_likelihood": 0.65,
        "actions": [
            {
                "action": "Review staging auth settings for smoke-test endpoints",
                "reason": "Protected endpoints are failing with authorization errors.",
                "priority": "P1",
            }
        ],
    },
    {
        "cause": "Wrong image tag or registry auth",
        "patterns": [
            r"pull access denied",
            r"manifest unknown",
            r"requested access to the resource is denied",
        ],
        "base_likelihood": 0.92,
        "actions": [
            {
                "action": "Validate image tag existence and registry credentials in CI",
                "reason": "Image pull failures indicate tag mismatch or auth issues.",
                "priority": "P0",
            }
        ],
    },
    {
        "cause": "Deployment configuration error",
        "patterns": [
            r"app_image",
            r"variable is not set",
            r"compose.*error",
            r"invalid compose",
        ],
        "base_likelihood": 0.84,
        "actions": [
            {
                "action": "Review compose/env interpolation values used for staging deploy",
                "reason": "Compose parsing/runtime indicates unresolved deployment variables.",
                "priority": "P0",
            }
        ],
    },
]


def utc_now_iso() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat()


def safe_line(line: str) -> str:
    compact = " ".join(line.strip().split())
    if len(compact) <= MAX_EVIDENCE_LINE_LEN:
        return compact
    return compact[: MAX_EVIDENCE_LINE_LEN - 3] + "..."


def read_file_if_exists(path: Path) -> Optional[str]:
    if not path.exists() or not path.is_file():
        return None
    try:
        return path.read_text(encoding="utf-8", errors="replace")
    except OSError:
        return None


def load_dotenv(path: Path) -> None:
    if not path.exists() or not path.is_file():
        return
    try:
        for raw in path.read_text(encoding="utf-8", errors="replace").splitlines():
            line = raw.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            key = key.strip()
            value = value.strip().strip("'").strip('"')
            if key and key not in os.environ:
                os.environ[key] = value
    except OSError:
        return


def pick_first_existing(base: Path, names: List[str]) -> Tuple[Optional[Path], Optional[str]]:
    for name in names:
        p = base / name
        txt = read_file_if_exists(p)
        if txt is not None:
            return p, txt
    return None, None


def parse_smoke(smoke_text: Optional[str]) -> Dict[str, object]:
    if smoke_text is None:
        return {"status": "unknown", "http_statuses_observed": [], "notes": "smoke_test.txt missing"}

    lower = smoke_text.lower()
    statuses = sorted(
        set(
            re.findall(
                r"(?:http|status|code|->)\s*[:=]?\s*([1-5][0-9]{2})\b",
                lower,
                flags=re.IGNORECASE,
            )
        )
    )

    fail_markers = [
        "smoke test failed",
        "assertionerror",
        "traceback",
        "connection refused",
        "timeout",
        "timed out",
        "failed",
        "error",
    ]
    pass_markers = ["smoke test passed", "all checks passed", "result: pass", "status: pass"]

    has_fail_marker = any(m in lower for m in fail_markers)
    has_pass_marker = any(m in lower for m in pass_markers)
    has_5xx = any(s.startswith("5") for s in statuses)

    if has_fail_marker or has_5xx:
        status = "fail"
    elif has_pass_marker:
        status = "pass"
    else:
        status = "unknown"

    note_parts = []
    if has_5xx:
        note_parts.append("5xx observed")
    if has_fail_marker and not has_5xx:
        note_parts.append("failure marker found")
    if has_pass_marker and status != "pass":
        note_parts.append("pass marker conflicts with failures")
    notes = ", ".join(note_parts) if note_parts else "no decisive marker"

    return {
        "status": status,
        "http_statuses_observed": statuses,
        "notes": notes,
    }


def parse_container_status(compose_text: Optional[str]) -> Dict[str, str]:
    if compose_text is None:
        return {"status": "unknown", "restart_count": "unknown"}

    lower = compose_text.lower()

    status = "unknown"
    if "restarting" in lower:
        status = "restarting"
    elif "exited" in lower or "dead" in lower:
        status = "exited"
    elif re.search(r"\bup\b|\brunning\b", lower):
        status = "running"

    restart_count = "unknown"
    m = re.search(r"restarting\s*\((\d+)\)", compose_text, flags=re.IGNORECASE)
    if m:
        restart_count = m.group(1)

    return {"status": status, "restart_count": restart_count}


def parse_health(health_text: Optional[str]) -> Dict[str, str]:
    if health_text is None:
        return {"status": "unknown", "raw_hint": "health endpoint evidence missing"}

    raw_hint = safe_line(health_text.splitlines()[0] if health_text.splitlines() else health_text)

    try:
        parsed = json.loads(health_text)
        if isinstance(parsed, dict):
            status_val = str(parsed.get("status", "")).lower()
            if status_val == "up":
                return {"status": "up", "raw_hint": safe_line(json.dumps({"status": parsed.get("status")}))}
            if status_val == "down":
                return {"status": "down", "raw_hint": safe_line(json.dumps({"status": parsed.get("status")}))}
    except json.JSONDecodeError:
        pass

    lower = health_text.lower()
    if re.search(r"\bdown\b", lower):
        return {"status": "down", "raw_hint": raw_hint}
    if re.search(r"\bup\b", lower):
        return {"status": "up", "raw_hint": raw_hint}
    return {"status": "unknown", "raw_hint": raw_hint}


def collect_source_lines(named_sources: Dict[str, Optional[str]]) -> List[Tuple[str, str]]:
    lines = []
    for source_name, content in named_sources.items():
        if content is None:
            continue
        for line in content.splitlines():
            stripped = line.strip()
            if stripped:
                lines.append((source_name, stripped))
    return lines


def match_causes(
    source_lines: List[Tuple[str, str]],
    container_status: Dict[str, str],
    smoke_signal: Dict[str, object],
    health_signal: Dict[str, str],
) -> List[CauseMatch]:
    matches: List[CauseMatch] = []
    benign_timeout_re = re.compile(
        r"(does not support get/set network timeout|feature not supported)",
        flags=re.IGNORECASE,
    )

    for entry in PATTERN_LIBRARY:
        cause_label = entry["cause"]
        patterns = [re.compile(p, flags=re.IGNORECASE) for p in entry["patterns"]]
        evidence_lines = []
        for src, line in source_lines:
            if any(p.search(line) for p in patterns):
                if cause_label == "Service dependency timeout" and benign_timeout_re.search(line):
                    continue
                evidence_lines.append(f"[{src}] {safe_line(line)}")
                if len(evidence_lines) >= 2:
                    break
        if evidence_lines:
            matches.append(
                CauseMatch(
                    cause=cause_label,
                    likelihood=entry["base_likelihood"],
                    evidence=evidence_lines,
                    actions=entry["actions"],
                )
            )

    if container_status["status"] == "restarting" and not any(m.cause == "Crash loop" for m in matches):
        matches.append(
            CauseMatch(
                cause="Crash loop",
                likelihood=0.72,
                evidence=["[compose_ps.txt|docker_ps.txt] Container status indicates restarting"],
                actions=[
                    {
                        "action": "Inspect first startup exception and restart frequency",
                        "reason": "Container is restarting and likely not stabilizing.",
                        "priority": "P0",
                    }
                ],
            )
        )

    if smoke_signal["status"] == "fail" and not matches:
        fallback_evidence = ["[smoke_test.txt] Smoke test marked as fail"]
        matches.append(
            CauseMatch(
                cause="Application failed smoke validation",
                likelihood=0.6,
                evidence=fallback_evidence,
                actions=[
                    {
                        "action": "Review failing smoke endpoint assertions in staging",
                        "reason": "Smoke checks failed but logs do not show a single dominant pattern.",
                        "priority": "P0",
                    }
                ],
            )
        )

    if health_signal["status"] == "down":
        has_health_cause = any(m.cause == "Application health endpoint is down" for m in matches)
        if not has_health_cause:
            matches.append(
                CauseMatch(
                    cause="Application health endpoint is down",
                    likelihood=0.78,
                    evidence=[f"[health] {health_signal['raw_hint']}"],
                    actions=[
                        {
                            "action": "Inspect readiness/liveness dependencies for staging",
                            "reason": "Health endpoint reports DOWN state.",
                            "priority": "P0",
                        }
                    ],
                )
            )

    matches.sort(key=lambda x: x.likelihood, reverse=True)
    return matches[:3]


def decide_verdict(
    smoke_signal: Dict[str, object],
    container_signal: Dict[str, str],
    health_signal: Dict[str, str],
    causes: List[CauseMatch],
) -> Tuple[str, str]:
    smoke_fail = smoke_signal["status"] == "fail"
    smoke_pass = smoke_signal["status"] == "pass"
    container_running = container_signal["status"] == "running"
    container_bad = container_signal["status"] in {"restarting", "exited"}
    health_down = health_signal["status"] == "down"
    health_up = health_signal["status"] == "up"

    critical_causes = {
        "DB unreachable / wrong DB host",
        "Port binding conflict",
        "Memory pressure / OOM",
        "Wrong image tag or registry auth",
        "Deployment configuration error",
        "Crash loop",
        "Application health endpoint is down",
    }
    has_critical = any(c.cause in critical_causes for c in causes)

    if smoke_fail or container_bad or health_down or has_critical:
        summary = "Deployment has failing signals (smoke/container/health) with evidence-backed likely causes."
        return "failed", summary

    warning_markers = any(
        c.cause in {"Service dependency timeout", "Auth/config issue on staging endpoint", "DNS / networking issue"}
        for c in causes
    )
    if smoke_pass and container_running and (health_up or health_signal["status"] == "unknown") and not warning_markers:
        summary = "Deployment appears healthy: smoke checks pass and container status is stable."
        return "healthy", summary

    summary = "Deployment is serving but warning signals or incomplete evidence indicate partial risk."
    return "degraded", summary


def compute_confidence(
    available_required: int,
    has_health: bool,
    verdict_state: str,
    causes: List[CauseMatch],
    smoke_signal: Dict[str, object],
    container_signal: Dict[str, str],
    health_signal: Dict[str, str],
) -> float:
    if available_required <= 0:
        confidence = 0.1
    elif available_required == 1:
        confidence = 0.35
    elif available_required == 2:
        confidence = 0.55
    else:
        confidence = 0.75

    if has_health:
        confidence += 0.1

    contradictory = False
    if smoke_signal["status"] == "pass" and container_signal["status"] in {"restarting", "exited"}:
        contradictory = True
    if smoke_signal["status"] == "pass" and health_signal["status"] == "down":
        contradictory = True
    if contradictory:
        confidence -= 0.18

    if verdict_state in {"failed", "degraded"} and not causes:
        confidence -= 0.1

    if smoke_signal["status"] == "unknown":
        confidence -= 0.08
    if container_signal["status"] == "unknown":
        confidence -= 0.08
    if health_signal["status"] == "unknown":
        confidence -= 0.04

    confidence = max(0.05, min(0.99, confidence))
    return round(confidence, 2)


def merge_actions(causes: List[CauseMatch], verdict_state: str) -> List[Dict[str, str]]:
    seen = set()
    out: List[Dict[str, str]] = []
    for c in causes:
        for action in c.actions:
            key = (action["action"], action["priority"])
            if key not in seen:
                seen.add(key)
                out.append(action)

    if verdict_state == "healthy":
        out.append(
            {
                "action": "Keep monitoring smoke, health, and restart count across next deploys",
                "reason": "Current evidence does not show critical failures.",
                "priority": "P2",
            }
        )
    elif verdict_state == "degraded":
        out.append(
            {
                "action": "Collect fresh smoke + logs + stats evidence after 5-10 minutes",
                "reason": "Signals are mixed and may indicate intermittent instability.",
                "priority": "P1",
            }
        )
    else:
        out.append(
            {
                "action": "Escalate to on-call engineer for staging triage before promoting",
                "reason": "Failure-level signals make deployment unsafe to promote.",
                "priority": "P0",
            }
        )
    return out[:6]


def load_meta(meta_text: Optional[str]) -> Dict[str, str]:
    if meta_text is None:
        return {}
    try:
        parsed = json.loads(meta_text)
        if isinstance(parsed, dict):
            return {str(k): str(v) for k, v in parsed.items()}
    except json.JSONDecodeError:
        return {}
    return {}


def build_limits(
    smoke_text: Optional[str],
    compose_text: Optional[str],
    logs_text: Optional[str],
    health_text: Optional[str],
) -> List[str]:
    limits = []
    if smoke_text is None:
        limits.append("Missing required evidence: smoke_test.txt")
    if compose_text is None:
        limits.append("Missing required evidence: compose_ps.txt or docker_ps.txt")
    if logs_text is None:
        limits.append("Missing required evidence: container_logs.txt")
    if health_text is None:
        limits.append("Missing optional evidence: health.json or health.txt")
    else:
        lower = health_text.lower()
        if "http 404" in lower or '"status":404' in lower or '"status": 404' in lower:
            limits.append("Health endpoint returned 404; health path may be unavailable in staging.")
    return limits


def extract_relevant_lines(text: Optional[str], max_lines: int = 20) -> List[str]:
    if text is None:
        return []
    out = []
    for line in text.splitlines():
        s = line.strip()
        if not s:
            continue
        if re.search(r"(error|exception|fail|timeout|refused|restarting|exited|http\s+[45]\d\d)", s, flags=re.IGNORECASE):
            out.append(safe_line(s))
        if len(out) >= max_lines:
            break
    return out


def apply_llm_decision(
    report: Dict[str, object],
    llm: Dict[str, object],
    smoke_signal: Dict[str, object],
    container_signal: Dict[str, str],
    health_signal: Dict[str, str],
) -> Tuple[Dict[str, object], bool]:
    changed = False
    llm_state = str(llm.get("verdict_state", "")).strip().lower()
    llm_confidence = llm.get("confidence")
    summary = str(llm.get("summary", "")).strip()
    needs_human = llm.get("needs_human")

    hard_failed = (
        smoke_signal["status"] == "fail"
        or container_signal["status"] in {"restarting", "exited"}
        or health_signal["status"] == "down"
    )

    if llm_state in {"healthy", "degraded", "failed"}:
        # Guardrails: the LLM can refine the decision, but cannot overrule hard-fail evidence.
        if hard_failed and llm_state == "healthy":
            llm_state = "failed"
        changed = changed or report["verdict"].get("state") != llm_state
        report["verdict"]["state"] = llm_state

    if isinstance(llm_confidence, (int, float)):
        bounded_conf = max(0.05, min(0.99, float(llm_confidence)))
        bounded_conf = round(bounded_conf, 2)
        changed = changed or report["verdict"].get("confidence") != bounded_conf
        report["verdict"]["confidence"] = bounded_conf

    if summary:
        changed = changed or report["verdict"].get("summary") != safe_line(summary)
        report["verdict"]["summary"] = safe_line(summary)

    if isinstance(needs_human, bool):
        changed = changed or report.get("needs_human") != needs_human
        report["needs_human"] = needs_human

    return report, changed


def openrouter_enrich_report(
    report: Dict[str, object],
    smoke_text: Optional[str],
    compose_text: Optional[str],
    logs_text: Optional[str],
    health_text: Optional[str],
    mode: str,
    model: str,
    smoke_signal: Dict[str, object],
    container_signal: Dict[str, str],
    health_signal: Dict[str, str],
    timeout_sec: int = 20,
) -> Dict[str, object]:
    llm_meta = report.setdefault("llm", {})
    llm_meta["mode"] = mode
    llm_meta["model"] = None
    llm_meta["attempted"] = False
    llm_meta["used"] = False
    llm_meta["decision_changed"] = False
    llm_meta["error"] = None

    if mode == "off":
        return report

    api_key = os.environ.get("OPENROUTER_API_KEY", "").strip() or os.environ.get("LLM_API_KEY", "").strip()
    base_url = DEFAULT_OPENROUTER_BASE_URL
    if not api_key:
        llm_meta["error"] = "missing_openrouter_config"
        return report

    llm_input = {
        "verdict": report.get("verdict", {}),
        "signals": report.get("signals", {}),
        "top_causes": report.get("top_causes", []),
        "recommended_actions": report.get("recommended_actions", []),
        "limits": report.get("limits", []),
        "evidence_excerpt": {
            "smoke": extract_relevant_lines(smoke_text),
            "compose": extract_relevant_lines(compose_text),
            "logs": extract_relevant_lines(logs_text, max_lines=30),
            "health": extract_relevant_lines(health_text),
        },
    }

    system_prompt = (
        "You are a CI/CD deployment triage assistant. "
        "Use only provided evidence. "
        "Return STRICT JSON with keys: verdict_state, confidence, summary, extra_causes, extra_actions, needs_human. "
        "verdict_state must be one of healthy, degraded, failed. "
        "confidence must be a float between 0 and 1. "
        "extra_causes is a list of objects {cause, likelihood, evidence}. "
        "extra_actions is a list of objects {action, reason, priority}. "
        "Do not output markdown."
    )
    user_prompt = (
        "Current rule-based report follows.\n"
        f"{json.dumps(llm_input, ensure_ascii=True)}\n"
        "Decide the final deployment state from the evidence and refine the summary. "
        "Add up to 2 additional causes/actions only if evidence supports them. "
        "Do not contradict clear hard facts such as failed smoke tests, exited/restarting containers, or health DOWN."
    )

    chosen_model = model or DEFAULT_OPENROUTER_MODEL
    payload = {
        "model": chosen_model,
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ],
        "temperature": 0.1,
        "response_format": {"type": "json_object"},
    }

    url = base_url.rstrip("/") + "/chat/completions"
    req = urlrequest.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        },
        method="POST",
    )

    llm_meta["attempted"] = True
    llm_meta["model"] = chosen_model

    try:
        with urlrequest.urlopen(req, timeout=timeout_sec) as resp:
            body = resp.read().decode("utf-8", errors="replace")
    except Exception as exc:
        llm_meta["error"] = safe_line(str(exc))
        return report

    try:
        parsed = json.loads(body)
        content = parsed["choices"][0]["message"]["content"]
        llm = json.loads(content)
    except Exception as exc:
        llm_meta["error"] = safe_line(f"invalid_llm_response: {exc}")
        return report
    llm_meta["used"] = True
    report, decision_changed = apply_llm_decision(report, llm, smoke_signal, container_signal, health_signal)
    llm_meta["decision_changed"] = decision_changed

    extra_causes = llm.get("extra_causes", [])
    if isinstance(extra_causes, list):
        for cause in extra_causes[:2]:
            if not isinstance(cause, dict):
                continue
            c = str(cause.get("cause", "")).strip()
            e = cause.get("evidence", [])
            if c and isinstance(e, list) and e:
                report["top_causes"].append(
                    {
                        "cause": safe_line(c),
                        "likelihood": round(float(cause.get("likelihood", 0.5)), 2),
                        "evidence": [safe_line(str(x)) for x in e[:2]],
                        }
                )
                llm_meta["decision_changed"] = True

    extra_actions = llm.get("extra_actions", [])
    if isinstance(extra_actions, list):
        for action in extra_actions[:2]:
            if not isinstance(action, dict):
                continue
            act = str(action.get("action", "")).strip()
            rea = str(action.get("reason", "")).strip()
            prio = str(action.get("priority", "P1")).strip().upper()
            if act and rea and prio in {"P0", "P1", "P2"}:
                report["recommended_actions"].append(
                    {"action": safe_line(act), "reason": safe_line(rea), "priority": prio}
                )
                llm_meta["decision_changed"] = True

    needs_human = llm.get("needs_human")
    if isinstance(needs_human, bool):
        report["needs_human"] = report["needs_human"] or needs_human

    # Keep report concise/deterministic.
    report["top_causes"] = report["top_causes"][:3]
    report["recommended_actions"] = report["recommended_actions"][:6]
    return report


def write_markdown(report: Dict[str, object], path: Path) -> None:
    verdict = report["verdict"]
    signals = report["signals"]
    causes = report["top_causes"]
    actions = report["recommended_actions"]
    limits = report["limits"]
    llm = report.get("llm", {})

    lines = [
        "# Deployment Verifier Report",
        "",
        f"- State: **{verdict['state']}**",
        f"- Confidence: **{verdict['confidence']}**",
        f"- Summary: {verdict['summary']}",
        "",
        "## Signals",
        f"- Smoke test: {signals['smoke_test']['status']} (HTTP: {', '.join(signals['smoke_test']['http_statuses_observed']) or 'none'})",
        f"- Container: {signals['container']['status']} (restart_count: {signals['container']['restart_count']})",
        f"- Health endpoint: {signals['health_endpoint']['status']} ({signals['health_endpoint']['raw_hint']})",
        "",
        "## Top Causes",
    ]

    if causes:
        for c in causes:
            lines.append(f"- {c['cause']} (likelihood={c['likelihood']})")
            for ev in c["evidence"][:2]:
                lines.append(f"  - evidence: `{ev}`")
    else:
        lines.append("- No specific cause matched")

    lines.extend(["", "## Recommended Actions"])
    for a in actions:
        lines.append(f"- [{a['priority']}] {a['action']} — {a['reason']}")

    lines.extend(["", "## Limits"])
    if limits:
        for l in limits:
            lines.append(f"- {l}")
    else:
        lines.append("- No major evidence limitations detected")

    lines.extend(
        [
            "",
            "## LLM",
            f"- Mode: {llm.get('mode', 'unknown')}",
            f"- Attempted: {llm.get('attempted', False)}",
            f"- Used: {llm.get('used', False)}",
            f"- Decision changed: {llm.get('decision_changed', False)}",
            f"- Model: {llm.get('model') or 'n/a'}",
        ]
    )
    if llm.get("error"):
        lines.append(f"- Error: {llm['error']}")

    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def run(args: argparse.Namespace) -> Dict[str, object]:
    evidence_dir = Path(args.evidence_dir)
    out_path = Path(args.out)

    smoke_path, smoke_text = pick_first_existing(evidence_dir, ["smoke_test.txt"])
    compose_path, compose_text = pick_first_existing(evidence_dir, ["compose_ps.txt", "docker_ps.txt"])
    logs_path, logs_text = pick_first_existing(evidence_dir, ["container_logs.txt"])
    _, health_text = pick_first_existing(evidence_dir, ["health.json", "health.txt"])
    _, meta_text = pick_first_existing(evidence_dir, ["deploy_meta.json"])

    meta = load_meta(meta_text)

    smoke_signal = parse_smoke(smoke_text)
    container_signal = parse_container_status(compose_text)
    health_signal = parse_health(health_text)

    source_lines = collect_source_lines(
        {
            smoke_path.name if smoke_path else "smoke_test.txt": smoke_text,
            compose_path.name if compose_path else "compose_ps.txt|docker_ps.txt": compose_text,
            logs_path.name if logs_path else "container_logs.txt": logs_text,
            "health": health_text,
        }
    )

    causes = match_causes(source_lines, container_signal, smoke_signal, health_signal)
    verdict_state, verdict_summary = decide_verdict(smoke_signal, container_signal, health_signal, causes)

    available_required = sum(x is not None for x in [smoke_text, compose_text, logs_text])
    confidence = compute_confidence(
        available_required=available_required,
        has_health=health_text is not None,
        verdict_state=verdict_state,
        causes=causes,
        smoke_signal=smoke_signal,
        container_signal=container_signal,
        health_signal=health_signal,
    )

    limits = build_limits(smoke_text, compose_text, logs_text, health_text)

    report = {
        "agent": {"name": "deployment_verifier_triage", "version": "1.0"},
        "context": {
            "environment": "staging",
            "service": args.service_name or "backend",
            "build_number": str(args.build_number or meta.get("build_number", "unknown")),
            "image": str(args.image or meta.get("image", "unknown")),
            "staging_host": str(args.staging_host or meta.get("staging_host", "unknown")),
            "timestamp_utc": utc_now_iso(),
        },
        "verdict": {
            "state": verdict_state,
            "confidence": confidence,
            "summary": verdict_summary,
        },
        "signals": {
            "smoke_test": smoke_signal,
            "container": container_signal,
            "health_endpoint": health_signal,
        },
        "top_causes": [
            {
                "cause": c.cause,
                "likelihood": round(c.likelihood, 2),
                "evidence": c.evidence,
            }
            for c in causes
        ],
        "recommended_actions": merge_actions(causes, verdict_state),
        "needs_human": verdict_state != "healthy" or confidence < 0.75,
        "limits": limits,
        "llm": {
            "mode": args.llm_mode,
            "attempted": False,
            "used": False,
            "decision_changed": False,
            "model": None,
            "error": None,
        },
    }

    report = openrouter_enrich_report(
        report=report,
        smoke_text=smoke_text,
        compose_text=compose_text,
        logs_text=logs_text,
        health_text=health_text,
        mode=args.llm_mode,
        model=args.llm_model,
        smoke_signal=smoke_signal,
        container_signal=container_signal,
        health_signal=health_signal,
    )

    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")

    if args.md_out:
        write_markdown(report, Path(args.md_out))

    return report


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Deployment Verifier & Triage (staging)")
    parser.add_argument("--evidence_dir", required=True, help="Path to evidence bundle directory")
    parser.add_argument("--out", required=True, help="Output path for JSON report")
    parser.add_argument("--md_out", help="Optional output path for markdown report")
    parser.add_argument("--service_name", help="Service name override")
    parser.add_argument("--build_number", help="Build number override")
    parser.add_argument("--image", help="Image tag override")
    parser.add_argument("--staging_host", help="Staging host override")
    parser.add_argument(
        "--llm_mode",
        choices=["auto", "off"],
        default="auto",
        help="Use OpenRouter LLM enrichment when config is available (default: auto)",
    )
    parser.add_argument(
        "--llm_model",
        default=DEFAULT_OPENROUTER_MODEL,
        help="OpenRouter model id (default: openai/gpt-4o-mini)",
    )
    return parser


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()

    try:
        run(args)
    except Exception as exc:
        fallback = {
            "agent": {"name": "deployment_verifier_triage", "version": "1.0"},
            "context": {
                "environment": "staging",
                "service": args.service_name or "backend",
                "build_number": str(args.build_number or "unknown"),
                "image": str(args.image or "unknown"),
                "staging_host": str(args.staging_host or "unknown"),
                "timestamp_utc": utc_now_iso(),
            },
            "verdict": {
                "state": "degraded",
                "confidence": 0.1,
                "summary": "Agent execution error; report generated in fallback mode.",
            },
            "signals": {
                "smoke_test": {"status": "unknown", "http_statuses_observed": [], "notes": "agent fallback mode"},
                "container": {"status": "unknown", "restart_count": "unknown"},
                "health_endpoint": {"status": "unknown", "raw_hint": "agent fallback mode"},
            },
            "top_causes": [
                {
                    "cause": "Agent runtime error",
                    "likelihood": 0.99,
                    "evidence": [safe_line(str(exc))],
                }
            ],
            "recommended_actions": [
                {
                    "action": "Inspect analyzer script error and rerun with complete evidence bundle",
                    "reason": "Analyzer could not complete normal parsing flow.",
                    "priority": "P0",
                }
            ],
            "needs_human": True,
            "limits": ["Analyzer encountered an internal error during execution."],
        }
        out = Path(args.out)
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(json.dumps(fallback, indent=2) + "\n", encoding="utf-8")

        if args.md_out:
            write_markdown(fallback, Path(args.md_out))

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
