import os
import re
import sys
import subprocess

# Path constants
ROOT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CHANGELOG_PATH = os.path.join(ROOT_DIR, "docs", "CHANGELOG.md")
BUILD_GRADLE_PATH = os.path.join(ROOT_DIR, "app", "build.gradle.kts")
BANNER_HINT_PATH = os.path.join(ROOT_DIR, ".release_banner_url")

DEFAULT_BETA_BANNER = "https://github.com/user-attachments/assets/52d9f9b9-722e-4e66-abab-2dcbf59b6648"
DEFAULT_STABLE_BANNER = "https://github.com/user-attachments/assets/f307174a-ec2e-41ec-b274-0a458123d4f7"

def parse_gradle_version_info():
    version_name = "Unknown"
    version_code = "Unknown"
    if os.path.exists(BUILD_GRADLE_PATH):
        with open(BUILD_GRADLE_PATH, "r", encoding="utf-8") as f:
            content = f.read()
        name_match = re.search(r"versionName\s*=\s*(?:overrideVersionName\s*\?:\s*)?\"(.*?)\"", content)
        if name_match:
            version_name = name_match.group(1)
        code_match = re.search(r"versionCode\s*=\s*(?:overrideVersionCode\s*\?:\s*)?(\d+)", content)
        if code_match:
            version_code = code_match.group(1)
    return version_name, version_code

def extract_release_notes(tag_name):
    # Normalize tag name, e.g. "v5.1.412.1078-beta" -> "5.1.412.1078"
    version_numbers = re.search(r"(\d+\.\d+\.\d+\.\d+)", tag_name)
    if not version_numbers:
        version_numbers = re.search(r"(\d+\.\d+\.\d+)", tag_name)
        
    if not version_numbers:
        print(f"Could not parse version numbers from tag: {tag_name}")
        return ""
        
    version_str = version_numbers.group(1)
    
    if not os.path.exists(CHANGELOG_PATH):
        print(f"Changelog file not found at: {CHANGELOG_PATH}")
        return ""
        
    with open(CHANGELOG_PATH, "r", encoding="utf-8") as f:
        content = f.read()
        
    version_esc = re.escape(version_str)
    pattern = rf"##\s*\[\s*v?{version_esc}.*?\][^\n]*\n(.*?)(?=\n##\s*\[|\Z)"
    
    match = re.search(pattern, content, re.DOTALL | re.IGNORECASE)
    if match:
        return match.group(1).strip()
    return ""


def load_banner_url(is_beta):
    """Load banner URL from .release_banner_url hint file, or return the default."""
    if os.path.exists(BANNER_HINT_PATH):
        with open(BANNER_HINT_PATH, "r", encoding="utf-8") as f:
            url = f.read().strip()
        # Empty file means "no banner"
        return url if url else None
    # No hint file — use defaults
    return DEFAULT_BETA_BANNER if is_beta else DEFAULT_STABLE_BANNER


def render_banner_html(banner_url):
    """Return an HTML snippet for the banner with rounded corners."""
    if not banner_url:
        return None
    return (
        '<p align="center">\n'
        f'  <img src="{banner_url}" alt="Release Banner"\n'
        '       style="border-radius: 16px; width: 100%; max-width: 960px;" />\n'
        '</p>'
    )

def clean_changelog_content(raw_notes):
    lines = raw_notes.splitlines()
    cleaned_items = []
    current_category = "Added"
    has_translation = False
    
    for line in lines:
        line = line.strip()
        if not line:
            continue
            
        cat_match = re.match(r"^###\s*(.*)", line)
        if cat_match:
            current_category = cat_match.group(1).strip()
            continue
            
        if line.startswith("-") or line.startswith("*") or line.startswith("•"):
            item_text = re.sub(r"^[-*•]\s*", "", line)
            if item_text and item_text != "-":
                # Collapse all translation/l10n lines into one
                if re.search(r"l10n|translation|localiz|update.*lang", item_text, re.IGNORECASE):
                    has_translation = True
                    continue
                cleaned_items.append(f"- **{current_category}:** {item_text}")

    if has_translation:
        cleaned_items.append("- **Added:** Updated translations")
                
    return cleaned_items

def get_commits_between_tags(current_tag, previous_tag=None):
    try:
        if not previous_tag:
            # Get list of tags sorted by version
            tags_output = subprocess.check_output(
                ["git", "tag", "--sort=-v:refname"],
                stderr=subprocess.DEVNULL
            ).decode("utf-8").strip().splitlines()
            
            tags = [t.strip() for t in tags_output if t.strip()]
            
            if current_tag in tags:
                idx = tags.index(current_tag)
                # Find the next older tag
                if idx + 1 < len(tags):
                    previous_tag = tags[idx + 1]
                
        if previous_tag:
            log_cmd = ["git", "log", f"{previous_tag}..{current_tag}", "--oneline"]
            print(f"Fetching commits between {previous_tag} and {current_tag}")
        else:
            log_cmd = ["git", "log", f"{current_tag}", "--oneline"]
            print(f"Fetching all commits up to {current_tag}")
            
        log_output = subprocess.check_output(log_cmd).decode("utf-8").strip()
        if not log_output:
            return []
            
        commits = []
        has_translation = False
        for line in log_output.splitlines():
            parts = line.split(" ", 1)
            if len(parts) > 1:
                msg = parts[1].strip()
                if msg.startswith("Merge branch") or msg.startswith("Merge pull request") or msg.startswith("Release "):
                    continue
                # Collapse all translation/l10n commits into one line
                if re.search(r"l10n|translation|chore\(l10n\)|update.*translation|localiz", msg, re.IGNORECASE):
                    has_translation = True
                    continue
                commits.append(msg)
        if has_translation:
            commits.append("Updated translations")
        return commits
    except Exception as e:
        print(f"Error fetching commits between tags: {e}")
        return []

def main():
    if len(sys.argv) < 2:
        print("Usage: python generate_release_notes.py <tag_name> [commit_sha] [previous_tag]")
        sys.exit(1)
        
    tag_name = sys.argv[1]
    commit_sha = sys.argv[2] if len(sys.argv) > 2 else os.environ.get("GITHUB_SHA", "unknown")
    custom_prev_tag = sys.argv[3] if len(sys.argv) > 3 else None
    
    is_beta = "beta" in tag_name.lower() or "alpha" in tag_name.lower() or "rc" in tag_name.lower()
    
    print(f"Generating release notes for tag: {tag_name} (IsBeta={is_beta})")
    raw_notes = extract_release_notes(tag_name)
    
    bullets = []
    if raw_notes:
        bullets = clean_changelog_content(raw_notes)
        if not bullets:
            # Fallback to general bullet lines if no categories matched
            for line in raw_notes.splitlines():
                line = line.strip()
                if line.startswith("-") or line.startswith("*") or line.startswith("•"):
                    bullets.append(line)
                    
    if not bullets:
        print("No changelog entries found in docs/CHANGELOG.md. Fetching commits since the previous tag...")
        commits = get_commits_between_tags(tag_name, custom_prev_tag)
        if commits:
            bullets = [f"- **Added:** {c}" for c in commits]
        else:
            bullets = ["- **Added:** Minor bug fixes and performance improvements."]
        
    version_name, version_code = parse_gradle_version_info()
    
    version_match = re.search(r"(\d+\.\d+\.\d+\.\d+)", tag_name)
    version_str = version_match.group(1) if version_match else version_name.split(" ")[0]

    major_minor = "5.1"
    mm_match = re.search(r"(\d+\.\d+)", tag_name)
    if mm_match:
        major_minor = mm_match.group(1)
    
    build_num = "1078"
    build_match = re.search(r"\.(\d+)(?:-|$)", tag_name)
    if build_match:
        build_num = build_match.group(1)
    elif version_name != "Unknown":
        parts = version_name.split(" ")[0].split(".")
        if len(parts) >= 4:
            build_num = parts[3]
            
    github_notes = []
    repository = os.environ.get("GITHUB_REPOSITORY", "").lower()
    is_fork = repository == "gustarmartins/rhythm-the-mekki-remix"
    channel = "Beta" if is_beta else "Stable"
    
    # Load banner URL (respects prepare_release.py choice, or uses default)
    banner_url = load_banner_url(is_beta)
    banner_html = render_banner_html(banner_url)

    if is_fork:
        # Keep the fork's release page short and self-contained. The old notes
        # template linked to upstream downloads and sponsors, which is confusing
        # when the APK and updater target this repository instead.
        release_url = "https://github.com/gustarmartins/Rhythm-the-Mekki-remix/releases"
        github_notes.append(f"# Rhythm Bluetooth Lyrics — {version_str} ({channel})\n")
        github_notes.append(
            "A small fork of [Rhythm](https://github.com/cromaguy/Rhythm) that keeps the "
            "upstream app and adds fork-specific Bluetooth lyrics defaults.\n"
        )
        github_notes.append("## What's changed")
        github_notes.extend(bullets or ["- **Changed:** Fork release and update metadata."])
        github_notes.append(
            "- **Updates:** The GitHub build checks this fork's Releases page for stable updates."
        )
        github_notes.append("")
        github_notes.append("## Downloads")
        github_notes.append(
            "Download the APK matching your device ABI. The **arm64-v8a** build is the best "
            "choice for the Poco F4; the **universal** build works on most devices."
        )
        github_notes.append(
            "Checksums are provided beside each APK. The GitHub APK is the update-enabled "
            "build; the F-Droid flavor intentionally does not poll GitHub Releases."
        )
        github_notes.append("")
        github_notes.append("## Update notes")
        github_notes.append(
            "- Existing settings remain authoritative. The new defaults do not overwrite an "
            "explicit choice."
        )
        github_notes.append(
            "- Keep a backup before updating. Android may require notification permission for "
            "update announcements."
        )
        github_notes.append("")
        github_notes.append("## Source and license")
        source_url = f"https://github.com/{repository}" if repository else release_url
        github_notes.append(f"- Source: [gustarmartins/Rhythm-the-Mekki-remix]({source_url})")
        github_notes.append("- Upstream: [cromaguy/Rhythm](https://github.com/cromaguy/Rhythm)")
        github_notes.append(
            "- Licensed under [GNU GPLv3](https://github.com/cromaguy/Rhythm/blob/main/LICENSE)."
        )
    else:
        # Preserve the upstream-compatible format if this script is reused in a
        # non-fork checkout.
        if is_beta:
            github_notes.append(f"# Rhythm {major_minor} - Bug Fix Update\n")
        else:
            github_notes.append(f"# Rhythm {major_minor} - Feature Update\n")

        if banner_html:
            github_notes.append(banner_html + "\n")

        github_notes.append("**What's New:**")
        github_notes.extend(bullets)
        github_notes.append("- **Many more reported Bug Fixes, UI & Performance Improvements.**")
        github_notes.append("")
        github_notes.append("**Known Issues:**")
        github_notes.append("   - Translation contributions are being collected.")
        github_notes.append("   - Report to GitHub Issues or Community on Discord & Telegram.")
        github_notes.append("")
        github_notes.append("**Build Information:**")
        github_notes.append(f"- Build: {build_num}")
        github_notes.append(f"- Type: {channel} Release")
        github_notes.append("\n---\n")
        github_notes.append("> [!NOTE]")
        github_notes.append("> **Important Update Notes**")
        github_notes.append("> * Turn on **Auto-Backup** so that you can recover your data.")
        github_notes.append("> * Manage APIs from **Settings**.\n")
    
    release_notes_content = "\n".join(github_notes)
    
    output_path = os.path.join(ROOT_DIR, "release_notes.md")
    with open(output_path, "w", encoding="utf-8") as f:
        f.write(release_notes_content)
        
    print(f"Generated release notes file at: {output_path}")
    
if __name__ == "__main__":
    main()
