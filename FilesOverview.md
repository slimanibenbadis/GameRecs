- **`app.component.css`**
  - **Purpose:** Styles for the main App component.
  - **Functions/Methods:** None.

- **`app.component.html`**
  - **Purpose:** Template for the main App component, sets up the application layout with dark mode support.
  - **Functions/Methods:** None.

- **`app.component.spec.ts`**
  - **Purpose:** Unit tests for the App component.
  - **Functions/Methods:**
    - `should create the app`: Checks if the App component is created successfully.
    - `should apply dark mode class when system preference is dark`: Verifies dark mode class application based on system preference.
    - `should handle system preference changes for dark mode`: Tests dynamic handling of dark mode preference changes.

- **`app.component.ts`**
  - **Purpose:** Main App component class; manages application initialization and dark mode toggling.
  - **Functions/Methods:**
    - `ngOnInit()`: Initializes dark mode based on system preferences and listens for preference changes.

- **`app.config.ts`**
  - **Purpose:** Application configuration setup, including routing, animations, HTTP client, and PrimeNG configurations.
  - **Functions/Methods:** None.

- **`app.routes.ts`**
  - **Purpose:** Defines application routes for health checks and authentication components.
  - **Functions/Methods:** None.

- **`email-verification.component.html`**
  - **Purpose:** Template for the Email Verification component, displaying verification status and handling user navigation.
  - **Functions/Methods:** None.

- **`email-verification.component.spec.ts`**
  - **Purpose:** Unit tests for the Email Verification component.
  - **Functions/Methods:**
    - `should create`: Ensures the component is created.
    - `should handle successful email verification`: Tests successful email verification flow.
    - `should handle verification failure from backend`: Tests handling of backend verification failures.
    - `should handle error during verification`: Tests error handling during verification process.
    - `should handle missing token`: Verifies behavior when verification token is missing.
    - `should navigate to registration page on button click`: Ensures navigation after verification.

- **`email-verification.component.ts`**
  - **Purpose:** Handles email verification logic, including token validation and status updates.
  - **Functions/Methods:**
    - `ngOnInit()`: Initializes verification process based on URL token.
    - `verifyEmail(token: string)`: Sends verification request to backend.
    - `handleError(message: string)`: Handles errors during verification.
    - `navigateToAuth()`: Navigates user to the authentication page.

- **`login-form.component.css`**
  - **Purpose:** Styles for the Login Form component.
  - **Functions/Methods:** None.

- **`login-form.component.html`**
  - **Purpose:** Template for the Login Form component, including form fields and validation messages.
  - **Functions/Methods:** None.

- **`login-form.component.spec.ts`**
  - **Purpose:** Unit tests for the Login Form component.
  - **Functions/Methods:**
    - `should create`: Ensures the component is created.
    - `should initialize with invalid form`: Checks initial form validity.
    - `should validate required fields`: Tests required field validations.
    - `should validate username format`: Verifies username pattern validation.
    - `should validate username length`: Tests username length constraints.
    - `should validate password format`: Verifies password pattern validation.
    - `should validate password length`: Tests password length constraints.
    - `should call login when form is valid`: Ensures login is called on valid form submission.
    - `should handle login error`: Tests error handling during login.
    - `should handle Error instance in login error`: Verifies error handling for different error types.
    - `should not submit if form is invalid`: Ensures form is not submitted when invalid.
    - `should return appropriate error messages`: Tests validation message accuracy.

- **`login-form.component.ts`**
  - **Purpose:** Manages the login form logic, including form validation and authentication requests.
  - **Functions/Methods:**
    - `ngOnInit()`: Initializes component.
    - `onSubmit()`: Handles form submission and authentication.
    - `getErrorMessage(controlName: string)`: Provides validation error messages.

- **`registration-form.component.css`**
  - **Purpose:** Styles for the Registration Form component.
  - **Functions/Methods:** None.

- **`registration-form.component.html`**
  - **Purpose:** Template for the Registration Form component, including form fields and validation messages.
  - **Functions/Methods:** None.

- **`registration-form.component.spec.ts`**
  - **Purpose:** Unit tests for the Registration Form component.
  - **Functions/Methods:**
    - `should create`: Ensures the component is created.
    - `should initialize with invalid form`: Checks initial form validity.
    - `should validate required fields`: Tests required field validations.
    - `should validate username format`: Verifies username pattern validation.
    - `should validate email format`: Tests email format validation.
    - `should validate password format`: Verifies password pattern validation.
    - `should validate password match`: Ensures password and confirm password match.
    - `should validate optional fields`: Tests optional field validations.
    - `should call registerUser when form is valid`: Ensures user registration on valid form submission.
    - `should handle registration error`: Tests error handling during registration.
    - `should handle Error instance in registration error`: Verifies error handling for different error types.
    - `should not submit if form is invalid`: Ensures form is not submitted when invalid.
    - `should return appropriate error messages`: Tests validation message accuracy.

- **`registration-form.component.ts`**
  - **Purpose:** Manages the registration form logic, including form validation and user registration requests.
  - **Functions/Methods:**
    - `ngOnInit()`: Initializes component.
    - `passwordMatchValidator(g: FormGroup)`: Validates password and confirm password match.
    - `onSubmit()`: Handles form submission and user registration.
    - `getErrorMessage(controlName: string)`: Provides validation error messages.

- **`health.component.ts`**
  - **Purpose:** Displays system health status by aggregating frontend and backend health checks.
  - **Functions/Methods:**
    - `ngOnInit()`: Fetches and processes health status on initialization.

- **`auth.service.spec.ts`**
  - **Purpose:** Unit tests for the AuthService.
  - **Functions/Methods:**
    - `should be created`: Ensures AuthService is instantiated.
    - `authentication state tests`: Verifies authentication state management.
    - `verifyEmail tests`: Tests email verification logic.
    - `registerUser tests`: Tests user registration logic.
    - `login tests`: Tests user login logic.
    - `logout tests`: Ensures logout functionality clears authentication data.

- **`auth.service.ts`**
  - **Purpose:** Handles authentication-related operations including user registration, login, logout, and email verification.
  - **Functions/Methods:**
    - `registerUser(user: IUserRegistration)`: Registers a new user.
    - `verifyEmail(token: string)`: Verifies user email with a token.
    - `login(loginData: ILoginRequest)`: Authenticates user and stores session data.
    - `logout()`: Clears authentication data and updates state.
    - `getAuthToken()`: Retrieves stored authentication token.
    - `storeAuthData(response: ILoginResponse, rememberMe: boolean)`: Stores authentication data based on user preference.
    - `hasValidToken()`: Checks for the presence of a valid token.
    - `loadStoredUser()`: Loads stored user data from storage.
    - `handleError(error: HttpErrorResponse)`: Handles HTTP errors.
    - `getOperationContext(url: string | null)`: Determines the context of an API operation based on URL.

- **`health.service.ts`**
  - **Purpose:** Provides system health checks by querying frontend and backend health endpoints.
  - **Functions/Methods:**
    - `checkHealth()`: Retrieves and aggregates health status from frontend and backend.

- **`mock-store.ts`**
  - **Purpose:** Provides utilities to create and configure a mock NgRx store for testing.
  - **Functions/Methods:**
    - `createMockStore(initialState: any)`: Creates a mock store with an initial state.
    - `setMockSelectors(mockStore: MockStore, selectorMap: Record<string, any>)`: Sets up mock selector values.

- **`test-utils.ts`**
  - **Purpose:** Offers common testing utilities for component tests.
  - **Functions/Methods:**
    - `queryByCss<T>(fixture: ComponentFixture<T>, selector: string)`: Finds an element by CSS selector.
    - `queryAllByCss<T>(fixture: ComponentFixture<T>, selector: string)`: Finds all elements by CSS selector.
    - `detectChanges<T>(fixture: ComponentFixture<T>)`: Triggers change detection and waits for it to complete.

- **`environment.cypress.ts`**
  - **Purpose:** Environment configuration for Cypress testing, disables password feedback during tests.
  - **Functions/Methods:** None.

- **`environment.prod.ts`**
  - **Purpose:** Production environment configuration, sets API URL for production deployment.
  - **Functions/Methods:** None.  
  
- **`JwtAuthenticationFilter.java`**
  - **Purpose:** Validates JWT tokens in incoming HTTP requests to secure endpoints.
  - **Classes/Methods:**
    - `doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)`: Extracts and verifies JWT token, sets authentication in the security context.

- **`OpenApiConfig.java`**
  - **Purpose:** Configures OpenAPI/Swagger documentation and security schemes for the API.
  - **Classes/Methods:** 
    - Configuration is managed through annotations; no explicit methods.

- **`SecurityConfig.java`**
  - **Purpose:** Sets up Spring Security configurations including JWT, CORS, and endpoint security rules.
  - **Classes/Methods:**
    - `filterChain(HttpSecurity http)`: Configures HTTP security, authentication providers, and adds JWT filter.
    - `authenticationProvider()`: Configures the authentication provider with user details service and password encoder.
    - `authenticationManager(AuthenticationConfiguration config)`: Provides the authentication manager bean.
    - `corsConfigurationSource()`: Defines CORS settings for allowed origins, methods, and headers.
    - `passwordEncoder()`: Provides a BCrypt password encoder bean.

- **`WebConfig.java`**
  - **Purpose:** Configures global CORS mappings for the web application.
  - **Classes/Methods:**
    - `addCorsMappings(CorsRegistry registry)`: Specifies allowed origins, methods, headers, and other CORS settings.

- **`AuthenticationController.java`**
  - **Purpose:** Handles authentication-related endpoints such as user login.
  - **Classes/Methods:**
    - `login(LoginRequestDto loginRequest)`: Authenticates user credentials and returns a JWT token upon successful login.

- **`UserController.java`**
  - **Purpose:** Manages user-related operations like registration and email verification.
  - **Classes/Methods:**
    - `handleUserRegistration(UserRegistrationDto registrationDto)`: Registers a new user and initiates email verification.
    - `verifyEmail(String token)`: Verifies a user's email using the provided verification token.

- **`LoginRequestDto.java`**
  - **Purpose:** Data Transfer Object for capturing user login requests.
  - **Fields:**
    - `username`: User's username.
    - `password`: User's password.

- **`LoginResponseDto.java`**
  - **Purpose:** Data Transfer Object for sending login responses.
  - **Fields:**
    - `token`: JWT token.
    - `username`: User's username.
    - `email`: User's email address.
    - `emailVerified`: Indicates if the user's email is verified.

- **`UserRegistrationDto.java`**
  - **Purpose:** Data Transfer Object for capturing user registration details.
  - **Fields:**
    - `username`: Desired username.
    - `email`: User's email address.
    - `password`: User's password.
    - `profilePictureUrl`: URL to the user's profile picture.
    - `bio`: User's biography.

- **`ApiError.java`**
  - **Purpose:** Standardizes the format of API error responses.
  - **Fields:**
    - `timestamp`: Time the error occurred.
    - `status`: HTTP status code.
    - `message`: Error message.
    - `errors`: Detailed field-specific error messages.

- **`GlobalExceptionHandler.java`**
  - **Purpose:** Handles exceptions globally and formats error responses consistently.
  - **Classes/Methods:**
    - `handleValidationExceptions(MethodArgumentNotValidException ex)`: Handles validation errors.
    - `handleMissingParams(MissingServletRequestParameterException ex)`: Handles missing request parameters.
    - `handleIllegalArgumentException(IllegalArgumentException ex)`: Handles illegal argument exceptions.
    - `handleIllegalStateException(IllegalStateException ex)`: Handles illegal state exceptions.
    - `handleBadCredentialsException(BadCredentialsException ex)`: Handles authentication failures.
    - `handleDisabledException(DisabledException ex)`: Handles disabled account exceptions.
    - `handleGenericException(Exception ex)`: Handles all other unexpected exceptions.

- **`GamerecsBackApplication.java`**
  - **Purpose:** Entry point for the Spring Boot application.
  - **Classes/Methods:**
    - `main(String[] args)`: Launches the Spring Boot application.

- **`User.java`**
  - **Purpose:** Represents the User entity in the database.
  - **Fields:**
    - `userId`: Unique identifier for the user.
    - `username`: User's username.
    - `email`: User's email address.
    - `passwordHash`: Hashed password.
    - `profilePictureUrl`: URL to the user's profile picture.
    - `bio`: User's biography.
    - `joinDate`: Timestamp of when the user joined.
    - `lastLogin`: Timestamp of the user's last login.
    - `emailVerified`: Indicates if the user's email is verified.

- **`VerificationToken.java`**
  - **Purpose:** Represents email verification tokens linked to users.
  - **Fields:**
    - `tokenId`: Unique identifier for the token.
    - `user`: Associated user entity.
    - `token`: Verification token string.
    - `expiryDate`: Expiration date and time of the token.
    - `createdDate`: Timestamp when the token was created.
  - **Classes/Methods:**
    - `isExpired()`: Checks if the token has expired.

- **`UserRepository.java`**
  - **Purpose:** Provides CRUD operations for User entities.
  - **Interfaces/Methods:**
    - `findByEmail(String email)`: Retrieves a user by email.
    - `findByUsername(String username)`: Retrieves a user by username.
    - `existsByEmail(String email)`: Checks if a user exists with the given email.
    - `existsByUsername(String username)`: Checks if a user exists with the given username.

- **`VerificationTokenRepository.java`**
  - **Purpose:** Provides CRUD operations for VerificationToken entities.
  - **Interfaces/Methods:**
    - `findByToken(String token)`: Retrieves a verification token by its string value.
    - `deleteByUser_UserId(Long userId)`: Deletes all verification tokens associated with a specific user ID.

- **`CustomUserDetailsService.java`**
  - **Purpose:** Loads user-specific data for authentication.
  - **Classes/Methods:**
    - `loadUserByUsername(String username)`: Retrieves user details by username for authentication.

- **`EmailService.java`**
  - **Purpose:** Handles sending emails, particularly verification emails to users.
  - **Classes/Methods:**
    - `sendVerificationEmail(String to, String username, String token)`: Sends a verification email to the specified user.
    - `generateVerificationToken()`: Generates a unique verification token.
    - `validateInputs(String email, String username, String token)`: Validates input parameters for sending emails.

- **`JwtService.java`**
  - **Purpose:** Manages JWT token creation and validation.
  - **Classes/Methods:**
    - `generateToken(UserDetails userDetails)`: Generates a JWT token for a user.
    - `generateToken(Map<String, Object> extraClaims, UserDetails userDetails)`: Generates a JWT token with additional claims.
    - `isTokenValid(String token, UserDetails userDetails)`: Validates a JWT token against user details.
    - `extractUsername(String token)`: Extracts the username from a JWT token.
    - `extractExpiration(String token)`: Retrieves the expiration date from a JWT token.
    - `extractClaim(String token, Function<Claims, T> claimsResolver)`: Extracts a specific claim from a JWT token.

- **`UserService.java`**
  - **Purpose:** Handles business logic related to user operations such as registration and email verification.
  - **Classes/Methods:**
    - `registerUser(User user)`: Registers a new user and sends a verification email.
    - `sendVerificationEmail(User user)`: Generates a verification token and sends the verification email to the user.
    - `verifyEmail(String token)`: Verifies a user's email using the provided token.  
  
- **`BaseTest.java`**
  - **Purpose:** Provides common configurations for all test classes.
  - **Functions/Methods:**
    - *No specific methods; serves as a base class.*

- **`JwtAuthenticationFilterTest.java`**
  - **Purpose:** Tests the `JwtAuthenticationFilter` functionality.
  - **Functions/Methods:**
    - `shouldProcessValidToken()`: Verifies processing of a valid JWT token.
    - `shouldSkipForMissingAuthHeader()`: Ensures filter skips when the Authorization header is missing.
    - `shouldSkipForInvalidAuthHeaderFormat()`: Checks filter behavior with an improperly formatted Authorization header.
    - `shouldHandleInvalidToken()`: Tests handling of invalid JWT tokens.
    - `shouldHandleJwtExtractionException()`: Verifies exception handling during JWT extraction.
    - `shouldHandleUserDetailsException()`: Ensures proper handling when `UserDetailsService` throws an exception.

- **`SecurityConfigTest.java`**
  - **Purpose:** Validates security configurations and endpoint access restrictions.
  - **Functions/Methods:**
    - `shouldAllowSwaggerUIAccess()`: Confirms access to Swagger UI endpoints without authentication.
    - `shouldAllowActuatorAccess()`: Ensures actuator endpoints are accessible without authentication.
    - `shouldRequireAuthForProtectedEndpoints()`: Verifies that protected endpoints require authentication.
    - `shouldAllowPublicEndpointsAccess()`: Checks that public endpoints are accessible without authentication.

- **`TestConfig.java`**
  - **Purpose:** Provides test-specific bean configurations.
  - **Functions/Methods:**
    - `objectMapper()`: Supplies a customized `ObjectMapper` bean for tests.

- **`WebConfigTest.java`**
  - **Purpose:** Tests CORS configurations in the web setup.
  - **Functions/Methods:**
    - `shouldConfigureCorsWithAllowedOrigins()`: Validates CORS with correct allowed origins.
    - `shouldAllowConfiguredHttpMethods()`: Ensures all configured HTTP methods are allowed.
    - `shouldAllowAllHeaders()`: Checks that all specified headers are permitted.
    - `shouldSetCorrectMaxAge()`: Verifies the CORS max age setting.
    - `shouldRejectNonAllowedOrigins()`: Confirms that non-allowed origins are rejected.

- **`AuthenticationControllerTest.java`**
  - **Purpose:** Tests authentication controller endpoints and behaviors.
  - **Functions/Methods:**
    - `shouldAuthenticateWithValidCredentials()`: Verifies successful authentication with valid credentials.
    - `shouldReturn401ForInvalidCredentials()`: Checks response for invalid login credentials.
    - `shouldReturn400ForInvalidRequestFormat()`: Ensures proper handling of malformed authentication requests.
    - `shouldReturn500ForUnexpectedErrors()`: Tests handling of unexpected errors during authentication.
    - `shouldReturn400WhenUserNotFound()`: Confirms response when user is not found post-authentication.
    - `shouldReturn401ForUnverifiedEmail()`: Verifies response for accounts with unverified emails.

- **`UserControllerTest.java`**
  - **Purpose:** Tests user-related controller endpoints.
  - **Functions/Methods:**
    - `shouldRegisterUserWithValidData()`: Confirms successful user registration with valid data.
    - `shouldReturn400ForInvalidEmailFormat()`: Checks response for registration with invalid email formats.
    - `shouldReturn400ForBlankUsername()`: Ensures registration fails with a blank username.
    - `shouldReturn400ForPasswordTooShort()`: Verifies registration failure for short passwords.
    - `shouldReturn400WhenEmailExists()`: Confirms response when registering with an existing email.
    - `shouldReturn400WhenUsernameExists()`: Checks response for duplicate usernames.
    - `shouldHandleUnexpectedErrors()`: Tests handling of unexpected errors during registration.
    - `shouldAcceptRegistrationWithOptionalFieldsNull()`: Ensures registration succeeds when optional fields are null.
    - `shouldVerifyEmailSuccessfully()`: Verifies successful email verification with a valid token.
    - `shouldReturn400ForInvalidToken()`: Confirms response for invalid or expired verification tokens.
    - `shouldHandleMissingToken()`: Checks response when the verification token is missing.
    - `shouldHandleUnexpectedVerificationErrors()`: Tests handling of unexpected errors during email verification.

- **`GamerecsBackApplicationTests.java`**
  - **Purpose:** Ensures the Spring Boot application context loads successfully.
  - **Functions/Methods:**
    - `contextLoads()`: Verifies that the application context loads without issues.

- **`UserTest.java`**
  - **Purpose:** Validates the `User` model's constraints and behaviors.
  - **Functions/Methods:**
    - `shouldCreateValidUser()`: Tests creation of a valid user with all required fields.
    - `shouldFailValidationWithBlankUsername()`: Ensures validation fails for blank usernames.
    - `shouldFailValidationWithInvalidEmail()`: Checks validation for improperly formatted emails.
    - `shouldFailValidationWithBlankPasswordHash()`: Verifies validation failure for blank passwords.
    - `shouldCreateUserWithOptionalFields()`: Confirms user creation with optional fields populated.
    - `shouldFailValidationWithUsernameTooShort()`: Tests validation for usernames that are too short.
    - `shouldFailValidationWithUsernameTooLong()`: Ensures validation fails for excessively long usernames.

- **`UserRepositoryTest.java`**
  - **Purpose:** Tests the `UserRepository` for CRUD operations and constraints.
  - **Functions/Methods:**
    - `shouldSaveUser()`: Verifies successful user saving.
    - `shouldFindUserByEmail()`: Tests finding a user by email.
    - `shouldFindUserByUsername()`: Confirms finding a user by username.
    - `shouldCheckIfEmailExists()`: Checks email existence queries.
    - `shouldCheckIfUsernameExists()`: Verifies username existence checks.
    - `shouldNotSaveUserWithDuplicateEmail()`: Ensures saving fails with duplicate emails.
    - `shouldNotSaveUserWithDuplicateUsername()`: Confirms saving fails with duplicate usernames.

- **`CustomUserDetailsServiceTest.java`**
  - **Purpose:** Tests the custom implementation of `UserDetailsService`.
  - **Functions/Methods:**
    - `loadUserByUsername_WhenUserExists_ReturnsUserDetails()`: Verifies loading user details for existing users.
    - `loadUserByUsername_WhenUserNotFound_ThrowsException()`: Ensures exception is thrown for non-existent users.
    - `loadUserByUsername_WhenUserNotVerified_ReturnsDisabledUser()`: Checks handling of unverified user accounts.

- **`EmailServiceTest.java`**
  - **Purpose:** Tests the `EmailService` for sending emails and handling related operations.
  - **Functions/Methods:**
    - `shouldSendVerificationEmail()`: Verifies successful sending of verification emails.
    - `shouldGenerateUniqueVerificationTokens()`: Ensures generated verification tokens are unique.
    - `shouldHandleEmailSendingFailure()`: Tests handling failures during email sending.
    - `shouldHandleTemplateProcessingFailure()`: Confirms proper handling of template processing errors.
    - `shouldThrowExceptionForNullEmail()`: Ensures exception is thrown for null email addresses.
    - `shouldThrowExceptionForNullUsername()`: Checks exception handling for null usernames.
    - `shouldThrowExceptionForNullToken()`: Verifies exception for null verification tokens.
    - `shouldThrowExceptionForMalformedEmail()`: Tests handling of malformed email addresses.

- **`JwtServiceTest.java`**
  - **Purpose:** Tests the `JwtService` for JWT token operations.
  - **Functions/Methods:**
    - `shouldGenerateToken()`: Verifies token generation.
    - `shouldGenerateTokenWithExtraClaims()`: Ensures tokens can include additional claims.
    - `shouldExtractUsername()`: Tests extraction of username from a token.
    - `shouldExtractExpiration()`: Confirms extraction of token expiration dates.
    - `shouldValidateToken()`: Verifies token validation for authenticity.
    - `shouldInvalidateTokenForDifferentUser()`: Ensures tokens are invalid for different users.
    - `shouldExtractClaims()`: Tests extraction of specific claims from a token.

- **`UserServiceTest.java`**
  - **Purpose:** Tests the `UserService` for user registration and email verification.
  - **Functions/Methods:**
    - `shouldRegisterUser()`: Verifies successful user registration.
    - `shouldThrowExceptionWhenEmailExists()`: Ensures exception is thrown when registering with an existing email.
    - `shouldThrowExceptionWhenUsernameExists()`: Confirms exception for duplicate usernames during registration.
    - `shouldHashPasswordDuringRegistration()`: Tests that passwords are hashed during registration.
    - `shouldPreserveUserFieldsDuringRegistration()`: Ensures user fields are preserved during registration.
    - `shouldVerifyEmailSuccessfully()`: Verifies successful email verification.
    - `shouldThrowExceptionForInvalidToken()`: Ensures exception is thrown for invalid verification tokens.
    - `shouldThrowExceptionForExpiredToken()`: Confirms exception for expired verification tokens.

- **`BaseIntegrationTest.java`**
  - **Purpose:** Provides common setup and utilities for integration tests.
  - **Functions/Methods:**
    - *No specific methods; serves as a base class for integration tests.*

- **`BaseUnitTest.java`**
  - **Purpose:** Provides common setup and utilities for unit tests.
  - **Functions/Methods:**
    - *No specific methods; serves as a base class for unit tests.*


