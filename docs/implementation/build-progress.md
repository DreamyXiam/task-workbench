# V1.5 full build ledger
User authorization: 开始全量构建项目, 2026-10-24. Replaces earlier task001-only restriction.
Spec: docs/requirements/v15-tasks.md. Plan: docs/superpowers/plans/2026-10-24-v15-build.md.
Workspace: D:/TASK/task-workbench, branch codex/v1.5-build, new isolated project; no production access.
Ruling: existing approved design is the source, no repeat design approval. Keep unresolved policy ADR defaults. Cost if incorrect: rework in this new folder only.
Ruling: external dependency DNS failure blocks formal CDEX002 acceptance; continue reviewable source work on an unmerged branch and explicit cached offline verification. Never freeze legacy versions or claim release. Cost: formal compatibility and deployment still require the approved candidates.
Tasks:
- Backend worker owns backend and backend-report.
- Frontend worker owns frontend and frontend-report.
- Operations worker owns deploy/CI/docs except requirements,plan,audit,ADR index and other reports.
- Root owns integration, candidate descriptors, final verification and review.
Preflight: contract fixed in plan; mutable files disjoint. Backend remains authority for route/JSON contracts; frontend changes adapt to actual reviewed contract.
