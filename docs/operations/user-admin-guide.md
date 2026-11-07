# User and administrator handover

Status: source-level workflow guide; user acceptance pending. UI and exact permitted actions follow active company/role context and server allowedActions.

Sign in with an individually provisioned account; change the temporary password, enroll TOTP when required, and keep recovery codes privately offline. Never share accounts or export data through an unapproved channel. After switching company/role, recheck the active context before viewing or writing. A denial is not a reason to change IDs or request administrator-wide data access.

## Initial administrator and authenticator setup

The provisioner uses the operator-supplied `TASK_BOOTSTRAP_ACCOUNT` and `TASK_BOOTSTRAP_PASSWORD`; it does not automatically create a password file. Obtain the temporary password through the secure channel used by the operator who provisioned the instance. Do not use a database password as the login password.

For the isolated test deployment where an operator has explicitly stored the initial account in a root-only JSON file, the server administrator can view that file in a private terminal:

```bash
sudo cat /etc/task-workbench-test/initial-admin.json
```

Use the `account` value as the login name and only the value of `password` as the password; do not include JSON quotes or field names. This path is specific to that test deployment and will not exist unless the operator created it. Do not paste the password into chat, tickets, logs, or shared screenshots. Once the administrator has changed the temporary password and verified access, remove the obsolete credential file using the operator's approved procedure.

During authenticator enrollment, scan the QR code using a TOTP authenticator app, or enter the displayed key manually. The QR code is generated locally in the browser from the provisioning URI. Enter the resulting six-digit code to complete enrollment, and save the recovery codes offline. A screenshot of a key or QR code exposes the authenticator secret; if setup is still pending, discard it and start enrollment again to generate a new key.

Leader: create task title/content/company/priority/deadline; review explainable department matching, choose exactly one host and at most two collaborators, then confirm. Cross-company collaboration is not effective until the other company accepts. Host department receives and assigns at least one executor and one owner. Executors post progress/results; the host summarizes; leader accepts or returns with reason. Current lateness and historical ever-overdue are distinct. Deadline changes preserve original deadline and history. Use a reason for rejection, cancellation, return or extension; reload after version conflict rather than resubmitting stale state.

Attachments are available only after authorization and scan approval. A quarantined or failed-scan file remains inaccessible; contact the operator to investigate rather than renaming/reuploading executable content. Exports contain internal data, watermark and audit trace; keep them within the authorized company scope.

KPI: inspect period/template/raw indicators/source snapshot; submit an objection within the7day Asia/Shanghai window. Authorized adjustments retain raw score, reason and audit. Locked scores cannot be silently recomputed after task/deadline changes. The effective original template weights remain30/25/20/10/15 until approved otherwise; alternative templates are simulations. Personal assessment stays off and is not payroll calculation.

Administrator: maintain authorized organization/users/memberships/tags/rules/channels. Disable departed accounts and verify sessions revoke. Reset/recover TOTP only through audited identity verification. Never reveal Webhook or TOTP secret in an ordinary response. Administrators do not gain blanket business data or score-adjustment privilege. Emergency access requires approved minimal scope and expiry; ordinary audit records cannot be erased by the UI.

Operators follow deployment/monitoring/backup runbooks. Record unresolved source limitations in the release checklist before training. Pilot1–2 departments through one full monthly assessment and obtain explicit business acceptance.
