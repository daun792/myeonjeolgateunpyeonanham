# 외부 연동·권한 확인 목록

확인일: 2026-11-13. 이 문서는 계획서의 기능 중 앱 밖의 승인, 사용자 권한 또는 차량 플랫폼 지원에 의존하는 부분을 정리합니다. 현재 프로젝트에서 승인이나 실차 동작을 확인하지 못한 항목은 완료로 간주하지 않습니다. 프로젝트별 SDK 상태와 공급사 정책은 바뀔 수 있으므로 실제 연동 전에 다시 확인해야 합니다.

## 신청·심사·계약 여부를 확인할 항목

| 대상 | 필요한 확인과 조치 | 현재 대체 동작 또는 한계 |
| --- | --- | --- |
| PLEOS SDK | [프로젝트별 SDK 접근 안내](https://document.pleos.ai/en/docs/pleos-only/create-my-project)의 상태를 확인합니다. 안내 예시에서 Vehicle State와 NaviHelper는 `Ready to Use`이며, 실제 프로젝트에서도 이 상태라면 별도의 SDK 접근 신청 없이 사용할 수 있습니다. `Request Required`는 신청·심사 대상이고, `Affiliation Required`는 별도 제휴가 필요합니다. Vehicle Control, FusedLocation, TTS, STT는 안내 예시에서 `Not Available Yet`으로 표시되어 신청 버튼이 없을 수 있습니다. **이 프로젝트의 실제 신청 상태는 아직 확인하지 못했습니다.** | SDK 사용 가능 표시가 특정 차량 속성의 지원, 호출 성공 또는 실차 배포 승인을 뜻하지는 않습니다. 개별 권한과 차량 capability도 확인해야 합니다. |
| Gleo 음성 명령 | [Gleo 스키마 제출 안내](https://document.pleos.ai/en/docs/pleos-only/app-integration-guide/guide/deliver-schema)에 따라 앱 스키마를 제출하고 심사를 받아야 합니다. | 앱 안의 제한적인 텍스트 규칙 제안은 음성 인식이나 AI가 아닙니다. 제안을 먼저 확인하고 별도로 저장해야 설정에 반영됩니다. |
| Krisp 등 음성 분리 | 공급사별 지원 기기, 라이선스와 계약 조건을 확인하고 필요시 도입 절차를 진행합니다. [Krisp RTC SDK](https://developers-docs.krisp.ai/docs/krisp-rtc-bvc-nc), [Krisp 계약 문의](https://krisp.ai/contact-sales/). | SDK 계약만으로 차량 기본 통화의 송수신 경로에 접근할 수는 없습니다. 차량 제조사의 오디오 경로 통합도 필요합니다. |
| 장소 유형 데이터 | 주유소 등 장소 유형을 자동 판별하려면 적합한 POI 데이터 제공자를 선정하고 사용 조건, 키 및 필요 시 계약을 확인해야 합니다. | 좌표만으로 장소 유형을 판별할 수는 없습니다. NaviHelper 사용 가능 표시도 원하는 POI 기능의 제공을 보장하지 않습니다. 위치 권한과 차량 지원은 별도입니다. |

PLEOS 차량 SDK 사용 시 API별 manifest 권한과 [PLEOS 권한 안내](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission)의 요구사항을 확인해야 합니다. [SDK 개요](https://document.pleos.ai/en/api-reference/connect-sdk-pleos/)에 따른 App Market 사전 협의·승인 범위와 실제 차량 지원 여부 역시 별도 확인 사항입니다. SDK 접근 상태가 `Ready to Use`라고 해서 이 조건들이 모두 확인된 것은 아닙니다.

## 사용자의 기기 권한과 선택

아래는 사업자 API 신청과 다른 단계입니다. 실제 연동 기능에 따라 사용자가 기기에서 허용하거나 선택해야 하며, 현재 이 앱이 해당 권한을 받았다는 뜻은 아닙니다.

| 기능 | 사용자 권한 또는 선택 | 범위와 한계 |
| --- | --- | --- |
| 타 앱 알림 수신 | 시스템 설정에서 알림 접근을 허용해야 합니다. 이는 일반 런타임 권한 창과 다른 특별 접근입니다. [Android 알림 리스너](https://developer.android.com/reference/android/service/notification/NotificationListenerService) | 게시된 알림을 전달받는 방식이므로 타 앱 알림의 최초 팝업이나 음성 출력 이전에 차단하는 것을 보장하지 않습니다. 현재 앱은 자체 테스트 알림만 다룹니다. |
| 연락처 | 실제 연락처를 읽는 기능을 도입한다면 `READ_CONTACTS` 런타임 권한이 필요합니다. [Android 권한 요청](https://developer.android.com/training/permissions/requesting) | 허용해도 기본 전화 앱이나 다른 앱의 화면을 변경할 수 있는 것은 아닙니다. 현재 가림 화면은 앱 자체 예시입니다. |
| 마이크 | 앱 자신의 마이크를 수집한다면 `RECORD_AUDIO` 런타임 권한이 필요합니다. [Android 오디오 소스](https://developer.android.com/reference/android/media/MediaRecorder.AudioSource) | 일반 마이크 권한만으로 기본 전화의 통화 송수신 음성 경로를 이용할 수 없습니다. 현재 텍스트 점검은 녹음이나 TTS를 수행하지 않습니다. |
| 위치·일정 | 실제 위치 또는 일정을 읽는 기능을 도입한다면 해당 Android 권한과 사용자 허용이 필요합니다. 백그라운드 위치는 별도 요건이 있을 수 있습니다. [Android 권한 요청](https://developer.android.com/training/permissions/requesting) | 거절, 제한 또는 기기 미지원 시 해당 자동화를 확인되지 않음으로 처리해야 합니다. PLEOS 위치 SDK의 사용 가능 여부도 별도입니다. |
| 사진 선택 | 사용자 사진을 지원할 경우 시스템 문서 선택기에서 사용자가 파일을 선택하는 방법을 사용할 수 있습니다. 재시작 후에도 접근하려면 제공자가 허용하는 URI 권한 유지 및 삭제·취소 처리가 필요합니다. [Android 문서 선택](https://developer.android.com/training/data-storage/shared/documents-files) | 선택한 파일에 대한 접근과 다른 갤러리 앱 화면의 제어는 다릅니다. 실제 사용자 사진 연결은 현재 확정된 구현 범위로 보지 않습니다. |
| 앱의 시스템 알림 발송 | Android 13 이상에서 해당하는 시스템 알림을 발송하려면 `POST_NOTIFICATIONS` 사용자 허용이 필요합니다. [Android 알림 권한](https://developer.android.com/develop/ui/views/notifications/notification-permission) | 앱 내부 보관 목록 표시와 시스템 알림 발송은 다릅니다. |

## 차량 제조사·플랫폼 통합이 필요한 부분

- **실제 주차 상태:** 차량별로 지원되는 속성과 읽기 권한, 업데이트 시각, 오류·연결 해제 처리를 확인해야 합니다. 독립 진단 앱에서 PLEOS Connect 2.1 에뮬레이터의 브레이크 **페달** 읽기만 확인했습니다. 이 값은 주차 브레이크나 주차 확정 신호가 아닙니다. 기어 P, 주차 브레이크 또는 속도 0 중 하나만으로 안전한 주차를 확정하지 않습니다. 차량 UI의 사용 가능 범위는 해당 UX 제한도 따라야 합니다. [Android Automotive 운전 상태와 UX 제한](https://source.android.com/docs/automotive/driver_distraction/consume), [프로젝트의 PLEOS 조사](pleos-integration.md).
- **실제 보호 적용 상태:** 앱에 보호 요청이나 설정이 저장된 사실은 차량 시스템에서 보호가 실제 적용되거나 해제되었다는 증명이 아닙니다. 실제 적용 상태를 읽을 수 있는 신뢰 가능한 제조사 인터페이스와 권한 및 상태 만료 처리가 필요합니다. 현재 실제 주차·보호 상태 공급자는 연결되지 않았으며, 확인되지 않거나 오래된 상태에서는 보관 알림 내용을 공개하지 않습니다.
- **타 앱 및 차량 기본 화면의 최초 표시 제어:** 일반 알림 리스너만으로 최초 표시와 음성 읽기 이전의 보류를 보장할 수 없습니다. 차량 알림 파이프라인에 대한 OEM 통합과 정책 승인을 확인해야 합니다. 현재 앱 내부의 보관 목록과 가림은 앱이 제어하는 예시에 한정됩니다. [Android 알림 리스너](https://developer.android.com/reference/android/service/notification/NotificationListenerService)
- **차량 기본 통화 음성 억제:** Android의 통화 송수신 오디오 캡처에 필요한 `CAPTURE_AUDIO_OUTPUT`은 일반 서드파티 앱용이 아닙니다. 제조사의 통화·Bluetooth 오디오 경로 통합이 필요하며 별도의 음성 처리 SDK 계약만으로 해결되지 않습니다. [Android 오디오 소스](https://developer.android.com/reference/android/media/MediaRecorder.AudioSource)
- **도어·탑승·조명:** 각 차량의 실제 속성 지원과 `CAR_DOORS`, `CAR_SEATS`, `CAR_LIGHTS` 등 해당 읽기 권한을 확인해야 합니다. 조명 변경은 제어 권한과 Vehicle Control 지원 여부가 추가로 필요합니다. 센서가 없거나 값이 확인되지 않을 때는 추정하지 않습니다. [PLEOS 차량 권한](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission)
- **차량 기본 미디어·전화·갤러리와 비접촉 제스처:** 다른 앱의 화면과 재생·통화 경로, 차량 고유 센서를 제어할 수 있는 공개 인터페이스 및 제조사 정책을 확인해야 합니다. 현재 앱 자체 미디어 자동화와 차량 화면은 시연용이며 차량 전체에 적용되지 않습니다.

## 다음 확인 순서

1. 실제 PLEOS 프로젝트에서 SDK별 접근 상태와 대상 차량의 지원 속성을 확인합니다.
2. 제조사에 신뢰 가능한 주차 상태와 보호 적용 상태, 타 앱 알림 사전 제어, 통화 음성 경로 및 UX 제한의 이용 가능 여부를 확인합니다.
3. 필요한 Gleo 심사와 SDK 계약·신청을 진행합니다. 아직 제공되지 않는 SDK는 신청 가능 여부부터 확인합니다.
4. 실제 기기에서 필요한 사용자 권한과 거절·철회·연결 해제·상태 만료를 검증합니다. 계정 비밀값이나 API 키는 저장소에 올리지 않습니다.
