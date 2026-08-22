# 설계 문서 (SDD)

실시간 지하철 도착 정보 앱 · 2021 모바일 프로그래밍

---

## 1. 개요

서울 열린데이터광장의 지하철 시간표 API를 조회해 선택한 역의 도착 정보를 보여주고,
자주 쓰는 조회 조건을 로컬에 저장해 다시 열람할 수 있게 하는 단일 Activity Android 앱이다.

| 항목 | 값 |
|------|-----|
| 언어 | Kotlin 1.7.20 |
| minSdk / targetSdk / compileSdk | 26 / 29 / 33 |
| 빌드 | AGP 7.4.2 / Gradle 7.6.4 / JDK 17 |
| 아키텍처 | MVVM (ViewModel + LiveData) |
| 화면 구성 | 단일 Activity + 5개 Fragment (Navigation Component) |
| 지원 노선 | 노선도 799역(24노선), 시간표 조회는 1~9호선 |

---

## 2. 아키텍처

```
Fragment (View)
   │  사용자 입력 / LiveData 관찰
   ▼
ViewModel                      ← viewModelScope + Dispatchers.IO
   │  조회 요청 / 결과 필터링
   ▼
SubwayRepository ──────────────► Seoul OpenAPI (XML)
   │
FreshDao (Room) ───────────────► subway.db
```

### 계층별 책임

| 계층 | 클래스 | 책임 |
|------|--------|------|
| View | `SearchFragment`, `ResultFragment`, `SaveFragment`, `SavedTimeTableFragment`, `SplashFragment` | 화면 표시, 입력 수집, LiveData 관찰 |
| ViewModel | `SearchViewModel`, `ResultViewModel`, `SaveViewModel`, `SavedTimeTableViewModel` | 조회 요청, 결과 필터링, 저장 |
| Repository | `SubwayRepository` | API 호출 + XML 파싱 (앱 전체 단일 구현) |
| Network | `SubwayApi` | 요청 URL 생성, 인증키 주입 |
| Data | `FreshDao`, `DatabaseModule` | Room 영속화 |
| Util | `TimeRemaining`, `NavKeys` | 남은 시간 계산, Bundle 키 상수 |

ViewModel은 모두 **Fragment 스코프**(`ViewModelProvider(this)`)다.
조회 상태가 화면을 벗어난 뒤에도 남지 않는다.

---

## 3. 데이터 모델

### 3.1 Room 엔티티

**`SaveItem`** — 저장한 조회 조건 (테이블 `SaveItem`)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `id` | Long? (PK, autoGenerate) | 저장 항목 식별자 |
| `saveTitle` | String | 목록에 표시할 이름 (예: "강남역") |
| `saveSubwayDirection` | String | "상행" / "하행" |
| `saveSubwayDays` | String | 요일 구분 |
| `saveSubwayLineNum` | String | 호선 (예: "02호선") |
| `saveSubwayStationName` | String | 역 이름 |

**`FreshData`** — 조회된 열차 1편 (테이블 **`Subway`**, 클래스명과 테이블명이 다름에 주의)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `id` | Long? (PK) | 행 식별자 |
| `saveId` | Long? (FK → `SaveItem.id`, CASCADE) | 소속 저장 항목 |
| `line_num` | String | 호선 |
| `station_name` | String | 역 이름 |
| `arrivetime` | String | 도착 시각 `HH:mm:ss` |
| `subway_end_name` | String | 종착역 |
| `timeDistance` | String | 남은 시간 표기 (예: "3분 20초") |
| `selectSubway` | String? | 조회 조건 — 역 코드(`0222`). 이전 버전은 `Subways` 상수 이름을 저장했다 |
| `selectDay` | String? | 조회 조건 — `DayOfWeek` 상수 이름 |
| `resultDirection` | String? | 조회 조건 — "1"(상행) / 그 외(하행) |

### 3.2 열거형

**`DayOfWeek`** — 평일(1) / 토요일(2) / 일요일(3).

**`SubwayLine`** — 노선과 노선색, 시간표 지원 여부. 역 목록은 [`StationCatalog`](#75-노선도와-역-목록-2026-08-22-추가).

**`Subways`** — 예전 2호선 20개 역 열거형. 지금은 쓰지 않지만,
Room 과 Bundle 에 `Subway9` 같은 값이 남아 있어 `StationCatalog.resolve` 가 그 키를
읽는 데만 쓴다.

> ⚠️ `DayOfWeek` 와 `Subways` 의 **상수 이름이 DB와 Bundle에 그대로 저장**된다.
> 이름 변경이나 순서 변경은 기존 저장 데이터를 읽지 못하게 만든다.

### 3.3 DAO

```kotlin
suspend fun insertFresh(freshData: List<FreshData>)
suspend fun insertSave(saveItem: SaveItem): Long
fun loadSaveItems(): DataSource.Factory<Int, SaveItem>
suspend fun loadSavedCondition(saveId: Long): FreshData?   // WHERE saveId = :saveId LIMIT 1
fun loadFreshData(saveId: Long): DataSource.Factory<Int, FreshData>
suspend fun deleteSaveData(saveId: Long)
```

목록 조회는 Paging 2의 `DataSource.Factory`, 단건 조회/쓰기는 suspend 함수다.
`allowMainThreadQueries()`는 사용하지 않는다.

---

## 4. 네트워크

### 4.1 요청

```
http://openapi.seoul.go.kr:8088/{KEY}/xml/SearchSTNTimeTableByIDService/1/250/{역코드}/{요일}/{방향}
```

- `KEY`는 `local.properties`(`SEOUL_OPENAPI_KEY`) 또는 동명의 환경변수에서 읽어
  `BuildConfig.SEOUL_OPENAPI_KEY`로 주입된다. 소스에는 없다.
- 역 코드는 호선 접두사를 포함한 완전한 값이다(강남 `0222`). 예전에는 2호선만 다루어
  세 자리 코드에 `0` 을 붙였다.
- 조회 범위 `1~250`은 하루 운행 편성을 모두 담기 위한 상한이다.

### 4.2 응답 파싱

응답은 XML이며 `<row>` 엘리먼트 하나가 열차 1편이다.
`SubwayRepository`가 `LINE_NUM`, `STATION_NM`, `ARRIVETIME`, `SUBWAYENAME` 네 태그를 읽어
`FreshData`로 변환한다.

```kotlin
fun loadTimeTable(subwayName: String, dayName: String, direction: String): List<FreshData>
```

블로킹 호출이므로 반드시 IO 디스패처에서 실행한다.

---

## 5. 시간 계산

`TimeRemaining.until(arriveTime)`이 현재 시각과 도착 시각(`HH:mm:ss`)의 차이를
`java.time.Duration`으로 돌려준다. 파싱 실패 시 `null`이다.

`List<FreshData>.upcoming(limit)`는 아직 지나지 않은 편성만 남기고
`timeDistance`에 "3분 20초" / "1시간 3분 20초" 형태를 채운 뒤 앞에서 `limit`개를 취한다.

- 결과 화면: `limit = 2`
- 시간표 "곧 도착": `limit = 1`

**제약**: 날짜 정보 없이 시각만 비교하므로, 자정을 넘겨 운행하는 편성(현재 23:50, 도착 00:15)은
음수가 되어 제외된다.

---

## 6. 화면 흐름

```
SplashFragment ──(3초)──► SearchFragment
                              │
                   ┌──────────┴───────────┐
                   ▼                      ▼
            ResultFragment          SaveFragment
             (검색 결과)            (저장 항목 열람)
                                          │
                                          ▼
                                SavedTimeTableFragment
```

### 6.1 SearchFragment

역·요일을 다이얼로그로 고르고 방향은 라디오 버튼으로 선택한다.
두 값이 모두 선택되어야 검색 버튼이 활성 색상으로 바뀐다.
하단에는 저장된 조회 조건 목록(`SearchAdapter`)이 표시되고, 삭제 버튼은
`SearchViewModel.delete()`로 위임된다.

### 6.2 ResultFragment

전달받은 조건으로 `ResultViewModel.load()`를 호출하고, 결과 2편을 목록에 표시한다.
FAB를 누르면 조회 조건과 결과가 Room에 저장된다.
조회 실패 시 `loadFailed`가 관찰되어 안내 문구를 띄운다.

### 6.3 SaveFragment

`SAVE_ID`로 저장된 열차 목록(Paging)과 저장 당시 조건을 불러온다.
시간표 화면 이동, 네이버 지도 버스/출구 검색 연결, 새로고침을 제공한다.

### 6.4 SavedTimeTableFragment

`TimeTableFilter`(ALL / FIRST / LAST / UPCOMING) 네 버튼으로 같은 조회 결과를 다르게 추려 보여준다.

---

## 7. 오류 처리

각 ViewModel은 `CoroutineExceptionHandler`로 예외를 잡아 로그를 남기고 빈 목록을 게시한다.
`ResultViewModel`은 추가로 `loadFailed` LiveData를 노출해 화면이 로딩 상태에 머무르지 않게 한다.

---

## 7.5 노선도와 역 목록 (2026-08-22 추가)

### 역 카탈로그

`StationCatalog` 는 두 API 응답을 합친 정적 표다(799역 / 24노선).

| 출처 | 얻는 것 |
|------|---------|
| `SearchSTNBySubwayLineInfo` | 노선(`LINE_NUM`), 역코드(`STATION_CD`), 역명, 역번호(`FR_CODE`) |
| `subwayStationMaster` | 역코드(`BLDN_ID`)별 위경도(`LAT`/`LOT`) — 768역 |

노선 내 순서는 `FR_CODE` 를 자연 정렬한 값이다. 지선(예: 2호선 성수지선 `234-1`)은
본선 뒤에 붙는다.

`resolve(key)` 는 역 코드와 **이전 버전이 저장한 `Subways` 상수 이름**을 모두 받는다.
Room 과 Bundle 에 `Subway9` 같은 값이 남아 있어 그 데이터를 계속 읽어야 한다.

### 노선도 (`SubwayNetworkMapView`)

역의 실제 위경도를 찍어 그린다. 공식 노선도는 사람이 배치한 도안이고 그 좌표는 공개되지
않으므로 재현하지 않는다.

| 처리 | 이유 |
|------|------|
| 경도에 `cos(중간 위도)` 를 곱한다 | 위도 1도와 경도 1도의 실제 길이가 달라 그냥 쓰면 가로로 늘어난다 |
| 연속한 두 역이 6km 를 넘으면 잇지 않는다 | 본선 끝에서 지선 시작으로 건너뛰는 구간에 없는 선로가 그려진다 |
| 첫 역과 마지막 역이 2km 안이면 이어 붙인다 | 2호선 순환선의 고리를 닫는다 |
| 역 이름은 자리 겹침을 검사해 건너뛴다 | 도심은 역이 촘촘해 전부 쓰면 글자가 서로 덮인다 |
| 환승역은 역명이 같은 역이 여러 노선에 있는지로 판단 | 응답에 환승 항목이 없다. 120곳이 잡힌다 |
| 확대·이동을 뷰가 직접 처리 | 스크롤 뷰로 감싸면 두 손가락 확대를 스크롤 뷰가 가로챈다 |

터치도 직접 처리한다. `GestureDetector.SimpleOnGestureListener` 의 인자 널 허용 여부가
SDK 버전마다 달라(`onScroll` 의 `e1` 이 API 34 에서 nullable) 오버라이드가 깨진다.

## 7.6 남은 시간 갱신

`ResultViewModel` 과 `SavedTimeTableViewModel` 은 조회한 시간표를 들고 있다가 1초마다
남은 시간을 다시 계산해 내보낸다. 조회 시점에 한 번만 계산하면 화면을 열어 둔 채로
값이 멈추고, 지나간 열차가 목록에 남는다.

- `SavedTimeTableViewModel` 은 "곧 도착"에서만 갱신한다. 전체/첫차/막차는 값이 바뀌지
  않으므로, 갱신하면 239행 목록을 1초마다 헛되이 다시 그린다.
- 어댑터는 목록 구성이 같으면 `notifyItemRangeChanged` 와 payload 로 시간 표시만 바꾼다.
- **부작용**: 화면이 idle 상태가 되지 않아 `uiautomator dump` 가 실패한다. 이 화면의
  검증은 스크린샷으로 한다.

## 7.7 24시 이후 표기

이 API 는 자정을 넘긴 편성을 `24:54:00` 처럼 24시 이상으로 준다(잠실 평일 상행 239편
중 4편). `LocalTime` 은 0~23 만 받으므로 24 이상은 24 를 빼고 자정 이후로 환산한다.

어느 날의 00:54 인지는 현재 시각으로 정한다. 그대로 계산해 음수면 하루를 더한다.

| 지금 | `24:21` 해석 | 남은 시간 |
|------|--------------|-----------|
| 23:00 | 다음 날 00:21 | 1시간 21분 |
| 00:20 | 오늘 00:21 | 1분 |

일반 표기(05:40 등)가 지났으면 음수로 남겨 호출부에서 걸러지게 한다. "곧 도착"은
**남은 시간이 짧은 순**으로 고른다. 응답이 시각 순이라 24시대 편성이 목록 끝에 붙고,
앞에서부터 고르면 1분 뒤 열차 대신 5시간 뒤 열차가 잡힌다.

## 8. 알려진 제약

| 항목 | 내용 |
|------|------|
| 시간표 | 1~9호선만. 나머지 노선은 API 가 데이터를 주지 않는다 |
| 자정 경계 | 날짜 없이 시각만 비교하므로 막차 이후 편성은 제외됨 |
| 통신 | API가 HTTPS를 제공하지 않아 평문 HTTP + `usesCleartextTraffic` 사용 |
| Paging | Paging 2 (`PagedListAdapter`) 사용, Paging 3 미적용 |
| View 접근 | `kotlin-android-extensions` synthetic 사용, ViewBinding 미적용 |
| SDK | targetSdk 29 — 동작 변화를 피하려고 유지했으며, 현재 Play Store 배포 기준에는 미달 |
