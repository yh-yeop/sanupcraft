# 산업크래프트 (sanupcraft)

친구들과 같이 하는 자작 마인크래프트 모드팩. 최대 10명.
**모험대는 보스를 잡아 공장의 다음 단계를 열고, 공장은 모험대를 무장시킨다.**

```
   모험대 ── 보스 전리품 · 희귀 재료 ──▶ 공장
     ▲                                  │
     └──── 장비 · 탄약 · 이동 수단 ◀──────┘
```

- 기획: [Plans 레포 · 추천 기획 2번](https://github.com/yh-yeop/Plans) (기획 결정은 거기, 할 일은 이 레포 [ROADMAP.md](ROADMAP.md))
- 기준: **같이 할 때 재밌는지가 먼저.** 완성도 · 배포보다 플레이테스트 반응.

## 버전

| 항목 | 버전 |
|---|---|
| Minecraft (Java) | 26.3 |
| Fabric Loader | 0.20.1 |
| Fabric API | 0.165.0+26.3 |
| Java | 25 (Gradle이 자동으로 받음) |

## 게임 안 흐름 (M1)

1. **공장**: 분쇄기(원석 → 철가루 2) → 화로(철가루 → 주괴) → 압착기(주괴 → 철판) → 조립대(철판 → 장비 부품). 기계는 화로 연료로 돈다.
2. **장비**: 장비 부품으로 강철 갑옷 · 강철 검 (바닐라 철 장비와 같은 모양).
3. **보스**: 수호자 제단(철판 + 철 블록 + 석재 벽돌)에 수호자 소환석(철판 + 장비 부품 + 레드스톤 블록)을 쓰면 녹슨 수호자.
   강철 한 벌이 없으면 녹 공격이 1.5배 아프고 느려진다. 강철 검은 1.5배.
4. **다음 단계**: 수호자의 핵으로 강화 기계 틀 (M2 기계 재료).

자세한 수치는 [ROADMAP.md](ROADMAP.md)의 M1 표.

![분쇄기 · 압착기 · 조립대 · 수호자 제단, 강철 갑옷, 녹슨 수호자](docs/m1-scene.png)

## 구조

모드는 jar 하나이고, 시스템은 패키지로 나눈다. 커지면 그때 모드를 쪼갠다.

| 패키지 (`io.github.yhyeop.sanupcraft.*`) | 하는 일 |
|---|---|
| `core` | 공용 아이템 · 재료 등록, 보스 전리품 ↔ 기계 레시피 연결 |
| `factory` | 기계 · 운반 · 동력. 단계별로 열림 |
| `boss` | 보스 AI · 패턴, 던전 구조물, 전리품 |
| `gear` | 모험대 장비 · 역할 |
| `progress` | 단계 해금 |

`src/main` 공통 · 서버, `src/client` 클라이언트 전용(렌더링), `src/gametest` 자동 검사.

## 개발

JDK를 따로 깔 필요 없다. Gradle이 Java 25를 `~/.gradle/jdks`에 받아서 쓴다 (`gradle/gradle-daemon-jvm.properties`).

```bash
./gradlew build          # 컴파일 + GameTest (서버를 화면 없이 띄워 검사)
./gradlew runGameTest    # GameTest만
./gradlew runClient      # 개발용 클라이언트 실행
./gradlew runServer      # 개발용 서버 실행
./gradlew runClientGameTest  # 진짜 클라이언트를 띄워 스크린샷 (build/run/clientGameTest/screenshots/)
python tools/make_textures.py --sheet  # 임시 도트 텍스처 다시 만들기 + 미리보기
```

`runClientGameTest`는 창을 띄우므로 Actions의 `build`에는 넣지 않았다. 화면에 보이는 것을 바꿨을 때 손으로 돌린다.

결과 jar는 `build/libs/sanupcraft-<버전>.jar`. GitHub Actions도 푸시마다 같은 `build`를 돌린다.

## 친구들 설치 방법

[릴리스](https://github.com/yh-yeop/sanupcraft/releases)에서 받는다.

- **플레이하는 사람**: `sanupcraft-<버전>.mrpack`을 [Modrinth App](https://modrinth.com/app) 또는 [Prism Launcher](https://prismlauncher.org/)에서 "모드팩 가져오기"로 연다. Minecraft 26.3 · Fabric · 편의 모드가 한 번에 깔린다.
- **서버 여는 사람**: [Fabric 서버](https://fabricmc.net/use/server/)(26.3)를 만들고 `mods` 폴더에 `sanupcraft-<버전>.jar`와 Fabric API를 넣는다. 음성 채팅을 쓰면 Simple Voice Chat도 넣고 UDP 24454를 연다. 서버에 필요한 모드 목록은 릴리스 본문 표의 "서버에도" 칸.

편의 모드 목록은 [tools/make_mrpack.py](tools/make_mrpack.py)의 `MODS`. 릴리스할 때 Modrinth에서 26.3 Fabric용 최신 파일을 골라 묶는다.

### 릴리스 만들기

```bash
git tag v0.1.0-beta.1 && git push origin v0.1.0-beta.1
```

`v*` 태그를 올리면 Actions가 빌드 · GameTest 후 jar · mrpack · `.sha256`을 릴리스에 올린다. 하이픈이 든 태그는 프리릴리스(테스트용). 버전은 태그에서 따온다.

## 라이선스

MIT
