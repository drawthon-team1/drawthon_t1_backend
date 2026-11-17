# Drawthon Backend

Java 25 / Spring Boot 기반 세션 인증 API입니다.

## 실행

```powershell
.\gradlew.bat bootRun
```

Java 25가 PATH에 없다면 실행 전에 `JAVA_HOME`을 설치된 JDK 25 경로로 설정하세요.
현재 DB는 기본 H2 메모리 DB이므로 서버를 재시작하면 가입 정보가 초기화됩니다.

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

회원가입·로그인·내 정보 조회 응답은 `id`, `email`, `nickname`, `role`로 통일합니다.
Swagger의 Authorize에 세션 ID를 입력할 필요는 없습니다. 같은 브라우저와 호스트에서 호출하세요.
POST 요청이 `403`이면 CSRF API를 다시 호출한 뒤 재시도하세요.

일반 회원가입(`POST /api/auth/signup`)의 역할은 서버에서 항상 `USER`로 지정합니다.
관리자 회원가입(`POST /api/auth/admin/signup`)은 로그인한 `ADMIN`만 호출할 수 있으며 생성되는 역할은 `ADMIN`입니다.
최초 관리자는 DB에서 대상 회원의 `role`을 `ADMIN`으로 한 번 지정한 뒤 다시 로그인해야 합니다.

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

인증 컨텍스트 테스트와 팀 생성·초대·가입 흐름 통합 테스트가 포함되어 있습니다.

## 팀 API

모든 팀 API는 로그인 세션이 필요합니다. POST 요청 전에는 `GET /api/auth/csrf`로 받은 토큰을 사용합니다.

1. `POST /api/teams`: 팀을 만들고 로그인 회원을 팀장으로 등록합니다.

   ```json
   { "name": "드로우톤 1팀" }
   ```

2. `POST /api/teams/{teamId}/invitations`: 팀장이 7일간 유효한 초대 링크를 만듭니다.
3. `GET /api/team-invitations/{token}`: 로그인한 초대 대상자가 팀명, 팀장, 현재 인원을 확인합니다.
4. `POST /api/team-invitations/{token}/accept`: 초대를 수락합니다. 이미 가입한 회원이 다시 호출해도 중복 가입되지 않습니다.
5. `GET /api/teams/{teamId}`: 팀원과 역할을 포함한 팀 현황을 조회합니다.
6. `GET /api/teams`: 로그인 회원이 가입한 팀 목록을 조회합니다.

초대 링크의 프론트엔드 주소는 `app.frontend-base-url` 설정으로 변경할 수 있습니다.

### 팀 기능 버튼 테스트

서버 실행 후 <http://localhost:8080/teammaker-test.html>에 접속합니다.

1. **관리자 자동 가입 + 로그인**을 누릅니다. `admin1@abc.com` / `1234` 계정을 자동으로 사용합니다.
2. 팀 이름을 입력하고 **팀 생성**을 누릅니다.
3. **초대 링크 생성**으로 링크를 확인합니다.
4. **테스트 회원으로 수락**을 누르면 임시 회원 가입·로그인·초대 수락·관리자 재로그인을 자동으로 수행합니다.
5. 팀 현황에서 추가된 회원을 확인합니다.

페이지 아래의 API 요청 기록에서 각 요청 URL, HTTP 상태와 응답을 확인할 수 있습니다.

## 배포 (Render + Vercel)

백엔드는 Render(Docker, 무료), 프론트는 Vercel에 올린다. 브라우저는 Vercel 주소로만 요청하고, Vercel이 `/api` 요청을 Render로 넘긴다(프론트 저장소의 `vercel.json`).

1. Render에 GitHub로 로그인한 뒤 **New → Blueprint**에서 이 저장소를 고른다. `render.yaml`대로 백엔드 서비스(`drawthon-t1-backend`)와 무료 Postgres(`drawthon-db`)가 만들어진다.
2. 대시보드에서 비밀 값을 입력한다: `VOLUNTEER_API_SERVICE_KEY`, `KAKAO_REST_API_KEY`, `OPENAI_API_KEY`, `DEV_ADMIN_PASSWORD`.
3. 배포가 끝나면 서비스 주소가 `https://drawthon-t1-backend.onrender.com`인지 확인한다. 다르면 프론트 `vercel.json`의 주소를 바꾼다.

- 배포에서는 `prod` 프로필(`application-prod.yaml`)로 Postgres를 써서, 서버가 잠들거나 재시작해도 계정·팀·신청 내역이 남는다. 로컬은 지금처럼 H2 메모리 DB를 쓴다.
- 무료 플랜은 15분 동안 요청이 없으면 잠들고, 다음 요청 때 깨어나는 데 약 1분 걸린다. 시연 전에 사이트를 한 번 열어 깨워 둔다.
- 무료 Postgres는 생성 후 30일 뒤 만료된다.
