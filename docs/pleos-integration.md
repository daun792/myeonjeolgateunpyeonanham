# PLEOS 연동 확인 사항

확인일: 2026-11-12. 아래의 공개 문서는 확인 시점의 최신 안내이며, 설치된 에뮬레이터의 모든 기능이 문서와 같다는 뜻은 아닙니다.

## 이 개발 환경에서 확인한 사실

- 설치된 `Pleos_Connect_2_1` AVD는 빌드 문자열이 `release_connect_v2.1.0`이고 Android API 34의 Automotive 이미지입니다. 에뮬레이터의 `android.hardware.type.automotive` 기능이 확인됐습니다.
- 에뮬레이터에 `ai.pleos.playground.service.vehicle` 서비스가 설치되어 있고, 해당 패키지의 버전은 `2.3.22`입니다. 이는 에뮬레이터 이미지 버전이나 앱에 추가하는 SDK 라이브러리 버전과는 별개입니다.
- 서비스는 `pleos.car.permission.CAR_DRIVE`를 `normal` 권한으로 선언합니다. 패키지와 권한의 존재만으로 특정 차량 속성의 지원, 앱 호출 성공 또는 실차 적용이 검증되는 것은 아닙니다.
- 에뮬레이터의 `dumpsys car_service --services CarPropertyService`에서는 `GEAR_SELECTION`(`0x11400400`), `PARKING_BRAKE_ON`(`0x11200402`), `PERF_VEHICLE_SPEED`(`0x11600207`)에 다른 클라이언트의 등록이 보였습니다. 이는 시스템 진단 결과이며 일반 앱의 접근 권한이나 실제 값의 신뢰성을 확인한 것은 아닙니다.
- 이번에 확인한 Android SDK 디렉터리와 Gradle 의존성 캐시에는 별도의 Pleos Vehicle SDK AAR/JAR이 없었습니다. 에뮬레이터 시스템 이미지 설치만으로 앱의 SDK 의존성이 설정된 것은 아닙니다.

## 차량 주차 상태와 권한

- [PLEOS Vehicle SDK 안내](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/intro)는 Vehicle SDK 2.3.2와 Vehicle Service 2.3.21 기준으로 Brake 도메인의 브레이크 페달·주차 브레이크 상태 조회를 설명합니다. 다만 확인한 [Brake API 목록](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/brake/entry)에는 페달 관련 메서드만 표시되어, 주차 브레이크에 대응하는 구체적인 공개 메서드는 아직 확인하지 못했습니다. 도메인별 지원 여부는 capability API로 확인해야 합니다.
- [Vehicle SDK 의존성 안내](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/project-setup)의 예시는 Pleos Maven 저장소와 `ai.pleos.playground:Vehicle:2.3.2` 의존성을 사용합니다. 2026-11-12 인증 정보 없이 읽기 전용 HTTP HEAD 요청으로 해당 [2.3.2 POM](https://nexus-playground.pleos.ai/repository/maven-releases/ai/pleos/playground/Vehicle/2.3.2/Vehicle-2.3.2.pom)과 [Maven 메타데이터](https://nexus-playground.pleos.ai/repository/maven-releases/ai/pleos/playground/Vehicle/maven-metadata.xml)가 각각 HTTP 200을 반환함을 확인했습니다. SDK 바이너리는 다운로드하지 않았습니다. 이 결과는 의존성 다운로드·API 호출 성공이나 설치된 이미지와의 호환성을 보장하지 않습니다.
- [PLEOS 권한 안내](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission)는 Brake, DrivingMode, Odometer의 대표 조회 권한으로 `pleos.car.permission.CAR_DRIVE`를 제시합니다. 개별 API의 요구 권한은 해당 함수의 `@RequiresPermission`을 확인해야 합니다. [SDK 개요](https://document.pleos.ai/en/api-reference/connect-sdk-pleos/)에 따르면 Vehicle SDK 권한 사용은 App Market과 사전 협의·승인된 범위를 따라야 합니다.
- [Android Automotive 차량 속성 문서](https://developer.android.com/reference/android/car/VehiclePropertyIds)는 `GEAR_SELECTION`과 `PARKING_BRAKE_ON` 읽기에 `Car.PERMISSION_POWERTRAIN`, `PERF_VEHICLE_SPEED` 읽기에 `Car.PERMISSION_SPEED`를 명시합니다. [권한 상수](https://developer.android.com/reference/android/car/Car)에 따르면 각각 `android.car.permission.CAR_POWERTRAIN`(normal), `android.car.permission.CAR_SPEED`(dangerous)입니다. 이는 Android 문서상의 후보 API이며, 이 PLEOS 이미지에서 실제 값·권한·지원 여부는 아직 앱으로 검증하지 않았습니다.
- 주차 브레이크 작동, P 기어 또는 속도 0 중 하나만으로 안전한 주차 상태가 확인됐다고 단정하지 않습니다. 실제 주차 판정, 값의 신선도, 오류·연결 해제 처리와 허용 정책은 확인이 필요합니다. 확인 불가 또는 상태 변경 시 비공개가 기본이어야 합니다.
- 별도의 [Vehicle Data API](https://document.pleos.ai/en/api-reference/vehicle-data-api/intro)는 OAuth와 차량 데이터 공유 조건을 가지며 문서상 2026년 8월 기준 유럽에서 제공됩니다. 차량 데이터 수신 시각과 지역 제한이 있으므로 이 차량 내 실시간 공개 판정에 사용할 수 있다고 가정하지 않습니다.

## 알림 접근 범위

- 이 프로젝트가 만드는 테스트 알림과 보관 데이터는 앱 자체 데이터입니다. 현재 타 앱 알림을 수집하거나 차량 알림 UI를 제어하지 않습니다.
- Android의 [NotificationListenerService](https://developer.android.com/reference/android/service/notification/NotificationListenerService)는 시스템이 알림 게시·제거 등을 통지하는 방식입니다. 서비스 선언 시 `BIND_NOTIFICATION_LISTENER_SERVICE`는 시스템만 서비스를 바인딩하도록 서비스에 요구하는 권한이며, 별도의 사용자 알림 접근 허용과 연결 상태 확인이 필요합니다. 기기 정책·프로필에 따라 접근이 제한될 수 있습니다.
- 알림이 게시될 때의 콜백이나 취소 API가 **다른 앱 알림의 첫 노출을 사전에 차단한다는 보장은 확인되지 않았습니다**. PLEOS에서의 접근 가능성, 개인정보 처리 및 App Market 심사도 별도 확인이 필요합니다. 테스트용 자체 보관 기능을 타 앱 알림 보호 기능으로 설명하지 않습니다.

## 실제 공개 기능을 연결하기 전

실제 보호 해제와 주차가 신뢰할 수 있는 최신 상태로 확인된 경우에만 사용자가 선택한 항목을 표시하고, 둘 중 하나라도 불명·실패·연결 해제라면 가립니다. 상세 화면이 열린 뒤 상태가 바뀌어도 즉시 내용을 숨기도록 실제 상태 공급자와 화면 생명주기를 함께 검증해야 합니다.
