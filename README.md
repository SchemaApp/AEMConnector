# Schema App AEM Connector

A connector to enable local caching of Schema Markup (JSON-LD) produced by [Schema App](https://www.schemaapp.com) within Adobe Experience Manager (AEM).

## Overview

The Schema App AEM Connector fetches structured data markup from the Schema App platform and caches it locally in your AEM instance. This enables:

- **Local caching** of Schema.org JSON-LD markup for improved page load performance
- **Automatic synchronization** with Schema App to keep markup up-to-date
- **Dispatcher cache integration** for invalidation when markup changes
- **Cloud service configuration** for easy setup and management

## Repository Information

| Property | Value |
|----------|-------|
| **Group ID** | `com.schemaapp` |
| **Artifact ID** | `schemaapp-aem` |
| **Version** | `2.0.6.1` |
| **Repository** | <https://github.com/SchemaApp/AEMConnector> |
| **License** | MIT |

## Related Technologies

### Runtime Requirements

| Technology | Version | Notes |
|------------|---------|-------|
| **Java** | 1.8 (Java 8) | Minimum required; source/target compilation level |
| **AEM** | 6.5.0+ | Tested with AEM 6.5 uber-jar APIs |
| **Maven** | 3.3.9+ | Required for building |

### AEM/Adobe APIs Used

- **AEM uber-jar 6.5.0** (`com.adobe.aem:uber-jar:6.5.0:apis`) - Core AEM APIs
- **Core WCM Components 2.23.2** (`com.adobe.cq:core.wcm.components.core`) - Component framework

### Apache Sling APIs

- **Sling Models API 1.3.6** (`org.apache.sling:org.apache.sling.models.api`) - Resource adaptation and injection
- **Sling Servlets Annotations 1.2.4** - Servlet registration
- **Sling Resource Resolver** - JCR resource access

### OSGi Framework

- **OSGi Service Component Annotations 1.4.0** - Declarative services
- **OSGi Metatype Annotations 1.4.0** - Configuration metadata
- **OSGi Configuration Admin 1.6.0** - Dynamic configuration

### JCR/Repository

- **JCR 2.0** (`javax.jcr:jcr:2.0`) - Content repository API
- **Apache Jackrabbit FileVault** - Content packaging

### Day CQ APIs (via uber-jar)

- **CQ Replication API** (`com.day.cq.replication.Replicator`) - Content replication
- **CQ WCM API** (`com.day.cq.wcm.api`) - Page management
- **CQ Search/QueryBuilder** (`com.day.cq.search.QueryBuilder`) - Content queries

### HTTP/JSON

- **Apache HttpComponents 4.5.14** - HTTP client for API calls
- **Jackson 2.16.2** - JSON processing

## Architecture

### High-Level Data Flow

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   Schema App    │────▶│  AEM Connector   │────▶│   AEM Pages     │
│   API Server    │     │   (Scheduler)    │     │  (JCR Nodes)    │
└─────────────────┘     └──────────────────┘     └─────────────────┘
                                │                         │
                                ▼                         ▼
                        ┌──────────────────┐     ┌─────────────────┐
                        │  Dispatcher      │◀────│  JSON-LD        │
                        │  Cache           │     │  Component      │
                        └──────────────────┘     └─────────────────┘
```

### Components

1. **SchemaAppDataAPIScheduler** - OSGi scheduled task that triggers data synchronization (default: every 30 minutes)

2. **CDNDataProcessor** - Orchestrates the sync process:
   - Discovers pages with Schema App cloud service configurations
   - Coordinates data fetching and storage
   - Manages page node creation/updates

3. **BulkDataLoaderAPIService** - Fetches markup data from Schema App:
   - Calls `https://api.schemaapp.com/export/{accountId}` with pagination
   - Compares ETags to detect changes
   - Supports proxy configuration for enterprise environments

4. **CDNHandlerService** - Persists data to AEM:
   - Saves JSON-LD graph data to page nodes
   - Creates `/jcr:content/schemaapp` nodes under each page
   - Stores entity data, ETags, and configuration metadata

5. **FlushService** - Cache invalidation:
   - Triggers AEM replication for updated nodes
   - Sends invalidation requests to Dispatcher

### Data Storage

Schema App markup is stored under each page's `jcr:content` node:

```
/content/mysite/mypage
  └── jcr:content
      └── schemaapp                          (sling:resourceType = schemaApp/components/content/entitydata)
          ├── entity        [String]         JSON-LD graph data
          ├── siteURL       [String]         Full page URL
          ├── accountId     [String]         Schema App account ID
          ├── deploymentMethod [String]      "crawler" or "javaScript"
          ├── eTag          [String]         ETag for change detection
          └── sourceHeader  [String]         Data source identifier
```

## How JSON-LD is Added to Pages

### The Entity Data Component

The connector uses the `schemaApp/components/content/entitydata` component to store JSON-LD data.

**Component location:** `/apps/schemaApp/components/content/entitydata`

**Sling Model:** `com.schemaapp.core.models.EntityDataModel`

The component:

1. Is automatically created under `jcr:content/schemaapp` for each page with Schema App configured
2. Stores the JSON-LD markup as a JSON string in the `entity` property
3. Exposes data via Sling Model JSON export on the child resource (not the page)

### Accessing Cached Data

The JSON-LD data can be accessed via:

1. **Child Resource Model Export** - Request the schemaapp resource directly with `.model.json`:
   ```
   /content/mysite/mypage/jcr:content/schemaapp.model.json
   ```
   Note: The `EntityDataModel` exporter is registered for the `schemaApp/components/content/entitydata` resource type, not the page. A page-level `.model.json` request will only include this data if the site's page exporter is configured to include child components.

2. **Direct Node Access** - Read from `/jcr:content/schemaapp/entity` property (returns a JSON string)

### Rendering JSON-LD Script Tags

The connector provides a cloud service headlibs integration for rendering JSON-LD in the page `<head>`:

**Headlibs JSP:** `/apps/schemaApp/registration/components/schemaapp/headlibs.jsp`

This JSP:

1. Calls `PageJSONDataReaderService` to retrieve the cached JSON-LD data
2. Emits a `<script type="application/ld+json">` tag containing the graph data
3. For JavaScript deployment method, also includes the Schema App highlighter script

To enable JSON-LD rendering, ensure the Schema App cloud service is configured on your site and the headlibs are included in your page template's head section via the cloud service configuration mechanism.

### PageJSONDataReaderService

For programmatic access, use `PageJSONDataReaderService`:

```java
@Reference
private PageJSONDataReaderService pageJSONDataReaderService;

// Initialize with page URL
pageJSONDataReaderService.init("https://www.example.com/content/mysite/mypage.html");

// Get the JSON-LD graph data
String jsonLd = pageJSONDataReaderService.getGraphData();

// Get the source header
String source = pageJSONDataReaderService.getSource();
```

## Configuration

### Cloud Service Configuration

Schema App is configured as an AEM Cloud Service at `/etc/cloudservices/schemaapp`.

**Configuration wizard:** `/apps/schemaApp/utilities/schemaAppcloudconfig/wizard.html`

#### Required Settings

| Field | Description |
|-------|-------------|
| **Title** | Configuration name |
| **Schema App API Key** | API key from Schema App Administration Console |
| **Schema App Account ID** | Account ID from Schema App Administration Console |
| **Website URL** | Production site URL (e.g., `https://www.mydomain.com`) |
| **Method** | `crawler` (default) or `javaScript` |

#### Optional Proxy Settings

For environments requiring HTTP proxy:

| Field | Description |
|-------|-------------|
| **Enable Proxy** | Toggle proxy support |
| **Proxy Hostname** | Proxy server address (e.g., `proxy.example.com`) |
| **Proxy Port** | Proxy port (e.g., `8080`) |
| **Proxy Username** | Authentication username (if required) |
| **Proxy Password** | Authentication password (if required) |

### OSGi Configuration

#### Scheduler Configuration

**PID:** `com.schemaapp.core.schedulers.SchemaAppDataAPIScheduler`

**Location:** `/apps/schemaApp/config/com.schemaapp.core.schedulers.SchemaAppDataAPIScheduler.xml`

| Property | Default | Description |
|----------|---------|-------------|
| `enabled` | `true` | Enable/disable the scheduler |
| `expression` | `0 */30 * ? * *` | Cron expression (default: every 30 min) |
| `concurrent` | `false` | Allow concurrent execution |

> **Note:** The scheduler's `@ObjectClassDefinition` interface defines methods `enabled()`, `expression()`, and `concurrent()`, which OSGi maps to config properties `enabled`, `expression`, and `concurrent`. The packaged XML config incorrectly uses `scheduler.expression` and `scheduler.concurrent`—these keys are not read by the scheduler code. Use the correct property names shown above when customizing the configuration.

#### API Endpoint Configuration

**PID:** `com.schemaapp.core.APIEndpointConfig`

**Location:** `/apps/schemaApp/config/com.schemaapp.core.APIEndpointConfig.xml`

| Property | Default | Description |
|----------|---------|-------------|
| `SchemaAppCDNDataAPIEndpoint` | `https://data.schemaapp.com` | Schema App data API endpoint |

> **Note:** This endpoint configuration is **not used** by the bulk data export flow. The `BulkDataLoaderAPIServiceImpl` hardcodes the API endpoint as `https://api.schemaapp.com`. Changing this configuration property will not redirect synchronization requests. The property may be intended for future use or other integration points.

### Service User

The connector uses a dedicated service user for JCR operations:

**Service User:** `aem-schema-app-service-user`

**Permissions:**

- Read: `/etc/cloudservices`
- Read/Write/Replicate: `/content`
- Read/Write: `/conf`

## Modules

| Module | Description |
|--------|-------------|
| **core** | Java bundle with OSGi services, Sling models, schedulers, and servlets |
| **ui.apps** | AEM components, clientlibs, cloud service configurations, and OSGi configs |
| **ui.content** | Sample content using the connector components |
| **ui.apps.structure** | Content package structure definition |
| **all** | Combined content package embedding all modules |

## How to Build

### Prerequisites

- Java 8 JDK
- Maven 3.3.9+
- Access to Adobe Maven repository

### Build Commands

Build all modules:

```bash
mvn clean install
```

Build and deploy to local AEM author (localhost:4502):

```bash
mvn clean install -PautoInstallPackage
```

Deploy to publish instance:

```bash
mvn clean install -PautoInstallSinglePackagePublish
```

Or specify a custom port:

```bash
mvn clean install -PautoInstallSinglePackage -Daem.port=4503
```

Deploy only the OSGi bundle:

```bash
mvn clean install -PautoInstallBundle
```

Deploy a single content package (from submodule directory):

```bash
cd ui.apps
mvn clean install -PautoInstallPackage
```

## Testing

### Unit Tests

```bash
mvn clean test
```

### Integration Tests

Run against local AEM instance:

```bash
mvn clean verify -Plocal
```

#### Integration Test Configuration

| Property | Description | Default |
|----------|-------------|---------|
| `it.author.url` | Author instance URL | `http://localhost:4502` |
| `it.author.user` | Author admin user | `admin` |
| `it.author.password` | Author admin password | `admin` |
| `it.publish.url` | Publish instance URL | `http://localhost:4503` |
| `it.publish.user` | Publish admin user | `admin` |
| `it.publish.password` | Publish admin password | `admin` |

## Key Packages and Classes

### Services (`com.schemaapp.core.services`)

| Class | Purpose |
|-------|---------|
| `CDNDataProcessor` | Main orchestrator for data sync |
| `BulkDataLoaderAPIService` | Fetches data from Schema App API |
| `CDNHandlerService` | Saves data to JCR nodes |
| `FlushService` | Dispatcher cache invalidation |
| `PageJSONDataReaderService` | Reads cached JSON-LD data |

### Models (`com.schemaapp.core.models`)

| Class | Purpose |
|-------|---------|
| `SchemaAppConfig` | Configuration POJO |
| `EntityDataModel` | Sling Model for entity data component |
| `CDNEntity` | Jackson model for API responses |

### Schedulers (`com.schemaapp.core.schedulers`)

| Class | Purpose |
|-------|---------|
| `SchemaAppDataAPIScheduler` | Triggers periodic data sync |

### Utilities (`com.schemaapp.core.util`)

| Class | Purpose |
|-------|---------|
| `Constants` | Shared constant values |
| `ConfigurationUtil` | OSGi configuration helper |
| `UrlUtils` | URL manipulation utilities |
| `JsonSanitizer` | JSON data sanitization |

### UI Components (`/apps/schemaApp`)

| Path | Purpose |
|------|---------|
| `/apps/schemaApp/components/content/entitydata` | Entity data storage component |
| `/apps/schemaApp/registration/components/schemaapp` | Cloud service registration |
| `/apps/schemaApp/utilities/schemaAppcloudconfig` | Configuration wizard |
| `/apps/schemaApp/config` | OSGi configurations |

## Maven Repository

The project uses Adobe's public Maven repository. Configure in your Maven settings:

```xml
<repository>
    <id>adobe-public-releases</id>
    <name>Adobe Public Repository</name>
    <url>https://repo.adobe.com/nexus/content/groups/public</url>
</repository>
```

See: <http://helpx.adobe.com/experience-manager/kb/SetUpTheAdobeMavenRepository.html>

## Support

- **Schema App Support:** <support@schemaapp.com>
- **Schema App Website:** <https://www.schemaapp.com>
