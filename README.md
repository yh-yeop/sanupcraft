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
```

결과 jar는 `build/libs/sanupcraft-<버전>.jar`. GitHub Actions도 푸시마다 같은 `build`를 돌린다.

## 친구들 설치 방법

첫 마일스톤이 끝나면 릴리스로 모드 jar와 모드팩 묶음(가져다 쓰는 편의 모드 목록 포함)을 올린다.

## 라이선스

MIT
