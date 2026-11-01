"""Fail closed when a successful scanner command did not inspect the built dependencies."""
import json
from pathlib import Path
import sys

reports = sys.argv[1:] or ["backend/target/trivy-sbom.json", "backend/target/trivy-jar.json"]
required = {
    "com.fasterxml.jackson.core:jackson-core": "2.21.7",
    "com.fasterxml.jackson.core:jackson-databind": "2.21.7",
    "org.apache.tomcat.embed:tomcat-embed-core": "10.1.61",
    "org.springframework:spring-webmvc": "6.2.19",
}
vex = json.loads(Path("docs/security/spring-mvc.openvex.json").read_text())
application = "pkg:maven/cn.workbench/task-workbench@1.5.0-SNAPSHOT"
subcomponent = "pkg:maven/org.springframework/spring-webmvc@6.2.19"
expected_ids = {"CVE-2026-47884", "CVE-2026-47890"}
statements = vex["statements"]
if len(statements) != 2 or {s["vulnerability"]["name"] for s in statements} != expected_ids:
    raise SystemExit("FAIL: application VEX scope changed; independent review required")
for statement in statements:
    if statement["products"] != [{"@id": application, "subcomponents": [{"@id": subcomponent}]}] \
            or statement["status"] != "not_affected" \
            or statement["justification"] != "vulnerable_code_not_in_execute_path":
        raise SystemExit("FAIL: unsupported application VEX statement")
for report in reports:
    document = json.loads(Path(report).read_text(encoding="utf-8-sig"))
    packages = [p for r in document.get("Results", []) for p in r.get("Packages", [])]
    found = {(p.get("Name"), p.get("Version")) for p in packages}
    missing = [(name, version) for name, version in required.items() if (name, version) not in found]
    if missing or len(found) < 50:
        raise SystemExit(f"FAIL: {report}: {len(found)} components, missing required dependency versions: {missing}")
    purls = {p.get("Identifier", {}).get("PURL", "").split("?")[0] for p in packages}
    if application not in purls or subcomponent not in purls:
        raise SystemExit(f"FAIL: {report}: exact application/Spring component identities not established")
    for result in document.get("Results", []):
        for finding in result.get("Vulnerabilities", []):
            if finding.get("Severity") not in {"HIGH", "CRITICAL"}:
                continue
            # Trivy's Java rootfs analyzer has no dependency graph, so cannot match
            # product+subcomponent VEX. Keep its original report and apply the same
            # exact application-scoped decision only after verifying both identities.
            if finding.get("VulnerabilityID") in expected_ids \
                    and finding.get("PkgName") == "org.springframework:spring-webmvc" \
                    and finding.get("InstalledVersion") == "6.2.19":
                print(f"NOT AFFECTED in this application: {finding['VulnerabilityID']}; see scoped VEX and exposure test")
            else:
                raise SystemExit(f"FAIL: {report}: blocking {finding.get('Severity')} {finding.get('VulnerabilityID')} in {finding.get('PkgName')}")
    print(f"PASS: {report}: {len(found)} components and required patched versions inspected")
