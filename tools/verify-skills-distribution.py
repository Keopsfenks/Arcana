"""Read-only release audit. Run after ./gradlew build; requires Python 3.11+."""
from pathlib import Path
import hashlib
import json
import re
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
version = re.search(r"^version\s*=\s*(.+)$", (ROOT / "gradle.properties").read_text(), re.M)[1].strip()
artifact = ROOT / f"build/libs/arcana-{version}-all.jar"
reports = list(ROOT.glob("arcana-*/**/build/test-results/*/TEST-*.xml"))
totals = dict.fromkeys(("tests", "failures", "errors", "skipped"), 0)
for report in reports:
    suite = ET.parse(report).getroot()
    for key in totals:
        totals[key] += int(suite.get(key, 0))
assert totals["tests"] >= 1380 and totals["failures"] == totals["errors"] == totals["skipped"] == 0, totals

with zipfile.ZipFile(artifact) as jar:
    names = set(jar.namelist())
    required = (
        "com/keopsfenks/arcana/core/skills/platform/SkillPlatform.class",
        "com/keopsfenks/arcana/skills/DefaultSkillEngine.class",
        "com/keopsfenks/arcana/skills/components/StandardSkillCatalog.class",
        "com/keopsfenks/arcana/skills/components/StandardSkillExamples.class",
        "com/keopsfenks/arcana/skills/components/effects/projectiles/ProjectileMechanic.class",
        "com/keopsfenks/arcana/skills/components/triggers/EntityAttackSkillTrigger.class",
        "com/keopsfenks/arcana/internal/spigot/ArcanaImpl.class",
        "LICENSE.md", "NOTICE.md", "plugin.yml", "arcana.yml",
    )
    assert set(required) <= names, set(required) - names
    assert not any(name.startswith(("com/willfp/", "com/auxilor/")) or "libreforge" in name.lower() for name in names)
    assert not any(name.endswith(("/LegacyConsumer.class", "/AuraApiConsumer.class", "/EngineFixture.class", "/PlatformFixture.class", "/ControlledSkillScheduler.class")) for name in names)
    metadata = jar.read("plugin.yml").decode().replace("\r\n", "\n")
    assert "name: Arcana\n" in metadata and f"version: {version}" in metadata
    settings = jar.read("arcana.yml").decode()
    assert "bstats-id: 35538" in settings and "resource-id: 0" in settings and "eco.yml" not in names
    schema = json.loads(jar.read("arcana-skills/schema-v2.json"))
    source_schema = json.loads((ROOT / "arcana-skills-components/src/main/resources/arcana-skills/schema-v2.json").read_text())
    assert schema == source_schema
    component_entries = {key: value for key, value in schema["$defs"].items() if "/" in key}
    assert len(component_entries) == 125, len(component_entries)
    for name in ("LICENSE.md", "NOTICE.md"):
        assert jar.read(name).decode().replace("\r\n", "\n") == (ROOT / name).read_text()
    examples = sorted(name for name in names if name.startswith("arcana-skills/examples/") and name.endswith(".yml"))
    assert len(examples) == 10, examples
    assert all("enabled: false" in jar.read(name).decode() for name in examples)

linkage = list(ROOT.glob("arcana-core/core-nms/*/build/reports/nms-linkage.txt"))
assert len(linkage) == 7 and all(not report.read_text().strip() for report in linkage), linkage
baseline = ROOT / "arcana-api/src/test/resources/skills-v1-descriptors.txt"
descriptors = len(baseline.read_text().splitlines())
assert descriptors == 96, descriptors
with artifact.open("rb") as stream:
    checksum = hashlib.file_digest(stream, "sha256").hexdigest()
print(json.dumps({
    "artifact": str(artifact), "bytes": artifact.stat().st_size, "sha256": checksum,
    "tests": totals, "suites": len(reports), "nms_linkage_checks": len(linkage),
    "v1_descriptors": descriptors, "native_catalog_entries": len(component_entries),
    "disabled_examples": len(examples), "live_paper_folia_tested": False,
}, indent=2))
