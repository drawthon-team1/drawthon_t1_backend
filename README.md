# Drawthon Backend

Java 25 / Spring Boot 기반 세션 인증 API입니다.

## 실행

```powershell
.\gradlew.bat bootRun
```

Java 25가 PATH에 없다면 실행 전에 `JAVA_HOME`을 설치된 JDK 25 경로로 설정하세요.
현재 DB는 기본 H2 메모리 DB이므로 서버를 재시작하면 가입 정보가 초기화됩니다.

### 관리자 계정

MVP 데모용으로 회원가입 요청에 `role`(`ADMIN` 또는 `USER`)을 함께 보내면 그 역할로 가입합니다. 생략하면 `USER`입니다.
누구나 `ADMIN`으로 가입할 수 있으므로 실서비스 전에는 막아야 합니다.

## Swagger UI에서 확인

서버 실행 후 <http://localhost:8080/swagger-ui/index.html>에 접속합니다.
각 API의 **Try it out → Execute**로 호출하세요.

1. `GET /api/auth/csrf`: CSRF 쿠키를 발급합니다. Swagger UI가 이후 POST 요청에 토큰 헤더를 자동으로 추가합니다.
2. `POST /api/auth/signup`: 아래 JSON으로 가입합니다. 성공 응답은 `201`이며 자동 로그인되지 않습니다.
3. `POST /api/auth/login`: 같은 이메일과 비밀번호로 로그인합니다. 성공 응답은 `200`이며 브라우저가 `JSESSIONID` 쿠키를 저장합니다.
4. `GET /api/auth/me`: 로그인한 회원 정보와 `200`을 확인합니다.
5. `POST /api/auth/logout`: 세션을 종료하고 `204`를 반환합니다.
6. `GET /api/auth/me`: 로그아웃 후 `401`을 확인합니다.

회원가입 요청:

```json
{
  "email": "test@example.com",
  "password": "password1234",
  "nickname": "테스트"
}
```

로그인 요청:

```json
{
  "email": "test@example.com",
  "password": "password1234"
}
```

회원가입·로그인·내 정보 조회 응답은 `id`, `email`, `nickname`으로 통일합니다.
Swagger의 Authorize에 세션 ID를 입력할 필요는 없습니다. 같은 브라우저와 호스트에서 호출하세요.
POST 요청이 `403`이면 CSRF API를 다시 호출한 뒤 재시도하세요.

## 입력과 오류 응답

- 이메일: 필수, 이메일 형식, 최대 120자. 저장·조회 시 앞뒤 공백 제거 및 소문자 변환.
- 회원가입 비밀번호: 8~72자이면서 UTF-8 기준 최대 72바이트. BCrypt로 해싱.
- 닉네임: 공백 제외 필수, 최대 40자.
- `400`: 잘못된 입력 또는 JSON. `401`: 로그인 실패 또는 미인증.
- `403`: CSRF 검증 실패 또는 접근 거부. `404`: 회원 없음. `409`: 이메일 중복 등 저장 충돌.

세션 유효 시간은 마지막 요청으로부터 30분입니다.
별도 프론트엔드에서는 쿠키를 포함해 CSRF API를 호출하고, 반환된 `headerName`/`token`을 변경 요청에 넣어야 합니다.
로컬 CORS 허용 주소는 `http://localhost:3000`, `http://localhost:5173`입니다.

## 빌드

```powershell
.\gradlew.bat build -x test
```

이번 정리에서는 테스트 코드를 추가하지 않았습니다.
