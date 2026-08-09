# 소프트웨어 설계 문서 (SDD)
## 실시간 지하철 도착 정보 앱

---

## 1. 개요

실시간 지하철 도착 정보 앱은 자주 이용하는 역을 등록하여 역에 대한 실시간 도착 정보, 시간표, 근처 버스 정류장, 출구 정보 등을 빠르게 제공하는 모바일 애플리케이션입니다.

**프로젝트**: 모바일 프로그래밍 수업 기말고사  
**년도**: 2021  
**언어**: Kotlin  
**아키텍처**: MVVM (Model-View-ViewModel)

---

## 2. 시스템 아키텍처

```
View Layer (Fragment)
    ↓
ViewModel Layer
    ↓
Model Layer (Database, Network)
    ↓
Data Sources (Room, API)
```

### 2.1 레이어 구성

| 레이어 | 역할 |
|--------|------|
| View (Fragment) | UI 표시 및 사용자 상호작용 |
| ViewModel | 비즈니스 로직 및 데이터 관리 |
| Repository | 데이터 소스 추상화 |
| Database (Room) | 로컬 지하철 역/시간표 저장 |
| Network (API) | 실시간 도착 정보 조회 |

---

## 3. 주요 컴포넌트

### 3.1 Database Layer

**DatabaseModule.kt**
- Room Database 설정
- DAO (Data Access Object) 정의
- 데이터 모델: FreshData, DayOfWeek, Subways

**Models**
- `FreshData`: 지하철 역 정보 및 시간표 데이터
- `Subways`: 지하철 노선 정보
- `DayOfWeek`: 요일별 시간표 데이터

### 3.2 Network Layer

**NetworkModule.kt**
- Retrofit + OkHttp 설정
- API 엔드포인트 정의
- 실시간 도착 정보 조회 API

**TimeCalculate.kt**
- 실시간 도착 시간 계산 유틸리티

### 3.3 UI Components

**Fragments**
- **SplashFragment**: 앱 시작 화면
- **SearchFragment**: 역 검색 및 조회
- **ResultFragment**: 검색 결과 및 실시간 도착 정보 표시
- **SaveFragment**: 즐겨찾기 역 추가
- **SavedTimeTableFragment**: 저장된 역의 시간표 조회

**Adapters**
- `SearchAdapter`: 검색 결과 목록
- `ResultAdapter`: 도착 정보 목록
- `SavedTimeTableAdapter`: 저장된 시간표 목록
- `Save_adapter`: 즐겨찾기 역 목록

### 3.4 ViewModel Layer

**ViewModels**
- `SaveViewModel`: 즐겨찾기 역 관리
- `ResultViewModel`: 실시간 도착 정보 관리
- `SavedTimeTableViewModel`: 저장된 시간표 조회

---

## 4. 데이터 흐름

### 4.1 역 검색 흐름

```
User Input (SearchFragment)
    ↓
SearchViewModel (검색 쿼리 처리)
    ↓
DatabaseModule (역 정보 조회)
    ↓
SearchAdapter (결과 표시)
```

### 4.2 실시간 도착 정보 조회

```
User Select Station (SearchAdapter)
    ↓
ResultFragment → ResultViewModel
    ↓
NetworkModule (API 호출)
    ↓
TimeCalculate (도착 시간 계산)
    ↓
ResultAdapter (정보 표시)
```

### 4.3 역 저장/조회

```
User Select SaveFragment
    ↓
SaveViewModel (저장된 역 목록 조회)
    ↓
DatabaseModule (저장된 역 정보)
    ↓
Save_adapter (목록 표시)
```

---

## 5. 기술 스택

| 기술 | 버전 | 용도 |
|------|------|------|
| Kotlin | 1.4.x | 언어 |
| Jetpack Lifecycle | 2.3.0 | ViewModel 관리 |
| Room | 2.2.5 | 로컬 데이터베이스 |
| Retrofit | 2.4.0 | HTTP 클라이언트 |
| OkHttp | 4.7.2 | 네트워크 통신 |
| GSON | 2.8.2 | JSON 파싱 |
| Moshi | 1.9.3 | JSON 변환 |
| Navigation | 2.2.2 | Fragment 네비게이션 |
| RecyclerView | 1.1.0 | 목록 표시 |
| Paging | 2.1.2 | 페이징 처리 |

---

## 6. 주요 기능 설계

### 6.1 역 검색 및 조회
- 사용자가 지하철 역명 입력
- 데이터베이스에서 일치하는 역 검색
- 검색 결과 목록 표시
- 선택한 역의 실시간 도착 정보 조회

### 6.2 실시간 도착 정보
- API를 통해 선택한 역의 실시간 도착 정보 수신
- 도착 예정 시간 계산 및 표시
- 상행/하행선 구분 표시
- 새로고침 기능 제공

### 6.3 시간표 조회
- 요일별 시간표 데이터 저장 (Room)
- 저장된 역의 시간표 조회
- 요일별 필터링 기능

### 6.4 즐겨찾기 관리
- 자주 이용하는 역 저장
- 저장된 역 목록 표시
- 저장된 역 삭제 기능

---

## 7. 데이터 모델

### 7.1 FreshData
```kotlin
@Entity
data class FreshData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val stationName: String,
    val line: String,
    val departureTime: String,
    val arrivalTime: String,
    val direction: String
)
```

### 7.2 Subways
```kotlin
@Entity
data class Subways(
    @PrimaryKey val id: String,
    val stationName: String,
    val subwayLine: String,
    val exits: List<String>
)
```

---

## 8. 화면 구성

| 화면 | 설명 |
|------|------|
| SplashScreen | 앱 로딩 화면 |
| SearchScreen | 역 검색 화면 |
| ResultScreen | 도착 정보 표시 화면 |
| SaveScreen | 즐겨찾기 추가 화면 |
| TimeTableScreen | 저장된 역 시간표 조면 |

---

## 9. 보안 고려사항

- **로컬 데이터**: Room Database를 통한 암호화된 저장
- **API 통신**: HTTPS를 통한 안전한 데이터 전송
- **입력 검증**: 사용자 입력값 유효성 검사

---

## 10. 성능 최적화

- **Paging**: 대량 데이터 로딩 시 페이징 처리
- **LiveData**: 데이터 변경 감지 및 효율적 업데이트
- **RecyclerView**: 목록 표시 최적화
- **Coroutine**: 비동기 처리를 통한 메인 스레드 보호

---

## 11. 테스트 전략

- **단위 테스트**: ViewModel, Repository 로직 검증
- **통합 테스트**: Database, Network 통합 검증
- **UI 테스트**: Fragment, Activity 동작 검증

---

## 12. 배포 고려사항

- **최소 SDK**: API 26 (Android 8.0)
- **목표 SDK**: API 29 (Android 10)
- **빌드 도구**: 29.0.3
- **App Bundle**: Google Play 배포용 번들 지원

---

**최종 수정**: 2021년  
**상태**: 완료
