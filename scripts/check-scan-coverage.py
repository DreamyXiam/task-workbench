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
for report in reports:
    document = json.loads(Path(report).read_text(encoding="utf-8-sig"))
    packages = [p for r in document.get("Results", []) for p in r.get("Packages", [])]
    found = {(p.get("Name"), p.get("Version")) for p in packages}
    missing = [(name, version) for name, version in required.items() if (name, version) not in found]
    if missing or len(found) < 50:
        raise SystemExit(f"FAIL: {report}: {len(found)} components, missing required dependency versions: {missing}")
    print(f"PASS: {report}: {len(found)} components and required patched versions inspected")
