## Context

The book detail screen is shared by Android, iOS, and Desktop through Compose Multiplatform. Its `Book` model already exposes a nullable public `coverPath`; the backend-generated text-cover path uses the stable `book_<id>_generated.*` filename convention. The existing screen and Material components derive foreground colors from the active theme, so the background treatment must retain sufficient theme color contribution rather than introducing cover-derived text colors.

## Goals / Non-Goals

**Goals:**

- Add one shared background implementation for all Compose client targets.
- Preserve the current page appearance for generated text covers and missing covers.
- Keep background selection deterministic and independently unit testable.
- Maintain the existing screen state, navigation, refresh, dialogs, and content layout.

**Non-Goals:**

- Adding or changing backend cover metadata, APIs, or persistence.
- Detecting whether arbitrary image pixels contain text.
- Extracting dominant colors or dynamically recoloring foreground content.
- Changing the foreground cover image, reader background, or other screens.

## Decisions

### Resolve background eligibility from the existing cover-path contract

Normalize the nullable cover path and exclude filenames matching the backend text-cover convention. A real nonblank cover path is eligible; generated text covers and missing values are not. This avoids a cross-service schema change for a client-only visual enhancement. The alternative of image-content classification was rejected because it would be expensive, nondeterministic, and inconsistent across platforms.

### Render the cover behind a transparent detail scaffold

Place the eligible cover behind the existing scaffold, crop it to fill the page, and allow the detail scaffold and top app bar to reveal it. Keep the standard theme background as the base layer so loading or decoding failure naturally falls back without a blank or contrasting surface. The alternative of applying the image only to the header was rejected because the requested behavior concerns the detail-page background as a whole.

### Combine blur, reduced opacity, and theme overlay

Slightly enlarge the image before applying blur to avoid visible blur edges, reduce its opacity, and draw a translucent active-theme background color above it. This makes the cover recognizable while keeping the current Material foreground color contract. Dominant-color extraction was rejected because it adds platform and caching complexity without being necessary for the desired softened effect.

### Test the deterministic selection boundary

Keep cover eligibility as pure shared logic and cover real image, generated text-cover, null, and blank inputs in JVM tests. Cross-platform compilation verifies the shared rendering APIs on Android and iOS simulator targets; visual tuning remains a simulator or device observation rather than a unit-test claim.

## Risks / Trade-offs

- [Risk] The client relies on the backend-generated filename convention. → Mitigation: document the convention in the design and regression test absolute URLs with query parameters.
- [Risk] Highly saturated covers may still influence perceived contrast. → Mitigation: retain a full theme base layer plus blur, opacity reduction, and a theme-color overlay.
- [Risk] Blur can add rendering cost on lower-end devices. → Mitigation: render a single static background layer only on the detail page and avoid color analysis or animated effects.
- [Risk] A failed remote image load could otherwise expose a transparent page. → Mitigation: always draw the standard theme background beneath the optional image.

## Migration Plan

No data or API migration is required. Release the shared client update normally; rollback consists of removing the optional background layer and restoring the opaque detail scaffold, with no persisted-state cleanup.
