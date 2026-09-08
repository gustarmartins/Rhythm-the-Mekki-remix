#!/usr/bin/env python3
"""Validate that plural translations never carry format placeholders the app
cannot fill.

The app renders plurals with `Resources.getQuantityString(id, quantity, args...)`
using the same arguments for every locale. If a translation contains a format
specifier that is absent from the English source, `String.format` throws
`IllegalFormatException` (e.g. MissingFormatArgumentException for a leftover
``%2$s``, or IllegalFormatConversionException when the conversion type does not
match the argument at that position) and the app crashes on non-English locales.

Each specifier is reduced to (positional index -> conversion character), so
reordering (``%2$s %1$d`` vs ``%1$d %2$s``) is fine, indexed and unindexed forms
(``%d`` vs ``%1$d``) are equivalent, but extras and type swaps are flagged.

Usage:
    python3 scripts/check_plural_specifiers.py                 # check every locale
    python3 scripts/check_plural_specifiers.py --files a b ... # check given files

Exit code 0 = clean, 1 = at least one problem found.
"""
import argparse
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

# Translations contain non-ASCII text; ensure output is UTF-8 even on
# terminals that default to another encoding (e.g. Windows cp1252).
for stream in (sys.stdout, sys.stderr):
    try:
        stream.reconfigure(encoding="utf-8")
    except (AttributeError, ValueError):
        pass

RES = Path("app/src/main/res")
SOURCE = RES / "values" / "strings.xml"
# Matches "%1$d", "%d", "%2$-5s", "%1$.1f" style specifiers; never "%%".
# NOTE: the space flag is deliberately excluded - allowing it lets "%% word"
# (escaped percent followed by a word) be misread as an unindexed specifier.
SPEC_RE = re.compile(r"%(?P<idx>\d+\$)?[-#+0,(<]*\d*(?:\.\d+)?(?P<conv>[A-Za-z])")

# Locale directories that may carry translated plurals (skip the source file and
# non-language variants such as values-night / values-v31).
def is_locale_dir(path: Path) -> bool:
    return (
        path.is_dir()
        and path.name.startswith("values")
        and path.name not in ("values", "values-night", "values-v31")
    )


def parse_plurals(path: Path) -> dict:
    tree = ET.parse(path)
    plurals = {}
    for plur in tree.getroot().findall("plurals"):
        items = {}
        for item in plur:
            items[item.get("quantity")] = item.text or ""
        plurals[plur.get("name")] = items
    return plurals


def specifier_map(text: str) -> dict:
    """Return {positional_index: conversion_char} for all specifiers.

    Unindexed specifiers (%d, %s) receive implicit indices following the largest
    explicitly used index, matching how String.format assigns arguments when
    explicit and implicit specifiers are mixed (and avoiding index collisions).
    """
    matches = list(SPEC_RE.finditer(text))
    explicit = [int(m.group("idx")[:-1]) for m in matches if m.group("idx")]
    next_implicit = max(explicit) + 1 if explicit else 1
    result = {}
    for m in matches:
        raw_idx = m.group("idx")
        if raw_idx:
            index = int(raw_idx[:-1])
        else:
            index = next_implicit
            next_implicit += 1
        result[index] = m.group("conv")
    return result


def compare_units(path, name, qty_label, src_text, tr_text, problems, warnings):
    src = specifier_map(src_text)
    tr = specifier_map(tr_text)
    for index in sorted(tr):
        if index not in src:
            problems.append(
                f"{path.name:>24} {name:<48} [{qty_label}] extra placeholder "
                f"%{index}${tr[index]} - source has none (app supplies no argument "
                f"for it): {tr_text}"
            )
        elif src[index] != tr[index]:
            problems.append(
                f"{path.name:>24} {name:<48} [{qty_label}] conversion mismatch at "
                f"%{index}$: source uses %{index}${src[index]} but translation uses "
                f"%{index}${tr[index]} (IllegalFormatConversionException risk): {tr_text}"
            )
    for index in src:
        if index not in tr:
            warnings.append(
                f"{path.name:>24} {name:<48} [{qty_label}] missing placeholder "
                f"%{index}${src[index]} that the source has"
            )


def check_file(path: Path, source: dict):
    problems = []
    warnings = []
    try:
        translated = parse_plurals(path)
    except ET.ParseError as exc:
        problems.append(f"{path}: not valid XML: {exc}")
        return problems, warnings
    for name, src_items in source.items():
        if name not in translated:
            continue
        for qty, src_text in src_items.items():
            if qty in translated[name]:
                compare_units(path, name, qty, src_text, translated[name][qty],
                              problems, warnings)
        # Variants the translation defines but the source does not (e.g. "zero",
        # "many") are still selectable at runtime, so compare them against the
        # source's default ("other") variant.
        src_other = src_items.get("other")
        if src_other is not None:
            for qty, tr_text in translated[name].items():
                if qty not in src_items:
                    compare_units(path, name, f"{qty} (vs source 'other')",
                                  src_other, tr_text, problems, warnings)
    return problems, warnings


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--files", nargs="*", default=None,
                    help="translation files to check (default: all locale files)")
    args = ap.parse_args()

    if not SOURCE.exists():
        print(f"ERROR: source strings not found at {SOURCE}")
        return 2

    source = parse_plurals(SOURCE)
    if args.files is not None:
        # Explicit --files: an empty list means "nothing to check" (all files
        # were skipped upstream), not "check everything".
        files = [Path(f) for f in args.files]
    else:
        files = [d / "strings.xml" for d in sorted(p for p in RES.iterdir() if is_locale_dir(p))]

    problems = []
    warnings = []
    for path in files:
        if not path.exists():
            continue
        p, w = check_file(path, source)
        problems.extend(p)
        warnings.extend(w)

    for warning in warnings:
        print(f"warning: {warning}")
    for problem in problems:
        print(problem)
    if problems:
        print(f"\nFAIL: {len(problems)} problem(s) - plural translations do not match "
              f"the English source placeholders and could crash the app. Fix them in "
              f"Weblate (or the repo).")
        return 1
    print("OK: all plural translations match the English source placeholders.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
