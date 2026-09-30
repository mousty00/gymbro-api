## 💪 GymBro API

The **GymBro API** is the robust backend service for the GymBro fitness application, designed to help users easily track their workouts and engage with friends in a social-media-like environment.

-----

## 🌟 Key Features

The GymBro API powers the core functionalities of the application, including:

  * **Workout Tracking:** Seamlessly log and monitor your fitness routines and progress.
  * **Social Interaction:** Interact with friends' workout posts, turning fitness into a shared, social experience.
  * **Secure Authentication:** User data and interactions are protected using industry-standard security practices.

-----

## 🛠️ Tech Stack and Architecture

This API is built using modern, scalable Java technologies and leverages cloud-native services.

### Backend

  * **Java 21 + Spring Boot 3:** The foundation of the API, providing rapid development and a robust, production-ready environment.
  * **GraphQL (Netflix DGS) + REST:** Full REST API, plus a GraphQL schema for auth, users and social features.
  * **Spring Security + JWT:** Stateless token-based authentication and authorization.
  * **PostgreSQL + Flyway:** Relational storage with versioned schema migrations.
  * **Java Mail:** Email verification and password resets.
  * **Testcontainers:** Integration tests against a real PostgreSQL instance.

### Cloud Services

  * **AWS S3 (Simple Storage Service) Bucket:** Used for scalable, secure storage of user-uploaded content (e.g., profile pictures, workout images).

### Documentation & Monitoring

  * **OpenAPI/Swagger UI:** Provides comprehensive, interactive API documentation for easy integration and testing.

-----

## 🚀 Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

You'll need the following installed:

  * **JDK 21+** (Maven comes bundled as `./mvnw`)
  * **Docker** (for the local database and the integration tests)
  * An **AWS S3 bucket** and an **SMTP account** (for uploads and verification emails)

### Run locally

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/mousty00/gymbro-api.git
    cd gymbro-api
    ```
2.  **Start PostgreSQL:**
    ```bash
    printf 'POSTGRES_USER=postgres\nPOSTGRES_PASSWORD=postgres\nPOSTGRES_DB=gymbro\n' > database.env
    docker compose up -d pg-service
    ```
3.  **Configure the app:** copy the template and fill in the blanks (database, mail, AWS, and a JWT secret from `openssl rand -base64 64`).
    ```bash
    cp src/main/resources/application.properties.template src/main/resources/application.properties
    ```
    `application.properties`, `*.env` and `.env.*` are gitignored — never commit real credentials.
4.  **Run:**
    ```bash
    ./mvnw spring-boot:run
    ```
    Flyway creates the schema on first start. The API is served at `http://localhost:8080/api`.

### Tests

```bash
./mvnw verify   # needs Docker running: Testcontainers starts its own PostgreSQL
```

### Deploy

See [DEPLOY_DOKPLOY.md](DEPLOY_DOKPLOY.md) and [.env.prod.example](.env.prod.example) for the production variables.

-----

## 📚 API Documentation

Once the API is running, you can access the interactive documentation to explore the available endpoints:

  * **GraphiQL:** `http://localhost:8080/api/graphiql`
  * **Swagger UI (REST):** `http://localhost:8080/api/swagger-ui.html`

Use this interface to understand how to interact with endpoints for user management, workout tracking, and social features.

-----

## 🤝 Contribution

Contributions are welcome\! Please feel free to open an issue or submit a pull request if you have suggestions or bug fixes.

-----

## 📄 License

Released under the [MIT License](LICENSE).

-----

## 📧 Contact

For any questions or suggestions, please contact me at moustapha.dev03@gmail.com.

