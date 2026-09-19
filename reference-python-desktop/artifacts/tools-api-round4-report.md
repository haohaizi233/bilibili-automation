# ToolsAPI bounded-close Round 4 verification

Date: 2026-07-29 (Asia/Shanghai)

## Scope and result

- In-scope production file: `ucrawl/tools.py`
- In-scope contract file: `tests/contract/python/test_tools_api.py`
- Result: complete; no known unmet in-scope close semantics.
- SDK owner generation is intentionally outside this contract. The facade accepts one explicit `ExecutionProfile`, preserves its object identity, and forwards it unchanged.

## Final snapshot

- `ucrawl/tools.py`
  - SHA256: `BC1EE5E7C2E3F101C0ED609A1ECD03877000FA2FDE88D41B2ABE4BA2F45DF0D5`
  - LastWriteTimeUtc: `2026-07-28T18:18:00.2750468Z`
- `tests/contract/python/test_tools_api.py`
  - SHA256: `CA22B0CA8E163A8275CF4237C9973F75F490B01D6684167C0C3290A55E2C5A1F`
  - LastWriteTimeUtc: `2026-07-28T18:24:17.1397784Z`
- The hashes and timestamps were unchanged across the pre-stress and post-stress snapshots.

## Semantic evidence

1. `timeout=0`: immediate shutdown is still requested with `timeout=0.0`; incomplete cleanup returns `False`, remains retained, and succeeds on retry.
2. Shutdown consumes budget: the remaining deadline is passed to `shutdown`; when shutdown spends it, no unbounded active-lease wait follows.
3. Creation in progress: close linearizes with lazy construction, closes admission, does not hold the lifecycle lock across shutdown, and cleans the created service exactly once.
4. Active lease: admitted calls finish safely; a short/default close budget returns `False` when the lease does not finish, while cleanup remains retryable. Reentrant close from the active lease fails fast instead of deadlocking.
5. Concurrent close: callers use independent deadlines against the same cleanup service; completed shutdown is not repeated.
6. Context body exception: cleanup failure is recorded/annotated without replacing the body exception, including when `add_note` itself fails.

## Verification commands and outputs

- `python -m pytest tests/contract/python/test_tools_api.py -q`
  - `43 passed in 6.20s`
- Targeted six-category close evidence run
  - `9 passed in 0.77s`
- Full contract stress, 50 independent pytest iterations split across four processes
  - shard A: `13/13`
  - shard B: `13/13`
  - shard C: `12/12`
  - shard D: `12/12`
  - aggregate: `50/50` iterations, equivalent to `2150/2150` parameter-expanded contract cases.
- `python -m ruff check ucrawl/tools.py tests/contract/python/test_tools_api.py`
  - `All checks passed!`
- `python -m py_compile ucrawl/tools.py tests/contract/python/test_tools_api.py`
  - exit code `0`
- `git diff --check -- ucrawl/tools.py tests/contract/python/test_tools_api.py`
  - exit code `0`
- Real `ToolRunnerService` lifecycle probe through `ToolsAPI`
  - before close: `tool-history_0`, `tool-runner_0`
  - default timeout: `1.0s`
  - close elapsed: `0.013518s`
  - residual matching threads after close: `[]`

## Contract boundary

The facade supplies the single remaining deadline to synchronous `service.shutdown(wait=True, timeout=remaining)`. As with any synchronous timeout-bearing interface, hard preemption is not possible if a foreign implementation ignores that timeout. The production `ToolRunnerService` honors the timeout contract and passed the real-runner bounded-close/thread-cleanup probe.

No commit, stage, or push was performed by this subtask.

---

## Independent review fix round 1

Review input: `.superpowers/sdd/2026-07-27-tool-runtime-hardening/tools-api-round4-review.md`

### Findings closed

- I1: added `reload(*, force=False)` to `_OPERATIONS`, `ToolRunnerServiceContract`, the public facade, and forwarding/race contracts. The complete service call runs under one `_active_service()` lease and receives the exact facade-owned `ExecutionProfile` plus the unchanged `force` value.
- I2: made cleanup recording best-effort across error recording, note construction, `add_note` lookup/invocation, and fallback attribute assignment. No `BaseException` raised by annotation can replace the context body exception; `_last_cleanup_error` remains available on the normal facade, including when hostile instance `__setattr__` is present.
- I3: replaced the sequential multi-close probe with an event-barrier schedule that truly overlaps callers, added direct run/wait/cancel/history/reload operation-vs-close lease coverage, and proved that time spent waiting for lazy creation is deducted before calling shutdown.
- M1: changed `ToolRunnerServiceContract.shutdown` from `Any` to `bool`.

### TDD evidence

- Initial RED before the implementation fix:
  - command: forwarding reload plus the two pre-call cleanup annotation probes
  - result: `3 failed, 9 deselected`
  - failures: missing `ToolsAPI.reload`; escaping cleanup `__str__`; escaping body `add_note` lookup.
- Initial GREEN after implementation:
  - same command: `3 passed, 9 deselected in 0.76s`.
- Concurrency schedule baseline after implementation:
  - overlapping close, direct stateful lease matrix, lazy-create remaining budget: `7 passed in 1.83s`.
- `_last_cleanup_error` defensive-boundary mutation check:
  - temporarily restored ordinary instance assignment: `1 failed`, with the hostile `__setattr__` exception masking the body.
  - restored defensive assignment and included fallback-failure coverage: `2 passed in 0.59s`.

### Rebase and final snapshots

An unrelated writer changed the production file after RED. The write was detected by a failed patch context; no old snapshot was forced over it. Work was rebased onto the new content and the other writer was asked to avoid the two scoped files.

- Stable write baseline after the collision was detected:
  - `ucrawl/tools.py`
    - SHA256: `A54C328D6E33597BA1275DEF906363EA5000B8BE2D3E64A84541624CE767B1D6`
    - LastWriteTimeUtc: `2026-07-28T18:45:36.0272573Z`
  - `tests/contract/python/test_tools_api.py`
    - SHA256: `6804506BB6A11908B4300B016F2A77D4D74B568684AE72ADE509F298956D7784`
    - LastWriteTimeUtc: `2026-07-28T18:44:44.6117101Z`
- Final source snapshot:
  - `ucrawl/tools.py`
    - SHA256: `6B4DB37EE05B33B726722A8E17B747F8B03258F53A15154AF938F7941E5A82C6`
    - LastWriteTimeUtc: `2026-07-28T18:51:07.9710880Z`
  - `tests/contract/python/test_tools_api.py`
    - SHA256: `CC5CD64D7B647FF3F5D4E954F4D6263CB39C955BDC90613E4B39D5D4D99150D6`
    - LastWriteTimeUtc: `2026-07-28T18:50:38.8345165Z`

### Final verification

- `python -m pytest tests/contract/python/test_tools_api.py -q`
  - `54 passed in 3.81s`
- Review-focused 50x stress with isolated pytest temp directories:
  - selected per iteration: all facade forwarding cases, four hostile cleanup paths, overlapping close, five direct stateful lease cases, and lazy-create remaining-budget accounting (`21` expanded cases per iteration).
  - shard A: `13/13`
  - shard B: `13/13`
  - shard C: `12/12`
  - shard D: `12/12`
  - aggregate: `50/50` iterations and `1050/1050` expanded cases, with clean output.
- `python -m ruff check ucrawl/tools.py tests/contract/python/test_tools_api.py`
  - `All checks passed!`
- `python -m py_compile ucrawl/tools.py tests/contract/python/test_tools_api.py`
  - exit code `0`
- `git diff --check -- ucrawl/tools.py tests/contract/python/test_tools_api.py`
  - exit code `0`
- Real `ToolRunnerService` probe through the public facade:
  - invoked `reload(force=True)` and completed a real `media_health` run.
  - before close: `tool-history_0`, `tool-runner_0`.
  - close elapsed: `0.014892s` with the public `1.0s` default.
  - residual matching threads after close: `[]`.

No commit, stage, or push was performed in review fix round 1.
