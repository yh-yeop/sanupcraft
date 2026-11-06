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

## 5. 2단계 후보 구현 (2026-11-04)
주인이 "실테스트 먼저, 그동안 고도화"를 고른 뒤, 1.5단계에서 넘긴 2단계 후보 넷을 기본값으로 구현했다. 숫자 · 범위는 ROADMAP "2단계"에 모았고 실테스트 뒤 다시 정한다.

| 후보 | 고른 기본값 | 고친 곳 | 시험 |
|---|---|---|---|
| D6 + A10 보스 인원 | 체력 × min(3, 1 + 0.5(n−1)), 장판 3~6, 졸개 3 + (n−1)/3, 5% 이상 기여자 각자 핵 · 심장 | `boss/BossParty`(새), `PartyBoss`(새), `RustyGuardian`, `FurnaceWarden`, `GuardianAltarBlock`, 전리품 표 2개 | `SanupcraftStage2GameTest` 5개 |
| A5 굴착기 쓰레기 | 태그 `quarry_junk`, 기본 켜짐, 웅크리고 빈손 우클릭으로 끄기 | `mining/QuarryBlockEntity`, `QuarryBlock`, `ModMining`, 태그 | 1개 (+ 기존 2개는 버림 끄고 돌림) |
| B6 엔더 채널 | 최소 안전판: 놓은 사람 = 주인, 잠그면 주인 · OP만 열기 · 채널 바꾸기 (기본 열림) | `logistics/EnderLinkBlock`, `EnderLinkBlockEntity` | 1개 |
| C2 JEI | 기계 5종 화면 · 기계 블록이 촉매 · 부산물 확률 · 처리 시간 | `client/jei`(새), `build.gradle`(compileOnly), `fabric.mod.json`(`jei_mod_plugin`) | 클라이언트 시험 로그 "기계 레시피 19개" |

- `./gradlew runGameTest`: **94개 전부 통과** (전: 87).
- `runClientGameTest` + 편의 모드 11개: 통과, JEI 플러그인 오류 없음.
- 남은 보류: A9 · D4 · D9 · D12 (실테스트 뒤).

## 6. 2차 리팩터링 1단계 (2026-11-06)

대상: 지난 리팩터링(`dcdf97c`) 이후 바뀐 본 코드 — 2단계(보스 인원 배율 · 굴착기 버림 · 엔더 잠금 · JEI), M4(지열 분출구 · 내열 드릴 · 엔더 닻), 이번 고도화(녹슨 사당 · 요새 · 보루 전리품 · 드릴 재탐색).
절차: Haiku 4기 병렬(파일 묶음 겹치지 않게) → Opus 1기 판정 → 비판적 Sonnet 반론 → Opus 최종 결정 → Sonnet n기 수정.
기준 시험: `./gradlew build` (GameTest 108개), 화면 쪽은 `runClientGameTest`.

### 6-1. 지적 (Haiku 4기)

#### H1 보스 (`boss/`)
1. FurnaceWarden.java:181-185 · 버그 · 중 — 화염 장판 수는 `party.size()`(48칸 인원)로 정하지만 위치는 `inflate(20)` 안 플레이어에게서만 뽑는다. 20~48칸에 있으면 장판이 0개 → 대상 한 곳 폴백. 혼자면 장판 1개인데 주석은 "3 ~ 6곳".
2. FurnaceWarden.java:146-157 · 주석 불일치 · 하 — "보스 바 제목 (시험용)" 주석이 `party()` 위에 붙어 있고 `bossBarName()`에는 없다.
3. RustyGuardian.java:441-467 / FurnaceWarden.java:541-570 · 중복 · 중 — `actuallyHurt` · `dropCustomDeathLoot` · party 저장/불러오기 · `party()`가 두 보스에 복사돼 있다. 키 아이템만 다르다.
4. BossParty.java:55, 113-117 · 죽은 코드 · 하 — `seen` 폴백은 끊긴 플레이어(`isRemoved()` true)에 거의 안 쓰이고, 끊긴 플레이어 객체를 보스가 죽을 때까지 붙잡는다.
5. BossParty.java:105-124 · 기타 · 하 — `contributors` 결과 정렬이 결과에 영향이 없다.
6. GuardianAltarBlock.java:73-77 · 중복 · 하 — `party().size()`를 두 번 읽는다.

#### H2 채굴 · 동력 (`mining/` · `power/`)
7. QuarryBlockEntity.java:150-164 · 버그 · 중 — `flush()`가 pending을 줄여도 `setChanged()`가 없다. 상자로 보낸 뒤 저장되면 재로드 때 같은 아이템이 다시 나올 수 있다.
8. GeothermalVentBlockEntity.java:95-104 · 버그 · 하 — `made == 0`이어도 `distribute()`가 증기를 줄이는데 `setChanged()`가 없다.
9. QuarryBlockEntity.java:37-38, 90-106 · 주석 불일치 · 하 — `MAX_SCAN = 81`은 한 층. 빈 층마다 4틱이 걸려 "공기 층에서 멈추지 않게"와 다르고, 드릴 재탐색이 판 층 수 × 4틱 늦다.
10. QuarryBlock.java:77, 84 · 기타 · 하 — 다 판 뒤 표시 층이 minY-1 (-65).
11. BoilerBlockEntity.java:93 · 기타 · 하 — 유휴 상태에서도 매 틱 `setChanged`.
12. BoilerBlockEntity.java:96-104 / GeothermalVentBlockEntity.java:106-115 · 중복 · 하 — 6방향 원천 유체 세기가 같은 패턴.

#### H3 기계 · JEI · 월드 · 실테스트
13. SanupcraftJeiPlugin.java:116-123 · 버그 · 중 — `JsonParser`의 언체크 예외는 `catch (IOException)`에 안 잡혀 등록 전체로 번지고, 한 파일의 IOException은 그 기계 폴더 나머지를 건너뛴다. 주석("못 읽은 파일은 건너뛰고")과 다르다.
14. PlaytestCommand.java:167-168 · 중복 · 하 — `give(p, Items.COAL, 64)` 두 줄 (석탄 128).
15. make_structures.py:20-25 · 죽은 코드 · 하 — `Int` · `Str` 클래스 미사용.
16. PlaytestCommand.java:124 · 버그 · 하 — 네더 목적지를 `x / 8`로 계산해, 엔드에서 부르면 엔드 좌표 / 8이 된다.
17. make_structures.py:141 · 기타 · 하 — 사슬이 높이 5 기둥의 y=4 석재를 덮어써 기둥 사이에 낀다.
18. docs/PLAYTEST.md:31 · 문서 불일치 · 하 — `outpost` 받는 것에 상자 2 · 깔때기 4가 빠짐.

#### H4 물류 (`logistics/`)
19. EnderAnchorBlockEntity.java:91 · 버그 · 중 — ACTIVE 등록이 tick에서만. 재시작 직후 아직 tick 안 돈 닻 A의 청크를, 먼저 꺼지는 닻 C가 `coveringAnchor`로 못 찾고 풀어 버린다. A는 `forced=true`를 읽어 와서 다시 붙잡지 않는다.
20. EnderAnchorBlockEntity.java:156-166 · 버그 · 중 — `setRemoved` 처리 없음. 청크 언로드로 BE가 내려가면 ACTIVE에 죽은 BE가 남는다 (EnderLinkBlockEntity는 `setRemoved`에서 해제).
21. EnderAnchorBlockEntity.java:101-112 · 기타 · 하 — 이미 외부 강제 로드된 자기 청크는 소유하지 않아, 20번 상황과 겹치면 아무도 안 푼다.
22. EnderLinkBlock.java:86-88, 98-110 · 주석 불일치 · 하 — "아니면 채널과 잠금 상태를 보여 준다"지만 안 잠긴 상태면 채널만.
23. EnderLinkBlockEntity.java:108-110, 42 · 주석 불일치 · 하 — 주인 없는 블록(옛 데이터)은 아무나 잠그고 주인이 된다. 주석은 "주인 · OP만".
24. EnderLinkBlockEntity.java:45-62 · 죽은 코드 · 하 — 송신기의 items 칸은 안 쓰인다.
25. EnderLinkBlockEntity.java:216-217 · 기타 · 하 — `level == null`이면 아이템을 조용히 버린다.
26. EnderLinkBlock.java:61-76 · 기타 · 하 — 클라이언트는 `canUse`를 안 보고 SUCCESS (잠긴 수신기에 염료 연출).
27. EnderAnchorBlock.java:44-49 · 기타 · 하 — 티커가 람다 인자 대신 바깥 `serverLevel`을 캡처.
28. EnderAnchorBlockEntity.java:85-87 · 기타 · 하 — 200틱마다 불필요한 `setChanged`.

### 6-2. Opus 판정
집계(28건): **수정 대상 8** · 보류 3 · 오탐 17 · 2단계로 0. 수정 대상 중 1 · 2 · 15 · 18은 주석 · 문서 · 도구만(동작 변화 없음).
검증 메모:
- 26.3 바닐라: `ServerLevel.unload` → `LevelChunk.clearAllBlockEntities()` → 모든 BE의 `setRemoved()`. 블록이 부서질 때도 `removeBlockEntity` → `setRemoved()`. 또 청크에 BE를 넣을 때 `setLevel` → `clearRemoved()` 순서(`LevelChunk.setBlockEntity`). → 20번 전제(청크가 내려가면 `setRemoved`)는 맞다. 다만 `coveringAnchor`가 이미 `isRemoved()`를 걸러서 오작동은 없고, 남는 것은 죽은 BE 참조(서버 종료 때 `clearActive`로 비움)뿐이다.
- 7번(굴착기 `flush`): `HopperBlockEntity.addItem` → `tryMoveInItem`이 넣는 데 성공하면 `container.setChanged()`를 부르고, 위 블록(상자 · 깔때기 · 벨트 · 송신기)은 굴착기와 **같은 청크 기둥**이라 `blockEntityChanged` → `markUnsaved()`가 굴착기 청크를 표시한다. 청크가 저장되면 굴착기 BE(줄어든 pending)도 같이 저장되므로 복제 없음. 예외는 위에 상자 광산 수레(엔티티 컨테이너)를 둘 때뿐 — 실제 배치에서 쓰지 않는 극단이라 오탐.
- `makeMockServerPlayer`(시험의 모의 플레이어)는 `PlayerList`에 안 들어간다 → 4번 `seen` 폴백은 시험(`SanupcraftStage2GameTest` 보상 시험)이 기대고 있다.

| 번호 | 판정 | 근거 (검증) | 수정 방향 |
|---|---|---|---|
| 1 | 수정(주석 · 문서) | 동작은 2단계 전과 같은 설계: 장판은 20칸 안 플레이어 발밑마다, 개수 상한만 인원 비례(`clamp(n,3,6)`). 20칸 밖이면 대상 폴백, 혼자 1개도 예전(`MAX_RINGS = 3`)과 같다. 틀린 건 `maxRings()` 주석 "한 번에 까는 화염 장판 수"와 ROADMAP 256행 "화염 장판 = 인원수(3~6개)" — 개수가 아니라 상한 | `FurnaceWarden.maxRings()` 주석 → "화염 장판 상한 (인원수, 3 ~ 6). 장판은 20칸 안 플레이어 발밑마다 하나". ROADMAP 256행 → "화염 장판 = 20칸 안 플레이어마다 하나, 상한 인원수(3~6)". 시험 변경 없음 |
| 2 | 수정(주석) | 확인: `FurnaceWarden` 146행 "보스 바에 보이는 제목 (시험용)"이 `party()` 위에 있고 `bossBarName()`에는 없다 (`RustyGuardian`은 제자리) | 주석을 `bossBarName()` 위로 옮긴다. 1번과 같은 파일 |
| 3 | 보류 | 두 보스의 `actuallyHurt` · `dropCustomDeathLoot` · 저장/불러오기는 각각 `BossParty`로 3~4줄 위임뿐. 묶으려면 `Monster`와 두 보스 사이에 공통 부모 클래스를 넣어야 함(인터페이스 default로는 protected 오버라이드 불가) → 구조 변경 대비 이득 작음 | 보스가 셋째로 늘 때 공통 부모(`PartyBossMonster`)로 묶을지 정한다 |
| 4 | 오탐 | `seen`은 `PlayerList`에 없는 플레이어용. 시험의 `makeMockServerPlayer`가 정확히 그 경우라 지우면 보상 시험이 깨진다. 끊긴 플레이어 참조는 보스가 죽거나 내려갈 때 같이 사라짐 | |
| 5 | 오탐 | `damage`가 `HashMap`이라 정렬이 보상 순서 · 메시지 순서를 결정적으로 만든다. 무해 | |
| 6 | 오탐 | `party().size()`는 필드 읽기. 노이즈 | |
| 7 | 오탐 | 위 검증 메모: 넣기 성공 시 대상 `setChanged()`가 같은 청크를 표시 → 굴착기도 함께 저장. 광산 수레 위 극단만 예외 | |
| 8 | 수정 | 확인: `work`가 `made > 0`일 때만 `setChanged`, 그런데 `distribute`는 `made == 0`(용암 없음 · 오버월드)이어도 증기를 내보낸다 → 저장값이 낡아 다시 불러오면 최대 1000 증기가 되살아남. 작지만 확실, 한 줄 수정 | `GeothermalVentBlockEntity.work`: `int before = steam;` … 끝에서 `if (steam != before) setChanged();` (`made > 0` 조건 대체). 시험 선택(필요 없음) |
| 9 | 오탐 | `MAX_SCAN = 81` = 한 번에 한 층을 다 훑음 → "공기 층에서 멈추지 않게"(칸마다 4틱 쓰지 않게)와 맞다. 드릴 재탐색은 판 층마다 4틱(100층 ≈ 20초) — `installDrill` 주석 "빨리 지나간다"의 범위 안 | |
| 10 | 오탐 | `steam_quarry.finished` 문구는 ko · en 둘 다 `%2$s`(캔 수)만 써서 층(−65)이 안 보인다 | |
| 11 | 오탐 | 1.5단계 B9에서 같은 지적을 기각함(보일러 수 적음, `setChanged` = 청크 표시 + 비교기 이웃 갱신). 새 근거 없음 | |
| 12 | 오탐 | 물(있나, bool)과 용암(몇 개, 상한 4)은 뜻이 달라 6줄 반복을 묶을 이득 없음 | |
| 13 | 수정 | 확인: `JsonParser.parseReader`의 `JsonParseException`(언체크)은 어디서도 안 잡혀 JEI 등록으로 번지고, 한 파일의 `IOException`은 바깥 `catch`로 빠져 그 기계 폴더 나머지를 건너뛴다 — 주석 "못 읽은 파일은 건너뛰고"와 다름. 멀티(전용 서버) 클라이언트 경로라 시험에 안 걸림 | `fromModJar`: 파일마다 `try (Reader …) { … } catch (IOException \| JsonParseException e) { LOGGER.warn(…, file, e); }`로 그 파일만 건너뜀. 바깥 `catch (IOException)`은 `Files.walk` 실패용으로 남김. 시험: 없음(클라이언트 전용) — `./gradlew build` + 가능하면 `runClientGameTest` 로그 "기계 레시피 19개" 유지 |
| 14 | 오탐 | 석탄 128이 의도(문서 표 "석탄"). `give`가 64씩 두 번이든 128 한 번이든 결과 같음. 노이즈 | |
| 15 | 수정(도구) | 확인: `Int` · `Str` 클래스는 어디서도 안 쓰임(`_tag_id`는 `int` · `str`로 판별) | 두 클래스 삭제. 출력 `.nbt`에 영향 없음 — 다시 돌리지 않아도 됨(돌려도 `mtime=0`이라 같은 바이트여야 함, 다르면 되돌림) |
| 16 | 오탐 | 엔드에서 `nether` 단계를 부르면 엔드 좌표 / 8에 흑요석 발판을 깔 뿐 — 실테스트 명령이라 어디든 서기만 하면 됨 | |
| 17 | 보류 | 확인: (8,8) 기둥(높이 5)의 y=4 석재를 사슬이 덮어 석재 사이에 사슬이 낀다. 다만 "무너진 기둥" 연출일 수도 있는 모양 판단이고, 고치면 `.nbt`를 다시 만들어야 함 | 클라이언트 스크린샷(사당 장면)에서 주인이 보고 정한다 |
| 18 | 수정(문서) | 확인: `kitOutpost`는 상자 2 · 깔때기 4를 주는데 PLAYTEST 표에 없음. 같은 표에서 `start`(레드스톤 4 · 참나무 판자 16, C11에서 추가)와 `end`(조약돌 64)도 빠져 있음 | `docs/PLAYTEST.md` 받는 것 표: `outpost`에 "상자 · 깔때기", `start`에 "레드스톤 · 판자", `end`에 "조약돌" 추가. 코드(`PlaytestCommand`)를 정본으로 한 번 대조 |
| 19 | 수정 | 확인: `ACTIVE` 등록이 `tick`에서만. 서버를 다시 켠 첫 틱에 꺼지는 닻 C가, 아직 한 번도 안 돈 닻 A(겹치는 청크, `forced = true`로 불러옴)를 `coveringAnchor`로 못 찾아 공유 청크를 풀고, A는 `forced`라 다시 안 붙잡는다. 창은 첫 틱뿐이라 드물지만 결과가 "전초기지 청크가 조용히 내려감"이라 크다. 수정은 국소적 | `EnderAnchorBlockEntity`: `clearRemoved()` 오버라이드(청크에 들어갈 때 `setLevel` 다음에 불림) → `super` 뒤 `level instanceof ServerLevel && fuelTicks > 0`이면 `ACTIVE`에 등록(`tick`과 같은 등록 코드를 `private void track(ServerLevel)`로 묶어 공유). 시험: 닻 C · A를 같은 청크에 두고 C 먼저 tick(C가 소유) → A tick → `clearActive()`(재시작 흉내) → `A.clearRemoved()` → C를 부숨 → 가운데 청크가 여전히 강제 로드 · A가 넘겨받음(`ownChunkCount() > 0`) |
| 20 | 수정 | 위 검증 메모: 26.3은 청크가 내려갈 때 `setRemoved`를 부른다. 지금은 `ACTIVE`에 죽은 BE가 서버 종료까지 남음(오작동은 `isRemoved()`로 걸러짐). `EnderLinkBlockEntity`와 같은 방식으로 맞춘다 | `setRemoved()` 오버라이드 → 그 차원 `ACTIVE`에서 `this`만 빼고 `super.setRemoved()`. **청크는 풀지 않는다**(서버 종료 때도 모든 청크가 내려가며 불리므로 여기서 풀면 재시작 지속이 깨짐 — 푸는 건 지금처럼 `preRemoveSideEffects` · 연료 끝에서만). 시험: 시험용 `isTracked()` getter, 켜진 닻에 `setRemoved()` → false |
| 21 | 보류 | 맞음: 자기 청크가 이미 외부(`/forceload`)로 붙잡혀 있으면 소유하지 않으므로, 관리자가 나중에 그 청크를 풀면 닻 BE가 내려가 연료가 멈추고 둘레 8청크는 계속 붙잡힌다. 고치려면 "외부 강제 로드 청크는 건드리지 않는다"는 규칙 자체를 바꿔야 함 | 정책 결정: 닻이 자기 청크는 늘 소유(외부 표시와 별개로 기록)할지, 또는 지금 규칙 유지(관리자 책임). 실테스트 · 서버 운영 뒤 |
| 22 | 오탐 | 잠긴 경우 `lockedMessage`(채널 · 주인), 안 잠긴 경우 채널 — "채널과 잠금 상태"의 뜻 그대로. 노이즈 | |
| 23 | 오탐 | `canManage` 주석이 "주인이 없을 때 (처음 잠그는 사람이 주인이 된다)"로 이미 밝힌 의도된 규칙(주인 필드 전 옛 데이터용) | |
| 24 | 오탐 | 송신기의 칸 1개는 깔때기 · 벨트가 넣을 자리(`getContainerSize` ≥ 1)로 필요. `getItem`이 늘 빈 것을 돌려줄 뿐 죽은 코드 아님 | |
| 25 | 오탐 | `level == null`은 월드에 안 놓인 BE — 깔때기 · 벨트가 접근할 수 없는 상태 | |
| 26 | 오탐 | 클라이언트는 잠금 상태를 모름(동기화 안 함). 손 흔들기 연출뿐이고 서버가 거부 · 안내, 염료 소모도 없음 | |
| 27 | 오탐 | 티커는 그 BE의 레벨마다 만들어져 `l`과 `serverLevel`은 같은 객체. 동작 차이 없음 | |
| 28 | 오탐 | 200틱마다 `setChanged`는 `fuelTicks` 감소를 주기적으로 저장하려는 의도(매 틱 표시를 피함) | |

### 6-3. 수정 묶음 (파일 소유)
세 묶음은 파일이 겹치지 않는다. 각자 작업 트리에서 고친 뒤 `./gradlew build`(GameTest 108 + 새 시험) 통과 확인. 2단계로 올린 것 없음.

| 묶음 | 항목 | 소유 파일 (이 묶음만 수정) | 시험 |
|---|---|---|---|
| **G1 엔더 닻 등록** | 19, 20 | `logistics/EnderAnchorBlockEntity.java`, `src/gametest/.../SanupcraftNetherEndGameTest.java`(닻 시험 추가만) | 새 시험 2개: 재시작 흉내 뒤 넘겨받기(19) · `setRemoved` 뒤 `isTracked()` false(20). `clearActive()`는 같은 틱 안에서만 쓰고, 다른 시험의 켜진 닻은 다음 틱 `tick`에서 다시 등록되므로 영향 없음 — 그래도 시험 끝에서 두 닻을 부숴 정리. 시험 구역 청크가 이미 강제 로드돼 있을 수 있으니 기존 `anchorForcesChunksAndReleasesOnlyItsOwn`처럼 `centerWasForced`를 먼저 읽고, 판정은 C가 실제로 소유했던 청크로 한다 |
| **G2 지열 · JEI** | 8, 13 | `power/GeothermalVentBlockEntity.java`, `client/.../jei/SanupcraftJeiPlugin.java` | 새 시험 없음. `./gradlew build`, 가능하면 `runClientGameTest`로 JEI 오류 없음 확인 |
| **G3 주석 · 문서 · 도구** | 1, 2, 15, 18 | `boss/FurnaceWarden.java`(주석 두 곳만, 코드 변경 금지), `ROADMAP.md`(256행 한 줄), `docs/PLAYTEST.md`(받는 것 표), `tools/make_structures.py` | 시험 변경 없음. `./gradlew build`로 컴파일만 확인. `.nbt`는 다시 만들지 않음 |

보류 3건(3 공통 부모, 17 사당 사슬, 21 외부 강제 로드 청크)은 ROADMAP에 옮기지 않고 이 표로만 넘긴다 — 최종 결정 단계에서 실테스트 항목으로 둘지 정한다.

### 6-4. 비판적 Sonnet 반론
판정당 1회. 근거는 코드 · 바닐라 소스를 직접 읽은 것 (`LevelChunk` · `BlockEntity` · `HopperBlockEntity` · `CompoundContainer` · `Level.blockEntityChanged`). 최종 결정은 Opus.
28건 중 **반론 2건**(아래 R1 · R2), 동의하되 보정이 필요한 1건(R3), 그리고 질문받은 지점의 검증 결과(V1~V4)를 적는다. 나머지 판정은 읽어 보고 동의라 건너뛴다.

| 번호 | 반론 | 근거 | 요청 | 확신도 |
|---|---|---|---|---|
| R1 (#9 주변, 지적에 없던 항목) | #9를 오탐으로 닫으면서 드릴 재탐색 변경의 **사용자 문구**를 안 봤다. 드릴을 끼울 때 나오는 안내가 이제 반대 말을 한다: ko "(이미 지나간 층은 다시 보지 않는다)", en "(layers already passed are not revisited)". 실제로는 `installDrill()`이 `started = false` · `finished = false` · `cursor = 0`으로 맨 위층부터 다시 훑는다. 플레이어가 "드릴 끼워도 지나간 층 흑요석은 못 캔다"고 믿고 굴착기를 새로 놓게 된다 | `src/main/resources/assets/sanupcraft/lang/ko_kr.json:109`, `en_us.json:109` (`block.sanupcraft.steam_quarry.drill_installed`). 같은 변경이 ROADMAP 181행("~~다시 안 본다~~ → 맨 위층부터 다시 훑는다")과 `QuarryBlockEntity.installDrill` 주석(219-220)에는 반영돼 있고 번역 파일만 빠짐 | **수정 대상 추가**: 두 번역 줄의 괄호를 "(맨 위층부터 다시 훑는다)" / "(rescans from the top layer)"로. 파일 소유는 G1 · G2 · G3 모두 안 건드리므로 G3에 넣으면 겹침 없음 | 상 |
| R2 (#1 수정 방향) | 수정 방향의 **ROADMAP 줄 번호가 틀렸다**. "ROADMAP 256행"이라 했지만 이 작업 트리 ROADMAP 256행은 "### 실테스트에서 볼 것" 제목이다. 고칠 문장("화염 장판 = 인원수(3~6개)")은 **275행** (보스 인원 배율 항목 안). 묶음 담당 Sonnet이 줄 번호만 믿으면 엉뚱한 줄을 고친다 | `grep -n "화염 장판" ROADMAP.md` → 66 · 80 · 81 · 99 · 105 · 275. 인원수 문장은 275행뿐 | 6-3의 G3와 #1의 "256행"을 "`화염 장판 = 인원수(3~6개)` 문장(275행 근처, 줄 번호 말고 문구로 찾기)"으로 | 상 (사소하지만 확실) |
| R3 (G1 시험 설계, 판정 유지 · 사양 보정) | 시험이 **아무것도 안 지키고 통과**할 수 있다. 시험 구역 청크는 이미 강제 로드돼 있을 수 있어서(`anchorForcesChunksAndReleasesOnlyItsOwn`가 `centerWasForced`를 읽는 이유) C가 소유한 청크가 0개면, 고치기 전에도 "C를 부숴도 청크가 그대로"가 참이 된다. 6-3은 "C가 실제로 소유했던 청크로 판정"이라 했지만 소유 목록은 `private`이고 공개된 건 `ownChunkCount()`뿐이다 | `EnderAnchorBlockEntity.java:109-112`(`already.contains`이면 `ownChunks`에 안 넣음), 시험 `SanupcraftNetherEndGameTest.java:136` | 사양 보정: C를 tick한 직후 `helper.assertTrue(c.ownChunkCount() > 0)`를 **전제**로 걸어 0이면 시험이 실패하게 하고(조용히 통과 금지), 판정은 `c.부수기 → a.ownChunkCount() == (부수기 전 c.ownChunkCount())`(넘겨받은 수가 같다)로. 시험 안에서 `clearActive()` · `clearRemoved()`는 같은 틱 안에서 끝내고 `helper.succeed()` (실제 BE 티커가 다음 틱에 다시 등록하므로 다른 시험엔 영향 없다는 6-3 판단에 동의) | 중 |

#### 질문받은 세 지점 검증 (반론 아님)
- **V1. G1 순서 (19)**: 청크를 읽을 때 `promotePendingBlockEntity` → `BlockEntity.loadStatic`(→ `loadAdditional`로 `fuelTicks` · `forced` · `ownChunks` 읽음) → `setLevel` → `addAndRegisterBlockEntity` → `setBlockEntity` 안에서 `setLevel` 다음 `clearRemoved` (`LevelChunk.java:421-442`, `594-610`). 즉 `clearRemoved`가 불리는 시점엔 `fuelTicks`가 이미 읽혀 있다. 새로 놓는 닻은 `fuelTicks = 0`이라 등록 안 됨. `ACTIVE.clear()`는 `SERVER_STARTING`(월드 불러오기 전)에서만 불려(`ModLogistics.java:60-62`) 청크 불러오기 때의 등록을 지우지 않는다. 판정 · 수정 방향 동의.
- **V2. G1 언로드 때 `ACTIVE`에서 빼면 다른 닻이 청크를 잘못 풀 수 있나 (20)**: 일반 실행 중엔 닻 A의 둘레 3x3이 강제 로드(자기 또는 외부)라 켜진 A의 BE가 내려갈 일이 없다. 내려가는 건 서버 종료 때뿐이고 그땐 아무도 tick하지 않는다. 남는 경로는 21번(A의 자기 청크가 원래 외부 강제 로드였다가 관리자가 풀 때)뿐인데, 지금도 `coveringAnchor`가 `isRemoved()`로 A를 걸러서 결과가 같다 → 20번 수정이 새 오작동을 만들지 않는다. 동의. (참고: `/clone` · 구조물 붙여넣기는 NBT째로 `forced = true` · `own_chunks`를 복사해 복제 닻이 원본의 청크를 풀 수 있는 기존 문제가 있다. G1 때문에 생긴 게 아니고, 보류 21번 옆 메모로만 남긴다. 확신 하)
- **V3. #7 굴착기 `flush`의 더티 표시 (오탐 판정)**: 위 블록은 늘 굴착기와 같은 x · z 기둥이라 같은 청크이고, 모든 경로에서 `tryMoveInItem`이 성공 시 `container.setChanged()`를 부른다 (`HopperBlockEntity.java:341`). 벨트: `setItem`이 아이템 엔티티를 만들 뿐 직접 `setChanged`는 없지만 `tryMoveInItem`이 벨트 BE의 `setChanged`를 부름 → 같은 청크 표시. 엔더 송신기: `getItem`이 늘 빈 칸이라 `setItem` 경로를 타고 역시 송신기의 `setChanged`. 큰 상자: `CompoundContainer.setChanged`가 두 반쪽 다 표시 (`CompoundContainer.java:64-66`) — 바로 위 반쪽이 굴착기 청크. `Level.blockEntityChanged`는 청크 단위 `markUnsaved` (`Level.java:871-875`). 자동저장이 채굴(N틱)과 `flush`(N+1틱) 사이에 끼어도 N+1에서 다시 표시된다. 오탐 판정 동의.
- **V4. #9 재탐색 시간**: 루프는 한 번에 81칸 = 한 층이고 4틱마다 한 번 돈다(`QuarryBlockEntity.java:83, 90`). 빈 층 하나 = 4틱. 오버월드 Y 60 → −64면 124층 × 4 = 496틱 ≈ 25초, 네더 Y 120 → 0이면 120층 ≈ 24초, 오버월드 한계 높이(Y 300대)에서도 ≈ 75초. 그 동안 증기는 안 쓰이고(`mine`이 참일 때만 `steam -= 8`) `work`가 참을 돌려줘 POWERED도 유지된다. 한 번 끼우는 일회성 비용이라 허용 범위 — 오탐 판정 동의. 다만 R1 문구가 이 25초를 알려 주는 쪽이 낫다.

#### 반론하지 않은 판정 (읽고 동의)
2 · 8 · 13 · 15 · 18(`start` · `end` 행 빠짐도 `PlaytestCommand` 143-153 · 255-270행으로 확인) 수정 방향, 3 · 17 · 21 보류, 4 · 5 · 6 · 10 · 11 · 12 · 14 · 16 · 22~28 오탐.

### 6-5. 최종 결정 (Opus)
| 반론 | 결정 | 이유 |
|---|---|---|
| R1 드릴 안내 문구 | **받아들임 → 수정 대상 추가 (#29)** | 확인: ko · en `steam_quarry.drill_installed`가 "지나간 층은 다시 보지 않는다"인데 `installDrill`은 맨 위층부터 다시 훑는다. 플레이어를 잘못 이끄는 문구, 두 줄 수정 |
| R2 ROADMAP 줄 번호 | **받아들임** | 확인: 문장은 "보스 인원 배율" 항목 안(지금 275행). 줄 번호 대신 문구로 찾게 고침 |
| R3 G1 시험 전제 | **받아들임** | C가 소유한 청크가 0개면 고치기 전에도 통과한다. 전제 단언 + "넘겨받은 수 = C가 가졌던 수" 판정으로 바꿈 |
| V1~V4 | 동의 | 6-2 판정 그대로. V2의 `/clone` 복제 닻 메모는 보류 21 옆 메모로만 둔다 |

최종 집계(29건 = 지적 28 + 반론 추가 1): **수정 대상 9** (1 · 2 · 8 · 13 · 15 · 18 · 19 · 20 · 29) · 보류 3 (3 · 17 · 21) · 오탐 17 · 2단계로 0.

### 6-6. 최종 수정 묶음
파일은 묶음끼리 겹치지 않는다. 각자 고친 뒤 `./gradlew build`(GameTest 108 + G1 새 시험 2) 통과 확인. 위치는 줄 번호가 아니라 문구로 찾는다.

| 묶음 | 소유 파일 (이 묶음만 수정) | 시험 |
|---|---|---|
| **G1 엔더 닻 등록** (19, 20) | `src/main/java/io/github/yhyeop/sanupcraft/logistics/EnderAnchorBlockEntity.java`, `src/gametest/java/io/github/yhyeop/sanupcraft/test/SanupcraftNetherEndGameTest.java`(닻 시험 추가만) | 새 시험 2개 (아래) |
| **G2 지열 · JEI** (8, 13) | `src/main/java/io/github/yhyeop/sanupcraft/power/GeothermalVentBlockEntity.java`, `src/client/java/io/github/yhyeop/sanupcraft/client/jei/SanupcraftJeiPlugin.java` | 새 시험 없음. build, 가능하면 `runClientGameTest`에서 JEI 오류 없음 |
| **G3 주석 · 문서 · 문구 · 도구** (1, 2, 15, 18, 29) | `src/main/java/io/github/yhyeop/sanupcraft/boss/FurnaceWarden.java`(주석만), `ROADMAP.md`, `docs/PLAYTEST.md`, `tools/make_structures.py`, `src/main/resources/assets/sanupcraft/lang/ko_kr.json`, `src/main/resources/assets/sanupcraft/lang/en_us.json` | 시험 변경 없음. build로 확인, `.nbt`는 다시 만들지 않음 |

**G1 단계**
- 19: `private void track(ServerLevel level)` = 지금 `tick` 안의 `ACTIVE.computeIfAbsent(...).add(this)`를 옮긴 것, `tick`은 이걸 부른다. `@Override clearRemoved()`: `super.clearRemoved()` 뒤 `level instanceof ServerLevel s && fuelTicks > 0`이면 `track(s)`.
- 20: `@Override setRemoved()`: 그 차원 `ACTIVE` 집합에서 `this`만 빼고 `super.setRemoved()`. **청크는 풀지 않는다**(서버 종료 때도 불림). 시험용 `public boolean isTracked()` = 그 차원 집합에 `this`가 있는가.
- 시험 ① `anchorHandsOverAfterReload`: 같은 청크에 닻 C · A, 둘 다 연료. `c.tick` → `helper.assertTrue(c.ownChunkCount() > 0, …)`(전제, 0이면 실패) → `int owned = c.ownChunkCount()` → `a.tick` → `EnderAnchorBlockEntity.clearActive()` → `a.clearRemoved()` → C 자리를 공기로 → `a.ownChunkCount() == owned` 단언 → 두 닻 정리 → `helper.succeed()`. 모두 같은 틱 안에서.
- 시험 ② `anchorUntracksOnRemoved`: 연료 넣은 닻 `tick` → `isTracked()` 참 → `setRemoved()` → 거짓 → `clearRemoved()`로 되돌린 뒤 블록 정리 → `succeed()`.

**G2 단계**
- 8: `work` 처음에 `int before = steam;`, 끝의 `if (made > 0) setChanged();`를 `if (steam != before) setChanged();`로.
- 13: `fromModJar` 파일 반복 안에서 `try (Reader …) { … } catch (IOException | JsonParseException e) { LOGGER.warn("JEI: 레시피 {}를 읽지 못함", file, e); }` — 그 파일만 건너뜀. 바깥 `catch (IOException)`은 `Files.walk` 실패용으로 그대로.

**G3 단계**
- 1: `FurnaceWarden.maxRings()` 주석 → "화염 장판 상한 (인원수, 3 ~ 6). 장판은 20칸 안 플레이어 발밑마다 하나". ROADMAP "보스 인원 배율" 항목의 `화염 장판 = 인원수(3~6개)` → `화염 장판 = 20칸 안 플레이어마다 하나, 상한 인원수(3~6)`.
- 2: "보스 바에 보이는 제목 (시험용)" 주석을 `party()` 위에서 `bossBarName()` 위로.
- 15: `make_structures.py`의 `class Int(int)` · `class Str(str)` 삭제.
- 18: PLAYTEST "받는 것" 표를 `PlaytestCommand` 꾸러미와 대조: `outpost`에 "상자 · 깔때기", `start`에 "레드스톤 · 판자", `end`에 "조약돌".
- 29: `block.sanupcraft.steam_quarry.drill_installed` 괄호 → ko "(맨 위층부터 다시 훑는다)", en "(rescans from the top layer)".

### 6-7. 수정 기록 (Sonnet 3기, 파일 겹치지 않게)
| 묶음 | 항목 | 바뀐 것 |
|---|---|---|
| G1 엔더 닻 등록 | 19, 20 | `clearRemoved()`에서 연료가 남은 닻을 ACTIVE에 등록 (재시작 직후 아직 tick 안 한 닻도 넘겨받기 대상), `setRemoved()`에서 ACTIVE에서만 뺌 (청크는 안 풂 — 서버 종료 때도 불림). 시험용 `isTracked()`. 시험 `anchorHandsOverAfterReload`(혼자 도는 환경 — 같은 묶음 다른 시험 구역이 강제 로드돼 있으면 C가 소유할 청크가 없어 전제에서 걸렸다) · `anchorUntracksOnRemoved` |
| G2 지열 · JEI | 8, 13 | 분출구는 증기 값이 바뀐 틱마다 `setChanged`. JEI 모드 jar 레시피 읽기: 파일마다 `IOException \| JsonParseException`을 잡아 그 파일만 건너뜀, 폴더 순회의 `UncheckedIOException`도 잡음 |
| G3 주석 · 문서 · 문구 · 도구 | 1, 2, 15, 18, 29 | 화염 장판 설명을 "20칸 안 플레이어마다 하나, 상한 인원수(3~6)"로 (FurnaceWarden · ROADMAP), 보스 바 제목 주석 제자리로, `make_structures.py`의 안 쓰는 클래스 삭제 (`.nbt` 바이트 그대로 확인), PLAYTEST 받는 것 표 (`start` · `outpost` · `end`), 드릴 끼울 때 안내 "맨 위층부터 다시 훑는다" (ko · en) |

검증:
- `./gradlew build`: GameTest 110개 통과 (108 + 닻 2).
- 새 닻 시험이 버그를 잡는지: `clearRemoved` 등록을 잠시 끄면 두 시험이 실패 → 되돌림.
- `runClientGameTest` (편의 모드 11개 포함): JEI 기계 레시피 25개, 우리 모드 오류 없음. 일반 지형 월드에서 녹슨 사당 생성 · 제단 확인.

### 6-8. 보고
| | 건수 |
|---|---|
| 지적 (Haiku 4기) | 28 (+ 반론에서 1 = 29) |
| 실제 문제로 판정 (최종) | 9 |
| 보류 | 3 (3 보스 공통 부모 — 보스가 셋째로 늘 때 · 17 사당 사슬 모양 — 주인 판단 · 21 외부 강제 로드 청크 정책) |
| 오탐 | 17 |
| 수정 | 9 |
| 2단계 · 3단계로 올린 것 | 0 |
| 시험 | GameTest 110개 통과, 클라이언트 시험 통과 |

Opus 샘플링: 판정 단계에서 Opus가 지적 밖 코드(굴착기 flush 경로 · 블록 엔티티 언로드 순서)를 직접 읽어 확인했고, 비판적 Sonnet이 드릴 안내 문구(29)를 추가로 찾았다.

## 7. 3차 리팩터링 1단계 · 확장 전체 (2026-11-06)

대상: 2차 리팩터링(`4b071fb`) 이후 바뀐 본 코드 전부 (확장 묶음 1~4, M5~M11, C3 · 약 2만 5천 줄, 패키지 21개).
절차: Haiku 9기 병렬(패키지 겹치지 않게) → Opus 1기 판정 → 비판적 Sonnet 반론 → Opus 최종 결정 → Sonnet n기 수정.
기준 시험: `./gradlew build` (GameTest 292개), 화면 쪽은 `runClientGameTest`.

### 7-1. 지적 (Haiku 9기, 79건)

#### H1 보스 · 소리 (`boss/` · `sound/`)
1. BossParty.java:42, :230 · 주석 불일치 · 중 — 기여자가 없으면 "하나만 떨군다"는 주석과 달리 `1 + tier`개를 떨군다 (강화 소환석이면 2~4개).
2. FurnaceWarden · RustyGuardian · 중복 · 중 — 보스 바 · party · 저장 · actuallyHurt · 상한 보정 · 방패 깨기 · 경고 · 거리 · 낙하/불 처리 · PatternGoal 등이 두 보스에 복사돼 있다 (공통 부모 필요). ※ 지난 2차 리팩터링 보류 3번과 같은 항목.
3. FurnaceWarden.java:292-321, :346-361, :432-441 · RustyGuardian.java:298-315 · 버그 · 하 — 패턴 판정의 플레이어 목록에 관전 · 크리에이티브 제외가 없어 밀치기와 집계에 들어간다 (BossParty.countPlayers · VoidColossus.targets는 뺀다).
4. BossParty.java:186-192 · 기타 · 하 — `seen` 맵이 나간 플레이어 객체를 보스가 살아 있는 동안 붙잡는다 (저장 안 됨, 한정적).
5. GuardianAltarBlock.java:88 · 기타 · 하 — `!(player instanceof ServerPlayer p) || p.connection != null` 이중 부정 조건.
6. GuardianAltarBlock.java:40 · BossParty.java:53 · 중복 · 하 — 같은 48.0 반경 상수가 두 곳.
7. RustyGuardian.java:440, :501 · 기타 · 하 — 근접 이벤트 번호 리터럴 4 (다른 보스는 EVENT_ATTACK 상수).
8. FurnaceWarden.java:155, :186 · 주석 불일치 · 하 — "20칸 안"이지만 `inflate(20)` 상자라 대각선 약 28칸.
9. GuardianAltarBlock.java:55-72, :141-146 · 중복 · 하 — 우클릭 경로와 summon()이 같은 소환 조건을 두 번 검사.

#### H2 창고 (`storage/`)
10. storage/StorageMachineBlock.java:79 · AutocrafterBlockEntity.java:42 · 버그 · 상 — 자동 제작대를 부수면 재료 · 결과 칸이 사라진다 (dropContents 없음).
11. storage/AutocrafterBlockEntity.java:203 · 버그 · 중 — acceptSteam에 `setChanged()` 없음.
12. storage/OwnerLock.java:47 · 주석 불일치 · 하 — 팀원도 허용하는데 주석은 "주인 · OP만".
13. storage/AutocrafterBlockEntity.java:38 · 주석 불일치 · 하 — "9칸이 차면"이지만 견본이 쓴 칸만 본다.
14. storage/StockerBlockEntity.java:225 · 버그 · 하 — 재료 1만 넣고 재료 2가 실패해도 되돌리지 않는다 (다음 주기에 회복).
15. storage/TerminalBlockEntity.java:75 · 기타 · 하 — 화면이 열린 동안 매 틱 setChanged.
16. storage/PortableTerminalItem.java:84 · 버그 · 하 — `toLowerCase()`에 Locale 없음.
17. storage/PortableTerminalItem.java:52 · 죽은 코드 · 하 — isEnder() 미사용.
18. storage/StorageNetwork.java:64 · 죽은 코드 · 하 — nodes() 미사용.
19. storage/DrawerBlockEntity.java:149 · 죽은 코드 · 하 — insertFromHand의 player 매개변수 미사용.

#### H3 증기 · 유체 (`power/` · `fluid/`)
20. power/SteamTurbineBlockEntity.java:73 · 버그 · 중 — acceptHighPressureSteam에 `setChanged()` 없음.
21. power/SteamTurbineBlockEntity.java:59 · 버그 · 중 — 변경 감지가 `hp + steam != before`라 배율 1이면 변환해도 표시가 빠진다 (설정 최소값 1).
22. HighPressurePipeBlock.java:30-51 · SteamPipeBlock.java:28-44 · 중복 · 하 — 두 파이프의 상태 · 연결 코드 복사.
23. power/BoilerBlockEntity.java:58, 94, 314, 324 · 죽은 코드 · 하 — litDuration 필드는 쓰기만 하고 읽지 않음.
24. power/SteamChargerBlockEntity.java:38, 63, 67-69 · 죽은 코드 · 하 — charging · isCharging() 미사용.
25. fluid/PumpBlockEntity.java:85-89 · 버그 · 하 — 원천을 먼저 지우고 kind가 null이면 내용물을 버림 (도달 가능성 낮음).
26. power/SteamStorage.java:3 · 주석 불일치 · 하 — 구현체 목록이 낡음.

#### H4 엔드 · 네더 (`end/` · `nether/`)
27. end/VaultData.java:67-71 · end/DimensionalVaultBlockEntity.java:113-116, 166-170 · 버그 · 중 — 저장 표시가 Channel.setChanged에만 있고 `removeItemNoUpdate`는 setChanged를 안 불러, 그 경로로 비우면 재시작 뒤 아이템이 되살아날 수 있다.
28. end/EndIslandStructure.java:23-24 · 주석 불일치 · 하 — 귀퉁이끼리는 비교하지 않는데 주석은 서로 비교한다고 함.
29. end/VoidCondenserBlockEntity.java:60-63, 73 · 버그 · 하 — 저장고가 차면 불은 꺼지는데 상태 문구는 RUNNING.
30. end/VoidGear.java:72-73, 118-121 · 기타 · 하 — UUID 맵(LAST_SAFE · LAST_RESCUE)을 로그아웃 때 안 지운다.
31. EndIslandStructure.java:47-71 · NetherFloorStructure.java:54-81 · 중복 · 하 — 귀퉁이 판정 루프와 배치 호출이 거의 같은 코드로 둘.
32. BarterCollectorBlock · GoldDispenserBlock · BreathCollectorBlock · 중복 · 하 — 같은 affectNeighborsAfterRemoval 셋.
33. end/ChorusHarvesterBlockEntity.java:79-112 · 기타 · 하 — 256블록 상한을 넘는 후렴 나무는 일부만 보고 거둔다.
34. nether/LavaSteamboat.java:44-69 · 죽은 코드 · 하 — floatOnLava()의 boolean 반환값을 버림.

#### H5 기계 · 산업 (`factory/` · `industry/`)
35. factory/MachineBlock.java:90 · 버그 · 상 — 기계를 부수면 칸 내용물(재료 · 연료 · 결과 · 부산물 · 업그레이드)이 사라진다. `preRemoveSideEffects`/`dropContents`가 없다 (다른 블록 엔티티는 떨군다).
36. industry/FurnaceTowerControllerBlock.java:111 · 버그 · 상 — 용광로 탑 칸 18개도 부서질 때 유실.
37. industry/FurnaceTowerControllerBlockEntity.java:111, 130-149 · 버그 · 중 — 묶음을 잡은 뒤 재료를 꺼내도 계속 증기를 쓰고 결과 없이 끝난다 (증기 낭비, batchUnits 낡은 값).
38. factory/MachineBlockEntity.java:459-464 · 버그 · 하 — 파이프로 받은 증기에 `setChanged()` 없음.
39. factory/MachineBlockEntity.java:320 · 주석 불일치 · 하 — "바닐라 계산과 같되"지만 하나만 든 기계가 0이 나올 수 있다.
40. industry/BigBoilerControllerBlock.java:74-88 · 중복 · 하 — 보일러 힌트 로직 복사.
41. industry/BigBoilerShape.java · 중복 · 하 — MultiblockShape와 거의 같은 코드.
42. 외벽 · 벽돌 · 두 제어기 · 중복 · 하 — notifyControllers · setCasingsFormed 복사.
43. industry/FurnaceTowerControllerBlockEntity.java:314-327 · 버그 · 하 — 풀릴 때 이웃 제어기가 쓰는 벽돌까지 끄고 link(null)를 조건 없이 부른다.
44. factory/MachineBlockEntity.java:199 · 기타 · 하 — 매 틱 업그레이드 효과 계산(Math.pow), 세척기 물 6방향 조회.
45. industry/FurnaceTowerControllerBlockEntity.java:43 · 주석 불일치 · 하 — 상한은 설정값인데 주석은 BATCH_SIZE.

#### H6 물류 · 수송 (`logistics/` · `transport/`)
46. transport/TrainCoupling.java:591 · 버그 · 하 — 속도를 x · z 따로 잘라 대각선이면 최대 √2배.
47. logistics/ItemFilter.java:508 · 죽은 코드 · 하 — contains() 미사용 (FilterMenu.sampleExists가 같은 일).
48. logistics/EnderLinkBlockEntity.java:477 · 주석 불일치 · 하 — 팀원 · 주인 없음도 허용하는데 주석은 "주인 · OP만".
49. logistics/EnderAnchorBlockEntity.java:304 · 중복 · 하 — 바닐라 `preRemoveSideEffects`가 Container 내용물을 이미 떨군다(javap 확인) → super 뒤 dropContents는 빈 호출. ※ 이 사실은 H2-1 · H5-1 · H5-2 판정에도 걸린다.
50. transport/SteamLocomotive.java:332 · 버그 · 하 — missingTicks가 칸별이 아니라 열차 전체라 뒤 칸이 40틱 전에 떨어질 수 있다.
51. logistics/EnderAnchorBlockEntity.java:278 · 버그 · 하 — 켜진 동안 반경 설정을 바꾸면 coveringAnchor 판정이 붙잡은 범위와 달라진다.
52. transport/SteamLocomotive.java:204 · 주석 불일치 · 하 — "빈손이면 출발/멈춤"이지만 아무 아이템이나 토글.
53. logistics/BeltMotorBlockEntity.java:311 · 주석 불일치 · 하 — 증기가 모자라면 belts = 0인데 주석은 센다고 함.
54. transport/CargoStationBlockEntity.java:27 · 주석 불일치 · 하 — 한도는 설정값인데 주석은 상수.
55. logistics/BeltBlockEntity.java:57 · 기타 · 하 — 벨트 칸마다 매 틱 엔티티 조회.

#### H7 장비 · 렌치 · 채굴 · 설정 · 실테스트
56. mining/QuarryBlockEntity.java:189-194, 157-171 · 버그 · 중 — acceptSteam · flush에 `setChanged()` 없음. ※ flush는 지난 2차 리팩터링에서 오탐(같은 청크 상자가 표시)으로 판정됨.
57. gear/Jetpack.java:86-88 · 주석 불일치 · 하 — "창작 모드는 증기를 안 쓴다"지만 증기 1 이상이 없으면 창작도 못 뜬다.
58. gear/ToolMining.java:34, 41-48 · 기타 · 하 — FACES 정적 맵을 LEAVE 때 안 지운다.
59. gear/SteamBoots.java:117 · 주석 불일치 · 하 — "막아 준 몫"이지만 실제 맞은 피해로 깎는다.
60. gear/SteamDrillItem.java:67-68 · 중복 · 하 — Wrench.tell과 같은 조건 (앞 절은 늘 거짓).
61. playtest/PlaytestCommand.java:197-198 · 중복 · 하 — 석탄 64 두 줄. ※ 지난 2차 리팩터링 14번에서 오탐 판정.
62. ProductionGear · SteelGear · HeatproofGear · VoidGear · 중복 · 하 — 같은 갑옷 방어 EnumMap 도우미 넷.
63. gear/ChainsawItem.java:9 · 주석 불일치 · 하 — "잎은 1/4"이지만 정수 나눗셈 max(1, cost/4).
64. playtest/PlaytestCommand.java:568-572 · 기타 · 하 — kitEnd2가 apply()의 엔드 발판을 버리고 다시 텔레포트.

#### H8 생산 · 서버 (`production/` · `server/`)
65. server/GoalBoardBlockEntity.java:228 · 버그 · 상 — 깔때기 · 벨트가 남은 목표보다 많이 넘기면 초과분이 사라진다 (setItem → insert가 남은 만큼만 받고 나머지를 버림). 생산 기계의 pushOut도 같은 경로.
66. production/TreeFellerBlockEntity.java:47 · 버그 · 중 — 묘목 다시 심을 자리(replantAt)가 저장되지 않아, 잎만 남은 상태로 다시 불러오면 묘목을 안 심는다.
67. production/TreeFellerBlockEntity.java:65-101 · 기타 · 중 — 잎 없는 통나무 구조물 옆이면 10틱마다 최대 18번 256블록 BFS가 헛돈다.
68. server/GoalCommand.java:38-40 · 죽은 코드 · 하 — register() 미사용.
69. TreeFelling.size · TreeFeller.felled()/queued() · CropHarvester.cursor() · 죽은 코드 · 하 — 미사용 (felled 필드는 저장만).
70. server/GoalBoardBlock.java:53-59 · 중복 · 하 — topContributors(3) 두 번, 상수 없음.
71. production/grinder/MobGrinderBlockEntity.java:153-178 · 중복 · 하 — 둘레 경험치 탱크 찾기 루프 둘.
72. production/TreeFellerBlockEntity.java:139-165 · 기타 · 하 — preferredSapling을 칸마다 다시 계산.

#### H9 클라이언트 (`client/`)
73. JetpackClient.java:34-43 (서버 gear/Jetpack.java:80-95) · 버그 · 중 — 클라이언트는 땅 · 물 · 용암 조건과 로컬 설정으로 먼저 띄우지만, 서버 `thrust()`는 그 조건을 안 보고 설정도 동기화되지 않는다. 변조 클라이언트는 땅 · 물에서 뜰 수 있고, 설정이 다르면 위치가 튄다.
74. storage/TerminalScreen.java:233 · 버그 · 하 — 드래그 비율 분모 `rows*18-15`가 그리기(`rows*18-17`)와 달라 막대가 약 2px 어긋난다.
75. storage/StockerScreen.java:42 · 버그 · 하 — `toLowerCase()`에 Locale 없음 (터키어에서 번역 키를 못 찾음).
76. SanupcraftClient.java:43 · 주석 불일치 · 하 — 거상은 이미 전용 모델인데 "임시 렌더러"라고 적힘.
77. 보스 렌더러 3곳 · 모델 3곳 · 중복 · 하 — 사망 페이드 · 내려치기 자세 계산이 거의 같은 식으로 셋. JetpackClient · Jetpack의 `Math.max(cost,1)`도.
78. storage/StockerScreen.java:48 · 중복 · 하 — `hoveredSlot.index == 0 && indexOf(...) == 0` 중복 조건.
79. storage/StorageClient.java:53-57 · 죽은 코드 · 하 — 삼항 한 줄을 감싼 중첩 클래스.

### 7-2. Opus 판정
집계(79건 + Opus 샘플링 2건 = 81): **수정 대상 31** · 오탐 42 · 보류 3 · 2단계로 5 · 3단계로 0. 수정 대상 중 1 · 8 · 12 · 13 · 26 · 28 · 45 · 48 · 52 · 53 · 54 · 57 · 76은 주석만(동작 변화 없음).

검증 메모 (바닐라 26.3 소스 직접 확인):
- **F1. 부술 때 내용물 (10 · 35 · 36 · 49)**: `BlockEntity.preRemoveSideEffects`(BlockEntity.java:235)가 `this instanceof Container`이면 `Containers.dropContents`를 부른다. 부르는 곳은 `LevelChunk.setBlockState`(310행: 블록이 바뀌고 BE가 빠질 때 — 부수기 · 폭발 · `/setblock` 공통). 자동 제작대 · 기계 · 용광로 탑 BE는 모두 `BaseContainerBlockEntity`이고 `preRemoveSideEffects`를 오버라이드하지 않으며, 블록 클래스(`StorageMachineBlock` · `MachineBlock` · `FurnaceTowerControllerBlock`)는 `affectNeighborsAfterRemoval`만 바꾼다 → **내용물은 이미 떨어진다**. 실제로 부수는 시험은 없지만(기존 시험은 Container가 아닌 굴착기 · 드릴뿐) 경로가 바닐라 그대로라 오탐. `Containers.dropItemStack`이 스택을 `split`으로 비우므로 `super` 뒤 `dropContents`(49)는 빈 호출 — 엔더 닻 외에 `SteamChargerBlockEntity` · `SteamWorkerBlockEntity`도 같다.
  - 같은 사실의 반대쪽(Opus 샘플링 **#81**): `DrawerBlockEntity`도 `Container`(출력 칸 = 최대 64개 보기)인데 오버라이드가 없다. 플레이어가 부수면 `removeBlock`(→ 보기 64개를 쏟음) 다음 `playerDestroy`(→ 나머지만 아이템에 담김)라 "부숴도 내용물을 품은 채"가 깨지고, **폭발은 순서가 반대**(`BlockBehaviour.onExplosionHit`: `getDrops` 먼저 → `setBlockAndUpdate(AIR)`)라 **최대 64개가 복제**된다. 바닐라 셜커 상자처럼 빈 `preRemoveSideEffects`가 맞다(`DimensionalVaultBlockEntity`도 이미 그렇게 함). 기존 `drawerKeepsContents`는 `Block.getDrops`만 불러 못 잡았다.
- **F2. `setChanged` 누락과 저장 (11 · 14 · 20 · 21 · 27 · 38 · 56)**: `ChunkMap.save`는 `tryMarkSaved()`가 거짓(표시 안 됨)이면 청크를 쓰지 않는다 — 자동저장(`saveChunkIfNeeded`) · 서버 종료(`saveAllChunks(true)`) · 언로드 모두. 쓸 때는 `SerializableChunkData.copyOf`가 그 청크의 **모든 BE**를 저장한다. `setChanged` → `Level.blockEntityChanged` → 청크 `markUnsaved`. 즉 누락은 "그 뒤 저장 전까지 같은 청크를 아무도 표시하지 않을 때" 마지막 변경만 잃는다. 판정 규칙(지난 #7 · #8과 같은 기준):
  - **줄어드는 쪽**(소모 · 내보냄 · 꺼냄)에서 빠지면 재시작 때 되살아나 복제 → 수정. 이번 지적 중 해당 없음(27 `removeItemNoUpdate`는 닿는 경로가 없음: 창고 화면은 `ChestMenu` → `removeItem`, 깔때기도 `removeItem`).
  - **받는 쪽 증기**(11 · 20 · 38 · 56의 `acceptSteam`)는 흐름 자원이고, 보낸 쪽(보일러 · 터빈 · 분출구 · 응축기)이 자기 청크를 표시하므로 잃는 건 마지막 몇 틱 분 증기뿐, 다음 소모 때 표시된다 → 오탐. 받는 쪽에 매 틱 `setChanged`를 넣으면 비교기 이웃 갱신(`updateNeighbourForOutputSignal`)이 매 틱 돈다.
  - **받는 쪽 아이템**은 실제 유실이라 수정(14).

| 번호 | 판정 | 근거 (검증) | 수정 방향 |
|---|---|---|---|
| 1 | 수정(주석) | 확인: 기여자가 없으면 `1 + tier`개 (`giveKeyItem`) | `BossParty` 클래스 문서 "하나만" · `giveKeyItem` 문서 "필수품만" → "필수품 `1 + 단계`개만" |
| 2 | **2단계로** | 지난 보류 3의 조건("보스가 셋째로 늘 때") 충족: 공허 거상까지 셋. `PartyBoss` 인터페이스로는 protected 오버라이드를 못 묶음 | `Monster`와 세 보스 사이 공통 부모 설계(아래). 77과 같이 |
| 3 | 수정 | 확인: 파수꾼 분출 · 장판 폭발 · 도약, 수호자 돌진(`inflate(0.6)`) · 내려치기의 플레이어 목록에 관전 · 창작 제외가 없음 → 창작 방패를 깨고 밀치고 `hits`에 셈. 장판 위치 고르기 · 거상 · `countPlayers`는 이미 뺌 | 다섯 곳 `getEntitiesOfClass(Player.class, box, EntitySelector.NO_CREATIVE_OR_SPECTATOR)`. 시험 없음 |
| 4 | 오탐 | 지난 #4와 같음(`seen` = 모의 플레이어용, 보상 시험이 기댐) | |
| 5 | 오탐 | 모의 플레이어(`connection == null`)에 문구를 안 보내는 의도된 가드 | |
| 6 | 오탐 | 값만 같고 뜻이 다름(보스 하나만 반경 · 인원 반경) | |
| 7 | 수정(가독) | 확인: 수호자만 리터럴 `(byte) 4` | `RustyGuardian`에 `EVENT_ATTACK = 4` 상수, 두 곳 교체 |
| 8 | 수정(주석) | 확인: `inflate(20)`은 상자(가로 대각 약 28칸, 위아래 ±20) | `maxRings()` 주석 "20칸 안" → "가로 · 세로 ±20칸 상자 안" |
| 9 | 오탐 | `summon()`은 명령 · 시험도 부르는 공개 경로라 자체 검사가 필요, 우클릭은 문구를 위해 먼저 검사 | |
| 10 | 오탐 | F1: 바닐라가 떨군다 | |
| 11 | 오탐 | F2: 받는 쪽 증기 | |
| 12 | 수정(주석) | 확인: `canManage`가 같은 팀(M10)도 허용 | `OwnerLock` 클래스 · `canManage` 문서에 "주인과 같은 팀" 추가 |
| 13 | 수정(주석) | 확인: `tryCraft`는 견본이 쓴 칸만 본다 | "재료 9칸이 차면" → "견본이 쓰는 재료 칸이 모두 차면" |
| 14 | 수정 | 지적의 "되돌리지 않음"은 재료가 칸에 남아 회복되므로 무해. 대신 확인: 재료 1을 넣고 재료 2가 실패하면 `machine.setChanged()` 없이 다음 레시피로 넘어감 → 네트워크 쪽은 표시, 기계 칸은 미표시(이웃 청크면 재시작 때 아이템 유실, F2) | `feedMachine`: 첫 `fill`이 칸을 바꿨으면(둘째 성공 여부와 상관없이) `machine.setChanged()`. 시험 없음 |
| 15 | 오탐 | 소모 쪽이라 표시가 필요(F2), 화면을 연 단말 수만큼 | |
| 16 | 수정 | 확인: `Check`의 `NOT_LINKED` · `OTHER_DIMENSION` · `MISSING`이 터키어 로캘에서 `ı`로 바뀌어 번역 키를 못 찾음 | `toLowerCase(Locale.ROOT)` |
| 17 | 오탐 | 단순 공개 조회 함수(시험 · 디버그용으로 무해) | |
| 18 | 오탐 | 위와 같음 | |
| 19 | 오탐 | `DrawerBlock` · 시험이 쓰는 시그니처, 노이즈 | |
| 20 | 오탐 | F2: 받는 쪽 증기 | |
| 21 | 오탐 | 배율 1이면 고압 → 보통으로 옮겨도 총량이 같아 표시가 빠지지만, 다시 불러오면 그만큼 다시 바꿀 뿐 손실 · 복제 없음 | |
| 22 | 오탐 | 연결 규칙(`connectsTo`)이 달라 공통 부모 이득 작음 | |
| 23 | 수정(죽은 코드) | 확인: 보일러 `litDuration`은 쓰기 · 저장만, 읽는 곳 없음(화면 · 시험 없음. 시험의 `litDuration()`은 기계 쪽) | 필드 · `lit_duration` 읽기/쓰기 삭제(옛 저장의 키는 무시됨) |
| 24 | 수정(죽은 코드) | 확인: `charging` · `isCharging()` 쓰는 곳 없음 | 둘 삭제 |
| 25 | 오탐 | `findSource`가 물 · 용암 **원천**이고 `BucketPickup`인 칸만 고름 → 바닐라 범위에선 늘 그 양동이. 물 태그를 단 다른 모드 유체만 이론상 | |
| 26 | 수정(주석) | 확인: 구현체 15곳인데 목록은 5곳 | 목록을 빼고 "증기를 모아 두는 블록 엔티티. 증기 계기판이 읽는다" |
| 27 | 오탐 | F2: `removeItemNoUpdate`에 닿는 경로 없음 | |
| 28 | 수정(주석) | 확인: 귀퉁이는 가운데와만 비교 | "서로 ±MAX_STEP 안" → "가운데와 ±MAX_STEP 안" |
| 29 | 오탐 | 상태 문구에 `고압 증기 저장/최대`가 같이 나와 가득 참이 보임, 불은 만든 양 기준 | |
| 30 | 수정 | 지적의 메모리는 플레이어 수만큼이라 무해. 대신 확인: 정적 맵이 서버(월드)를 넘어 남는다 → 한 JVM에서 월드를 바꾸면(싱글) 다른 월드의 `LAST_RESCUE` 게임 시각과 비교해 `now - last`가 음수 → **쿨다운으로 구조 거부**, `LAST_SAFE`도 다른 월드 자리 | `init`에서 `ServerLifecycleEvents.SERVER_STARTING`에 두 맵 `clear()` (`ModLogistics`의 닻 정리와 같은 방식). 로그아웃 때 지우면 쿨다운 우회가 되므로 그때는 지우지 않는다. 시험 없음 |
| 31 | 오탐 | 높이 함수(`surface` · `floorBelow`)가 달라 8줄 반복을 묶을 이득 없음 | |
| 32 | 오탐 | 한 줄짜리 바닐라 패턴 | |
| 33 | 오탐 | 후렴 나무는 자연 상태에서 상한보다 훨씬 작음, 상한은 안전장치 | |
| 34 | 오탐 | 반환값 무시는 정상 | |
| 35 | 오탐 | F1 | |
| 36 | 오탐 | F1 | |
| 37 | 수정 | 확인: 묶음을 잡은 뒤 재료를 빼도 `work`는 잡을 때의 `units`로 증기를 쓰고, 0개면 `finish`가 아무것도 안 낸다 | `work` 처음에 `available(current)`: 0이면 `batch = null; progress = 0; searchTimer = 0;` 후 false, `units`보다 적으면 묶음 `units`를 그만큼 줄인다(진행 유지). **시험 추가**(M8b): 묶음 시작 → 재료 비움 → tick → `batchUnits() == 0` · 증기 그대로 |
| 38 | 오탐 | F2: 받는 쪽 증기 | |
| 39 | 오탐 | 바닐라 `getRedstoneSignalFromContainer`와 같은 `Mth.lerpDiscrete`(값이 0보다 크면 +1) → 하나만 들어도 1 | |
| 40 | **2단계로** | 큰 보일러 · 용광로 탑 두 멀티블록의 힌트 · 모양 · 외벽 표시가 각자 복사 → 공통화는 두 모듈 설계 판단 | 41 · 42 · 43과 한 묶음(아래) |
| 41 | **2단계로** | `BigBoilerShape` ↔ `MultiblockShape` 통합 | 위와 같음 |
| 42 | **2단계로** | 외벽 · 벽돌 블록 · 두 제어기의 `notifyControllers` · `setCasingsFormed` 복사 | 위와 같음 |
| 43 | **2단계로** | 맞음: 이웃 탑과 공유한 벽돌도 끄고 `link(null)`. 다음 확인(`CHECK_INTERVAL`)에서 이웃이 다시 켜 일시적. "외벽 공유 허용 여부"는 큰 보일러에도 같은 문제라 40~42 공통화 때 규칙으로 정한다 | |
| 44 | 오탐 | 틱당 `Math.pow` 몇 번 · 6방향 조회는 기계 수 대비 무시할 비용 | |
| 45 | 수정(주석) | 확인: 상한은 `furnaceTowerBatchSize` | "최대 BATCH_SIZE개" → "최대 설정값(기본 BATCH_SIZE)개" |
| 46 | 오탐 | `velocity`는 투영(`along`)에만 쓰고 최종 속도는 `follow`가 `CAR_MAX_SPEED`로 다시 자름, 레일은 축 방향 | |
| 47 | 수정(죽은 코드) | 확인: `ItemFilter.contains`는 호출 없음(`FilterMenu.sampleExists`가 크기 제한까지 반영해 대신함) | 삭제 |
| 48 | 수정(주석) | 확인: 지적 줄(477)은 없고 클래스 문서의 "주인 · OP만"이 `canManage`(팀 포함)와 다름 | "주인 · 같은 팀 · OP만" |
| 49 | 수정(정리) | F1: `super` 뒤 `dropContents`는 빈 호출. 엔더 닻 · 증기 충전기 · 증기 일꾼 셋 | 세 곳에서 `Containers.dropContents(...)` 줄만 삭제(닻의 청크 풀기 · 일꾼의 `pending` 떨구기는 유지), 주석 "내용물은 `super`(바닐라)가 떨군다" |
| 50 | 수정 | 확인: `missingTicks`가 열차 전체에 하나라, 칸 i가 39틱 비었다가 나타난 같은 틱에 칸 j가 비면 바로 떼어 냄 | `missingIndex` 필드: 빈 칸 번호가 바뀌면 `missingTicks = 0`부터. 끝까지 돌면 · 떼면 `-1`. 시험 없음 |
| 51 | 보류 | 맞음: 켜진 동안 반경 설정을 바꾸면(재시작 · `config reload`) `coveringAnchor`가 새 반경으로 판정. 고치려면 닻마다 켠 때 반경을 저장(저장 형식 변경) | 정책: 닻이 반경을 기록할지, "설정 변경 뒤에는 닻을 다시 놓는다"로 문서화할지 |
| 52 | 수정(주석) | 확인: 연료 · 증기 통이 아니면 무엇을 들든 출발/멈춤 | "빈손이면" → "그 밖(빈손 · 다른 아이템)이면" |
| 53 | 수정(주석) | 확인: 증기가 모자라면 `belts = 0` | `poweredBelts` 주석에 "증기가 모자라면 0" |
| 54 | 수정(주석) | 확인: 한도는 `stationItemsPerTransfer` | "최대 ITEMS_PER_TRANSFER개" → "최대 설정값(기본 ITEMS_PER_TRANSFER)개" |
| 55 | 오탐 | 켜진 벨트만, 바닐라 깔때기와 같은 수준의 비용 | |
| 56 | 오탐 | `flush`는 지난 #7, `acceptSteam`은 F2 | |
| 57 | 수정(주석) | 확인: 창작은 덜지 않을 뿐 1 이상 있어야 뜸 | "증기를 안 쓴다" → "증기를 덜지 않는다 (그래도 1 이상 들어 있어야 뜬다)" |
| 58 | 오탐 | `BEFORE`에 넣고 `AFTER`에서 꺼냄, 남아도 다음 캐기에 덮어씀(플레이어 수만큼) | |
| 59 | 오탐 | 보정이 −50%라 막아 준 몫 = 맞은 몫 | |
| 60 | 오탐 | 5와 같은 모의 플레이어 가드 | |
| 61 | 오탐 | 지난 #14와 같음 | |
| 62 | 오탐 | 6줄 도우미, 파일마다 독립 | |
| 63 | 오탐 | 근사 표기(실제 값은 설정), 노이즈 | |
| 64 | 오탐 | 실테스트 명령: 발판이 하나 더 깔릴 뿐 (지난 #16과 같은 기준) | |
| 65 | 수정 | 확인(**상**): 26.3 `HopperBlockEntity.tryMoveInItem`은 빈 칸이면 `setItem(slot, 스택 전체)` 후 `EMPTY`를 돌려줌. 벨트 끝(`BeltBlockEntity.insert`) · 증기 일꾼 `pushOut`은 스택째 넘기므로 남은 목표보다 많으면 초과분이 사라짐(깔때기는 1개씩이라 무사) | `GoalBoardBlockEntity.setItem`: `insert` 뒤 남은 몫이 있고 다음 목표가 같은 아이템이면 한 번 더 `insert`, 그래도 남으면 판 위(`y + 1`)에 `Containers.dropItemStack`. **시험 추가**(M10): 남은 목표 10에 `HopperBlockEntity.addItem(null, board, 64개, Direction.UP)` → 진행 = 목표, 판 둘레 아이템 엔티티 합 = 54(다음 목표가 같은 아이템이면 그만큼 뺀 값) |
| 66 | 수정 | 확인: `replantAt` · `replantLog`가 저장 안 됨 → 원목을 다 벤 뒤(잎만 남음) 다시 불러오면 `startFelling`이 원목을 못 찾아 묘목을 안 심음 | `saveAdditional`/`loadAdditional`에 `replant_at`(`BlockPos.CODEC`) · `replant_log`(블록 id, 없으면 null). 시험 선택(M9a: 저장 → `loadStatic` 뒤 값 비교) |
| 67 | 오탐 | 10틱마다 시작점 12칸 × 최대 256칸, 벌목기 수가 적음 | |
| 68 | 수정(죽은 코드) | 확인: `GoalCommand.register`는 호출 없음(`ModServer`가 `node()`를 씀). 부르면 `sanupcraft` 뿌리를 따로 만들 함수 | 삭제 |
| 69 | 오탐 | 단순 조회 함수 · 저장되는 누적 수, 무해 | |
| 70 | 오탐 | 노이즈 | |
| 71 | 오탐 | 두 루프의 뜻(통째 넣기 · 나눠 비우기)이 다름 | |
| 72 | 오탐 | 나무 하나당 9칸 조회 | |
| 73 | 보류 | 맞음: 서버 `thrust`는 땅 · 물 조건을 안 보고, 클라이언트는 로컬 설정 값으로 판단(설정 동기화 없음). 다만 움직임은 원래 클라이언트가 정해 변조 클라이언트 대비 이득이 없고, 설정 불일치는 서버 설정을 바꾼 멀티에서만 | 정책: 클라이언트가 설정을 쓰는 곳 전체의 동기화(서버 → 클라이언트 페이로드) 여부. 멀티 실테스트 뒤 |
| 74 | 수정 | 확인: 그리기 이동 범위 `rows*18-17`, 끌기 분모 `rows*18-15` → 끝에서 약 2px 어긋남 | `scrollTo` 분모를 `rows * 18 - 17.0`으로(손잡이 반 7은 그대로). 클라이언트 전용 |
| 75 | 수정 | 16과 같음(`IDLE` · `MISSING` · `NO_PRODUCER`) | `toLowerCase(Locale.ROOT)` |
| 76 | 수정(주석) | 확인: 거상은 X1에서 전용 모델 | "(임시 렌더러, M11에서 전용 모델)" → "(전용 모델, X1)" |
| 77 | 보류 | 렌더러 · 모델의 사망 페이드 · 내려치기 식 공통화는 2(보스 공통 부모) 결정 뒤 클라이언트 쪽에서 같이 | |
| 78 | 오탐 | 앞 조건이 뒤 조건에 포함된 중복일 뿐, 노이즈 | |
| 79 | 오탐 | 노이즈 | |
| **80** (Opus 추가) | 수정 | 16 · 75와 같은 문제: `PlaytestCommand.Stage.id()`의 `name().toLowerCase()` → 터키어 로캘에서 `I`가 든 단계 이름이 바뀜 | `toLowerCase(Locale.ROOT)` |
| **81** (Opus 추가) | 수정 | F1 아래: 서랍을 부수면 64개를 쏟고, 폭발이면 최대 64개 복제 | `DrawerBlockEntity`에 빈 `preRemoveSideEffects` 오버라이드(바닐라 셜커 상자 · 차원 창고와 같음) + 주석. **시험 추가**(Storage): 레드스톤 5000 서랍을 `level.destroyBlock(pos, true)` → 떨어진 아이템 엔티티가 서랍 하나(내용물 5000)뿐, 낱개 레드스톤 없음. 가능하면 폭발(`level.explode`)로도 한 번 |

### 7-3. 수정 묶음 (파일 소유)
다섯 묶음은 파일이 겹치지 않는다. 공용 파일(`lang` · README · ROADMAP · `src/gametest/resources/fabric.mod.json`)은 아무도 안 건드린다 — 새 시험은 **이미 등록된 시험 파일**에 메서드로만 넣는다. 각자 작업 트리에서 고친 뒤 `./gradlew build`(GameTest 292 + 새 시험) 통과 확인, 새 시험은 고치기 전에 실패하는지도 확인. 위치는 줄 번호가 아니라 문구로 찾는다.
경로 앞 `main/` = `src/main/java/io/github/yhyeop/sanupcraft/`, `client/` = `src/client/java/io/github/yhyeop/sanupcraft/client/`, `test/` = `src/gametest/java/io/github/yhyeop/sanupcraft/test/`.

| 묶음 | 항목 | 소유 파일 (이 묶음만 수정) | 시험 |
|---|---|---|---|
| **K1 보스 · 클라이언트 화면** | 1, 3, 7, 8, 74, 75, 76 | `main/boss/BossParty.java`(주석만), `main/boss/FurnaceWarden.java`, `main/boss/RustyGuardian.java`, `client/storage/TerminalScreen.java`, `client/storage/StockerScreen.java`, `client/SanupcraftClient.java`(주석만) | 새 시험 없음. build + 가능하면 `runClientGameTest` |
| **K2 창고** | 12, 13, 14, 16, 81 | `main/storage/OwnerLock.java`(주석만), `main/storage/AutocrafterBlockEntity.java`(주석만), `main/storage/StockerBlockEntity.java`, `main/storage/PortableTerminalItem.java`, `main/storage/DrawerBlockEntity.java`, `test/SanupcraftStorageGameTest.java`(서랍 부수기 시험 추가만) | 새 시험 1~2 (81: 부수기, 가능하면 폭발) |
| **K3 증기 · 산업** | 23, 24, 26, 37, 45, 49(충전기) | `main/power/BoilerBlockEntity.java`, `main/power/SteamChargerBlockEntity.java`, `main/power/SteamStorage.java`(주석만), `main/industry/FurnaceTowerControllerBlockEntity.java`, `test/SanupcraftM8bGameTest.java`(탑 시험 추가만) | 새 시험 1 (37) |
| **K4 물류 · 수송 · 엔드** | 28, 30, 47, 48, 49(닻), 50, 52, 53, 54 | `main/logistics/ItemFilter.java`, `main/logistics/EnderLinkBlockEntity.java`(주석만), `main/logistics/EnderAnchorBlockEntity.java`, `main/logistics/BeltMotorBlockEntity.java`(주석만), `main/transport/SteamLocomotive.java`, `main/transport/CargoStationBlockEntity.java`(주석만), `main/end/EndIslandStructure.java`(주석만), `main/end/VoidGear.java` | 새 시험 없음. 기존 닻 시험(`anchorHandsOverAfterReload` 등) 그대로 통과 |
| **K5 생산 · 서버 · 장비 · 실테스트** | 49(일꾼), 57, 65, 66, 68, 80 | `main/server/GoalBoardBlockEntity.java`, `main/server/GoalCommand.java`, `main/production/SteamWorkerBlockEntity.java`, `main/production/TreeFellerBlockEntity.java`, `main/gear/Jetpack.java`(주석만), `main/playtest/PlaytestCommand.java`, `test/SanupcraftM10GameTest.java`(목표판 시험 추가만), `test/SanupcraftM9aGameTest.java`(벌목기 저장 시험, 선택) | 새 시험 1~2 (65 필수, 66 선택) |

주의:
- K3 · K4 · K5가 49를 나눠 맡는다(파일은 각자 하나). 지우는 건 `Containers.dropContents(...)` 한 줄뿐 — 닻의 `setForced(false)`, 일꾼의 `pending` 떨구기 · `pending.clear()`는 남긴다.
- K5의 `SteamWorkerBlockEntity`는 `TreeFellerBlockEntity`의 부모라 같은 묶음에 둔다.
- K1의 3번은 바닐라 `net.minecraft.world.entity.EntitySelector.NO_CREATIVE_OR_SPECTATOR`를 쓰고 공통 도우미를 새로 만들지 않는다(2단계 보스 공통화와 겹치지 않게).

2단계로 올린 것 (1단계에서 손대지 않음):
- **2 (+77) 보스 공통 부모**: 세 보스(`RustyGuardian` · `FurnaceWarden` · `VoidColossus`)의 중복을 `Monster` 아래 공통 부모로 묶을지, 묶으면 어디까지(보스 바 · party 저장 · `actuallyHurt` · 방패 깨기 · 경고 · PatternGoal 뼈대)인지. 77(렌더러 · 모델)은 그 결정을 따라간다. K1이 먼저 고친 3 · 7 · 8을 그대로 옮긴다.
- **40 · 41 · 42 · 43 멀티블록 공통화**: `BigBoilerShape` ↔ `MultiblockShape`, 두 제어기 · 외벽 블록의 `notifyControllers` · `setCasingsFormed` · 힌트를 한 틀로. 그때 "두 구조물이 외벽 · 벽돌을 공유할 수 있나"(43)를 규칙으로 정한다(금지 → 모양 확인에서 거부 / 허용 → 끌 때 내 제어기에 이어진 것만). K3이 먼저 고친 37 · 45를 그대로 둔다.

보류 3건(51 닻 반경 기록, 73 제트팩 설정 동기화, 77 렌더러 공통화)은 이 표로만 넘긴다. 3단계로 올린 것 없음.

### 7-4. 비판적 Sonnet 반론
판정당 1회. 근거는 코드 · 바닐라 소스를 직접 읽은 것 (`BlockEntity.preRemoveSideEffects` · `LevelChunk.setBlockState` · `Level.destroyBlock` · `ServerPlayerGameMode.destroyBlock` · `BlockBehaviour.onExplosionHit` · `Containers.dropItemStack` · `HopperBlockEntity.tryMoveInItem` · `GameRules`). 최종 결정은 Opus.
81건 중 **반론 6건**(R1~R6. 그중 R1~R3은 #81 하나에 대한 보강이라 "수정" 판정은 유지하고 사양만 바꾸자는 것), 동의하되 보정 1건(R7), 질문받은 지점의 검증 결과(V1~V4)를 적는다. 나머지 판정은 읽어 보고 동의라 건너뛴다.

| 번호 | 반론 | 근거 | 요청 | 확신도 |
|---|---|---|---|---|
| R1 (#81 수정 방향 보강: 도구 없이 부수면 소멸) | 빈 `preRemoveSideEffects`로 바꾸면 **맨손 · 도끼로 부술 때 서랍 내용물 전부가 아무 흔적 없이 사라진다**. 서랍 블록은 `requiresCorrectToolForDrops()`라서 곡괭이가 아니면 `playerDestroy`(= 전리품 표)가 아예 안 불린다. 지금은 그 경우에도 보기 칸 최대 64개가 쏟아지지만(나머지는 어차피 소실), 고치고 나면 5000개든 1만 개든 전부 증발한다. 바닐라 셜커 상자를 본뜬다면서 셜커는 도구 제한이 없다는 점이 빠졌다 | `ModStorage.java:119-125` (`.strength(3.0F, 6.0F).requiresCorrectToolForDrops()`), `ServerPlayerGameMode.java:319-323` (`canDestroy = hasCorrectToolForDrops` 일 때만 `playerDestroy`), 곡괭이 태그 `data/minecraft/tags/block/mineable/pickaxe.json`에 두 서랍이 있고 `needs_*_tool` 태그는 없음 → 아무 곡괭이면 됨. 시나리오: 철제 서랍에 레드스톤 5000개를 넣고 맨손으로 부수면 현재는 64개가 나오고 4936개 소실, 수정 뒤엔 5000개 소실 | 둘 중 하나를 #81에 같이: (a) `drawerProperties`에서 `.requiresCorrectToolForDrops()` 삭제(셜커와 같게 "부숴도 내용물 유지"를 도구와 무관하게 보장). (b) 유지하려면 서랍 `playerWillDestroy`에서 도구가 틀릴 때 내용물을 아이템 하나로 묶어 떨구는 별도 코드 필요(복잡). (a)를 권장. `ModStorage.java`를 K2 소유에 추가 | 상 (코드 사실) / 중 (a가 디자인 의도에 맞는지는 Opus 판단) |
| R2 (#81 보강: 폭발 · 전리품 조건) | 폭발 복제는 막아도 **내용물이 "살아남는" 것은 아니다**. 서랍 전리품 표에 `survives_explosion` 조건이 있어 크리퍼(`mob_explosion_drop_decay` 기본 true, 반경 3) 폭발이면 서랍 아이템이 **약 2/3 확률로 안 나온다**. 즉 크리퍼 폭발에서 "내용물 유지"는 확률 1/3. 이전에도 소실이었으니 퇴행은 아니지만, 판정문이 "폭발 복제 해결"로 끝나 플레이어는 내용물이 지켜진다고 이해하기 쉽다 | `data/sanupcraft/loot_table/blocks/iron_drawer.json` · `steel_drawer.json` (`"condition": {"type": "minecraft:survives_explosion"}`), `GameRules.java:56` (`MOB_EXPLOSION_DROP_DECAY` 기본 true), `:28`(블록 폭발도 true), `:86`(`TNT_EXPLOSION_DROP_DECAY` 기본 false). 손으로 쓴 JSON이라 생성기에 덮어써질 일 없음 (`DataGenerator` 없음) | 두 JSON에서 `condition` 줄 삭제(내용물 든 아이템은 폭발에도 드롭). 바닐라 셜커 전리품 표에 이 조건이 없다는 건 내 기억이고, 이 작업 환경의 디컴파일에는 데이터 JSON이 없어 직접 확인하지 못함. 원치 않으면 판정문에 "폭발은 감쇠 확률로 소실, 의도됨"이라고 적기만 | 중 |
| R3 (#81 시험 사양 보정) | 시험을 `level.destroyBlock(pos, true)`로 쓰는 건 **옳고 충분하다**: `Level.destroyBlock`은 `dropResources` 먼저 → `setBlock`(= `preRemoveSideEffects`) 나중이라 폭발과 같은 순서의 복제가 그대로 재현돼 고치기 전에 확실히 실패한다. 반면 "가능하면 `level.explode`"는 권하지 않는다: 반경 1이면 서랍(폭발 저항 6)이 안 부서지고, 크게 하면 이웃 시험 구조물을 건드리며, 크리퍼형이면 R2의 확률 때문에 흔들린다 | `Level.java:276-295` (순서), `BlockBehaviour.java:174-188` (폭발도 `getDrops` → `setBlockAndUpdate(AIR)`), `ModStorage.java:122` (저항 6) | 시험은 `destroyBlock(pos, true)` 하나만 필수로 하고 폭발 시험은 빼거나 `TNT` 상호작용 + 반경 4 + 주변 빈 구역일 때만(감쇠 규칙 false라 결정적). 기대값: 아이템 엔티티가 서랍 아이템 하나(`DRAWER_CONTENTS.count == 5000`)이고 낱개 `REDSTONE` 엔티티 합 0 | 상 |
| R4 (#17 · #18 · #69 오탐 사유가 사실과 다름) | "시험 · 디버그용으로 무해"라고 했지만 **시험에서도 안 부른다**. 같은 기준으로 #47(`ItemFilter.contains`) · #24(`isCharging`) · #68(`GoalCommand.register`)은 지우면서 호출 0건인 다른 함수는 남기는 건 판정 기준이 일관되지 않다 | `grep`: `PortableTerminalItem.isEnder`(`:52`) · `StorageNetwork.nodes`(`:64`) · `CropHarvesterBlockEntity.cursor`(`:158`) · `TreeFellerBlockEntity.felled`/`queued` · `TreeFelling.Tree.size`(`:40`) 모두 정의 한 줄 외에 `src/main` · `src/client` · `src/gametest` 어디에도 호출 없음 | 기준을 하나로: 호출 0건 공개 함수는 지운다(`PortableTerminalItem` · `StorageNetwork`는 K2, `CropHarvester` · `TreeFeller` · `TreeFelling`은 K5 영역). `felled` 필드와 저장 키는 유지. 아니면 판정문에 "남기는 기준"을 적기 (`CropHarvesterBlockEntity` · `TreeFelling`은 소유 목록에 없으니 추가 필요) | 상 (사실) / 하 (중요도, 건너뛰어도 무방) |
| R5 (#2 보스 공통 부모: 에스컬레이션 방향) | 2단계로 올리는 건 동의하지만 **"`Monster`와 세 보스 사이 공통 부모"가 맞는 도구인지**는 다시 보자. 중복의 상당수는 위임 한두 줄짜리 오버라이드(`actuallyHurt` · `getDamageAfterMagicAbsorb` · `dropCustomDeathLoot` · 저장/읽기. 상태는 이미 `BossParty`가 받음)이고, 진짜 복사본은 **정적 함수**(`breakShield` 10줄 · `horizontalDistance` · `warnRust`/`warnHeat` 쿨다운 맵 · `PatternGoal` 뼈대)다. 이건 상속 없이 `BossParty`(또는 `BossCombat`)의 정적 도우미로 먼저 옮길 수 있고, 그것만으로 복사의 절반이 사라진다. 부모 클래스는 그다음 단계로 쪼개야 시험이 깨질 때 원인을 좁힌다 | `RustyGuardian.java:379-400` ↔ `FurnaceWarden.java:469-490`(문구 키만 다르고 본문 동일). 세 보스의 `hurtServer`는 **서로 다르다**(수호자는 `IS_FIRE` 면역 없음 · 파수꾼은 `IS_FIRE` 면역 · 거상은 방어막 선검사 `VoidColossus.java:423-445`) → 부모가 가져갈 수 있는 건 `super` 뒤 `playSound` 류뿐. 시험 12개 파일이 세 클래스를 직접 씀(`grep -rl`). 저장 키는 `BossParty.save/load` 안이라 부모로 옮겨도 NBT 키는 안 변함(안전), 거상의 `shield_wave` · `crystals`는 자기 클래스에 남아야 함 | 2단계 안에서 두 걸음으로: ① 정적 도우미 이관(동작 불변, `./gradlew build`), ② 그다음에만 부모(보스 바 · `setCustomName`/`refreshBossBar`/`startSeenByPlayer`/`stopSeenByPlayer`/`removeWhenFarAway`/저장 위임). 둘 다 **K1 병합 뒤**, `runClientGameTest`(렌더러가 세 클래스를 직접 참조) 포함. **3단계는 불필요**(엔티티 id · NBT · 패킷 · 렌더러 계약이 그대로라 "구조 자체를 바꾸는" 경우가 아님). Opus 직접 수정으로 충분 | 중 (방향) / 하 (두 걸음 분할이 꼭 필요한지) |
| R6 (#41 분리) | 40~43을 한 덩어리로 2단계에 올렸는데 **41(`BigBoilerShape` ↔ `MultiblockShape`)은 설계 판단 없이 기계적**이다. `MultiblockShape`는 이미 깊이 · 높이 · 배치 · 외벽 판정을 생성자로 받고, `BigBoilerShape`는 그 특수한 경우(3x3x4, 가운데 기둥 2 · 3층 빈칸)를 정적으로 박은 98줄이다. 42(`notifyControllers`/`setCasingsFormed`)와 43(공유 벽돌 규칙)만 두 제어기 사이의 규칙 결정이 필요하다 | `diff BigBoilerShape MultiblockShape`: 차이는 정적 ↔ 인스턴스와 `Layout`뿐. 호출처는 `BigBoilerCasingBlock.java:30` · `BigBoilerControllerBlock.java:61-63, 119` · `BigBoilerControllerBlockEntity.java:35` · 시험 `SanupcraftM8aGameTest.java:27, 60, 67, 246`(`BigBoilerShape.Part`/`casings`를 `MultiblockShape`로 바꾸는 수정 필요) | 41은 2단계 안에서 **먼저 단독 진행**(`static final MultiblockShape BIG_BOILER = new MultiblockShape(3, 4, layout, isCasing)`), 40 · 42 · 43만 규칙 결정(외벽 공유 허용 여부) 뒤로. 43은 동의(다음 확인에서 이웃이 다시 켜므로 일시적: `FurnaceTowerControllerBlockEntity.java:297-312`의 `now \|\| changed` 재연결) | 하 |
| R7 (#65 동의하되 보정) | 판정 유지. 두 가지만 보정: (1) 기본 목표 순서는 철 주괴 1000 → 철판 2000 → 리벳 5000 → 기어 500으로 **연속 두 목표가 같은 아이템이 아니다**(`Goals.java:16-19`). "다음 목표가 같은 아이템이면 한 번 더 `insert`"는 `/goal set`으로 같은 아이템을 연속 지정했을 때만 도는 가지라, 시험의 기대값(남은 10 · 64개 → 진행 = 목표, 판 둘레 54개)은 이 가지 없이 결정적으로 54다. (2) 초과분은 `y+1`에 떨구는데 완료 직후 `spawnReward`가 같은 칸(`above()`)에 보상 상자를 놓는다. 아이템 엔티티가 상자 칸 안에 생겨 어색할 수 있어도 유실은 아니고, 기존 폴백(`spawnReward` 끝)이 이미 같은 위치다 → 그대로 | `GoalBoardBlockEntity.java:258-268`(`spawnReward` 후보 `above()` 첫째), `HopperBlockEntity.java:319-322`(빈 칸 → `setItem` 스택째) | 시험 기대값에서 "(다음 목표가 같은 아이템이면 …)" 단서를 빼고 54로 고정 | 하 |

#### 질문받은 세 지점 검증 (반론 아님)
- **V1. `setChanged` 규칙(줄어드는 쪽만 수정, 받는 쪽 증기는 오탐)에 구멍이 있나**: 받는 쪽 **아이템 · 유체**가 표시 없이 바뀌는 경로를 따라가 봤다. 결과는 **구멍 없음(#14 · #65 외)**.
  - 유체: `FluidTank.fill/drain`이 `onChange`로 BE의 `setChanged`를 부르고(`FluidTank.java:50-64`), 펌프 `pumpOnce`(`PumpBlockEntity.java:85-92`) · 펌프의 밀어내기(`:143-145`, `target.fill` → 대상의 `setChanged`) · 보일러 `fill`(`BoilerBlockEntity.java:246-256`) · 기계(세척기) `fill`(`MachineBlockEntity.java:409-419`) · 탱크 · 경험치 탱크(`ExperienceTankBlockEntity.java:28`, `MobGrinderBlockEntity.store`가 `orb.discard()` 뒤 `tank.fill`)가 전부 표시한다. 보일러 `drawFromTanks`의 `tank.drain`도 표시.
  - 아이템: `StorageNetwork.fill`(`:262-285`)은 `changed`면 `container.setChanged()`, `SteamWorkerBlockEntity.stash/flushPending` · `BarterCollector` 흡수(`taken > 0`) · `FilterHopperBlockEntity.eject`(`removeItem` + 대상 `setChanged`) · `CargoStationBlockEntity.move`(`addItem`이 대상 표시, `removeItem`이 출발 표시) · 엔더 송신기 `setItem` → 수신기 `addItem` · `GoldDispenserBlock`/`SoulFurnaceBlock` 손 넣기(`setItem` 또는 `setChanged`)가 모두 표시.
  - 서랍 · 차원 창고: `DrawerBlockEntity`의 모든 변경은 `changed()`/`setChangedOnly()`를 거치고, 창고 채널은 `SimpleContainer.setChanged` → `setDirty`(`VaultData.java:67-71`). #27의 `removeItemNoUpdate`는 바닐라에서 `AbstractContainerMenu.clearContainer`(제작칸 닫힘) · 상인 · 석재절단기 같은 **자기 결과 칸**에만 쓰이고 `ChestMenu`/깔때기는 안 부른다(디컴파일 호출처 확인) → 오탐 동의.
  - 증기: 보낸 쪽이 자기 청크를 표시하므로 받는 쪽 누락은 소실일 뿐 복제가 아니다 → 규칙 동의.
- **V2. 렌치 `UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS`(= 플래그 256)와 #81의 상호작용**: `Wrench.pickUp`은 `copyData`(= `saveCustomOnly` + `collectComponents`, 서랍은 `DRAWER_CONTENTS`에 보기 칸까지 합친 전체 개수)로 아이템을 먼저 만든 뒤 256을 켜고 `setBlock`하므로 `preRemoveSideEffects`가 아예 안 불린다(`LevelChunk.java:304-311`, `sideEffects = (flags & 256) == 0`). 빈 오버라이드를 넣어도 렌치 경로는 변하지 않는다 → 충돌 없음. 소실 경로는 R1(도구 없음)과 `/setblock replace`(전리품 표를 안 거침)뿐이며 셜커도 같다. 플레이어가 곡괭이로 부수면 지금도 복제는 없다(`removeBlock` 먼저 → 보기 칸 `split` → `playerDestroy`의 `contents()`가 `sync()`로 나머지만 계산)는 Opus 설명도 확인했다. 다만 개수가 64 이하면 보기 칸이 전부 쏟아져 아이템에는 빈 서랍(업그레이드만)이 남는 게 현재 동작이라, 수정 뒤에 "작은 개수일수록 달라 보이는" 변화가 있다는 점만 알아 둘 것.
- **V3. 부모 클래스(#2)를 지금 해도 안전한가**: 저장 키는 안전하다(파티 상태는 전부 `BossParty.save/load`, 거상 전용 `shield_wave` · `crystals`만 자기 클래스). 위험은 시험 쪽이다: 12개 시험 파일이 `RustyGuardian` · `FurnaceWarden` · `VoidColossus`와 각자의 중첩 `Pattern` · `hitWithRust/Heat` · `startPattern`을 직접 부른다. 생성자 제네릭(`EntityType<? extends RustyGuardian>`), 필드 초기화 순서(`ServerBossEvent`가 필드 초기화에서 `getDisplayName()`을 부름), `private class PatternGoal`의 비공개 필드 접근이 부모로 올리면 `protected`가 돼야 한다는 점이 변경 폭을 키운다. K1이 `FurnaceWarden` · `RustyGuardian`을 같은 라운드에 고치므로 **K1 병합 뒤에** 시작해야 한다. R5의 두 걸음 순서면 안전하다.
- **V4. #3 창작 · 관전 제외가 기존 시험을 깨나**: 보스 시험이 쓰는 모의 플레이어는 전부 `GameType.SURVIVAL`(`FixBoss` · `M2` · `M3` · `Fin` · `C3` · `Stage2`)이고 `CREATIVE`는 보스와 무관한 시험(`Bundle3:235`, `FixLogistics:216, 220`, `M9a:490`)에만 쓴다 → 깨지지 않음. `Monster`의 일반 표적 선정도 창작을 이미 제외하므로 동작 일관성도 맞다.

#### 반론하지 않은 판정 (읽고 동의)
- 오탐 F1(10 · 35 · 36): `BlockEntity.preRemoveSideEffects`가 `Container`면 `Containers.dropContents`를 부르고(`BlockEntity.java:235-239`, `LevelChunk.java:304-311`), 자동 제작대 · 기계 · 탑 BE는 모두 `BaseContainerBlockEntity`이며 슬롯 수가 `items.size()`/`SLOT_COUNT`로 전부 노출돼 재료 · 결과 · 부산물 · 업그레이드가 떨어진다 → 직접 확인. 부수는 시험이 없다는 점만 약점이니 값싼 시험 하나(`destroyBlock` → 아이템 엔티티에 재료 존재)를 K2/K3에 얹으면 좋지만 필수는 아님.
- 수정: 1 · 3 · 7 · 8 · 12 · 13 · 14(`feedMachine`의 첫 `fill` 뒤 `setChanged`: 둘째가 실패하면 네트워크에서 뺀 재료가 기계 청크 미표시로 사라질 수 있음, 맞음) · 16 · 23 · 24 · 26 · 28 · 30 · 37(`finish`가 이미 `Math.min(units, available)`이라 `work` 쪽만 보강하면 됨) · 45 · 47 · 48 · 49 · 50 · 52 · 53 · 54 · 57 · 65 · 66(`replant`가 `isAir`를 확인하므로 구조가 선 채 불러와도 무해, 위에서 아래로 베므로 바닥 원목이 마지막) · 68 · 74(그리기 이동 범위 `trackHeight - 2 - 15 = rows*18-17`, 끌기 분모 `-15` 확인) · 75 · 76 · 80.
- 오탐: 4 · 5 · 6 · 9 · 10 · 11 · 15 · 19 · 20 · 21 · 22 · 25 · 27 · 29 · 31~36 · 38 · 39 · 44 · 46 · 55 · 56 · 58~64 · 67 · 70~72 · 78 · 79. (#67은 통나무 벽 옆에서 10틱마다 시작 칸 최대 12곳 × 256칸 탐색이 도는 건 사실이지만 상한이 있고 반 초 간격이라 허용.)
- 보류: 51 · 73 · 77. 2단계: 2 · 40 · 42 · 43 (R5 · R6의 분할 요청 외 동의). **3단계로 올릴 것은 없음** (동의).

#### 요약 (Opus 결정 요청)
| 반론 | 요청 | 확신도 |
|---|---|---|
| R1 | #81에 `requiresCorrectToolForDrops()` 삭제 (또는 소실 감수를 명시) | 상 / 중 |
| R2 | 서랍 전리품 표의 `survives_explosion` 삭제 여부 | 중 |
| R3 | #81 시험은 `destroyBlock(pos, true)`만 필수, 폭발 시험은 선택 | 상 |
| R4 | 호출 0건 함수 일괄 삭제 (또는 남기는 기준 명시) | 상 / 하 |
| R5 | #2는 정적 도우미 → 부모의 두 걸음으로, K1 병합 뒤 | 중 / 하 |
| R6 | #41 먼저 단독 | 하 |
| R7 | #65 시험 기대값 54로 고정 | 하 |

### 7-5. 최종 결정 (Opus)
| 반론 | 결정 | 이유 |
|---|---|---|
| R1 서랍 도구 없이 부수면 소멸 | **받아들임 (a)** | 확인: `drawerProperties`에 `.requiresCorrectToolForDrops()`. 빈 `preRemoveSideEffects`만 넣으면 맨손 · 도끼로 부술 때 내용물 전부가 사라진다. 바닐라 셜커 상자 · 통도 도구 제한이 없다 → `drawerProperties`에서만 삭제(곡괭이 태그는 그대로 두어 캐는 속도 유지). 같은 파일의 다른 속성 함수(저장 기계용, 바로 위)는 손대지 않는다 |
| R2 폭발 · `survives_explosion` | **기각** | 확인: 바닐라 `BlockLootSubProvider.createShulkerBoxDrop`도 `applyExplosionCondition`으로 같은 조건을 건다 → 셜커와 같은 규칙. 퇴행도 아님. 판정문 보정만: #81은 "복제 해결", 크리퍼 폭발 감쇠로 서랍째 사라질 수 있는 것은 바닐라와 같은 의도 |
| R3 #81 시험 사양 | **받아들임** | `Level.destroyBlock`이 `dropResources` → `setBlock` 순서라 고치기 전에 확실히 실패한다. 폭발 시험은 뺀다. 맨손 부수기 시험을 하나 더(R1 고정) |
| R4 호출 0건 함수 기준 | **받아들임** | 기준을 하나로: **호출 0건(본 · 클라이언트 · 시험 모두)인 함수는 지우고, 그 때문에 쓰기만 남는 필드 · 저장 키도 지운다**(#23과 같은 기준). 17 · 18 · 69를 오탐 → 수정(죽은 코드)으로 바꾼다. `cursor`처럼 다른 로직이 읽는 필드는 남긴다 |
| R5 #2 두 걸음 | **받아들임** | 중복의 큰 몫이 정적 함수(`breakShield` · `horizontalDistance` · 경고 쿨다운)이고 세 보스 `hurtServer`가 서로 달라 부모가 가져갈 게 적다. ① 정적 도우미 → ② 부모 순서, K1 병합 뒤, 3단계 불필요 |
| R6 #41 먼저 단독 | **받아들임** | `BigBoilerShape`는 `MultiblockShape`의 특수한 경우(정적 · `Layout`뿐 차이). 규칙 판단 없이 기계적으로 옮길 수 있다 |
| R7 #65 시험 기대값 | **받아들임** | 기본 목표 순서에 같은 아이템 연속이 없다(`Goals`). 기대값 54로 고정. 같은 아이템 다음 목표용 재`insert` 가지는 코드에 남기되(`/goal set` 연속 지정) 시험은 안 건다 |
| V1~V4 | 동의 | V1: 받는 쪽 아이템 · 유체 경로에 다른 구멍 없음. V2: 렌치(플래그 256)는 `preRemoveSideEffects`를 안 거쳐 #81과 충돌 없음 — 64개 이하 서랍을 곡괭이로 부수면 지금은 빈 서랍 + 낱개, 고친 뒤엔 내용물 든 서랍 하나(의도대로). V3: #2는 K1 병합 뒤. V4: #3이 기존 시험(전부 SURVIVAL)을 안 깬다 |

최종 집계(81건 = 지적 79 + Opus 추가 2): **수정 대상 34** (7-2의 31 + R4로 17 · 18 · 69) · 오탐 39 · 보류 3 (51 · 73 · 77) · 2단계로 5 (2 · 40 · 41 · 42 · 43) · 3단계로 0.

### 7-6. 최종 수정 묶음
다섯 묶음은 파일이 겹치지 않는다. 각자 자기 git 작업 트리에서 병렬로 고친다. **공용 파일(`lang/*.json` · README · ROADMAP · `src/gametest/resources/fabric.mod.json` · 전리품 JSON)은 아무 묶음도 건드리지 않는다** — 새 시험은 이미 등록된 시험 파일에 메서드로만 넣는다. REFACTOR.md도 묶음은 안 고친다(수정 기록은 조정자가 병합 뒤 적는다).
경로 앞 `main/` = `src/main/java/io/github/yhyeop/sanupcraft/`, `client/` = `src/client/java/io/github/yhyeop/sanupcraft/client/`, `test/` = `src/gametest/java/io/github/yhyeop/sanupcraft/test/`. 위치는 줄 번호가 아니라 문구로 찾는다.
모든 묶음 공통 완료 조건: `./gradlew build` 통과(GameTest 292 + 새 시험), 새 시험은 고치기 전에 한 번 실패하는지 확인(되돌려 보기), 주석만 고치는 파일은 코드 변경 금지.

| 묶음 | 소유 파일 (이 묶음만 수정) | 시험 |
|---|---|---|
| **K1 보스 · 클라이언트 화면** (1, 3, 7, 8, 74, 75, 76) | `main/boss/BossParty.java`(주석만), `main/boss/FurnaceWarden.java`, `main/boss/RustyGuardian.java`, `client/storage/TerminalScreen.java`, `client/storage/StockerScreen.java`, `client/SanupcraftClient.java`(주석만) | 새 시험 없음. build + `runClientGameTest` |
| **K2 창고** (12, 13, 14, 16, 17, 18, 81) | `main/storage/OwnerLock.java`(주석만), `main/storage/AutocrafterBlockEntity.java`(주석만), `main/storage/StockerBlockEntity.java`, `main/storage/PortableTerminalItem.java`, `main/storage/StorageNetwork.java`, `main/storage/DrawerBlockEntity.java`, `main/storage/ModStorage.java`(`drawerProperties` 한 줄만), `test/SanupcraftStorageGameTest.java`(시험 추가만) | **필수 2**: 81 곡괭이 부수기 · 81 맨손 부수기. 선택 1: 자동 제작대 부수기(F1 고정) |
| **K3 증기 · 산업** (23, 24, 26, 37, 45, 49-충전기) | `main/power/BoilerBlockEntity.java`, `main/power/SteamChargerBlockEntity.java`, `main/power/SteamStorage.java`(주석만), `main/industry/FurnaceTowerControllerBlockEntity.java`, `test/SanupcraftM8bGameTest.java`(시험 추가만) | **필수 1**: 37 |
| **K4 물류 · 수송 · 엔드** (28, 30, 47, 48, 49-닻, 50, 52, 53, 54) | `main/logistics/ItemFilter.java`, `main/logistics/EnderLinkBlockEntity.java`(주석만), `main/logistics/EnderAnchorBlockEntity.java`, `main/logistics/BeltMotorBlockEntity.java`(주석만), `main/transport/SteamLocomotive.java`, `main/transport/CargoStationBlockEntity.java`(주석만), `main/end/EndIslandStructure.java`(주석만), `main/end/VoidGear.java` | 새 시험 없음. 기존 닻 시험(`anchorHandsOverAfterReload` · `anchorUntracksOnRemoved` · `anchorForcesChunksAndReleasesOnlyItsOwn`) · 열차 · 공허 장비 시험 그대로 통과 |
| **K5 생산 · 서버 · 장비 · 실테스트** (49-일꾼, 57, 65, 66, 68, 69, 80) | `main/server/GoalBoardBlockEntity.java`, `main/server/GoalCommand.java`, `main/production/SteamWorkerBlockEntity.java`, `main/production/TreeFellerBlockEntity.java`, `main/production/TreeFelling.java`, `main/production/CropHarvesterBlockEntity.java`, `main/gear/Jetpack.java`(주석만), `main/playtest/PlaytestCommand.java`, `test/SanupcraftM10GameTest.java`(시험 추가만), `test/SanupcraftM9aGameTest.java`(시험 추가만) | **필수 1**: 65. 선택 1: 66 |

**K1 단계**
- 1: `BossParty` 클래스 문서 "기여자가 없으면 … 보스 자리에 하나만 떨군다" → "필수품 `1 + 단계`개만 떨군다". `giveKeyItem` 문서 "필수품만 떨군다" → "필수품(`1 + 단계`개)만 떨군다".
- 3: `FurnaceWarden`의 화염 분출(`BLAST_RANGE`) · 장판 폭발(`explodeRings`) · 도약 착지(`LEAP_RADIUS`), `RustyGuardian`의 돌진(`inflate(0.6)`) · 내려치기(`slamReach`) — 다섯 `getEntitiesOfClass(Player.class, …)`에 셋째 인자 `EntitySelector.NO_CREATIVE_OR_SPECTATOR`. 새 도우미 만들지 않음.
- 7: `RustyGuardian`에 `public static final byte EVENT_ATTACK = 4;`(파수꾼과 같은 이름), `broadcastEntityEvent(this, (byte) 4)` · `if (id == 4)` 교체.
- 8: `FurnaceWarden.maxRings()` 주석 "장판은 20칸 안 플레이어 발밑마다 하나" → "장판은 가로 · 세로 ±20칸 상자 안 플레이어 발밑마다 하나".
- 74: `TerminalScreen.scrollTo` 분모 `rows * 18 - 15.0` → `rows * 18 - 17.0`.
- 75: `StockerScreen` `toLowerCase()` → `toLowerCase(Locale.ROOT)` (`java.util.Locale` 가져오기).
- 76: `SanupcraftClient` 주석 "(임시 렌더러, M11에서 전용 모델)" → "(전용 모델, X1)".

**K2 단계**
- 12: `OwnerLock` 클래스 문서 "주인 · OP가 … 주인 · OP만 쓴다" → "주인 · 같은 팀(M10) · OP가 … 그들만 쓴다", `canManage` 문서 "주인, OP, 또는 주인이 없을 때" → "주인, 주인과 같은 팀, OP, 또는 주인이 없을 때".
- 13: `AutocrafterBlockEntity` 클래스 문서 "재료 9칸(견본과 같은 칸에 같은 아이템)이 차면" → "견본이 쓰는 재료 칸(견본과 같은 칸에 같은 아이템)이 모두 차면".
- 14: `StockerBlockEntity.feedMachine`: 레시피마다 `boolean first = fill(… SLOT_INPUT …)`로 받고, 둘째 `fill`이 실패해 다음 레시피로 넘어가기 전에도 `if (first) machine.setChanged();` (성공 경로의 기존 `setChanged`는 그대로).
- 16: `PortableTerminalItem` `check.name().toLowerCase()` → `toLowerCase(Locale.ROOT)`.
- 17: `PortableTerminalItem.isEnder()` 삭제(필드 `ender`는 `inRange`가 쓰므로 유지).
- 18: `StorageNetwork.nodes()` 삭제, 쓰기만 남는 필드 `nodes`와 생성자 인자도 삭제(만들 때의 지역 변수 `nodes` · `maxNodes` 상한은 그대로).
- 81: `DrawerBlockEntity`에 `@Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {}` + 주석 "부숴도 내용물은 서랍 아이템에 담긴다 (바닐라 셜커 상자와 같다: 보기 칸을 따로 쏟지 않는다)". `ModStorage.drawerProperties`에서 `.requiresCorrectToolForDrops()` 한 줄 삭제(R1).
- 시험 ① `drawerBreaksIntoOneItem`: 강철 서랍에 레드스톤 5000 → 곡괭이를 든 모의 플레이어로 부수거나 `helper.getLevel().destroyBlock(abs, true)` → 둘레 아이템 엔티티: 서랍 아이템 정확히 하나(`DRAWER_CONTENTS.count == 5000`), 낱개 `REDSTONE` 합 0. ② `drawerBreaksByHandKeepsContents`: 같은 조건에서 맨손(빈손 모의 플레이어 `gameMode.destroyBlock`)으로 부숴도 서랍 아이템 하나 · 5000. ③(선택) 자동 제작대에 재료를 넣고 `destroyBlock` → 재료 아이템 엔티티가 있다.

**K3 단계**
- 23: `BoilerBlockEntity`의 `litDuration` 필드 · 대입 · `lit_duration` 읽기/쓰기 삭제(`litTime`은 그대로).
- 24: `SteamChargerBlockEntity`의 `charging` 필드 · 대입 · `isCharging()` 삭제.
- 26: `SteamStorage` 문서 → "증기를 모아 두는 블록 엔티티. 증기 계기판이 읽는다."
- 37: `FurnaceTowerControllerBlockEntity.work` 처음에 `int left = available(current);` → `left <= 0`이면 `batch = null; progress = 0; searchTimer = 0; return false;`, `left < current.units()`이면 같은 레시피 · 진행으로 `units`만 `left`인 묶음으로 바꿔 `batch`에 넣고 계속(증기 계산은 바뀐 `units`로). `finish`의 `Math.min`은 그대로.
- 45: 클래스 문서 "최대 {@link #BATCH_SIZE}개" → "최대 설정값(기본 {@link #BATCH_SIZE})개".
- 49(충전기): `preRemoveSideEffects`의 `Containers.dropContents(...)` 줄과 그 `if`를 삭제(`super`만 남김) + 주석 "내용물은 `super`(바닐라)가 떨군다". 오버라이드 자체가 `super`만 부르게 되면 오버라이드째 지운다.
- 시험 `towerCancelsBatchWhenInputsTaken`(M8b): 탑을 세우고 고압 증기 · 재료 넣기 → 몇 틱 돌려 `batchUnits() > 0` → 재료 칸 비움 → 한 틱 → `batchUnits() == 0`이고 증기가 그 틱에 줄지 않음. 재료 일부만 빼면 `batchUnits()`가 남은 수와 같은지도 한 번.

**K4 단계**
- 28: `EndIslandStructure` 문서 "서로 ±{@link #MAX_STEP} 안일 때만" → "가운데와 ±{@link #MAX_STEP} 안일 때만".
- 30: `VoidGear.init`에 `ServerLifecycleEvents.SERVER_STARTING.register(server -> { LAST_SAFE.clear(); LAST_RESCUE.clear(); });` + 주석 "한 JVM에서 월드를 바꾸면 다른 월드의 게임 시각 · 자리가 남는다 (로그아웃 때는 지우지 않는다: 쿨다운 우회 방지)".
- 47: `ItemFilter.contains` 삭제.
- 48: `EnderLinkBlockEntity` 클래스 문서 "주인 · OP만 수신기를 열고" → "주인 · 같은 팀 · OP만 수신기를 열고".
- 49(닻): `preRemoveSideEffects`의 `Containers.dropContents(serverLevel, pos, this);` 한 줄만 삭제(`setForced(false)`는 유지), 주석 "청크를 놓는다. 연료는 `super`(바닐라)가 떨군다".
- 50: `SteamLocomotive`에 `private int missingIndex = -1;`. `pullCars`에서 빈 칸을 만나면 `if (i != missingIndex) { missingIndex = i; missingTicks = 0; }` 뒤 `++missingTicks` 판정. 끝까지 돌면 · `detachFrom`에서 `missingIndex = -1`(기존 `missingTicks = 0` 옆).
- 52: 클래스 문서 "빈손이면 출발 / 멈춤" → "그 밖(빈손 · 다른 아이템)이면 출발 / 멈춤".
- 53: `poweredBelts` 주석에 "증기가 모자라면 0" 추가.
- 54: `CargoStationBlockEntity` 문서 "최대 {@link #ITEMS_PER_TRANSFER}개" → "최대 설정값(기본 {@link #ITEMS_PER_TRANSFER})개".

**K5 단계**
- 49(일꾼): `SteamWorkerBlockEntity.preRemoveSideEffects`의 `Containers.dropContents(serverLevel, pos, this);` 한 줄만 삭제(`pending` 떨구기 · `pending.clear()` 유지), 주석 "칸 내용물은 `super`(바닐라)가 떨군다".
- 57: `Jetpack.thrust` 문서 "창작 모드는 증기를 안 쓴다" → "창작 모드는 증기를 덜지 않는다 (그래도 1 이상 들어 있어야 뜬다)".
- 65: `GoalBoardBlockEntity.setItem`: `int accepted = insert(null, stack);` → 남은 수 `rest = stack.getCount() - accepted`; `rest > 0 && accepted > 0 && isGoalItem(stack) && progress < goalCount`이면 남은 몫으로 한 번 더 `insert`; 그래도 남으면 `level != null`일 때 `Containers.dropItemStack(level, x + 0.5, y + 1.0, z + 0.5, stack.copyWithCount(rest))`. 주석 "깔때기 · 벨트 · 일꾼은 스택째 넘긴다 (26.3 `tryMoveInItem`): 남는 몫은 판 위에 떨군다".
- 66: `TreeFellerBlockEntity` 저장에 `output.storeNullable("replant_at", BlockPos.CODEC, replantAt)`, `replant_log`(블록 id `Identifier`, 없으면 저장 안 함). 읽기는 `input.read(...).orElse(null)`, 블록은 `BuiltInRegistries.BLOCK.getOptional(id)`.
- 68: `GoalCommand.register` 삭제(쓰지 않게 된 import 정리).
- 69: `TreeFellerBlockEntity.felled()` · `queued()` 삭제, `felled`는 쓰기만 남으므로 필드 · 증가 · `felled` 읽기/쓰기도 삭제(#23 기준). `TreeFelling.Tree.size()` 삭제. `CropHarvesterBlockEntity.cursor()` 삭제(필드 `cursor`는 로직이 쓰므로 유지).
- 80: `PlaytestCommand.Stage.id()` `name().toLowerCase()` → `toLowerCase(Locale.ROOT)`.
- 시험 `goalBoardDropsOverflow`(M10): 목표판에 남은 목표 10이 되게 진행을 맞춘 뒤 `HopperBlockEntity.addItem(null, board, new ItemStack(목표 아이템, 64), Direction.UP)` → 목표 완료(`completedGoals()` +1), 판 둘레의 그 아이템 엔티티 개수 합 **54**. (선택) `treeFellerKeepsReplantSpot`(M9a): `startFelling` 뒤 `saveWithFullMetadata` → `loadStatic` 사본이 같은 자리에 심는지(원목 · 묘목을 두고 `replant` 경로까지).

병합 순서: K1~K5는 서로 독립이라 어떤 순서든 된다. 병합 뒤 조정자가 한 번 더 `./gradlew build` + `runClientGameTest`, 그 뒤 7-7의 2단계.

### 7-7. 2단계 계획
담당: **K1~K5가 모두 병합된 뒤 Opus 1기**가 직접 수정한다(설계 판단이 있는 일이라 나누지 않음). 걸음마다 `./gradlew build`(보스 걸음은 `runClientGameTest`도) 통과 후 다음으로. 비판적 Sonnet 반론은 아래 설계 결정(보스 ② 부모의 범위, 43 규칙)에 1회씩. 3단계는 필요 없음(엔티티 id · NBT 키 · 패킷 · 렌더러 계약이 그대로).

**보스 공통 코드 (#2, 77은 따라감)** — R5의 두 걸음
- ① 정적 도우미 이관(동작 불변): `boss/BossCombat`(final, 정적)으로 `breakShield` · `horizontalDistance` · 경고 쿨다운(`warnHeat`/`warnRust`의 플레이어별 200틱, 문구 키만 인자) · 패턴 대상 목록(`NO_CREATIVE_OR_SPECTATOR` 상자 조회)을 옮기고 세 보스가 부른다. `PatternGoal` 뼈대는 이 걸음에서 손대지 않는다.
- ② 부모 `PartyBossMonster extends Monster implements PartyBoss`: 보스 바(`ServerBossEvent` 생성 · `setCustomName`/`refreshBossBar`/`startSeenByPlayer`/`stopSeenByPlayer`), `removeWhenFarAway`, `BossParty` 저장/읽기 · `actuallyHurt` · `getDamageAfterMagicAbsorb` · `dropCustomDeathLoot` 위임(키 아이템은 추상 `keyItem()`)만. `hurtServer`(세 보스가 다름) · 패턴 · 거상의 `shield_wave`/`crystals`는 각자 남긴다. 필드 초기화 순서(보스 바가 `getDisplayName()`을 부름)는 부모 생성자에서 지연 생성으로 처리.
- 77(렌더러 · 모델 사망 페이드 · 내려치기 식): ② 뒤에도 클라이언트 쪽 공통 도우미 한 곳으로 묶을 가치가 있으면 같은 Opus가 같은 라운드에, 아니면 보류 유지.

**멀티블록 (#40~43)** — R6 순서
- ① 41 단독(기계적): `BigBoilerShape`를 `MultiblockShape` 인스턴스(`BIG_BOILER = new MultiblockShape(…)`, 3x3x4 · 가운데 기둥 · 빈칸 `Layout`)로 바꾸고 `BigBoilerShape` 삭제. 호출처(`BigBoilerCasingBlock` · `BigBoilerControllerBlock` · `BigBoilerControllerBlockEntity` · `SanupcraftM8aGameTest`)를 옮긴다.
- ② 40 · 42: 두 제어기 공통으로 `MultiblockShape`(또는 작은 `MultiblockController` 도우미)에 `setCasingsFormed(level, controller, facing, formed, owns)` · `notifyControllers(level, casingPos)` · 힌트 문구 만들기를 하나로. 외벽 블록 둘(큰 보일러 외벽 · 내열 벽돌)의 이웃 변화 → 제어기 알림도 그 도우미를 부른다.
- **43 규칙 결정: 외벽 공유를 허용한다** (금지하면 모양 확인이 이웃 제어기를 찾아야 하고 큰 보일러 외벽에는 소유 기록이 없어 BE를 새로 달아야 함 → 이득보다 변경이 큼). 규칙: 끌 때 ⓐ 벽돌 `link`는 **나를 가리킬 때만** `null`로, ⓑ FORMED를 끈 외벽마다 둘레 제어기에 `notifyControllers`(= `requestCheck`)를 보내 이웃이 **다음 틱에** 다시 켜고 다시 잇게 한다(지금의 `CHECK_INTERVAL`만큼의 공백을 1틱으로). 시험: 벽돌 한 면을 공유하는 탑 둘을 세우고 A의 벽돌 하나를 부숨 → 1~2틱 뒤 B가 여전히 `isFormed()`, 공유 벽돌 FORMED 참 · B에 이어짐.

### 7-8. 2단계 설계 (Opus)
기준: K1~K5 병합 뒤(`b08c16d`). 코드를 다시 읽고 7-7을 구체화한다. 걸음은 다섯: **보스 ① → 보스 ② → 77 ③ → 멀티블록 ① → 멀티블록 ②(+43)**, 걸음마다 `./gradlew build`, 보스 ② · ③은 `runClientGameTest`도, 걸음마다 따로 커밋. 엔티티 id · NBT 키 · 패킷 · 블록 상태 이름은 하나도 안 바뀐다 → 3단계 불필요.

#### 보스 ① `boss/BossCombat` (final, 정적, 동작 불변)
| 멤버 | 옮겨 오는 곳 | 비고 |
|---|---|---|
| `static final float SHIELD_BREAK_SECONDS = 5.0F` | `RustyGuardian` · `FurnaceWarden`의 같은 이름 공개 상수 | 두 상수는 지운다(본 · 클라이언트 · 시험 어디서도 이름으로 안 씀, 시험은 `getSecondsToDisableBlocking()`만). 두 보스의 `getSecondsToDisableBlocking()` 오버라이드는 각자 남기고 이 상수를 돌려준다(거상은 오버라이드가 없어 0 그대로) |
| `static void breakShield(ServerLevel, LivingEntity)` | 두 보스의 `private static breakShield` (본문 동일) | |
| `static double horizontalDistance(Vec3, Vec3)` | 두 보스의 `private static horizontalDistance` | 수호자 1곳 · 파수꾼 2곳 |
| `static final class PlayerCooldown` (`PlayerCooldown(int ticks)`, `boolean tryUse(UUID, long now)`) + `static void warn(ServerLevel, @Nullable Entity target, PlayerCooldown, String key)` | 수호자 `warnedAt` + `warnRust` · 파수꾼 `warnedAt` + `warnHeat` (200틱) · 거상 `shieldMessageAt` + `hurtServer` 안 문구(60틱) | `tryUse` = 지금 두 보스 식 그대로(`removeIf(now − last ≥ ticks)` → `putIfAbsent`가 비었으면 참). 거상 식(`last == null ‖ now − last ≥ 60`이면 보내고 기록)과 결과가 같다(지난 기록은 지워져 비고, 60틱 안이면 남아 거짓 · 기록 안 고침). 차이는 거상 맵이 이제 비워진다는 것뿐(메모리만). `warn`은 대상이 `ServerPlayer`일 때만 `sendOverlayMessage(translatable(key))` |
| `static List<Player> targets(ServerLevel, AABB)` · `targets(ServerLevel, AABB, Predicate<Player> extra)` | 수호자 돌진 · 내려찍기, 파수꾼 분출 · 장판 위치 고르기 · 장판 폭발 · 도약 착지(6곳 `getEntitiesOfClass(Player.class, box, NO_CREATIVE_OR_SPECTATOR)` 또는 같은 뜻의 람다), 거상 `targets` | `NO_CREATIVE_OR_SPECTATOR`는 바이트코드로 확인: 플레이어면 `!isSpectator() && !isCreative()` → 파수꾼 장판 위치 람다와 같다. 거상 `targets(level, range)`는 자기 함수로 남기되 몸통을 `BossCombat.targets(level, box, p -> p.isAlive() && p.distanceTo(this) <= range)`로(살아 있음 · 구 거리는 거상만의 조건이라 다른 보스에 퍼뜨리지 않음 = 동작 불변) |

손대지 않는 것: `hitWithRust` · `hitWithHeat`(갑옷 판정 · 효과가 달라 묶으면 함수 인자만 늘어남), `PatternGoal`(② 몫).

#### 보스 ② `boss/PartyBossMonster` — `public abstract class PartyBossMonster extends Monster implements PartyBoss`
생성자: `protected PartyBossMonster(EntityType<? extends PartyBossMonster> type, Level level, BossEvent.BossBarColor color, int xpReward)` — `super(type, level)` 뒤 `bossEvent = new ServerBossEvent(Mth.createInsecureUUID(random), getDisplayName(), color, NOTCHED_10)`, `this.xpReward = xpReward`, `setPersistenceRequired()`. 세 보스 생성자는 `super(type, level, RED/YELLOW/PURPLE, 50/80/120)` 한 줄.
- **필드 초기화 순서**: 7-7의 "지연 생성"은 필요 없다. 지금도 `bossEvent`는 필드 초기화식이라 `Monster` 생성자가 끝난 뒤 만들어진다. 부모 생성자 몸통(같은 시점: `Monster` 생성자 뒤, 자식 필드 초기화 앞)으로 옮겨도 시점이 같다. `Mob` 생성자가 부르는 가상 함수는 `registerGoals`뿐이고 거기서 `bossEvent` · 자식 필드를 읽지 않는다(`new PatternGoal()`은 바깥 객체 참조만). `getDisplayName()`은 아무 보스도 오버라이드하지 않는다. `random` 소비 순서도 같다(첫 소비가 이 UUID).

부모가 갖는 것 (세 보스에서 글자 그대로 같거나, 차이가 인자 하나):
| 부모 멤버 | 지금 | 비고 |
|---|---|---|
| `private final ServerBossEvent bossEvent`, `private final BossParty party`, `public final BossParty party()` | 세 보스 필드 | 자식의 `party.` 직접 접근은 `party()`로 |
| `public Component bossBarName()` | 셋 | 시험이 씀(상속으로 그대로) |
| `public boolean isEnraged()` | 셋 | 렌더러가 씀 |
| `setCustomName` → `refreshBossBar`, `refreshBossBar()`, `removeWhenFarAway`, `startSeenByPlayer` · `stopSeenByPlayer` | 셋 | |
| `customServerAiStep`: `super` → `bossEvent.setProgress` → 처음 화나면 `phaseAnnounced = true; announcePhase(level)` | 셋의 머리 4줄 | `private boolean phaseAnnounced`는 부모로. 자식은 `super.customServerAiStep(level)` 다음에 패턴 진행(지금과 같은 순서) |
| `protected abstract void announcePhase(ServerLevel)` | `ModSounds.bossPhase(…)` 인자만 다름 | 수호자 CRIT, 파수꾼 FLAME, 거상 `COLOSSUS_RAGE` + REVERSE_PORTAL |
| `actuallyHurt`(기여 기록), `getDamageAfterMagicAbsorb`(상한 보정) | 셋 | |
| `dropCustomDeathLoot`: `super` → `party.giveKeyItem(level, this, keyItem())`, `protected abstract Item keyItem()` | 셋 | 거상은 오버라이드해 `super` 뒤 수정 정리(지금 순서 그대로) |
| `addAdditionalSaveData`: `super` → `party.save` / `readAdditionalSaveData`: `super` → `party.load` → `refreshBossBar()` → `phaseAnnounced = isEnraged()` | 셋 | 거상은 오버라이드해 `super` 뒤 `shield_wave` · `crystals`만. 지금은 거상이 수정 목록을 먼저 읽고 보스 바를 고치지만 서로 무관(보스 바 · 화남은 체력 · 단계만 봄) |
| `makeStuckInBlock` 빈 몸통 | 셋 | |
| `canBeAffected`: 구속 면역 | 셋(거상은 + 공중 부양) | 거상 오버라이드: `!effect.is(LEVITATION) && super.canBeAffected(effect)` |
| `public static final byte EVENT_ATTACK = 4`, `private int attackTick`, `protected void playAttackAnimation(ServerLevel)`(10틱 + 이벤트 방송), `handleEntityEvent`(4 → 10틱, 아니면 `super`), `aiStep`(`super` 뒤 1 감소), `public int getAttackTick()` | 셋(수호자만 이름이 `attackAnimationTick` · `getAttackAnimationTick` · `swingArms`) | 수호자 `swingArms` · 파수꾼 `swingHead` · 거상 `doHurtTarget` 첫 두 줄 → `playAttackAnimation`. 파수꾼은 `handleEntityEvent`에서 `EVENT_ROAR`만 보고 나머지는 `super`, `aiStep`은 `super` 뒤 `roarTick` · 불꽃(지금 순서 그대로). `RustyGuardian.EVENT_ATTACK` · `VoidColossus.EVENT_ATTACK`처럼 자식 이름으로 쓰는 곳(시험 1곳)은 상속된 정적 필드라 그대로 컴파일된다 |
| `protected final class PatternGoal extends Goal` (`MOVE` · `LOOK`, `canUse() = inPattern()`, 매 틱), `protected abstract boolean inPattern()` | 셋의 `private class PatternGoal`(같은 본문, 각자 `pattern != Pattern.NONE`) | 7-7은 ②에 안 넣었지만 셋이 글자 그대로 같고 상태 접근이 추상 함수 하나라 같이 옮긴다. `registerGoals`는 시선 거리(12 / 16)가 달라 각자 남김 |

각 보스에 남는 것: 상수 · `Pattern` 열거 · `CYCLE` · 패턴 필드와 `startPattern` · `tickPattern` · `endPattern` · 패턴 판정 함수 전부, `hurtServer`(셋이 다름: 수호자 낙하 · 익사 + 강철 검, 파수꾼 + 불 면역 + 내열 검, 거상 + 방어막 선검사), `doHurtTarget`, `hitWithRust` · `hitWithHeat`, `getSecondsToDisableBlocking`(둘), `registerGoals`, `createAttributes`, `getDeathSound`, 파수꾼 `roarTick` · 입 위치 · 착지 예측, 거상 동기화 데이터 · 수정 · 애니메이션 값 · `VoidBullet`.

`PartyBoss` 인터페이스는 남긴다(`BossParty.setTier` · `GuardianAltarBlock` · `MobGrinderBlockEntity` · `SanupcraftFinGameTest`의 `<T extends Monster & PartyBoss>`가 씀). `GuardianAltarBlock.isBoss`는 `monster instanceof PartyBossMonster` 한 줄로(같은 세 클래스).

**저장 키 (바뀌지 않음, 위치만 부모로)**: `BossParty.save/load` — `party_size`(int) · `party_summoned`(long) · `party_tier`(int) · `party_damage`(UUID 문자열 → float 맵). 거상만 — `shield_wave`(int) · `crystals`(UUID 목록). 바닐라 키(`CustomName` · `Health` · `attributes` 등)는 `super`가 그대로. 패턴 · 쿨다운 · 경고 시각은 지금도 저장 안 함.

렌더러 · 시험 영향: `RustyGuardianRenderer`의 `getAttackAnimationTick()` → `getAttackTick()`(③에서 어차피 공통 추출로 바뀜). 시험은 공개 API(`party()` · `bossBarName()` · `startPattern` · `tickPattern` · `hitWith*` · `pattern()` · 상수)가 이름 그대로라 고칠 곳 없음.

#### 77 ③ 클라이언트 공통 (이번에 한다)
② 뒤에는 세 렌더러의 `extractRenderState` 앞부분(`getAttackTick` → 남은 틱, `isEnraged`)이 글자 그대로 같아지므로 보류할 이유가 없다. 새 클래스 둘, 고치는 클래스 여덟.
- `client/boss/BossRenderState extends LivingEntityRenderState`: `attackTicksRemaining` · `enraged` + `float attackProgress()`(= 1 − t/10) · `float attackRaise()`(앞 40% 들기 · 다음 30% 내리기 식). 세 `*RenderState`는 이를 상속하고 겹치는 두 필드를 지운다.
- `client/boss/BossRenderer<T extends PartyBossMonster, S extends BossRenderState, M extends EntityModel<S>> extends MobRenderer<T, S, M>`: 생성자 `(context, model, shadow, scale, name, ToDoubleFunction<S> glow)` → 텍스처 `name` · 빛 `name_glow`, `BossGlowLayer` 추가, `getTextureLocation` · `scale` · `extractRenderState`(공통 두 값). 세 렌더러는 `createRenderState`와 자기 값(파수꾼 포효, 거상 당기기 · 보호막)만. 거상의 `VoidShieldLayer`는 `super` 뒤 추가 → 레이어 순서(빛 → 보호막) 그대로.
- `BossGlowLayer`: 사망 페이드(`deathTime > 0`이면 1 − deathTime/20)를 레이어가 먼저 본다 → 세 `glow` 함수 첫 세 줄 삭제(사용처는 세 보스뿐).
- 세 모델 `setupAnim`의 `p` · `raise` 계산 두 줄 → `state.attackProgress()` · `state.attackRaise()`. 각도 상수는 보스마다 달라 그대로.
- 같은 77의 `Math.max(cost, 1)`: `Jetpack.hasThrustSteam(ItemStack pack)`(= `SteamCharge.get(pack) >= Math.max(설정값, 1)`)을 만들고 `Jetpack.thrust` · `JetpackClient` 두 곳이 부른다.

#### 멀티블록 ① #41 (기계적)
- `BigBoilerControllerBlockEntity.SHAPE = new MultiblockShape(3, 4, layout, state -> state.getBlock() instanceof BigBoilerCasingBlock, BigBoilerCasingBlock.FORMED)` — `layout`은 `BigBoilerShape.partAt` 본문 그대로(제어기 (0,0,0), 가운데 기둥 깊이 1의 1 · 2층만 빈칸, 나머지 외벽 33칸). 탑의 `SHAPE`와 같은 자리 · 같은 꼴.
- `BigBoilerShape` 삭제. 호출처: `BigBoilerControllerBlockEntity`(`check` · `positions` · `Problem` 형) · `BigBoilerControllerBlock`(`Problem` · `Part` · `at` · `HEIGHT`) · `BigBoilerCasingBlock`(`HEIGHT`) · 시험 `SanupcraftM8aGameTest`(`BigBoilerShape.Part` → `MultiblockShape.Part`, `BigBoilerShape.casings` → `BigBoilerControllerBlockEntity.SHAPE.casings`). `MultiblockShape` 클래스 문서의 `BigBoilerShape` 언급도 고친다.
- ①에서는 `MultiblockShape` 생성자에 `BooleanProperty formed` 인자만 먼저 넣는다(②가 씀; 탑 `SHAPE`도 `HeatproofBricksBlock.FORMED` 전달). 동작 불변.

#### 멀티블록 ② #40 · #42 · #43 — `MultiblockShape`에 붙는 API
```java
// MultiblockShape 안 (industry 패키지)
public interface Controller { MultiblockShape shape(); void requestCheck(); }          // 두 제어기 BE가 구현
public interface LinkedCasing { @Nullable BlockPos linkedController(); void link(@Nullable BlockPos controller); } // 내열 벽돌 BE가 구현

public void setCasingsFormed(ServerLevel level, BlockPos controller, Direction facing, boolean formed);
public void unform(ServerLevel level, BlockPos controller, Direction facing);   // 43 규칙
public void notifyControllers(ServerLevel level, BlockPos casing);              // 외벽이 부서질 때
public Component problemMessage(Level level, Problem problem, String keyPrefix); // 틀린 칸 문구
```
- `setCasingsFormed`: 외벽 칸마다 `isCasing`이면 `formed` 속성을 `UPDATE_CLIENTS`로 맞추고(다를 때만), BE가 `LinkedCasing`이면 켤 때 `link(controller)`, 끌 때는 **`controller.equals(linkedController())`일 때만** `link(null)`. 큰 보일러 외벽은 BE가 없어 링크 단계가 저절로 빠진다. 두 BE의 `static setCasingsFormed` 복사본 삭제.
- `notifyControllers(level, casing)`: 상자 `casing.offset(−r, −(height−1), −r)` ~ `casing.offset(r, 0, r)`, `r = max(1, depth − 1)`(깊이 3이면 지금 값 2 그대로), 안의 BE가 `Controller`이고 `shape() == this`이면 `requestCheck()`. `BigBoilerCasingBlock` · `HeatproofBricksBlock`의 `static notifyControllers` 복사본 삭제 → 두 블록의 `affectNeighborsAfterRemoval`이 `제어기BE.SHAPE.notifyControllers(level, pos)`.
- `problemMessage`: `translatable(keyPrefix + (expected == CASING ? ".need_casing" : ".need_hollow"), x, y, z, 그 칸 블록 이름)`. 두 제어기 `status`가 `"block.sanupcraft.big_boiler_controller"` · `"block.sanupcraft.furnace_tower_controller"`로 부른다(lang 키 그대로).
- #40 힌트: `BoilerBlock.hint`를 `protected static`으로 열고 `BoilerBlockEntity.STEAM_CAPACITY` 대신 `boiler.steamCapacity()`(보일러는 같은 값, 큰 보일러는 6배)를 본다 → `BigBoilerControllerBlock.hint` 복사본 삭제(큰 보일러 제어기는 `BoilerBlock` 자식이라 바로 부름). 탑 힌트는 키 · 조건이 달라 그대로.
- 두 제어기 BE의 `check` 끝: `if (now) SHAPE.setCasingsFormed(…, true); else if (changed) SHAPE.unform(…);` (지금의 `now || changed` 와 같은 조건, 끄는 쪽만 `unform`). 두 제어기 블록의 `affectNeighborsAfterRemoval`도 `SHAPE.unform(level, pos, facing)`.
- `formed` · `problem` · `checkTimer` 뼈대까지 묶는 것은 하지 않는다(큰 보일러는 `outletStarts`, 탑은 `setChanged`만 — 남는 몫이 4줄이라 이득 작음).

**#43 알고리즘 (외벽 공유 허용)** — `unform(level, controller, facing)`:
1. `setCasingsFormed(level, controller, facing, false)` — 무늬는 공유 외벽까지 끄되(누가 같이 쓰는지 큰 보일러 외벽은 알 길이 없음), 벽돌 링크는 나를 가리킬 때만 푼다.
2. 내 외벽 전체를 덮는 상자(외벽 좌표 최소 ~ 최대를 가로 `r`, 아래로 `height−1`만큼 넓힘; 탑 7×7×5, 보일러 7×7×7) 안에서 `shape() == this`인 다른 제어기(`!pos.equals(controller)`)에 `requestCheck()`.
3. 이웃은 다음 자기 틱(같은 틱 뒤 순서면 그 틱)에 다시 확인 → 아직 맞으면 `setCasingsFormed(true)`로 공유 외벽 무늬를 켜고 벽돌을 자기에게 잇는다. 지금은 이웃의 `CHECK_INTERVAL`(40틱)까지 꺼져 있던 공백이 1~2틱이 된다.
- 끝남: `unform`은 "맞음 → 안 맞음"으로 바뀐 순간과 제어기 제거 때만 부르고, 맞는 쪽 확인은 알림을 보내지 않는다 → 알림은 한 번씩만 퍼지고 되돌아오지 않는다(공유 벽돌이 부서져 A · B가 같이 풀리면 서로 한 번 더 확인하지만 `changed`가 거짓이라 거기서 멈춤).
- 남는 깜빡임: 이웃이 먼저 틱을 돌았으면 공유 외벽이 한 틱 꺼졌다 켜진다(클라이언트 무늬만, 동작 영향 없음 — 증기는 `formed`와 벽돌 링크를 보는데, 링크는 1에서 이웃 것을 건드리지 않음).
- 금지안을 버린 이유는 7-7 그대로(큰 보일러 외벽에 소유 기록이 없어 BE를 새로 달아야 함).

#### 위험
1. 보스 ②: `Monster` 위 계층 변경이라 바닐라 가상 함수 순서가 미묘하게 바뀔 수 있음 → 위 표대로 모든 오버라이드가 "`super` 먼저 → 같은 줄 순서"를 지킨다. 특히 파수꾼 `handleEntityEvent` · `aiStep`, 거상 `readAdditionalSaveData` · `dropCustomDeathLoot`.
2. 보스 ②: 자식 필드가 부모 생성자 뒤에 초기화되므로 부모 생성자에서 자식 상태를 읽으면 안 된다 → 부모 생성자는 `getDisplayName` · `random` · `setPersistenceRequired`만.
3. 77 ③: 렌더 결과는 GameTest가 못 본다 → `runClientGameTest`(M6 · Fin · M11a 보스 장면 스크린숏)로만 확인. 식은 글자 그대로 옮기고 상수는 보스마다 남긴다.
4. 멀티블록 ②: 공유 외벽이 1틱 깜빡일 수 있음(위). 알림 상자 크기는 확인할 때 한 번(탑 245칸 · 보일러 343칸 BE 조회), 풀릴 때만이라 비용 무시.
5. `BoilerBlock.hint`가 가상 `steamCapacity()`를 보게 바뀜 → 일반 보일러 값은 같음(`STEAM_CAPACITY` 반환 확인).

#### 시험 계획
- 계속 통과해야 하는 것: `./gradlew build` 전체(지금 수, K1~K5 추가분 포함). 보스 쪽은 `SanupcraftGameTest` · `FixBoss` · `M2` · `M3` · `M6` · `M10` · `Fin` · `C3` · `Stage2`(12개 파일 중 클라이언트 제외 9개), 클라이언트는 `M6Client` · `FinClient` · `M11aClient`(보스 렌더 · 공격 이벤트 4). 멀티블록은 `M8a`(큰 보일러 3개) · `M8b`(탑).
- 새 시험 (이미 등록된 파일에 메서드로만):
  - ① `FixBoss.bossWarningCooldownPerPlayer`: `BossCombat.PlayerCooldown(200)` — 첫 `tryUse` 참, 같은 시각 같은 사람 거짓, 다른 사람 참, 199틱 뒤 거짓, 200틱 뒤 참.
  - ② `FixBoss.bossSaveKeysStayCompatible`: 세 보스를 인원 2 · 단계 1로 소환해 피해를 조금 준 뒤 `saveWithoutId` → 태그에 `party_size` · `party_summoned` · `party_tier` · `party_damage`(거상은 + `shield_wave` · `crystals`)가 있고, 새 개체에 읽으면 단계 · 인원 · 보스 바 제목(+N)이 같다(파수꾼 왕복은 지금 시험이 없어 이것으로 채움).
  - 멀티블록 ① `M8a.bigBoilerFormsOnlyWithRightShape`에 `SHAPE.casings(...).size() == 33` · `positions(...).size() == 36` 한 줄씩(탑 시험의 23과 같은 꼴).
  - 멀티블록 ② `M8b.sharedWallTowerSurvivesNeighbourBreak`: 동쪽 벽(x 3)을 같이 쓰는 탑 A(제어기 (2,1,1)) · B(제어기 (4,1,1)), 둘 다 북향. 둘 다 `check` 뒤 A만의 벽돌(서쪽 벽 (1,2,2))을 부숨 → 3틱 뒤 A 풀림, A만의 다른 벽돌 무늬 꺼짐, B `isFormed()`, 공유 벽돌 (3,2,2) 무늬 켜짐 · `linkedController()`가 B. 부순 칸은 B 제어기에서 가로 3칸이라 B는 벽돌 알림을 못 받음 → 지금 코드면 B 확인이 37틱 뒤라 실패해야 한다(고치기 전 실패 확인).
  - 멀티블록 ② `M8b.sharedWallTowerSurvivesNeighbourControllerRemoval`: 같은 배치에서 A 제어기를 부숨 → 3틱 뒤 B 그대로 서 있고 공유 벽돌 무늬 · 링크가 B.
  - 시험에서 링크를 보려고 `HeatproofBricksBlockEntity.linkedController()`를 공개한다(`LinkedCasing` 구현을 겸함).
- 걸음마다 새 시험이 고치기 전에 실패하는지(① 쿨다운 · ② 저장 키는 동작 불변이라 해당 없음, 43 둘은 확인) 되돌려 본다.

### 7-9. 비판적 Sonnet 반론 (2단계 설계)
대상: 7-8. 판정자(Opus)가 최종 결정한다. 설계가 맞는 곳은 건너뛰고, 결정마다 반론은 한 번씩만 낸다. 코드는 건드리지 않았다(근거는 `b08c16d` + 7-8 커밋 기준 코드와 디컴파일 소스를 직접 읽은 것).

#### 반론 (결정별 1회)
**(1) ② 부모를 PatternGoal · 공격 틱 · canBeAffected · makeStuckInBlock까지 넓히는 것 — 신뢰도 중**
넓힌 내용이 "글자 그대로 같다"는 주장은 맞다(세 보스 본문을 대조해 확인: `customServerAiStep` 머리 4줄, `handleEntityEvent`, `aiStep`, `PatternGoal`, 효과 면역). 반론은 내용이 아니라 **한 걸음에 묶는 범위와 그 하나의 부모 계약**이다.
- 한 걸음 ②가 바꾸는 것이 보스 3 × (보스 바 · 파티 저장 · 전리품 · 피해 후크 · 패턴 목 · 공격 애니 · 효과 면역)이다. 7-7이 정한 범위(보스 바 · `BossParty` · 저장 · 전리품)에서 PatternGoal(추상 `inPattern`)과 공격 애니(`playAttackAnimation` · 이벤트 · 틱)와 효과 두 개가 더 붙었다. 이 중 하나라도 순서가 틀리면 `runClientGameTest`는 크래시만 잡고 모양은 못 본다(아래 (2) 근거 참고). 한 커밋이면 `git bisect`로 못 가른다.
- `makeStuckInBlock` · `canBeAffected` 빈 몸통의 이유가 원래 **패턴 이동 때문**이다: 수호자 "돌진이 막히지 않게", 파수꾼 "도약이 막히지 않게". 거상은 같은 오버라이드가 있지만 돌진 · 도약 패턴이 없고(끌어당기기 · 일제 사격), 구속 말고 공중 부양 면역까지 따로 가진다. 이걸 `PartyBossMonster`의 기본 동작으로 올리면 "인원 규칙을 따르는 보스 = 거미줄 · 구속 면역"이 클래스 계약이 되어, 네 번째 보스(예: 원거리형)가 말없이 물려받는다. 동작은 지금과 같지만 이름과 뜻이 어긋난다.
- 제안: ②를 두 커밋으로 쪼갠다. **②a** = 7-7 범위(보스 바 · party · 저장 · 전리품 · `announcePhase`/`keyItem` 추상 · `phaseAnnounced`). **②b** = PatternGoal + 공격 틱/이벤트. 효과 면역 둘은 부모에 올리지 말고 각자 두거나, 올리더라도 `protected boolean ignoresSlowdown()` 같은 이름 붙은 훅 하나로 둔다. ②b에서만 `runClientGameTest`를 돌려도 된다.
- 반대로 틀리지 않은 것: 부모 생성자 몸통으로 옮겨도 시점이 같다는 판단, `PatternGoal`의 `protected final class`(자식 `registerGoals`에서 `new PatternGoal()`은 `this`가 바깥 객체라 컴파일된다), 이름 충돌 없음(`Entity`/`LivingEntity`/`Mob`/`Monster`/`PathfinderMob`에 `getAttackTick` · `playAttackAnimation` · `inPattern` · `keyItem` · `announcePhase` · `bossEvent` · `isEnraged` 없음 — grep으로 확인).

**(2) 77 클라이언트 공통을 이번에 하는 것 — 신뢰도 중 (부분 반론)**
- `BossRenderState`(공격 진행 · 들기 식)와 `BossGlowLayer` 사망 페이드 이관은 맞다: 세 모델의 `p` · `raise` 두 줄(앞 40% 들기 / 다음 30% 내리기)과 세 `glow`의 첫 세 줄이 글자 그대로 같음을 확인했다. 단 모델 `body.xRot`가 `p < 0.4F`도 직접 쓰므로 `attackProgress()`는 `p`를 그대로 돌려줘야 하고 `raise`만 대체하면 된다.
- 반론은 **`BossRenderer<T, S, M>` 제네릭 기반 클래스**다. 줄 수를 세면 득이 거의 없다: 렌더러 하나에서 빠지는 것은 텍스처 상수 2 · `addLayer` 1 · `getTextureLocation` 5 · `scale` 4 정도(약 14줄 × 3 = 40줄), 새로 생기는 기반 클래스가 생성자 6인자 · 필드 3 · 오버라이드 3으로 약 40줄에 형 매개변수 셋이 늘어난다. 줄 수로는 순이득이 0 근처인데 렌더 결과를 확인할 수단이 없다.
- 결정적 근거: 클라이언트 시험(`M6Client` · `FinClient` · `M11aClient`)은 `takeScreenshot`만 하고 **이미지를 비교하거나 단언하지 않는다**(grep 확인). 그래서 7-8 위험 3의 "runClientGameTest로 확인"은 "크래시 안 남"까지만 보증한다. 글자 레이어 순서(빛 → 보호막)나 `scale`이 어긋나도 통과한다.
- 제안: (a) `BossRenderState` + 사망 페이드 이관 + 모델 두 줄만 한다. `BossRenderer` 기반 클래스는 보류 유지(7-7의 "가치가 있으면" 단서대로). (b) 이관 전 커밋에서 위 세 시험이 만든 스크린숏을 보관해 두고 이후 바이트/해시로 비교한다(같은 시드 · 시각이라 입자 무작위만 빼면 같다 — 달라지면 사람이 눈으로 확인). 이것이 이 걸음의 유일한 실검증이다. (c) `Jetpack.hasThrustSteam`(`Jetpack.java:86` · `JetpackClient.java:37`의 `Math.max(…, 1)`)은 보스 렌더 걸음과 무관한 증기 장비 코드라 걸음에서 떼어 멀티블록 쪽 소묶음이나 따로 한 줄 커밋으로 한다(보스 걸음에 섞이면 되돌릴 때 같이 떨어진다).

**(3) #43: "끄고 이웃이 다음 틱에 다시 켜게 함" vs "다른 제어기에 이어진 벽돌은 무늬를 유지" — 신뢰도 하**
정직하게 말하면 설계(D)가 틀린 것은 아니다. 알림 상자 · 소유 범위 계산 · `link` 조건(나를 가리킬 때만 null) 모두 맞게 짜여 있고, 두 새 시험은 고치기 전 실패한다(아래 확인). 다만 대안 K가 더 단순할 수 있어 한 번만 짚는다.
- D의 약점은 설계가 스스로 적은 "남는 깜빡임"과 **이웃이 틱을 안 돌리는 경우**다. 이웃 B가 있는 청크가 블록 엔티티를 틱하지 않는 가장자리 상태면 B는 `requestCheck`를 받고도 안 돌아서 공유 벽이 B가 다시 틱할 때까지 꺼져 있다. 또 `link`는 "마지막으로 쓴 쪽"일 뿐이라(두 탑이 40틱마다 번갈아 덮어씀) 링크만 보고 "누가 쓰는 벽돌"을 가릴 수 없다 — 그래서 K도 링크가 아니라 **이웃 제어기가 그 칸을 자기 외벽으로 가졌는지**(`neighbour.isFormed() && neighbour.shape().casings(...).contains(칸)`)로 판정해야 하고, 큰 보일러 외벽은 BE가 없어도 이 방식은 된다.
- K 흐름: `unform`에서 상자 안 이웃 제어기(= 단계 2에서 이미 찾는 것)를 모아 두고, 내 외벽 칸마다 "formed인 이웃이 가진 칸이면 무늬 유지 + `link(이웃)`, 아니면 끄고 링크 해제". `requestCheck` 연쇄도, 깜빡임도, 이웃 틱 순서 의존도 없다. 이웃의 `formed`가 최대 40틱 낡을 수 있으나, 그 이웃이 정말 깨졌다면 같은 부숨이 둘 다에 알림을 보내 한 틱 안에 스스로 `unform`하므로(둘 다 풀리면 마지막 `unform`이 벽을 끈다) 굳을 일은 없다.
- 그래도 D를 고른다면 다음은 바로잡을 것: 알림 상자 · `unform` 상자 안에서 쓰는 `level.getBlockEntity(near)`는 **청크를 강제로 불러온다**(`Level.getBlockEntity` → `getChunkAt(pos)` → `getChunk(x, z, FULL, true)`, 디컴파일 소스 `Level.java:693~699`, `199~205`). 지금 `notifyControllers`의 ±2는 부순 칸이 불러온 청크에 있어 거의 문제 없지만, 새 `unform` 상자는 제어기에서 최대 4칸까지 뻗어 청크 경계 밖 안 불러온 곳을 강제로 불러올 수 있다. `HeatproofBricksBlockEntity.controller()`가 이미 `level.isLoaded(controller)`로 막고 있으니 같은 식으로 `if (!level.isLoaded(near)) continue;`를 `notifyControllers`와 `unform` 상자 순회 양쪽에 넣는 것을 권한다(신뢰도 중, 비용 한 줄).

#### 확인 결과 (질문받은 점검, 반론 없음)
- **부모 생성자에서 부르는 가상 함수**: 안전. `Mob` 생성자는 `registerGoals()`만 부르고(`Mob.java:158`), 거기서 `new PatternGoal()`은 바깥 객체 참조만 잡고 보스 바 · 자식 필드를 읽지 않는다. `Entity` 생성자의 `defineSynchedData`(`Entity.java:332`)는 거상만 오버라이드하며 `static DATA_FLAGS`만 쓴다(자식 정적 초기화는 인스턴스 생성 전에 끝남). `getDisplayName()`은 세 보스 다 오버라이드하지 않는다. `setCustomName`은 어떤 상위 생성자도 부르지 않아 `bossEvent`가 널일 때 `refreshBossBar`가 불리는 길은 지금도 없다. `random` 소비 순서도 같다. 단, 지금 필드 초기화식이 "Monster 생성자 뒤, 자식 필드 앞"이고 부모 생성자 몸통도 정확히 같은 자리라 시점은 같다 — 다만 부모의 `private final BossParty party = new BossParty()` 초기화식은 몸통보다 먼저 돈다는 점만 기억할 것.
- **동기화 데이터 id**: 부모 `PartyBossMonster`가 `EntityDataAccessor`를 정의하지 않으므로 id 변화 없음. 설령 나중에 정의해도 `SynchedEntityData.defineId`는 `ClassTreeIdRegistry`(클래스 계층을 거슬러 올라가 마지막 id + 1)로 id를 주므로 부모 클래스가 항상 먼저 초기화되어 자식 id가 밀릴 뿐이고 저장에는 안 들어간다(NBT에 id 없음). 서버 · 클라이언트가 같은 jar이면 영향 없음.
- **저장 키 호환**: 키 이름 · 자료형이 `BossParty.save/load`와 거상 `shield_wave` · `crystals` 그대로라 호환된다. 읽기 순서 변화(거상: 부모가 `party.load` → `refreshBossBar` → `phaseAnnounced`를 하고 자식이 `shield_wave` · `crystals`)는 서로 독립이라 무해. 다만 새 시험 `bossSaveKeysStayCompatible`은 **자기 저장 → 자기 읽기**라 키 이름이 바뀌어도 양쪽이 같이 바뀌어 통과할 수 있다 — 설계대로 "태그에 키가 있다"를 단언하되, 파수꾼은 아예 **옛 형식 태그를 손으로 만든 `CompoundTag`/`TagValueInput`**(`party_size`=2 · `party_tier`=1 …)으로 읽어 단계 · 인원 · 보스 바 제목을 확인하는 쪽이 진짜 호환 시험이다. (수호자 · 거상의 자기 왕복은 `C3.tierSurvivesSaveAndLoad` · `Fin` 거상 단계 시험이 이미 있다. 파수꾼 왕복이 없는 것은 맞다.)
- **이름이 바뀌는 메서드를 쓰는 기존 시험**: 없다. `getAttackAnimationTick`은 `RustyGuardianRenderer`(클라이언트 한 곳)만 쓴다. 시험이 쓰는 `EVENT_ATTACK`은 `FinClient`의 `VoidColossus.EVENT_ATTACK` 하나뿐이고 상속된 정적 필드라 그대로 컴파일된다(`M11aClient`는 자기 지역 상수 4). `SHIELD_BREAK_SECONDS` 이름 사용처는 보스 본 파일 밖에 없다(시험은 `getSecondsToDisableBlocking()`). `new RustyGuardian(…)` 직접 생성 · 리플렉션 · 접근 확장 · 믹스인 없음.
- **상자 범위 수학**: 맞다. 탑 · 보일러 모두 제어기→외벽 가로 거리는 깊이 방향 0..depth−1, 옆 방향 ±1이라 `r = max(1, depth−1)`(= 2)의 정사각 상자는 방향을 모르는 상태의 상위 집합이다. 세로는 제어기 y = 칸 y − up(0..height−1)이라 `칸.y−(height−1) .. 칸.y`가 맞다(지금 코드 `-2/-3`과 같은 값). `unform` 상자(외벽 최소~최대 ± r, 아래로 height−1)도 "이웃 제어기가 내 외벽 한 칸에서 r 이내"를 덮는다. 또한 `FORMED` 갱신은 같은 블록의 상태 변경(`UPDATE_CLIENTS`만)이라 `affectNeighborsAfterRemoval`(`LevelChunk.java:317`, `blockChanged && (flags & 1)`)을 부르지 않으므로 알림이 되돌아 울리지 않는다.
- **새 시험 실패 예측**: `sharedWallTowerSurvivesNeighbourBreak`는 고치기 전에도 `B.isFormed()`는 참(B 자신의 `formed`는 B가 돌기 전엔 안 바뀜)이고, 실패하는 것은 "공유 벽돌 무늬 켜짐 · 링크가 B" 단언이다 — 단언이 그 둘을 포함하니 설계대로 잡힌다. 구조물은 x 1..5이므로 기본 빈 구조물(8칸)에 들어간다. 시험 1은 `check` 직접 호출 뒤 B의 `checkTimer`가 40이라 3틱 안에 B가 스스로 안 돈다는 전제에 의존한다(순서를 바꿔 B만 먼저 `check`하지 말 것).
- **`BoilerBlock.hint` 합치기(#40)**: `boiler.steamCapacity()`가 일반 보일러에서 `STEAM_CAPACITY`를 돌려주는 것(`BoilerBlockEntity.java:275`)을 확인, 힌트 본문 둘이 글자 그대로 같다. `protected static`이면 `industry` 패키지의 자식(`BigBoilerControllerBlock`)에서 접근된다. `LinkedCasing.link`는 지금 패키지 전용 메서드인데 인터페이스 구현이면 `public`이 되어야 한다(설계에 이미 공개한다고 적혀 있다).

#### 요약 (판정자용)
| 결정 | 반론 | 신뢰도 | 권고 |
|---|---|---|---|
| (1) ② 범위 확대 | 내용은 맞음. 한 커밋 묶음 + 효과 면역을 부모 계약으로 올림이 문제 | 중 | ②a/②b 두 커밋, 효과 둘은 각자 또는 이름 붙은 훅 |
| (2) 77 지금 | 기반 클래스는 줄 수 순이득 0, 클라이언트 시험은 단언 없음 | 중 | 상태 · 레이어 · 모델 식만, `BossRenderer` 보류, 스크린숏 해시 비교, Jetpack 줄은 분리 |
| (3) #43 규칙 | 설계는 유효. 대안 K(이웃이 가진 칸이면 유지)가 깜빡임 · 틱 의존이 없음 | 하 | D를 유지하되 `unform`/`notifyControllers`에 `isLoaded` 가드(중) |

### 7-10. 최종 결정 (2단계, Opus)
| 반론 | 결정 | 이유 |
|---|---|---|
| (1) ②를 ②a / ②b 두 커밋으로 | **받아들임** | 되돌리기 · 원인 좁히기 단위가 맞다. ②a = 7-7 범위(보스 바 · `party` · 저장/읽기 · 전리품 · 피해 후크 · `announcePhase` · `keyItem` · `phaseAnnounced` · `isEnraged` · `bossBarName` · `removeWhenFarAway`), ②b = `PatternGoal` + 공격 틱/이벤트(`EVENT_ATTACK` · `playAttackAnimation` · `getAttackTick`). `runClientGameTest`는 ②b 뒤에 |
| (1) `makeStuckInBlock` · `canBeAffected`를 부모 계약으로 | **받아들임 (각자 둔다)** | 빈 몸통의 이유가 보스마다 다르고(돌진 · 도약이 막히지 않게 / 거상은 공중 부양까지) "인원 규칙 보스"의 뜻이 아니다. 이름 붙은 훅을 만들어도 세 줄 오버라이드보다 짧아지지 않는다 |
| (2) `BossRenderer` 제네릭 기반 클래스 | **받아들임 (보류)** | 순이득이 0에 가깝고 형 매개변수 셋이 는다. ③은 `BossRenderState`(공통 두 필드 + `attackProgress()`는 `p` 그대로 · `attackRaise()`) · `BossGlowLayer` 사망 페이드 · 모델 두 줄 · 렌더러 추출 두 줄을 정적 도우미 하나(`BossRenderState.extractCommon`)로만 |
| (2) 클라이언트 시험에 단언이 없음 → 스크린숏 비교 | **받아들임** | ② · ③ 전 커밋에서 `runClientGameTest` 스크린숏(M11a · Fin · 기존 보스 장면)을 보관하고, ③ 뒤 같은 이름끼리 픽셀 차이를 잰다. 보스는 숨쉬기 · 입자처럼 시각에 따라 움직이므로 바이트 일치는 기대하지 않는다 → 같은 코드로 한 번 더 찍어 잡음 수준을 먼저 재고, ③ 뒤 차이가 그 수준을 넘는 그림만 사람이 눈으로 본다. 결과는 7-11에 |
| (2) `Jetpack.hasThrustSteam`을 보스 걸음에서 분리 | **받아들임** | 증기 장비 코드라 따로 한 커밋(③과 멀티블록 사이) |
| (3) #43: D(끄고 이웃이 다시 켬) vs K(formed 이웃이 가진 칸이면 유지) | **K로 바꿈** | K가 깜빡임 · 이웃 틱 순서 · 틱 안 도는 청크 의존이 없고 `requestCheck` 연쇄도 없다. 판정은 링크가 아니라 "formed인 이웃 제어기의 `casings()`에 그 칸이 있나". 이웃 `formed`가 낡을 수 있지만 그 이웃이 정말 깨졌다면 같은 부숨 알림으로 스스로 `unform`하고 그때 끈다(마지막 `unform`이 정리). 유지한 칸의 벽돌은 그 이웃에 다시 잇는다. `Controller`에 `isFormed()` · `facing()` · (BE의) `getBlockPos()` 추가 |
| (3) `isLoaded` 가드 | **받아들임** | `notifyControllers` · `unform` 상자 순회 모두 `if (!level.isLoaded(near)) continue;` — 안 불러온 청크를 강제로 불러오지 않게 |
| 확인: 저장 키 시험이 자기 왕복이라 약함 → 파수꾼은 옛 형식 태그 손으로 | **받아들임** | `bossSaveKeysStayCompatible`: 세 보스는 저장 태그에 키 이름이 있는지 단언(+ 거상 두 키), 파수꾼은 새 개체를 저장한 태그에서 `party_*`를 지우고 손으로 쓴 옛 형식(`party_size` 2 · `party_summoned` · `party_tier` 1 · `party_damage` {uuid: 50})을 넣어 읽은 뒤 단계 · 인원 · 기여 · 보스 바 +N 확인 |
| 확인: 시험 1은 B를 먼저 `check`하지 말 것 | 받아들임 | 두 탑을 A → B 순으로 `check`, B의 `checkTimer`가 40인 상태에서 3틱 안 판정 |

그 밖의 7-8 내용(① `BossCombat`, 멀티블록 ① · ②의 나머지 API, #40 힌트)은 그대로 간다. 걸음: 보스 ① → ②a → ②b → ③ → 제트팩 → 멀티블록 ① → 멀티블록 ②(+43, K).

### 7-11. 2단계 수정 기록 (Opus)
시작 `e5acd64`(GameTest 297 통과). 걸음마다 `./gradlew build --no-configuration-cache`, 보스 · 렌더 걸음은 `runClientGameTest`도. 줄 수는 본 · 클라이언트 코드(시험 제외).

| 걸음 · 커밋 | 옮긴 것 | 줄 (+/−) | 시험 |
|---|---|---|---|
| 보스 ① `c05de29` | 새 `boss/BossCombat`(`SHIELD_BREAK_SECONDS` · `breakShield` · `horizontalDistance` · `targets(level, box[, extra])` · `PlayerCooldown` + `warn`). 수호자 · 파수꾼의 방패 깨기 · 수평 거리 · 경고 맵 복사본과 두 공개 상수 삭제, 패턴 대상 7곳 → `targets`, 거상 방어막 문구 · `targets`도 같은 도우미로 | +106 / −109 | 298 통과. 새 `FixBoss.bossWarningCooldownPerPlayer` (새 API라 고치기 전 실패 확인은 해당 없음) |
| 보스 ②a `f74619c` | 새 `PartyBossMonster extends Monster implements PartyBoss`: 보스 바(부모 생성자에서 만듦) · `party()` · `bossBarName` · `isEnraged` · `customServerAiStep` 머리(진행도 · 첫 화남 → 추상 `announcePhase`) · `actuallyHurt` · `getDamageAfterMagicAbsorb` · `dropCustomDeathLoot`(추상 `keyItem`) · 저장/읽기 · `setCustomName`/`refreshBossBar` · `removeWhenFarAway` · 보이기/숨기기. 세 보스 생성자는 `super(type, level, 색, 경험치)` 한 줄. 거상은 `shield_wave` · `crystals` 저장과 수정 정리만 오버라이드. `GuardianAltarBlock.isBoss` → `instanceof PartyBossMonster` | +174 / −276 | 299 통과. 새 `FixBoss.bossSaveKeysStayCompatible`(세 보스 태그 키 이름 + 파수꾼 손으로 쓴 옛 형식 태그 읽기 — 동작 불변 확인용이라 고치기 전에도 통과하는 시험) |
| 보스 ②b `a1ccb6f` | `PatternGoal`(추상 `inPattern`) · 공격 동작(`EVENT_ATTACK` · `ATTACK_ANIMATION_TICKS` · `playAttackAnimation` · `handleEntityEvent` · `aiStep` · `getAttackTick`)을 부모로. 수호자 `swingArms` · 파수꾼 `swingHead` · 거상 `doHurtTarget` 첫 두 줄 → `playAttackAnimation`. 파수꾼은 `EVENT_ROAR`만 자기 `handleEntityEvent`에. `RustyGuardianRenderer`: `getAttackAnimationTick` → `getAttackTick`. `makeStuckInBlock` · `canBeAffected`는 7-10대로 각자 둠 | +80 / −125 | 첫 빌드에서 `M9b.grinderKillFeedsExperienceTank` 1건 실패(좀비 경험치 4 < 5, 보스와 무관한 기존 흔들림) → 같은 코드로 다시 돌려 299 통과. `runClientGameTest` 통과 + 스크린숏 비교(아래) |
| ③ 77 `87f97c7` | 새 `client/boss/BossRenderState`(`attackTicksRemaining` · `enraged` · `attackProgress()` · `attackRaise()` · `extractCommon`). 세 `*RenderState`가 상속, 세 모델의 `p` · `raise` 계산 → 상태 함수, 세 렌더러 추출 두 줄 → `extractCommon`, 사망 페이드 → `BossGlowLayer`. `BossRenderer` 기반 클래스는 7-10대로 안 만듦 | +52 / −50 | 299 통과, `runClientGameTest` 통과 + 스크린숏 비교 |
| 제트팩 `c8af18f` | `Jetpack.hasThrustSteam(pack)` — `Jetpack.thrust` · `JetpackClient`의 `Math.max(설정값, 1)` 판정 하나로 | +8 / −5 | 299 통과 |
| 멀티블록 ① `32920b6` | `BigBoilerControllerBlockEntity.SHAPE = new MultiblockShape(3, 4, …)`, `BigBoilerShape` 삭제, 호출처 넷 · `M8a` 옮김, `MultiblockShape` 문서 | +22 / −109 | 299 통과. `M8a.bigBoilerFormsOnlyWithRightShape`에 외벽 33 · 전체 36 단언 |
| 멀티블록 ② `82cdd45` | `MultiblockShape`: 생성자에 `formed` 속성, `form` · `unform`(K안) · `notifyControllers` · `problemMessage`, 인터페이스 `Controller`(`shape` · `getBlockPos` · `facing` · `isFormed` · `requestCheck`) · `LinkedCasing`(`linkedController` · `link`). 두 제어기 BE의 `setCasingsFormed`, 두 외벽 블록의 `notifyControllers`, 두 제어기 블록의 틀린 칸 문구 복사본 삭제. 제어기 제거도 `unform`. 상자 순회는 `isLoaded` 가드. #40: `BoilerBlock.hint`를 `protected static` + `boiler.steamCapacity()`로, 큰 보일러 `hint` 삭제 | +190 / −93 | 새 `M8b.sharedWallTowerSurvivesNeighbourBreak` · `sharedWallTowerSurvivesNeighbourControllerRemoval`: **고치기 전 코드에서 2건 실패 확인**("공유 벽돌 무늬가 꺼짐", 3틱째) → 고친 뒤 301 통과 |

합계: 본 · 클라이언트 29파일 +618 / −753(−135줄), 시험 3파일 +143 / −7. 보스 세 파일 568 · 671 · 622줄 → 401 · 516 · 520줄(+ 부모 191, `BossCombat` 83).

**스크린숏 비교** (`runClientGameTest` 54장, 1280×720 보스 장면은 `M11a` · `Fin`. 지표: 채널 최대 차이가 24를 넘는 픽셀 비율):
- 잡음 수준: 같은 코드(`e5acd64`)로 두 번 찍어 비교. 보스 장면 대부분 0.004~0.2%, 수정이 무작위 자리에 뜨는 `fin-shielded` 9.5% · `fin-shielded-angle` 2.6%.
- ②b 뒤 · ③ 뒤 · 마지막(멀티블록 ② 뒤)을 처음 사진과 비교: 보스 장면 0.004~0.93%, `fin-shielded` 12~13%(잡음과 같은 원인). 잡음보다 큰 `fin-close` 0.93% · `fin-pulling` 0.74%는 나란히 놓고 차이 그림을 봤다 — 윤곽만 한두 픽셀 어긋남(숨쉬기 · 머리 위 조각 회전이 시각에 따라 다름), 자세 · 레이어 · 크기 같음. 공격 자세(`m11a-attack` 0.23% · `fin-attack` 0.25%)도 팔을 든 자세가 같음을 눈으로 확인. 멀티블록 장면(`m8a-industry` 0.94% · `m8b-blocks` 0.40% 등)도 잡음 수준.
- 결론: 렌더 결과 변화 없음.

**미룬 것**: 없음(7-10 범위 모두 처리). 참고 — `M9b.grinderKillFeedsExperienceTank`가 가끔 실패한다(경험치 구슬이 탱크로 덜 들어감, 이번 변경과 무관). 다음 리팩터링 1단계 후보.

### 7-12. 보고 (3차 리팩터링)
| | 건수 |
|---|---|
| 지적 (Haiku 9기) | 79 (+ Opus 판정 중 2 = 81) |
| 실제 문제로 판정 (최종) | 34 (1단계 수정) + 5 (2단계로) |
| 보류 | 3 (51 닻 반경 저장 정책 · 73 제트팩 설정 동기화 · 77은 2단계에서 처리) |
| 오탐 | 39 |
| 1단계 수정 (Sonnet 5기, K1~K5 병렬 worktree) | 34 |
| 2단계 (Opus 직접, 비판적 Sonnet 반론 1회) | 보스 공통 부모 `PartyBossMonster` · 도우미 `BossCombat`, 보스 렌더 공통, 다중 블록 공통 API · 외벽 공유 (#2 · 40~43 · 77) |
| 3단계로 올린 것 | 0 |
| 시험 | GameTest 292 → 301개 통과 (두 번 연속), 클라이언트 시험 통과, 보스 렌더는 스크린숏 비교로 변화 없음 확인 |

주요 실제 버그: 서랍을 부수면 내용물이 쏟아지고 폭발 때 복제됨(#81, 곡괭이 제한도 없앰), 목표판에 깔때기 · 벨트로 넘친 몫이 사라짐(#65), 용광로 탑이 재료를 빼도 증기를 계속 씀(#37), 벌목기가 다시 심을 자리를 저장 안 함(#66), 재고 유지기가 기계에 넣은 재료를 저장 표시 안 함(#14).
흔들리던 시험 정리: 분쇄기 경험치(같은 틱에 모음, 1 이상), 보스 기록(먼저 비움), 수확기(무작위 성장 허용), 경험치 시험 전용 환경.

## 8. 4차 리팩터링 1단계 · 확장 3 (M12 ~ M15) (2026-11-06)

대상: 릴리스 v0.3.0-beta.1(`e9fa2e0`) 이후 바뀐 본 코드 전부 (확장 3: 채굴 · 전투 · 장비 · 리소스, 약 1만 5천 줄).
절차: Haiku 6기 병렬 → Opus 판정 → 비판적 Sonnet 반론 → Opus 최종 결정 → Sonnet n기 수정 (병렬 worktree).
기준 시험: `./gradlew build` (GameTest 409개), `runClientGameTest` (편의 모드 12개 포함 통과, 스크린샷 157장).

### 8-1. 지적 (Haiku 6기)

#### H1 보스 (`boss/` · `boss/gear/` · `boss/phase/`)
1. gear/RustyHammerItem.java:16-26 · FurnaceShieldItem.java:18-37 · VoidBolt.java:23-61 · 중복/설정 불일치 · 중 — 보스는 효과 시간 · 단계를 설정에서 읽는데 보스 장비는 같은 수치를 상수로 따로 가져, 설정을 바꿔도 장비 효과는 그대로.
2. RustyGuardian.java:60-61 vs :488-490 · 주석 불일치 · 하 — 크롬강 검도 1.5배인데 주석은 강철 · 내열만.
3. VoidColossus.java:380-383 · 버그 · 하 — 32칸 안에 아무도 없으면 48칸 대상에게도 탄을 쏨 (문서는 32칸 안만).
4. PartyBossMonster.java:110 vs :92 · VoidColossus.java:273 · 주석 불일치 · 하 — 체력이 정확히 절반이면 2페이즈엔 들어가지만 isEnraged는 거짓 → 거상의 두 번째 수정 · 쿨다운 단축이 빠짐.
5. FurnaceWarden.java:539-552 vs RustyGuardian.java:437-451 · 중복 · 중하 — hitWithHeat · hitWithRust 같은 구조 (부모로).
6. 원 둘레 입자 루프 여러 곳 · 중복 · 하.
7. phase/BossHazard.java:95-97 · 죽은 코드 · 하 — lifetime() 미사용.
8. gear/ModBossGear.java:116-119 · 죽은 코드 · 하 — moduleSlots(stack)은 시험만 씀 (실제는 ModuleSlots 표).
9. VoidColossus.java:441 · 기타 · 하 — 균열 기본 반경이 수정 반경 상수에 묶임.
10. FurnaceWarden.java:55 vs :588 · 주석 불일치 · 하 — 익사 면역이 문서에 없음 (수호자도).

#### H2 클라이언트 (`client/`)
11. 보스 렌더러 3종 · 중복 · 하 — 텍스처 · 렌더 상태 · 추출 · 크기 골격 반복. ※ 3차 2단계에서 BossRenderer 기반 클래스 보류로 결정.
12. BossCrackLayer:37 · BossGlowLayer:40 · EliteAuraLayer:38 · 중복 · 하 — 사망 페이드 20틱 매직 넘버 셋.
13. machine/MachinePartLayers.java:43 · 주석 불일치 · 하 — "톱니 8개"인데 박스 4개.
14. MachineScreen:21 · GuiPanels:37 · AutocrafterScreen:58-61 · 중복 · 하 — 같은 텍스처 경로 상수 둘, 자동 제작대 좌표 하드코딩.
15. GearWorkbenchScreen · AutocrafterScreen · TerminalScreen · 중복 · 하 — 라벨 그리기 · inventoryLabelY 복사.
16. storage/TerminalScreen.java:90-92 · 기타 · 하 — 정렬마다 이름 Component 생성 (내용 바뀔 때만).
(서버 검증: 새 C2S 패킷 없음, 기존 3종 검증 확인)

#### H3 모듈 · 효과 (`module/` · `effect/`)
17. module/RepairStationBlockEntity.java:160-166 · 버그 · 중 — 장비가 비어 있으면 재료 칸이 아무 아이템이나 받아(위 깔때기), 흙 등이 끼면 진짜 재료를 못 넣고 영구 정체.
18. module/GearInheritance.java:41-42 · 기타 · 하 — 닳은 정도를 원시값으로 옮겨 최대 내구도가 다르면 닳은 비율이 바뀜 (의도 확인).
19. module/RepairStationBlockEntity.java:49, 88-94 · 죽은 코드 · 하 — working() · pool() 미사용.
20. module/GearUpgradeRecipes.java:26-27 vs 42-49 · 주석 불일치 · 하 — 클래스 주석은 "첫 장비", 실제는 마법 부여 · 모듈 · 이름 있는 칸 우선.
21. module/RepairStationBlockEntity.java:84, 129-130 · 기타 · 하 — 수리 중 · 증기 받을 때 매 틱 setChanged.

#### H4 장비 · 전투 (`gear/` · `combat/`)
22. combat/SteamSyringeItem.java:60-67 · 버그 · 중 — 주사기 증기가 비용보다 적으면 주사기 잔량을 안 쓰고 통에서 200을 전부 뺀다 (주석은 "주사기 먼저, 모자라면 통"). 통이 없으면 150이 있어도 실패.
23. gear/AreaMining.java:37-48 · 기타 · 하 — FACES를 AFTER에서만 지워, 다른 BEFORE 리스너(쓰러짐 캐기 차단)가 취소하면 낡은 항목이 남는다. ※ 3차 판정 오탐(결과 같음).
24. gear/SteelGear.java:25 · 주석 불일치 · 하 — "내구도 철의 1.5배"인데 실제 2배(500/250).
25. combat/Downed.java:79, 208-215, 288 · 기타 · 하 — LAST_HIT 맵이 쓰러진 채 나가면 안 지워진다.
26. combat/Downed.java:254-260 · 버그 · 하 — 쓰러진 동안 토템을 주우면 출혈사 때 토템이 막아 쓰러짐이 조용히 풀림.
27. combat/FlamethrowerItem.java:142-157 · 기타 · 하 — 물속에서는 증기 · 연료를 쓰고 효과 없음 (주석에 없음).
28. gear/special/HeadLamp.java:66-116 · 기타 · 하 — 청크가 안 불러와졌을 때 빛 블록을 못 지우고 추적만 지워 고아 빛 블록이 남을 수 있다.
29. AreaMining · Downed BEFORE 리스너 등록 순서 의존 · 기타 · 하 — 2번과 같은 묶음.
30. BronzeGear · HeatproofGear · SteelGear · ModSpecialGear · 중복 · 하 — 갑옷 방어 EnumMap 도우미 · armor(name,type) 반복. ※ 3차 판정 오탐(지난번 #64).
31. HeatproofGear · SteelGear · PowerArmor · MinerGear · 중복 · 하 — 네 부위 한 벌 검사 반복.

#### H5 몹 · 위험 요소 · 자원 · 채굴 (`mob/` · `hazard/` · `resource/` · `mining/`)
32. mob/VoidShade.java:83-90 · 버그(성능) · 중 — 등 뒤 순간이동이 실패하면 쿨다운이 안 걸려 매 틱 9번 randomTeleport를 시도한다.
33. mob/EliteMonster.java:127-133 · 주석 불일치 · 하 — 반사 30%가 방어 전 원본 피해 기준.
34. resource/vein/ProspectorItem.java:48-54 · 기타 · 하 — 클라이언트 · 서버의 성공/실패 판정이 어긋나 팔 휘두름 예측이 틀림.
35. mining/blast/RemoteDetonatorItem.java:62-77 · 기타 · 하 — 3번과 같은 패턴.
36. mob/EliteMonster.java:161-169 · ModBlast.java:63-69 · 중복 · 하 — 발전 과제 수여 코드 둘.
37. hazard/ToxicGasPocketFeature.java:26 · 주석 불일치 · 하 — 모닥불 · 영혼불도 불씨로 보는데 주석에 없음.
38. mob/EmberWisp.java:105-131 · 중복 · 하 — 낙하 피해 없음을 두 번 구현.
39. resource/vein/Veins.java:115-129 · 중복 · 하 — rollBonus가 광맥 조회 · 생성을 채굴 1회에 두 번.

#### H6 기계 · 산업 · 생산 · 소리 · 설정 · 실테스트
40. industry/MultiblockShape.java:166-173 · 버그 · 하 — unform이 불러온 제어기만 heldBy로 보아, 안 불러온 청크의 제어기가 쥔 공유 외벽 무늬가 꺼진 채 남을 수 있다 (주석과 차이).
41. playtest/PlaytestCommand.java:172 · 버그 · 하 — 네더 목적지를 현재 좌표 /8 (엔드에서 부르면 엉뚱한 곳). ※ 2차 리팩터링 16번 오탐 판정.
42. playtest/PlaytestCommand.java:215-216 · 중복 · 하 — 석탄 64 두 줄. ※ 이전 판정 오탐.
43. playtest/PlaytestCommand.java:588-589, 163 · 중복 · 하 — END2: apply()의 엔드 발판 뒤 kitEnd2가 다시 이동. ※ 3차 판정 오탐(무해).
44. playtest/PlaytestCommand.java:346-348 · 주석 불일치 · 하 — "외벽 33"인데 40개 줌.
45. sound/ModSounds.java:107-109 · 주석 불일치 · 하 — "40틱에 한 번쯤"인데 분쇄기 30틱.
46. sound/ModSounds.java:39-40 · 주석 불일치 · 하 — 대형 보일러도 STEAM_HISS를 쓴다는 주석 (상속 경로 확인 필요).
47. config/SanupcraftConfig.java:276-280 외 · 중복 · 하 — 기본값 리터럴이 필드 초기값과 OPTIONS 표에 따로 (상수로 묶기).
48. factory/MachineBlockEntity.java:124 · 기타 · 하 — static recipeCache가 이전 서버 RecipeManager를 붙잡음.


### 8-2. Opus 판정
집계(48건, Opus 샘플링 추가 0): **수정 대상 19** · 오탐 24 · 보류 2 · 2단계로 3 · 3단계로 0. 수정 대상 중 2 · 3 · 10 · 20 · 24 · 27 · 33 · 37 · 40 · 44 · 45는 주석만(동작 변화 없음), 7 · 19는 죽은 코드(7-5 R4 기준: 본 · 클라이언트 · 시험 어디서도 안 부르는 함수와 그 때문에 쓰기만 남는 필드를 지운다).

검증 메모:
- 지난 판정과 같은 기준을 그대로 쓴다: `setChanged`는 줄어드는 쪽(아이템 · 유체 소모)만 문제, 받는 쪽 증기는 오탐(F2). 짧은 반복 도우미 · 바닐라와 같은 패턴은 오탐(#62 · #64 기준). 실테스트 명령의 무해한 중복은 오탐(#16 · #64). 보스 렌더러 기반 클래스는 7-10에서 "안 만듦"으로 정했다.
- **#22 주사기**: `inject`는 주사기 증기가 비용보다 적으면 그 잔량은 그대로 두고 통에서 비용 전부(`drain(user, cost)`)를 뺀다. 충전대는 아무 양이나 채우므로 주사기 150 + 통 100(합 250)이면 지금은 `NO_STEAM`. 클래스 문서 "주사기에서 먼저 쓰고, 모자라면 통에서"와 다르다.
- **#17 수리대**: `canPlaceItem(SLOT_MATERIAL)`이 장비가 비어 있으면 무엇이든 참 → 위 깔때기가 흙 · 돌을 넣고, 그 칸은 아래로 안 빠지며(`canTakeItemThroughFace`는 장비 칸만) 다음 재료도 같은 아이템이 아니면 안 들어가 자동화가 멈춘다. 손으로 넣는 길(`useItemOn`)은 이미 "장비가 있고 맞는 재료"만 받으므로, 이 너그러움이 쓰이는 곳은 깔때기뿐이다.
- **#32 그림자**: 6 ~ 16칸 사이에서 `teleportBehind`가 실패하면(좁은 굴 · 대상 뒤가 막힘) `ambushCooldown`이 안 걸려 매 AI 틱 `randomTeleport` 최대 9번(각각 아래로 땅 찾기 + 충돌 검사)을 되풀이한다.
- **#4 절반 체력**: `checkPhaseTwo`는 `체력 <= 최대 × 0.5`, `isEnraged`는 `체력 < 최대 / 2` → 정확히 절반이면 2페이즈(새 패턴 · 알림)는 들어가는데 거상 둘째 수정 · 짧은 쿨다운 · 렌더 화남 · 불러올 때 `phase_two` 기본값이 빠진다(정수 피해가 많아 실제로 정확히 절반에 멈출 수 있다).
- **#26 토템**: 쓰러진 동안 아이템은 못 쓰지만 줍기 · 손 칸 바꾸기는 된다. `bleedOut`의 `BLED_OUT`은 `bypasses_invulnerability`가 아니라 손의 토템이 막는다(토템은 소모). 결과는 "토템을 들고 죽은 것"과 같아 손해 · 복제는 없다 → 규칙 결정이 필요해 보류.

| 번호 | 판정 | 근거 (검증) | 수정 방향 |
|---|---|---|---|
| 1 | **2단계로** | 확인: 녹슨 망치 녹 II 100틱 · 화구 방패 과열 I 100틱 · 공허 탄 침식 I 200틱이 상수, 보스는 `statusEffect.guardianRust*` 등 설정을 읽음. 장비가 보스 설정을 따를지 / 자기 설정 키를 가질지 / 상수로 둘지는 설정 체계(설정 키 · 문서 · 시험) 결정 | 2단계에서 정책 결정 뒤 `boss/gear/` 세 파일 + `SanupcraftConfig` |
| 2 | 수정(주석) | 확인: `hurtServer`가 강철 · 내열 · 크롬강 검 모두 1.5배 | 클래스 문서 "강철 검 · 내열 검으로" → "강철 · 내열 · 크롬강 검으로" |
| 3 | 수정(주석) | 확인: 32칸 안에 아무도 없으면 `getTarget()`(추적 거리 48)에 한 발. 패턴을 헛되이 끝내지 않는 의도된 대체로 보이고 시험도 없음 | `volley` 문서에 "아무도 없으면 지금 대상에게 하나 (32칸 밖이어도)" |
| 4 | 수정 | 위 메모 | `PartyBossMonster.isEnraged()` → `getHealth() <= getMaxHealth() * PHASE_TWO_HEALTH` (`checkPhaseTwo`와 같은 식). **시험 추가**(C4): 체력을 정확히 `최대 × PHASE_TWO_HEALTH`로 → `isEnraged()` 참 (고치기 전 실패) |
| 5 | **2단계로** | 확인: `hitWithHeat` · `hitWithRust`가 방패 깨기 · 강화 배율 · 저항 검사 · 효과 · 경고의 같은 뼈대. 저항 판정 · 효과 · 경고 키만 다름 → `PartyBossMonster`(3차 2단계 부모)로 올리는 설계 | 2단계: 부모에 `hitWithEffect(level, target, damage, pattern, resisted, multiplier, effect…)` 꼴 하나, 두 보스는 위임. 6과 같이 |
| 6 | **2단계로** | 확인: 원 둘레 입자 루프 9곳(수호자 · 파수꾼 · 거상 · `BossHazard`). 네 파일에 걸친 도우미(`BossCombat.ring`) 정리는 5와 같은 걸음 | 5와 한 걸음 |
| 7 | 수정(죽은 코드) | 확인: `BossHazard.lifetime()` 호출 0건(필드는 `tick`이 읽음) | 함수만 삭제 |
| 8 | 오탐 | 시험(`SanupcraftC4GameTest`)이 부름 → R4 기준상 남김. 문서에 "모듈 시스템은 ModuleSlots 표를 본다"가 이미 있음 | |
| 9 | 오탐 | 균열 대체 반경 6 = 수정 반경 6이고 문서("둘레 6칸")와 맞음. 노이즈 | |
| 10 | 수정(주석) | 확인: 파수꾼 · 수호자 모두 `IS_DROWNING` 면역, 문서는 파수꾼 "불 · 낙하"만, 수호자는 없음 | 파수꾼 "불 · 낙하 · 익사 피해를 받지 않는다", 수호자 문서에 "낙하 · 익사 피해를 받지 않는다" |
| 11 | 보류 | 7-10에서 `BossRenderer` 기반 클래스는 안 만들기로 결정(형 매개변수 셋, 순이득 0). 결정 유지 | 보스가 넷째로 늘 때 다시 |
| 12 | 오탐 | 20틱은 바닐라 `LivingEntity` 사망 시간(고정값), 레이어마다 한 줄 | |
| 13 | 오탐 | 십자 판 하나 = 박스 2개 = 톱니 4개, 그것을 45도 돌린 판이 하나 더(`teeth_45`) → 8개. 주석이 맞음 | |
| 14 | 오탐 | 같은 경로 상수 둘 · 툴팁 좌표는 노이즈(화면 하나 안) | |
| 15 | 오탐 | 바닐라 `AbstractContainerScreen` 라벨 패턴, 화면마다 위치가 다름 | |
| 16 | 오탐 | 정렬은 `seenVersion` · 검색어가 바뀔 때만, 항목 수백 개 | |
| 17 | 수정 | 위 메모 | `canPlaceItem(SLOT_MATERIAL)`: `!gear.isEmpty() && gear.isValidRepairItem(stack)` (장비가 없으면 재료를 안 받음, 깔때기는 기다림). 클래스 문서 "깔때기: 위로 재료" 뒤에 "(장비가 올라간 뒤에, 맞는 재료만)". **시험 추가**(Modules): 빈 수리대에 `canPlaceItemThroughFace(SLOT_MATERIAL, 흙, UP)` 거짓 · 강철 주괴도 거짓, 강철 검을 올린 뒤 강철 주괴 참 (고치기 전 실패) |
| 18 | 오탐 | 원시 닳은 값을 옮기는 것은 바닐라 대장장이 작업대(다이아 → 네더라이트, 컴포넌트 통째 복사)와 같다. `to.getMaxDamage() - 1`로 부서짐만 막음 | |
| 19 | 수정(죽은 코드) | 확인: `working()` · `pool()` 호출 0건, `working` 필드는 쓰기만 | 두 함수 · `working` 필드(대입 두 줄 포함) 삭제. `pool` 필드는 유지 |
| 20 | 수정(주석) | 확인: `source`가 마법 부여 · 모듈 · 이름 있는 장비를 먼저 고름(G5) | 클래스 문서 "격자의 첫 장비(내구도가 있는 칸)에서" → "격자의 장비({@link #source}: 옮길 것이 있는 것 먼저, 없으면 첫 장비)에서" |
| 21 | 오탐 | `repairTick`의 `setChanged`는 재료 · 증기 소모 쪽(F2상 필요), `acceptSteam`은 받는 쪽 증기(다른 소비자 여럿도 같음) | |
| 22 | 수정 | 위 메모 | `inject`: 주사기 몫 `have = min(get(syringe), cost)`, 나머지 `need = cost - have`; `need > 0`이면 `SteamCharge.total(user) >= need`일 때만 `drain(user, need)` 후 `take(syringe, have)`, 모자라면 아무것도 안 빼고 `NO_STEAM`. **시험 추가**(M13): 주사기 150 + 통 100 → `REVIVED`, 주사기 0 · 통 50 / 주사기 50 + 통 100 → `NO_STEAM`, 둘 다 그대로 (앞쪽이 고치기 전 실패) |
| 23 | 오탐 | 3차 #58과 같음 | |
| 24 | 수정(주석) | 확인: 갑옷 22 = 철 15의 약 1.5배, 도구 500 = 철 250의 2배 | "내구도 철의 1.5배" → "내구도 갑옷 철의 약 1.5배 · 도구 2배" |
| 25 | 수정 | 메모리는 플레이어 수만큼이라 무해. 대신 3차 #30과 같은 문제: 정적 맵이 월드를 넘어 남아, 다른 월드에서 쓰러지면 `now - last`가 음수 → 시간 깎기가 안 걸림 | `goDown`에서 `LAST_HIT.remove(uuid)`(쓰러질 때마다 새로), `init`에 `ServerLifecycleEvents.SERVER_STARTING`으로 `LAST_HIT.clear()`. 시험 없음 |
| 26 | 보류 | 위 메모: 토템이 소모되어 손해 없음 | 정책: 출혈사를 토템이 막게 둘지(지금) / `bled_out`을 `bypasses_invulnerability`에 넣을지(그러면 `allowDamage` 통과 규칙과도 엮임). 실테스트 뒤 |
| 27 | 수정(주석) | 확인: 물속이면 증기 · 연료를 쓰고 거품만. 설계로 볼 수 있어(바닐라엔 대응 없음) 동작은 그대로 | 클래스 문서에 "물속에서는 증기 · 연료를 쓰고 거품만 난다(불이 안 붙음)" |
| 28 | 오탐 | 옛 칸은 2틱 전 머리 칸 옆이라 늘 불러와져 있고, 나감 · 서버 멈춤은 그 자리에서 바로 지운다(`LEAVE` · `SERVER_STOPPING`). 차원 이동도 다음 조명 틱(2틱 안)에 옛 차원의 칸을 지운다(언로드 전) | |
| 29 | 오탐 | 23과 같음 | |
| 30 | 오탐 | 3차 #64와 같음 | |
| 31 | 오탐 | 네 줄 검사, 규칙이 다름(강철은 윗단계 갑옷 · 증기 부츠 허용) | |
| 32 | 수정 | 위 메모 | `customServerAiStep`: 거리 조건 안에서 `teleportBehind`가 실패하면 `ambushCooldown = 20`(가까울 때와 같은 값). 시험 없음(선택) |
| 33 | 수정(주석) | 확인: `damage`는 방어 · 저항 전 들어온 피해 | "받은 피해의 30%" → "들어온 피해(방어 전)의 30%" (`EliteMonster.hurtServer` 문서 · `EliteAbility` 반사 항목) |
| 34 | 오탐 | 클라이언트는 휘두름만 예측, 결과는 서버가 정함(리벳총 · 공허 지팡이와 같은 바닐라 패턴) | |
| 35 | 오탐 | 34와 같음 | |
| 36 | 오탐 | 발전 과제 수여 4줄 반복, 파일마다 독립(짧은 도우미 기준) | |
| 37 | 수정(주석) | 확인: `isIgnitionSource`는 불(영혼불 포함) · 용암 · 횃불 · 켜진 모닥불 | "불 · 용암 · 횃불 옆 칸" → "불씨({@link ToxicGasBlock#isIgnitionSource}: 불 · 용암 · 횃불 · 켜진 모닥불) 옆 칸" |
| 38 | 오탐 | `checkFallDamage`(떨어진 거리 쌓기)와 `causeFallDamage`(피해)는 뜻이 다르고 바닐라 나는 몹도 같은 두 겹 | |
| 39 | 오탐 | 확률에 맞을 때만 두 번째 조회, 생성은 시드 난수 몇 번 | |
| 40 | 수정(주석) | 확인: 안 불러온 이웃 제어기는 `controllersNear`가 건너뛰어, 그 제어기가 가진 공유 외벽 무늬가 꺼진다. 그 제어기가 불러와져 다음 확인에서 `form`이 다시 켜므로 일시적(3차 #43과 같은 기준) — 동작은 그대로 | `unform` 문서의 "안 불러온 청크는 건드리지 않는다" → "안 불러온 청크의 이웃 제어기는 못 보므로, 그 제어기가 가진 공유 외벽은 잠시 꺼졌다가 그 제어기의 다음 확인에서 다시 켜진다" |
| 41 | 오탐 | 2차 #16과 같음 | |
| 42 | 오탐 | 이전 판정과 같음 | |
| 43 | 오탐 | 3차 #64와 같음 | |
| 44 | 수정(주석) | 확인: 모양은 외벽 33, 주는 것은 40 | "외벽 33 + 제어기로" → "외벽 33(여분 포함 40개) + 제어기로" |
| 45 | 수정(주석) | 확인: 분쇄기만 30틱 | "40틱에 한 번쯤" → "40틱(분쇄기 30틱)에 한 번쯤" |
| 46 | 오탐 | `BigBoilerControllerBlockEntity extends BoilerBlockEntity`, `tick`이 `super.tick` → `STEAM_HISS`. 주석이 맞음 | |
| 47 | 수정(정리) | 확인: `moduleRapidCooldownPercent` 50 · `moduleScatterPellets` 3 · `moduleScatterDamagePercent` 50이 필드 초기값과 `OPTIONS` 기본값에 리터럴로 따로(다른 항목은 다 클래스 상수). 둘이 어긋나면 키가 빠진 JSON의 기본값과 `defaults()`가 달라진다. 켬/끔 플래그(0 · 1)는 그대로 | `ModuleEffects`에 `RAPID_COOLDOWN_PERCENT = 50` · `SCATTER_PELLETS = 3` · `SCATTER_DAMAGE_PERCENT = 50`(기본값 주석), 설정 두 곳이 참조. 시험: `configDefaultsMatchConstants`에 세 줄 추가는 선택(고치기 전에도 통과) |
| 48 | 오탐 | `manager()` 동일성으로 다음 서버 · `/reload`의 첫 조회에서 바뀐다. 붙잡는 것은 옛 관리자 하나 | |

Opus 샘플링(직접 읽음, Haiku 담당 영역): `boss/gear/VoidStaffItem` · `BossGearDrops`, `combat/SteamHammerItem` · `CombatBeaconBlockEntity`, `gear/RivetGunItem`, `gear/special/PowerArmor` · `SpecialEffects` · `SpecialGearTicks` · `DivingHelmet`, `module/ModuleEffects`(야간 투시 거두기 ↔ 잠수 헬멧 · 파워 아머 효과 구별), `hazard/ToxicGasBlock` · `LooseRockBlock`, `mining/blast/BlastCharges`, `mining/QuarryBlockEntity`(`flush`: 위 상자는 같은 청크 열이라 표시됨). 새 실제 문제 없음 → 추가 항목 0.

### 8-3. 수정 묶음 (파일 소유)
세 묶음은 파일이 겹치지 않는다. 각자 자기 git 작업 트리에서 병렬로 고친다. **공용 파일(`lang/*.json` · README · ROADMAP · `src/gametest/resources/fabric.mod.json` · `sounds.json` · 데이터 JSON)은 아무 묶음도 건드리지 않는다** — 새 시험은 이미 등록된 시험 파일에 메서드로만 넣는다. REFACTOR.md도 묶음은 안 고친다(수정 기록은 조정자가 병합 뒤 적는다).
경로 앞 `main/` = `src/main/java/io/github/yhyeop/sanupcraft/`, `test/` = `src/gametest/java/io/github/yhyeop/sanupcraft/test/`. 위치는 줄 번호가 아니라 문구로 찾는다.
모든 묶음 공통 완료 조건: `./gradlew build` 통과(GameTest 409 + 새 시험), 새 시험은 고치기 전에 한 번 실패하는지 확인(되돌려 보기), 주석만 고치는 파일은 코드 변경 금지.

| 묶음 | 소유 파일 (이 묶음만 수정) | 시험 |
|---|---|---|
| **L1 보스 · 몹** (2, 3, 4, 7, 10, 32, 33) | `main/boss/RustyGuardian.java`(주석만), `main/boss/FurnaceWarden.java`(주석만), `main/boss/VoidColossus.java`(주석만), `main/boss/PartyBossMonster.java`, `main/boss/phase/BossHazard.java`, `main/mob/VoidShade.java`, `main/mob/EliteMonster.java`(주석만), `main/mob/EliteAbility.java`(주석만), `test/SanupcraftC4GameTest.java`(시험 추가만) | **필수 1**: 4. build + `runClientGameTest`(렌더가 `isEnraged`를 읽음) |
| **L2 모듈 · 장비 · 설정** (17, 19, 20, 24, 47) | `main/module/RepairStationBlockEntity.java`, `main/module/GearUpgradeRecipes.java`(주석만), `main/module/ModuleEffects.java`(상수 셋 추가만), `main/config/SanupcraftConfig.java`(세 항목만), `main/gear/SteelGear.java`(주석만), `test/SanupcraftModulesGameTest.java`(시험 추가만), `test/SanupcraftBundle1GameTest.java`(47 선택 시 세 줄만) | **필수 1**: 17. 선택: 47 |
| **L3 전투 · 기타 주석** (22, 25, 27, 37, 40, 44, 45) | `main/combat/SteamSyringeItem.java`, `main/combat/Downed.java`, `main/combat/FlamethrowerItem.java`(주석만), `main/hazard/ToxicGasPocketFeature.java`(주석만), `main/industry/MultiblockShape.java`(주석만), `main/playtest/PlaytestCommand.java`(주석만), `main/sound/ModSounds.java`(주석만), `test/SanupcraftM13GameTest.java`(시험 추가만) | **필수 1**: 22 |

주의:
- L1의 4는 `isEnraged` 식만 바꾼다. `checkPhaseTwo` · 불러오기(`phase_two` 기본값)는 그대로 두면 같은 식을 따라간다. 거상 · 수호자 · 파수꾼 본문은 주석 외에 손대지 않는다(2단계 5 · 6과 겹치지 않게).
- L2의 17은 `canPlaceItem`(깔때기 · `canPlaceItemThroughFace`가 부름)만 바꾼다. 손으로 넣는 `RepairStationBlock.useItemOn`은 이미 같은 조건이라 안 고친다. 기존 시험 `repairStationRestoresDurability`는 장비를 먼저 올리므로 그대로 통과해야 한다.
- L2의 47은 기본값을 바꾸지 않는다(50 · 3 · 50 그대로). `RivetGunItem`은 설정 값을 읽으므로 안 고친다.
- L3의 22는 창작 면제(`hasInfiniteMaterials`) · `SELF` · `NOT_DOWNED` 순서를 그대로 둔다. 기존 시험(주사기 600 → 400, 빈 주사기 + 통 250 → 통 50)이 그대로 통과해야 한다.
- L3의 25는 로그아웃 때는 지우지 않아도 된다(쓰러질 때 새로 시작하므로).

2단계로 올린 것 (1단계에서 손대지 않음, L1 병합 뒤):
- **1 보스 장비 효과 ↔ 설정**: 장비 효과 수치를 보스 설정에 묶을지 · 장비용 설정 키를 새로 둘지 · 상수로 둘지(설정 문서 · `configDefaultsMatchConstants` 포함).
- **5 · 6 보스 공격 · 입자 공통화**: `hitWithHeat` · `hitWithRust`를 `PartyBossMonster`로, 원 둘레 입자 9곳을 `BossCombat` 도우미로. 보스 시험이 두 함수를 직접 부르므로 이름은 남기고 위임.

보류 2건(11 렌더러 기반 클래스: 7-10 결정 유지, 26 출혈사 토템 정책)은 이 표로만 넘긴다. 3단계로 올린 것 없음.

### 8-4. 비판적 Sonnet 반론
판정당 1회. 근거는 코드와 디컴파일 소스를 직접 읽은 것 (`LivingEntity.checkTotemDeathProtection` · `ServerPlayerGameMode.useItem` · `MultiPlayerGameMode.useItem` · `Minecraft.startUseItem` · `ServerChunkCache.hasChunk` / `runDistanceManagerUpdates` / `chunkAbsent` · `TicketType` · `ReloadableServerResources`). 코드는 건드리지 않았다. 최종 결정은 Opus.
48건 중 **반론 8건**(R1~R8), 질문받은 지점의 검증 결과 V1~V6. 나머지 판정은 읽어 보고 동의라 건너뛴다. 3단계로 올릴 것이 없다는 판정에도 동의.
※ 요청 문구의 번호와 표 번호가 다르다: "#12/#13 (탐사기 · 기폭기)"는 표의 **34 · 35**, "#21/#23 (머리 조명 · 화염 방사기)"는 **28 · 27**이다. 아래는 표 번호로 쓴다.

#### 반론
| 번호 | 반론 | 근거 | 요청 | 확신도 |
|---|---|---|---|---|
| **28** (오탐) | **오탐이 아니다. 옛 칸이 "늘 불러와져 있다"는 전제가 차원 이동 · 먼 순간이동에서는 틀려서 고아 빛 블록이 실제로 남는다.** | `HeadLamp.remove`는 `level.isLoaded(spot.pos())`가 거짓이면 아무것도 안 하는데, `tick`은 그 뒤에 무조건 `LAMPS.remove(uuid)`를 한다(`HeadLamp.java:65-69, 111-116`) → 추적은 지워지고 블록은 남는다. `isLoaded` = `ServerChunkCache.hasChunk` = `holder.getTicketLevel() > FULL`이면 부재(`chunkAbsent`). 플레이어 칸 티켓(`PLAYER_LOADING` · `PLAYER_SIMULATION`)은 timeout 0이라 플레이어가 떠나면 바로 빠지고, 레벨 갱신은 다음 틱 `ServerChunkCache.tick`의 `runDistanceManagerUpdates`(:321)에서 반영된다. 조명 틱은 `END_SERVER_TICK`의 `tick % 2 == 0`뿐이다(`SpecialGearTicks.java:27`). **시나리오**: 어두운 곳(램프 켜짐)에서 홀수 틱에 네더 포탈을 타거나 `/tp`로 시야 거리 밖으로 간다 → 그 틱의 조명 틱은 건너뛰고, 다음(짝수) 틱은 `ServerChunkCache.tick`이 먼저 돌아 옛 칸이 이미 부재 → `remove`가 건너뛰고 추적만 지워짐. 포탈은 `PORTAL` 티켓을 목적지에만 건다. 포탈 빛(11)이 있어도 램프는 켜진 채다(`isDark`가 자기 조명 몫을 빼서 `block(13) > mine(13)`이 거짓 → `other = 0`). 이동 틱의 홀짝에 따라 절반쯤. 판정 근거 중 맞는 것: 죽음(죽은 플레이어는 목록에 남아 있어 2틱 안에 `want = null`로 지움) · 나감(`LEAVE`로 즉시) · 같은 차원을 걸어서 이동(시야 안)은 안전. | **수정(소)**: (a) `ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD`에서 `HeadLamp.clear(player)` (같은 틱이라 칸이 아직 불러와져 있다) + (b) `remove`가 못 지운 칸은 `PENDING` 집합(차원 + 청크)에 남기고 `ServerChunkEvents.CHUNK_LOAD`에서 그 청크의 칸이면 지움. 시험은 `PENDING`에 직접 넣고 청크 불러오기 훅을 부르는 꼴로 가능. 안 하더라도 클래스 문서의 "지운다" 보증을 "불러온 청크에서만"으로 낮춘다. | 중 |
| **1** (2단계로) | **2단계 논점 자체가 잘못 세워졌다: 장비 효과를 보스 설정에 묶으면 안 된다. 설계로 닫는다.** | 도구 설명이 수치를 박아 놓았다: `rusty_hammer.tooltip` "녹 II (방어 −4)", `furnace_shield.tooltip` "과열 I", `void_staff.tooltip` "공허 침식 I" (`ko_kr.json:904, 907, 910`) → 설정에 묶으면 툴팁이 거짓이 된다. 보스 설정(`statusEffect.guardianRustLevel` 등, `SanupcraftConfig.java:438-449`, 범위 0까지)은 **플레이어가 맞는 난이도**이고 장비 효과는 **플레이어의 무기 균형**이다: 운영자가 보스를 쉽게 하려 `guardianRustTicks = 0`으로 내리면 같은 설정으로 망치의 녹도 꺼진다. 또 보스 방해물(`BossHazard.PUDDLE_RUST_TICKS` · `RIFT_EROSION_TICKS`, 파수꾼 `PILLAR_OVERHEAT_TICKS`)도 이미 설정 없는 상수라 "보스 효과는 전부 설정"도 성립하지 않는다. 수치가 같은 것(100 · 2, 100 · 1, 200 · 1)은 우연이 아니라 의도한 균형일 뿐이다. | 1을 **오탐(설계: 장비 수치는 장비 상수)**으로 닫고 2단계 목록에서 뺀다. 장비 전용 설정 키가 필요해지면 그때 새 항목으로(툴팁이 숫자를 같이 읽게). 우선은 상수 옆에 "보스 설정과 일부러 따로" 한 줄 주석. | 상 |
| **5** (2단계로) | **부모에 `hitWithEffect(…, resisted, multiplier, effect…)`를 올리는 설계는 매개변수만 늘린다. 같은 것은 앞 3줄뿐이다.** | `hitWithRust`(`RustyGuardian.java:437-451`)와 `hitWithHeat`(`FurnaceWarden.java:539-552`)에서 진짜 같은 것은 `if (pattern) { breakShield; damage *= outgoingDamageScale }` 3줄과 `hurtServer(…, 면역 ? damage : damage * 배율)`. 다른 것: 저항 판정 함수, 배율 상수, **수호자만 구속 60틱 II를 더 건다**, 효과 종류 · 설정 키 · 경고 쿨다운 필드 · 경고 키. 하나로 합치면 7~8개 매개변수나 람다가 되고, 시험이 이름으로 부르는 곳이 여럿(`SanupcraftC3GameTest` · `FixBossGameTest` · `M2GameTest` · `StatusEffectGameTest` 등)이라 위임 껍데기가 남는다. 함수 둘이 각 14줄이고 사용처가 둘뿐이라 세 번째 사례도 없다(거상은 `hitWith…`가 없다). 줄이는 양에 비해 보스 세 마리의 공격 경로를 한꺼번에 건드리는 위험(7-9 (1)과 같은 이유)이 크다. | 올린다면 **앞 3줄만** `PartyBossMonster.prepareStrike(level, target, damage, pattern)` (스케일된 피해를 돌려줌)으로, 본문은 각자. 그것도 이득이 작으면 5를 #62 · #64 기준(짧은 반복 도우미)으로 오탐 처리. | 중 |
| **6** (2단계로) | **"9곳"은 입자 원이 아니다. 모양이 달라 도우미 하나로 안 모인다. 2단계 조건(설계 판단)에도 안 맞는다.** | `Math.cos` 쓰는 곳 약 17곳 중 서버 입자 원은 `FurnaceWarden.java:350, 396, 478` · `RustyGuardian.java:289, 397` · `VoidColossus.java:469`(+398은 반지름이 변하는 루프) · `BossHazard.java:171, 177`. 나머지는 입자가 아니라 **위치 계산**(`RustyGuardian.java:359, 381` · `VoidColossus.java:299, 441, 444`)이거나 클라이언트 `addParticle`(`BossHazard.java:191`)이다. 입자 종류 · 개수 · 오프셋 · `DustParticleOptions` · 반지름 규칙이 모두 다르다. 파일 넷에 걸쳐도 서로 의존이 없어 "여러 모듈에 걸친 설계 수준 판단"이 아니다. | 2단계 대신 **1.5단계 보류**: 하려면 `BossCombat.ring(level, particle, center, radius, y, points)` 한 개로 서버 입자 5~6곳만(줄어드는 양 적음). 아니면 오탐(#62 기준). 5와 같은 걸음에 묶지 않는다. | 하 |
| **26** (보류) | **보류가 아니라 "문제 없음 / 문서에 이미 있음"으로 닫을 수 있다.** 질문받은 쓰러짐 잠금 · 효과 잔류는 일어나지 않는다(V1). | V1 참고. 클래스 문서에도 "불사의 토템을 들고 있으면 토템이 먼저다"(`Downed.java:56`)가 이미 있고 `canGoDown`이 `holdsTotem`이면 쓰러짐을 안 거는 것과 같은 규칙이다. 보류 사유의 대안 `bled_out`을 `bypasses_invulnerability`에 넣는 쪽은 오히려 해롭다: 쓰러진 동안 토템을 주운 사람이 토템을 쥐고도 죽는다. | 26을 **오탐(설계, 문서 있음)**으로 닫는다. 논의를 남기고 싶으면 `ROADMAP`에 "쓰러진 동안 토템을 줍는 경우" 한 줄이면 충분. | 중 |
| **17** (수정) | **방향은 맞지만 (a) 고칠 위치가 프로젝트 관례와 다르고 (b) 같은 정체의 다른 입구가 남는다.** | (a) 이 프로젝트는 "깔때기만 막고 손은 안 막는다"를 `canPlaceItemThroughFace`에서 한다: `SanupcraftFixFactoryGameTest.java:58-61` ("이 기계의 재료가 아닌 것은 깔때기로 못 넣는다. 손으로 넣는 것(`canPlaceItem`)은 그대로"). `canPlaceItem`은 `Container` 공개 규약이라 `/item replace` · 다른 모드 · 메뉴 슬롯 `mayPlace`도 부른다. (b) 장비를 깔때기로 빼 간 뒤(`canTakeItemThroughFace`는 장비 칸만) **남은 재료**(예: 강철 주괴 5개)가 그대로 있고 다음에 청동 장비가 오면, 청동 주괴는 칸에 못 들어가고(`canPlaceItem` 거짓) 강철 주괴는 청동 장비에 못 쓰여(`repairTick`의 `isValidRepairItem`) 같은 영구 정체가 난다. 사람이 장비를 먼저 빼야 재료가 나온다(`RepairStationBlock.useWithoutItem`이 장비 칸 우선). | 시험 호출 경로(`canPlaceItemThroughFace(SLOT_MATERIAL, …)`)는 그대로니, 고치는 곳을 `canPlaceItemThroughFace`의 재료 갈래(`!gear.isEmpty() && …`)로 두는 쪽이 관례에 맞다. (b)는 이번 묶음에선 안 고치고 REFACTOR에 "남은 재료 정체: 장비 교체 시 — 2단계 후보"로 기록. | 하 |
| **4** (수정) | **고침 자체는 맞다(V2). 다만 "2페이즈 판정"과 "화남 판정"이 같은 식이 되기만 하고 단조(monotone)가 아니다 — 같은 수정에서 한 줄 더.** | `checkPhaseTwo`는 한 번 들어가면 체력이 차도 2페이즈 그대로이고 `SanupcraftC4GameTest.java:72-75`가 그것을 시험으로 고정하는데, `isEnraged`는 순간 체력 식이다. 보스가 회복하면(재생 · 치유 효과를 막는 코드는 보스에 없다: `canBeAffected`는 구속만 막음 `RustyGuardian.java:465`) 2페이즈 패턴 순환(`CYCLE_TWO`)인 채로 쿨다운 · 방패 파동 · 화남 렌더는 1페이즈로 돌아간다(`FurnaceWarden.java:264` · `VoidColossus.java:273, 370` · `BossRenderState.java:30`). | `isEnraged()` = `isPhaseTwo() \|\| getHealth() <= getMaxHealth() * PHASE_TWO_HEALTH` (`DATA_PHASE_TWO`가 클라이언트에도 동기라 같은 값). 로드 기본값 `getBooleanOr("phase_two", isEnraged())`는 그 시점에 `isPhaseTwo()`가 거짓이라 영향 없음. 시험 C4는 "정확히 절반" + "회복 뒤에도 참" 둘로. | 하 |
| **25** (수정) | **`SERVER_STARTING`의 `LAST_HIT.clear()`는 불필요하다.** | 이미 `clear(player)`가 `LAST_HIT.remove(uuid)`를 한다(`Downed.java:288`). 남는 길은 "쓰러진 채 나감"뿐인데 `goDown`의 `remove` 한 줄이 다음 쓰러짐 때 지운다 → 월드를 넘어가도(싱글 같은 JVM) `now - last < 0`이 될 틈이 없다. 이벤트 등록만 늘고 `Downed.init`의 의존이 커진다. | `goDown`의 `remove` 한 줄만. `SERVER_STARTING` 훅은 뺀다. | 중 |
| **27** (수정 · 주석) | 주석만으로는 물속에서 증기 · 연료가 실제로 타는 것이 남는다. | `emit`이 `ready` 검사 뒤 증기 · 연료를 쓰고(`item.shrink(1)` 포함) 나서 물속 분기에 들어간다(`FlamethrowerItem.java:142-157`). 물속에서 우클릭을 누르고 있으면 2틱마다 증기 4 + 연료 2틱이 나가 증기 2000이 약 100초에 다 쓰인다. 시험 · 문서 없음. 설계로 읽을 수는 있다. | 동의에 가깝다. 코드를 고친다면 물속 분기를 비용 앞으로 옮기고 `stopUsingItem()` + 소리만(새 문구 없이)이지만, 공용 lang 제약 때문에 이번엔 주석 유지로 두고 REFACTOR에 "물속 비용: 결정 필요"라 적자. | 하 |

#### 검증 (반론 없음, 판정이 맞는 것의 확인)
- **V1 · #26 토템**: `bleedOut`은 **`clear(player)`를 먼저** 부른 뒤 `hurtServer(…, BLED_OUT, 1_000_000)`를 부른다(`Downed.java:254-260`). 토템이 막으면(`LivingEntity.checkTotemDeathProtection`: `BYPASSES_INVULNERABILITY`가 아니면 손의 `DEATH_PROTECTION`을 소모하고 체력 1 + 토템 효과) 이미 `DOWNED` 첨부와 `DOWNED` / `GLOWING` 효과가 지워진 상태 → 쓰러진 채 잠기거나 효과가 남는 길이 없다. `bled_out`은 `bypasses_invulnerability` 태그에 없다(데이터에 `damage_type/bled_out.json`만 있고 태그 파일은 없음). 토템의 효과 전부 지우기는 `clear` 뒤라 `ALLOW_EARLY_REMOVE` 보호 훅에도 안 걸린다. 포즈는 다음 틱 바닐라 `updatePlayerPose`가 되돌린다. 게임 모드가 크리에이티브로 바뀌어 피해가 안 들어가도 `clear`가 먼저라 잠기지 않는다. 남는 것은 `LAST_DOWNED` 60초 쿨다운(의도). 토템을 쥔 쓰러진 사람은 "20초 무적 뒤 토템으로 부활"이 되지만 손해 · 복제는 없다.
- **V2 · #4 시험 상호작용**: 기존 시험에 `isEnraged()`를 직접 읽는 곳이 없다(시험 검색 0건; "화남"은 주석 · 소리 시험뿐). `phaseTwoTriggersOnceAtHalfHealth`는 `max × PHASE_TWO_HEALTH`(0.5F)에서 `checkPhaseTwo`만 부르고 로드 시험(예전 저장 40% / 80%)은 경계에서 멀다. `getHealth() <= getMaxHealth() * 0.5F`는 `< getMaxHealth() / 2.0F`와 부동소수 값이 같아서(2의 거듭제곱 곱) 경계에서 `checkPhaseTwo`와 완전히 같다. 클라이언트 스크린샷은 40%라 안 변한다 → `runClientGameTest` 필수 조건은 과하지만 해롭지 않다.
- **V3 · #34 · #35 서버 / 클라이언트 불일치**: 26.3의 클라이언트 `Minecraft.startUseItem`은 `useItem`이 `Success`이고 `swingSource == PREDICTED`면 `player.swing(hand, …, false)` + `itemUsed(hand)`(손 올림 애니메이션)까지만 한다. 서버 `handleUseItem`은 서버가 `Success`이고 `shouldSwing`일 때만 `swingAndResetAttackStrength`로 다른 사람에게 휘두름을 보인다. 탐사기 · 기폭기의 `use`는 클라이언트에서 아이템 상태를 안 바꾼다(증기 · 쿨다운 · 연결은 서버만) → 불일치는 **내 화면의 헛휘두름 + 손 올림**뿐이고 인벤토리 어긋남(고스트 사용)은 없다: `ServerPlayerGameMode.useItem`은 같은 스택 · 개수 · 내구도면 early return(:347-349)하고 구성요소 변화는 일반 슬롯 동기화가 맞춘다. 쿨다운은 서버가 `addCooldown`으로 알리고 그 전의 연타는 서버가 `PASS`로 거른다. 오탐 판정 동의.
- **V4 · #22**: 제안한 순서(`drain` 먼저, 그다음 주사기 `take`)면 `drain`이 모자랄 때 아무것도 빼지 않으므로(`SteamCharge.drain`은 합계 검사 뒤에 뺀다) 부분 소모가 없다. 주사기는 `isCanister`가 아니라 통 합계에 안 들어간다. 기존 시험(600→400, 빈 주사기 + 통 250 → 통 50)과 결과가 같다. 동의.
- **V5 · #40**: 이웃 제어기는 `checkTimer`로 주기 확인(`BigBoilerControllerBlockEntity.java:57` · `FurnaceTowerControllerBlockEntity.java:103`)하고 그때마다 `form`이 공유 외벽을 다시 켠다 → 일시적이라는 판정과 같음. 동의.
- **V6 · #48**: `new RecipeManager`는 `ReloadableServerResources` 생성자(:44)에서 리로드마다 새로 만들어지므로 `manager()` 동일성 비교는 `/reload`에서 맞게 갈린다. 동의.

#### 에스컬레이션 정리
- 2단계: 1은 닫고(R2) 5는 앞 3줄로 축소(R3) 6은 보류(R4)하면 2단계 항목이 거의 남지 않는다. 받아들이면 이번 4차는 사실상 2단계 없이 끝난다.
- 3단계: 없음에 동의.
- 묶음 소유: R1을 받아들이면 `gear/special/HeadLamp.java` · `gear/special/SpecialGearTicks.java`(이벤트 등록) · 시험 파일 하나가 필요하다. 세 묶음 어디에도 없으므로 새 묶음 **L4**로(파일 겹침 없음).

### 8-5. 최종 결정 (Opus)
| 반론 | 결정 | 이유 |
|---|---|---|
| R1 #28 머리 조명 고아 빛 블록 | **받아들임 (a)만, (b)는 보류** | 확인: `tick`은 `remove`가 `isLoaded` 거짓으로 건너뛰어도 `LAMPS.remove`를 하고, 조명 틱은 2틱마다라 차원 이동 직후 옛 칸 티켓이 먼저 빠질 수 있다. 차원 이동은 Fabric `ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL`(26.3 이름, `fabric-entity-events-v1`에서 확인)이 옮긴 그 틱에 불려 옛 칸이 아직 불러와져 있다 → 오탐 → **수정**. 같은 차원 안 시야 밖 `/tp`(홀수 틱)만 남는데, 막으려면 대기 집합 + 청크 불러오기 훅(새 상태)이 필요해 이번엔 문서로 낮추고 보류(28b) |
| R2 #1 보스 장비 ↔ 설정 | **받아들임** | 툴팁(`ko_kr.json`)이 장비 수치를 적고, 보스 설정은 "맞는 쪽 난이도"라 묶으면 운영자가 보스를 쉽게 할 때 장비도 약해진다. 보스 방해물도 이미 상수. 2단계에서 빼고 **수정(주석)**: 세 장비 상수 옆에 "보스 설정과 일부러 따로 (툴팁이 이 수치를 적는다)" |
| R3 #5 `hitWithHeat` · `hitWithRust` | **받아들임 → 오탐** | 같은 것은 앞 3줄뿐이고 수호자만 구속을 더 건다. 사용처 둘 · 각 14줄이라 #62 기준(짧은 반복)으로 닫는다. `prepareStrike`로 3줄만 올리는 것도 보스 공격 경로를 건드리는 위험에 비해 이득이 작다 |
| R4 #6 원 둘레 입자 | **받아들임 → 오탐** | 서버 입자 원은 5~6곳이고 입자 · 개수 · 오프셋 · 반지름 규칙이 다 다르다(나머지는 위치 계산). 설계 판단도 아니다. #62 기준 |
| R5 #26 토템 | **받아들임 → 오탐** | V1 확인: `bleedOut`이 `clear` 먼저라 토템이 막아도 잠김 · 효과 잔류가 없고, 클래스 문서에 "불사의 토템을 들고 있으면 토템이 먼저다"가 이미 있다. `bypasses_invulnerability` 대안은 오히려 해롭다 |
| R6 #17 수리대 | **받아들임 (a), (b)는 보류** | 이 프로젝트는 "깔때기만 막고 손은 그대로"를 `canPlaceItemThroughFace`에서 한다(`SanupcraftFixFactoryGameTest`의 기계 재료 시험). `canPlaceItem`은 그대로 두고 면 경로의 재료 갈래만 바꾼다. 장비를 바꿀 때 남은 재료 정체(b)는 칸 비우기 규칙 결정이 필요해 보류(17b) |
| R7 #4 화남 판정 | **받아들임** | 2페이즈는 한 번 들어가면 유지(시험 고정)인데 화남은 순간 체력이라, 보스가 회복하면 2페이즈 패턴인 채로 쿨다운 · 수정 파동 · 렌더가 1페이즈로 돌아간다. `isPhaseTwo()`는 클라이언트에도 동기 → `isPhaseTwo() \|\| 체력 <= 최대 × PHASE_TWO_HEALTH`. 로드 기본값은 그 시점 `isPhaseTwo()`가 거짓이라 그대로 |
| R8 #25 `LAST_HIT` | **받아들임** | `clear`가 이미 지우고, 남는 길(쓰러진 채 나감)은 `goDown`의 `remove`가 덮는다. `SERVER_STARTING` 훅은 뺀다 |
| R9 #27 물속 화염 방사기 | **받아들임 (주석 유지)** | 동작 변경은 문구 · 설계 결정이 따른다. 주석만 고치고 "물속 비용(쓸지 · 멈출지)"은 보류 메모(27b) |
| V1~V6 | 동의 | V2: `<=` 식은 경계에서 `checkPhaseTwo`와 같다. V3: 34 · 35는 내 화면의 헛휘두름뿐. V4: #22는 `drain` 먼저 → 주사기 `take` 순서로 부분 소모 없음. V5 · V6: 40 · 48 판정 그대로 |

**최종 집계** (48건): **수정 대상 21** (1 · 2 · 3 · 4 · 7 · 10 · 17 · 19 · 20 · 22 · 24 · 25 · 27 · 28 · 32 · 33 · 37 · 40 · 44 · 45 · 47. 그중 1 · 2 · 3 · 10 · 20 · 24 · 27 · 33 · 37 · 40 · 44 · 45는 주석만, 7 · 19는 죽은 코드) · **오탐 26** (5 · 6 · 8 · 9 · 12 · 13 · 14 · 15 · 16 · 18 · 21 · 23 · 26 · 29 · 30 · 31 · 34 · 35 · 36 · 38 · 39 · 41 · 42 · 43 · 46 · 48) · **보류 1** (11) · **2단계로 0** · **3단계로 0**.
보류 메모 (번호 안에 딸린 것, 다음 라운드 후보): **17b** 수리대에서 장비를 바꿀 때 남은 재료가 다음 장비에 안 맞으면 칸이 막힘(칸 비우기 규칙) · **27b** 물속 화염 방사기 비용(쓰지 않거나 멈추게 할지, 문구 필요) · **28b** 같은 차원 시야 밖 순간이동 때 머리 조명 빛 블록이 남을 수 있음(대기 집합 + 청크 불러오기 훅).

### 8-6. 최종 수정 묶음
세 묶음은 파일이 겹치지 않는다. 각자 자기 git 작업 트리에서 병렬로 고친다. **공용 파일(`lang/*.json` · README · ROADMAP · `src/gametest/resources/fabric.mod.json` · `sounds.json` · 데이터 JSON)은 아무 묶음도 건드리지 않는다** — 새 시험은 이미 등록된 시험 파일에 메서드로만 넣는다. REFACTOR.md도 묶음은 안 고친다(수정 기록은 조정자가 병합 뒤 적는다).
경로 앞 `main/` = `src/main/java/io/github/yhyeop/sanupcraft/`, `test/` = `src/gametest/java/io/github/yhyeop/sanupcraft/test/`. 위치는 줄 번호가 아니라 문구로 찾는다.
모든 묶음 공통 완료 조건: `./gradlew build` 통과(GameTest 409 + 새 시험), **새 시험은 고치기 전에 한 번 실패하는지 확인**(수정을 되돌려 돌려 보기), 주석만 고치는 파일은 코드 변경 금지.

| 묶음 | 소유 파일 (이 묶음만 수정) | 시험 |
|---|---|---|
| **L1 보스 · 몹** (1, 2, 3, 4, 7, 10, 32, 33) | `main/boss/PartyBossMonster.java`, `main/boss/phase/BossHazard.java`, `main/boss/RustyGuardian.java`(주석만), `main/boss/FurnaceWarden.java`(주석만), `main/boss/VoidColossus.java`(주석만), `main/boss/gear/RustyHammerItem.java`(주석만), `main/boss/gear/FurnaceShieldItem.java`(주석만), `main/boss/gear/VoidBolt.java`(주석만), `main/mob/VoidShade.java`, `main/mob/EliteMonster.java`(주석만), `main/mob/EliteAbility.java`(주석만), `test/SanupcraftC4GameTest.java`(시험 추가만) | **필수 1** (4). build + `runClientGameTest`(렌더가 `isEnraged`를 읽음) |
| **L2 모듈 · 장비 · 설정** (17, 19, 20, 24, 28, 47) | `main/module/RepairStationBlockEntity.java`, `main/module/GearUpgradeRecipes.java`(주석만), `main/module/ModuleEffects.java`(상수 셋 추가만), `main/config/SanupcraftConfig.java`(세 항목만), `main/gear/SteelGear.java`(주석만), `main/gear/special/HeadLamp.java`(문서만), `main/gear/special/SpecialGearTicks.java`, `test/SanupcraftModulesGameTest.java`(시험 추가만), `test/SanupcraftBundle1GameTest.java`(47 선택 시 세 줄만) | **필수 1** (17). 선택: 47 · 28 |
| **L3 전투 · 기타 주석** (22, 25, 27, 37, 40, 44, 45) | `main/combat/SteamSyringeItem.java`, `main/combat/Downed.java`, `main/combat/FlamethrowerItem.java`(주석만), `main/hazard/ToxicGasPocketFeature.java`(주석만), `main/industry/MultiblockShape.java`(주석만), `main/playtest/PlaytestCommand.java`(주석만), `main/sound/ModSounds.java`(주석만), `test/SanupcraftM13GameTest.java`(시험 추가만) | **필수 1** (22) |

#### L1 걸음
1. **#4** `PartyBossMonster.isEnraged()` 본문 → `return isPhaseTwo() || getHealth() <= getMaxHealth() * PHASE_TWO_HEALTH;`, 문서 "2페이즈이거나 체력이 절반 이하". `checkPhaseTwo` · 불러오기의 `getBooleanOr("phase_two", isEnraged())`는 그대로. **시험**(C4 새 메서드): 보스 소환 → `setHealth(max × PHASE_TWO_HEALTH)` → `isEnraged()` 참(고치기 전 실패) → `checkPhaseTwo(level)` → `setHealth(max)` → `isEnraged()` 참(고치기 전 실패). 세 보스 중 하나(수호자)면 충분
2. **#7** `BossHazard.lifetime()` 함수만 삭제(필드 · `spawn`의 대입 · `tick`의 읽기는 유지)
3. **#32** `VoidShade.customServerAiStep`: 거리 조건(`> AMBUSH_MIN_DISTANCE && < AMBUSH_MAX_DISTANCE`) 안에서 `teleportBehind`가 거짓이면 `ambushCooldown = 20`. 성공 · 가까울 때 분기는 그대로. 시험 없음
4. 주석: **#2** 수호자 클래스 문서 "강철 검 · 내열 검으로" → "강철 · 내열 · 크롬강 검으로". **#10** 수호자 문서에 "낙하 · 익사 피해를 받지 않는다", 파수꾼 "불 · 낙하 피해를" → "불 · 낙하 · 익사 피해를". **#3** `VoidColossus.volley` 문서에 "아무도 없으면 지금 대상에게 하나 (32칸 밖이어도)". **#33** `EliteMonster.hurtServer` 문서 · `EliteAbility` 반사 항목의 "받은 피해" → "들어온 피해(방어 전)". **#1** `RustyHammerItem` · `FurnaceShieldItem` · `VoidBolt`의 효과 상수 위에 "보스 설정(statusEffect.*)과 일부러 따로 둔 장비 수치 (툴팁이 이 수치를 적는다)"

#### L2 걸음
1. **#17** `RepairStationBlockEntity.canPlaceItemThroughFace`: 재료 갈래를 `canPlaceItem(slot, stack)` → `!items.get(SLOT_GEAR).isEmpty() && items.get(SLOT_GEAR).isValidRepairItem(stack)`. `canPlaceItem`은 그대로. 클래스 문서 "깔때기: 위로 재료" → "깔때기: 위로 재료(장비가 올라간 뒤 그 장비의 재료만)". **시험**(Modules 새 메서드): 빈 수리대에 `canPlaceItemThroughFace(SLOT_MATERIAL, 흙, UP)` 거짓 · `(강철 주괴, UP)` 거짓(고치기 전 둘 다 실패) → 강철 검을 `setItem` → `(강철 주괴, UP)` 참 · `(흙, UP)` 거짓. 기존 `repairStationRestoresDurability`는 그대로 통과
2. **#19** `working()` · `pool()` 함수와 `working` 필드(선언 · `repairTick`의 대입 두 줄) 삭제. `pool` 필드 · 저장 키는 유지
3. **#47** `ModuleEffects`에 `RAPID_COOLDOWN_PERCENT = 50` · `SCATTER_PELLETS = 3` · `SCATTER_DAMAGE_PERCENT = 50`(기존 "기본값" 주석 묶음 안에). `SanupcraftConfig`의 세 필드 초기값과 `OPTIONS`의 `rapidCooldownPercent` · `scatterPellets` · `scatterDamagePercent` 기본값이 이 상수를 참조. 값은 바꾸지 않는다. 선택: `configDefaultsMatchConstants`에 세 줄
4. **#28** `SpecialGearTicks.init`에 `ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> HeadLamp.clear(player));` (`net.fabricmc.fabric.api.entity.event.v1`). `HeadLamp`는 코드 그대로, 클래스 문서의 "차원을 옮기거나 … 지운다" 뒤에 "(불러온 청크에서만 지운다. 같은 차원 안 먼 순간이동 직후에는 드물게 빛 블록이 남을 수 있다)". 선택 시험: 모의 플레이어 조명을 켠 뒤 다른 차원으로 `teleport` → 옛 칸이 공기 · `lampOf` null (모의 플레이어 차원 이동이 불안정하면 빼도 됨)
5. 주석: **#20** `GearUpgradeRecipes` 클래스 문서 "격자의 첫 장비(내구도가 있는 칸)에서" → "격자의 장비({@link #source}: 마법 부여 · 모듈 · 이름이 있는 것 먼저, 없으면 첫 장비)에서". **#24** `SteelGear` "내구도 철의 1.5배" → "내구도 갑옷은 철의 약 1.5배 · 도구는 2배"

#### L3 걸음
1. **#22** `SteamSyringeItem.inject`의 비창작 분기: `int have = Math.min(SteamCharge.get(syringe), cost); int need = cost - have; if (need > 0 && !SteamCharge.drain(user, need)) return Result.NO_STEAM; SteamCharge.take(syringe, have);` (`drain`이 먼저라 모자라면 아무것도 안 빠진다). `SELF` · `NOT_DOWNED` · 창작 면제 순서 그대로. 클래스 문서 "모자라면 인벤토리의 증기 통에서 쓴다" → "모자란 몫만 인벤토리의 증기 통에서 쓴다 (합쳐도 모자라면 아무것도 안 쓴다)". **시험**(M13 새 메서드, 쓰러진 사람 + 주사자 + 셋째로 기존 시험과 같은 꼴): 주사기 150 + 통 100 → `REVIVED` · 주사기 0 · 통 50(고치기 전 실패). 다시 쓰러뜨릴 대상으로 주사기 50 + 통 100 → `NO_STEAM` · 주사기 50 · 통 100 그대로. 기존 시험(600 → 400, 빈 주사기 + 통 250 → 통 50) 그대로 통과
2. **#25** `Downed.goDown`에 `LAST_HIT.remove(player.getUUID());`(쓰러질 때마다 새로). `SERVER_STARTING` 훅은 넣지 않는다. 시험 없음
3. 주석: **#27** `FlamethrowerItem` 클래스 문서에 "물속에서는 증기 · 연료를 쓰고 거품만 난다 (불이 안 붙는다)". **#37** `ToxicGasPocketFeature` "불 · 용암 · 횃불 옆 칸" → "불씨({@link ToxicGasBlock#isIgnitionSource}: 불 · 용암 · 횃불 · 켜진 모닥불) 옆 칸". **#40** `MultiblockShape.unform` 문서 "안 불러온 청크는 건드리지 않는다" → "안 불러온 청크의 이웃 제어기는 못 보므로, 그 제어기가 가진 공유 외벽은 잠시 꺼졌다가 그 제어기의 다음 확인에서 다시 켜진다". **#44** `kitIndustry` 문서 "외벽 33 + 제어기로" → "외벽 33(여분 포함 40개) + 제어기로". **#45** `ModSounds.machine` 문서 "40틱에 한 번쯤" → "40틱(분쇄기 30틱)에 한 번쯤"

### 8-7. 2단계 계획
**2단계 없음.** 8-2에서 올린 셋은 반론으로 모두 닫았다: 1은 설계상 장비 상수(주석 한 줄, L1), 5 · 6은 #62 기준 오탐. 3단계도 없음. 다음 라운드 후보는 보류 11(렌더러 기반 클래스, 보스가 넷이 될 때)과 보류 메모 17b · 27b · 28b.
