#!/usr/bin/env python3
"""Create and verify a fail-closed Dependency-Check NVD data archive."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import shutil
import subprocess
import sys
import tarfile
import tempfile
from datetime import datetime, timedelta, timezone
from pathlib import Path, PurePosixPath


SCANNER_VERSION = "12.2.2"
DATABASE_PATH = PurePosixPath("data/odc.mv.db")
MAX_ARCHIVE_BYTES = 2 * 1024 * 1024 * 1024
MIN_DATABASE_BYTES = 1024 * 1024


class ArchiveError(ValueError):
    """Raised when NVD archive provenance or content cannot be trusted."""


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


def parse_timestamp(value: str, field: str) -> datetime:
    if not isinstance(value, str) or not value.endswith("Z"):
        raise ArchiveError(f"{field} must be a UTC timestamp")
    try:
        return datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError as error:
        raise ArchiveError(f"{field} is invalid") from error


def iso_timestamp(value: datetime) -> str:
    return value.astimezone(timezone.utc).isoformat().replace("+00:00", "Z")


def relative_member(name: str) -> PurePosixPath:
    path = PurePosixPath(name)
    if not name or path.is_absolute() or ".." in path.parts or path.parts[0] != "data":
        raise ArchiveError("archive contains an unsafe path")
    return path


def archive_members(data_dir: Path) -> list[dict[str, object]]:
    if not data_dir.is_dir():
        raise ArchiveError("NVD data directory is missing")
    members: list[dict[str, object]] = []
    for path in sorted(data_dir.rglob("*")):
        if path.is_dir():
            continue
        if not path.is_file() or path.is_symlink():
            raise ArchiveError("NVD data contains a non-regular file")
        rel = PurePosixPath("data") / path.relative_to(data_dir).as_posix()
        members.append({"path": str(rel), "size": path.stat().st_size, "sha256": sha256(path)})
    if not members:
        raise ArchiveError("NVD data directory is empty")
    return members


def create_archive(data_dir: Path, archive_path: Path) -> list[dict[str, object]]:
    members = archive_members(data_dir)
    database = data_dir / DATABASE_PATH.relative_to("data")
    if not database.is_file() or database.stat().st_size < MIN_DATABASE_BYTES:
        raise ArchiveError("Dependency-Check database is missing or undersized")
    archive_path.parent.mkdir(parents=True, exist_ok=True)
    with tarfile.open(archive_path, "w:gz", format=tarfile.PAX_FORMAT) as archive:
        for item in members:
            source = data_dir.parent / item["path"]
            info = archive.gettarinfo(str(source), arcname=str(item["path"]))
            info.uid = info.gid = 0
            info.uname = info.gname = ""
            with source.open("rb") as stream:
                archive.addfile(info, stream)
    if archive_path.stat().st_size > MAX_ARCHIVE_BYTES:
        raise ArchiveError("NVD archive exceeds the maximum permitted size")
    return members


def build_manifest(args: argparse.Namespace, members: list[dict[str, object]]) -> dict[str, object]:
    created = utc_now()
    expires = created + timedelta(hours=args.max_age_hours)
    return {
        "archiveSha256": sha256(args.archive),
        "archiveSize": args.archive.stat().st_size,
        "createdAt": iso_timestamp(created),
        "expiresAt": iso_timestamp(expires),
        "files": members,
        "publisherWorkflow": args.publisher_workflow,
        "repository": args.repository,
        "scannerVersion": SCANNER_VERSION,
        "sourceRevision": args.source_revision,
        "workflowRunId": args.workflow_run_id,
    }


def write_manifest(args: argparse.Namespace, members: list[dict[str, object]]) -> None:
    args.manifest.write_text(json.dumps(build_manifest(args, members), indent=2, sort_keys=True) + "\n")


def validate_manifest(manifest: dict[str, object], args: argparse.Namespace) -> list[dict[str, object]]:
    required = {
        "archiveSha256",
        "archiveSize",
        "createdAt",
        "expiresAt",
        "files",
        "publisherWorkflow",
        "repository",
        "scannerVersion",
        "sourceRevision",
        "workflowRunId",
    }
    if set(manifest) != required:
        raise ArchiveError("manifest fields are incomplete or unexpected")
    if manifest["repository"] != args.repository:
        raise ArchiveError("manifest repository mismatch")
    if manifest["publisherWorkflow"] != args.publisher_workflow:
        raise ArchiveError("manifest publisher workflow mismatch")
    if manifest["scannerVersion"] != SCANNER_VERSION:
        raise ArchiveError("Dependency-Check scanner version mismatch")
    if not isinstance(manifest["sourceRevision"], str) or len(manifest["sourceRevision"]) != 40:
        raise ArchiveError("manifest source revision is invalid")
    if args.expected_source_revision and manifest["sourceRevision"] != args.expected_source_revision:
        raise ArchiveError("manifest source revision does not match the publisher run")
    if not isinstance(manifest["workflowRunId"], str) or not manifest["workflowRunId"].isdigit():
        raise ArchiveError("manifest workflow run identity is invalid")
    if args.expected_workflow_run_id and manifest["workflowRunId"] != args.expected_workflow_run_id:
        raise ArchiveError("manifest workflow run identity does not match the publisher run")
    created = parse_timestamp(manifest["createdAt"], "createdAt")
    expires = parse_timestamp(manifest["expiresAt"], "expiresAt")
    now = parse_timestamp(args.now, "now") if args.now else utc_now()
    if expires <= now or created > now or expires - created > timedelta(hours=args.max_age_hours):
        raise ArchiveError("NVD archive is expired or violates the maximum-age policy")
    if not isinstance(manifest["archiveSize"], int) or not 0 < manifest["archiveSize"] <= MAX_ARCHIVE_BYTES:
        raise ArchiveError("manifest archive size is invalid")
    files = manifest["files"]
    if not isinstance(files, list) or not files:
        raise ArchiveError("manifest file inventory is invalid")
    validated: list[dict[str, object]] = []
    seen: set[str] = set()
    for item in files:
        if not isinstance(item, dict) or set(item) != {"path", "size", "sha256"}:
            raise ArchiveError("manifest file entry is invalid")
        relative_member(str(item["path"]))
        if item["path"] in seen or not isinstance(item["size"], int) or item["size"] < 0:
            raise ArchiveError("manifest file inventory is invalid")
        if not isinstance(item["sha256"], str) or len(item["sha256"]) != 64:
            raise ArchiveError("manifest file digest is invalid")
        seen.add(item["path"])
        validated.append(item)
    if str(DATABASE_PATH) not in seen:
        raise ArchiveError("manifest lacks the Dependency-Check database")
    return validated


def verify_and_extract(args: argparse.Namespace) -> None:
    try:
        manifest = json.loads(args.manifest.read_text())
    except (OSError, json.JSONDecodeError) as error:
        raise ArchiveError("manifest cannot be read") from error
    files = validate_manifest(manifest, args)
    if not args.archive.is_file() or args.archive.stat().st_size != manifest["archiveSize"]:
        raise ArchiveError("archive is missing or has an unexpected size")
    if sha256(args.archive) != manifest["archiveSha256"]:
        raise ArchiveError("archive digest mismatch")
    if args.destination.exists():
        raise ArchiveError("destination must not already exist")
    expected = {str(item["path"]): item for item in files}
    try:
        with tarfile.open(args.archive, "r:gz") as archive:
            members = archive.getmembers()
            actual = set()
            for member in members:
                path = str(relative_member(member.name))
                if not member.isfile() or path not in expected:
                    raise ArchiveError("archive contains an unexpected or unsafe entry")
                if member.size != expected[path]["size"]:
                    raise ArchiveError("archive member size mismatch")
                actual.add(path)
            if actual != set(expected):
                raise ArchiveError("archive member inventory mismatch")
            args.destination.mkdir(mode=0o700)
            for member in members:
                target = args.destination / member.name
                target.parent.mkdir(parents=True, exist_ok=True)
                source = archive.extractfile(member)
                if source is None:
                    raise ArchiveError("archive member cannot be read")
                with target.open("wb") as destination:
                    shutil.copyfileobj(source, destination)
                os.chmod(target, 0o600)
                if sha256(target) != expected[member.name]["sha256"]:
                    raise ArchiveError("archive member digest mismatch")
    except (OSError, tarfile.TarError) as error:
        raise ArchiveError("archive cannot be safely extracted") from error
    database = args.destination / DATABASE_PATH
    if not database.is_file() or database.stat().st_size < MIN_DATABASE_BYTES:
        raise ArchiveError("extracted Dependency-Check database is invalid")


def refresh_and_build(args: argparse.Namespace) -> None:
    if not os.environ.get("NVD_API_KEY"):
        raise ArchiveError("NVD API key is unavailable")
    with tempfile.TemporaryDirectory() as directory:
        temp = Path(directory)
        pom = temp / "pom.xml"
        log = temp / "update.log"
        pom.write_text(
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"><modelVersion>4.0.0</modelVersion>"
            "<groupId>local</groupId><artifactId>trusted-nvd-refresh</artifactId><version>1</version>"
            "<build><plugins><plugin><groupId>org.owasp</groupId>"
            "<artifactId>dependency-check-maven</artifactId><version>12.2.2</version>"
            "</plugin></plugins></build></project>"
        )
        data_dir = temp / "data"
        result = subprocess.run(
            [
                "mvn", "-B", "-q", "-f", str(pom),
                "org.owasp:dependency-check-maven:12.2.2:update-only",
                f"-DdataDirectory={data_dir}",
                "-DnvdApiKeyEnvironmentVariable=NVD_API_KEY",
                "-DnvdApiDelay=6000", "-DnvdMaxRetryCount=10",
                "-DconnectionTimeout=30000", "-DconnectionReadTimeout=90000",
                "-DfailOnError=true",
            ],
            stdout=log.open("w"), stderr=subprocess.STDOUT, text=True, check=False,
        )
        output = log.read_text(errors="replace").lower()
        if result.returncode or any(term in output for term in ("nodataexception", "nvdapiexception", "failed to process cve-", "database update failed")):
            raise ArchiveError("Dependency-Check NVD refresh failed")
        args.data_dir.mkdir(parents=True, exist_ok=False)
        shutil.copytree(data_dir, args.data_dir, dirs_exist_ok=True)
    members = create_archive(args.data_dir, args.archive)
    write_manifest(args, members)


def parser() -> argparse.ArgumentParser:
    root = argparse.ArgumentParser()
    commands = root.add_subparsers(dest="command", required=True)
    for name in ("build", "refresh-and-build"):
        command = commands.add_parser(name)
        command.add_argument("--data-dir", type=Path, required=True)
        command.add_argument("--archive", type=Path, required=True)
        command.add_argument("--manifest", type=Path, required=True)
        command.add_argument("--repository", required=True)
        command.add_argument("--publisher-workflow", required=True)
        command.add_argument("--source-revision", required=True)
        command.add_argument("--workflow-run-id", required=True)
        command.add_argument("--max-age-hours", type=int, default=24)
    verify = commands.add_parser("verify")
    verify.add_argument("--archive", type=Path, required=True)
    verify.add_argument("--manifest", type=Path, required=True)
    verify.add_argument("--destination", type=Path, required=True)
    verify.add_argument("--repository", required=True)
    verify.add_argument("--publisher-workflow", required=True)
    verify.add_argument("--max-age-hours", type=int, default=24)
    verify.add_argument("--now")
    verify.add_argument("--expected-source-revision")
    verify.add_argument("--expected-workflow-run-id")
    return root


def main() -> int:
    args = parser().parse_args()
    try:
        if args.command == "build":
            write_manifest(args, create_archive(args.data_dir, args.archive))
        elif args.command == "refresh-and-build":
            refresh_and_build(args)
        else:
            verify_and_extract(args)
    except ArchiveError as error:
        print(f"trusted NVD archive verification failed: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
