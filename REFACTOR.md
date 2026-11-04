# REFACTOR — 1.5단계 (밸런싱 · 고도화)

대상: `C:\App\MineCraft` (sanupcraft, Fabric 26.3) 전체. 판단이 필요한 국소 문제 — 밸런스, 로직 최적화, 단계별 플레이 흐름(UX).
단계 간 정보는 이 파일 하나로 넘긴다. 상위 단계는 이 파일과 지적된 부분만 읽는다.

## 검증 방법 (현재 동작 고정)
- `./gradlew build` — 컴파일 + GameTest 49개 (화면 없는 서버). 2026-11-04 기준 전부 통과 (`ee63e74`).
- `./gradlew runClientGameTest` — 실제 클라이언트 스크린샷 (화면 · 텍스처 · 이동 확인, 판정은 사람).
- 수정 후에는 항상 `./gradlew build`로 통과를 확인한다.

## 절차
1. Sonnet 4기가 겹치지 않는 영역을 맡아 문제를 찾는다 (A 밸런스 · 경제 / B 공장 · 자동화 로직 / C 플레이 흐름 UX / D 보스 · 전투).
2. Opus 1기가 지적을 묶어 검증하고 "수정 대상"만 판정한다. 판정마다 비판적 Sonnet이 근거와 함께 반론 1회, 최종 결정은 Opus.
3. Sonnet n기가 판정된 문제만 고친다. 같은 파일을 동시에 고치지 않는다 (각자 작업 트리). 고친 뒤 `./gradlew build`.
4. 2단계로 올리는 조건: 같은 문제를 고쳤는데 테스트가 연속으로 실패, 또는 여러 모듈에 걸쳐 설계 판단이 필요할 때.

## 1. 지적 (Sonnet 4기, 2026-11-04)
총 46건: A 10 · B 12 · C 12 · D 12. 겹치는 것: A6=B5, A10≈D6, C7≈D7, A5 일부≈B1.

### A 밸런스 · 경제 (Sonnet)
비용표 (기호: I 철 주괴, R 원석 철, C 석탄, S 강철 주괴 = R 1 + C 1, 부품 1 = 0.5 S)

| 단계 | 추가 비용 | 누적 핵 / 비고 |
|---|---|---|
| 공장 1단계 (분쇄기 · 압착기 · 합금로 · 조립대) | 25 I | 0 |
| 강철 갑옷 한 벌 | 24 S (R 24 · C 24) | 검 +2, 해머 +6, 굴삭 삽 +4 → 36 S |
| 보스 1 (제단 12 I, 소환석 4 I + 2 S + 레드스톤 9) | 환급 평균 판 6 + 부품 2 + 핵 1 | 1회 순수지 I +2, S −1 |
| 강화 기계 틀 | 4 I + 2 S + 핵 1 | |
| 보일러 · 파이프 · 벨트 32 · 모터 | 약 28 I + 3 S + 핵 1 | 2 |
| 증기 굴착기 | 13 S + 4 I + 핵 1 + 보일러 | 3 |
| 내열 한 벌 + 검 | 10 S + 조각 6 | |
| 파수꾼 소환석 | 4 I + 조각 4 + 블레이즈 막대 | |
| 심장 제련로 · 엔더 운반기 한 쌍 | 틀 3개(핵 3) + 심장 + 바닐라 재료 | |
| 전체 | 원석 철 120~150, 석탄 약 80 | **핵 약 8개 = 수호자 8번** (문서의 "4번"은 옛 숫자) |

- **A1** · `recipe/heatproof_*.json`, `gear/HeatproofGear.java` · 밸런스 · 상 — 내열 갑옷(부품 19 ≈ 9.5 S + 조각 5)이 강철 갑옷(24 S)보다 싸면서 다이아급 + 불 저항. 강철 갑옷이 보스 1 직후 쓸모없음. 제안: 내열 각 부위가 같은 부위 강철 갑옷을 재료로(투구 `GSG/GHG`, 흉갑 `GHG/GSG/GGG`, 각반 `GSG/GHG/G G`, 부츠 `SHS/G G`).
- **A2** · `recipe/heatproof_sword.json`, `HeatproofGear.HEATPROOF_TOOL` · 밸런스 · 중 — 내열 검(조각 1 + 부품 1)이 강철 검(2 S)보다 싸고 공격 7.5로 다이아(7) 위. 제안: 패턴 `S/W/G`(W = 강철 검), 공격 보너스 3.5 → 3.0.
- **A3** · `loot_table/entities/rusty_guardian.json`, `recipe/guardian_summoner.json` · 경제 · 중 — 전리품(판 4~8, 부품 1~3)이 소환석 값보다 커서 핵 파밍이 공짜에 가깝다. 제안: 판 2~4, 부품 1~2, 문서의 "4번쯤" → "핵 약 8개".
- **A4** · `recipe/ender_*.json`, `loot_table/entities/furnace_warden.json` · 고도화 · 중 — 파수꾼의 심장 쓰는 곳이 제련로 하나뿐, 두 번째 처치는 손해. 제안: 엔더 송신기 · 수신기에 심장 1개씩 → 파수꾼 3번.
- **A5** · `mining/QuarryBlockEntity.java`(`flush`), `MachineBlockEntity.canPlaceItem` · 경제 · 중 — 굴착기(초당 15블록)가 상자를 6분이면 채우고 멈춤, 분쇄기의 약 25배 속도, 흙 · 안산암이 분쇄기 입력을 막음. 제안: `sanupcraft:quarry_junk` 태그(조약돌 · 흙 · 안산암 등)는 위가 가득이면 버림, 분쇄기 입력은 레시피 재료만(B1과 겹침).
- **A6** · `power/BoilerBlockEntity.java` · 경제 · 중 — 증기 가득에도 연료가 탐(B5와 같음) → 부하가 낮으면 증기가 직결보다 나을 게 없음. `steam` 꾸러미(굴착기 2 + 모터 1 + 기계 2 = 5)에 보일러(4) 하나는 모자람. 제안: 증기 가득 · 물 없음이면 litTime 멈춤, 꾸러미 보일러 2.
- **A7** · `recipe/crushing/cobblestone.json`, `cobbled_deepslate.json` · 경제 · 하 — 조약돌 → 철 너겟 10%는 주괴 하나에 석탄 1.4개분 증기라 쓸 이유 없음. 제안: 철 너겟 0.2 · 80틱, 금 너겟 0.15 · 80틱.
- **A8** · `recipe/crushing/gold_ore.json` · 밸런스 · 하 — `#gold_ores`에 네더 금 원석 포함 → 블록 하나가 금 3개(바닐라 약 0.44). 제안: 금 · 심층암 금 원석만, 네더 금 원석은 따로 가루 2 · 200틱.
- **A9** · `gear/SteelGear.java` · 밸런스 · 하 — 해머 60%여도 칸당 약 5.4배 빠름, 근접 피해(해머 6.5 · 삽 6)가 무기급. 제안: 해머 · 삽 기본 피해 1.0, 내구도 500 → 400.
- **A10** · `RustyGuardian.MAX_HEALTH`, `FurnaceWarden.MAX_HEALTH` · 밸런스 · 중 — 체력 고정이라 10명이면 2~3초에 죽음(혼자 약 22초). 제안: 소환 때 48칸 안 인원 n으로 체력 × min(3, 1 + 0.4(n−1)). (D6과 겹침)
- 확인: 모든 중간재가 쓰임, 부산물 무한 루프 없음, 방어력 서열(철 15 < 강철 17 < 다이아 = 내열 20 < 네더라이트) 의도대로.

### B 공장 · 자동화 로직 (Sonnet)
- **B1** · `factory/MachineBlockEntity.java:289-296` (`canPlaceItemThroughFace`) · 버그 · 상 — 합금로에 깔때기로 넣으면 첫 칸이 비었을 때 무엇이든 들어가, 두 칸이 같은 재료(예: 석탄 가루 · 석탄 가루)가 되어 영구히 막힘. 첫 칸 64개 가득 + 같은 아이템이면 둘째 칸도 거부 → 교착. 테스트는 setItem으로 넣어 이 경로를 안 봄. 제안: 첫 칸은 "같은 것 있음 또는 비었고 둘째 칸에 같은 것 없음", 둘째 칸은 "같은 것 있음 또는 비었고 첫 칸에 다른 것 있음", 가능하면 레시피 재료만 받음 + 깔때기 GameTest.
- **B2** · `logistics/BeltBlockEntity.java:94` (`deliver`) · 버그 · 중 — 벨트 끝에 기계를 두면 옆면 삽입이라 연료 칸에만 들어감(원석은 거부되어 벨트 끝에 쌓임). README · ROADMAP의 "굴착기 → 벨트 → 분쇄기"와 다름. 제안: 대상이 기계면 위(UP) 면으로 삽입 + 기계 앞 벨트 GameTest.
- **B3** · `mining/QuarryBlockEntity.java:110` (`Block.getDrops(... TOOL)`) · 익스플로잇 · 중 — 도구 등급을 확인하지 않아 흑요석 · 고대 잔해 등이 철 등급 굴착기로 다 나옴(고대 잔해 = 심장 제련로 재료). 섬세한 손길 블록(유리 · 얼음)은 드롭 없이 사라짐. 제안: `requiresCorrectToolForDrops && !TOOL.isCorrectToolForDrops`면 건너뜀, 드롭이 비면 건너뜀 여부 결정.
- **B4** · `mining/QuarryBlockEntity.java:26` (`pending`) · 버그 · 중 — 굴착기를 부수면 위로 못 보낸 pending이 사라짐(Container가 아니라 자동 드롭 없음). 제안: `preRemoveSideEffects`에서 pending 떨어뜨림.
- **B5** · `power/BoilerBlockEntity.java:56-76` · 고도화 · 중 — 증기가 가득 차도 연료가 계속 탐(수요 없는 라인에서 석탄 1600틱 낭비). 제안: 증기 가득이면 litTime을 멈춤(수요 비례 연료 소모).
- **B6** · `logistics/EnderLinkBlockEntity.java:41,97`, `EnderLinkBlock.java:52` · 익스플로잇(멀티) · 중 — 채널이 서버 전체 공유 16색, 소유자 없음: 남의 수신기에 넣고 빼기 · 채널 바꾸기 가능, 수신기 여러 개면 비결정적 선택. 제안: 채널 키 = 소유자 UUID + 색, 채널 변경은 소유자 · OP만, 결정적 선택(가까운 순 · 라운드로빈).
- **B7** · `MachineBlockEntity.java:135`, `BoilerBlockEntity.java:70` (`fuel.shrink(1)`) · 버그 · 하 — 용암 양동이를 태우면 양동이가 사라짐(바닐라는 잔여물 반환). 제안: 바닐라 consumeFuel처럼 잔여물 처리, 공용 메서드로.
- **B8** · `MachineBlockEntity.java:153-158`, `QuarryBlockEntity.java:48`, `BeltMotorBlockEntity.java:52` · 성능 · 중 — 증기가 모자라면 LIT · POWERED가 틱마다 뒤집혀 setBlockAndUpdate 반복(빛 · 동기화). 제안: 히스테리시스(꺼짐은 5~10틱 연속일 때만, 굴착기는 최근 20틱 작업이면 유지).
- **B9** · `BoilerBlockEntity.java:84,88-91`, `SteamNetwork.java:22` · 성능 · 하 — 보일러가 매 틱 setChanged, 파이프 BFS를 보일러마다 20틱마다. 제안: 바뀐 틱에만 setChanged, 이웃 변화로 BFS 무효화.
- **B10** · `EnderLinkBlockEntity.java:41,75` · 버그 · 하 — 정적 수신기 맵이 서버 종료 때 안 비워져 낡은 ServerLevel을 붙잡음(오작동은 걸러짐). 제안: SERVER_STOPPED에서 clear, 다른 서버 항목 정리.
- **B11** · `EnderLinkBlockEntity.java:133` (`setItem`) · 버그 · 하 — 수신기 자리가 일부만 있어도 송신기가 스택 전체를 받고 넘치는 걸 바닥에 떨어뜨림(벨트 위면 아이템 스팸). 제안: canPlaceItem이 스택 전체가 들어갈 자리 확인.
- **B12** · `BeltBlockEntity.java:43-56`, `BeltMotorBlockEntity.java:32` · 성능 · 하 — 모터는 벨트가 비어도 증기를 쓰고, 켜진 벨트는 양쪽에서 매 틱 엔티티 질의(32칸 = 틱당 64회). 제안: 아이템 있을 때만 스캔 · 간격 늘림, 빈 줄이면 펄스 생략, BFS는 변화 때만.
- 참고(범위 밖): 부산물 칸 가득이면 조용히 사라짐(의도), 블록 loot table이 `"condition"`(단수) 키라 survives_explosion이 무시될 가능성.

### C 플레이 흐름 UX (Sonnet)
- **C1** · advancement `crusher`/`alloy`/`iron_plate`, `recipe/alloy_smelter.json`, `recipe/press.json` · 안내 · 상 — 합금로에 철판 6(압착기) + 화로 2 + 용광로가 필요한데 게임 안 어디에도 없음. 압착기 · 철판 발전 과제 없음, `iron_plate` 과제는 실제로 강철판 감지. 제안: `press` 과제(부모 crusher, 철판 감지, "압착기(철 주괴 5 · 피스톤 · 조약돌 3)… 철판 6 + 화로 2 + 용광로로 합금로"), `alloy` 부모를 press로.
- **C2** · 기계 레시피 전체 · 안내/고도화 · 상 — 기계 레시피가 레시피북 · JEI에 안 뜸. 조약돌 · 자갈 · 뼈 · 광석 블록 분쇄를 게임 안에서 알 방법이 없음, 안 맞는 입력은 반응 없음. 제안: 기계 블록 아이템에 LORE(받는 재료 목록), crusher 설명에 한 줄, 장기적으로 JEI/EMI 플러그인.
- **C3** · `logistics/EnderLinkBlockEntity.java`, advancement `ender_link` · 안내/표시 · 상 — 로드된 수신기에만 보내므로 "네더 채굴장 → 오버월드 공장"은 오버월드 청크가 안 불러져 있으면 안 됨. 안 될 때 알림 없음(벨트 · 깔때기만 멈춤). 제안: 송신기 우클릭에 "연결된 수신기 N개", 설명에 "수신기 지역이 로드돼 있어야".
- **C4** · `logistics/EnderLinkBlock.java` · UX · 중 — 수신기는 빈손 우클릭이 상자 열기라 채널 확인 불가, 기본 채널 흰색이라 모두 이어짐. 제안: 웅크리고 빈손 우클릭이면 둘 다 채널 표시, 수신기 메뉴 제목에 채널.
- **C5** · `power/BoilerBlock.java`, lang `boiler.status`, advancement `boiler` · 안내 · 중 — 물 · 연료 넣는 법, 물 원천, 상태 확인이 게임 안에 없음. 가득 차거나 다른 연료면 메시지 없이 FAIL. 제안: 상태 줄에 부족한 것 안내, "물이 가득 찼다", 설명 교체.
- **C6** · `mining/QuarryBlock(Entity).java`, lang, advancement `quarry` · UX · 중 — 멈춤 이유(증기 · 위 상자 없음/가득 · 끝)를 구분 못 함, 시작 전 "Y 0층"으로 보임, 9x9 구덩이 위험 안내 없음. 제안: waiting / no_steam / 시작 전 상태 키, 설명에 "파이프 · 위 상자 · 구덩이 주의".
- **C7** · `RustyGuardian.java:271-274`, `FurnaceWarden.java:333`, advancement `steel`/`warden_summoner` · 표시 · 중 — 녹 · 과열 1.5배가 맞고 나서야 느껴짐(D7과 같음), 과열 규칙은 어디에도 없음, 모드 아이템 툴팁 0. 제안: 첫 피격 오버레이(쿨다운), steel · warden_summoner 설명 보강, 갑옷 · 검에 LORE 한 줄.
- **C8** · advancement `steel`/`guardian_core`, `GuardianAltarBlock.java` · 안내 · 중 — 소환 방법(소환석 들고 제단 우클릭, 소모, 48칸 한 마리)이 없음. guardian_core 설명이 "틀도 만들고 조각도" 둘 다처럼 읽힘. 핵 수요: 틀 5곳 + 조각 10 → 보스 약 8회. busy 메시지가 "수호자"만 언급. 제안: 설명 교체, busy를 "근처에 이미 보스가 있다".
- **C9** · `MachineBlockEntity.java`, `MachineScreen.java`, `MachineMenu.java` · UX · 중 — 깔때기 면 규칙이 너무 늦게 나옴(강철판 이후), 기계 화면 칸 툴팁 없음 · 증기 게이지 없음 · 재료 안 맞음/연료 없음 구분 없음, 부산물 칸이 막히면 조용히 버림. 제안: 규칙을 crusher 설명으로, 빈 칸 호버 툴팁, 증기 표시, 부산물 막힘 처리.
- **C10** · advancement `belt_motor`, `BeltBlock.java` · 안내 · 중 — 벨트 규칙(바라보는 방향 · 32칸 · 웅크리면 안 밀림 · 깔때기)이 없고 belt_motor 설명은 내열 장비 이야기. 제안: 설명 교체, 내열 이야기는 heatproof로.
- **C11** · `docs/PLAYTEST.md`, `playtest/PlaytestCommand.java` · 고도화 · 중 — 오버월드로 돌아오는 단계 없음("너무 차갑다" 확인 불가), 엔드 크로스 차원 확인 불가, start 꾸러미에 레드스톤 · 판자 없음, 문서 "GameTest 39개" 낡음. 제안: `home`(오버월드) 단계, boss1에 파수꾼 소환석 1, start에 레드스톤 4 · 판자 8, 체크리스트 항목 추가.
- **C12** · advancement `alloy`/`hammer`/`heatproof` · 안내 · 하 — 강철 요구량(갑옷 24 · 해머 6 · 검 2 ≈ 원석 철 30 + 석탄 30), 해머 60% 속도 · 단단한 블록 제외 규칙이 설명에 없음. 제안: 설명 교체.
- 문제없음 확인: ko/en 키 일치, raw_iron · crusher 설명 수치 맞음, 루트 접속 즉시 달성, 화면 칸 위치 맞음, 꾸러미는 각 단계 자체 시험에 충분.

### D 보스 · 전투 (Sonnet)
- **D1** · `boss/RustyGuardian.java:168-191` · 공정성 · 상 — 돌진 방향이 예고 마지막 틱(20)까지 대상을 추적하다 잠김. 질주로도 d≲6칸이면 못 피함, 맨몸 18 + 구속 II. 제안: 12틱에 방향 잠금, 잠긴 뒤 경로를 입자로 그림, 예고 25틱, 속도 0.9→0.75, 벽에 박히면(수평 이동 <0.1 3틱) 끝내고 20틱 경직.
- **D2** · `boss/FurnaceWarden.java:187-233` · 공정성 · 상 — 증기 분출이 발사 순간까지 추적(틱당 20도). "옆으로 돌면 피한다"는 설계와 다름. 제안: 8~10틱에 방향 고정, 남은 시간 동안 ±35° 경계를 FLAME 선으로.
- **D3** · `RustyGuardian.java:270-276`, `FurnaceWarden.java:332-339` · 익스플로잇 · 상 — 방패로 모든 패턴 · 근접이 막히고 녹 · 과열도 안 걸림 (`hurt`가 false). 세트 효과 가치를 방패가 대체. 제안: `blockedByItem` 오버라이드로 방패 무력화(쿨다운 100틱, 돌진 · 내려찍기 항상), 또는 방패 무시 피해 종류.
- **D4** · `RustyGuardian.java:224,104-113`, `FurnaceWarden.java:219,260` · 익스플로잇 · 상 — 기둥 위 · 좁은 통로에서 활로만 잡힘 (내려찍기 위 5.5칸, 분출 위 4.6칸 밖 안전, 원거리 없음, 회복 없음). 제안: 도달 불가 80틱 이상이면 반격 패턴(수직 충격파 / 상향 도약 / 접근 순간이동), 도달 불가가 길면 초당 1% 회복.
- **D5** · `RustyGuardian.java:196-236`, `FurnaceWarden.java:281-329` · 공정성 · 중 — 내려찍기 경고 고리(5.0)가 판정(5.9)보다 좁음, 입자 5틱마다 4번. 도약 착지 · 분출 범위 표시 없음. 제안: 고리 = 판정 반경, 2틱마다 36개, 도약은 예상 착지점에 고리.
- **D6** · `RustyGuardian.java:53`, `FurnaceWarden.java:47,65,137-141`, `loot_table/entities/*.json` · 고도화 · 중 — 10명이어도 체력 고정, 핵 · 심장 1개뿐(진행 필수품), 화염 장판 3명 고정. 제안: 주변 인원 n에 따라 체력 ×(1+0.6(n-1)), 기여자(피해 ≥5%) 각자 1개, 고리 수 clamp(n,3,6) + 셔플.
- **D7** · `hitWithRust` · `hitWithHeat`, `lang/ko_kr.json` · 고도화 · 중 — 세트 없이 1.5배를 맞아도 전투 중 안내 없음. 제안: 맞을 때 오버레이 "녹이 슨다! 강철 갑옷 한 벌이 필요하다" / "과열! …" (플레이어별 200틱 쿨다운) + 입자 · 소리, 소환 때 세트 없는 사람에게 한 번 안내.
- **D8** · `RustyGuardian.java:87-112`, `FurnaceWarden.java:88-115` · 익스플로잇 · 중 — 거미줄 · 구속 · 나약함이 보스에 먹힘(위더는 막음). 제안: `makeStuckInBlock` 빈 오버라이드, 구속 · 나약함 거부.
- **D9** · `RustyGuardian.java:78-91,336-338`, `FurnaceWarden.java:77-92` · 고도화 · 중 — 플레이어가 죽거나 떠나도 보스가 체력 그대로 영구히 남음(부활 반복 공략 가능, 서버에서 방치). 제안: 48칸 안 생존 플레이어 없음 600틱 → 초당 2% 회복, 소환 지점 복귀.
- **D10** · `GuardianAltarBlock.java:80-93` · 익스플로잇 · 하 — 중복 방지가 로드된 엔티티만 셈, 소환 공간 검사 없음(천장 낮으면 끼고 소환석만 소비). 제안: `noCollision` 확인 후 소비, 실패 시 "공간이 좁다".
- **D11** · `RustyGuardian.java:77-78,340-350`, `FurnaceWarden.java:77-78` · 버그 · 하 — 이름표로 바꿔도 보스 바 제목 고정. 평화로움에서 보스가 아무것도 안 하는 샌드백(소환석만 소비). 제안: `setCustomName` · 불러오기에서 `bossEvent.setName`, 평화로움이면 제단이 소환 거부.
- **D12** · `RustyGuardian.java:138-150`, `FurnaceWarden.java:157-169` · 고도화 · 하 — 고정 순환 패턴, 격노는 쿨다운만, 거리 무관 패턴 낭비. 제안: 격노 시 추가 패턴(연속 돌진 / 분출 직후 도약), 거리 조건(내려찍기 8칸 안, 돌진 4~16칸).
- 문제없음 확인: 보스 바 중복 없음, 세트 판정 정확, 패턴은 플레이어만, 낙하 · 익사 · 불 면역 정상.


## 2. 판정 (Opus) · 반론 (비판적 Sonnet) · 최종 결정

### 2-1. Opus 판정
집계(46건): **수정 대상 31** · 보류 7 · 기각 5 · 중복 3 (A6→B5, A10→D6, D7→C7). 부분 판정: A3(문서만 수정, 전리품 보류), A5(입력 필터는 B1로, 쓰레기 버림은 보류), C2(툴팁 수정, JEI 보류), C9(부산물 막힘은 의도 → 제외).
검증 메모: 블록 loot table의 `"condition"`(단수)은 26.x 정식 키(`LootPool` 코덱이 `condition` · `modifier`) → B "참고"는 오탐. 굴착기 속도는 초당 5블록(4틱에 1개)이지 15가 아님.

| ID | 판정 | 근거 (검증) | 수정 사양 |
|---|---|---|---|
| A1 | 수정 | 내열 한 벌 = 부품 19(≈ S 9.5) + 조각 5, 강철 한 벌 = S 24. 보스 1을 위해 이미 만든 강철 갑옷이 바로 버려짐. 맞음 | 내열 각 부위 = 같은 부위 강철 갑옷 + 지금 재료(부품 · 조각 수 그대로). `recipe/heatproof_helmet.json` `["GSG","GHG"]` H=`sanupcraft:steel_helmet` · `chestplate` `["GHG","GSG","GGG"]` H=steel_chestplate · `leggings` `["GSG","GHG","G G"]` H=steel_leggings · `boots` `["SHS","G G"]` H=steel_boots. 이미 강철을 가진 사람의 추가 비용은 지금과 같음 (인챈트는 제작으로 사라짐 → 툴팁 · 문서에 한 줄). 시험: 레시피 재료에 강철 부위가 들어 있나 (F4) |
| A2 | 수정 | 내열 검 공격 = 1+3+3.5 = 7.5 > 다이아 7, 재료는 강철 검(S 2)보다 쌈. 클래스 주석 "다이아 수준"과 어긋남 | `HeatproofGear.HEATPROOF_TOOL` 공격 보너스 3.5F → 3.0F (= 7, 다이아). `recipe/heatproof_sword.json` `["S","G","W"]` S=core_shard, G=gear_part, W=`sanupcraft:steel_sword` (막대 삭제). 파수꾼 1.5배 시험은 피해값을 직접 넣으므로 영향 없음 |
| A3 | 수정(문서) · 전리품 보류 | 소환석 = 철판 4 + 부품 4 + 레드스톤 블록, 전리품 평균 판 6 · 부품 2 → 거의 공짜 맞음. 다만 10명 서버에서는 핵 수요가 인원에 비례(조각) → 전리품을 줄이면 멀티가 고됨. 문서 "4번쯤"은 틀림: 틀 5곳(모터 · 굴착기 · 송신기 · 수신기 · 제련로) = 핵 5 + 조각 10 = 핵 3 → **8번** | ROADMAP("핵 조각 필요량" 줄 · "보스 1을 4번쯤" 2곳) · PLAYTEST("4번쯤") → "핵 8개(틀 5 + 조각 10 = 핵 3) ≈ 보스 1을 8번". 전리품 수치는 실테스트 시간 측정 뒤 결정 (F4) |
| A4 | 보류 | 심장 쓰임새가 제련로 하나인 건 맞음. 엔더 운반기에 넣으면 진행 순서(파수꾼 → 엔드)가 바뀌는 기획 결정 | 결정 필요: 심장의 두 번째 용도(엔더 운반기 / 다른 3단계 기계 / 없음) |
| A5 | →B1 · 보류 | "초당 15블록"은 틀림(5블록). 위가 가득이면 멈춤은 설계대로(주석 · ROADMAP). 분쇄기 입력이 흙 · 안산암으로 막히는 건 맞음 → B1 입력 필터로 해결 | 쓰레기 버림(`quarry_junk` 태그 · 필터)은 기획 결정 → 보류 |
| A6 | 중복 → B5 | | |
| A7 | 기각 | 계산은 맞으나 굴착기 한 번(9x9 × 바닥까지 ≈ 1만 블록)에 조약돌이 대량 → 10%여도 너겟 수백. 올리면 조약돌이 철의 주 공급원이 됨 | 실테스트 뒤 재검토 |
| A8 | 기각 | 광석 블록 분쇄는 섬세한 손길이 있어야 가능(굴착기 · 해머는 너겟을 떨굼). 비교 기준은 바닐라 드롭이 아니라 "섬세한 손길 + 화로 = 주괴 1" → 3배는 다른 광석 블록과 같은 규칙. 금은 모드 안 쓰임새가 없음 | |
| A9 | 기각 | 해머 근접 6.5 · 속도 1.0 = 초당 6.5 < 강철 검 10.4, 철 도끼(9)보다 약함 → "무기급" 아님. 속도 60%는 직전 커밋에서 주인이 정한 값 | 실테스트 항목 그대로 |
| A10 | 중복 → D6 | | |
| B1 | 수정 | 확인: 합금로 첫 칸이 비고 둘째 칸에 석탄 가루가 남아 있을 때 깔때기가 석탄 가루를 첫 칸에 넣음 → 두 칸이 같은 재료, 철가루는 어느 칸에도 못 들어가 **영구 정지**. 분쇄기도 깔때기 · 벨트로 아무거나 받아 흙 하나에 막힘 | `MachineBlockEntity.canPlaceItemThroughFace`: 재료 칸(INPUT · INPUT2)은 ① 이 기계 종류의 어떤 레시피 재료일 때만(`MachineRecipe.acceptsIngredient(stack)` 추가 = ingredient 또는 ingredient2가 test; 서버에서 `serverLevel.recipeAccess().getRecipes()` 중 `kind.recipeType()`만; 서버가 아니면 true) ② 합금로: INPUT은 "같은 아이템이 이미 있음" 또는 "비었고 INPUT2에 같은 아이템이 없음", INPUT2는 "같은 아이템이 이미 있음" 또는 "비었고 INPUT이 비지 않았고 INPUT과 다름". 손으로 넣는 칸(`canPlaceItem`)은 그대로. 시험: 둘째 칸에 석탄 가루 2를 두고 깔때기(석탄 가루 2 + 철가루 2) → 강철 나옴 / 분쇄기 위 깔때기에 흙 + 원석 철 → 철가루 나옴, 흙은 깔때기에 남음 |
| B2 | 수정 | 확인: `deliver`가 `facing.getOpposite()`(옆면)로 넣어 기계는 연료 칸만 받음. README · ROADMAP의 "굴착기 → 벨트 → 분쇄기"가 안 됨 | `BeltBlockEntity.deliver`: 대상이 `MachineBlockEntity`면 `Direction.UP`으로 먼저 넣고, 남은 것을 원래 면(연료)으로. 다른 대상은 그대로. 시험: 벨트 끝 분쇄기(증기 또는 연료)에 원석 철 → 철가루 |
| B3 | 수정 | 확인: `Block.getDrops(..., TOOL)`은 도구 등급을 안 봄(등급 검사는 플레이어 쪽) → 철 굴착기가 흑요석 · 고대 잔해(심장 제련로 재료)까지 캠 | `QuarryBlockEntity.mine`: `state.requiresCorrectToolForDrops() && !TOOL.isCorrectToolForDrops(state)`면 캐지 않고 false(건너뜀, 증기 안 씀). 드롭이 빈 블록(유리 · 얼음)은 지금처럼 캠(곡괭이와 같음). 시험(`alone_quarry`): 아래에 흑요석 + 돌 → 돌은 캐고 흑요석은 남음 |
| B4 | 수정 | 확인: 굴착기는 Container가 아니라 기본 `preRemoveSideEffects`가 아무것도 안 떨굼 → pending 소실(최대 블록 하나분이라 작지만 확실한 손실) | `QuarryBlockEntity.preRemoveSideEffects(pos, state)` 오버라이드: pending을 `Containers.dropItemStack`으로 떨구고 비움. 시험: 위 상자 없이 증기 → 한 블록 캔 뒤 굴착기 제거 → 아이템 엔티티 있음 |
| B5 | 수정 (A6 포함) | 확인: 불이 붙은 뒤에는 증기 가득 · 물 없음이어도 `litTime--` → 연료 낭비 | `BoilerBlockEntity.serverTick` 순서: 점화(조건 그대로) → `litTime > 0`이면 `made` 계산, **`made > 0`일 때만** 증기 · 물 반영 + `litTime--`. LIT은 `litTime > 0` 그대로(멈춘 동안도 켜짐). 시험: 물 가득 · 석탄 1 · 소비처 없음 → 1700틱 뒤에도 `isLit()` (지금 코드는 1600틱에 꺼짐) |
| B6 | 보류 (2단계 후보) | 채널 소유자 없음 · 남의 수신기 사용 맞음. 소유 모델(개인 / 팀 / 공용)은 멀티 설계 판단 | 결정 필요: 채널 범위(서버 공용 유지 / 놓은 사람 UUID + 색 / 팀) |
| B7 | 수정 | 확인: 바닐라 화로는 `getCraftingRemainder()`를 돌려주는데 기계 · 보일러는 `shrink(1)`만 → 용암 양동이의 양동이 소실 | `MachineBlockEntity`에 공용 static `consumeFuel(Level, BlockPos, NonNullList<ItemStack>, int slot)`(바닐라 `AbstractFurnaceBlockEntity` 227~237행과 같은 처리), 기계 · 보일러 둘 다 사용. 빈 양동이가 연료 칸을 막지 않게: 기계 `getSlotsForFace(DOWN)`에 FUEL 추가 + `canTakeItemThroughFace(FUEL, s, DOWN)`은 `s.is(Items.BUCKET)`일 때만, 보일러 `canTakeItemThroughFace`도 아래 면 + 빈 양동이만 true. 시험: 용암 양동이로 분쇄 → 연료 칸에 빈 양동이 |
| B8 | 수정 (기계 · 굴착기) | 확인: 증기가 모자라면 기계 LIT(빛 8) · 굴착기 POWERED가 틱 단위로 뒤집혀 `setBlockAndUpdate` + 빛 갱신 반복. 모터는 10틱 펄스라 제외 | 기계: `idleTicks` 카운터, `active = litTime>0 \|\| steamWorking`, 보이는 상태 = `active \|\| idleTicks < 10`, 블록 상태와 다를 때만 갱신. 굴착기: `lastWorkTick`(실제로 캔 틱), POWERED = `work()` 결과 `\|\|` 최근 20틱 안에 캠. 시험은 선택 |
| B9 | 기각 | 보일러 수는 적고 `setChanged`는 청크 표시 + 비교기 이웃 갱신뿐, 256칸 BFS를 20틱마다는 싸다. 타는 동안 litTime이 매 틱 바뀌어 "바뀐 틱만"으로도 이득이 거의 없음 | |
| B10 | 수정 | 확인: 정적 `RECEIVERS`가 서버 종료 때 안 비워짐 → 싱글에서 월드를 바꿀 때 옛 ServerLevel 유지(오작동은 서버 비교로 걸러짐). 한 줄 수정 | `EnderLinkBlockEntity.clearReceivers()` + `ModLogistics.init`에서 `ServerLifecycleEvents.SERVER_STOPPED`에 등록 |
| B11 | 수정 | 확인: 송신기는 `getItem`이 늘 비어 깔때기 · 벨트의 `addItem`이 스택 전체를 `setItem`, 수신기에 일부만 들어가면 나머지를 송신기 위에 떨굼(벨트 위면 아이템이 쌓임). 깔때기는 1개씩이라 벨트 · 굴착기 경로에서만 생김 | `canPlaceItem`(송신기) = 같은 채널 · 같은 서버 수신기들의 남은 자리 합 ≥ `stack.getCount()`. `setItem`은 수신기를 정해진 순서(같은 차원 먼저, 그다음 거리순)로 돌며 다 넣음, 그래도 남으면 그때만 떨굼. 시험: 수신기 자리가 4개뿐일 때 16개 거부 · 4개 수락 / 수신기 둘에 나눠 담김 |
| B12 | 기각 | 벨트당 틱마다 작은 AABB 질의 1회는 섹션 조회라 싸다(32칸 = 64회도 무시할 수준). 빈 줄 증기 틱당 1도 미미 | |
| C1 | 수정 | 확인: 합금로 = 철판 6 + 화로 2 + 용광로, 철판은 압착기로만 나오는데 압착기를 알려 주는 곳이 없음. JEI는 제작대 레시피만 보여 철판 만드는 법(압착)이 안 보임. `iron_plate` 과제는 실제로 강철판 감지 | 새 과제 `advancement/press.json`: 부모 `sanupcraft:crusher`, 조건 `sanupcraft:press` 소지, 아이콘 press, task. `alloy.json` 부모 → `sanupcraft:press`. 문구는 F4 표. `SanupcraftM3GameTest.advancementGuideLoads` 목록에 `"press"` 추가 (F4만 수정) |
| C2 | 수정(툴팁) · JEI 보류 | 확인: 기계 레시피가 JEI · 레시피북에 없음, 게임 안 안내 0. JEI 플러그인은 별도 모듈 · 의존성 → 2단계 후보 | 클라이언트 `SanupcraftClient`에 `ItemTooltipCallback.EVENT`: `<descriptionId>.tooltip`, `.tooltip2` 키가 `Language.getInstance().has(key)`면 회색 줄로 추가. 기계 · 장비 · 소환석 등 문구는 F4 표 (모든 문구는 lang에만) |
| C3 | 수정 | 확인: 로드된 수신기만 등록, 송신기 빈손 우클릭은 채널만 보여 줌 → 안 될 때 이유를 알 수 없음 | 송신기 빈손 우클릭: `block.sanupcraft.ender_link.sender_status`(채널, 연결된 수신기 수), 0개면 `...no_receiver`. 수 = 같은 서버 · 같은 채널 등록 수신기(`EnderLinkBlockEntity.linkedReceivers()`). 과제 설명에 "수신기 쪽이 로드돼 있어야" (F4) |
| C4 | 수정 | 확인: 수신기 빈손 우클릭 = 열기라 채널 확인 불가 | `EnderLinkBlock.useWithoutItem`: 수신기에서 `player.isSecondaryUseActive()`(웅크림, 빈손)면 채널 메시지, 아니면 열기. 수신기 `getDefaultName` = `block.sanupcraft.ender_receiver.titled`(이름, 채널 색) → 상자 화면 제목에 채널. 기본 흰색 채널은 그대로(엔더 상자처럼 의도) |
| C5 | 수정 | 확인: 물이 가득이면 `pourBucket` false → 메시지 없이 FAIL, 다른 연료도 FAIL만. 상태 줄에 무엇이 모자란지 없음 | `BoilerBlock`: 양동이 실패 → `block.sanupcraft.boiler.water_full`, 다른 연료 → `...other_fuel`. 빈손 상태 = 기존 status + " · " + 힌트 하나(우선순위: 물 0 → `hint.no_water`, 안 타고 연료 0 → `hint.no_fuel`, 증기 가득 → `hint.steam_full`, 그 외 `hint.ok`). 기존 getter만 씀 |
| C6 | 수정 | 확인: 상태가 "Y %s층" 하나뿐(시작 전 layer 0 → "Y 0층"), 증기 부족 · 위 막힘 구분 없음 | `QuarryBlockEntity.status()` enum `NOT_STARTED / RUNNING / NO_STEAM / BLOCKED / FINISHED`(BLOCKED = pending이 남음 또는 위에 Container 없음). `QuarryBlock`이 상태별 키(F4 표)로 표시 |
| C7 | 수정 (D7 포함) | 확인: 녹 · 과열 규칙은 과제 설명 일부에만, 맞아도 안내 없음, 장비 툴팁 0 | ① 툴팁: C2 경로로 강철 · 내열 갑옷 · 검 문구(F4). ② 전투: `hitWithRust`/`hitWithHeat`에서 맞았고(hurt) 세트가 없고 대상이 `ServerPlayer`면 `sendOverlayMessage(rust_warning / heat_warning)`, 보스 인스턴스의 `Map<UUID, Long>`로 플레이어별 200틱 쿨다운 (F3) |
| C8 | 수정 | 확인: 소환 방법(제단에 소환석 들고 우클릭, 소모) 설명 없음, busy 문구가 "수호자"만 | 문구만: steel · guardian_core 설명 교체, `guardian_altar.busy` → "근처(48칸)에 이미 보스가 있다." (F4) |
| C9 | 수정(일부) | 확인: 화면에 칸 설명 · 증기 표시 없음, 깔때기 규칙은 강철판 단계에서야 나옴. 부산물 칸 가득 → 버림은 의도(1 참고) → 제외 | ① `MachineScreen`: 빈 기계 칸에 마우스를 올리면 칸 설명 툴팁(키 `gui.sanupcraft.machine.slot.*`). ② 증기: `DATA_STEAM = 5`, `DATA_COUNT = 6`, `MachineMenu.steam()`, 불꽃 자리(56,36 14x14)에 마우스 → `gui.sanupcraft.machine.steam`(증기, 용량). ③ 깔때기 규칙 문구를 crusher 과제로 (F4) |
| C10 | 수정 | 확인: belt_motor 설명이 내열 장비 이야기, 벨트 규칙 없음 | 문구만 (F4) |
| C11 | 수정 | 확인: 오버월드로 돌아오는 단계 없음("너무 차갑다" 확인 불가), steam 꾸러미 수요 5/틱(굴착기 2 + 모터 1 + 분쇄기 1 + 조립대 1) > 보일러 4, 문서 "GameTest 39개" 낡음 | `PlaytestCommand`: `HOME("오버월드로 돌아오기", Level.OVERWORLD, kitHome)` — 오버월드 월드 스폰의 지표면(`getHeightmapPos(MOTION_BLOCKING)`)으로 이동(발판 없음), 꾸러미 = 수호자 제단 1 + 파수꾼 소환석 1 + 음식. `steam` 보일러 1 → 2. `start`에 레드스톤 4 · 참나무 판자 16(피스톤 → 압착기). PLAYTEST 표 · 체크리스트 반영(F4) |
| C12 | 수정 | 확인: 강철 요구량 · 해머 60% · 단단한 블록 제외가 설명에 없음 | 문구만 (F4) |
| D1 | 수정 | 확인: 돌진 방향을 20틱째에 잠그고 직전까지 추적 → 가까우면 잠금을 본 뒤 피할 시간이 없음 | `RustyGuardian`: `CHARGE_LOCK = 12`. 0~11틱 추적 + 연기, 12틱에 `chargeDirection` 잠금(`hitThisCharge.clear()`), 12~19틱은 추적 안 함 + 그 방향을 바라봄 + 2틱마다 잠긴 경로(1~14칸) `CRIT` 입자, 20틱부터 돌진(속도 0.9 · 16틱 그대로). 테스트용 `chargeDirection()` getter. 시험: 목표를 +x에 두고 시작 → 13틱에 목표를 +z로 옮김 → 21틱에 방향 x ≈ 1 |
| D2 | 수정 | 확인: 머리가 틱당 20°로 발사 순간까지 추적 → 원을 그려 도는 속도(4칸 거리에서 틱당 약 4°)로는 못 피함. 주석 "옆이나 뒤로 돌면 된다"와 다름 | `FurnaceWarden`: `BLAST_LOCK = 10`. 10틱에 `blastYaw = getYHeadRot()` 고정, 10~19틱 추적 안 하고 yRot · 머리 · 몸을 `blastYaw`로, 2틱마다 ±35° 경계 두 줄(1~7칸) `FLAME`. `steamBlast`는 잠겨 있으면 `blastYaw`, 아니면 지금 머리 방향. `endPattern`에서 잠금 해제. 시험: 정면 3칸 목표 → 11틱에 옆(90°) 3칸으로 옮김 → 21틱 뒤 피해 없음 |
| D3 | 수정 | 확인: 방패로 막으면 `hurtServer` false → 피해 · 녹 · 과열 모두 없음. 바닐라 워든은 `getSecondsToDisableBlocking()` 5초로 방패를 무력화 | 두 보스에 `getSecondsToDisableBlocking()` 오버라이드 → `5.0F` (바닐라 워든과 같음; `Player.blockUsingItem`이 방패를 5초 끔). 시험: 값 > 0 |
| D4 | 보류 | 기둥 위 · 좁은 곳에서 활로만 잡히는 건 맞음. 반격 패턴(도약 · 순간이동 · 충격파)은 새 패턴 설계 | 실테스트에서 문제인지 본 뒤 패턴 결정 |
| D5 | 수정 | 확인: 내려찍기 고리 5.0인데 판정은 3D 거리 ≤ 5 + 0.9 = 5.9, 입자 5틱마다 24개. 도약 착지 범위 표시 없음 | 수호자: `slamReach() = SLAM_RADIUS + getBbWidth()/2`, 고리를 이 반경에 **2틱마다 36개**, 판정은 수평 거리 ≤ slamReach(세로는 지금 AABB ±2 유지). 파수꾼 도약: 뛰는 순간 조준점(지금 계산한 `push` 방향 × distance, 12칸 제한)을 저장, 공중에 있는 동안 2틱마다 그 점에 반경 `LEAP_RADIUS + getBbWidth()/2` `FLAME` 고리 36개. 판정은 그대로(착지 위치 기준) |
| D6 | 보류 (2단계 후보, A10 포함) | 인원 무관 고정 체력 · 핵 1개 맞음. 배율 식 · 기여자별 전리품 · 장판 수는 보스 + 제단 + 전리품에 걸친 멀티 설계 | 결정 필요: 체력 배율 식, 전리품을 기여자마다 줄지 |
| D7 | 중복 → C7 | | |
| D8 | 수정(거미줄 · 구속) | 확인: 위더는 `makeStuckInBlock`을 비워 막는데 두 보스는 안 막음 → 거미줄에 가두면 돌진 · 도약이 무력화. 나약함은 근접 속성만 줄여 패턴 피해(상수)에 영향 없음 → 허용 | 두 보스: `makeStuckInBlock(BlockState, Vec3)` 빈 오버라이드, `canBeAffected`에서 `MobEffects.SLOWNESS`만 false. 시험: 구속 효과가 안 걸림 |
| D9 | 보류 | 죽은 뒤에도 깎인 체력이 유지되는 건 맞음. "전멸하면 회복"은 시체 회수(Corpse Graves) 흐름과 엮인 규칙 결정 | 결정 필요: 전멸 시 보스 회복 여부 |
| D10 | 수정(공간 검사) | 확인: 공간 검사 없이 소환 → 천장이 낮으면 끼고 소환석만 소비. "로드된 엔티티만 셈"은 48칸이라 거의 안 생김 → 제외 | `GuardianAltarBlock.hasRoom(level, pos, type)` = `level.noCollision(type.getSpawnAABB(x, y+1, z))`, **`useItemOn`에서만** 검사(실패 → `guardian_altar.no_room`, 소환석 안 씀). `summon()`은 그대로 둠 — 네더 시험(y 100 네더랙 안)이 `summon()`을 직접 부르기 때문. 시험: 제단 2칸 위에 돌 → `hasRoom` false |
| D11 | 수정 | 확인: 이름표를 붙여도 보스 바 제목 고정. 평화로움에서는 몬스터라 `checkDespawn`이 바로 지움(샌드백이 아니라 **소환석만 날아감**) | 두 보스 `setCustomName` 오버라이드 → `bossEvent.setName(getDisplayName())` (불러올 때도 이 경로). `useItemOn`에서 평화로움이면 `guardian_altar.peaceful`, 소환석 안 씀 |
| D12 | 보류 | 패턴 다양화 · 거리 조건은 손맛 영역 | 실테스트 뒤 |

### 2-2. 수정 묶음 (파일 소유)
**사전 준비 (오케스트레이터, 분기 전 1회):** 빈 시험 클래스 4개 `src/gametest/java/io/github/yhyeop/sanupcraft/test/SanupcraftFix{Factory,Logistics,Boss,Guide}GameTest.java`를 만들고 `src/gametest/resources/fabric.mod.json` `fabric-gametest`에 등록, `./gradlew build` 통과 확인. 이후 `fabric.mod.json`은 아무 묶음도 건드리지 않는다.
문구는 전부 F4가 `ko_kr.json` · `en_us.json`에 넣는다(키 · 문구는 아래 F4 표가 정본). F1~F3은 키 이름만 코드에 쓴다 — F4가 합쳐지기 전에는 키가 그대로 보이는 게 정상.

| 묶음 | 항목 | 소유 파일 (이 묶음만 수정) | 시험 |
|---|---|---|---|
| **F1 공장 · 증기 · 벨트** | B1(+A5 필터), B2, B5, B7, B8(기계), C9 ①② | `factory/MachineBlockEntity.java`, `factory/MachineRecipe.java`, `factory/MachineMenu.java`, `client/.../MachineScreen.java`, `power/BoilerBlockEntity.java`, `logistics/BeltBlockEntity.java` | `SanupcraftFixFactoryGameTest`: 합금로 깔때기 교착 · 분쇄기 흙 거름 · 벨트 → 분쇄기 · 보일러 연료 아낌 · 용암 양동이 반환 |
| **F2 굴착기 · 운반기 · 보일러 화면 · 실테스트** | B3, B4, B8(굴착기), C6, B10, B11, C3, C4, C5, C11(코드) | `mining/QuarryBlockEntity.java`, `mining/QuarryBlock.java`, `logistics/EnderLinkBlockEntity.java`, `logistics/EnderLinkBlock.java`, `logistics/ModLogistics.java`, `power/BoilerBlock.java`, `playtest/PlaytestCommand.java` | `SanupcraftFixLogisticsGameTest`: 흑요석 남김(`alone_quarry`) · pending 떨굼 · 송신기 부분 자리 거부 / 둘로 나눔 · 상태 enum. 꾸러미는 `steam`에 보일러 2개인지 정도 |
| **F3 보스 · 제단** | D1, D2, D3, D5, C7②(D7), D8, D10, D11 | `boss/RustyGuardian.java`, `boss/FurnaceWarden.java`, `boss/GuardianAltarBlock.java` | `SanupcraftFixBossGameTest`: 돌진 잠금 · 분출 잠금 회피 · 방패 무력화 값 · 구속 면역 · `hasRoom` · 이름표 → 보스 바 이름 |
| **F4 밸런스 데이터 · 길잡이 · 문구 · 문서** | A1, A2, A3(문서), C1, C2, C7①, C8, C10, C12, C3 · C5 · C6 · C9 · C11의 문구와 문서 | `recipe/heatproof_*.json`(5개), `gear/HeatproofGear.java`, `advancement/*.json`(+ 새 `press.json`), `lang/ko_kr.json`, `lang/en_us.json`, `client/.../SanupcraftClient.java`, `README.md`, `ROADMAP.md`, `docs/PLAYTEST.md`, 기존 `SanupcraftM3GameTest.java`(과제 목록에 `press` 추가만) | `SanupcraftFixGuideGameTest`: 내열 5종 레시피에 강철 부위가 재료로 들어감(`placementInfo` 재료 검사) · 내열 검 공격력 = 다이아 검 |

순서: F1~F4 병렬. 합칠 때 F4를 마지막에 넣고, 문서의 GameTest 총수는 모두 합친 뒤 `./gradlew build` 결과로 오케스트레이터가 고친다. 문서에 반영할 동작 변화(F4): 벨트 끝 기계는 재료 칸 먼저 · 기계 깔때기는 레시피 재료만 · 보일러는 증기를 안 쓰면 연료를 아낌 · 굴착기는 철 곡괭이로 못 캐는 블록을 남김 · 내열 = 강철 장비 업그레이드 · 내열 검 7 · 핵 8개 · `home` 단계 · 보스 방패 무력화 · 평화로움 소환 거부.

**F4 문구 표 (ko_kr 정본, en_us는 같은 뜻으로)**

| 키 | 문구 |
|---|---|
| `advancements.sanupcraft.crusher.description` | 원석 → 가루 2 + 가끔 다른 금속. 석탄도 갈자(강철 재료). 다음은 압착기(철 주괴 5 · 피스톤 · 조약돌 3). 깔때기: 위 재료 · 옆 연료 · 아래 결과. |
| `advancements.sanupcraft.press.title` / `.description` | 철판 찍기 / 압착기: 철 주괴 → 철판. 합금로 = 철판 6 + 화로 2 + 용광로. |
| `advancements.sanupcraft.alloy.description` | 강철! 갑옷 한 벌 24 · 해머 6 · 검 2 (강철 하나 = 원석 철 1 + 석탄 1). 압착기로 강철판 → 조립대로 기계 부품. |
| `advancements.sanupcraft.hammer.description` | 3x3으로 캔다 (웅크리면 한 칸). 속도는 60%, 훨씬 단단한 블록은 남긴다. |
| `advancements.sanupcraft.steel.description` | 강철 한 벌이면 녹 공격을 막는다. 수호자 제단을 놓고 소환석을 들고 우클릭하면 녹슨 수호자가 깨어난다(소환석 소모). |
| `advancements.sanupcraft.guardian_core.description` | 핵 하나 = 강화 기계 틀 하나, 또는 분쇄기로 핵 조각 4개. 틀 5곳 + 조각 10개 → 수호자를 8번쯤 잡게 된다. |
| `advancements.sanupcraft.boiler.description` | 물 양동이를 붓거나 물 원천 옆에 두고 연료를 넣는다. 파이프로 이은 기계는 연료 없이 돈다. 빈손 우클릭: 상태. |
| `advancements.sanupcraft.belt_motor.description` | 모터가 이어진 벨트(32칸)를 돌린다. 벨트는 바라보는 쪽으로 나르고 끝의 상자 · 기계에 넣는다. 웅크리면 안 밀린다. |
| `advancements.sanupcraft.heatproof.description` | 내열 = 강철 장비 + 기계 부품 + 핵 조각(인챈트는 사라진다). 한 벌이면 불 · 용암에 강해진다. 네더 요새에서 블레이즈 막대를 구하자. |
| `advancements.sanupcraft.quarry.description` | 파이프로 증기, 위에 상자 · 벨트. 아래 9x9를 바닥까지 판다 — 구덩이에 빠지지 않게. 철 곡괭이로 못 캐는 블록은 남긴다. |
| `advancements.sanupcraft.ender_link.description` | 송신기와 수신기를 같은 염료 색으로. 차원을 넘어 가지만 수신기 쪽이 로드돼 있어야 한다(근처에 사람이 있거나 스폰 청크). |
| `block.sanupcraft.guardian_altar.busy` / `.no_room` / `.peaceful` | 근처(48칸)에 이미 보스가 있다. / 공간이 좁다. 제단 위를 비우자. / 평화로움에서는 보스가 깨어나지 않는다. |
| `entity.sanupcraft.rusty_guardian.rust_warning` | 녹이 슨다! 강철 갑옷 한 벌이 없으면 피해 1.5배 + 구속. |
| `entity.sanupcraft.furnace_warden.heat_warning` | 과열! 내열 갑옷 한 벌이 없으면 피해 1.5배 + 불. |
| `block.sanupcraft.boiler.water_full` / `.other_fuel` | 물이 가득 찼다. / 다른 연료가 들어 있다. 같은 연료만 더 넣을 수 있다. |
| `block.sanupcraft.boiler.hint.no_water` / `.no_fuel` / `.steam_full` / `.ok` | 물 없음: 양동이로 붓거나 물 원천 옆에 / 연료 없음: 연료를 들고 우클릭 / 증기 가득: 쓰는 만큼만 연료를 태운다 / 작동 중 |
| `block.sanupcraft.steam_quarry.not_started` (증기, 용량) | 준비됨 · 증기 %s/%s · 아래 9x9를 바닥까지 판다 |
| `block.sanupcraft.steam_quarry.no_steam` (층, 캔 수, 증기, 용량) | 증기 부족 · Y %s층 · 캔 블록 %s · 증기 %s/%s |
| `block.sanupcraft.steam_quarry.blocked` (층, 캔 수) | 멈춤: 위에 상자 · 깔때기 · 벨트가 없거나 가득 · Y %s층 · 캔 블록 %s |
| (기존 `status` · `finished`는 RUNNING · FINISHED에 그대로) | |
| `block.sanupcraft.ender_link.sender_status` (채널, 수) / `.no_receiver` (채널) | 채널: %s · 연결된 수신기 %s개 / 채널: %s · 연결된 수신기 없음 (수신기 쪽이 로드돼 있어야 한다) |
| `block.sanupcraft.ender_receiver.titled` (이름, 채널) | %s (채널: %s) |
| `gui.sanupcraft.machine.slot.input` / `.input2` / `.fuel` / `.result` / `.byproduct` | 재료 (깔때기: 위) / 둘째 재료 (깔때기: 위) / 연료 (깔때기: 옆) · 증기를 이으면 필요 없음 / 결과 (깔때기: 아래) / 부산물 (깔때기: 아래) · 가득 차면 버려진다 |
| `gui.sanupcraft.machine.steam` (증기, 용량) | 증기 %s/%s |
| 툴팁 `<id>.tooltip`: `block.sanupcraft.crusher` | 원석 → 가루 2 · 광석 블록 → 가루 3 · 석탄 · 숯 → 석탄 가루 (`.tooltip2`: 조약돌 → 자갈 · 자갈 → 모래 · 뼈 → 뼛가루 · 수호자의 핵 → 조각 4) |
| `block.sanupcraft.press` / `assembler` / `alloy_smelter` / `heart_forge` | 철 주괴 → 철판 · 강철 주괴 → 강철판 / 강철판 → 기계 부품 2 / 철가루 2 + 석탄 가루 2 → 강철 주괴 / 고대 잔해 → 네더라이트 파편 2 · 블레이즈 막대 → 가루 4 |
| `item.sanupcraft.steel_{helmet,chestplate,leggings,boots}` | 한 벌: 녹슨 수호자의 녹 공격(피해 1.5배 · 구속)을 막는다 |
| `item.sanupcraft.heatproof_{helmet,chestplate,leggings,boots}` | 한 벌: 화염 저항 · 용광로 파수꾼의 과열(피해 1.5배 · 불)을 막는다 |
| `item.sanupcraft.steel_sword` / `heatproof_sword` | 녹슨 수호자에게 피해 1.5배 / 용광로 파수꾼에게 피해 1.5배 |
| `item.sanupcraft.steel_hammer` / `steel_excavator` | 3x3 채굴 (웅크리면 한 칸) · 속도 60% |
| `item.sanupcraft.guardian_summoner` / `warden_summoner` | 수호자 제단에 들고 우클릭 → 녹슨 수호자 (소모) / 네더의 수호자 제단에 들고 우클릭 → 용광로 파수꾼 (소모) |
| `item.sanupcraft.guardian_core` | 강화 기계 틀 재료 · 분쇄기 → 핵 조각 4 |
| `block.sanupcraft.boiler` / `steam_quarry` / `conveyor_belt` / `ender_sender` · `ender_receiver` | 물 + 연료 → 증기 · 빈손 우클릭: 상태 / 아래 9x9를 바닥까지 · 증기 + 위에 상자 · 벨트 / 바라보는 쪽으로 나른다 · 벨트 모터 필요 / 같은 염료 채널로 전송 · 수신기 쪽이 로드돼 있어야 함 |

### 2-3. 비판적 Sonnet 반론
판정당 1회. 근거는 코드 · 바닐라 소스를 직접 읽은 것. 요청: 뒤집기(판정 변경) / 사양 수정(판정 유지, 사양 보정) / 유지. 최종 결정은 Opus.

| ID | 반론 (근거 포함) | 요청 |
|---|---|---|
| A1 | 제작은 새 아이템이라 인챈트뿐 아니라 **손상도도 초기화** → 다 닳은 강철 부위를 넣고 새 내열을 뽑는 수리 우회 (바닐라 대장간 변환은 손상 유지). 더 큰 부작용: 업그레이드하면 강철 세트가 사라져 `SteelGear.wearsFullSet` 거짓 → 핵 8개 파밍(수호자 8회)에서 녹 1.5배 + 구속을 다시 맞음. `SteelGear.java`는 어느 묶음도 소유하지 않음 | 사양 수정: 내열 세트도 녹 방어로 인정(`wearsFullSet` 확장, F4에 `gear/SteelGear.java` 배정) 또는 문서 경고, 손상 초기화는 의도로 명시 |
| A2 | 같은 인챈트 · 손상 초기화 문제(강철 검을 재료로). 공격 7.0은 다이아와 같지만 내구도 900 · 인챈트 14라 다이아(1561)보다 못함 → "다이아급" 서열은 맞음. 동의 쪽으로 기움 | 유지 |
| A3 | 문서에 "8번"을 3곳(ROADMAP 2 · PLAYTEST · 과제 설명)에 박으면 D6/전리품을 나중에 건드릴 때 또 틀어짐. 파수꾼 전리품 `core_shard` 1~3도 계산에서 빠짐 | 사양 수정: 숫자는 ROADMAP 한 곳, 나머지는 "여러 번" |
| A4 | 보류 사유("진행 순서가 바뀐다")가 약함: 엔더 송신기 · 수신기는 이미 셜커 껍데기(엔드)를 요구하고, 바닐라가 엔드를 네더 뒤로 강제함. 레시피 JSON 두 개에 `warden_heart` 추가는 F4 데이터만으로 끝나는 싼 수정 | 뒤집기(수정 대상): ender_sender · receiver에 심장 1개씩 |
| A5 | 보류가 B2의 대표 사용 사례를 계속 깨뜨림: README는 "굴착기 → 벨트 → 분쇄기" 자동 라인을 약속하는데, 굴착기는 흙 · 안산암 · 네더랙을 섞어 내보내고 B1 필터는 그걸 **거부**만 함 → 벨트 끝 / 깔때기 맨 앞이 첫 흙에 막힘(문제가 분쇄기에서 벨트로 옮겨갈 뿐). F1 시험은 원석 철만 쓰므로 통과해도 실사용은 실패 | 뒤집기(최소 수정): 굴착기에서 쓰레기 태그만 버림, 또는 README 문구 정정 |
| A6 | 중복 처리 자체는 맞음. 단 B5 수정은 **벨트 모터가 붙은 줄에서는 효과 없음**(모터가 10틱마다 증기 10을 늘 먹어 보일러가 쉬지 않음). 동의 쪽으로 기움 | 유지 (B12 참고) |
| A7 | 기각 사유(조약돌이 대량 → 철의 주 공급원이 됨)는 **현재 10%에도 그대로 적용**: 조약돌 생성기는 무한이라 이미 재생 가능한 철. 올리는 제안 기각은 맞지만 "실테스트 뒤 재검토"는 하향(5%)도 포함해야 | 사양 수정: 기각 유지 + 실테스트 항목에 "조약돌 → 철 너겟 하향" 추가 |
| A8 | 섬세한 손길 전제는 맞음. 다만 `#gold_ores`가 네더 금 원석을 포함해 블록 하나 = 금가루 3인데 네더 금은 바닐라에서 너겟 드롭이라 3배 가치 차이가 가장 큼. 동의 쪽으로 기움 | 유지 |
| A9 | 근접 피해만 반박했고 A9의 핵심(칸당 9칸 x 60% = 5.4배 처리량)은 다루지 않음. 기각이 아니라 "의도 확인 보류"가 정확 | 사양 수정: 기각 → 보류(실테스트 항목) |
| A10 | 중복(D6) 처리 맞음. 동의 쪽으로 기움 | 유지 |
| B1 | ① `acceptsIngredient`가 호출마다 `getRecipes()`(바닐라 포함 1000개+) 전체를 훑음 — 깔때기는 8틱마다 · 막힌 벨트 끝 아이템은 **매 틱** 호출. 레시피 리로드 때 종류별 재료 집합을 캐시해야 함. ② 석탄 · 숯은 분쇄 재료이기도 해 필터 뒤에도 연료로 잘 들어감(옆면은 필터 없음)이라 OK | 사양 수정: 종류별 캐시(레시피 로드 시 갱신) |
| B2 | UP 먼저 → 석탄 · 숯 · 블레이즈 막대(분쇄 재료이면서 연료)는 벨트 끝에서 **항상 재료 칸**으로 가서 갈려 버리고 연료 칸에는 못 들어감(지금은 옆면이라 연료로 들어감). 규칙을 문서에 써야 함 | 사양 수정: 연료이면서 재료인 것은 재료 칸이 가득일 때만 연료로 + 문서 |
| B3 | 도구 판정은 맞지만 건너뛴 블록(흑요석 등)이 영구히 남아 아래 층은 계속 캐서 구멍이 흑요석 바닥 위에 뜸. 철 곡괭이 기준이 "캐지 못함 = 남김"임을 상태(C6)에는 안 알림. 동의 쪽으로 기움 | 유지 |
| B4 | 가장 흔한 철거 상황이 "위 상자가 없어 pending이 남은 상태"라 수정 가치는 있음. 동의 쪽으로 기움 | 유지 |
| B5 | `litTime`을 멈추면 `LIT`이 켜진 채(연기 · 불빛 · 소리) 연료는 안 줄어듦 → 아무 일도 안 하는데 "타는 중"으로 보임. 시각 상태와 연료 소비를 분리해야 하는데 사양은 `isLit()` = `litTime > 0` 그대로 | 사양 수정: LIT 표시는 이번 틱에 `made > 0`일 때만(또는 5틱 유지) |
| B6 | 보류 이유(소유 모델 설계)는 맞지만 수신기를 **남이 열어 털 수 있는 것**은 멀티에서 곧 도둑질임. 최소 안전판(수신기를 놓은 사람만 열기 / 채널 변경)은 설계 결정 없이도 가능 | 유지(보류), 단 최소 안전판을 2단계 후보 첫 항목으로 |
| B7 | **보일러에 빈 양동이가 남으면 꺼낼 방법이 없음**: 보일러에는 메뉴가 없고(`BoilerBlock`은 F2 파일) 사양은 깔때기 아래 면만 풀어 줌. 손으로 연료를 넣으면 `useItemOn`이 "다른 연료" FAIL을 내고, `fuelCount()`는 양동이를 연료 1개로 셈 → C5 힌트도 오답. F1(BoilerBlockEntity) ↔ F2(BoilerBlock) 교차 | 사양 수정: 보일러는 빈 양동이를 연료 칸에 두지 않고 바로 `Containers.dropItemStack`(또는 플레이어 인벤토리로)하는 편이 단순. 기계만 칸 보존 |
| B8 | 기계 쪽 전제가 약함: `distribute`는 고정 순서로 `share = max(1, steam/n)`씩 줘서 증기 부족이면 깜박이는 게 아니라 **순서 끝 소비자가 계속 굶음**(공급 4 · 소비자 5 → 항상 같은 하나). 진짜 문제는 공정 분배(시작 인덱스 회전) — BoilerBlockEntity는 F1 소유. 굴착기(8씩 모았다 쓰는 구조)는 실제로 깜박임 | 사양 수정: 기계 히스테리시스는 선택으로 낮추고 `distribute` 시작 인덱스 회전 추가(F1) |
| B9 | 기각 사유는 맞지만 B5에서 이미 이 코드 경로를 고치므로 `setChanged`를 값이 바뀐 틱에만 부르는 것은 같은 diff에서 공짜(비교기 이웃 갱신 4회/틱 제거). 동의 쪽으로 기움 | 유지 (B5와 함께 선택) |
| B10 | `SERVER_STOPPED`만 비우면 크래시 · 강제 종료 때 안 불림. `SERVER_STARTING`에서도 비우는 것이 안전 | 사양 수정: STARTING + STOPPED 둘 다 |
| B11 | 전량 수락이 아니면 거부하는 규칙은 벨트 끝에서 "수신기 자리 4 · 스택 5"가 영원히 안 풀림. 그래도 아이템 스팸보다는 낫고 대안(부분 수락)은 `Container` 구조상 어려움. 동의 쪽으로 기움 | 유지 |
| B12 | 쿼리 비용은 무시할 수준이 맞지만 **모터는 빈 벨트에도 증기를 평균 1/틱** 써서 B5(수요 비례 연료)의 이득을 벨트 줄에서 무효화함. 모터에 연결된 줄에 아이템이 없으면 펄스를 건너뛰는 것은 싼 수정 | 뒤집기(일부): 빈 줄 펄스 생략만 수정 대상 |
| C1 | `iron_plate` 과제가 실제로는 **강철판**을 감지하는 점(제목 "철판" ≠ 조건)은 그대로 남음 → 새 `press`와 이름이 겹침. 동의 쪽으로 기움 | 사양 수정: `iron_plate` 과제 조건을 `iron_plate`로 정정하거나 제목을 강철판으로 |
| C2 | 클라이언트 `ItemTooltipCallback` 방식은 전용 서버 · 다른 클라이언트에서 안 보임. 단 모드가 클라이언트 필수이고 데이터(아이템 구성요소) 변경 없이 기존 월드 호환이라 이쪽이 안전. 동의 쪽으로 기움 | 유지 |
| C3 | 수신기 수 표시는 "로드된 것만" 센다는 점이 오해를 부름(안 되는 이유가 로드 문제인지 채널 문제인지 구분 못함). `no_receiver` 문구가 이미 그 설명을 담아 해결됨. 동의 쪽으로 기움 | 유지 |
| C4 | 웅크림 + 빈손 우클릭은 바닐라 상자와 관례가 반대(웅크리면 보통 열림). 그래도 채널 확인 방법이 없던 것보다 낫고 메뉴 제목 병행으로 보완됨. 동의 쪽으로 기움 | 유지 |
| C5 | 힌트 우선순위가 `fuelCount()`에 의존하는데 B7의 양동이 잔여가 연료로 잡혀 오답이 됨(B7 참고). 동의 쪽으로 기움 | 유지 (B7 수정과 함께) |
| C6 | `BLOCKED`와 `FINISHED` 우선순위: 마지막 배치가 위로 못 나간 채 끝나면 `finished`이면서 `pending` 남음 → "끝남" 표시가 먼저면 사용자가 안 막힌 줄 앎. 상태 판정 순서를 사양에 명시해야 함 | 사양 수정: FINISHED는 pending 비었을 때만 |
| C7 | 오버레이는 보스 패턴 중 다른 메시지(보일러 상태 등)와 겹치지 않지만 전투 중 HUD 한 줄은 놓치기 쉬움. 첫 피격 때만이면 충분. 동의 쪽으로 기움 | 유지 |
| C8 | 문구만의 변경이라 위험 없음. 동의 쪽으로 기움 | 유지 |
| C9 | 증기 게이지가 `DATA_LIT_TIME`의 `steamWorking ? 1`과 중복 표시 소지. 불꽃 칸 호버로 증기 수치를 보여 주는 설계가 실제 화면에서 가려지는지 `runClientGameTest` 스크린샷 확인 항목이 없음 | 사양 수정: F1 시험에 스크린샷(클라이언트 시험) 1장 추가 |
| C10 | 문구만의 변경. 동의 쪽으로 기움 | 유지 |
| C11 | `home` 단계의 `getHeightmapPos(MOTION_BLOCKING)`는 월드 스폰이 나무 위 · 바다면 잎 위 / 물 위에 내림. 미생성 청크 로드도 필요 | 사양 수정: 안전한 지점 탐색(`safePlatform` 재사용) |
| C12 | 문구만의 변경. 동의 쪽으로 기움 | 유지 |
| D1 | 구현 시 20틱 `chargeDirection` 재계산 코드를 반드시 제거해야 함(안 지우면 12틱 잠금이 덮어씌워짐). 새 시험은 보스 AI 틱에 의존해 기존 시험에 없는 종류라 flaky 위험(목표가 `getTarget()`에서 풀릴 수 있음) | 사양 수정: 시험은 `startPattern` 후 `patternTick`을 직접 구동하는 결정적 방식 |
| D2 | `LookControl.tick()`이 `lookAtCooldown` 동안 머리를 되돌리므로 잠금 후 매 틱 `setYHeadRot`/`setYRot`/`setYBodyRot`를 직접 덮어써야 함(사양은 "고정"만 말함). 시험은 D1과 같은 flaky 우려 | 사양 수정: 매 틱 덮어쓰기 명시 + 결정적 시험 |
| D3 | **5초는 패턴 간격과 거의 같아 효과가 약함**: 패턴 사이(쿨다운 80틱 + 앞 패턴 잔여 약 10틱 + 다음 타격 시점 약 25틱) ≈ 115틱 > 비활성화 100틱이라 1단계에서는 방패가 **매 패턴마다 복귀**해 계속 막음. 격노(쿨다운 45)일 때만 무력화됨. 시험이 `값 > 0`뿐이라 이를 못 잡음 | 사양 수정: 비활성화 8~10초(`getSecondsToDisableBlocking` 10.0F) 또는 돌진 · 내려찍기에서 방패 무시 |
| D4 | 보류 사유("실테스트에서 보자")는 이해되지만 기둥 + 활 치즈는 코드상 확실하고 보스가 원거리 수단이 없음. 최소 수정(도달 불가 N초면 졸개 소환 쿨다운 단축 / 약한 회복)은 패턴 설계가 아님 | 유지(보류), 단 실테스트 필수 항목으로 격상 |
| D5 | 도약 착지 고리 위치가 틀림: 도약은 한 번 `push`를 주고 공중 마찰 0.91로 약 26틱 날아 수평 이동이 `distance x 0.075 x 약 10.1` = **거리의 약 76%**. 사양은 "push 방향 x distance"에 고리를 그리므로 최대 12칸 도약에서 약 3칸 어긋남 → 고리 안인데 안 맞음. 또 36개 x 2틱마다 x 25틱은 인원 수만큼 패킷이 곱해짐 | 사양 수정: 착지 예상 = 거리 x 0.76(또는 시뮬레이션), 입자는 3틱마다 24~36개 |
| D6 | 보류는 타당(설계 영역). 다만 A10의 "체력 배율"은 `createAttributes`/소환 시 한 줄이라 싸고, 기여자별 전리품만 설계가 필요함 — 둘을 분리해야 함. 동의 쪽으로 기움 | 유지 |
| D7 | 중복(C7) 처리 맞음. 동의 쪽으로 기움 | 유지 |
| D8 | 구속만 막고 나약함을 허용하면 근접(`ATTACK_DAMAGE` 속성) 피해가 줄어듦. 패턴 피해는 상수라 영향 없음 — 판정 근거는 맞음. 동의 쪽으로 기움 | 유지 |
| D9 | 보류 사유는 맞지만 "보스가 영구히 살아 있어 제단이 48칸 안에서 busy가 됨" 부분은 시체 회수와 무관한 별개 문제(소환 불가 상태가 풀리지 않음). 죽인 뒤 재소환이 아니라 방치된 보스를 못 치우면 제단이 영구 잠김 | 유지(보류), 단 "busy가 풀리지 않음"은 별도로 문서화 |
| D10 | 보스 크기(수호자 1.8 x 3.5 / 파수꾼 2.3 x 2.6)가 제단 1x1보다 커서 제단 옆 벽 · 계단이 소환 AABB에 걸림. 네더 동굴 제단은 흔히 걸려 "공간이 좁다"가 자주 뜸. `noCollision`은 블록 + 엔티티 충돌을 모두 봄 | 사양 수정: 메시지에 필요 크기 안내 또는 블록 충돌만 검사 |
| D11 | 불러오기 경로는 `Entity.load`가 `setCustomName`을 부르므로(2219행) 오버라이드로 충분 — 바닐라 위더가 따로 한 번 더 쓰는 것은 중복. 평화로움 거부는 맞음. 동의 쪽으로 기움 | 유지 |
| D12 | 보류는 맞지만 "거리 조건"만은 손맛이 아니라 패턴 낭비(5.9칸 밖이면 내려찍기가 허공) 수정이라 D1 / D5와 같은 파일에서 싸게 가능. 동의 쪽으로 기움 | 유지 |

**묶음(배치) 문제**
1. **F1 ↔ F2 교차 (B7)**: 보일러 연료 잔여물 처리는 `BoilerBlockEntity`(F1)인데 빈 양동이 처리 · "다른 연료" 메시지는 `BoilerBlock`(F2). 양동이를 칸에 남기는 사양이면 어느 쪽도 단독으로 끝낼 수 없음. 양동이를 바로 내보내는 방식으로 F1 안에서 닫을 것.
2. **F1 ↔ F2 교차 (C5)**: 힌트가 `fuelCount()`를 쓰는데 B7의 잔여 양동이가 연료로 세어짐.
3. **소유자 없는 파일**: A1 완화(`SteelGear.wearsFullSet`)와 B12 모터 수정(`BeltMotorBlockEntity`)을 받는 묶음이 없음 → F4(`gear/`)와 F1/F2(`logistics/`)에 배정 필요.
4. **문구 키 ↔ 코드 인수 불일치는 컴파일 · GameTest가 못 잡음** (F4 표의 `%s` 개수 vs F1/F2 코드의 인수 순서). ko/en 키 존재 + 인수 수를 검사하는 시험 1개가 F4에 필요.
5. **새 보스 시험 4종은 AI 틱 의존**: 기존 시험에는 패턴 진행 시험이 하나도 없음. 결정적으로 `startPattern` 후 `patternTick`을 직접 구동하거나 `chargeDirection()` 같은 읽기용 getter만 검증할 것.
6. **순서**: F1 B2(벨트 → 기계 UP 면)는 F1 B1 필터가 있어야 안전하고(둘 다 F1이라 문제 없음), F4의 "벨트 끝 기계는 재료 칸 먼저" 문서는 B2 최종 사양(연료 겸 재료 예외)이 정해진 뒤에 써야 함.

### 2-4. 최종 결정 (Opus)
반론을 코드 · 바닐라 소스로 다시 확인했다(`BlocksAttacks.disable` · `CollisionGetter.noBlockCollision` · `Boiler.distribute` · `SmithingTransformRecipe`). 아래는 **바뀐 것만** 적는다.

| ID | 최종 | 이유 |
|---|---|---|
| A1 | 수정 (사양 보강) | 반론이 맞다: 업그레이드하면 `SteelGear.wearsFullSet`가 거짓이 돼, 핵을 모으려고 수호자를 계속 잡는 동안 녹 공격을 다시 맞는다. → `wearsFullSet`는 **부위마다 강철 또는 내열**이면 참("내열은 강철의 윗단계"). 수호자의 강철 검 1.5배도 내열 검에 똑같이 적용(F3). 손상도 초기화는 의도로 둔다(어차피 S 9.5 + 조각을 냄). 인챈트가 사라진다는 문구는 남긴다. 대장간 변환(`smithing_transform`)은 재료가 3칸뿐이라 부품 비용을 못 담아서 쓰지 않음 |
| A3 | 수정 (사양 보강) | 숫자는 ROADMAP 한 곳에만 둔다: "핵 약 8개(틀 5 + 조각 10 = 핵 3, 파수꾼 전리품 조각 1~3은 빼고 셈)". PLAYTEST와 과제 설명에는 "여러 번"이라고만 쓴다 |
| A4 | 보류 → **수정** (가벼운 안) | 엔더 운반기는 이미 셜커 껍데기(엔드)가 있어야 하고, 엔드는 바닐라에서 네더 다음이다. 그래서 진행 순서는 바뀌지 않고, 파수꾼 두 번째 처치에 쓸모가 생긴다. 다만 수신기는 여러 개 놓으므로 **송신기에만** 심장 1개: `recipe/ender_sender.json` `["EWE","SFS","PHP"]`(E 엔더 진주, W `sanupcraft:warden_heart`, S 셜커 껍데기, F 강화 기계 틀, P 철판, H 깔때기). 운반기 한 쌍 = 파수꾼 2번(제련로 포함). 데이터만 바꾸므로 주인이 되돌리기 쉽다 |
| A5 | 보류 → **수정(문서)** + 쓰레기 버림은 보류 | 다시 계산했다. B1 필터 뒤에 벨트 끝에서 받지 못한 아이템은 아이템 엔티티로 쌓이고 서로 합쳐질 뿐, 뒤에 오는 원석이 막히지는 않는다. 다만 굴착기 위에 깔때기를 두면 쓰레기로 깔때기 5칸이 차서 굴착기가 멈춘다. 근본 원인은 처리량 차이다(굴착기 초당 5블록, 분쇄기 5~10초에 1개). README · ROADMAP의 "굴착기 → 벨트 → 분쇄기"는 "굴착기 → 상자, 원석 · 조약돌만 골라 분쇄기로(받지 못한 것은 벨트 끝에 쌓임)"로 고쳐 쓴다. 버림 필터는 기획 결정이라 보류 |
| A7 | 기각 (사양 보강) | 실테스트 항목에 "조약돌 → 철 너겟 10%: 많으면 낮추기"를 넣는다 |
| A9 | 기각 → 보류 | 처리량 5.4배는 반박하지 않았다. 실테스트 항목으로 둔다 |
| B1 | 수정 (사양 보강) | 반론이 맞다: `getRecipes()`는 바닐라를 포함해 1000개가 넘는데, 벨트 끝 아이템이 매 틱 이걸 부른다. → `MachineBlockEntity`에 static 캐시를 둔다: `RecipeManager` 인스턴스(재로드하면 새 인스턴스) → `Map<MachineKind, List<MachineRecipe>>`. 다른 인스턴스면 다시 만든다 |
| B2 | 수정 (사양 변경) | UP을 먼저 넣으면 석탄 · 숯 · 블레이즈 막대가 늘 갈려 버린다. → 벨트 끝이 기계이면 **연료 아이템(`MachineBlockEntity.isFuel`)은 옆면(연료 칸) 먼저, 남으면 UP. 연료가 아닌 것은 UP만.** 연료 칸은 옛 동작과 같다. 문서에 이 규칙을 적는다 |
| B5 | 수정 (사양 보강) | LIT를 `litTime > 0`로 두면 멈춰 있어도 "타는 중"으로 보인다. → `idleTicks`를 둔다(이번 틱 `made > 0`이면 0, 아니면 +1). LIT = `litTime > 0 && idleTicks < 10`. 블록 상태와 다를 때만 `setBlockAndUpdate`. `isLit()` getter도 같은 뜻으로 |
| B7 | 수정 (사양 변경, F1 안에서 끝냄) | 보일러에는 메뉴가 없고 빈 양동이를 꺼낼 방법도 없다. → **보일러**: 남은 것(양동이)을 칸에 두지 않고 `Containers.dropItemStack(level, x+0.5, y+1, z+0.5, …)`으로 위에 떨군다. `fuelCount()`는 그대로 맞다. **기계**: 바닐라 화로처럼 연료 칸에 남기고, `getSlotsForFace(DOWN)`에 FUEL을 더한다. `canTakeItemThroughFace(FUEL, s, DOWN)`는 `s.is(Items.BUCKET)`일 때만 참. 공용 static `MachineBlockEntity.consumeFuel(...)`는 남은 것을 `ItemStack`으로 돌려주고, 놓을 곳은 부르는 쪽이 정한다 |
| B8 | 수정 (사양 변경) | 반론이 맞다: `distribute` 순서가 고정이라 증기가 모자라면 늘 같은 소비자가 굶는다. → `BoilerBlockEntity`에 `int rotation`을 두고, 매 틱 시작 인덱스를 `rotation++ % n`으로 돌려 공평하게 나눈다. 그러면 모든 기계가 80%처럼 띄엄띄엄 돌기 때문에 기계 히스테리시스(`idleTicks < 10`)는 **필수**로 둔다. 굴착기 POWERED 20틱 유지도 그대로 |
| B10 | 수정 (사양 보강) | `SERVER_STARTING`과 `SERVER_STOPPED` 둘 다에서 `clearReceivers()` |
| B12 | 기각 → **수정 (빈 줄 펄스 생략만)** | 모터는 빈 줄에서도 틱당 증기 1을 써서 B5의 이득을 없앤다. → `BeltMotorBlockEntity`는 펄스 때 BFS로 벨트를 모은 뒤, 어느 벨트 위에도 `rides` 대상(아이템 · 밀리는 몹 · 플레이어)이 없으면 증기를 쓰지 않고 `power`도 하지 않는다. `belts`는 BFS 칸 수로 둔다(기존 `motorPowersTheWholeLine`이 3을 보도록). `BeltBlockEntity`에 `public static boolean hasRider(Level, BlockPos)` 추가. 칸마다 엔티티 질의는 그대로(기각 부분) |
| C6 | 수정 (사양 보강) | 상태 판정 순서: ① pending이 남았거나 위에 Container가 없음 → BLOCKED, ② finished → FINISHED, ③ !started → NOT_STARTED, ④ 증기 < 8 → NO_STEAM, ⑤ RUNNING |
| C9 | 유지 (반론 기각) | 클라이언트 시험은 `build`에 들어가지 않고, 마우스를 올린 툴팁은 자동으로 찍기 어렵다. 대신 PLAYTEST 체크리스트에 "빈 칸 툴팁 · 불꽃 칸 증기 표시"를 넣는다(F4) |
| C11 | 수정 (사양 보강) | `home`: 오버월드 청크를 불러오고(`getChunk`), 월드 스폰 x · z에서 `MOTION_BLOCKING_NO_LEAVES` 높이로 간다. 그 아래가 액체이면 그 자리에 `safePlatform`을 깐다 |
| D1 · D2 | 수정 (시험 방식 변경) | AI 틱에 기대지 않는다. 두 보스에 `public void tickPattern(ServerLevel)`(= `patternTick++` + switch, `customServerAiStep`이 부름)을 둔다. 시험은 `setNoAi(true)` → `setTarget` → `startPattern` → `tickPattern`을 직접 N번 부른다. D1은 20틱에서 방향을 다시 계산하는 코드를 **지운다**. D2는 잠근 뒤 10~20틱 **매 틱** `setYRot` · `setYHeadRot` · `setYBodyRot(blastYaw)`로 덮어쓴다 |
| D3 | 수정 (사양 변경) | 반론이 맞다: 패턴 간격 ≈ 115틱 > 방패 끄는 시간 100틱이라, 패턴 때만 방패를 들면 다 막힌다. → **패턴 타격(돌진 · 내려찍기 · 분출 · 장판 · 도약)은 맞히기 직전 대상이 막는 중이면 방패를 깨고(`BlocksAttacks.disable(level, target, 5.0F, stack)` — 쿨다운 + `stopUsingItem`) 그대로 맞힌다.** `hitWithRust` / `hitWithHeat`에 `boolean pattern` 인자를 추가한다(근접 `doHurtTarget`은 false). 근접은 `getSecondsToDisableBlocking()` = 5.0F(워든과 같음)로 한 번 막히면 5초 꺼진다 |
| D5 | 수정 (사양 보강) | 반론이 맞다: 공중 마찰 0.91 때문에 실제 수평 이동은 push의 약 10배, 거리의 약 76%다. → 뛰는 순간 `predictLanding(start, velocity)`로 착지점을 계산한다(틱마다 위치 += v, v.y = (v.y − 0.08) × 0.98, v.xz × 0.91, y가 시작 높이 아래로 내려가거나 40틱이면 멈춤). 그 점에 고리를 그린다. 입자는 수호자 고리 · 파수꾼 착지 고리 모두 **3틱마다 32개** |
| D10 | 수정 (사양 보강) | `hasRoom` = `level.noBlockCollision(null, type.getSpawnAABB(x+0.5, y+1, z+0.5))`(블록만 보고 엔티티는 무시). 메시지 `no_room`은 "공간이 좁다: 제단 위로 3x3 · 높이 4칸을 비우자." |

나머지는 2-1 유지. (A2 · B3 · B4 · B6 · B9 · B11 · C1~C5 · C7 · C8 · C10 · C12 · D4 · D6~D9 · D11 · D12 포함. B6의 "수신기는 놓은 사람만 연다"는 2단계 후보 첫 항목이다. D4 기둥 공략, D9 방치된 보스로 제단이 계속 busy인 것은 F4가 PLAYTEST 필수 항목 · ROADMAP 미해결로 적는다.)
최종 집계(46): 수정 대상 **34** (A4 · A5(문서) · B12 추가) · 보류 6 (A9 · B6 · D4 · D6 · D9 · D12) · 기각 3 (A7 · A8 · B9) · 중복 3 (A6 · A10 · D7). 2단계 후보: B6, D6 (+A10), C2 JEI, A5 쓰레기 버림.

### 2-5. 최종 수정 묶음
**사전 준비 (오케스트레이터, 분기 전):** 빈 시험 클래스 `SanupcraftFix{Factory,Logistics,Boss,Guide}GameTest`(`src/gametest/java/io/github/yhyeop/sanupcraft/test/`)를 만든다. `src/gametest/resources/fabric.mod.json`의 `fabric-gametest`에 등록하고 `./gradlew build`로 통과를 확인한다. 이후 이 파일은 아무도 건드리지 않는다.
**규칙:** 아래 표의 파일은 그 묶음만 고친다. 표에 없는 파일은 고치지 않는다(필요하면 멈추고 보고). 문구는 F4만 lang에 넣고, F1~F3은 2-1 F4 문구 표의 키 이름만 쓴다. 바뀐 문구는 이 절 끝의 표를 따른다. 기존 시험 파일은 `SanupcraftM3GameTest.java`만 F4가 고친다(과제 목록에 `"press"` 추가). 각 묶음은 끝나면 `./gradlew build`를 돌린다.

**F1 · 기계 · 보일러 엔티티** — `SanupcraftFixFactoryGameTest`
- 파일: `factory/MachineBlockEntity.java`, `factory/MachineRecipe.java`, `factory/MachineMenu.java`, `client/.../MachineScreen.java`, `power/BoilerBlockEntity.java`
- **B1** — 재료 칸은 깔때기 · 벨트로 넣을 때(`canPlaceItemThroughFace`)만 거른다. 손으로 넣을 때(`canPlaceItem`)는 그대로.
  - 받는 것: 이 기계 레시피의 재료(`MachineRecipe.acceptsIngredient(stack)`), 레시피는 RecipeManager 인스턴스별 static 캐시에서 찾는다.
  - 합금로 INPUT: 같은 아이템이 이미 있거나, 비었고 INPUT2에 같은 아이템이 없을 때.
  - 합금로 INPUT2: 같은 아이템이 이미 있거나, 비었고 INPUT이 차 있고 INPUT과 다를 때.
- **B5** — 보일러는 `made > 0`일 때만 증기 · 물을 바꾸고 `litTime--`. LIT = `litTime > 0 && idleTicks < 10`.
- **B7** — `consumeFuel` 공용 메서드. 기계는 남은 것을 연료 칸에 두고 아래 면으로 빈 양동이만 꺼낼 수 있다. 보일러는 남은 것을 위에 떨군다.
- **B8** — `distribute` 시작 인덱스를 돌린다. 기계 LIT은 `idleTicks < 10` 히스테리시스로, 블록 상태와 다를 때만 갱신.
- **C9** — 빈 칸 툴팁 `gui.sanupcraft.machine.slot.{input,input2,fuel,result,byproduct}`. `DATA_STEAM = 5`, `DATA_COUNT = 6`, `MachineMenu.steam()`, 불꽃 칸에 마우스를 올리면 `gui.sanupcraft.machine.steam`(증기, 용량 200).
- 시험:
  - 합금로 교착: INPUT2에 석탄 가루 2, 위 깔때기에 석탄 가루 2 + 철가루 2 → 강철.
  - 분쇄기 위 깔때기에 흙 + 원석 철 → 철가루, 흙은 깔때기에 남음.
  - 보일러: 물 가득 · 석탄 1 · 소비처 없음 → 1700틱 뒤 `litTime > 0`(getter 추가), `isLit()` 거짓.
  - 기계 연료가 용암 양동이 → 연료 칸에 빈 양동이.
  - 보일러 연료가 용암 양동이 → 위에 양동이 아이템 엔티티.
  - 보일러 1 + 분쇄기 5(증기 부족) → 200틱 뒤 다섯 대 모두 결과 > 0.

**F2 · 벨트 · 굴착기 · 운반기 · 보일러 블록 · 실테스트** — `SanupcraftFixLogisticsGameTest`
- 파일: `logistics/BeltBlockEntity.java`, `logistics/BeltMotorBlockEntity.java`, `logistics/EnderLinkBlockEntity.java`, `logistics/EnderLinkBlock.java`, `logistics/ModLogistics.java`, `mining/QuarryBlockEntity.java`, `mining/QuarryBlock.java`, `power/BoilerBlock.java`, `playtest/PlaytestCommand.java`
- **B2** — 벨트 끝이 기계이면 연료는 옆면 먼저 → 남으면 UP, 연료가 아니면 UP만.
- **B12** — 줄에 탈 것이 없으면 펄스를 건너뛴다. `belts` = BFS 칸 수.
- **B3** — 철 곡괭이로 드롭이 안 나오는 블록(`requiresCorrectToolForDrops && !TOOL.isCorrectToolForDrops`)은 건너뛴다.
- **B4** — `preRemoveSideEffects`에서 pending을 떨군다.
- **B8 굴착기** — POWERED를 최근 20틱 동안 유지.
- **C6** — `status()` enum과 판정 순서(2-4), 상태별 키.
- **B10** — STARTING · STOPPED 때 비운다.
- **B11** — 수신기 자리 합이 스택 수 이상일 때만 받는다. 정해진 순서로 나눠 담는다.
- **C3** — `sender_status` / `no_receiver`.
- **C4** — 웅크리고 빈손이면 채널을 보여 준다. 수신기 이름은 `ender_receiver.titled`.
- **C5** — `water_full` / `other_fuel` / 힌트 4종.
- **C11** — `HOME` 단계(2-4 사양), steam 꾸러미 보일러 2개, start 꾸러미 레드스톤 4 · 참나무 판자 16.
- 시험:
  - 벨트 끝 분쇄기: 석탄 → 연료 칸, 원석 철 → 재료 칸.
  - 빈 줄이면 모터 증기가 줄지 않음.
  - `alone_quarry`: 흑요석 남김.
  - pending 떨굼.
  - 송신기: 자리 4일 때 16 거부 · 4 수락, 수신기 둘에 나눠 담김.
  - 굴착기 상태: 시작 전 NOT_STARTED, 위 상자 없음 → BLOCKED.

**F3 · 보스 · 제단** — `SanupcraftFixBossGameTest`
- 파일: `boss/RustyGuardian.java`, `boss/FurnaceWarden.java`, `boss/GuardianAltarBlock.java`
- **D1** — 12틱에 잠그고 경로를 `CRIT`으로 표시, 20틱 재계산 제거, `chargeDirection()` getter.
- **D2** — 10틱에 잠그고 매 틱 덮어쓴다, ±35° `FLAME` 경계.
- `tickPattern` 공개(시험용).
- **D3** — 패턴 타격은 방패를 깨고 맞힌다. 근접은 `getSecondsToDisableBlocking` 5.
- **D5** — `slamReach` 고리 · 판정, `predictLanding` 착지 고리, 3틱마다 32개.
- **C7②** — `rust_warning` · `heat_warning`, 플레이어별 200틱 쿨다운.
- **D8** — 거미줄 무시, 구속 거부.
- **D10** — `hasRoom`은 블록만 본다. `useItemOn`에서만 쓴다.
- **D11** — 이름표를 보스 바에 반영, 평화로움이면 거부.
- **A1 짝** — `RustyGuardian.hurtServer`의 1.5배를 강철 검 **또는 내열 검**에.
- 시험(모두 `setNoAi(true)`로 결정적으로):
  - 돌진 잠금(13틱에 목표 이동 → 방향 유지).
  - 분출 잠금(11틱에 옆으로 → 피해 없음).
  - `slamReach` 경계 안팎 피해.
  - 구속 거부.
  - `hasRoom` 거짓(2칸 위에 돌).
  - 이름표 → `bossEvent` 이름.
  - 내열 검 → 수호자 1.5배.

**F4 · 장비 데이터 · 길잡이 · 문구 · 문서** — `SanupcraftFixGuideGameTest`
- 파일: `gear/SteelGear.java`, `gear/HeatproofGear.java`, `recipe/heatproof_{helmet,chestplate,leggings,boots,sword}.json`, `recipe/ender_sender.json`, `advancement/*.json`(+ 새 `press.json`), `lang/ko_kr.json`, `lang/en_us.json`, `client/.../SanupcraftClient.java`, `README.md`, `ROADMAP.md`, `docs/PLAYTEST.md`, `SanupcraftM3GameTest.java`(press 한 줄)
- **A1** — 레시피 4종(2-1 사양) + `wearsFullSet`는 부위마다 강철 또는 내열.
- **A2** — 공격 보너스 3.0, 검 레시피 `["S","G","W"]`.
- **A4** — 송신기 레시피에 심장.
- **C1** — `press` 과제, alloy 부모 변경.
- **C2 · C7①** — `ItemTooltipCallback`으로 `<descriptionId>.tooltip` / `.tooltip2`.
- 문구 전부.
- 문서:
  - 동작 변화: 2-2 끝 목록 + 벨트 끝 연료 규칙, 굴착기 라인 문구(A5), 내열 = 강철 상위(녹도 막음), 송신기에 심장, 핵 8개는 ROADMAP 한 곳만.
  - PLAYTEST 체크 항목 추가: 조약돌 너겟 · 해머 처리량 · 기둥 공략 · 방치된 보스로 제단 busy · 기계 화면 툴팁.
  - GameTest 총수는 합친 뒤 오케스트레이터가 고친다.
- 시험:
  - 내열 5종 레시피에 강철 부위(검은 강철 검)가 재료로 들어감, 송신기 레시피에 심장.
  - 내열 검 공격력 = 다이아 검.
  - 강철 + 내열을 섞은 세트 → `SteelGear.wearsFullSet` 참.
  - **문구 키 시험**: 클래스패스의 `/assets/sanupcraft/lang/ko_kr.json` · `en_us.json`을 Gson으로 읽는다. 새 키 · 바뀐 키가 둘 다에 있는지 본다. `%s` · `%n$s` 개수가 기대값과 같은지 본다(정본: 2-1 F4 표의 인수 목록).
    - 2개: `steam_quarry.not_started`, `steam_quarry.blocked`, `ender_link.sender_status`, `ender_receiver.titled`, `gui.sanupcraft.machine.steam`
    - 4개: `steam_quarry.no_steam`
    - 1개: `ender_link.no_receiver`
    - 0개: 나머지 새 키

**바뀐 문구 (2-1 F4 표를 덮어씀)**
| 키 | 문구 |
|---|---|
| `block.sanupcraft.guardian_altar.no_room` | 공간이 좁다: 제단 위로 3x3 · 높이 4칸을 비우자. |
| `advancements.sanupcraft.guardian_core.description` | 핵 하나 = 강화 기계 틀 하나, 또는 분쇄기로 핵 조각 4개. 틀과 조각이 여러 곳에 들어가 수호자를 여러 번 잡게 된다. |
| `advancements.sanupcraft.heatproof.description` | 내열 = 강철 장비 + 기계 부품 + 핵 조각(인챈트는 사라진다). 녹 공격도 그대로 막고, 한 벌이면 불 · 용암에 강해진다. 네더 요새에서 블레이즈 막대를 구하자. |
| `advancements.sanupcraft.ender_link.description` | 송신기(파수꾼의 심장 필요)와 수신기를 같은 염료 색으로. 차원을 넘어 가지만 수신기 쪽이 로드돼 있어야 한다. |
| `item.sanupcraft.heatproof_{helmet,chestplate,leggings,boots}.tooltip` | 한 벌: 화염 저항 · 과열과 녹 공격을 막는다 (강철 한 벌 대신) |
| `item.sanupcraft.heatproof_sword.tooltip` | 녹슨 수호자 · 용광로 파수꾼에게 피해 1.5배 |

순서: F1~F4 병렬. 합치는 순서는 F1 → F2 → F3 → F4. 마지막에 `./gradlew build`를 돌리고 문서의 시험 수를 고친다.

## 3. 수정 기록
Sonnet 4기가 각자 작업 트리에서 고치고 `./gradlew build`로 확인한 뒤, 오케스트레이터가 F1 → F2 → F3 → F4 순서로 합쳤다. 같은 파일을 두 묶음이 고친 일은 없다 (충돌 0).

| 묶음 | 커밋 | 항목 | 추가 시험 | 작업 트리 단독 결과 |
|---|---|---|---|---|
| F1 기계 · 보일러 | `c1735cf` | B1(+A5 거름) · B5 · B7 · B8(기계 · 분배 회전) · C9 | `SanupcraftFixFactoryGameTest` 9 | 58 통과 |
| F2 벨트 · 굴착기 · 엔더 · 보일러 블록 · 실테스트 | `149f7fb` | B2 · B12 · B3 · B4 · B8(굴착기) · C6 · B10 · B11 · C3 · C4 · C5 · C11 | `SanupcraftFixLogisticsGameTest` 10 | 59 통과 |
| F3 보스 · 제단 | `9ae32d1` | D1 · D2 · D3 · D5 · C7(전투 경고) · D8 · D10 · D11 · 내열 검 수호자 보너스 | `SanupcraftFixBossGameTest` 12 | 61 통과 |
| F4 장비 데이터 · 길잡이 · 문구 · 문서 | `ec6811c` | A1 · A2 · A3(문서) · A4 · A5(문서) · C1 · C2 · C7(툴팁 · 설명) · C8 · C10 · C12 · 모든 문구 | `SanupcraftFixGuideGameTest` 7 (+ M3 시험에 `press`) | 56 통과 |
| 합치기 맞춤 | `7db498a` | 보일러 힌트: 증기 가득을 연료 없음보다 먼저 (F1이 증기 가득일 때 불을 멈추게 바꿔서 F2 힌트가 "연료 없음"으로 잘못 나오던 것) | | |

사양에서 벗어난 점 (각 묶음 보고에서):
- F1: 보일러 연료 아낌 시험을 1700틱 대신 320틱에서 판정(결정적 · 빠름). `consumeFuel(ItemStack) → 남은 것` 시그니처(2-4).
- F2: 굴착기 시작 전 상태는 첫 층(Y-1)을 보여 줌. `home` 단계는 오버월드에 있어도 월드 스폰으로 보냄. B10 · 굴착기 POWERED 유지 · home 이동은 시험 없음.
- F3: 기존 시험이 쓰는 3인자 `hitWithRust` · `hitWithHeat`는 남겨 둠(pattern=false). 제단 거부 순서 too_cold → peaceful → busy → no_room. 전투 경고 오버레이는 ServerPlayer에만 나가서 자동 검증 못 함.
- F4: `iron_plate` 과제에서 깔때기 문장 삭제(crusher로 옮김), 툴팁은 `<아이템>.tooltip(2)` 키가 있으면 붙이는 방식.

합친 뒤 확인:
- `./gradlew build`: **GameTest 87개 전부 통과** (전: 49).
- 편의 모드 서버 4개(Lithium · FerriteCore · Simple Voice Chat · Corpse Graves)와 같이 87개 통과.
- 코드가 쓰는 번역 키 71개가 ko · en 양쪽에 다 있음.
- `runClientGameTest`: 기계 · 합금로 화면, 길잡이 탭(새 `press` 과제), 네더 · 엔드 장면 정상. 기계 화면 칸 툴팁 · 전투 경고 오버레이는 마우스 · 실제 플레이어가 필요해 스크린샷으로 확인 못 함 → 실테스트 항목.

## 4. 보고
| 단계 | 지적 | 실제 문제로 판정 | 수정 | 테스트 |
|---|---|---|---|---|
| 1.5단계 (Sonnet 4 → Opus · 비판적 Sonnet → Sonnet 4) | 46 | 34 수정 대상 (보류 6, 기각 3, 중복 3) | 34 (건너뜀 0) | 49 → 87 전부 통과 |

- 2단계로 올린 것: 없음 (같은 문제로 테스트가 연속 실패한 적 없음).
- 2단계 후보 (주인 결정 필요): B6 엔더 채널 소유자, D6 + A10 인원수 보스 체력 · 기여자 전리품, C2 JEI 플러그인, A5 굴착기 쓰레기 버리기.
- 보류 (실테스트 뒤 판단): A9 해머 근접 피해, D4 기둥 공략 대응, D9 방치된 보스 회복, D12 패턴 추가.
