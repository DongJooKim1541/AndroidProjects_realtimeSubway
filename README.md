# 실시간 지하철 도착 정보 앱

<img src="./docs/images/icon.jpg" width="20%">

자주 이용하는 지하철역을 저장해 두고 도착 시간표, 첫차/막차, 주변 버스 정류장과 출구 정보를 빠르게 확인하는 Android 앱입니다.

- **과목**: 모바일 프로그래밍 (2021)
- **언어**: Kotlin
- **데이터**: [서울 열린데이터광장](https://data.seoul.go.kr) 지하철 시간표 API (`SearchSTNTimeTableByIDService`)
- **지원 범위**: 2호선 20개 역 (강변 ~ 문래)

## 기능

| 기능 | 설명 |
|------|------|
| 역 검색 | 역·요일(평일/토요일/일요일)·방향(상행/하행)을 선택해 조회 |
| 도착 정보 | 지금 시각 기준으로 다가오는 열차 2편과 남은 시간 표시 |
| 저장 | 조회 조건과 결과를 로컬 DB에 저장, 목록에서 다시 열람 |
| 시간표 | 저장한 역의 전체 / 첫차 / 곧 도착 / 막차 시간표 전환 |
| 주변 정보 | 네이버 지도로 역 주변 버스 노선·출구 정보 연결 |

## 기술 스택

| 영역 | 사용 기술 |
|------|-----------|
| 언어 | Kotlin 1.3.71 |
| 아키텍처 | MVVM (ViewModel + LiveData) |
| 비동기 | Kotlin Coroutines (`viewModelScope`, `Dispatchers.IO`) |
| 로컬 DB | Room 2.2.5 (suspend DAO) |
| 목록 | RecyclerView + Paging 2 |
| 화면 전환 | Navigation Component 2.2.2 |
| 네트워크 | OkHttp `HttpUrl` + `DocumentBuilderFactory` XML 파싱 |

## 구조

```
app/src/main/java/com/example/gc_last/
├── data/        SubwayRepository — API 조회 및 XML 파싱 단일 진입점
├── database/    DatabaseModule — Room 데이터베이스
├── model/       FreshData, SaveItem, FreshDao, Subways, DayOfWeek
├── network/     SubwayApi — 서울 열린데이터광장 URL 생성
├── ui/          FreshAdapter, FreshPagedAdapter — 공용 목록 어댑터
├── util/        TimeRemaining(남은 시간 계산), NavKeys(화면 간 Bundle 키)
├── main/        MainActivity, SplashFragment
├── search/      SearchFragment/ViewModel/Adapter — 검색 및 저장 목록
├── result/      ResultFragment/ViewModel — 검색 결과
└── local/       SaveFragment/ViewModel, SavedTimeTable* — 저장된 역 상세·시간표
```

데이터 흐름은 `Fragment → ViewModel → SubwayRepository → (Seoul OpenAPI | Room)` 한 방향입니다.
네트워크 조회와 XML 파싱은 `SubwayRepository`에만 존재하고, ViewModel은 결과를 어떤 기준으로
추릴지(전체 / 첫차 / 막차 / 다가오는 N편)만 결정합니다.

## 빌드

### 요구사항

- Android Studio 4.0 이상
- Android SDK 29, Build Tools 29.0.3
- JDK 8

### API 키 설정 (필수)

인증키는 저장소에 포함하지 않습니다. [서울 열린데이터광장](https://data.seoul.go.kr/together/mypage/actKey.do)에서
키를 발급받아 `local.properties`에 넣으세요.

```properties
SEOUL_OPENAPI_KEY=발급받은_키
```

`SEOUL_OPENAPI_KEY` 환경변수로도 읽습니다. 키가 없으면 빌드는 되지만 조회 시 오류가 납니다.

### 실행

```bash
git clone https://github.com/DongJooKim1541/AndroidProjects_realtimeSubway.git
cd AndroidProjects_realtimeSubway
./gradlew assembleDebug
```

## 문서

- [설계 문서 (SDD)](docs/SDD.md) — 아키텍처, 데이터 모델, 화면 흐름
- [테스트 케이스 (TC)](docs/TC.md) — 수동 검증 시나리오

## 라이선스

[LICENSE](LICENSE) 참고.
