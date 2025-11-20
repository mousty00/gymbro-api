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

  * **Java Spring Boot:** The foundation of the API, providing rapid development and a robust, production-ready environment.
  * **Spring Security:** Handles all authentication and authorization within the application.
  * **JWT (JSON Web Tokens):** Used for secure, stateless token-based authentication.
  * **Java Mail:** Enables communication functionalities, such as password resets and notification emails.
  * **Apache Kafka (Introducing):** Being integrated for building real-time data pipelines and streaming features (e.g., real-time feed updates, notifications).

### Cloud Services

  * **AWS S3 (Simple Storage Service) Bucket:** Used for scalable, secure storage of user-uploaded content (e.g., profile pictures, workout images).

### Documentation & Monitoring

  * **OpenAPI/Swagger UI:** Provides comprehensive, interactive API documentation for easy integration and testing.

-----

## 🚀 Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

You'll need the following installed:

  * **Java Development Kit (JDK) 21+**
  * **Maven** 
  * An **IDE** (IntelliJ)
  * **AWS Account** (for S3 setup)
  * **Local Kafka** 

### Installation

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/mousty00/gymbro-api.git
    cd gymbro-api
    ```
2.  **Configure Environment Variables:**
      * Create an `application.properties` or `application.yml` file in `src/main/resources`.
      * Set up configurations for:
          * **Database connection** (PostgreSQL).
          * **AWS S3 credentials and bucket name.**
          * **JWT secret key and expiration time.**
          * **Java Mail properties.**
          * **Kafka broker configuration.**
3.  **Build the project:**
    ```bash
    mvn clean install
    ```
4.  **Run the application:**
    ```bash
    mvn spring-boot:run
    ```

The API will typically start on `http://localhost:8080/api`.

-----

## 📚 API Documentation

Once the API is running, you can access the interactive documentation to explore the available endpoints:

  * **Swagger UI:** `http://localhost:8080/api/swagger-ui.html`

Use this interface to understand how to interact with endpoints for user management, workout tracking, and social features.

-----

## 🤝 Contribution

Contributions are welcome\! Please feel free to open an issue or submit a pull request if you have suggestions or bug fixes.

-----

## 📧 Contact

For any questions or suggestions, please contact me at moustapha.dev03@gmail.com.

