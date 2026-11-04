"""모드팩 묶음(.mrpack) 만들기.

Modrinth App · Prism Launcher 등에서 파일 하나로 가져오면 Fabric + 우리 모드 + 편의 모드가 한 번에 깔린다.
편의 모드는 Modrinth에서 Minecraft 버전 · Fabric에 맞는 최신 파일을 찾아 그 주소와 해시를 적는다 (파일 자체는 넣지 않음).
우리 모드 jar는 overrides/mods/ 안에, pack/ 아래 파일(편의 모드 설정)은 overrides/ 안에 들어간다.

    python tools/make_mrpack.py build/libs/sanupcraft-0.1.0.jar 0.1.0
    → build/sanupcraft-0.1.0.mrpack

충돌 확인용으로 편의 모드 jar도 받으려면 (fabric-api 제외, sha512 확인):
    python tools/make_mrpack.py build/libs/sanupcraft-0.1.0.jar 0.1.0 --download build/packmods
    → build/packmods/all/ (전부), build/packmods/server/ (서버에도 넣는 것)
"""
import hashlib
import json
import os
import shutil
import sys
import urllib.parse
import urllib.request
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MINECRAFT = "26.3"
FABRIC_LOADER = "0.20.1"

# (Modrinth 프로젝트, 서버에도 필요한가, 설명)
MODS = [
    ("fabric-api", True, "필수 라이브러리"),
    ("lithium", True, "서버 · 게임 최적화"),
    ("ferrite-core", True, "메모리 절약"),
    ("sodium", False, "렌더링 최적화"),
    ("entityculling", False, "안 보이는 엔티티 안 그리기"),
    ("modmenu", False, "모드 목록 화면"),
    ("xaeros-minimap", False, "미니맵 · 웨이포인트"),
    ("xaeros-world-map", False, "전체 지도"),
    ("simple-voice-chat", True, "거리 기반 음성 채팅 (서버 UDP 24454 열기)"),
    ("player-corpse", True, "죽은 자리에 시체 (주인만 열 수 있음, 남은 시간 표시)"),
    ("jei", False, "레시피 보기"),
]

USER_AGENT = "yh-yeop/sanupcraft (mrpack builder)"


def api(path, **params):
    query = urllib.parse.urlencode({k: json.dumps(v) for k, v in params.items()})
    req = urllib.request.Request(f"https://api.modrinth.com/v2/{path}?{query}", headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=30) as res:
        return json.load(res)


def latest_file(project):
    versions = api(f"project/{project}/version", loaders=["fabric"], game_versions=[MINECRAFT])
    if not versions:
        raise SystemExit(f"{project}: Minecraft {MINECRAFT} Fabric 버전이 없음")
    # 정식 → 베타 → 알파 순으로, 같은 등급이면 최신
    rank = {"release": 0, "beta": 1, "alpha": 2}
    best_rank = min(rank.get(v["version_type"], 3) for v in versions)
    candidates = [v for v in versions if rank.get(v["version_type"], 3) == best_rank]
    chosen = max(candidates, key=lambda v: v["date_published"])
    file = next((f for f in chosen["files"] if f["primary"]), chosen["files"][0])
    return chosen, file


def main():
    jar, version = sys.argv[1], sys.argv[2]
    files = []
    lines = []
    for project, server, note in MODS:
        chosen, file = latest_file(project)
        files.append({
            "path": f"mods/{file['filename']}",
            "hashes": {"sha1": file["hashes"]["sha1"], "sha512": file["hashes"]["sha512"]},
            "env": {"client": "required", "server": "required" if server else "unsupported"},
            "downloads": [file["url"]],
            "fileSize": file["size"],
        })
        lines.append(f"| {project} | {chosen['version_number']} | {'O' if server else '-'} | {note} |")
        print(f"{project}: {chosen['version_number']} ({chosen['version_type']})")

    index = {
        "formatVersion": 1,
        "game": "minecraft",
        "versionId": version,
        "name": "산업크래프트",
        "summary": "친구들과 같이 하는 모드팩: 보스 전리품이 공장을 열고, 공장이 모험대를 무장시킨다",
        "files": files,
        "dependencies": {"minecraft": MINECRAFT, "fabric-loader": FABRIC_LOADER},
    }
    out = os.path.join(ROOT, "build", f"sanupcraft-{version}.mrpack")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("modrinth.index.json", json.dumps(index, ensure_ascii=False, indent=2))
        z.write(jar, f"overrides/mods/{os.path.basename(jar)}")
        # pack/ 아래 파일(편의 모드 설정 등)은 인스턴스 폴더에 그대로 깔린다
        pack = os.path.join(ROOT, "pack")
        for base, _, names in os.walk(pack):
            for name in names:
                path = os.path.join(base, name)
                z.write(path, "overrides/" + os.path.relpath(path, pack).replace(os.sep, "/"))

    # 릴리스 본문에 붙일 모드 표
    table = os.path.join(ROOT, "build", "mrpack-mods.md")
    with open(table, "w", encoding="utf-8", newline="\n") as f:
        f.write("| 모드 | 버전 | 서버에도 | 하는 일 |\n|---|---|---|---|\n" + "\n".join(lines) + "\n")
    print(out)

    if "--download" in sys.argv:
        download(files, sys.argv[sys.argv.index("--download") + 1])


def download(files, target):
    """편의 모드 jar를 받아 all/ · server/ 로 나눈다. fabric-api는 개발 환경에 이미 있으므로 뺀다."""
    for sub in ("all", "server"):
        shutil.rmtree(os.path.join(target, sub), ignore_errors=True)
        os.makedirs(os.path.join(target, sub))
    for f in files:
        name = os.path.basename(f["path"])
        if name.startswith("fabric-api"):
            continue
        req = urllib.request.Request(f["downloads"][0], headers={"User-Agent": USER_AGENT})
        with urllib.request.urlopen(req, timeout=60) as res:
            data = res.read()
        if hashlib.sha512(data).hexdigest() != f["hashes"]["sha512"]:
            raise SystemExit(f"{name}: sha512가 맞지 않음")
        with open(os.path.join(target, "all", name), "wb") as out:
            out.write(data)
        if f["env"]["server"] == "required":
            shutil.copy(os.path.join(target, "all", name), os.path.join(target, "server", name))
    print(f"받음: {target}")


if __name__ == "__main__":
    main()
