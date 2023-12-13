# OrganizationManager


[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
![Java Version](https://img.shields.io/badge/Java-17-blue)
![Spring Boot Version](https://img.shields.io/badge/Spring%20Boot-3-brightgreen)

<p align="center">
  <img src="docs/images/logo-sdk-white-transparent.png" alt="SDK LOGO" width="50%">
  <br>
  <em>A data platform for everyone</em>
</p>

- [OrganizationManager](#organizationmanager)
  - [About](#about)
  - [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Setup](#setup)
    - [Configuration](#configuration)
    - [Usage](#usage)
  - [Contributing](#contributing)
  - [Changelog](#changelog)

## About


The `OrganizationManager` is a component of the **Superb Data Kraken Platform (SDK)**.
It's designed for managing organizations and spaces within the SDK.
*Organizations* group use-cases, while *Spaces* represent individual use-cases.

For a more detailed understanding of the broader context of the platform this project is used in, refer to
the [architecture documentation](https://github.com/EFS-OpenSource/superb-data-kraken-frontend).

For instructions on how to deploy the `OrganizationManager` on an instance of the **SDK**, refer to
the [installation instructions](https://efs-opensource.github.io/superbdatakraken/operator-manual/installation/).

Refer to [index.md](docs%2Findex.md) for more detailed but deployment specific documentation.


## Getting Started


Follow the instructions below to set up a local copy of the project for development and testing.


### Prerequisites

- jdk >= 17
- Maven 3.6.x (if you're not using the Maven wrapper)
- A running OIDC/OAuth2 provider instance
- A running Kafka instance
- [sdk-common package](https://github.com/EFS-OpenSource/superb-data-kraken-common)
- [storage-schemas package](https://github.com/EFS-OpenSource/superb-data-kraken-storage-schemas)

### Setup


To set up your local environment for development and testing, follow these steps:

1. Clone the repository:
   ```bash
   git clone https://github.com/EFS-OpenSource/superb-data-kraken-organizationmanager.git
   cd superb-data-kraken-organizationmanager
1. Set up the service configuration:
    ```bash
   cp src/main/resources/application-local-template.yml src/main/resources/application-local.yaml
   ```
   Configure the `application-local.yaml` file based on your local development setup. The OIDC provider instance needs to be
   configured correctly for the organizationmanager to run as expected.
2. Run the service:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot:run.profiles=local
3. After successful setup, you can test the service using the auto-generated API documentation at:
   ```
   https://localhost:8094/organizationmanager/swagger-ui/index.html

### Configuration

- `sdk.oauth2.config-url`: Specifies the URL pointing to the OpenID Connect discovery document. This URL provides essential configuration details for OAuth2
  and OpenID Connect operations, enabling dynamic discovery of authentication server endpoints and supported features. It's instrumental in configuring the
  OAuth2-based security settings within the Spring Boot application's security plugin.
- Replace all placeholders enclosed in `$()`, e.g.:
    - `REALM`: the specific realm set up with the openid connect (oidc) provider.
    - `CLIENT_ID`: the unique identifier for the service account that the service utilizes.

### Usage


Here are the essential commands you'll need to use the service:

- Build the service:
  ```bash
  ./mvnw package
- Executing the tests alone:
  ```bash
  ./mvnw test
- Run the organizationmanager:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot:run.profiles=local
- Build the service Docker image:
  ```bash
  docker build -t sdk/organizationmanager .

## Contributing


See the [Contribution Guide](./CONTRIBUTING.md).


## Changelog


See the [Changelog](./CHANGELOG.md).

