Initial commit for GameRecs application, including backend and frontend setup. Added Docker configurations, environment variables, and initial database schema. Implemented basic Spring Boot application structure with OpenAPI support. Frontend built with Angular, including routing and health check component. Configured ESLint and Tailwind CSS for styling. Added test utilities and mock store for NgRx state management.
.env.example
.gitignore
docker-compose.yml
gamerecs-back/.gitattributes
gamerecs-back/.gitignore
gamerecs-back/.mvn/wrapper/maven-wrapper.properties
gamerecs-back/Dockerfile
gamerecs-back/mvnw
gamerecs-back/mvnw.cmd
gamerecs-back/pom.xml
gamerecs-back/src/main/java/com/gamerecs/back/GamerecsBackApplication.java
gamerecs-back/src/main/java/com/gamerecs/back/config/OpenApiConfig.java
gamerecs-back/src/main/resources/application.yml
gamerecs-back/src/main/resources/db/migration/V1__Initial_schema.sql
gamerecs-back/src/main/resources/logback-spring.xml
gamerecs-back/src/test/java/com/gamerecs/back/GamerecsBackApplicationTests.java
gamerecs-back/src/test/java/com/gamerecs/back/config/BaseTest.java
gamerecs-back/src/test/java/com/gamerecs/back/config/TestConfig.java
gamerecs-back/src/test/java/com/gamerecs/back/util/BaseIntegrationTest.java
gamerecs-back/src/test/java/com/gamerecs/back/util/BaseUnitTest.java
gamerecs-front/.editorconfig
gamerecs-front/.eslintrc.json
gamerecs-front/.gitignore
gamerecs-front/Dockerfile
gamerecs-front/README.md
gamerecs-front/angular.json
gamerecs-front/karma.conf.js
gamerecs-front/nginx.conf
gamerecs-front/package-lock.json
gamerecs-front/package.json
gamerecs-front/public/favicon.ico
gamerecs-front/src/app/app.component.css
gamerecs-front/src/app/app.component.html
gamerecs-front/src/app/app.component.spec.ts
gamerecs-front/src/app/app.component.ts
gamerecs-front/src/app/app.config.ts
gamerecs-front/src/app/app.routes.ts
gamerecs-front/src/app/core/components/health/health.component.ts
gamerecs-front/src/app/core/services/health.service.ts
gamerecs-front/src/app/testing/mock-store.ts
gamerecs-front/src/app/testing/test-utils.ts
gamerecs-front/src/index.html
gamerecs-front/src/main.ts
gamerecs-front/src/styles.css
gamerecs-front/src/test.ts
gamerecs-front/tailwind.config.js
gamerecs-front/tsconfig.app.json
gamerecs-front/tsconfig.json
gamerecs-front/tsconfig.spec.json

Refactor and enhance GameRecs application setup. Improved health check functionality with detailed logging and error handling. Updated Docker configurations for both frontend and backend, resolving build issues and ensuring proper service dependencies. Enhanced nginx configuration for better debugging and security. Documented local development setup verification process. Fixed health check issues and improved UI for health status display.
docker-compose.yml
gamerecs-back/Dockerfile
gamerecs-front/Dockerfile
gamerecs-front/nginx.conf
gamerecs-front/src/app/core/components/health/health.component.ts
gamerecs-front/src/app/core/services/health.service.ts

Update ESLint configuration for Angular development. Enhanced rules for maintainability by adjusting error levels to warnings, adding Angular-specific rules, and improving TypeScript settings. Updated package.json to include Angular ESLint dependencies.
gamerecs-front/.eslintrc.json
gamerecs-front/package-lock.json
gamerecs-front/package.json

Enhance test configuration by adding H2 in-memory database for testing. Introduced application-test.yml for test-specific settings and disabled Flyway migrations during tests. Updated pom.xml to include H2 dependency for test scope.
gamerecs-back/pom.xml
gamerecs-back/src/test/resources/application-test.yml

Integrate SonarQube for code quality analysis and enhance coverage reporting. Added SonarQube configuration in docker-compose and pom.xml, including JaCoCo plugin for backend coverage. Updated frontend configurations for LCOV reporting and Karma testing. Improved code coverage thresholds and quality gates. Fixed report paths and deprecated configurations. Added sonar-project.properties for frontend analysis. Updated .gitignore to include SonarQube directories.
.gitignore
docker-compose.yml
gamerecs-back/pom.xml
gamerecs-front/angular.json
gamerecs-front/karma.conf.js
gamerecs-front/package.json
gamerecs-front/sonar-project.properties

Create ci-cd.yml
.github/workflows/ci-cd.yml

Implement backend CI/CD pipeline with GitHub Actions. Added workflow for build, test, and SonarQube analysis. Configured Docker image build and push for main and develop branches. Removed deprecated ci-cd.yml file.
.github/workflows/backend-ci.yml
.github/workflows/ci-cd.yml

Update backend CI/CD workflow to integrate SonarCloud for code quality analysis. Added caching for SonarCloud packages and configured project-specific secrets. Removed local SonarQube references and updated analysis commands to use SonarCloud settings.
.github/workflows/backend-ci.yml

Enhance CI/CD workflows by adding a new GitHub Actions pipeline for frontend, including build, test, lint, and Docker image build steps. Configured manual and branch-specific triggers, and implemented caching for npm dependencies and Docker layers. Updated backend CI/CD workflow to correct SonarCloud token usage.
.github/workflows/backend-ci.yml
.github/workflows/frontend-ci.yml

Implement comprehensive CORS and Spring Security configurations. Added WebConfig for CORS settings with logging and allowed origins, methods, and headers. Introduced SecurityConfig to permit access to Swagger UI and actuator endpoints, while disabling CSRF for API requests. Updated application and test configuration files to include CORS properties.
gamerecs-back/src/main/java/com/gamerecs/back/config/SecurityConfig.java
gamerecs-back/src/main/java/com/gamerecs/back/config/WebConfig.java
gamerecs-back/src/main/resources/application.yml
gamerecs-back/src/test/resources/application-test.yml

Implement User entity with JPA annotations and validation constraints. Added fields for user details including username, email, and timestamps for join date and last login. Utilized Lombok for boilerplate reduction and ensured validation for critical fields.
gamerecs-back/src/main/java/com/gamerecs/back/model/User.java

Enhance Security Configuration and Testing for Swagger UI and Actuator Endpoints
gamerecs-back/src/main/java/com/gamerecs/back/config/SecurityConfig.java
gamerecs-back/src/test/java/com/gamerecs/back/config/SecurityConfigTest.java
gamerecs-back/src/test/resources/application-test.yml

Enhance CORS and Security Configuration with Comprehensive Testing
gamerecs-back/pom.xml
gamerecs-back/src/main/java/com/gamerecs/back/config/SecurityConfig.java
gamerecs-back/src/test/java/com/gamerecs/back/config/WebConfigTest.java

Implement UserRepository for User entity database operations
gamerecs-back/src/main/java/com/gamerecs/back/repository/UserRepository.java

Implement UserService for user registration with validation and logging
gamerecs-back/src/main/java/com/gamerecs/back/service/UserService.java

Implement User Registration functionality with DTO and Controller
gamerecs-back/src/main/java/com/gamerecs/back/controller/UserController.java
gamerecs-back/src/main/java/com/gamerecs/back/dto/UserRegistrationDto.java

Enhance user registration process with comprehensive input validation
gamerecs-back/src/main/java/com/gamerecs/back/exception/GlobalExceptionHandler.java

Enhance User Component Testing and Security Configuration
gamerecs-back/src/main/java/com/gamerecs/back/config/SecurityConfig.java
gamerecs-back/src/test/java/com/gamerecs/back/controller/UserControllerTest.java
gamerecs-back/src/test/java/com/gamerecs/back/model/UserTest.java
gamerecs-back/src/test/java/com/gamerecs/back/repository/UserRepositoryTest.java
gamerecs-back/src/test/java/com/gamerecs/back/service/UserServiceTest.java

Update springdoc-openapi dependency version to 2.7.0 in pom.xml
gamerecs-back/pom.xml

Remove linebreak-style rule from ESLint configuration for improved cross-platform compatibility
gamerecs-front/.eslintrc.json

Implement user registration form with validation and UI enhancements
gamerecs-front/.eslintrc.json
gamerecs-front/src/app/app.component.html
gamerecs-front/src/app/app.component.ts
gamerecs-front/src/app/app.config.ts
gamerecs-front/src/app/app.routes.ts
gamerecs-front/src/app/core/components/auth/registration-form.component.css
gamerecs-front/src/app/core/components/auth/registration-form.component.html
gamerecs-front/src/app/core/components/auth/registration-form.component.ts
gamerecs-front/src/styles.css
gamerecs-front/tailwind.config.js

Enhance Registration Form validation and user experience
gamerecs-front/src/app/core/components/auth/registration-form.component.html
gamerecs-front/src/app/core/components/auth/registration-form.component.ts

Fix SonarQube test coverage configuration and improve Angular test settings
gamerecs-front/angular.json
gamerecs-front/karma.conf.js
gamerecs-front/sonar-project.properties

Refactor AppComponent tests to enhance dark mode functionality
gamerecs-front/src/app/app.component.spec.ts

Implement User Registration Service and API Communication Enhancements
gamerecs-front/angular.json
gamerecs-front/package.json
gamerecs-front/proxy.conf.json
gamerecs-front/src/app/app.config.ts
gamerecs-front/src/app/core/components/auth/registration-form.component.ts
gamerecs-front/src/app/core/services/auth.service.ts

Enhance User Registration API Communication
gamerecs-front/nginx.conf

Enhance user registration error handling and improve frontend experience
gamerecs-back/src/main/java/com/gamerecs/back/controller/UserController.java
gamerecs-back/src/main/java/com/gamerecs/back/exception/ApiError.java
gamerecs-back/src/main/java/com/gamerecs/back/exception/GlobalExceptionHandler.java
gamerecs-front/src/app/core/components/auth/registration-form.component.ts
gamerecs-front/src/app/core/services/auth.service.ts

Refactor UserControllerTest for Consistent JSON Error Responses
gamerecs-back/src/test/java/com/gamerecs/back/controller/UserControllerTest.java

Enhance Registration Form and AuthService Testing
gamerecs-front/package-lock.json
gamerecs-front/package.json
gamerecs-front/src/app/core/components/auth/registration-form.component.spec.ts
gamerecs-front/src/app/core/services/auth.service.spec.ts

Enhance End-to-End Testing for Registration Form
gamerecs-front/angular.json
gamerecs-front/cypress.config.ts
gamerecs-front/cypress/e2e/registration.cy.ts
gamerecs-front/cypress/support/commands.ts
gamerecs-front/cypress/support/e2e.ts
gamerecs-front/cypress/tsconfig.json
gamerecs-front/package.json
gamerecs-front/src/app/app.config.ts
gamerecs-front/src/app/core/components/auth/registration-form.component.html
gamerecs-front/src/app/core/components/auth/registration-form.component.spec.ts
gamerecs-front/src/app/core/components/auth/registration-form.component.ts
gamerecs-front/src/environments/environment.cypress.ts
gamerecs-front/src/environments/environment.test.ts
gamerecs-front/src/environments/environment.ts

Implement email verification feature for user registration
docker-compose.yml
gamerecs-back/pom.xml
gamerecs-back/src/main/java/com/gamerecs/back/config/SecurityConfig.java
gamerecs-back/src/main/java/com/gamerecs/back/controller/UserController.java
gamerecs-back/src/main/java/com/gamerecs/back/model/User.java
gamerecs-back/src/main/java/com/gamerecs/back/model/VerificationToken.java
gamerecs-back/src/main/java/com/gamerecs/back/repository/VerificationTokenRepository.java
gamerecs-back/src/main/java/com/gamerecs/back/service/EmailService.java
gamerecs-back/src/main/java/com/gamerecs/back/service/UserService.java
gamerecs-back/src/main/resources/application.yml
gamerecs-back/src/main/resources/db/migration/V2__Add_email_verification.sql
gamerecs-back/src/main/resources/db/migration/V3__Add_verification_token.sql
gamerecs-back/src/main/resources/templates/verification-email.html

Refactor Security Configuration and Enhance User Service Tests
gamerecs-back/src/main/java/com/gamerecs/back/config/SecurityConfig.java
gamerecs-back/src/test/java/com/gamerecs/back/config/SecurityConfigTest.java
gamerecs-back/src/test/java/com/gamerecs/back/service/UserServiceTest.java
gamerecs-back/src/test/resources/application-test.yml

Implement email verification feature in frontend
gamerecs-front/src/app/app.routes.ts
gamerecs-front/src/app/core/components/auth/email-verification.component.spec.ts
gamerecs-front/src/app/core/components/auth/email-verification.component.ts
gamerecs-front/src/app/core/components/auth/registration-form.component.spec.ts
gamerecs-front/src/app/core/components/auth/registration-form.component.ts
gamerecs-front/src/app/core/services/auth.service.spec.ts
gamerecs-front/src/app/core/services/auth.service.ts

Update dependencies and refine TypeScript configurations
gamerecs-front/cypress/tsconfig.json
gamerecs-front/package-lock.json
gamerecs-front/package.json
gamerecs-front/src/app/core/components/auth/email-verification.component.spec.ts
gamerecs-front/src/app/core/services/auth.service.spec.ts
gamerecs-front/tsconfig.json

Enhance email verification process and UI
gamerecs-back/src/main/java/com/gamerecs/back/service/EmailService.java
gamerecs-front/src/app/app.routes.ts
gamerecs-front/src/app/core/components/auth/email-verification.component.html
gamerecs-front/src/app/core/components/auth/email-verification.component.spec.ts
gamerecs-front/src/app/core/components/auth/email-verification.component.ts
gamerecs-front/src/app/core/components/auth/registration-form.component.spec.ts
gamerecs-front/src/app/core/components/auth/registration-form.component.ts

Enhance EmailService with input validation and comprehensive tests
gamerecs-back/src/main/java/com/gamerecs/back/service/EmailService.java
gamerecs-back/src/test/java/com/gamerecs/back/service/EmailServiceTest.java

Enhance email verification process with comprehensive tests and error handling
gamerecs-back/src/main/java/com/gamerecs/back/controller/UserController.java
gamerecs-back/src/main/java/com/gamerecs/back/exception/GlobalExceptionHandler.java
gamerecs-back/src/test/java/com/gamerecs/back/controller/UserControllerTest.java

Implement comprehensive email verification tests and enhance toast message handling
gamerecs-front/cypress/e2e/email-verification.cy.ts
gamerecs-front/cypress/support/commands.ts
gamerecs-front/package-lock.json
gamerecs-front/package.json
gamerecs-front/src/app/core/components/auth/email-verification.component.html
gamerecs-front/src/app/core/components/auth/email-verification.component.spec.ts
gamerecs-front/src/app/core/components/auth/email-verification.component.ts
gamerecs-front/src/app/core/components/auth/registration-form.component.spec.ts

Enhance user registration and email verification tests
gamerecs-back/src/test/java/com/gamerecs/back/service/UserServiceTest.java
gamerecs-front/src/app/core/components/auth/registration-form.component.spec.ts
gamerecs-front/src/app/core/services/auth.service.spec.ts

Refactor email verification logging in UserController and UserService
gamerecs-back/src/main/java/com/gamerecs/back/controller/UserController.java
gamerecs-back/src/main/java/com/gamerecs/back/service/UserService.java

Implement JWT Authentication and Security Filter
gamerecs-back/pom.xml
gamerecs-back/src/main/java/com/gamerecs/back/service/JwtService.java
gamerecs-back/src/main/java/com/gamerecs/back/config/JwtAuthenticationFilter.java
gamerecs-back/src/main/java/com/gamerecs/back/config/SecurityConfig.java
gamerecs-back/src/main/resources/application.yml

Verified JWT Authentication Filter and Service Implementation
- Confirmed proper implementation of JWT authentication components:
  - gamerecs-back/src/main/java/com/gamerecs/back/config/JwtAuthenticationFilter.java
  - gamerecs-back/src/main/java/com/gamerecs/back/service/JwtService.java

Verified JWT Dependencies in Backend
- Confirmed JWT dependencies (jjwt-api, jjwt-impl, jjwt-jackson) are already present in:
  - gamerecs-back/pom.xml

Implement comprehensive JwtService tests for token generation, extraction, and validation
- gamerecs-back/src/test/java/com/gamerecs/back/service/JwtServiceTest.java

Implement comprehensive JwtAuthenticationFilter tests
- gamerecs-back/src/test/java/com/gamerecs/back/config/JwtAuthenticationFilterTest.java
  - Added tests for token extraction and validation
  - Added tests for authentication process
  - Added tests for error handling scenarios
  - Added tests for security context management

Implement CustomUserDetailsService for JWT Authentication
gamerecs-back/src/main/java/com/gamerecs/back/service/CustomUserDetailsService.java
gamerecs-back/src/test/java/com/gamerecs/back/service/CustomUserDetailsServiceTest.java
- Implemented CustomUserDetailsService to integrate with Spring Security
- Added comprehensive unit tests for user details loading scenarios
- Integrated with existing UserRepository for username-based user lookup
- Added support for email verification status in user authentication
- Fixed JWT authentication dependency issue in application context

Implement Login Endpoint for JWT Authentication
gamerecs-back/src/main/java/com/gamerecs/back/controller/AuthenticationController.java
gamerecs-back/src/main/java/com/gamerecs/back/dto/LoginRequestDto.java
gamerecs-back/src/main/java/com/gamerecs/back/dto/LoginResponseDto.java

Implement LoginRequestDto for JWT Authentication
gamerecs-back/src/main/java/com/gamerecs/back/dto/LoginRequestDto.java

Enhance Login Endpoint with Comprehensive Error Handling and Tests
- gamerecs-back/src/main/java/com/gamerecs/back/controller/AuthenticationController.java
- gamerecs-back/src/test/java/com/gamerecs/back/controller/AuthenticationControllerTest.java

Refactor Login Endpoint to Use GlobalExceptionHandler
- gamerecs-back/src/main/java/com/gamerecs/back/controller/AuthenticationController.java
- gamerecs-back/src/test/java/com/gamerecs/back/controller/AuthenticationControllerTest.java
- gamerecs-back/src/main/java/com/gamerecs/back/exception/GlobalExceptionHandler.java

Verified LoginResponseDto Implementation
- Confirmed proper implementation of LoginResponseDto with JWT token and user details:
  - gamerecs-back/src/main/java/com/gamerecs/back/dto/LoginResponseDto.java

Enhance Authentication Tests and Error Handling for Unverified Email
- gamerecs-back/src/test/java/com/gamerecs/back/controller/AuthenticationControllerTest.java
- gamerecs-back/src/main/java/com/gamerecs/back/exception/GlobalExceptionHandler.java
  - Added test for login attempt with unverified email
  - Added DisabledException handler for unverified email accounts
  - Enhanced error messages and logging for account verification status

Implement Login Form Component
- Created login form component with username, password, and remember me fields
- Added form validation and error handling
- Implemented PrimeNG components and Tailwind CSS styling
  - gamerecs-front/src/app/core/components/auth/login-form.component.ts
  - gamerecs-front/src/app/core/components/auth/login-form.component.html
  - gamerecs-front/src/app/core/components/auth/login-form.component.css

Implement loading state and error handling for login form
- gamerecs-front/src/app/core/services/auth.service.ts
  - Added login interfaces and method
- gamerecs-front/src/app/core/components/auth/login-form.component.ts
  - Enhanced loading state handling during authentication
  - Added proper type safety and error handling

Implement comprehensive tests for login form component
- gamerecs-front/src/app/core/components/auth/login-form.component.spec.ts
  - Added tests for form validation
  - Added tests for form submission
  - Added tests for error handling
  - Added tests for loading state
  - Added tests for toast messages
