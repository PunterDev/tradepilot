#!/usr/bin/env python3
"""Fail closed before CircleCI attaches a protected context."""
from __future__ import annotations

import argparse, json, os, re, subprocess
from pathlib import Path

REPOSITORY = "PunterDev/tradepilot"
INTENT_PATH = "docs/security/trusted-owasp-activation-intent.json"
CONTROL_FILES = (".circleci/trusted-owasp-control.yml", ".circleci/trusted-owasp-continuation.yml", "scripts/circleci/prepare_trusted_owasp_continuation.py", "scripts/circleci/validate_trusted_owasp_activation.py")

def fail(message: str) -> None: raise SystemExit(f"trusted OWASP dispatcher rejected: {message}")
def git(*args: str, binary: bool = False): return subprocess.check_output(["git", *args], text=not binary).strip() if not binary else subprocess.check_output(["git", *args])

def trusted_metadata(control_sha: str, activation_sha: str) -> None:
    expected={"TP_INTEGRATION_SOURCE":"github_app","TP_CONFIG_REPOSITORY":REPOSITORY,"TP_CONFIG_PATH":".circleci/trusted-owasp-control.yml","TP_CONFIG_REF":"refs/tags/trusted-owasp-control-v1","TP_REPO_OWNER":"PunterDev","TP_REPO_NAME":"tradepilot","TP_CHECKOUT_BRANCH":"master","TP_TRIGGER_TYPE":"github_app","TP_EVENT_NAME":"push","TP_GITHUB_PUSH_REF":"refs/heads/master"}
    if any(os.environ.get(k)!=v for k,v in expected.items()): fail("configuration metadata is missing or untrusted")
    if os.environ.get("CIRCLE_SSH_CONNECTION"): fail("SSH reruns are not trusted")
    if not all(re.fullmatch(r"[0-9a-f]{40}",value or "") for value in (control_sha, activation_sha)): fail("control or activation revision is invalid")
    if os.environ.get("TP_CONFIG_SHA")!=control_sha or os.environ.get("TP_CHECKOUT_SHA")!=activation_sha or os.environ.get("TP_GITHUB_PUSH_AFTER")!=activation_sha: fail("configuration, push, and checkout revisions do not match their trusted roles")
    if git("rev-parse","HEAD")!=activation_sha: fail("checked-out HEAD does not match the GitHub push revision")
    try: git("cat-file", "-e", f"{control_sha}^{{commit}}")
    except subprocess.CalledProcessError: fail("pinned trusted control commit is unavailable")

def same_control_files(control_sha: str, activation_sha: str) -> bool:
    return all(git("show",f"{control_sha}:{path}",binary=True)==git("show",f"{activation_sha}:{path}",binary=True) for path in CONTROL_FILES)

def main() -> None:
    parser=argparse.ArgumentParser(); parser.add_argument("--intent",type=Path,required=True); parser.add_argument("--parameters",type=Path,required=True); parser.add_argument("--control-sha",required=True); parser.add_argument("--activation-sha",required=True); args=parser.parse_args()
    trusted_metadata(args.control_sha,args.activation_sha)
    try: intent=json.loads(args.intent.read_text())
    except (OSError,json.JSONDecodeError): fail("activation intent is unreadable")
    enabled=False
    if intent.get("status")=="READY":
        parent=git("rev-parse",f"{args.activation_sha}^1"); changed=git("diff","--name-only",parent,args.activation_sha).splitlines(); candidate=intent.get("candidate",{}); approval=intent.get("approval",{})
        if intent.get("repository")!=REPOSITORY or intent.get("controlCommit")!=args.control_sha or parent!=args.control_sha or changed!=[INTENT_PATH] or not same_control_files(args.control_sha,args.activation_sha): fail("READY intent is not isolated from the approved immutable control revision")
        if not re.fullmatch(r"[0-9a-f]{64}",str(candidate.get("sha256",""))) or not re.fullmatch(r"[0-9a-f]{40}",str(approval.get("targetSha",""))) or not isinstance(approval.get("pullRequest"),int) or approval["pullRequest"]<1 or approval.get("expiresAt")!="2026-12-15T23:59:59Z" or intent.get("scope")!="expanded-sixteen-dec15": fail("READY intent is incomplete or outside the approved scope")
        enabled=True
    elif intent.get("status")!="INACTIVE": fail("activation intent status is invalid")
    args.parameters.write_text(json.dumps({"run_protected":enabled})+"\n")
if __name__=="__main__": main()
