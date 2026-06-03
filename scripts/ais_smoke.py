"""Smoke-test AIS Stream against the live WS. Reads .key from repo root.

Run from the avimar repo root: `python3 scripts/ais_smoke.py [duration_seconds]`.
Prints a per-MessageType frame count + a sample envelope, then exits.
"""
from __future__ import annotations

import asyncio
import json
import pathlib
import sys
from collections import Counter

import websockets


REPO_ROOT = pathlib.Path(__file__).resolve().parent.parent
KEY_PATH = REPO_ROOT / ".key"
WS_URL = "wss://stream.aisstream.io/v0/stream"
# Northern Adriatic — busy enough to get traffic within seconds, small enough
# to avoid the global firehose.
BBOX = [[[44.0, 12.0], [46.0, 15.0]]]


async def run(duration_s: float) -> int:
  api_key = KEY_PATH.read_text().strip()
  if not api_key:
    print("no api key in .key", file=sys.stderr)
    return 2

  counts: Counter[str] = Counter()
  sample: dict | None = None

  async with websockets.connect(WS_URL) as ws:
    sub = {
      "APIKey": api_key,
      "BoundingBoxes": BBOX,
      "FilterMessageTypes": [
        "PositionReport",
        "ShipStaticData",
        "StandardClassBPositionReport",
        "ExtendedClassBPositionReport",
      ],
    }
    await ws.send(json.dumps(sub))
    print(f"subscribed bbox={BBOX} for {duration_s:.0f}s", file=sys.stderr)

    async def consume() -> None:
      nonlocal sample
      async for raw in ws:
        msg = json.loads(raw)
        if "error" in msg:
          raise RuntimeError(f"server error: {msg['error']}")
        mt = msg.get("MessageType", "?")
        counts[mt] += 1
        if sample is None:
          sample = msg

    try:
      await asyncio.wait_for(consume(), timeout=duration_s)
    except asyncio.TimeoutError:
      pass

  print(f"\nframes by type ({sum(counts.values())} total):")
  for mt, n in counts.most_common():
    print(f"  {mt:32s} {n}")
  if sample is not None:
    print("\nsample envelope:")
    print(json.dumps(sample, indent=2)[:2000])
  return 0


if __name__ == "__main__":
  duration = float(sys.argv[1]) if len(sys.argv) > 1 else 15.0
  sys.exit(asyncio.run(run(duration)))
