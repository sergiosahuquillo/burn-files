#!/usr/bin/env python3
"""Fail when a string resource key exists in one language and not in the other.

Usage: check_strings.py <res-dir> [--base values] [--locales values-es ...]

Compares the names of <string>, <plurals> and <string-array> resources (and the
quantities of each <plurals>) between the base folder (English) and every locale
folder. Resources marked translatable="false" only need to exist in the base.
"""

from __future__ import annotations

import argparse
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

RESOURCE_TAGS = ("string", "plurals", "string-array")


def collect(folder: Path) -> dict[str, set[str]]:
    """Return {"string:name": set(), "plurals:name": {"one", "other"}, ...}."""
    keys: dict[str, set[str]] = {}
    for xml_file in sorted(folder.glob("*.xml")):
        root = ET.parse(xml_file).getroot()
        if root.tag != "resources":
            continue
        for element in root:
            if element.tag not in RESOURCE_TAGS:
                continue
            if element.get("translatable") == "false":
                continue
            key = f"{element.tag}:{element.get('name')}"
            if key in keys:
                raise SystemExit(f"Duplicate resource {key} in {folder.name}")
            quantities = {item.get("quantity", "") for item in element} if element.tag == "plurals" else set()
            keys[key] = quantities
    return keys


def compare(base_name: str, base: dict[str, set[str]], other_name: str, other: dict[str, set[str]]) -> list[str]:
    problems = []
    for key in sorted(base.keys() - other.keys()):
        problems.append(f"{other_name}: missing {key}")
    for key in sorted(other.keys() - base.keys()):
        problems.append(f"{base_name}: missing {key} (present in {other_name})")
    for key in sorted(base.keys() & other.keys()):
        if key.startswith("plurals:") and "other" not in other[key]:
            problems.append(f"{other_name}: {key} lacks quantity 'other'")
        if key.startswith("plurals:") and "other" not in base[key]:
            problems.append(f"{base_name}: {key} lacks quantity 'other'")
    return problems


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("res_dir", type=Path)
    parser.add_argument("--base", default="values")
    parser.add_argument("--locales", nargs="*", default=["values-es"])
    args = parser.parse_args()

    base_dir = args.res_dir / args.base
    if not base_dir.is_dir():
        print(f"Base folder not found: {base_dir}", file=sys.stderr)
        return 2
    base = collect(base_dir)

    problems: list[str] = []
    for locale in args.locales:
        locale_dir = args.res_dir / locale
        if not locale_dir.is_dir():
            problems.append(f"Locale folder not found: {locale_dir}")
            continue
        problems += compare(args.base, base, locale, collect(locale_dir))

    if problems:
        print("String resources out of sync:", file=sys.stderr)
        for problem in problems:
            print(f"  - {problem}", file=sys.stderr)
        return 1
    print(f"OK: {len(base)} resources in sync across {args.base} and {', '.join(args.locales)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
