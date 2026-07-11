# Implementation Plan - Location Fallback Strategy

Refactor `CurrentLocationFetcher.kt` to improve code quality, error handling, and implement an IP-based location fallback strategy when GPS/Network location is unavailable or permissions are denied.

## Proposed Changes

### Infrastructure

#### [MODIFY] [CurrentLocationFetcher.kt](file:///F:/ProjectsFiles/AndroidStudio_Projects/Mawaqit/app/src/main/java/com/hussein/mawaqit/infrastructure/location/CurrentLocationFetcher.kt)

-   **IP-Based Fallback**: Integrate `https://ipapi.co/json/` as a fallback when:
    -   Location permissions are not granted.
    -   Location services are disabled on the device.
    -   GPS/Network location fix fails or times out.
-   **Networking**: Use Ktor `HttpClient` with `ContentNegotiation` and `kotlinx.serialization` to fetch and parse the IP location data.
-   **Permission Handling**: Move permission check logic into the `fetch()` method to handle the fallback automatically.
-   **Code Quality & Error Handling**:
    -   Use `withTimeoutOrNull` for fresh location requests (already exists, but will be refined).
    -   Ensure all asynchronous operations are properly handled and cancelled if needed.
    -   Add logging for better debugging of the location acquisition process.
    -   Use a lazy-initialized `HttpClient` to optimize resources.
    -   Improve the `requestFreshLocation` and `getLastKnownLocation` methods to be more robust.

## Verification Plan

### Automated Tests
-   Since this involves physical sensors and network calls, I will verify the code structure and compilation.
-   (Optional) If unit testing infrastructure is set up, I could mock the `FusedLocationProviderClient` and `HttpClient` to verify the fallback logic.

### Manual Verification
1.  **Grant Permissions**: Verify that the app still uses GPS/Network when permissions are granted.
2.  **Deny Permissions**: Verify that the app falls back to IP-based location when permissions are denied during onboarding or in settings.
3.  **Disable GPS**: Verify that the app falls back to IP-based location when GPS is disabled but permissions are granted.
4.  **Simulate Timeout**: Verify that the app falls back to IP-based location if the fresh fix times out (e.g., in a basement).
