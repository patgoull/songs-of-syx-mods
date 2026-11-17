"""Summarize a Java Flight Recorder file from `run.ps1 -Profile`: where each thread spends its CPU time.

Usage:
  python tools\\jfr-hot.py build\\profile-1234.jfr [--thread updater] [--top 30] [--under settlement.main.SETT.render]

For each thread it prints:
  self       methods the samples were in (the code doing the work)
  inclusive  methods on the stack (the work they and everything they call did)
--under keeps only samples whose stack passes through that method, e.g. to split the city render.
--spans A,B,C prints, per thread, the wall time from the first to the last sample with A (B, C) on the stack, and how
  many samples (one per ~10 ms of Java CPU), e.g. --spans GameSaver.load,ENVIRONMENT.init,Minimap.putPixels for a load.
Needs the `jfr` tool from a JDK or JRE on PATH.
"""
import argparse
import collections
import datetime
import json
import subprocess
import sys


def frame_name(f):
    m = f["method"]
    return m["type"]["name"] + "." + m["name"]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("file")
    ap.add_argument("--thread", help="only this thread (substring of its name)")
    ap.add_argument("--top", type=int, default=25)
    ap.add_argument("--under", help="only samples with this method on the stack (substring)")
    ap.add_argument("--spans", help="comma-separated methods (substrings): wall time and samples of each")
    a = ap.parse_args()

    out = subprocess.run(["jfr", "print", "--json", "--stack-depth", "128", "--events", "jdk.ExecutionSample", a.file],
                         capture_output=True, text=True, encoding="utf-8")
    if out.returncode != 0:
        sys.exit(out.stderr)
    events = json.loads(out.stdout)["recording"]["events"]

    if a.spans:
        keys = a.spans.split(",")
        spans = collections.defaultdict(list)
        for e in events:
            v = e["values"]
            thread = (v.get("sampledThread") or {}).get("javaName") or "?"
            if a.thread and a.thread not in thread:
                continue
            frames = [frame_name(f) for f in (v.get("stackTrace") or {}).get("frames", [])]
            t = datetime.datetime.fromisoformat(v["startTime"])
            for k in keys:
                if any(k in n for n in frames):
                    spans[(thread, k)].append(t)
        for (thread, k), ts in sorted(spans.items(), key=lambda kv: min(kv[1])):
            print(f"{thread:16} {k:40} {(max(ts) - min(ts)).total_seconds():6.2f} s  {len(ts):5} samples  "
                  f"from {min(ts).time()}")
        return

    per_thread = collections.defaultdict(lambda: (collections.Counter(), collections.Counter(), [0]))
    for e in events:
        v = e["values"]
        thread = (v.get("sampledThread") or {}).get("javaName") or "?"
        if a.thread and a.thread not in thread:
            continue
        frames = [frame_name(f) for f in (v.get("stackTrace") or {}).get("frames", [])]
        if not frames:
            continue
        if a.under:
            idx = [i for i, n in enumerate(frames) if a.under in n]
            if not idx:
                continue
            frames = frames[:idx[-1] + 1]
        selfc, incl, total = per_thread[thread]
        total[0] += 1
        selfc[frames[0]] += 1
        for n in set(frames):
            incl[n] += 1

    for thread, (selfc, incl, total) in sorted(per_thread.items(), key=lambda t: -t[1][2][0]):
        n = total[0]
        if n < 20:
            continue
        print(f"\n=== thread {thread}: {n} samples")
        print("  self:")
        for name, c in selfc.most_common(a.top):
            print(f"    {100 * c / n:5.1f}%  {name}")
        print("  inclusive:")
        for name, c in incl.most_common(a.top):
            print(f"    {100 * c / n:5.1f}%  {name}")


if __name__ == "__main__":
    main()
