#!/usr/bin/env python3
"""openrouter_generate_tests.py

Generate high-coverage Java tests for this workspace using OpenRouter.

This repository contains multiple folders (frontend, db, etc). For Java tests we
focus on the Spring Boot module in `facade/`.

The model is instructed to output STRICT JSON with a list of files.

Typical usage:

    export OPENROUTER_API_KEY='...'
    python3 openrouter_generate_tests.py --write

Optional:
    # also run mvn test after writing
    python3 openrouter_generate_tests.py --write --run-tests

    # log energy/CO2 with explicit assumptions
    python3 openrouter_generate_tests.py --write \
        --kwh-per-1k-tokens 0.0002 \
        --co2g-per-kwh 350

Notes on energy/CO2:
- There is no reliable “true” energy for a remote LLM call from the client.
    We therefore only provide an *estimation* based on token usage and user-
    supplied factors.
"""

from __future__ import annotations

import argparse
import base64
import json
import os
import sys
import time
import traceback
import urllib.error
import urllib.request
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterable, Optional


DEFAULT_MODEL = "openai/gpt-4.1-mini"
OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"


@dataclass(frozen=True)
class GeneratedFile:
    path: str
    content: str


@dataclass(frozen=True)
class PlannedFile:
    path: str
    purpose: str


def _read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace")


def _iter_files(base: Path, globs: Iterable[str]) -> Iterable[Path]:
    for pattern in globs:
        yield from base.glob(pattern)


def _load_facade_context(repo_root: Path) -> dict[str, str]:
    """Load all relevant code for the Spring Boot module in `facade/`.

    Keeping this set explicit prevents accidentally uploading large folders
    (e.g., node_modules, db/hsqldb sources, build artifacts).
    """

    module = repo_root / "facade"
    if not module.exists():
        raise FileNotFoundError("Expected Spring module at ./facade")

    include_globs = [
        "pom.xml",
        "src/main/resources/application.properties",
        "src/main/java/**/*.java",
        # Include existing tests (can help the LLM extend instead of rewriting).
        "src/test/java/**/*.java",
    ]

    context: dict[str, str] = {}
    for p in sorted({*list(_iter_files(module, include_globs))}):
        if p.is_dir():
            continue
        rel = p.relative_to(repo_root).as_posix()
        # Skip build outputs if present (safety).
        if "/target/" in rel or rel.startswith("facade/target/"):
            continue
        context[rel] = _read_text(p)

    if not context:
        raise FileNotFoundError("No facade context files found")

    return context


def _format_context_for_prompt(context: dict[str, str], *, max_chars: int) -> str:
    """Format as a single prompt string.

    We include file boundaries to keep the model anchored.
    If the project grows, max_chars prevents runaway payload sizes.
    """

    parts: list[str] = []
    used = 0

    for path, content in context.items():
        header = f"\n\n--- {path} ---\n"
        chunk = header + content
        if used + len(chunk) > max_chars:
            remaining = max_chars - used
            if remaining <= 0:
                break
            parts.append(chunk[:remaining] + "\n\n[TRUNCATED]\n")
            used = max_chars
            break
        parts.append(chunk)
        used += len(chunk)

    return "".join(parts).lstrip()


def _build_prompt(*, formatted_context: str) -> list[dict[str, str]]:
    system = (
        "You are a senior Java/Spring engineer specialized in testing and high coverage. "
        "Generate tests for a Spring Boot 3.x project. Use JUnit 5 (org.junit.jupiter.*) "
        "and Spring Boot test starter dependencies already present in the project.\n\n"
        "Return STRICT JSON ONLY. No markdown. No commentary.\n\n"
        "Schema:\n"
        "{\n"
        "  \"files\": [\n"
        "    {\"path\": \"facade/src/test/java/n7/facade/SomeTest.java\", \"content_b64\": \"...\"}\n"
        "  ]\n"
        "}\n\n"
        "Rules:\n"
        "- Each path MUST be under facade/src/test/java/ and end with .java\n"
        "- Use the correct Java package declaration matching the folder (e.g., package n7.facade;)\n"
        "- Prefer deterministic tests; avoid random/time-based flakiness\n"
        "- Aim for high line coverage and good assertions\n"
        "- Use MockMvc for controller endpoints when appropriate\n"
        "- Use @DataJpaTest for repository/entity persistence behavior where meaningful\n"
        "- Do not modify production code; only add/modify test files\n"
        "- Keep the number of test classes reasonable (quality > quantity)\n"
        "- IMPORTANT: Put the full Java file content in content_b64 as Base64 of UTF-8 bytes. Do NOT use a plain content field.\n"
    )

    user = (
        "Generate comprehensive JUnit 5 tests for the Spring Boot module `facade/`. "
        "Focus on controllers (HTTP endpoints), repositories, and key entity behaviors. "
        "Write tests that would run with `./mvnw test` from the facade directory.\n\n"
        "Output must be STRICT JSON and must be parseable by Python json.loads. "
        "Because Java code contains quotes/newlines, you MUST Base64-encode each file content.\n\n"
        "Example of one entry:\n"
        "{\"path\": \"facade/src/test/java/n7/facade/ExampleTest.java\", \"content_b64\": \"cGFja2FnZSBuNy5mYWNhZGU7XG4uLi4=\"}\n\n"
        "Here is the full relevant project context (files separated by markers):\n"
        + formatted_context
        + "\n"
    )

    return [{"role": "system", "content": system}, {"role": "user", "content": user}]


def _build_plan_prompt(*, formatted_context: str) -> list[dict[str, str]]:
    system = (
        "You are a senior Java/Spring engineer specialized in testing and high coverage.\n\n"
        "Return STRICT JSON ONLY. No markdown. No commentary.\n\n"
        "Schema:\n"
        "{\n"
        "  \"files\": [\n"
        "    {\"path\": \"facade/src/test/java/n7/facade/SomeTest.java\", \"purpose\": \"what this test covers\"}\n"
        "  ]\n"
        "}\n\n"
        "Rules:\n"
        "- Each path MUST be under facade/src/test/java/ and end with .java\n"
        "- Keep the number of files reasonable (avoid dozens)\n"
        "- Do NOT include test contents yet\n"
    )

    user = (
        "Propose a test suite plan for the Spring Boot module `facade/`. "
        "Aim for high coverage across controllers (HTTP endpoints), repositories, and important entity behaviors. "
        "Return only the JSON plan.\n\n"
        "Project context:\n"
        + formatted_context
        + "\n"
    )

    return [{"role": "system", "content": system}, {"role": "user", "content": user}]


def _build_single_file_prompt(*, formatted_context: str, target_path: str, purpose: str) -> list[dict[str, str]]:
    system = (
        "You are a senior Java/Spring engineer specialized in testing and high coverage. "
        "Generate exactly ONE Java test file requested by the user. Use JUnit 5 and Spring Boot test utilities already in the project.\n\n"
        "Return STRICT JSON ONLY. No markdown. No commentary.\n\n"
        "Schema:\n"
        "{\n"
        "  \"files\": [\n"
        "    {\"path\": \"facade/src/test/java/n7/facade/SomeTest.java\", \"content_b64\": \"...\"}\n"
        "  ]\n"
        "}\n\n"
        "Rules:\n"
        "- MUST output exactly one element in files[]\n"
        "- path MUST equal the requested path exactly\n"
        "- Put the full Java file content in content_b64 as Base64 of UTF-8 bytes\n"
        "- Do not invent non-existent imports\n"
    )

    user = (
        f"Generate the single test file at: {target_path}\n"
        f"Purpose: {purpose}\n\n"
        "Project context:\n"
        + formatted_context
        + "\n"
    )

    return [{"role": "system", "content": system}, {"role": "user", "content": user}]


@dataclass(frozen=True)
class OpenRouterResult:
    content: str
    raw: dict[str, Any]
    duration_s: float


def _call_openrouter(
    *, api_key: str, model: str, messages: list[dict[str, str]], timeout_s: int, max_tokens: int
) -> OpenRouterResult:
    payload = {
        "model": model,
        "messages": messages,
        "temperature": 0.2,
        "max_tokens": max_tokens,
    }

    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json",
        "HTTP-Referer": "http://localhost",
        "X-Title": "TestAI-Cercle-LLMULIKE",
    }

    request = urllib.request.Request(
        OPENROUTER_URL,
        data=json.dumps(payload).encode("utf-8"),
        headers=headers,
        method="POST",
    )

    started = time.perf_counter()
    try:
        with urllib.request.urlopen(request, timeout=timeout_s) as response:
            data = response.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"OpenRouter HTTP {e.code}: {body}") from e
    except urllib.error.URLError as e:
        raise RuntimeError(f"OpenRouter connection error: {e}") from e
    duration_s = time.perf_counter() - started

    parsed: dict[str, Any] = json.loads(data)
    content = parsed["choices"][0]["message"]["content"]
    return OpenRouterResult(content=content, raw=parsed, duration_s=duration_s)


def _extract_json(text: str) -> dict[str, Any]:
    text = text.strip()
    if text.startswith("{") and text.endswith("}"):
        return json.loads(text)

    start = text.find("{")
    end = text.rfind("}")
    if start != -1 and end != -1 and end > start:
        return json.loads(text[start : end + 1])

    raise ValueError("Model output is not valid JSON")


def _looks_truncated_json(text: str) -> bool:
    stripped = text.strip()
    if not stripped:
        return False
    return stripped.count("{") > stripped.count("}") or (stripped.count('"') % 2 == 1)


def _parse_plan(obj: dict[str, Any]) -> list[PlannedFile]:
    files_obj = obj.get("files")
    if not isinstance(files_obj, list) or not files_obj:
        raise ValueError("Plan JSON must contain non-empty 'files' array")

    planned: list[PlannedFile] = []
    for entry in files_obj:
        path = entry.get("path")
        purpose = entry.get("purpose")

        if not isinstance(path, str) or not path.startswith("facade/src/test/java/") or not path.endswith(".java"):
            raise ValueError(f"Invalid planned path: {path}")
        if ".." in Path(path).parts:
            raise ValueError(f"Invalid path traversal: {path}")
        if not isinstance(purpose, str) or not purpose.strip():
            purpose = "High coverage tests"

        planned.append(PlannedFile(path=path, purpose=purpose.strip()))

    return planned


def _validate_and_parse_files(obj: dict[str, Any]) -> list[GeneratedFile]:
    files_obj = obj.get("files")
    if not isinstance(files_obj, list) or not files_obj:
        raise ValueError("JSON must contain non-empty 'files' array")

    result: list[GeneratedFile] = []

    for entry in files_obj:
        path = entry.get("path")
        content = entry.get("content")
        content_b64 = entry.get("content_b64")

        if not path.startswith("facade/src/test/java/") or not path.endswith(".java"):
            raise ValueError(f"Invalid path: {path}")

        if ".." in Path(path).parts:
            raise ValueError(f"Invalid path traversal: {path}")

        if content_b64 is not None:
            if not isinstance(content_b64, str) or not content_b64.strip():
                raise ValueError(f"Missing/invalid content_b64 for: {path}")
            try:
                decoded = base64.b64decode(content_b64, validate=True).decode("utf-8", errors="strict")
            except Exception as e:
                raise ValueError(f"Invalid base64 content for: {path}: {e}") from e
            result.append(GeneratedFile(path=path, content=decoded))
            continue

        # Backwards compatibility (older prompt). Strongly prefer content_b64.
        if content is None:
            raise ValueError(f"Missing content/content_b64 for: {path}")
        if not isinstance(content, str):
            raise ValueError(f"Invalid content for: {path}")
        result.append(GeneratedFile(path=path, content=content))

    return result


def _write_files(project_root: Path, files: list[GeneratedFile]) -> None:
    for f in files:
        out_path = project_root / f.path
        out_path.parent.mkdir(parents=True, exist_ok=True)
        out_path.write_text(f.content, encoding="utf-8")


def _iso_utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()


def _write_text(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def _estimate_tokens_from_text(text: str) -> int:
    # Very rough heuristic when API doesn't return usage.
    return max(1, len(text) // 4)


def _compute_energy_and_co2(*, total_tokens: Optional[int], kwh_per_1k_tokens: Optional[float], co2g_per_kwh: Optional[float]) -> dict[str, Optional[float]]:
    if total_tokens is None or kwh_per_1k_tokens is None or co2g_per_kwh is None:
        return {"estimated_energy_kwh": None, "estimated_co2_g": None}
    energy_kwh = (total_tokens / 1000.0) * kwh_per_1k_tokens
    co2_g = energy_kwh * co2g_per_kwh
    return {"estimated_energy_kwh": energy_kwh, "estimated_co2_g": co2_g}


def _run_mvn_tests(repo_root: Path) -> int:
    import subprocess

    proc = subprocess.run(
        ["./mvnw", "test"],
        cwd=str(repo_root / "facade"),
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        check=False,
    )
    sys.stdout.write(proc.stdout)
    return proc.returncode


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--timeout", type=int, default=120)
    parser.add_argument("--model", default=DEFAULT_MODEL)
    parser.add_argument(
        "--mode",
        choices=["single", "per-file"],
        default="per-file",
        help="per-file avoids truncated JSON by generating one test file per API call",
    )
    parser.add_argument("--max-tokens", type=int, default=6000, help="max_tokens for OpenRouter per call")
    parser.add_argument("--write", action="store_true", help="Write generated files into the workspace")
    parser.add_argument("--json-out", default="openrouter_generated_tests.json", help="Where to write the generated JSON archive (use - to skip)")
    parser.add_argument("--log-jsonl", default="openrouter_runs.jsonl", help="Append per-run stats as JSON lines (use - to skip)")
    parser.add_argument("--raw-response-out", default="openrouter_last_response.txt", help="Write the raw model response text here (use - to skip)")
    parser.add_argument("--max-context-chars", type=int, default=200_000, help="Max characters of repo context to include in the prompt")
    parser.add_argument("--run-tests", action="store_true", help="Run ./mvnw test in facade after writing")
    parser.add_argument("--kwh-per-1k-tokens", type=float, default=None, help="Energy estimation factor (kWh per 1000 tokens). Optional.")
    parser.add_argument("--co2g-per-kwh", type=float, default=None, help="Carbon intensity (grams CO2 per kWh). Optional.")
    args = parser.parse_args()

    api_key = os.environ.get("OPENROUTER_API_KEY", "").strip()
    if not api_key:
        print("ERROR: Missing OPENROUTER_API_KEY environment variable.", file=sys.stderr)
        print("Example: export OPENROUTER_API_KEY='sk-or-v1-...'", file=sys.stderr)
        return 2

    repo_root = Path(__file__).resolve().parent

    # 1) Load code context
    context = _load_facade_context(repo_root)
    formatted_context = _format_context_for_prompt(context, max_chars=args.max_context_chars)

    overall_start = time.perf_counter()
    call_summaries: list[dict[str, Any]] = []

    def _save_raw(suffix: str, content: str) -> None:
        if args.raw_response_out == "-":
            return
        base = Path(args.raw_response_out)
        out_name = f"{base.stem}_{suffix}{base.suffix}"
        _write_text(repo_root / out_name, content)

    generated_files: list[GeneratedFile] = []

    if args.mode == "single":
        messages = _build_prompt(formatted_context=formatted_context)
        result = _call_openrouter(
            api_key=api_key,
            model=args.model,
            messages=messages,
            timeout_s=args.timeout,
            max_tokens=args.max_tokens,
        )
        call_summaries.append({"kind": "single", "duration_s": result.duration_s})
        _save_raw("single", result.content)

        try:
            obj = _extract_json(result.content)
        except Exception as e:
            print("ERROR: Failed to parse model response as JSON.", file=sys.stderr)
            if _looks_truncated_json(result.content):
                print("The response looks truncated. Use --mode per-file.", file=sys.stderr)
            print(f"Reason: {e}", file=sys.stderr)
            print(traceback.format_exc(), file=sys.stderr)
            return 3
        generated_files = _validate_and_parse_files(obj)

    else:
        # Plan first
        plan_messages = _build_plan_prompt(formatted_context=formatted_context)
        plan_result = _call_openrouter(
            api_key=api_key,
            model=args.model,
            messages=plan_messages,
            timeout_s=args.timeout,
            max_tokens=min(args.max_tokens, 2000),
        )
        call_summaries.append({"kind": "plan", "duration_s": plan_result.duration_s})
        _save_raw("plan", plan_result.content)

        try:
            plan_obj = _extract_json(plan_result.content)
            planned = _parse_plan(plan_obj)
        except Exception as e:
            print("ERROR: Failed to parse plan JSON.", file=sys.stderr)
            print(f"Reason: {e}", file=sys.stderr)
            print(traceback.format_exc(), file=sys.stderr)
            return 3

        # Generate each file separately (small JSON each time)
        for idx, pf in enumerate(planned, start=1):
            file_messages = _build_single_file_prompt(
                formatted_context=formatted_context,
                target_path=pf.path,
                purpose=pf.purpose,
            )
            file_result = _call_openrouter(
                api_key=api_key,
                model=args.model,
                messages=file_messages,
                timeout_s=args.timeout,
                max_tokens=args.max_tokens,
            )
            call_summaries.append({"kind": "file", "path": pf.path, "duration_s": file_result.duration_s})
            _save_raw(f"file_{idx:02d}", file_result.content)

            try:
                file_obj = _extract_json(file_result.content)
            except Exception as e:
                print(f"ERROR: Failed to parse JSON for {pf.path}", file=sys.stderr)
                if _looks_truncated_json(file_result.content):
                    print("The response looks truncated. Try increasing --max-tokens.", file=sys.stderr)
                print(f"Reason: {e}", file=sys.stderr)
                print(traceback.format_exc(), file=sys.stderr)
                return 3

            files = _validate_and_parse_files(file_obj)
            if len(files) != 1 or files[0].path != pf.path:
                print("ERROR: Model returned unexpected file set (expected exactly 1 file at the requested path).", file=sys.stderr)
                return 3

            generated_files.append(files[0])
            if args.write:
                _write_files(repo_root, [files[0]])

    overall_duration_s = time.perf_counter() - overall_start

    archive_obj = {"files": [f.__dict__ for f in generated_files]}

    print(json.dumps(archive_obj, ensure_ascii=False, indent=2))

    if args.json_out != "-":
        out_path = repo_root / args.json_out
        out_path.write_text(
            json.dumps(archive_obj, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )

    if args.write:
        if args.mode == "single":
            _write_files(repo_root, generated_files)

    # 4) Structured logging (duration, tokens, optional energy/CO2)
    # Token usage is not reliably available across providers; estimate to keep KPIs comparable.
    prompt_tokens = None
    completion_tokens = None
    total_tokens = _estimate_tokens_from_text(formatted_context) + sum(_estimate_tokens_from_text(f.content) for f in generated_files)

    kpi = _compute_energy_and_co2(
        total_tokens=total_tokens,
        kwh_per_1k_tokens=args.kwh_per_1k_tokens,
        co2g_per_kwh=args.co2g_per_kwh,
    )

    run_log = {
        "ts_utc": _iso_utc_now(),
        "model": args.model,
        "timeout_s": args.timeout,
        "mode": args.mode,
        "max_tokens": args.max_tokens,
        "overall_duration_s": overall_duration_s,
        "calls": call_summaries,
        "context_files": len(context),
        "context_chars": len(formatted_context),
        "response_chars": sum(len(f.content) for f in generated_files),
        "usage": {
            "prompt_tokens": prompt_tokens,
            "completion_tokens": completion_tokens,
            "total_tokens": total_tokens,
        },
        "energy": {
            "kwh_per_1k_tokens": args.kwh_per_1k_tokens,
            "co2g_per_kwh": args.co2g_per_kwh,
            **kpi,
        },
        "generated_files": [asdict(f) for f in generated_files],
    }

    if args.log_jsonl != "-":
        log_path = repo_root / args.log_jsonl
        log_path.parent.mkdir(parents=True, exist_ok=True)
        with log_path.open("a", encoding="utf-8") as f:
            f.write(json.dumps(run_log, ensure_ascii=False) + "\n")

    # Also print a short run summary to stderr (keeps stdout as the JSON archive).
    print(
        f"[openrouter] model={args.model} mode={args.mode} duration={overall_duration_s:.2f}s est_tokens={total_tokens} files={len(generated_files)}",
        file=sys.stderr,
    )

    if args.write and args.run_tests:
        return _run_mvn_tests(repo_root)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())