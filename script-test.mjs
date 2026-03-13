#!/usr/bin/env node
/**
 * One-shot AI test generation:
 * - finds changed production .java files vs base branch (main or PR base)
 * - ALSO finds their internal transitive dependencies (jdeps via Scriptdepandance)
 * - sends full content (changed + deps) to OpenRouter
 * - writes generated tests ONLY under Projet/src/test/java/
 *
 * Safety:
 * - Only writes files under Projet/src/test/java/
 * - Only writes filenames ending with Test.java or Tests.java
 * - If file exists: overwrite ONLY if safe (existing content preserved 100%; new tests may be inserted before final class brace)
 */

import { execSync, spawnSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import process from "node:process";

/** Repo root */
const REPO_ROOT = process.cwd();

function detectMavenProjectDirName() {
  const fromEnv = String(process.env.AI_MAVEN_PROJECT_DIR || "").trim();
  const candidates = [
    fromEnv || null,
    "facade",
    "Projet",
    null, // repo root
  ];

  for (const name of candidates) {
    if (name === "") continue;
    const dirAbs = name ? path.join(REPO_ROOT, name) : REPO_ROOT;
    if (fileExists(path.join(dirAbs, "pom.xml"))) return name || "";
  }

  // Fall back to historical default to keep behavior stable.
  return "Projet";
}

/** Maven project folder (auto-detected; prefer facade/) */
const PROJECT_DIRNAME = detectMavenProjectDirName();
const PROJECT_DIR = PROJECT_DIRNAME ? path.join(REPO_ROOT, PROJECT_DIRNAME) : REPO_ROOT;

/** Repo-relative prefix like "facade/" or "" */
const PROJECT_PREFIX = PROJECT_DIRNAME ? `${PROJECT_DIRNAME}/` : "";
const MAIN_JAVA_ROOT = `${PROJECT_PREFIX}src/main/java/`;
const TEST_JAVA_ROOT = `${PROJECT_PREFIX}src/test/java/`;

/** LLM configuration */
const apiKey = process.env.LLM_API_KEY;
//const model = process.env.OPENROUTER_MODEL || "gpt-4o-mini";
const model = "gpt-4.1-mini";
const AI_ENABLED = Boolean(apiKey);

/** Context controls */
// When enabled, we include existing tests as context for the model.
// Disable (set to "0") if you want tests generated solely from production code.
const INCLUDE_EXISTING_TESTS_CONTEXT =
  (process.env.AI_INCLUDE_EXISTING_TESTS_CONTEXT || "1") !== "0";

/** Repair loop configuration */
const REPAIR_ENABLED = (process.env.AI_TEST_REPAIR_ENABLED || "1") !== "0";
const MAX_REPAIR_ITERS = Math.max(
  0,
  Number.parseInt(process.env.AI_TEST_REPAIR_MAX_ITERS || "2", 10) || 0
);
const REPAIR_STRICT = (process.env.AI_TEST_REPAIR_STRICT || "0") === "1";

/** Execute shell and return stdout string */
function sh(cmd, opts = {}) {
  return execSync(cmd, {
    stdio: ["ignore", "pipe", "pipe"],
    encoding: "utf8",
    ...opts,
  }).trim();
}

function shAllowFail(cmd, opts = {}) {
  try {
    return { ok: true, stdout: sh(cmd, opts) };
  } catch (e) {
    return { ok: false, stdout: "", error: e };
  }
}

/** Exists */
function fileExists(p) {
  try {
    fs.accessSync(p);
    return true;
  } catch {
    return false;
  }
}

/** Ensure dir */
function ensureDir(p) {
  fs.mkdirSync(p, { recursive: true });
}

/** Write safe */
function writeFileSafe(p, content) {
  ensureDir(path.dirname(p));
  fs.writeFileSync(p, content, "utf8");
}

/** Best-effort rm -rf */
function rmrf(p) {
  try {
    fs.rmSync(p, { recursive: true, force: true });
  } catch {
    // ignore
  }
}

/** Read JSON */
function readJson(p) {
  return JSON.parse(fs.readFileSync(p, "utf8"));
}

/** Safety: test filename */
function isLikelyJavaTestFilename(p) {
  return /Test\.java$/.test(p) || /Tests\.java$/.test(p);
}

/** Run Maven tests (Surefire). Returns { ok, exitCode, stdout, stderr } */
function runMavenTests({ testFilter } = {}) {
  const mvnw = fileExists(path.join(PROJECT_DIR, "mvnw")) ? "./mvnw" : "mvn";
  const args = ["-q", "test"];
  if (testFilter && String(testFilter).trim()) {
    // Surefire supports -Dtest=ClassName,OtherTest or Class#method (depending on plugin/version)
    args.splice(1, 0, `-Dtest=${String(testFilter).trim()}`);
  }

  const res = spawnSync(mvnw, args, {
    cwd: PROJECT_DIR,
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
  });

  return {
    ok: res.status === 0,
    exitCode: typeof res.status === "number" ? res.status : 1,
    stdout: res.stdout || "",
    stderr: res.stderr || "",
  };
}

/** Parse failing testcases from Maven Surefire XML reports */
function parseSurefireFailures() {
  const reportsDir = path.join(PROJECT_DIR, "target", "surefire-reports");
  if (!fileExists(reportsDir)) return [];

  const xmlFiles = fs
    .readdirSync(reportsDir)
    .filter((f) => f.startsWith("TEST-") && f.endsWith(".xml"));

  const failures = [];

  for (const file of xmlFiles) {
    const xml = fs.readFileSync(path.join(reportsDir, file), "utf8");

    let idx = 0;
    while (true) {
      const start = xml.indexOf("<testcase", idx);
      if (start === -1) break;
      const tagEnd = xml.indexOf(">", start);
      if (tagEnd === -1) break;
      const openTag = xml.slice(start, tagEnd + 1);

      const isSelfClosing = /\/>\s*$/.test(openTag);
      const nameMatch = openTag.match(/\bname\s*=\s*"([^"]+)"/);
      const classMatch = openTag.match(/\bclassname\s*=\s*"([^"]+)"/);
      const testName = nameMatch?.[1];
      const className = classMatch?.[1];

      if (!testName || !className) {
        idx = tagEnd + 1;
        continue;
      }

      if (isSelfClosing) {
        idx = tagEnd + 1;
        continue;
      }

      const close = xml.indexOf("</testcase>", tagEnd + 1);
      if (close === -1) break;
      const inner = xml.slice(tagEnd + 1, close);

      const failureMatch = inner.match(/<(failure|error)\b[^>]*>([\s\S]*?)<\/(failure|error)>/);
      if (failureMatch) {
        const kind = failureMatch[1];
        const payload = (failureMatch[2] || "").trim();
        const messageMatch = inner.match(/<(failure|error)\b[^>]*\bmessage\s*=\s*"([^"]*)"/);
        const message = messageMatch?.[2] || "";
        failures.push({
          className,
          testName,
          kind,
          message,
          details: payload,
        });
      }

      idx = close + "</testcase>".length;
    }
  }

  return failures;
}

function testClassPathFromClassName(className) {
  return `${TEST_JAVA_ROOT}${className.replace(/\./g, "/")}.java`;
}

function normalizePathSlashes(p) {
  return String(p || "").replace(/\\/g, "/");
}

function relPathFromAbsJavaPath(absPath) {
  const absNorm = normalizePathSlashes(absPath);
  const rootNorm = normalizePathSlashes(REPO_ROOT);
  if (absNorm.startsWith(rootNorm + "/")) return absNorm.slice(rootNorm.length + 1);
  return absNorm;
}

function testFqcnFromTestFileContent({ relTestPathFromRepo, content }) {
  const pkg = getJavaPackageName(content);
  const cls = path.basename(relTestPathFromRepo, ".java");
  return pkg ? `${pkg}.${cls}` : cls;
}

function findEnclosingJavaMethodNameByLine(content, lineNumber1Based) {
  const lines = String(content).split(/\r?\n/);
  let i = Math.min(Math.max(0, Number(lineNumber1Based) - 1), Math.max(0, lines.length - 1));

  // Walk upwards until we find a method signature.
  for (; i >= 0; i--) {
    const line = String(lines[i] || "");
    // Skip obvious noise.
    if (/^\s*\/?\//.test(line)) continue;
    if (/^\s*\*/.test(line)) continue;

    // Try to match a Java method signature line and capture the method name.
    const m = line.match(
      /\b(?:public|protected|private)?\s*(?:static\s+)?(?:final\s+)?(?:synchronized\s+)?[\w<>\[\]]+\s+([A-Za-z_][A-Za-z0-9_]*)\s*\(/ 
    );
    if (m && m[1] && m[1] !== "class" && m[1] !== "interface") return m[1];
  }

  return null;
}

/** Parse Maven compiler errors from output. Returns [{ path, line, message }] (path repo-relative when possible). */
function parseMavenCompilationErrors(outputText) {
  const text = String(outputText || "");
  const lines = text.split(/\r?\n/);
  const out = [];

  // Maven/javac typical formats:
  // [ERROR] /home/.../Projet/src/test/java/.../FooTest.java:[44,23] cannot find symbol
  // [ERROR] /home/.../FooTest.java:[44,23] ...
  const re = /^\[ERROR\]\s+(.+?\.java):\[(\d+),(\d+)\]\s*(.*)$/;

  for (const line of lines) {
    const m = String(line).match(re);
    if (!m) continue;
    const absPath = m[1];
    const lineNo = Number.parseInt(m[2], 10);
    const msg = (m[4] || "").trim();
    if (!Number.isFinite(lineNo) || lineNo <= 0) continue;

    const rel = relPathFromAbsJavaPath(absPath);
    out.push({ path: rel, line: lineNo, message: msg });
  }

  return out;
}

function simpleClassName(fqcn) {
  const parts = String(fqcn).split(".");
  return parts[parts.length - 1] || fqcn;
}

function testFilterFromTestPaths(testPaths) {
  // Convert `<module>/src/test/java/.../FooTest.java` -> `FooTest`
  const names = [];
  for (const p of testPaths || []) {
    if (typeof p !== "string") continue;
    if (!p.startsWith(TEST_JAVA_ROOT)) continue;
    if (!p.endsWith(".java")) continue;
    names.push(path.basename(p, ".java"));
  }
  return Array.from(new Set(names)).filter(Boolean).join(",");
}

function detectEol(s) {
  return String(s).includes("\r\n") ? "\r\n" : "\n";
}

function isTestAnnotationLine(line) {
  // Allow trailing whitespace and line comments.
  return /^\s*@(?:org\.junit\.jupiter\.api\.)?Test\s*(?:(?:\/\/).*?)?$/.test(String(line));
}

function dedupeTestAnnotationsPerAnnotatedElement(text, eol = "\n") {
  // Dedupe @Test inside a contiguous annotation block (even if separated by blank lines
  // or other annotations). Reset once we hit a non-annotation, non-blank line.
  const lines = String(text).split(/\r?\n/);
  const out = [];
  let inAnnotationBlock = false;
  let seenTestInBlock = false;

  for (const line of lines) {
    const trimmed = String(line).trim();

    if (trimmed.startsWith("@")) {
      inAnnotationBlock = true;
      if (isTestAnnotationLine(trimmed)) {
        if (seenTestInBlock) continue;
        seenTestInBlock = true;
      }
      out.push(line);
      continue;
    }

    if (trimmed === "") {
      // Blank lines don't end the annotation block (whitespace is allowed between annotations).
      out.push(line);
      continue;
    }

    // Any other line ends the current annotation block.
    inAnnotationBlock = false;
    seenTestInBlock = false;
    out.push(line);
  }

  return out.join(eol);
}

function sanitizeJavaMethodCode(methodCode, eol = "\n") {
  // Conservative: only fix patterns that break compilation.
  return dedupeTestAnnotationsPerAnnotatedElement(methodCode, eol);
}

/** Find the range of a Java method block by name (best-effort). Returns { start, end } indices */
function findJavaMethodRange(content, methodName) {
  const name = String(methodName);
  if (!name) return null;

  // Prefer matching a void method with that name.
  const patterns = [
    new RegExp(`(^|\\n)\\s*(?:public|protected|private)?\\s*(?:static\\s+)?void\\s+${name}\\s*\\(`, "m"),
    new RegExp(`(^|\\n)\\s*(?:public|protected|private)?\\s*(?:static\\s+)?[\\w<>\\[\\]]+\\s+${name}\\s*\\(`, "m"),
  ];

  let matchIndex = -1;
  let matchLen = 0;
  for (const re of patterns) {
    const m = content.match(re);
    if (m && typeof m.index === "number") {
      matchIndex = m.index + (m[1] ? m[1].length : 0);
      matchLen = m[0].length;
      break;
    }
  }

  if (matchIndex === -1) return null;

  // Expand start upward to include contiguous annotations directly above the method.
  // This avoids leaving the old @Test in place when the replacement includes @Test.
  let startIndex = matchIndex;
  let cursor = matchIndex;
  while (cursor > 0) {
    const prevLineBreak = content.lastIndexOf("\n", cursor - 1);
    if (prevLineBreak === -1) break;
    const prevPrevLineBreak = content.lastIndexOf("\n", prevLineBreak - 1);
    const lineStart = prevPrevLineBreak === -1 ? 0 : prevPrevLineBreak + 1;
    const prevLine = content.slice(lineStart, prevLineBreak);

    // Stop if there's a blank line between annotations and method.
    if (/^\s*$/.test(prevLine)) break;

    if (/^\s*@/.test(prevLine)) {
      startIndex = lineStart;
      cursor = lineStart;
      continue;
    }

    break;
  }

  // Find first '{' after signature.
  const braceOpen = content.indexOf("{", matchIndex + matchLen);
  if (braceOpen === -1) return null;

  let depth = 0;
  for (let i = braceOpen; i < content.length; i++) {
    const ch = content[i];
    if (ch === "{") depth++;
    else if (ch === "}") {
      depth--;
      if (depth === 0) {
        // include trailing newline if present
        let end = i + 1;
        if (content[end] === "\r" && content[end + 1] === "\n") end += 2;
        else if (content[end] === "\n") end += 1;
        return { start: startIndex, end };
      }
    }
  }

  return null;
}

function insertBeforeFinalBrace(oldContent, insertContent) {
  const trimmed = oldContent.replace(/\s+$/g, "");
  const lastBrace = trimmed.lastIndexOf("}");
  if (lastBrace === -1 || lastBrace !== trimmed.length - 1) return null;
  const prefix = trimmed.slice(0, lastBrace);
  const insertion = String(insertContent).replace(/^\s+/, "\n");
  return `${prefix}${insertion}\n}` + (oldContent.endsWith("\n") ? "\n" : "");
}

/** Apply a targeted replacement of one method in a Java test file */
function applyMethodReplacement({ filePathAbs, methodName, newMethodCode }) {
  const oldContent = fs.readFileSync(filePathAbs, "utf8");
  const eol = detectEol(oldContent);
  const sanitizedMethodCode = sanitizeJavaMethodCode(newMethodCode, eol);
  const range = findJavaMethodRange(oldContent, methodName);
  if (!range) {
    const appended = insertBeforeFinalBrace(oldContent, sanitizedMethodCode);
    if (!appended) {
      throw new Error(`Could not find method '${methodName}' and could not append safely.`);
    }
    fs.writeFileSync(filePathAbs, appended, "utf8");
    return { changed: true, mode: "append" };
  }

  const before = oldContent.slice(0, range.start);
  const after = oldContent.slice(range.end);
  const updated = `${before}${sanitizedMethodCode.trimEnd()}${eol}${after}`;
  fs.writeFileSync(filePathAbs, updated, "utf8");
  return { changed: true, mode: "replace" };
}

/** Append-only guard */
function isAppendOnlyUpdate(oldContent, newContent) {
  const oldTrimRight = oldContent.replace(/\s+$/g, "");
  const newTrimRight = newContent.replace(/\s+$/g, "");

  // Strict append-only (rarely valid for Java class files, but keep it).
  if (newTrimRight.startsWith(oldTrimRight)) return true;

  // Java-friendly mode: allow inserting new content just BEFORE the final closing brace.
  // This preserves every existing line and only moves the last '}' down.
  const oldLastBrace = oldTrimRight.lastIndexOf("}");
  const newLastBrace = newTrimRight.lastIndexOf("}");
  if (oldLastBrace === -1 || newLastBrace === -1) return false;

  // Old file must end with its final brace (after trim-right)
  if (oldLastBrace !== oldTrimRight.length - 1) return false;
  // New file must end with its final brace too
  if (newLastBrace !== newTrimRight.length - 1) return false;

  const oldPrefixBeforeFinalBrace = oldTrimRight.slice(0, oldLastBrace);
  return newTrimRight.startsWith(oldPrefixBeforeFinalBrace);
}

function buildAppendOnlyUpdateWithSanitizedInsertion({ oldContent, newContent }) {
  const oldTrimRight = String(oldContent).replace(/\s+$/g, "");
  const newTrimRight = String(newContent).replace(/\s+$/g, "");

  // Only works for the "insert before final brace" append-only mode.
  const oldLastBrace = oldTrimRight.lastIndexOf("}");
  const newLastBrace = newTrimRight.lastIndexOf("}");
  if (oldLastBrace === -1 || newLastBrace === -1) return null;
  if (oldLastBrace !== oldTrimRight.length - 1) return null;
  if (newLastBrace !== newTrimRight.length - 1) return null;

  const prefixBeforeFinalBrace = oldTrimRight.slice(0, oldLastBrace);
  if (!newTrimRight.startsWith(prefixBeforeFinalBrace)) return null;

  const insertion = newTrimRight.slice(prefixBeforeFinalBrace.length, newLastBrace);
  const eol = detectEol(oldContent);
  const sanitizedInsertion = dedupeTestAnnotationsPerAnnotatedElement(insertion, eol);

  let rebuilt = `${prefixBeforeFinalBrace}${sanitizedInsertion}`;
  if (!rebuilt.endsWith(eol)) rebuilt += eol;
  rebuilt += "}";
  if (String(newContent).endsWith(eol)) rebuilt += eol;
  return rebuilt;
}

/** Determine Git diff context */
function getEventContext() {
  const eventPath = process.env.GITHUB_EVENT_PATH;

  if (!eventPath || !fileExists(eventPath)) {
    return { baseBranch: "main", baseSha: "origin/main", headSha: "HEAD" };
  }

  const ev = readJson(eventPath);

  if (ev.before && ev.after) {
    return { baseBranch: "main", baseSha: ev.before, headSha: ev.after };
  }

  if (ev.pull_request?.base?.sha && ev.pull_request?.head?.sha) {
    return {
      baseBranch: ev.pull_request.base.ref || "main",
      baseSha: ev.pull_request.base.sha,
      headSha: ev.pull_request.head.sha,
    };
  }

  return { baseBranch: "main", baseSha: "origin/main", headSha: "HEAD" };
}

function getDiffNumstat(base, head, relPathFromRepo) {
  const out = shAllowFail(`git diff --numstat ${base}..${head} -- "${relPathFromRepo}"`, {
    cwd: REPO_ROOT,
  });
  if (!out.ok || !out.stdout) return { additions: null, deletions: null };
  const line = out.stdout.split("\n").map((s) => s.trim()).filter(Boolean)[0];
  if (!line) return { additions: null, deletions: null };
  const parts = line.split(/\s+/);
  const additions = parts[0] === "-" ? null : Number.parseInt(parts[0], 10);
  const deletions = parts[1] === "-" ? null : Number.parseInt(parts[1], 10);
  return {
    additions: Number.isFinite(additions) ? additions : null,
    deletions: Number.isFinite(deletions) ? deletions : null,
  };
}

function getJavaPackageName(sourceContent) {
  const m = String(sourceContent).match(/^\s*package\s+([a-zA-Z0-9_.]+)\s*;\s*$/m);
  return m?.[1] || null;
}

function prodFqcnFromProdPath(prodRelPathFromRepo) {
  const abs = path.join(REPO_ROOT, prodRelPathFromRepo);
  if (!fileExists(abs)) return null;
  const content = fs.readFileSync(abs, "utf8");
  const pkg = getJavaPackageName(content);
  const cls = path.basename(prodRelPathFromRepo, ".java");
  if (!pkg) return cls;
  return `${pkg}.${cls}`;
}

function findDirectDependentProdFilesByImport(fqcn) {
  if (!fqcn) return [];

  const parts = String(fqcn).split(".");
  const className = parts[parts.length - 1];
  const packageName = parts.length > 1 ? parts.slice(0, -1).join(".") : null;

  const importNeedle = `import ${fqcn};`;
  const grep1 = shAllowFail(`git grep -l -F "${importNeedle}" -- "${MAIN_JAVA_ROOT}"`, {
    cwd: REPO_ROOT,
  });
  const grep2 = shAllowFail(`git grep -l -F "${fqcn}" -- "${MAIN_JAVA_ROOT}"`, {
    cwd: REPO_ROOT,
  });

  // Same-package references often omit imports; catch `SoustractionIA` mentions in the same package.
  const grep3 = className
    ? shAllowFail(`git grep -l -w "${className}" -- "${MAIN_JAVA_ROOT}"`, {
        cwd: REPO_ROOT,
      })
    : { ok: true, stdout: "" };

  const out = new Set();
  for (const g of [grep1, grep2]) {
    if (!g.ok || !g.stdout) continue;
    for (const line of g.stdout.split("\n")) {
      const p = line.trim();
      if (!p) continue;
      if (!p.startsWith(PROJECT_PREFIX)) continue;
      if (!p.includes("src/main/java/")) continue;
      if (!p.endsWith(".java")) continue;
      out.add(p);
    }
  }

  // Filter grep3 results by package equality to reduce noise.
  if (grep3.ok && grep3.stdout && packageName) {
    for (const line of grep3.stdout.split("\n")) {
      const p = line.trim();
      if (!p) continue;
      if (!p.startsWith(PROJECT_PREFIX)) continue;
      if (!p.includes("src/main/java/")) continue;
      if (!p.endsWith(".java")) continue;

      const abs = path.join(REPO_ROOT, p);
      if (!fileExists(abs)) continue;
      const content = fs.readFileSync(abs, "utf8");
      const pkg = getJavaPackageName(content);
      if (pkg && pkg === packageName) out.add(p);
    }
  }

  return Array.from(out);
}

function testsForProdPath(prodRelPathFromRepo) {
  const out = new Set();
  const className = path.basename(prodRelPathFromRepo, ".java");

  // 1) naming convention guesses
  const baseCandidates = [];
  if (prodRelPathFromRepo.startsWith(MAIN_JAVA_ROOT)) {
    const testDir = path
      .dirname(prodRelPathFromRepo)
      .replace(MAIN_JAVA_ROOT, TEST_JAVA_ROOT);
    baseCandidates.push(
      path.join(testDir, `${className}Test.java`),
      path.join(testDir, `${className}Tests.java`),
      path.join(testDir, `${className}UnitTest.java`),
      path.join(testDir, `${className}IT.java`),
      path.join(testDir, `${className}IATest.java`),
      path.join(testDir, `${className}IntegrationTest.java`)
    );
  }
  for (const c of baseCandidates) {
    const p = String(c).replace(/\\/g, "/");
    if (fileExists(path.join(REPO_ROOT, p))) out.add(p);
  }

  // 2) grep tests importing the class
  const fqcn = prodFqcnFromProdPath(prodRelPathFromRepo);
  if (fqcn) {
    const importNeedle = `import ${fqcn};`;
    const g1 = shAllowFail(`git grep -l -F "${importNeedle}" -- "${TEST_JAVA_ROOT}"`, {
      cwd: REPO_ROOT,
    });
    const g2 = shAllowFail(`git grep -l -F "${fqcn}" -- "${TEST_JAVA_ROOT}"`, { cwd: REPO_ROOT });
    for (const g of [g1, g2]) {
      if (!g.ok || !g.stdout) continue;
      for (const line of g.stdout.split("\n")) {
        const p = line.trim();
        if (!p) continue;
        if (!p.startsWith(TEST_JAVA_ROOT)) continue;
        if (!p.endsWith(".java")) continue;
        out.add(p);
      }
    }
  }

  // 3) last resort: simple class name mention (can be noisy, so only include if uniquely small)
  if (out.size === 0) {
    const g = shAllowFail(`git grep -l -w "${className}" -- "${TEST_JAVA_ROOT}"`, { cwd: REPO_ROOT });
    if (g.ok && g.stdout) {
      const lines = g.stdout
        .split("\n")
        .map((s) => s.trim())
        .filter(Boolean)
        .filter((p) => p.startsWith(TEST_JAVA_ROOT) && p.endsWith(".java"));
      // Avoid exploding selection for common names
      if (lines.length > 0 && lines.length <= 10) {
        for (const p of lines) out.add(p);
      }
    }
  }

  return Array.from(out);
}

function computeImpactedTestPaths({ changedProdPaths, baseSha, headSha }) {
  const impactedProd = new Set();

  for (const prod of changedProdPaths) {
    const stat = getDiffNumstat(baseSha, headSha, prod);
    const isAppendOnly = stat.deletions === 0 && (stat.additions === null || stat.additions > 0);

    impactedProd.add(prod);

    // Only in modification case do we add dependents.
    if (!isAppendOnly) {
      const fqcn = prodFqcnFromProdPath(prod);
      const dependents = findDirectDependentProdFilesByImport(fqcn);
      for (const d of dependents) {
        if (d !== prod) impactedProd.add(d);
      }
    }
  }

  const impactedTests = new Set();
  for (const prod of impactedProd) {
    for (const t of testsForProdPath(prod)) impactedTests.add(t);
  }

  const testPaths = Array.from(impactedTests).sort();
  const testFilter = testFilterFromTestPaths(testPaths);

  const impactedProdPaths = Array.from(impactedProd).sort();
  const impactedProdClasses = impactedProdPaths
    .map((p) => prodFqcnFromProdPath(p))
    .filter(Boolean);

  return { testPaths, testFilter, impactedProdPaths, impactedProdClasses };
}

/** Fetch base branch */
function fetchBaseBranch(baseBranch) {
  sh(
    `git fetch --no-tags --prune origin +refs/heads/${baseBranch}:refs/remotes/origin/${baseBranch}`
  );
}

/**
 * List changed production Java files
 * - under the detected Maven module folder (e.g. facade/)
 * - only src/main/java
 * - exclude src/test/java
 */
function listChangedJavaFiles(base, head) {
  const diff = sh(`git diff --name-only ${base}..${head} -- "*.java"`, {
    cwd: REPO_ROOT,
  });

  if (!diff) return [];

  return diff
    .split("\n")
    .map((s) => s.trim())
    .filter(Boolean)
    .filter((p) => p.startsWith(PROJECT_PREFIX))
    .filter((p) => p.includes("src/main/java/"))
    .filter((p) => !p.includes("src/test/java/"));
}

/**
 * Given a production Java file path like:
 *   <module>/src/main/java/.../Foo.java
 * return likely existing test file paths:
 *   <module>/src/test/java/.../FooTest.java
 *   <module>/src/test/java/.../FooTests.java
 */
function getLikelyExistingTestPathsForProductionPath(prodRelPathFromRepo) {
  if (!prodRelPathFromRepo.startsWith(MAIN_JAVA_ROOT)) return [];
  if (!prodRelPathFromRepo.endsWith(".java")) return [];

  const testDir = path
    .dirname(prodRelPathFromRepo)
    .replace(MAIN_JAVA_ROOT, TEST_JAVA_ROOT);
  const className = path.basename(prodRelPathFromRepo, ".java");

  return [
    path.join(testDir, `${className}Test.java`).replace(/\\/g, "/"),
    path.join(testDir, `${className}Tests.java`).replace(/\\/g, "/"),
  ];
}

/** Collect existing test files related to changed production files */
function collectExistingTestsForChangedProductionFiles(changedProdPaths) {
  const byPath = new Map();

  for (const prod of changedProdPaths) {
    for (const candidate of getLikelyExistingTestPathsForProductionPath(prod)) {
      const abs = path.join(REPO_ROOT, candidate);
      if (!fileExists(abs)) continue;
      if (!byPath.has(candidate)) {
        byPath.set(candidate, {
          path: candidate,
          role: "existing_test",
          content: fs.readFileSync(abs, "utf8"),
        });
      }
    }
  }

  return Array.from(byPath.values());
}

/** OpenRouter call */
async function callOpenRouter(messages) {
  const res = await fetch("https://openrouter.ai/api/v1/chat/completions", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${apiKey}`,
      "Content-Type": "application/json",
      "HTTP-Referer": "https://github.com",
      "X-Title": "ci-ai-tests",
    },
    body: JSON.stringify({
      model,
      temperature: 0.2,
      messages,
      response_format: { type: "json_object" },
    }),
  });

  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new Error(`OpenRouter error: ${res.status} ${res.statusText}\n${body}`);
  }

  const data = await res.json();
  const content = data?.choices?.[0]?.message?.content;
  if (!content) throw new Error("Empty LLM response");
  return content;
}

/**
 * Try to locate the dependency script.
 * You said it's in Projet/Scriptdepandance/ (and there is src/.. after).
 * We'll check multiple candidate paths to be safe.
 */
function findDependencyScript() {
  const candidates = [
    // Common candidates in this repo
    path.join(REPO_ROOT, "scriptdependance"),
    path.join(REPO_ROOT, "Scriptdepandance"),
    path.join(PROJECT_DIR, "scriptdependance"),
    path.join(PROJECT_DIR, "Scriptdepandance"),

    // Legacy locations (kept for compatibility)
    path.join(PROJECT_DIR, "Scriptdepandance", "Scriptdepandance"),
    path.join(PROJECT_DIR, "Scriptdepandance"),
    path.join(PROJECT_DIR, "Scriptdepandance", "src", "Scriptdepandance"),
    path.join(REPO_ROOT, "scripts", "Scriptdepandance"),
  ];

  for (const p of candidates) {
    if (fileExists(p)) return p;
  }
  return null;
}

/**
 * Run dependency discovery (jdeps) for a given changed source file.
 * Returns dependency source paths relative to Projet/ like "src/main/java/.../B.java"
 */
function getDependenciesForJavaSource(relPathFromRepo, depScriptPath) {
  // relPathFromRepo: "<module>/src/main/java/.../A.java"
  const relFromProject = relPathFromRepo.startsWith(PROJECT_PREFIX)
    ? relPathFromRepo.slice(PROJECT_PREFIX.length)
    : relPathFromRepo;

  if (!depScriptPath) return [];

  try {
    const out = sh(`bash "${depScriptPath}" "${relFromProject}"`, { cwd: PROJECT_DIR });
    if (!out) return [];
    return out
      .split("\n")
      .map((s) => s.trim())
      .filter(Boolean)
      .filter((p) => p.startsWith("src/main/java/") && p.endsWith(".java"));
  } catch {
    console.log(`Dependency discovery failed for ${relPathFromRepo}. Continuing without deps.`);
    return [];
  }
}

/** Limit deps */
function limitDependencies(depPaths, { maxDepsPerFile = 30 } = {}) {
  return depPaths.slice(0, maxDepsPerFile);
}

/** Build dependency map + file contents */
function buildDependencyPayload(changedPaths) {
  const depScriptPath = findDependencyScript();
  if (!depScriptPath) {
    console.log("Dependency script not found. No dependencies will be provided to LLM.");
    return { depMap: {}, dependencyFiles: [] };
  }

  console.log(`Using dependency script: ${depScriptPath}`);

  const depMap = {}; // changed repo path -> deps (repo-ish paths)
  const depSet = new Set(); // deps relative to project dir

  for (const p of changedPaths) {
    const depsRelToProjet = limitDependencies(
      getDependenciesForJavaSource(p, depScriptPath),
      { maxDepsPerFile: 30 }
    );

    depMap[p] = depsRelToProjet.map((d) => `${PROJECT_PREFIX}${d}`);
    for (const d of depsRelToProjet) depSet.add(d);
  }

  const dependencyFiles = Array.from(depSet)
    .map((depRelToProject) => {
      const abs = path.join(PROJECT_DIR, depRelToProject);
      if (!fileExists(abs)) return null;
      return {
        path: `${PROJECT_PREFIX}${depRelToProject}`,
        role: "dependency",
        content: fs.readFileSync(abs, "utf8"),
      };
    })
    .filter(Boolean);

  return { depMap, dependencyFiles };
}

/** Build messages */
function buildMessages(allFiles, depMap) {
  return [
    {
      role: "system",
      content:
        "You are an expert Java QA engineer. Generate high-quality unit tests.\n" +
        "Rules:\n" +
        "- Output ONLY valid JSON.\n" +
        `- Create or update ONLY test files under ${TEST_JAVA_ROOT}.\n` +
        "- Use JUnit 5. Mockito allowed.\n" +
        "- Do NOT change production code.\n" +
        "- Keep tests deterministic.\n" +
        "- Production code is the source of truth for behavior unless the user explicitly specifies otherwise.\n" +
        "  Existing tests may be outdated; do NOT copy expected values blindly from existing tests.\n" +
        "- If a test file already exists, DO NOT modify or remove any existing lines.\n" +
        "  Add new @Test methods by INSERTING them immediately BEFORE the final closing '}' of the class.\n" +
        "  Never duplicate annotations on the same method (e.g., do not output '@Test' twice).\n" +
        "  Do NOT add/modify imports or package declarations in existing files; use fully-qualified names if needed.\n" +
        "- Always return full file content for each file.\n" +
        "- Prefer pure unit tests; mock Spring/DB.\n" +
        "\n" +
        "Context:\n" +
        "- role='changed' files are TARGETS (generate tests for these)\n" +
        "- role='existing_test' files are CURRENT TESTS you must preserve\n" +
        "- role='dependency' files are SUPPORTING CONTEXT\n",
    },
    {
      role: "user",
      content:
        "Generate tests for the CHANGED production files below.\n" +
        "You are also provided with TRANSITIVE INTERNAL DEPENDENCIES so tests match real behavior.\n\n" +
        "Dependency map (changed -> dependencies):\n" +
        JSON.stringify(depMap, null, 2) +
        "\n\n" +
        "Return JSON exactly like:\n" +
        `{ "files": [ { "path": "${TEST_JAVA_ROOT}...Test.java", "content": "..." } ], "notes": "..." }\n\n` +
        JSON.stringify({ files: allFiles }, null, 2),
    },
  ];
}

function buildRepairMessages({ failing, relatedFiles }) {
  return [
    {
      role: "system",
      content:
        "You are an expert Java QA engineer fixing failing JUnit 5 tests.\n" +
        "Rules:\n" +
        "- Output ONLY valid JSON.\n" +
        `- You may modify ONLY existing test files under ${TEST_JAVA_ROOT}.\n` +
        "- You must ONLY modify the specific failing test METHODS listed (match by method name).\n" +
        "- Do NOT add/modify package declarations or imports; use fully-qualified names if needed.\n" +
        "- Keep tests deterministic. Prefer unit tests; mock Spring/DB.\n" +
        "- Return method bodies as full Java method declarations (including annotations like @Test) in 'methodCode'.\n" +
        "- Never output duplicate annotations (e.g., do not output '@Test' twice in a row).\n" +
        "\n" +
        "Return JSON exactly like:\n" +
        `{ "repairs": [ { "path": "${TEST_JAVA_ROOT}...Test.java", "methodName": "testX", "methodCode": "..." } ], "notes": "..." }\n`,
    },
    {
      role: "user",
      content:
        "These tests are failing. Provide targeted fixes by returning repaired methods only.\n\n" +
        "Failing testcases (authoritative):\n" +
        JSON.stringify(failing, null, 2) +
        "\n\n" +
        "Files (test + relevant production context):\n" +
        JSON.stringify({ files: relatedFiles }, null, 2),
    },
  ];
}

/** Main */
async function main() {
  if (!fileExists(PROJECT_DIR)) {
    console.log("Maven project directory not found. Skipping AI step.");
    process.exit(0);
  }

  const ctx = getEventContext();
  fetchBaseBranch(ctx.baseBranch);

  const changedPaths = listChangedJavaFiles(ctx.baseSha, ctx.headSha);
  if (changedPaths.length === 0) {
    console.log("No changed Java production files detected. Skipping AI generation.");
    process.exit(0);
  }

  // Always compute impacted tests (even if AI is disabled) so CI can run a subset.
  const impacted = computeImpactedTestPaths({
    changedProdPaths: changedPaths,
    baseSha: ctx.baseSha,
    headSha: ctx.headSha,
  });
  const testFilterPath = path.join(REPO_ROOT, "ai-test-filter.txt");
  const impactedListPath = path.join(REPO_ROOT, "ai-impacted-tests.txt");
  const impactedClassesPath = path.join(REPO_ROOT, "ai-impacted-classes.txt");
  fs.writeFileSync(testFilterPath, (impacted.testFilter || "").trim() + "\n", "utf8");
  fs.writeFileSync(
    impactedListPath,
    impacted.testPaths.join("\n") + (impacted.testPaths.length ? "\n" : ""),
    "utf8"
  );
  fs.writeFileSync(
    impactedClassesPath,
    (impacted.impactedProdClasses || []).join("\n") + ((impacted.impactedProdClasses || []).length ? "\n" : ""),
    "utf8"
  );
  console.log("Wrote impacted test filter:", testFilterPath);
  console.log("Wrote impacted test list:", impactedListPath);
  console.log("Wrote impacted classes list:", impactedClassesPath);
  if (impacted.testFilter) console.log("Impacted tests filter (-Dtest=...):", impacted.testFilter);
  else console.log("Impacted tests filter is empty (no tests found for changed/impacted classes).");
  if (impacted.testPaths.length > 0) {
    console.log("Impacted test files:");
    for (const p of impacted.testPaths) console.log(`- ${p}`);
  }

  if (!AI_ENABLED) {
    console.log("LLM_API_KEY not set. Skipping AI test generation (impact-based test selection still produced)." );
    process.exit(0);
  }

  const changedFiles = changedPaths.map((relPath) => ({
    path: relPath,
    role: "changed",
    content: fs.readFileSync(path.join(REPO_ROOT, relPath), "utf8"),
  }));

  const existingTests = INCLUDE_EXISTING_TESTS_CONTEXT
    ? collectExistingTestsForChangedProductionFiles(changedPaths)
    : [];

  console.log(`Detected ${changedFiles.length} changed Java production file(s):`);
  console.log(changedFiles.map((f) => `- ${f.path}`).join("\n"));

  if (existingTests.length > 0) {
    console.log(`Detected ${existingTests.length} existing related test file(s):`);
    console.log(existingTests.map((f) => `- ${f.path}`).join("\n"));
  } else {
    if (!INCLUDE_EXISTING_TESTS_CONTEXT) {
      console.log(
        "Existing test context disabled (AI_INCLUDE_EXISTING_TESTS_CONTEXT=0). Generating tests from production code only."
      );
    } else {
      console.log("No existing related test files detected (will create new tests if needed).");
    }
  }

  console.log(`AI generating tests for ${changedFiles.length} changed file(s)...`);

  // deps
  const { depMap, dependencyFiles } = buildDependencyPayload(changedPaths);
  const allFilesForLLM = [...changedFiles, ...existingTests, ...dependencyFiles];

  // summary file for CI
  const summaryLines = [];
  for (const p of changedPaths) {
    const deps = depMap[p] || [];
    summaryLines.push(`- ${p}`);
    if (deps.length === 0) summaryLines.push(`  -> (no internal deps detected)`);
    else for (const d of deps) summaryLines.push(`  -> ${d}`);
  }
  const summaryPath = path.join(REPO_ROOT, "ai-deps-summary.txt");
  fs.writeFileSync(summaryPath, summaryLines.join("\n") + "\n", "utf8");
  console.log("Wrote dependency summary:", summaryPath);

  // call llm
  const raw = await callOpenRouter(buildMessages(allFilesForLLM, depMap));

  let parsed;
  try {
    parsed = JSON.parse(raw);
  } catch {
    throw new Error(`Model did not return valid JSON.\nRaw:\n${raw}`);
  }

  const outFiles = parsed.files;

  if (!Array.isArray(outFiles) || outFiles.length === 0) {
    console.log("Model returned no test files. Notes:", parsed.notes || "(none)");
    process.exit(0);
  }

  let written = 0;

  for (const f of outFiles) {
    if (!f?.path || typeof f?.content !== "string") continue;

    if (!f.path.startsWith(TEST_JAVA_ROOT)) continue;
    if (!isLikelyJavaTestFilename(f.path)) continue;

    const absPath = path.join(REPO_ROOT, f.path);

    if (fileExists(absPath)) {
      const oldContent = fs.readFileSync(absPath, "utf8");
      if (!isAppendOnlyUpdate(oldContent, f.content)) {
        console.log(`Refusing overwrite (not append-only): ${f.path}`);
        continue;
      }

      // Preserve the append-only barrier, but prevent accidental duplicate @Test lines
      // in the newly inserted chunk.
      const sanitized = buildAppendOnlyUpdateWithSanitizedInsertion({
        oldContent,
        newContent: f.content,
      });
      if (sanitized && isAppendOnlyUpdate(oldContent, sanitized)) {
        writeFileSafe(absPath, sanitized);
      } else {
        writeFileSafe(absPath, f.content);
      }
      written++;
      continue;
    }

    // New file: safe to sanitize whole content to avoid rare invalid constructs.
    const eol = detectEol(f.content);
    const sanitizedNew = dedupeTestAnnotationsPerAnnotatedElement(f.content, eol);
    writeFileSafe(absPath, sanitizedNew);
    written++;
  }

  console.log(`Wrote/updated ${written} test file(s).`);
  if (parsed.notes) console.log("Notes:", parsed.notes);

  // Recompute impacted tests now that new tests may have been created.
  try {
    const impactedAfter = computeImpactedTestPaths({
      changedProdPaths: changedPaths,
      baseSha: ctx.baseSha,
      headSha: ctx.headSha,
    });
    fs.writeFileSync(testFilterPath, (impactedAfter.testFilter || "").trim() + "\n", "utf8");
    fs.writeFileSync(
      impactedListPath,
      impactedAfter.testPaths.join("\n") + (impactedAfter.testPaths.length ? "\n" : ""),
      "utf8"
    );
    fs.writeFileSync(
      impactedClassesPath,
      (impactedAfter.impactedProdClasses || []).join("\n") +
        ((impactedAfter.impactedProdClasses || []).length ? "\n" : ""),
      "utf8"
    );
    if (impactedAfter.testFilter) {
      console.log("Updated impacted test filter (-Dtest=...):", impactedAfter.testFilter);
    }
  } catch {
    // ignore
  }

  // Option 1: repair failing tests in a bounded loop so CI doesn't fail immediately
  if (REPAIR_ENABLED && MAX_REPAIR_ITERS > 0) {
    console.log(`Running Maven tests with AI repair enabled (max iters: ${MAX_REPAIR_ITERS})...`);

    // Best-effort clean to avoid stale failures
    rmrf(path.join(PROJECT_DIR, "target", "surefire-reports"));

    let lastFailures = [];

    // Start by running only the impacted tests subset.
    // If empty, do NOT run the full suite (by design).
    let currentTestFilter = (fs.readFileSync(testFilterPath, "utf8") || "").trim();
    if (!currentTestFilter) {
      console.log("[AI-REPAIR] Impacted test filter is empty; skipping test execution/repair loop.");
      return;
    }

    for (let iter = 1; iter <= MAX_REPAIR_ITERS; iter++) {
      console.log(`\n[AI-REPAIR] Test run ${iter}/${MAX_REPAIR_ITERS}...`);
      const run = runMavenTests({ testFilter: currentTestFilter });
      if (run.ok) {
        console.log("[AI-REPAIR] Tests passing.");
        lastFailures = [];
        break;
      }

      let failures = parseSurefireFailures();
      lastFailures = failures;

      // If we have no Surefire testcase failures, we might still have compilation errors
      // (e.g., rename in production causing tests to not compile: "cannot find symbol").
      if (failures.length === 0) {
        const compilationErrors = parseMavenCompilationErrors(`${run.stdout}\n${run.stderr}`)
          .filter((e) => e.path.includes(TEST_JAVA_ROOT) && e.path.endsWith(".java"));

        if (compilationErrors.length === 0) {
          console.log(
            "[AI-REPAIR] mvn test failed but no Surefire XML failures or parseable compilation errors were found."
          );
          break;
        }

        // Convert compilation errors into method-level failures.
        const compileFailures = [];
        for (const e of compilationErrors.slice(0, 20)) {
          const relPath = normalizePathSlashes(e.path);
          const abs = path.join(REPO_ROOT, relPath);
          if (!fileExists(abs)) continue;

          const content = fs.readFileSync(abs, "utf8");
          const methodName = findEnclosingJavaMethodNameByLine(content, e.line) || "unknown";
          const fqcn = testFqcnFromTestFileContent({ relTestPathFromRepo: relPath, content });

          // If we cannot locate a method, we still pass something; applyMethodReplacement
          // can append when method not found, but we try to keep it targeted.
          if (methodName === "unknown") continue;

          compileFailures.push({
            className: fqcn,
            testName: methodName,
            kind: "compile",
            message: e.message || "Compilation error",
            details: `Compilation error at ${relPath}:[${e.line}]: ${e.message || ""}`,
          });
        }

        if (compileFailures.length === 0) {
          console.log(
            "[AI-REPAIR] Compilation errors detected, but could not map them to enclosing test methods."
          );
          break;
        }

        failures = compileFailures;
        lastFailures = failures;
        console.log(
          `[AI-REPAIR] Detected compilation-related failures in ${new Set(
            failures.map((f) => f.className)
          ).size} test class(es). Attempting targeted fixes...`
        );
      }

      // Next iteration: re-run only failing test classes for faster feedback.
      currentTestFilter = Array.from(new Set(failures.map((f) => simpleClassName(f.className))))
        .filter(Boolean)
        .join(",");

      console.log(`[AI-REPAIR] Detected ${failures.length} failing testcase(s). Attempting targeted fixes...`);

      // Collect only the failing test source files + changed prod + deps (context)
      const failingTestPaths = Array.from(
        new Set(failures.map((f) => testClassPathFromClassName(f.className)))
      );

      const failingTestFiles = failingTestPaths
        .map((p) => {
          const abs = path.join(REPO_ROOT, p);
          if (!fileExists(abs)) return null;
          return { path: p, role: "existing_test", content: fs.readFileSync(abs, "utf8") };
        })
        .filter(Boolean);

      const relatedFiles = [...changedFiles, ...dependencyFiles, ...failingTestFiles];

      // Ask the LLM to return method-level repairs
      const rawRepair = await callOpenRouter(
        buildRepairMessages({ failing: failures, relatedFiles })
      );

      let repairParsed;
      try {
        repairParsed = JSON.parse(rawRepair);
      } catch {
        console.log("[AI-REPAIR] Model did not return valid JSON for repairs.");
        break;
      }

      const repairs = repairParsed.repairs;
      if (!Array.isArray(repairs) || repairs.length === 0) {
        console.log("[AI-REPAIR] Model returned no repairs.");
        break;
      }

      // Allowed methods per file (based on failing list)
      const allowedByPath = new Map();
      for (const f of failures) {
        const p = testClassPathFromClassName(f.className);
        if (!allowedByPath.has(p)) allowedByPath.set(p, new Set());
        allowedByPath.get(p).add(f.testName);
      }

      let applied = 0;
      for (const r of repairs) {
        if (!r?.path || typeof r?.methodName !== "string" || typeof r?.methodCode !== "string") continue;
        if (!r.path.startsWith(TEST_JAVA_ROOT)) continue;
        if (!/\.java$/.test(r.path)) continue;

        const allowed = allowedByPath.get(r.path);
        if (!allowed || !allowed.has(r.methodName)) {
          console.log(`[AI-REPAIR] Refusing repair outside failing methods: ${r.path}#${r.methodName}`);
          continue;
        }

        const abs = path.join(REPO_ROOT, r.path);
        if (!fileExists(abs)) {
          console.log(`[AI-REPAIR] Test file not found for repair: ${r.path}`);
          continue;
        }

        try {
          applyMethodReplacement({
            filePathAbs: abs,
            methodName: r.methodName,
            newMethodCode: r.methodCode,
          });
          applied++;
        } catch (e) {
          console.log(`[AI-REPAIR] Failed applying repair to ${r.path}#${r.methodName}: ${e?.message || e}`);
        }
      }

      console.log(`[AI-REPAIR] Applied ${applied} repair(s).`);

      // Clean reports before next run
      rmrf(path.join(PROJECT_DIR, "target", "surefire-reports"));
    }

    if (lastFailures.length > 0) {
      if (REPAIR_STRICT) {
        console.log("[AI-REPAIR] Still failing after max iterations. Failing the CI step.");
        process.exit(1);
      }
      console.log(
        "[AI-REPAIR] Still failing after max iterations. Continuing so downstream Maven verification can decide the build outcome."
      );
    }
  }
}

main().catch((err) => {
  console.error("AI generation failed:");
  console.error(err?.stack || err?.message || err);
  process.exit(1);
});
