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
MAX_TOP_SIGNATURES = 3
DEFAULT_OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1"
DEFAULT_OPENROUTER_MODEL = "openai/gpt-4o-mini"


@dataclass(frozen=True)
class SignatureRule:
    key: str
    title: str
    severity: str
    recommended_action: str
    patterns: Tuple[str, ...]


SIGNATURE_RULES: Tuple[SignatureRule, ...] = (
    SignatureRule(
        key="db_connectivity",
        title="Database connectivity errors",
        severity="critical",
        recommended_action="Verify staging database reachability and datasource host settings.",
        patterns=(
            r"connection refused",
            r"could not connect to server",
            r"postgres",
            r"\b5432\b",
        ),
    ),
    SignatureRule(
        key="missing_config",
        title="Missing configuration",
        severity="critical",
        recommended_action="Inspect unresolved environment variables or Spring placeholders in staging config.",
        patterns=(
            r"could not resolve placeholder",
            r"environment variable .* not set",
            r"missing required",
            r"no value present",
        ),
    ),
    SignatureRule(
        key="crash_loop",
        title="Crash loop indicators",
        severity="critical",
        recommended_action="Inspect the first fatal exception and recent restart sequence for the service.",
        patterns=(
            r"exception",
            r"traceback",
            r"crashloop",
            r"restarting",
        ),
    ),
    SignatureRule(
        key="oom",
        title="Memory pressure or OOM",
        severity="critical",
        recommended_action="Check memory limits and recent usage spikes before the service degrades further.",
        patterns=(
            r"outofmemoryerror",
            r"oomkilled",
            r"killed",
            r"cannot allocate memory",
        ),
    ),
    SignatureRule(
        key="port_conflict",
        title="Port binding conflict",
        severity="critical",
        recommended_action="Confirm the exposed port is free and the compose mapping matches the staging host.",
        patterns=(
            r"bindexception",
            r"address already in use",
            r"port is already allocated",
        ),
    ),
    SignatureRule(
        key="timeouts",
        title="Dependency timeout warnings",
        severity="warning",
        recommended_action="Check upstream dependency latency and confirm timeout settings remain appropriate.",
        patterns=(
            r"timeout",
            r"timed out",
            r"connecttimeoutexception",
            r"readtimeoutexception",
        ),
    ),
    SignatureRule(
        key="auth_failures",
        title="Repeated auth failures",
        severity="warning",
        recommended_action="Review recent 401/403 responses and confirm expected auth behavior on staging endpoints.",
        patterns=(
            r"\b401\b",
            r"\b403\b",
            r"unauthorized",
            r"forbidden",
        ),
    ),
    SignatureRule(
        key="http_5xx",
        title="Server-side 5xx responses",
        severity="warning",
        recommended_action="Inspect recent request failures and correlate them with application log entries.",
        patterns=(
            r"http.{0,20}\b5\d{2}\b",
            r"status.{0,12}\b5\d{2}\b",
            r"response.{0,12}\b5\d{2}\b",
            r"\bserver error\b",
        ),
    ),
)


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


def pick_first_existing(base: Path, names: List[str]) -> Tuple[Optional[Path], Optional[str]]:
    for name in names:
        candidate = base / name
        text = read_file_if_exists(candidate)
        if text is not None:
            return candidate, text
    return None, None


def load_json_if_possible(text: Optional[str]) -> Optional[object]:
    if not text:
        return None
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        return None


def parse_int(value: object) -> Optional[int]:
    if value is None:
        return None
    if isinstance(value, bool):
        return int(value)
    if isinstance(value, int):
        return value
    if isinstance(value, float):
        return int(value)
    text = str(value).strip()
    if not text:
        return None
    try:
        return int(float(text))
    except ValueError:
        return None


def map_app_status(raw_value: object) -> str:
    if raw_value is None:
        return "UNKNOWN"
    text = str(raw_value).strip().upper()
    if text in {"UP", "OK", "HEALTHY", "PASS"}:
        return "UP"
    if text in {"DOWN", "ERROR", "FAILED", "FAIL", "UNHEALTHY"}:
        return "DOWN"
    return "UNKNOWN"


def parse_meta(meta_text: Optional[str]) -> Dict[str, object]:
    parsed = load_json_if_possible(meta_text)
    if isinstance(parsed, dict):
        return parsed
    return {}


def extract_relevant_lines(text: Optional[str], max_lines: int = 20) -> List[str]:
    if text is None:
        return []
    out: List[str] = []
    for line in text.splitlines():
        s = line.strip()
        if not s:
            continue
        if re.search(
            r"(error|exception|fail|timeout|refused|restarting|exited|http\s+[45]\d\d|oom|unauthorized|forbidden)",
            s,
            flags=re.IGNORECASE,
        ):
            out.append(safe_line(s))
        if len(out) >= max_lines:
            break
    return out


def extract_head_lines(text: Optional[str], max_lines: int = 8) -> List[str]:
    if text is None:
        return []
    out: List[str] = []
    for line in text.splitlines():
        s = line.strip()
        if not s:
            continue
        out.append(safe_line(s))
        if len(out) >= max_lines:
            break
    return out


def merge_unique_strings(existing: List[str], extras: object, limit: int = 5) -> List[str]:
    out = [safe_line(str(x)) for x in existing if str(x).strip()]
    seen = set(out)
    if isinstance(extras, list):
        for item in extras:
            text = safe_line(str(item))
            if text and text not in seen:
                out.append(text)
                seen.add(text)
            if len(out) >= limit:
                break
    return out[:limit]


def merge_follow_up_checks(existing: List[Dict[str, str]], extras: object, limit: int = 5) -> List[Dict[str, str]]:
    out: List[Dict[str, str]] = []
    seen = set()

    for item in existing:
        if not isinstance(item, dict):
            continue
        key = (item.get("check", ""), item.get("expected_signal", ""))
        if key not in seen:
            out.append(item)
            seen.add(key)

    if isinstance(extras, list):
        for item in extras:
            if not isinstance(item, dict):
                continue
            check = safe_line(str(item.get("check", "")).strip())
            expected_signal = safe_line(str(item.get("expected_signal", "")).strip())
            urgency = str(item.get("urgency", "soon")).strip().lower()
            if urgency not in {"now", "soon", "later"}:
                urgency = "soon"
            if not check or not expected_signal:
                continue
            key = (check, expected_signal)
            if key in seen:
                continue
            out.append(
                {
                    "check": check,
                    "expected_signal": expected_signal,
                    "urgency": urgency,
                }
            )
            seen.add(key)
            if len(out) >= limit:
                break

    return out[:limit]


def parse_health(health_text: Optional[str]) -> Dict[str, object]:
    if health_text is None:
        return {
            "http_status": None,
            "app_status": "UNKNOWN",
            "latency_ms": None,
            "hint": "health evidence missing",
        }

    hint = ""
    parsed = load_json_if_possible(health_text)
    if isinstance(parsed, dict):
        http_status = parse_int(parsed.get("http_status"))
        latency_ms = parse_int(parsed.get("latency_ms"))
        body_excerpt = safe_line(str(parsed.get("body_excerpt", ""))) if parsed.get("body_excerpt") else ""
        app_status = map_app_status(parsed.get("app_status"))

        if app_status == "UNKNOWN":
            body_json = parsed.get("body_json")
            if isinstance(body_json, dict):
                app_status = map_app_status(body_json.get("status"))
            elif body_excerpt:
                status_match = re.search(r'"status"\s*:\s*"([^"]+)"', body_excerpt, flags=re.IGNORECASE)
                if status_match:
                    app_status = map_app_status(status_match.group(1))

        if body_excerpt:
            hint = body_excerpt
        elif parsed.get("hint"):
            hint = safe_line(str(parsed["hint"]))
        else:
            hint = safe_line(health_text)

        return {
            "http_status": http_status,
            "app_status": app_status,
            "latency_ms": latency_ms,
            "hint": hint or "health response parsed without body excerpt",
        }

    lines = [line.strip() for line in health_text.splitlines() if line.strip()]
    first_line = lines[0] if lines else health_text.strip()
    body_line = ""
    for line in lines[1:]:
        if "body " in line.lower():
            body_line = line.split("body", 1)[1].strip()
            break

    http_match = re.search(r"\bHTTP\s+([1-5][0-9]{2}|000)\b", health_text, flags=re.IGNORECASE)
    latency_match = re.search(r"latency_ms[=: ]+(\d+)", health_text, flags=re.IGNORECASE)
    http_status = parse_int(http_match.group(1)) if http_match else None
    latency_ms = parse_int(latency_match.group(1)) if latency_match else None

    app_status = "UNKNOWN"
    body_source = body_line or health_text
    body_json_match = re.search(r'(\{.*\})', body_source)
    if body_json_match:
        body_json = load_json_if_possible(body_json_match.group(1))
        if isinstance(body_json, dict):
            app_status = map_app_status(body_json.get("status"))

    if app_status == "UNKNOWN":
        if re.search(r"\b(up|ok|healthy)\b", body_source, flags=re.IGNORECASE):
            app_status = "UP"
        elif re.search(r"\b(down|unhealthy)\b", body_source, flags=re.IGNORECASE):
            app_status = "DOWN"

    hint = safe_line(body_line or first_line or health_text)
    return {
        "http_status": http_status,
        "app_status": app_status,
        "latency_ms": latency_ms,
        "hint": hint or "health response missing body excerpt",
    }


def parse_compose_ps(compose_text: Optional[str]) -> List[Dict[str, object]]:
    if not compose_text:
        return []

    containers: List[Dict[str, object]] = []
    seen_names = set()
    for raw_line in compose_text.splitlines():
        line = raw_line.strip()
        if not line or line.lower().startswith("name") or set(line) <= {"-", " "}:
            continue
        if line.lower().startswith("warning"):
            continue

        parts = re.split(r"\s{2,}", line)
        if not parts:
            continue

        name = parts[0].strip()
        lower = line.lower()
        state = "unknown"
        if "restarting" in lower:
            state = "restarting"
        elif "exited" in lower or "dead" in lower:
            state = "exited"
        elif re.search(r"\brunning\b|\bup\b", lower):
            state = "running"

        restart_count = None
        restart_match = re.search(r"restarting\s*\((\d+)\)", line, flags=re.IGNORECASE)
        if restart_match:
            restart_count = parse_int(restart_match.group(1))

        if name not in seen_names:
            containers.append({"name": name, "state": state, "restart_count": restart_count})
            seen_names.add(name)
    return containers


def parse_inspect(inspect_text: Optional[str]) -> List[Dict[str, object]]:
    parsed = load_json_if_possible(inspect_text)
    if not isinstance(parsed, list):
        return []

    containers: List[Dict[str, object]] = []
    for item in parsed:
        if not isinstance(item, dict):
            continue
        name = str(item.get("Name", "")).strip().lstrip("/")
        state_block = item.get("State") if isinstance(item.get("State"), dict) else {}
        state = str(state_block.get("Status") or "").strip().lower() or "unknown"
        if state == "dead":
            state = "exited"
        restart_count = parse_int(item.get("RestartCount"))
        if not name:
            continue
        containers.append(
            {
                "name": name,
                "state": state if state in {"running", "restarting", "exited", "paused", "created"} else "unknown",
                "restart_count": restart_count,
            }
        )
    return containers


def merge_containers(compose_containers: List[Dict[str, object]], inspect_containers: List[Dict[str, object]]) -> List[Dict[str, object]]:
    merged: Dict[str, Dict[str, object]] = {}

    for container in compose_containers:
        name = str(container.get("name", "")).strip()
        if name:
            merged[name] = dict(container)

    for container in inspect_containers:
        name = str(container.get("name", "")).strip()
        if not name:
            continue
        existing = merged.get(name, {})
        merged[name] = {
            "name": name,
            "state": container.get("state") or existing.get("state") or "unknown",
            "restart_count": container.get("restart_count")
            if container.get("restart_count") is not None
            else existing.get("restart_count"),
        }

    return sorted(merged.values(), key=lambda item: str(item.get("name", "")))


def parse_stats(stats_text: Optional[str]) -> Dict[str, Optional[str]]:
    if not stats_text:
        return {"cpu_pct": None, "mem_pct": None}

    cpu_values: List[float] = []
    mem_values: List[float] = []

    for line in stats_text.splitlines():
        parsed = load_json_if_possible(line)
        if not isinstance(parsed, dict):
            continue
        cpu_text = str(parsed.get("CPUPerc", "")).strip().rstrip("%")
        mem_text = str(parsed.get("MemPerc", "")).strip().rstrip("%")
        try:
            if cpu_text:
                cpu_values.append(float(cpu_text))
        except ValueError:
            pass
        try:
            if mem_text:
                mem_values.append(float(mem_text))
        except ValueError:
            pass

    cpu_pct = f"{max(cpu_values):.1f}%" if cpu_values else None
    mem_pct = f"{max(mem_values):.1f}%" if mem_values else None
    return {"cpu_pct": cpu_pct, "mem_pct": mem_pct}


def analyze_logs(log_text: Optional[str]) -> Dict[str, object]:
    if not log_text:
        return {"count": 0, "top_signatures": [], "details": {}, "evidence": {}}

    details: Dict[str, Dict[str, object]] = {}
    evidence: Dict[str, List[str]] = {}
    benign_timeout_re = re.compile(
        r"(does not support get/set network timeout|feature not supported)",
        flags=re.IGNORECASE,
    )

    for raw_line in log_text.splitlines():
        line = raw_line.strip()
        if not line:
            continue
        for rule in SIGNATURE_RULES:
            if any(re.search(pattern, line, flags=re.IGNORECASE) for pattern in rule.patterns):
                if rule.key == "timeouts" and benign_timeout_re.search(line):
                    break
                bucket = details.setdefault(
                    rule.key,
                    {
                        "title": rule.title,
                        "severity": rule.severity,
                        "recommended_action": rule.recommended_action,
                        "count": 0,
                    },
                )
                bucket["count"] = int(bucket["count"]) + 1
                evidence.setdefault(rule.key, [])
                if len(evidence[rule.key]) < 2:
                    evidence[rule.key].append(safe_line(line))
                break

    ordered = sorted(
        details.items(),
        key=lambda item: (-int(item[1]["count"]), item[0]),
    )
    top_signatures = [f"{item[1]['title']} ({item[1]['count']})" for item in ordered[:MAX_TOP_SIGNATURES]]
    total = sum(int(item[1]["count"]) for item in ordered)
    return {"count": total, "top_signatures": top_signatures, "details": details, "evidence": evidence}


def load_previous_state(path: Path) -> Dict[str, object]:
    text = read_file_if_exists(path)
    parsed = load_json_if_possible(text)
    if isinstance(parsed, dict):
        return parsed
    return {}


def current_state_snapshot(
    status_state: str,
    health_signal: Dict[str, object],
    containers: List[Dict[str, object]],
    log_summary: Dict[str, object],
) -> Dict[str, object]:
    return {
        "timestamp_utc": utc_now_iso(),
        "state": status_state,
        "health": {
            "http_status": health_signal.get("http_status"),
            "app_status": health_signal.get("app_status"),
        },
        "containers": {
            str(container["name"]): {
                "state": container.get("state"),
                "restart_count": container.get("restart_count"),
            }
            for container in containers
        },
        "signatures": {
            key: int(details["count"])
            for key, details in dict(log_summary.get("details", {})).items()
        },
    }


def detect_changes(previous: Dict[str, object], current: Dict[str, object]) -> Dict[str, object]:
    if not previous:
        return {"changed": False, "highlights": []}

    highlights: List[str] = []
    previous_state = str(previous.get("state", "unknown"))
    current_state = str(current.get("state", "unknown"))
    if previous_state != current_state:
        highlights.append(f"State changed from {previous_state} to {current_state}")

    prev_health = previous.get("health") if isinstance(previous.get("health"), dict) else {}
    curr_health = current.get("health") if isinstance(current.get("health"), dict) else {}

    prev_http = prev_health.get("http_status")
    curr_http = curr_health.get("http_status")
    if prev_http != curr_http:
        highlights.append(f"Health HTTP status changed from {prev_http} to {curr_http}")

    prev_app = prev_health.get("app_status")
    curr_app = curr_health.get("app_status")
    if prev_app != curr_app:
        highlights.append(f"Application health changed from {prev_app} to {curr_app}")

    prev_containers = previous.get("containers") if isinstance(previous.get("containers"), dict) else {}
    curr_containers = current.get("containers") if isinstance(current.get("containers"), dict) else {}
    for name in sorted(set(prev_containers) | set(curr_containers)):
        prev_item = prev_containers.get(name) if isinstance(prev_containers.get(name), dict) else {}
        curr_item = curr_containers.get(name) if isinstance(curr_containers.get(name), dict) else {}
        if not prev_item and curr_item:
            highlights.append(f"Container {name} appeared with state {curr_item.get('state')}")
            continue
        if prev_item and not curr_item:
            highlights.append(f"Container {name} disappeared from compose status")
            continue
        if prev_item.get("state") != curr_item.get("state"):
            highlights.append(f"Container {name} state changed from {prev_item.get('state')} to {curr_item.get('state')}")
        prev_restart = parse_int(prev_item.get("restart_count")) or 0
        curr_restart = parse_int(curr_item.get("restart_count")) or 0
        if curr_restart > prev_restart:
            highlights.append(f"Restart count increased for {name} from {prev_restart} to {curr_restart}")

    prev_signatures = previous.get("signatures") if isinstance(previous.get("signatures"), dict) else {}
    curr_signatures = current.get("signatures") if isinstance(current.get("signatures"), dict) else {}
    for key in sorted(set(prev_signatures) | set(curr_signatures)):
        prev_count = parse_int(prev_signatures.get(key)) or 0
        curr_count = parse_int(curr_signatures.get(key)) or 0
        if prev_count == 0 and curr_count > 0:
            highlights.append(f"New error signature detected: {key} ({curr_count})")
        elif prev_count > 0 and curr_count == 0:
            highlights.append(f"Error signature cleared: {key}")

    return {"changed": bool(highlights), "highlights": highlights[:6]}


def compute_status(
    health_signal: Dict[str, object],
    containers: List[Dict[str, object]],
    log_summary: Dict[str, object],
    changes: Dict[str, object],
) -> Tuple[str, str]:
    http_status = parse_int(health_signal.get("http_status"))
    app_status = str(health_signal.get("app_status", "UNKNOWN"))
    health_ok = http_status is not None and 200 <= http_status < 300 and app_status != "DOWN"
    health_failed = (http_status is not None and http_status >= 400) or app_status == "DOWN"

    states = {str(container.get("state", "unknown")) for container in containers}
    has_restarting = "restarting" in states
    has_exited = "exited" in states
    all_running = bool(containers) and states.issubset({"running"})

    signature_details = dict(log_summary.get("details", {}))
    critical_signature_counts = [
        int(details["count"])
        for details in signature_details.values()
        if str(details.get("severity")) == "critical"
    ]
    warning_signature_counts = [
        int(details["count"])
        for details in signature_details.values()
        if str(details.get("severity")) == "warning"
    ]
    critical_burst = any(count >= 3 for count in critical_signature_counts)
    warning_present = any(count > 0 for count in warning_signature_counts)

    highlights = changes.get("highlights") if isinstance(changes.get("highlights"), list) else []
    restart_increase = any("Restart count increased" in item for item in highlights)

    if health_failed or has_exited or has_restarting or critical_burst:
        return "failed", "Staging is failing: health, container state, or critical log signatures require investigation."

    if not health_ok:
        return "degraded", "Staging is reachable but health evidence is incomplete or not clearly healthy."

    if restart_increase or warning_present or signature_details:
        return "degraded", "Staging is serving traffic but warnings or new runtime changes were detected."

    if all_running:
        return "healthy", "Staging is healthy: health checks pass, containers are stable, and no new error signatures were detected."

    return "degraded", "Staging evidence is partially healthy but container state is incomplete."


def compute_confidence(
    health_text: Optional[str],
    compose_text: Optional[str],
    inspect_text: Optional[str],
    logs_text: Optional[str],
    containers: List[Dict[str, object]],
    resources: Dict[str, Optional[str]],
) -> float:
    confidence = 0.25
    if health_text is not None:
        confidence += 0.3
    if compose_text is not None or inspect_text is not None:
        confidence += 0.2
    if logs_text is not None:
        confidence += 0.15
    if resources.get("cpu_pct") or resources.get("mem_pct"):
        confidence += 0.05
    if health_text is None:
        confidence -= 0.1
    if compose_text is None and inspect_text is None and not containers:
        confidence -= 0.1
    return round(max(0.05, min(confidence, 0.95)), 2)


def build_alerts(
    status_state: str,
    health_signal: Dict[str, object],
    containers: List[Dict[str, object]],
    log_summary: Dict[str, object],
    changes: Dict[str, object],
    previous_state: Dict[str, object],
) -> List[Dict[str, object]]:
    alerts: List[Dict[str, object]] = []

    http_status = parse_int(health_signal.get("http_status"))
    if http_status is not None and http_status >= 400:
        alerts.append(
            {
                "severity": "critical" if http_status >= 500 else "warning",
                "title": "Health endpoint returned a failing status",
                "evidence": [f"HTTP {http_status}", safe_line(str(health_signal.get('hint', 'no health hint')))],
                "recommended_action": "Validate the configured staging health endpoint and inspect recent backend failures.",
            }
        )

    for container in containers:
        state = str(container.get("state", "unknown"))
        restart_count = container.get("restart_count")
        if state in {"restarting", "exited"}:
            alerts.append(
                {
                    "severity": "critical",
                    "title": f"Container {container['name']} is {state}",
                    "evidence": [f"state={state}", f"restart_count={restart_count}"],
                    "recommended_action": "Inspect recent startup logs and compare the container state with the last healthy run.",
                }
            )

    highlights = changes.get("highlights") if isinstance(changes.get("highlights"), list) else []
    for highlight in highlights:
        if "Restart count increased" in highlight:
            alerts.append(
                {
                    "severity": "warning" if status_state != "failed" else "critical",
                    "title": "Restart count increased since the last monitoring run",
                    "evidence": [highlight],
                    "recommended_action": "Inspect the log window around the restart and confirm the service stabilized afterward.",
                }
            )
            break

    details = dict(log_summary.get("details", {}))
    evidence_map = dict(log_summary.get("evidence", {}))
    ordered = sorted(
        details.items(),
        key=lambda item: (-int(item[1]["count"]), item[0]),
    )
    for key, detail in ordered[:MAX_TOP_SIGNATURES]:
        alerts.append(
            {
                "severity": str(detail.get("severity", "warning")),
                "title": str(detail.get("title", key)),
                "evidence": evidence_map.get(key, [])[:2],
                "recommended_action": str(detail.get("recommended_action")),
            }
        )

    previous_status = str(previous_state.get("state", "")) if previous_state else ""
    if previous_status in {"failed", "degraded"} and status_state == "healthy":
        alerts.insert(
            0,
            {
                "severity": "info",
                "title": "Staging recovered since the last run",
                "evidence": [f"Previous state was {previous_status}", "Current state is healthy"],
                "recommended_action": "Keep observing the next monitoring window to confirm the recovery is stable.",
            }
        )

    deduped: List[Dict[str, object]] = []
    seen = set()
    for alert in alerts:
        key = (alert["severity"], alert["title"])
        if key in seen:
            continue
        deduped.append(alert)
        seen.add(key)
    return deduped[:6]


def build_limits(
    health_text: Optional[str],
    compose_text: Optional[str],
    inspect_text: Optional[str],
    logs_text: Optional[str],
    stats_text: Optional[str],
    previous_state: Dict[str, object],
) -> List[str]:
    limits: List[str] = []
    if health_text is None:
        limits.append("Missing required evidence: monitor_health.json or monitor_health.txt")
    if compose_text is None and inspect_text is None:
        limits.append("Missing required evidence: monitor_compose_ps.txt or monitor_container_inspect.json")
    if logs_text is None:
        limits.append("Missing required evidence: monitor_container_logs.txt")
    if stats_text is None:
        limits.append("Missing optional evidence: monitor_resource_stats.txt")
    if not previous_state:
        limits.append("No previous monitor_state.json was available for change detection")
    return limits


def apply_llm_decision(
    report: Dict[str, object],
    llm: Dict[str, object],
    health_signal: Dict[str, object],
    containers: List[Dict[str, object]],
) -> Tuple[Dict[str, object], bool]:
    changed = False
    llm_state = str(llm.get("status_state", "")).strip().lower()
    llm_confidence = llm.get("confidence")
    summary = str(llm.get("one_line_summary", "")).strip()

    hard_failed = (
        (parse_int(health_signal.get("http_status")) or 0) >= 500
        or any(str(container.get("state")) in {"restarting", "exited"} for container in containers)
    )

    if llm_state in {"healthy", "degraded", "failed"}:
        if hard_failed and llm_state == "healthy":
            llm_state = "failed"
        changed = changed or report["status"].get("state") != llm_state
        report["status"]["state"] = llm_state

    if isinstance(llm_confidence, (int, float)):
        bounded_conf = round(max(0.05, min(0.99, float(llm_confidence))), 2)
        changed = changed or report["status"].get("confidence") != bounded_conf
        report["status"]["confidence"] = bounded_conf

    if summary:
        summary = safe_line(summary)
        changed = changed or report["status"].get("one_line_summary") != summary
        report["status"]["one_line_summary"] = summary

    return report, changed


def openrouter_enrich_report(
    report: Dict[str, object],
    health_text: Optional[str],
    compose_text: Optional[str],
    inspect_text: Optional[str],
    logs_text: Optional[str],
    stats_text: Optional[str],
    previous_state: Dict[str, object],
    mode: str,
    model: str,
    health_signal: Dict[str, object],
    containers: List[Dict[str, object]],
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
    if not api_key:
        llm_meta["error"] = "missing_openrouter_config"
        return report

    llm_input = {
        "context": report.get("context", {}),
        "status": report.get("status", {}),
        "signals": report.get("signals", {}),
        "changes_since_last": report.get("changes_since_last", {}),
        "alerts": report.get("alerts", []),
        "limits": report.get("limits", []),
        "previous_state": previous_state,
        "evidence_excerpt": {
            "health": extract_relevant_lines(health_text),
            "compose": extract_relevant_lines(compose_text),
            "inspect": extract_relevant_lines(inspect_text),
            "logs": extract_relevant_lines(logs_text, max_lines=30),
            "stats": extract_relevant_lines(stats_text),
        },
        "raw_excerpt": {
            "health_head": extract_head_lines(health_text, max_lines=8),
            "compose_head": extract_head_lines(compose_text, max_lines=8),
            "inspect_head": extract_head_lines(inspect_text, max_lines=8),
            "logs_head": extract_head_lines(logs_text, max_lines=12),
            "stats_head": extract_head_lines(stats_text, max_lines=8),
        },
    }

    system_prompt = (
        "You are a staging runtime monitoring assistant. "
        "Use only provided evidence. "
        "Do not invent causes, incidents, or metrics. "
        "Return strict JSON with keys: "
        "status_state, confidence, one_line_summary, operator_summary, rationale, risk_notes, follow_up_checks. "
        "status_state must be one of healthy, degraded, failed. "
        "rationale and risk_notes must be arrays of strings. "
        "follow_up_checks must be a list of objects {check, expected_signal, urgency}. "
        "If evidence is weak, say so explicitly in rationale or risk_notes."
    )

    user_prompt = (
        "Review this staging monitoring report and refine it conservatively.\n"
        "Keep the response concise, evidence-based, and machine-readable only.\n\n"
        f"{json.dumps(llm_input, ensure_ascii=True)}\n"
    )

    chosen_model = model or DEFAULT_OPENROUTER_MODEL
    req_body = {
        "model": chosen_model,
        "temperature": 0.1,
        "response_format": {"type": "json_object"},
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ],
    }

    request_obj = urlrequest.Request(
        f"{DEFAULT_OPENROUTER_BASE_URL}/chat/completions",
        data=json.dumps(req_body).encode("utf-8"),
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
            "HTTP-Referer": "https://openai.com/",
            "X-Title": "Projetweb Monitoring Summarizer",
        },
        method="POST",
    )

    llm_meta["attempted"] = True
    llm_meta["model"] = chosen_model

    try:
        with urlrequest.urlopen(request_obj, timeout=timeout_sec) as response:
            payload = json.loads(response.read().decode("utf-8", errors="replace"))
            content = payload["choices"][0]["message"]["content"]
            llm = json.loads(content)
    except (urlerror.URLError, TimeoutError, KeyError, IndexError, json.JSONDecodeError) as exc:
        llm_meta["error"] = safe_line(str(exc))
        return report
    except Exception as exc:
        llm_meta["error"] = safe_line(f"invalid_llm_response: {exc}")
        return report

    llm_meta["used"] = True
    report, decision_changed = apply_llm_decision(report, llm, health_signal, containers)
    llm_meta["decision_changed"] = decision_changed

    llm_insights = report.setdefault(
        "llm_insights",
        {
            "operator_summary": "",
            "rationale": [],
            "risk_notes": [],
            "follow_up_checks": [],
        },
    )

    operator_summary = str(llm.get("operator_summary", "")).strip()
    if operator_summary:
        llm_insights["operator_summary"] = safe_line(operator_summary)
        llm_meta["decision_changed"] = True

    llm_insights["rationale"] = merge_unique_strings(
        list(llm_insights.get("rationale", [])),
        llm.get("rationale", []),
        limit=5,
    )
    llm_insights["risk_notes"] = merge_unique_strings(
        list(llm_insights.get("risk_notes", [])),
        llm.get("risk_notes", []),
        limit=5,
    )
    llm_insights["follow_up_checks"] = merge_follow_up_checks(
        list(llm_insights.get("follow_up_checks", [])),
        llm.get("follow_up_checks", []),
        limit=5,
    )

    llm_insights["rationale"] = llm_insights["rationale"][:5]
    llm_insights["risk_notes"] = llm_insights["risk_notes"][:5]
    llm_insights["follow_up_checks"] = llm_insights["follow_up_checks"][:5]
    return report


def write_markdown(report: Dict[str, object], path: Path) -> None:
    context = report["context"]
    status = report["status"]
    signals = report["signals"]
    changes = report["changes_since_last"]
    alerts = report["alerts"]
    limits = report["limits"]
    llm = report.get("llm", {})
    llm_insights = report.get("llm_insights", {})

    lines = [
        "# Monitoring Summarizer Report",
        "",
        f"- Environment: **{context['environment']}**",
        f"- Service: **{context['service']}**",
        f"- State: **{status['state']}**",
        f"- Confidence: **{status['confidence']}**",
        f"- Summary: {status['one_line_summary']}",
        "",
        "## Signals",
        f"- Health: HTTP {signals['health']['http_status']} / app {signals['health']['app_status']} / latency {signals['health']['latency_ms']} ms",
        f"- Errors last window: {signals['errors_last_window']['count']}",
        f"- Top signatures: {', '.join(signals['errors_last_window']['top_signatures']) or 'none'}",
        f"- Resources: CPU {signals['resources']['cpu_pct'] or 'n/a'}, MEM {signals['resources']['mem_pct'] or 'n/a'}",
        "",
        "## Containers",
    ]

    containers = signals.get("containers", [])
    if containers:
        for container in containers:
            lines.append(
                f"- {container['name']}: {container['state']} (restart_count={container['restart_count']})"
            )
    else:
        lines.append("- No container state could be parsed")

    lines.extend(["", "## Changes Since Last"])
    if changes["changed"]:
        for highlight in changes["highlights"]:
            lines.append(f"- {highlight}")
    else:
        lines.append("- No changes detected")

    lines.extend(["", "## Alerts"])
    if alerts:
        for alert in alerts:
            lines.append(
                f"- [{alert['severity']}] {alert['title']} — {alert['recommended_action']}"
            )
    else:
        lines.append("- No alerts generated")

    if llm_insights.get("operator_summary"):
        lines.extend(["", "## LLM Operator Summary", f"- {llm_insights['operator_summary']}"])

    rationale = list(llm_insights.get("rationale", []))
    if rationale:
        lines.extend(["", "## LLM Rationale"])
        for item in rationale:
            lines.append(f"- {item}")

    follow_up_checks = list(llm_insights.get("follow_up_checks", []))
    if follow_up_checks:
        lines.extend(["", "## LLM Follow-Up Checks"])
        for item in follow_up_checks:
            lines.append(
                f"- [{item.get('urgency', 'soon')}] {item.get('check', '')} -> {item.get('expected_signal', '')}"
            )

    risk_notes = list(llm_insights.get("risk_notes", []))
    if risk_notes:
        lines.extend(["", "## LLM Risk Notes"])
        for item in risk_notes:
            lines.append(f"- {item}")

    lines.extend(["", "## Limits"])
    if limits:
        for limit in limits:
            lines.append(f"- {limit}")
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
    state_path = Path(args.state)
    out_path = Path(args.out)

    _, meta_text = pick_first_existing(evidence_dir, ["monitor_meta.json"])
    compose_path, compose_text = pick_first_existing(
        evidence_dir,
        ["monitor_compose_ps.txt", "compose_ps.txt", "docker_ps.txt"],
    )
    inspect_path, inspect_text = pick_first_existing(
        evidence_dir,
        ["monitor_container_inspect.json", "container_inspect.json"],
    )
    logs_path, logs_text = pick_first_existing(
        evidence_dir,
        ["monitor_container_logs.txt", "container_logs.txt"],
    )
    _, health_text = pick_first_existing(
        evidence_dir,
        ["monitor_health.json", "monitor_health.txt", "health.json", "health.txt"],
    )
    _, stats_text = pick_first_existing(
        evidence_dir,
        ["monitor_resource_stats.txt", "resource_stats.txt"],
    )

    meta = parse_meta(meta_text)
    previous_state = load_previous_state(state_path)

    health_signal = parse_health(health_text)
    compose_containers = parse_compose_ps(compose_text)
    inspect_containers = parse_inspect(inspect_text)
    containers = merge_containers(compose_containers, inspect_containers)
    resources = parse_stats(stats_text)
    log_summary = analyze_logs(logs_text)

    provisional_state = current_state_snapshot("unknown", health_signal, containers, log_summary)
    provisional_changes = detect_changes(previous_state, provisional_state)
    status_state, summary = compute_status(health_signal, containers, log_summary, provisional_changes)
    confidence = compute_confidence(
        health_text=health_text,
        compose_text=compose_text,
        inspect_text=inspect_text,
        logs_text=logs_text,
        containers=containers,
        resources=resources,
    )

    current_state = current_state_snapshot(status_state, health_signal, containers, log_summary)
    changes = detect_changes(previous_state, current_state)
    alerts = build_alerts(status_state, health_signal, containers, log_summary, changes, previous_state)
    limits = build_limits(health_text, compose_text, inspect_text, logs_text, stats_text, previous_state)

    report = {
        "agent": {"name": "monitoring_summarizer", "version": "1.0"},
        "context": {
            "environment": "staging",
            "service": str(args.service_name or meta.get("service") or "backend"),
            "staging_host": str(args.staging_host or meta.get("staging_host") or "unknown"),
            "timestamp_utc": utc_now_iso(),
            "window": {
                "logs": f"last_{int(args.window_minutes)}_minutes",
                "health_timeout_ms": int(args.health_timeout_ms),
            },
        },
        "status": {
            "state": status_state,
            "confidence": confidence,
            "one_line_summary": summary,
        },
        "signals": {
            "health": health_signal,
            "containers": containers,
            "errors_last_window": {
                "count": int(log_summary["count"]),
                "top_signatures": list(log_summary["top_signatures"]),
            },
            "resources": resources,
        },
        "changes_since_last": changes,
        "alerts": alerts,
        "limits": limits,
        "llm": {
            "mode": args.llm_mode,
            "attempted": False,
            "used": False,
            "decision_changed": False,
            "model": None,
            "error": None,
        },
        "llm_insights": {
            "operator_summary": "",
            "rationale": [],
            "risk_notes": [],
            "follow_up_checks": [],
        },
    }

    report = openrouter_enrich_report(
        report=report,
        health_text=health_text,
        compose_text=compose_text,
        inspect_text=inspect_text,
        logs_text=logs_text,
        stats_text=stats_text,
        previous_state=previous_state,
        mode=args.llm_mode,
        model=args.llm_model,
        health_signal=health_signal,
        containers=containers,
    )

    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")

    state_path.parent.mkdir(parents=True, exist_ok=True)
    state_path.write_text(json.dumps(current_state, indent=2) + "\n", encoding="utf-8")

    if args.md_out:
        write_markdown(report, Path(args.md_out))

    return report


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Monitoring Summarizer (staging runtime)")
    parser.add_argument("--evidence_dir", required=True, help="Path to the monitoring evidence bundle")
    parser.add_argument("--out", required=True, help="Output path for JSON report")
    parser.add_argument("--md_out", help="Optional output path for markdown report")
    parser.add_argument("--state", required=True, help="Path to persisted monitor state file")
    parser.add_argument("--service_name", help="Service name override")
    parser.add_argument("--staging_host", help="Staging host override")
    parser.add_argument("--window_minutes", default=5, type=int, help="Log lookback window in minutes")
    parser.add_argument("--health_timeout_ms", default=2000, type=int, help="Health probe timeout in milliseconds")
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
            "agent": {"name": "monitoring_summarizer", "version": "1.0"},
            "context": {
                "environment": "staging",
                "service": args.service_name or "backend",
                "staging_host": args.staging_host or "unknown",
                "timestamp_utc": utc_now_iso(),
                "window": {
                    "logs": f"last_{int(args.window_minutes)}_minutes",
                    "health_timeout_ms": int(args.health_timeout_ms),
                },
            },
            "status": {
                "state": "degraded",
                "confidence": 0.1,
                "one_line_summary": "Monitoring agent execution error; fallback report generated.",
            },
            "signals": {
                "health": {
                    "http_status": None,
                    "app_status": "UNKNOWN",
                    "latency_ms": None,
                    "hint": "agent fallback mode",
                },
                "containers": [],
                "errors_last_window": {"count": 0, "top_signatures": []},
                "resources": {"cpu_pct": None, "mem_pct": None},
            },
            "changes_since_last": {"changed": False, "highlights": []},
            "alerts": [
                {
                    "severity": "critical",
                    "title": "Agent runtime error",
                    "evidence": [safe_line(str(exc))],
                    "recommended_action": "Inspect the monitoring analyzer script and rerun with a complete evidence bundle.",
                }
            ],
            "limits": ["Monitoring analyzer encountered an internal error during execution."],
            "llm": {
                "mode": args.llm_mode,
                "attempted": False,
                "used": False,
                "decision_changed": False,
                "model": None,
                "error": None,
            },
            "llm_insights": {
                "operator_summary": "",
                "rationale": [],
                "risk_notes": [],
                "follow_up_checks": [],
            },
        }

        out_path = Path(args.out)
        out_path.parent.mkdir(parents=True, exist_ok=True)
        out_path.write_text(json.dumps(fallback, indent=2) + "\n", encoding="utf-8")

        state_path = Path(args.state)
        state_path.parent.mkdir(parents=True, exist_ok=True)
        state_path.write_text(
            json.dumps(
                {
                    "timestamp_utc": utc_now_iso(),
                    "state": "degraded",
                    "health": {"http_status": None, "app_status": "UNKNOWN"},
                    "containers": {},
                    "signatures": {"agent_runtime_error": 1},
                },
                indent=2,
            )
            + "\n",
            encoding="utf-8",
        )

        if args.md_out:
            write_markdown(fallback, Path(args.md_out))

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
