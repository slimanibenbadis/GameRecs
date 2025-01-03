- Initial commit for GameRecs application, including backend and frontend setup. Added Docker configurations, environment variables, and initial database schema. Implemented basic Spring Boot application structure with OpenAPI support. Frontend built with Angular, including routing and health check component. Configured ESLint and Tailwind CSS for styling. Added test utilities and mock store for NgRx state management.     
  - .env.example
  - .gitignore
  - docker-compose.yml
  - gamerecs-back/.gitattributes
  - gamerecs-back/.gitignore
  - gamerecs-back/.mvn/wrapper/maven-wrapper.properties
  - gamerecs-back/Dockerfile
  - gamerecs-back/mvnw
  - gamerecs-back/mvnw.cmd
  - gamerecs-back/pom.xml
  - gamerecs-back/src/main/java/com/gamerecs/back/GamerecsBackApplication.java
  - gamerecs-back/src/main/java/com/gamerecs/back/config/OpenApiConfig.java
  - gamerecs-back/src/main/resources/application.yml
  - gamerecs-back/src/main/resources/db/migration/V1__Initial_schema.sql
  - gamerecs-back/src/main/resources/logback-spring.xml
  - gamerecs-back/src/test/java/com/gamerecs/back/GamerecsBackApplicationTests.java
  - gamerecs-back/src/test/java/com/gamerecs/back/config/BaseTest.java
  - gamerecs-back/src/test/java/com/gamerecs/back/config/TestConfig.java
  - gamerecs-back/src/test/java/com/gamerecs/back/util/BaseIntegrationTest.java
  - gamerecs-back/src/test/java/com/gamerecs/back/util/BaseUnitTest.java
  - gamerecs-front/.editorconfig
  - gamerecs-front/.eslintrc.json
  - gamerecs-front/.gitignore
  - gamerecs-front/Dockerfile
  - gamerecs-front/README.md
  - gamerecs-front/angular.json
  - gamerecs-front/karma.conf.js
  - gamerecs-front/nginx.conf
  - gamerecs-front/package-lock.json
  - gamerecs-front/package.json
  - gamerecs-front/public/favicon.ico
  - gamerecs-front/src/app/app.component.css
  - gamerecs-front/src/app/app.component.html
  - gamerecs-front/src/app/app.component.spec.ts
  - gamerecs-front/src/app/app.component.ts
  - gamerecs-front/src/app/app.config.ts
  - gamerecs-front/src/app/app.routes.ts
  - gamerecs-front/src/app/core/components/health/health.component.ts
  - gamerecs-front/src/app/core/services/health.service.ts
  - gamerecs-front/src/app/testing/mock-store.ts
  - gamerecs-front/src/app/testing/test-utils.ts
  - gamerecs-front/src/index.html
  - gamerecs-front/src/main.ts
  - gamerecs-front/src/styles.css
  - gamerecs-front/src/test.ts
  - gamerecs-front/tailwind.config.js
  - gamerecs-front/tsconfig.app.json
  - gamerecs-front/tsconfig.json
  - gamerecs-front/tsconfig.spec.json

- Enhanced health check functionality and added comprehensive logging for local development setup verification. Updated health service to use proper typing and improved error handling. Added detailed console logging for debugging component communication.
  - gamerecs-front/src/app/core/components/health/health.component.ts
  - gamerecs-front/src/app/core/services/health.service.ts

- Fixed backend Dockerfile base image configuration to use a more stable Eclipse Temurin JRE tag, resolving Docker build issues.
  - gamerecs-back/Dockerfile

- Updated frontend Dockerfile to handle platform-specific dependencies correctly, fixing esbuild compatibility issues between Windows development and Linux container environments.
  - gamerecs-front/Dockerfile

- Added comprehensive local development setup verification process. Documented manual steps for environment setup, starting Docker services, and verifying service health. Enhanced logging in health checks for better debugging capabilities.
  - .env
  - docker-compose.yml
  - gamerecs-front/src/app/core/components/health/health.component.ts
  - gamerecs-front/src/app/core/services/health.service.ts
  - gamerecs-back/src/main/resources/application-dev.yml

- Fixed frontend container configuration in docker-compose.yml to resolve empty server response. Removed unnecessary volume mounts, added proper health checks and logging configuration, and ensured proper service dependencies.
  - docker-compose.yml

- Enhanced nginx configuration and frontend container setup to resolve empty server response. Added proper error logging, updated security headers, fixed volume mounts, and improved nginx configuration for better debugging capabilities.
  - gamerecs-front/nginx.conf
  - docker-compose.yml

- Performed complete local development setup verification. All services are running and healthy, with backend health checks passing successfully. Frontend container is running but health check endpoint needs investigation.
  - docker-compose.yml
  - gamerecs-front/src/app/core/components/health/health.component.ts
  - gamerecs-front/src/app/core/services/health.service.ts
  - gamerecs-back/src/main/resources/application-dev.yml
  - .env

- Fixed frontend health check issues by implementing comprehensive health monitoring. Updated health service to check both frontend and backend status, enhanced health component UI with detailed status display, configured proper CORS and proxy settings in nginx, and ensured health check dependencies in Docker.
  - gamerecs-front/src/app/core/services/health.service.ts
  - gamerecs-front/src/app/core/components/health/health.component.ts
  - gamerecs-front/nginx.conf
  - gamerecs-front/Dockerfile

- Updated ESLint configuration for less strict but maintainable Angular development. Changed error levels to warnings, added Angular-specific rules, improved TypeScript configuration, and added console logging allowances for development. Updated package.json with Angular ESLint dependencies.
  - gamerecs-front/.eslintrc.json
  - gamerecs-front/package.json

- Fixed test configuration by adding H2 in-memory database for testing. Added test configuration file and disabled Flyway for tests.
  - gamerecs-back/src/test/resources/application-test.yml
  - gamerecs-back/pom.xml

- Added SonarQube configuration for code quality analysis. Set up local SonarQube server with Docker and configured both backend and frontend for analysis.
  - docker-compose.yml
  - gamerecs-back/pom.xml
  - gamerecs-front/sonar-project.properties

- Enhanced code coverage configuration for SonarQube analysis:
  - Added JaCoCo plugin to backend for Java code coverage reporting
  - Updated frontend test configuration for LCOV report generation
  - Configured Karma for ChromeHeadless testing and coverage thresholds
  - gamerecs-back/pom.xml
  - gamerecs-front/karma.conf.js
  - gamerecs-front/angular.json

- Fixed JaCoCo report path configuration to match actual report location:
  - Bound JaCoCo report generation to verify phase
  - Added sonar.jacoco.reportPaths property
  - Set sonar.language to java explicitly
  - gamerecs-back/pom.xml

- Enhanced SonarQube coverage configuration:
  - Added explicit JaCoCo coverage plugin configuration
  - Enabled dynamic analysis with report reuse
  - gamerecs-back/pom.xml

- Fixed JaCoCo report path configuration to match actual report location:
  - Bound JaCoCo report generation to verify phase
  - Added sonar.jacoco.reportPaths property
  - Set sonar.language to java explicitly
  - gamerecs-back/pom.xml

- Completed SonarQube local server setup:
  - Added sonar script to frontend package.json for running analysis
  - Verified SonarQube configuration in docker-compose.yml
  - gamerecs-front/package.json

- Configured SonarQube quality gates according to PRD requirements:
  - Set up quality gate with 80% minimum coverage
  - Configured code smells, security, and maintainability metrics
  - Added project-specific configurations for both frontend and backend
  - docker-compose.yml
  - gamerecs-back/pom.xml
  - gamerecs-front/sonar-project.properties

- Fixed backend code coverage reporting in SonarQube:
  - Enhanced JaCoCo configuration with proper XML report generation
  - Added coverage exclusions for config, model, and dto classes
  - Configured coverage check rule to enforce 80% minimum coverage
  - Added SonarQube properties for coverage analysis
  - gamerecs-back/pom.xml

- Removed deprecated JaCoCo configuration:
  - Removed deprecated sonar.jacoco.reportPath property
  - Updated to use only XML report path as per latest SonarQube recommendations
  - gamerecs-back/pom.xml

- Switched to Maven-based sonar-jacoco plugin:
  - Added sonar-jacoco-plugin as Maven dependency
  - Added sonar.core.codeCoveragePlugin property
  - Removed manual plugin installation from docker-compose.yml
  - gamerecs-back/pom.xml
  - docker-compose.yml

- Added GitHub Actions workflow for backend CI/CD pipeline:
  - Configured build, test, and code quality checks with SonarQube
  - Set up Docker image build and push for main and develop branches
  - Added manual trigger capability and branch-specific triggers
  - Configured H2 database for testing environment
  - .github/workflows/backend-ci.yml

- Updated backend CI/CD workflow to use SonarCloud instead of local SonarQube:
  - Added SonarCloud specific configuration
  - Added caching for SonarCloud packages
  - Configured organization and project key as secrets
  - .github/workflows/backend-ci.yml

- Added GitHub Actions workflow for frontend CI/CD pipeline:
  - Configured build, test, lint, and Docker image build steps
  - Set up manual trigger capability and branch-specific triggers
  - Added caching for npm dependencies and Docker layers
  - .github/workflows/frontend-ci.yml

- Added comprehensive CORS configuration:
  - Created WebConfig class for CORS configuration with proper logging
  - Added CORS properties to application configuration files
  - Configured allowed origins, methods, headers, and credentials
  - gamerecs-back/src/main/java/com/gamerecs/back/config/WebConfig.java
  - gamerecs-back/src/main/resources/application.yml
  - gamerecs-back/src/test/resources/application-test.yml

- Added Spring Security configuration to allow Swagger UI access:
  - Created SecurityConfig class with proper security chain configuration
  - Whitelisted Swagger UI and OpenAPI endpoints
  - Disabled CSRF for API endpoints
  - Allowed access to actuator endpoints
  - gamerecs-back/src/main/java/com/gamerecs/back/config/SecurityConfig.java
