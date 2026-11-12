#!/usr/bin/env python3
"""Validate a master-controlled, owner-approved December activation intent."""
from __future__ import annotations
import argparse, hashlib, json, os, re, subprocess, urllib.request, xml.etree.ElementTree as ET
from datetime import UTC, datetime, timedelta
from pathlib import Path

REPO="PunterDev/tradepilot"; BRANCH="codex/security-consolidated"; EXPIRY="2026-12-15T23:59:59Z"; MAX_APPROVAL_AGE=timedelta(days=7)
TUPLES=frozenset({
 ("CVE-2026-53914","pkg:maven/org.jetbrains.kotlin/kotlin-stdlib@1.9.25"),("CVE-2026-53914","pkg:maven/org.jetbrains.kotlin/kotlin-stdlib-jdk7@1.9.25"),
 *((c,f"pkg:maven/org.springframework/{a}@6.2.19") for c in ("CVE-2026-47884","CVE-2026-47890","CVE-2026-47891","CVE-2026-47892","CVE-2026-59313","CVE-2026-59283") for a in ("spring-core","spring-tx")),
 ("CVE-2026-59270","pkg:maven/org.springframework.security/spring-security-core@6.5.11"),("CVE-2026-59270","pkg:maven/org.springframework.security/spring-security-web@6.5.11")})
NS="{https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd}"
def fail(msg): raise SystemExit(f"trusted OWASP activation rejected: {msg}")
def stamp(value): return datetime.fromisoformat(value.replace("Z","+00:00"))
def git(*args, binary=False): return subprocess.check_output(["git",*args], text=not binary)
def api(path):
 token=os.environ.get("TRADEPILOT_GITHUB_APPROVAL_READ_TOKEN")
 if not token: fail("GitHub approval-read credential is missing")
 request=urllib.request.Request("https://api.github.com"+path,headers={"Accept":"application/vnd.github+json","Authorization":"Bearer "+token,"X-GitHub-Api-Version":"2022-11-28"})
 return json.loads(urllib.request.urlopen(request,timeout=15).read())
def validate_candidate(candidate, expected_hash):
 if hashlib.sha256(candidate).hexdigest()!=expected_hash: fail("candidate hash mismatch")
 try: root=ET.fromstring(candidate)
 except ET.ParseError: fail("candidate XML is invalid")
 actual=set()
 for node in root.findall(NS+"suppress"):
  p=node.find(NS+"packageUrl"); c=node.findtext(NS+"cve")
  if node.get("until")!="2026-12-15" or p is None or p.get("regex")!="true" or not c: fail("candidate contains an invalid suppression")
  pair=next(((expected_cve,purl) for expected_cve,purl in TUPLES if expected_cve==c and p.text=="^"+re.escape(purl)+"$"),None)
  if pair is None or pair in actual: fail("candidate scope is not exact")
  actual.add(pair)
 if actual!=TUPLES: fail("candidate does not contain all sixteen exact tuples")
def main():
 p=argparse.ArgumentParser(); p.add_argument("--intent",type=Path,required=True); p.add_argument("--control-sha",required=True); p.add_argument("--activation-sha",required=True); p.add_argument("--output",type=Path,required=True); p.add_argument("--fixture",type=Path); a=p.parse_args()
 try: intent=json.loads(a.intent.read_text())
 except (OSError,json.JSONDecodeError): fail("activation intent is unreadable")
 if intent.get("status")!="READY" or intent.get("repository")!=REPO or intent.get("scope")!="expanded-sixteen-dec15" or intent.get("controlCommit")!=a.control_sha: fail("activation intent is not ready")
 if not re.fullmatch(r"[0-9a-f]{40}",a.control_sha) or not re.fullmatch(r"[0-9a-f]{40}",a.activation_sha): fail("trusted control or activation SHA is invalid")
 approval=intent.get("approval",{}); candidate=intent.get("candidate",{})
 target=str(approval.get("targetSha","")); number=approval.get("pullRequest"); source=str(candidate.get("sourcePath","")); digest=str(candidate.get("sha256",""))
 if source != "docs/security/candidates/dependency-check-expanded-sixteen-dec15-candidate.xml": fail("candidate path is not the approved December candidate")
 if not re.fullmatch(r"[0-9a-f]{40}",target) or not isinstance(number,int) or number<1 or not re.fullmatch(r"[0-9a-f]{64}",digest): fail("activation intent identifiers are invalid")
 if a.fixture: evidence=json.loads(a.fixture.read_text()); pr=evidence["pr"]; comment=evidence["comment"]
 else: pr=api(f"/repos/{REPO}/pulls/{number}"); comment=api(f"/repos/{REPO}/issues/comments/{approval.get('commentId','')}")
 if pr.get("draft") or pr.get("base",{}).get("ref")!="master" or pr.get("head",{}).get("ref")!=BRANCH or pr.get("head",{}).get("sha")!=target: fail("PR identity or reviewed head changed")
 if comment.get("user",{}).get("login")!="PunterDev" or comment.get("author_association")!="OWNER" or comment.get("issue_url")!=f"https://api.github.com/repos/{REPO}/issues/{number}": fail("owner approval provenance is invalid")
 body=comment.get("body",""); required=("OWNER_AUTHORIZATION: APPROVE",target,digest,EXPIRY,"OWNER_ATTESTED_WITH_LIMITATIONS","LIVE_CONTAINER_FILES_NOT_INDEPENDENTLY_VERIFIED","HOSTED_JVM_PROPERTIES_NOT_INDEPENDENTLY_VERIFIED","FUTURE_CONFIGURATION_DRIFT_NOT_INDEPENDENTLY_VERIFIED")
 if any(term not in body for term in required): fail("owner approval comment is incomplete")
 observed={(m.group(1),m.group(2)) for m in re.finditer(r"(?m)^\\s*-\\s*(CVE-\\d{4}-\\d+)\\s*\\|\\s*(pkg:maven/[A-Za-z0-9._-]+/[A-Za-z0-9._-]+@[A-Za-z0-9._-]+)\\s*$",body)}
 if observed!=TUPLES: fail("owner approval comment tuple scope is not exact")
 now=datetime.now(UTC); updated=stamp(comment["updated_at"])
 if updated>stamp(EXPIRY) or now-updated>MAX_APPROVAL_AGE: fail("owner approval is expired or stale")
 git("fetch","--no-tags","origin",target); blob=git("show",f"{target}:{source}",binary=True); validate_candidate(blob,digest)
 a.output.write_text(json.dumps({"repository":REPO,"prNumber":number,"targetSha":target,"candidateSha256":digest,"candidatePath":source,"expiresAt":EXPIRY,"trustedControlSha":a.control_sha,"activationIntentSha":a.activation_sha},sort_keys=True)+"\n")
if __name__=="__main__": main()
