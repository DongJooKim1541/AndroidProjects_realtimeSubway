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
| 지원 노선 | 2호선 20개 역 |

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
| `selectSubway` | String? | 조회 조건 — `Subways` 상수 이름 |
| `selectDay` | String? | 조회 조건 — `DayOfWeek` 상수 이름 |
| `resultDirection` | String? | 조회 조건 — "1"(상행) / 그 외(하행) |

### 3.2 열거형

**`Subways`** — 2호선 20개 역. `holder`는 표시 이름, `scode`는 API 역 코드.
**`DayOfWeek`** — 평일(1) / 토요일(2) / 일요일(3).

> ⚠️ 두 열거형의 **상수 이름이 DB와 Bundle에 그대로 저장**된다.
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
http://openapi.seoul.go.kr:8088/{KEY}/xml/SearchSTNTimeTableByIDService/1/250/{0+역코드}/{요일}/{방향}
```

- `KEY`는 `local.properties`(`SEOUL_OPENAPI_KEY`) 또는 동명의 환경변수에서 읽어
  `BuildConfig.SEOUL_OPENAPI_KEY`로 주입된다. 소스에는 없다.
- 역 코드 앞의 `0`은 호선 접두사이며 현재 2호선만 지원한다.
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

## 8. 알려진 제약

| 항목 | 내용 |
|------|------|
| 노선 | 2호선 20개 역만 지원 (`Subways` 하드코딩) |
| 자정 경계 | 날짜 없이 시각만 비교하므로 막차 이후 편성은 제외됨 |
| 통신 | API가 HTTPS를 제공하지 않아 평문 HTTP + `usesCleartextTraffic` 사용 |
| Paging | Paging 2 (`PagedListAdapter`) 사용, Paging 3 미적용 |
| View 접근 | `kotlin-android-extensions` synthetic 사용, ViewBinding 미적용 |
| SDK | targetSdk 29 — 동작 변화를 피하려고 유지했으며, 현재 Play Store 배포 기준에는 미달 |
