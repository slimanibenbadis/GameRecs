Commit: a7346e59b308da4c2c4eca47ca85a2adf527d9a9
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

Commit: 22f4aa5875d6ed5a4730cd63992e203c5dc598d4
Refactor and enhance GameRecs application setup. Improved health check functionality with detailed logging and error handling. Updated Docker configurations for both frontend and backend, resolving build issues and ensuring proper service dependencies. Enhanced nginx configuration for better debugging and security. Documented local development setup verification process. Fixed health check issues and improved UI for health status display.

Development_History_Log.md
docker-compose.yml
gamerecs-back/Dockerfile
gamerecs-front/Dockerfile
gamerecs-front/nginx.conf
gamerecs-front/src/app/core/components/health/health.component.ts
gamerecs-front/src/app/core/services/health.service.ts

Commit: 23eb6a7276162048644ecdd54302bdcb6b64cc04
Update ESLint configuration for Angular development. Enhanced rules for maintainability by adjusting error levels to warnings, adding Angular-specific rules, and improving TypeScript settings. Updated package.json to include Angular ESLint dependencies.

Development_History_Log.md
gamerecs-front/.eslintrc.json
gamerecs-front/package-lock.json
gamerecs-front/package.json

Commit: a20c3098e2f84c9f15dc6f84d83fbd949f2403fb
Enhance test configuration by adding H2 in-memory database for testing. Introduced application-test.yml for test-specific settings and disabled Flyway migrations during tests. Updated pom.xml to include H2 dependency for test scope.

Development_History_Log.md
gamerecs-back/pom.xml
gamerecs-back/src/test/resources/application-test.yml

Commit: d187ea4d928e21797f56e760c79a5c065fea9082
Integrate SonarQube for code quality analysis and enhance coverage reporting. Added SonarQube configuration in docker-compose and pom.xml, including JaCoCo plugin for backend coverage. Updated frontend configurations for LCOV reporting and Karma testing. Improved code coverage thresholds and quality gates. Fixed report paths and deprecated configurations. Added sonar-project.properties for frontend analysis. Updated .gitignore to include SonarQube directories.

.gitignore
Development_History_Log.md
docker-compose.yml
gamerecs-back/pom.xml
gamerecs-front/angular.json
gamerecs-front/karma.conf.js
gamerecs-front/package.json
gamerecs-front/sonar-project.properties

Commit: 01dc4fd1e4f477a16c8e2e6d1f4f535e3f86233d
Create ci-cd.yml
.github/workflows/ci-cd.yml

Commit: 2a4f2b4056021017ec9f07cce9798fc3b9767744
Implement backend CI/CD pipeline with GitHub Actions. Added workflow for build, test, and SonarQube analysis. Configured Docker image build and push for main and develop branches. Removed deprecated ci-cd.yml file.

.github/workflows/backend-ci.yml
.github/workflows/ci-cd.yml
Development_History_Log.md

Commit: 3e0b5110ba227f37c6ed4f0f2445bd0d7b8b44d3
Update backend CI/CD workflow to integrate SonarCloud for code quality analysis. Added caching for SonarCloud packages and configured project-specific secrets. Removed local SonarQube references and updated analysis commands to use SonarCloud settings.

.github/workflows/backend-ci.yml
Development_History_Log.md

Commit: 26610481dbe05299dbb696b8ca4bfa9981d45945
Enhance CI/CD workflows by adding a new GitHub Actions pipeline for frontend, including build, test, lint, and Docker image build steps. Configured manual and branch-specific triggers, and implemented caching for npm dependencies and Docker layers. Updated backend CI/CD workflow to correct SonarCloud token usage.

.github/workflows/backend-ci.yml
.github/workflows/frontend-ci.yml
Development_History_Log.md

Commit: a1a51eca05ba7f697f577994b93fbd03cdf18503
Implement comprehensive CORS and Spring Security configurations. Added WebConfig for CORS settings with logging and allowed origins, methods, and headers. Introduced SecurityConfig to permit access to Swagger UI and actuator endpoints, while disabling CSRF for API requests. Updated application and test configuration files to include CORS properties.

Development_History_Log.md
gamerecs-back/src/main/java/com/gamerecs/back/config/SecurityConfig.java
gamerecs-back/src/main/java/com/gamerecs/back/config/WebConfig.java
gamerecs-back/src/main/resources/application.yml
gamerecs-back/src/test/resources/application-test.yml

Commit: c7dd89421dc21c518ee3dd37c82dee9f47e79091
Implement User entity with JPA annotations and validation constraints. Added fields for user details including username, email, and timestamps for join date and last login. Utilized Lombok for boilerplate reduction and ensured validation for critical fields.

Development_History_Log.md
gamerecs-back/src/main/java/com/gamerecs/back/model/User.java

Commit: 3c687968d0060dbcd2d9321fcb75a80683990e27
Enhance Security Configuration and Testing for Swagger UI and Actuator Endpoints

- Added comprehensive tests for SecurityConfig, validating access to Swagger UI and actuator endpoints.
- Implemented custom AuthenticationEntryPoint in SecurityConfig to return 401 Unauthorized for unauthenticated requests.
- Updated SecurityConfig to allow access to necessary Swagger UI resources and modified tests to ensure correct endpoint handling.
- Enhanced test configuration with detailed logging and assertions for various response codes in Swagger UI tests.
- Updated application-test.yml for improved SpringDoc OpenAPI configuration and debugging.

These changes improve security handling and ensure proper access control for API endpoints.

Development_History_Log.md
gamerecs-back/src/main/java/com/gamerecs/back/config/SecurityConfig.java
gamerecs-back/src/test/java/com/gamerecs/back/config/SecurityConfigTest.java
gamerecs-back/src/test/resources/application-test.yml

Commit: 18955e8282a040b7905769b799dc9684375bc9f7
Enhance CORS and Security Configuration with Comprehensive Testing

- Added detailed tests for CORS configuration in WebConfigTest, validating allowed origins, HTTP methods, headers, max age, and rejection of non-allowed origins.
- Improved SecurityConfig by adding a CorsConfigurationSource bean for proper CORS handling and configuring security to allow OPTIONS requests and test endpoints.
- Replaced wildcard origin pattern with specific allowed origins for enhanced security.
- Updated logging for better debugging and test execution visibility.

These changes ensure robust CORS handling and strengthen security measures for API endpoints.

Development_History_Log.md
gamerecs-back/pom.xml
gamerecs-back/src/main/java/com/gamerecs/back/config/SecurityConfig.java
gamerecs-back/src/test/java/com/gamerecs/back/config/WebConfigTest.java

Commit: b72ec8f1d1ffaf833661666c5566a43edd84468c
Implement UserRepository for User entity database operations

- Created UserRepository interface extending JpaRepository for standard CRUD operations.
- Added custom query methods for finding users by email and username.
- Implemented existence check methods for email and username to enhance user management capabilities.

Development_History_Log.md
gamerecs-back/src/main/java/com/gamerecs/back/repository/UserRepository.java

Commit: 593297b14360171aaaef39deccc960b004c943dd
Implement UserService for user registration with validation and logging

- Created UserService class to handle user registration logic.
- Added registerUser method with email and username uniqueness validation.
- Implemented password hashing using PasswordEncoder.
- Included comprehensive logging for registration attempts and outcomes.
- Updated Development History Log to reflect these changes.

Development_History_Log.md
gamerecs-back/src/main/java/com/gamerecs/back/service/UserService.java

Commit: 6625e841152c717e08fa5fd87a8708e6b730b68c
Implement User Registration functionality with DTO and Controller

- Created UserRegistrationDto for handling user registration requests with validation constraints.
- Implemented UserController with a registration endpoint following REST principles, including error handling and logging.
- Updated Development History Log to reflect the addition of user registration features.

Development_History_Log.md
gamerecs-back/src/main/java/com/gamerecs/back/controller/UserController.java
gamerecs-back/src/main/java/com/gamerecs/back/dto/UserRegistrationDto.java

Commit: c54ca0b20118b62795e22209cbbcfd9e9b565433
Enhance user registration process with comprehensive input validation

- Added GlobalExceptionHandler for centralized validation error handling and logging.
- Improved validation annotations in User entity and UserRegistrationDto.
- Confirmed usage of @Valid annotation in UserController for the registration endpoint.
- Updated Development History Log to reflect these enhancements.

Development_History_Log.md
gamerecs-back/src/main/java/com/gamerecs/back/exception/GlobalExceptionHandler.java

Commit: f7b133f7ae65e1168f5b03fec6f56809431d355a
Enhance User Component Testing and Security Configuration

- Added comprehensive tests for User-related components, including UserTest, UserRepositoryTest, UserServiceTest, and UserControllerTest, with improved logging and coverage.
- Fixed missing PasswordEncoder bean in SecurityConfig, allowing proper password hashing and added /api/users/register to permitted endpoints.
- Updated UserTest validation expectations and checks for better accuracy and readability.
- Enhanced UserServiceTest to verify password encoding during user registration.

These changes improve the robustness of user-related functionalities and ensure secure password handling.

Development_History_Log.md
gamerecs-back/src/main/java/com/gamerecs/back/config/SecurityConfig.java
gamerecs-back/src/test/java/com/gamerecs/back/controller/UserControllerTest.java
gamerecs-back/src/test/java/com/gamerecs/back/model/UserTest.java
gamerecs-back/src/test/java/com/gamerecs/back/repository/UserRepositoryTest.java
gamerecs-back/src/test/java/com/gamerecs/back/service/UserServiceTest.java

Commit: 92254fa667f4b876439803b50e33a89bfed8fcab
Update springdoc-openapi dependency version to 2.7.0 in pom.xml

gamerecs-back/pom.xml

Commit: d8a85096f613eb9203c391f1ed2047f0d17ac920
Remove linebreak-style rule from ESLint configuration for improved cross-platform compatibility

gamerecs-front/.eslintrc.json

Commit: a7e09ac51f1c7cbfdec1ba0e570cc1746bb39969
Implement user registration form with validation and UI enhancements

- Created a new Registration Form component with reactive form implementation, including validation for username, email, password, and password confirmation.
- Designed the UI using Tailwind CSS and PrimeNG components, adhering to PRD design guidelines.
- Implemented a dark mode feature with proper theme configuration and color palette adjustments.
- Enhanced global styles to support dark mode and integrated Google Fonts for improved typography.
- Fixed various CSS build errors related to Tailwind configuration and ensured proper font utility classes are applied.
- Updated app routing to include the registration form and set it as the default landing page.
- Added comprehensive logging for form actions and improved error handling for user feedback.

These changes enhance the user experience during registration and ensure robust input validation.

Development_History_Log.md
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

Commit: 337c2436e4c30fe405113d159b716acd4611dca4
Enhance Registration Form validation and user experience

- Implemented comprehensive client-side validation for the registration form, including specific patterns and rules for username, email, password, bio, and profile picture URL.
- Added detailed error messages using the getErrorMessage helper method to provide user-friendly feedback.
- Improved validation UX by displaying errors only for touched fields, enhancing user interaction.
- Updated Development History Log to reflect these enhancements.

These changes significantly improve the robustness and usability of the registration process.

Development_History_Log.md
gamerecs-front/src/app/core/components/auth/registration-form.component.html
gamerecs-front/src/app/core/components/auth/registration-form.component.ts

Commit: 643132074ff06d49e4c9a97f7933d520f496fdb7
Fix SonarQube test coverage configuration and improve Angular test settings

- Updated sonar-project.properties to refine test inclusions and exclusions, ensuring accurate coverage reporting.
- Added coverage exclusions for non-testable files and set a minimum coverage requirement of 80% in quality gate settings.
- Enhanced Karma configuration with proper coverage thresholds and reporting adjustments.
- Updated Angular test configuration for better coverage tracking and reporting.

These changes enhance the project's test coverage management and ensure compliance with quality standards.

Development_History_Log.md
gamerecs-front/angular.json
gamerecs-front/karma.conf.js
gamerecs-front/sonar-project.properties

Commit: 961823fb61478b5509f005b653a035b49c83ba83
Refactor AppComponent tests to enhance dark mode functionality

- Removed default title tests that caused TypeScript errors.
- Added comprehensive tests for dark mode, including system preference changes.
- Implemented proper mocking for window.matchMedia to simulate dark mode behavior.
- Updated Development History Log to reflect these changes.

Development_History_Log.md
gamerecs-front/src/app/app.component.spec.ts

Commit: 8e851f076ad268c7338393ba9f13ef07f4c6e3e0
Implement User Registration Service and API Communication Enhancements

- Developed AuthService for user registration, integrating API calls and error handling.
- Updated RegistrationFormComponent to utilize AuthService for user registration.
- Configured proxy settings for API communication with the backend server.
- Enhanced Angular development server configuration to ensure proper API routing and CORS settings.
- Added HttpClient configuration to app.config.ts for improved HTTP handling.
- Updated Development History Log to document these significant changes.

These updates streamline the user registration process and improve API interaction, enhancing overall application functionality.

Development_History_Log.md
gamerecs-front/angular.json
gamerecs-front/package.json
gamerecs-front/proxy.conf.json
gamerecs-front/src/app/app.config.ts
gamerecs-front/src/app/core/components/auth/registration-form.component.ts
gamerecs-front/src/app/core/services/auth.service.ts

Commit: 5fa80a00ea254a1bd2d2b294d5eb0f3d4e8f7eca
Enhance User Registration API Communication

- Added missing /api location block in nginx.conf to properly proxy API requests to the backend.
- Configured CORS headers and OPTIONS request handling for API routes to improve cross-origin requests.
- Ensured consistent proxy configuration between API and actuator endpoints for better service integration.
- Updated Development History Log to document these changes.

These updates streamline API communication and enhance the overall functionality of the user registration process.

Development_History_Log.md
gamerecs-front/nginx.conf

Commit: 91d9fbc55c8ee2ef940daddd4a98d98bfb982203
Enhance user registration error handling and improve frontend experience

- Implemented standardized ApiError class for consistent error responses across the application.
- Updated GlobalExceptionHandler to utilize ApiError for centralized error handling, replacing plain text responses with structured JSON.
- Refactored UserController to streamline error handling during user registration, ensuring proper error message propagation to the frontend.
- Enhanced error handling in AuthService and RegistrationFormComponent to display user-friendly error messages using PrimeNG Toast.
- Fixed loading state management in the registration form to ensure proper user feedback during registration attempts.

These changes significantly improve the robustness and user experience of the registration process.

Development_History_Log.md
gamerecs-back/src/main/java/com/gamerecs/back/controller/UserController.java
gamerecs-back/src/main/java/com/gamerecs/back/exception/ApiError.java
gamerecs-back/src/main/java/com/gamerecs/back/exception/GlobalExceptionHandler.java
gamerecs-front/src/app/core/components/auth/registration-form.component.ts
gamerecs-front/src/app/core/services/auth.service.ts

Commit: e36afc2a2a419e3ccf42503032fb08ab8c3d8116
Refactor UserControllerTest for Consistent JSON Error Responses

- Updated tests to expect structured JSON responses for various validation errors in UserController.
- Ensured all error handling tests align with the ApiError format, including status, message, timestamp, and errors fields.
- Improved assertions for existing tests to validate error responses for email, username, and password issues.
- Enhanced overall test coverage and consistency with GlobalExceptionHandler's error handling.

These changes improve the reliability of error handling in user registration tests.

Development_History_Log.md
gamerecs-back/src/test/java/com/gamerecs/back/controller/UserControllerTest.java

Commit: 0e494cd3a0b36b09fcf6e7abc368e75d995444f0
Enhance Registration Form and AuthService Testing

- Added comprehensive unit tests for the Registration Form Component, covering form validation, submission, and error handling scenarios.
- Implemented unit tests for AuthService, ensuring robust testing of user registration functionality, including successful registration and error handling.
- Updated package.json to include Cypress for end-to-end testing and added relevant scripts for running Cypress tests.
- Enhanced Development History Log to document the addition of new tests and improvements in the registration process.

These changes significantly improve the test coverage and reliability of the user registration features.

Development_History_Log.md
gamerecs-front/package-lock.json
gamerecs-front/package.json
gamerecs-front/src/app/core/components/auth/registration-form.component.spec.ts
gamerecs-front/src/app/core/services/auth.service.spec.ts

Commit: acab2b4559132a3f9104bbe1f8a5e74c32edc7c3
Enhance End-to-End Testing for Registration Form

- Added comprehensive Cypress end-to-end tests for the registration form, including form validation, submission, error handling, and loading states.
- Created custom Cypress commands for filling the registration form and checking toast messages.
- Configured Cypress environment settings and TypeScript support for testing.
- Updated Angular configuration to support Cypress-specific builds and file replacements.
- Enhanced the registration form component to utilize PrimeNG Toast for user feedback on registration success and errors.
- Improved the Development History Log to document these significant testing enhancements.

These changes significantly improve the reliability and coverage of the user registration process through automated testing.

Development_History_Log.md
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

Commit: abf57a6250c209c9d34612273ac1d09fd42717fe
Implement email verification feature for user registration

- Added email verification functionality to the User entity, including a new migration to add the email_verified column and an index for performance.
- Developed EmailService to handle sending verification emails, utilizing Spring Mail and Thymeleaf for email templating.
- Integrated Mailhog for local email testing, updating docker-compose for SMTP settings.
- Created VerificationToken entity and repository for managing verification tokens, including methods for token lookup and expiration handling.
- Enhanced UserController with a new /verify endpoint for processing email verification requests.
- Updated UserService to generate and send verification tokens upon user registration, ensuring email verification logic is robust and user-friendly.
- Configured application.yml for email settings and added a verification email template.
- Reviewed and validated the entire email verification implementation for correctness and error handling.

These changes significantly enhance the user registration process by ensuring users verify their email addresses, improving account security and user engagement.

Development_History_Log.md
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

Commit: 5bee80b3b5193dfd1878aa71ef9a81072f813332
Refactor Security Configuration and Enhance User Service Tests

- Introduced a new constant for actuator endpoints in SecurityConfig to improve maintainability and readability.
- Updated SecurityConfigTest to include detailed assertions for actuator endpoint access, ensuring proper response content and status checks.
- Enhanced UserServiceTest by adding mocks for EmailService and VerificationTokenRepository, improving the user registration flow with email verification.
- Updated user registration tests to include verification token generation and email sending, ensuring comprehensive coverage of the registration process.
- Configured application-test.yml for local mail server settings to facilitate email testing during integration tests.

These changes improve the security configuration and enhance the robustness of user registration tests, ensuring better maintainability and functionality.

gamerecs-back/src/main/java/com/gamerecs/back/config/SecurityConfig.java
gamerecs-back/src/test/java/com/gamerecs/back/config/SecurityConfigTest.java
gamerecs-back/src/test/java/com/gamerecs/back/service/UserServiceTest.java
gamerecs-back/src/test/resources/application-test.yml

Commit: 4ca6e31dc8cf3e51c5838d41ace1d39375722ef5
Implement email verification feature in frontend

- Enhanced registration success message to inform users about email verification.
- Added EmailVerificationComponent for handling email verification via token.
- Integrated email verification route in app routing.
- Developed verifyEmail method in AuthService for backend communication.
- Created comprehensive tests for EmailVerificationComponent and AuthService.
- Updated Development History Log to reflect these changes.

These updates improve user experience by ensuring email verification is clearly communicated and handled effectively during the registration process.

Development_History_Log.md
gamerecs-front/src/app/app.routes.ts
gamerecs-front/src/app/core/components/auth/email-verification.component.spec.ts
gamerecs-front/src/app/core/components/auth/email-verification.component.ts
gamerecs-front/src/app/core/components/auth/registration-form.component.spec.ts
gamerecs-front/src/app/core/components/auth/registration-form.component.ts
gamerecs-front/src/app/core/services/auth.service.spec.ts
gamerecs-front/src/app/core/services/auth.service.ts

Commit: 6443bbf5db8925a75815c59b62aca55544c34900
Update dependencies and refine TypeScript configurations

- Upgraded @types/jasmine from ~5.1.0 to ^5.1.5 in package.json and package-lock.json for improved type definitions.
- Excluded cypress.config.ts from TypeScript compilation in tsconfig.json to streamline the build process.
- Removed unnecessary type references for jasmine in email-verification.component.spec.ts and auth.service.spec.ts to clean up test files.

These changes enhance the development environment by ensuring up-to-date type definitions and optimizing TypeScript configurations.

gamerecs-front/cypress/tsconfig.json
gamerecs-front/package-lock.json
gamerecs-front/package.json
gamerecs-front/src/app/core/components/auth/email-verification.component.spec.ts
gamerecs-front/src/app/core/services/auth.service.spec.ts
gamerecs-front/tsconfig.json

Commit: ac438707627712352d3a62b7744ff86a185460da
Enhance email verification process and UI

- Improved registration success message to include a 24-hour expiration notice for email verification.
- Fixed email verification URL routing and updated backend to align with new frontend patterns.
- Enhanced email verification UI with better state handling, visual design, and more descriptive success/error messages.
- Separated email verification component template into a dedicated HTML file for better organization.
- Updated tests for email verification component and registration form to reflect changes in messaging and functionality.

These updates significantly improve the user experience during email verification and ensure clearer communication regarding verification status.

Development_History_Log.md
gamerecs-back/src/main/java/com/gamerecs/back/service/EmailService.java
gamerecs-front/src/app/app.routes.ts
gamerecs-front/src/app/core/components/auth/email-verification.component.html
gamerecs-front/src/app/core/components/auth/email-verification.component.spec.ts
gamerecs-front/src/app/core/components/auth/email-verification.component.ts
gamerecs-front/src/app/core/components/auth/registration-form.component.spec.ts
gamerecs-front/src/app/core/components/auth/registration-form.component.ts

Commit: f08a2afc8d4f0c3886406e1ae7a598deb4b970ae
Enhance EmailService with input validation and comprehensive tests

- Refactored EmailService to include input validation for email, username, and token parameters, throwing IllegalArgumentException for invalid inputs.
- Improved error handling in sendVerificationEmail method to log failures and throw runtime exceptions on messaging errors.
- Added EmailServiceTest class with extensive unit tests covering email sending functionality, token generation, and error handling scenarios.
- Implemented proper mocking of JavaMailSender and TemplateEngine in tests, ensuring robust coverage of email service operations.

These changes improve the reliability and maintainability of the email verification process, ensuring better error handling and comprehensive test coverage.

Development_History_Log.md
gamerecs-back/src/main/java/com/gamerecs/back/service/EmailService.java
gamerecs-back/src/test/java/com/gamerecs/back/service/EmailServiceTest.java

Commit: 6cf241a8b0caf1860af4b4465ee9371f4aa0dd88
Enhance email verification process with comprehensive tests and error handling

- Updated UserController to enforce required token parameter for email verification requests.
- Added a new GlobalExceptionHandler method to handle missing request parameters, returning structured error responses.
- Implemented extensive integration tests for the email verification endpoint, covering successful verification, invalid tokens, missing parameters, and unexpected errors.
- Improved logging and assertions in tests to ensure consistent error response formats.

These changes improve the reliability and user experience of the email verification process, ensuring better error handling and comprehensive test coverage.

Development_History_Log.md
gamerecs-back/src/main/java/com/gamerecs/back/controller/UserController.java
gamerecs-back/src/main/java/com/gamerecs/back/exception/GlobalExceptionHandler.java
gamerecs-back/src/test/java/com/gamerecs/back/controller/UserControllerTest.java

Commit: 12e358f7acc338c5f66bed4d57aca0f1d8f02ea9
Implement comprehensive email verification tests and enhance toast message handling

- Added extensive end-to-end tests for the email verification process, covering valid and invalid token scenarios, expired tokens, and server errors.
- Improved toast message handling in the EmailVerificationComponent to ensure consistent user feedback during the verification process.
- Updated the email verification component's HTML to include a unique key for the toast messages, enhancing message management.
- Refactored Cypress commands for better toast message verification, ensuring robust testing of user notifications.

These changes significantly enhance the reliability and user experience of the email verification feature, ensuring thorough testing and improved feedback mechanisms.

Development_History_Log.md
gamerecs-front/cypress/e2e/email-verification.cy.ts
gamerecs-front/cypress/support/commands.ts
gamerecs-front/package-lock.json
gamerecs-front/package.json
gamerecs-front/src/app/core/components/auth/email-verification.component.html
gamerecs-front/src/app/core/components/auth/email-verification.component.spec.ts
gamerecs-front/src/app/core/components/auth/email-verification.component.ts
gamerecs-front/src/app/core/components/auth/registration-form.component.spec.ts

Commit: 9041db83978e77c334719ef5fd4e85093118d031
Enhance user registration and email verification tests

- Added comprehensive unit tests for email verification in UserService, covering successful verification, invalid tokens, and expired tokens.
- Improved error handling in the registration form component tests, ensuring proper feedback for registration errors.
- Enhanced test coverage for the registration form, including validation messages for username, email, password, bio, and profile picture URL.
- Implemented error handling for parsing error responses in AuthService tests, ensuring robust error management during registration.

These updates significantly improve the reliability and user experience of the registration and email verification processes, ensuring thorough testing and better error handling.

gamerecs-back/src/test/java/com/gamerecs/back/service/UserServiceTest.java
gamerecs-front/src/app/core/components/auth/registration-form.component.spec.ts
gamerecs-front/src/app/core/services/auth.service.spec.ts

Commit: de9d30b8a1a23fc202232723ec58dfc3e80b688f
Refactor email verification logging in UserController and UserService

- Updated logging statements in UserController and UserService to remove token details for enhanced security and privacy.
- Simplified log messages for email verification success and failure scenarios, focusing on the action rather than the token value.

These changes improve the security of the email verification process by ensuring sensitive information is not logged, while maintaining clear logging for operational monitoring.

gamerecs-back/src/main/java/com/gamerecs/back/controller/UserController.java
gamerecs-back/src/main/java/com/gamerecs/back/service/UserService.java

Commit: 02df640f8b9e66670cd2da8ba3a5c34bb815c7fb
Changelog rename

Changelog.md
Development_History_Log.md

Commit: 7fe08239480b41117cb13d637819522302846824
Implement JWT Authentication and Security Enhancements

- Added JWT authentication support with a new JwtService for token generation and validation.
- Implemented JwtAuthenticationFilter to validate JWT tokens in incoming requests.
- Created CustomUserDetailsService to load user details for authentication.
- Updated SecurityConfig to integrate JWT authentication and configure security settings.
- Added comprehensive unit tests for JwtService, JwtAuthenticationFilter, and CustomUserDetailsService.
- Verified JWT dependencies in pom.xml and updated application.yml for JWT configuration.

These changes enhance the security of the application by implementing robust JWT authentication, improving user authentication processes, and ensuring comprehensive test coverage for the new features.

Changelog.md
gamerecs-back/pom.xml
gamerecs-back/src/main/java/com/gamerecs/back/config/JwtAuthenticationFilter.java
gamerecs-back/src/main/java/com/gamerecs/back/config/SecurityConfig.java
gamerecs-back/src/main/java/com/gamerecs/back/service/CustomUserDetailsService.java
gamerecs-back/src/main/java/com/gamerecs/back/service/JwtService.java
gamerecs-back/src/main/resources/application.yml
gamerecs-back/src/test/java/com/gamerecs/back/config/JwtAuthenticationFilterTest.java
gamerecs-back/src/test/java/com/gamerecs/back/config/SecurityConfigTest.java
gamerecs-back/src/test/java/com/gamerecs/back/service/CustomUserDetailsServiceTest.java
gamerecs-back/src/test/java/com/gamerecs/back/service/JwtServiceTest.java

Commit: 2bfd331a0f508994fae57870ea309bf59f4a8348
Implement JWT Authentication Login Endpoint and Enhance Error Handling

- Introduced a new AuthenticationController to handle user login requests with JWT authentication.
- Created LoginRequestDto and LoginResponseDto for structured request and response handling.
- Enhanced error handling in the login process, including specific responses for unverified emails and invalid credentials.
- Implemented a GlobalExceptionHandler to manage exceptions and provide consistent error responses.
- Added comprehensive unit tests for the AuthenticationController to ensure robust functionality and error handling.

These changes improve the user authentication experience by providing a secure and user-friendly login process, along with detailed error feedback.

Changelog.md
gamerecs-back/src/main/java/com/gamerecs/back/controller/AuthenticationController.java
gamerecs-back/src/main/java/com/gamerecs/back/dto/LoginRequestDto.java
gamerecs-back/src/main/java/com/gamerecs/back/dto/LoginResponseDto.java
gamerecs-back/src/main/java/com/gamerecs/back/exception/GlobalExceptionHandler.java
gamerecs-back/src/test/java/com/gamerecs/back/config/JwtAuthenticationFilterTest.java
gamerecs-back/src/test/java/com/gamerecs/back/controller/AuthenticationControllerTest.java

Commit: c2eb61aac50bdac94db05d5f904166fdeb87e5e9
Add production environment configuration file

gamerecs-front/src/environments/environment.prod.ts

Commit: 9f1bc3b5ee4932a3f529825ab2926d7ffbe63866
Implement Login Form Component with Enhanced Error Handling and Comprehensive Tests

- Created a new login form component featuring username, password, and remember me fields, utilizing PrimeNG components and Tailwind CSS for styling.
- Added form validation and error handling to improve user experience during login attempts.
- Implemented loading state management during authentication processes.
- Developed comprehensive unit tests for the login form component, covering form validation, submission, error handling, and loading states.
- Enhanced the AuthService to include login functionality with structured request and response handling, along with improved error management for various scenarios.

These changes significantly enhance the user authentication experience by providing a secure, user-friendly login process with detailed feedback and robust testing coverage.

Changelog.md
gamerecs-front/src/app/core/components/auth/login-form.component.css
gamerecs-front/src/app/core/components/auth/login-form.component.html
gamerecs-front/src/app/core/components/auth/login-form.component.spec.ts
gamerecs-front/src/app/core/components/auth/login-form.component.ts
gamerecs-front/src/app/core/services/auth.service.spec.ts
gamerecs-front/src/app/core/services/auth.service.ts

Commit: 49938704cf898173631f2f92a0d4dda6cc54d4a9
Enhance AuthService with JWT Authentication and State Management

- Added isAuthenticated() method to check JWT token validity.
- Implemented JWT storage in localStorage/sessionStorage based on "Remember me" functionality.
- Enhanced authentication state management using BehaviorSubject for real-time updates.
- Updated ILoginResponse interface to align with backend changes.
- Verified logout functionality with comprehensive test coverage.
- Added extensive tests for authentication status checking and JWT storage.

These changes significantly improve the user authentication experience by providing robust state management and ensuring secure handling of authentication tokens.

Changelog.md
gamerecs-front/src/app/core/components/auth/login-form.component.spec.ts
gamerecs-front/src/app/core/services/auth.service.spec.ts
gamerecs-front/src/app/core/services/auth.service.ts

Commit: 5b01771ad6639487acdfeb49d3246a115a495ccd
Enhance Login Functionality and User Experience

- Verified integration of the login form with AuthService, ensuring proper error handling, loading states, and user feedback.
- Implemented navigation to the home page upon successful login and updated routes for login and registration.
- Improved network error handling in the login form to provide specific feedback for connection issues.
- Confirmed functionality of the "Remember Me" feature with localStorage/sessionStorage management.
- Fixed navigation issues in the login form related to the sign-up link and registration route.

These changes significantly enhance the user authentication experience by providing a seamless login process, improved error handling, and robust navigation.

Changelog.md
gamerecs-front/src/app/app.routes.ts
gamerecs-front/src/app/core/components/auth/email-verification.component.spec.ts
gamerecs-front/src/app/core/components/auth/email-verification.component.ts
gamerecs-front/src/app/core/components/auth/login-form.component.html
gamerecs-front/src/app/core/components/auth/login-form.component.ts

Commit: cbfe140de99802cf3cf70b1ea2a75cdabab962c9
Refactor Auth Components and Enhance Styling

- Updated styles for login and registration forms to improve UI consistency and responsiveness, utilizing Tailwind CSS for better design integration.
- Enhanced form components with improved error handling and user feedback, including placeholder text and validation messages.
- Integrated PrimeNG components for buttons and input fields, ensuring a cohesive look and feel across the application.
- Added comprehensive unit tests for login and registration components, ensuring robust functionality and error management.
- Improved toast message handling for user notifications during authentication processes.

These changes significantly enhance the user experience by providing a more polished and user-friendly interface for authentication.

gamerecs-front/src/app/core/components/auth/email-verification.component.spec.ts
gamerecs-front/src/app/core/components/auth/login-form.component.css
gamerecs-front/src/app/core/components/auth/login-form.component.html
gamerecs-front/src/app/core/components/auth/login-form.component.spec.ts
gamerecs-front/src/app/core/components/auth/registration-form.component.css
gamerecs-front/src/app/core/components/auth/registration-form.component.html
gamerecs-front/src/app/core/components/auth/registration-form.component.spec.ts
gamerecs-front/src/app/core/components/auth/registration-form.component.ts
gamerecs-front/src/styles.css
