# Breeze - 스마트 러닝 앱 기획서

## 1. 앱 개요

| 항목 | 내용 |
|------|------|
| 앱 이름 | Breeze |
| 플랫폼 | Android (Kotlin) |
| 패키지명 | com.sleepysoong.breeze |
| 최소 SDK | API 26 (Android 8.0) |
| 타겟 SDK | API 34 (Android 14) |
| 핵심 컨셉 | AI 기반 스마트 메트로놈으로 목표 페이스를 달성하는 러닝 앱 |

## 2. 디자인 시스템

### 2.1 컬러

| 용도 | 컬러 코드 |
|------|-----------|
| 배경 | #0D0D0D |
| 카드 배경 | rgba(255,255,255,0.08) + blur |
| 카드 테두리 | rgba(255,255,255,0.1) |
| 키 컬러 (Primary) | #FF3B30 |
| 키 컬러 (Light) | #FF6B5E |
| 텍스트 (Primary) | #FFFFFF |
| 텍스트 (Secondary) | #999999 |
| 텍스트 (Tertiary) | #666666 |

### 2.2 타이포그래피

| 항목 | 값 |
|------|-----|
| 폰트 | Pretendard |
| 자간 | -10% (letterSpacing: -0.1em) |
| 제목 (H1) | 32sp, Bold |
| 제목 (H2) | 24sp, SemiBold |
| 본문 (Body) | 16sp, Regular |
| 캡션 | 14sp, Regular |
| 수치 표시 | 48sp, Bold (러닝 데이터) |

### 2.3 컴포넌트 스타일

- 글래스모피즘: 반투명 배경 + blur 효과 + 미세 border
- 카드 모서리: 16dp radius
- 버튼 모서리: 12dp radius
- 그림자: 최소화 (다크모드 최적화)

## 3. 주요 기능

### 3.1 기본 러닝 측정

- 실시간 거리, 시간, 현재 페이스, 평균 페이스
- 칼로리 소모량 계산
- GPS 기반 경로 추적
- 러닝 기록 저장 및 히스토리 조회

### 3.2 지도 표시

- 카카오 지도 Android SDK v2 사용 (무료, 일 30만 회)
- 실시간 경로 표시 (러닝 중)
- 완료된 러닝 경로 시각화 (기록 조회)
- API 키: 앱 설정에서 사용자가 직접 입력

### 3.3 스마트 메트로놈 (킬러 기능)

목표 페이스에 맞춰 발이 땅에 닿아야 하는 타이밍을 소리로 안내

#### 목표 페이스 설정

| 항목 | 내용 |
|------|------|
| UI | 다이얼(타이머 스타일) 인터페이스 |
| 범위 | 1분 00초 ~ 30분 00초 (1km당) |
| 조절 단위 | 10초 |
| 기본값 | 마지막 선택값 (최초 실행 시 6분 30초) |
| 안내 문구 | "오늘은 어떤 속도로 뛰어 볼까요?" |

#### 메트로놈 소리

- 구름을 가볍게 밟는 듯한 부드러운 "꿍" 소리
- 낮은 톤의 가벼운 사운드

#### 스마트 BPM 조절 로직

**첫 번째 러닝 (데이터 없음)**

- 단순 계산 기반 고정 BPM
- 보폭 초기값: 0.8m
- BPM = (1000m / 보폭) / 목표 페이스(분)

**두 번째 러닝부터 (AI 모델 적용)**

- 사용자의 과거 러닝 기록에서 피로 패턴 학습
- 목표: 러닝 종료 시 평균 페이스가 목표값과 일치하도록 조절
- 초반: 목표보다 약간 빠른 BPM (에너지 저금)
- 후반: 목표보다 약간 느린 BPM (피로 감안)

**예시 (목표: 5분/km, 총 5km 러닝)**

| 구간 | 일반 앱 | Breeze |
|------|---------|--------|
| 0-1km | 5:00 | 4:45 |
| 1-2km | 5:00 | 4:50 |
| 2-3km | 5:00 | 5:00 |
| 3-4km | 5:00 | 5:10 |
| 4-5km | 5:00 | 5:15 |
| 평균 | 5:00 | 5:00 |

## 4. 온디바이스 머신러닝 설계

### 4.1 기술 선택

- TensorFlow Lite (일반 스마트폰에서 구동 가능)
- 사전 학습 모델 없음, 개인 데이터만으로 학습

### 4.2 학습 데이터

- 러닝 기록별 구간 페이스 데이터
- 입력: 현재 거리, 경과 시간, 최근 페이스 변화율
- 출력: 다음 구간 예측 페이스

### 4.3 모델 구조

- 경량 회귀 모델 (Linear Regression 또는 작은 MLP)
- 러닝 완료 후 로컬에서 모델 업데이트

### 4.4 보폭 자동 보정

- 초기값: 0.8m
- 러닝 데이터 누적 시 자동 계산: GPS 거리 / 가속도계 걸음 수

## 5. 기술 스택

| 분류 | 기술 |
|------|------|
| 언어 | Kotlin |
| UI | Jetpack Compose |
| 최소 SDK | API 26 (Android 8.0) |
| 지도 | Kakao Maps Android SDK v2 |
| 위치 | FusedLocationProviderClient |
| 머신러닝 | TensorFlow Lite |
| 로컬 DB | Room |
| 아키텍처 | MVVM + Clean Architecture |
| DI | Hilt |
| 비동기 | Coroutines + Flow |
| 오디오 | SoundPool (메트로놈 소리) |
| 네비게이션 | Navigation Compose (하단 탭) |

## 6. 화면 구성

### 6.1 네비게이션 구조

하단 탭 바 (3개 탭):
1. 홈 (러닝 시작)
2. 히스토리 (기록 목록)
3. 설정

### 6.2 홈 화면

- 러닝 시작 버튼 (큰 원형, 키 컬러)
- 최근 러닝 기록 요약 카드 (거리, 시간, 평균 페이스)
- 주간 통계 요약

### 6.3 목표 페이스 설정 화면 (모달/바텀시트)

- "오늘은 어떤 속도로 뛰어 볼까요?" 문구
- 다이얼 UI (1:00 ~ 30:00, 10초 단위)
- 시작 버튼

### 6.4 러닝 중 화면

- 상단: 실시간 지도 (현재 위치 + 경로)
- 중앙: 현재 페이스, 평균 페이스, 거리, 시간 (큰 숫자)
- 하단: 메트로놈 상태, 일시정지/종료 버튼

### 6.5 러닝 완료 화면

- 전체 경로 지도
- 총 거리, 시간, 평균 페이스, 칼로리
- 구간별 페이스 그래프
- 저장 버튼

### 6.6 히스토리 화면

- 과거 러닝 기록 목록 (카드 형태)
- 기록별 간략 정보 (날짜, 거리, 페이스)
- 클릭 시 상세 화면으로 이동

### 6.7 기록 상세 화면

- 지도에 경로 표시
- 구간별 페이스 그래프
- 상세 통계

### 6.8 설정 화면

- 카카오 지도 API 키 입력
- 메트로놈 볼륨 조절
- 거리 단위 (km/mi)
- 데이터 초기화

## 7. 데이터 모델

### 7.1 RunningRecord

```kotlin
data class RunningRecord(
    val id: Long,
    val startTime: Long,           // timestamp
    val endTime: Long,             // timestamp
    val totalDistance: Double,     // meters
    val totalTime: Long,           // milliseconds
    val targetPace: Int,           // seconds per km
    val averagePace: Int,          // seconds per km
    val calories: Int,
    val routePoints: List<LatLng>,
    val paceSegments: List<PaceSegment>
)
```

### 7.2 PaceSegment

```kotlin
data class PaceSegment(
    val segmentIndex: Int,         // 0부터 시작, 1km 단위
    val pace: Int,                 // seconds per km
    val startTime: Long,
    val endTime: Long
)
```

### 7.3 UserSettings

```kotlin
data class UserSettings(
    val strideLength: Double,      // meters, 초기값 0.8
    val lastTargetPace: Int,       // seconds per km, 초기값 390 (6분 30초)
    val metronomeVolume: Float,    // 0.0 ~ 1.0
    val distanceUnit: DistanceUnit,// KM or MILE
    val kakaoApiKey: String        // 사용자 입력
)
```

## 8. 개발 우선순위

### Phase 1 (MVP)

1. 프로젝트 설정 (Gradle, 의존성)
2. 테마 및 디자인 시스템 구축
3. 기본 러닝 측정 기능
4. 단순 계산 기반 메트로놈

### Phase 2

5. 목표 페이스 다이얼 UI
6. 러닝 기록 저장 (Room)
7. 히스토리 화면
8. 카카오 지도 연동

### Phase 3

9. TensorFlow Lite 모델 구현
10. 피로 패턴 학습 및 예측
11. 스마트 BPM 조절

### Phase 4

12. 구간별 페이스 그래프
13. 보폭 자동 보정
14. UI/UX 개선
