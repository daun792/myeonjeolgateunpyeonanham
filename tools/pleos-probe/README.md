# PLEOS Vehicle SDK 읽기 전용 진단

이 독립 프로젝트는 PLEOS 에뮬레이터에서 공식 Vehicle SDK의 차량 서비스 연결과 읽기 API를 점검한다. 실제 앱과 독립된 테스트 패키지이며, 앱의 의존성이나 권한을 변경하지 않는다. 테스트용 AVD에서만 사용한다.

`pleos.car.permission.CAR_DRIVE`를 선언하고 `Vehicle.initialize()` 후 브레이크 **페달** 상태의 capability를 확인한다. 읽기가 가능하다고 응답한 경우에만 `getCurrentBrakePedalStatus`를 한 번 호출하고, 결과 또는 오류를 진단 로그에 남긴다. 쓰기 API와 차량 제어는 사용하지 않는다.

**페달 상태는 주차 상태가 아니다.** 이 결과는 알림 내용 공개나 주행 안전 판정에 사용하지 않는다. 진단용 receiver는 ADB 명시적 실행을 위해 exported이며 호출을 `android.permission.DUMP` 보유 주체로 제한한다. 프로덕션 앱으로 배포하지 않는다. 실제 차량 적용에는 해당 차량·서비스 버전, 권한 및 PLEOS App Market 심사를 별도로 확인해야 한다.

## 실행

레포지토리 루트에서 PowerShell로 실행한다. JDK와 SDK 경로는 각 개발 환경에 맞춘다. 첫 빌드에서는 공식 PLEOS Maven 저장소와 의존성 저장소에 접근해야 한다.

```powershell
$env:JAVA_HOME = 'D:\Android\android-studio\jbr'
$env:GRADLE_USER_HOME = 'D:\Android\gradle'
$env:ANDROID_HOME = 'D:\Android\sdk'
.\gradlew.bat -p tools\pleos-probe :app:assembleDebug --no-daemon --max-workers=2
$adb = 'D:\Android\sdk\platform-tools\adb.exe'
& $adb devices
& $adb -s emulator-5554 install -r 'tools\pleos-probe\app\build\outputs\apk\debug\app-debug.apk'
& $adb -s emulator-5554 shell am broadcast --include-stopped-packages -n com.example.pleosprobe/.ProbeReceiver
& $adb -s emulator-5554 logcat -d -s 'PleosReadOnlyProbe:I' '*:S'
```

`emulator-5554`는 실제 테스트용 AVD의 일련번호로 바꾼다. 권한은 실험 패키지의 manifest에서 선언하며, `adb grant`로 특수 권한을 강제 부여하거나 차량 값을 조작하지 않는다. 테스트가 끝나면 실험 패키지만 `adb -s emulator-5554 uninstall com.example.pleosprobe`로 제거할 수 있다. 기존 앱의 데이터는 삭제하지 않는다.

## 검증 범위

2026-11-12, Android API 34 PLEOS Connect 2.1 AVD(차량 서비스 2.3.22)와 SDK 2.3.2에서 `initialize returned`, `capability success: READ`, `brake pedal read success: false` 콜백을 확인했다. 테스트 패키지에 `pleos.car.permission.CAR_DRIVE`와 SDK가 선언한 `ai.pleos.playground.service.vehicle.VEHICLE_BINDING`이 부여된 것으로 표시됐다. 이는 해당 AVD에서 한 번의 읽기에 대한 결과다.

출처: [Vehicle SDK 의존성](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/project-setup), [초기화와 도메인 객체](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/obtain-domain-object), [권한](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission), [Brake API](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/brake/entry).
