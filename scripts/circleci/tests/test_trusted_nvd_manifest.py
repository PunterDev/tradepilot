from __future__ import annotations

import hashlib
import json
import subprocess
import tarfile
import tempfile
import unittest
from datetime import UTC, datetime
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
VALIDATOR = ROOT / "scripts/circleci/verify_trusted_nvd_manifest.py"
CONFIG = ROOT / ".circleci/trusted-owasp-control.yml"


class TrustedNvdManifestTests(unittest.TestCase):
    def bundle(self, directory: Path, *, created_at: str = "2026-11-08T12:00:00Z") -> tuple[Path, Path]:
        database = directory / "odc.mv.db"
        database.write_bytes(b"synthetic dependency-check database")
        archive = directory / "dependency-check-data.tar.gz"
        with tarfile.open(archive, "w:gz") as output:
            output.add(database, arcname="data/odc.mv.db")
        manifest = directory / "manifest.json"
        manifest.write_text(json.dumps({
            "scannerVersion": "12.2.2",
            "createdAt": created_at,
            "archiveSha256": hashlib.sha256(archive.read_bytes()).hexdigest(),
            "repository": "PunterDev/tradepilot",
            "controlCommit": "a" * 40,
        }))
        return manifest, archive

    def validate(self, manifest: Path, archive: Path) -> subprocess.CompletedProcess[str]:
        return subprocess.run([
            "python3", str(VALIDATOR), "--manifest", str(manifest), "--archive", str(archive),
            "--as-of", "2026-11-08T13:00:00Z",
        ], text=True, capture_output=True, check=False)

    def test_accepts_fresh_verified_bundle(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            manifest, archive = self.bundle(Path(temporary))
            self.assertEqual(0, self.validate(manifest, archive).returncode)

    def test_rejects_missing_stale_and_corrupted_bundles(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            manifest, archive = self.bundle(directory, created_at="2026-11-07T12:00:00Z")
            self.assertNotEqual(0, self.validate(manifest, archive).returncode)
            manifest, archive = self.bundle(directory)
            archive.write_bytes(b"corrupted")
            self.assertNotEqual(0, self.validate(manifest, archive).returncode)
            self.assertNotEqual(0, self.validate(manifest, directory / "missing.tar.gz").returncode)

    def test_rejects_untrusted_provenance_and_unsafe_paths(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            manifest, archive = self.bundle(directory)
            payload = json.loads(manifest.read_text())
            payload["repository"] = "untrusted/example"
            manifest.write_text(json.dumps(payload))
            self.assertNotEqual(0, self.validate(manifest, archive).returncode)

            unsafe = directory / "unsafe.tar.gz"
            with tarfile.open(unsafe, "w:gz") as output:
                database = directory / "odc.mv.db"
                output.add(database, arcname="../odc.mv.db")
            payload["repository"] = "PunterDev/tradepilot"
            payload["archiveSha256"] = hashlib.sha256(unsafe.read_bytes()).hexdigest()
            manifest.write_text(json.dumps(payload))
            self.assertNotEqual(0, self.validate(manifest, unsafe).returncode)

    def test_trusted_configuration_never_grants_context_to_analysis(self) -> None:
        config = CONFIG.read_text()
        self.assertIn("context: tradepilot-approval-read", config)
        self.assertIn("context: tradepilot-nvd-refresh", config)
        analysis = config.split("secretless-dependency-check:", 1)[1].split("workflows:", 1)[0]
        self.assertNotIn("context:", analysis)
        self.assertIn("env -u NVD_API_KEY -u TRADEPILOT_GITHUB_APPROVAL_READ_TOKEN", analysis)
        self.assertIn("equal: [master, << pipeline.git.branch >>]", config)
        self.assertIn("trusted-final-approval-validation", config)
        self.assertIn("cmp /tmp/approved-target.json /tmp/revalidated-target.json", config)

    def test_control_pipeline_is_master_only_and_requires_explicit_enablement(self) -> None:
        config = CONFIG.read_text()
        self.assertIn("equal: [true, << pipeline.parameters.security_activation >>]", config)
        self.assertIn("test \"$CIRCLE_BRANCH\" = master", config)
        self.assertIn("context: tradepilot-approval-read", config)
