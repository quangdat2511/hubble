---
title: Material 3 UI Rules — Android Chat App

---

# Material 3 UI Rules — Android Chat App

> A living style guide for our Discord-like Android app. All UI contributions must conform to these rules before review approval. Last updated: 2026-03-06.

---

## Table of Contents

1. [Design System Foundation](#1-design-system-foundation)
2. [Color System](#2-color-system)
3. [Typography](#3-typography)
4. [Spacing & Layout](#4-spacing--layout)
5. [Component Rules](#5-component-rules)
6. [Navigation Patterns](#6-navigation-patterns)
7. [Chat & Messaging UI](#7-chat--messaging-ui)
8. [Icons & Imagery](#8-icons--imagery)
9. [Motion & Animation](#9-motion--animation)
10. [Theming (Light / Dark)](#10-theming-light--dark)
11. [Accessibility](#11-accessibility)
12. [Code Conventions](#12-code-conventions)
13. [Anti-Patterns (Never Do)](#13-anti-patterns-never-do)

---

## 1. Design System Foundation

We use **Material Design 3 (Material You)** as the single source of truth for all UI decisions. Do not invent custom design patterns that already exist in M3.

### Key Principles

| Principle | What it means for us |
|---|---|
| **Adaptive** | UI adapts to user's dynamic color, screen size, and input method |
| **Personal** | Respect system-level theme (wallpaper-derived colors via Monet on Android 12+) |
| **Expressive** | Use M3 components to convey hierarchy and interactivity clearly |

### Dependency

```kotlin
// build.gradle (app)
implementation("com.google.android.material:material:1.12.0") // always use latest stable
```

Always check [material.io/develop/android](https://material.io/develop/android) for the current stable version before starting a feature.

---

## 2. Color System

### 2.1 Token Hierarchy

Never use hardcoded hex values in layouts or code. Always reference semantic color tokens in this order of preference:

```
System tokens  →  Role tokens  →  Custom tokens (last resort)
```

### 2.2 Required Color Roles

| Token | Usage |
|---|---|
| `colorPrimary` | Primary actions, FABs, active nav indicators |
| `colorOnPrimary` | Text/icons on primary backgrounds |
| `colorSecondary` | Secondary actions, chips, badges |
| `colorTertiary` | Accent highlights (e.g., online status dot) |
| `colorSurface` | Card and sheet backgrounds |
| `colorSurfaceVariant` | Input field backgrounds, secondary containers |
| `colorBackground` | Screen background |
| `colorError` | Error states, destructive actions |
| `colorOutline` | Dividers, borders, unfocused fields |
| `colorOnSurfaceVariant` | Secondary text, placeholder text, icons |

### 2.3 App-Specific Semantic Tokens

Define these in `res/values/colors.xml` as references to M3 roles, not raw hex:

```xml
<!-- res/values/colors.xml -->
<color name="color_online_status">@color/md_theme_tertiary</color>
<color name="color_unread_badge">@color/md_theme_error</color>
<color name="color_mention_highlight">@color/md_theme_secondary_container</color>
<color name="color_own_message_bubble">@color/md_theme_primary_container</color>
<color name="color_other_message_bubble">@color/md_theme_surface_variant</color>
```

### 2.4 Dynamic Color (Monet)

Dynamic Color must be enabled on Android 12+ devices. Wrap the theme application in `MainActivity`:

```kotlin
// MainActivity.kt
override fun onCreate(savedInstanceState: Bundle?) {
    // Apply dynamic color before setContentView
    DynamicColors.applyToActivityIfAvailable(this)
    super.onCreate(savedInstanceState)
    setContentView(...)
}
```

On Android < 12, fall back to the branded seed palette defined in `res/values/themes.xml`.

### 2.5 Rules

- ✅ Always use `?attr/colorXxx` in XML layouts
- ✅ Always use `MaterialColors.getColor(view, R.attr.colorXxx)` in Kotlin
- ❌ Never hardcode `#RRGGBB` values in layouts or Kotlin
- ❌ Never create a color that is simply a lighter/darker version of a role — use the matching `container` or `onXxx` role instead

---

## 3. Typography

### 3.1 Type Scale

We use the M3 type scale exclusively. Map our UI contexts to M3 roles:

| M3 Role | Size | Weight | Our Usage |
|---|---|---|---|
| `displayLarge` | 57sp | Regular | — (not used) |
| `headlineLarge` | 32sp | Regular | Server/channel name headers |
| `headlineMedium` | 28sp | Regular | Sheet titles |
| `headlineSmall` | 24sp | Regular | Section headers |
| `titleLarge` | 22sp | Regular | Toolbar titles |
| `titleMedium` | 16sp | Medium | Message sender name |
| `titleSmall` | 14sp | Medium | Channel names in list |
| `bodyLarge` | 16sp | Regular | Message body text |
| `bodyMedium` | 14sp | Regular | Secondary descriptions |
| `bodySmall` | 12sp | Regular | Timestamps, captions |
| `labelLarge` | 14sp | Medium | Buttons, tabs |
| `labelMedium` | 12sp | Medium | Chips, badges |
| `labelSmall` | 11sp | Medium | Tiny labels |

### 3.2 Applying Text Styles

```xml
<!-- In XML layouts -->
<TextView
    android:textAppearance="?attr/textAppearanceBodyLarge"
    ... />
```

```kotlin
// In Kotlin (e.g., dynamic text)
textView.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
```

### 3.3 Rules

- ✅ Only use `?attr/textAppearanceXxx` references in XML
- ✅ Increase text size for accessibility via system font scale — never block it with `sp` → `dp` conversion
- ❌ Never set `android:textSize` with a raw value
- ❌ Never set `android:fontFamily` directly — use the type scale
- ❌ Never use more than 3 different type roles on a single screen

---

## 4. Spacing & Layout

### 4.1 Grid & Keyline

We use an **8dp base grid**. All padding, margin, and size values must be multiples of 4dp (preferred: 4, 8, 12, 16, 24, 32, 48, 64).

```xml
<!-- res/values/dimens.xml -->
<dimen name="spacing_xs">4dp</dimen>
<dimen name="spacing_sm">8dp</dimen>
<dimen name="spacing_md">16dp</dimen>
<dimen name="spacing_lg">24dp</dimen>
<dimen name="spacing_xl">32dp</dimen>
<dimen name="spacing_xxl">48dp</dimen>
```

Always reference these tokens. Never write `android:padding="10dp"`.

### 4.2 Screen Edge Margins

| Screen width | Edge margin |
|---|---|
| Compact (< 600dp) | 16dp |
| Medium (600–839dp) | 24dp |
| Expanded (≥ 840dp) | 24dp + auto-centered content max 840dp |

Use `WindowSizeClass` to apply adaptive margins:

```kotlin
val windowSizeClass = calculateWindowSizeClass(this)
val margin = when (windowSizeClass.widthSizeClass) {
    WindowWidthSizeClass.Compact -> 16.dp
    WindowWidthSizeClass.Medium -> 24.dp
    else -> 24.dp
}
```

### 4.3 Component Spacing

| Context | Value |
|---|---|
| Between list items | 0dp (dividers handle separation) |
| Between cards | 8dp |
| Content inside cards | 16dp |
| FAB bottom margin from screen edge | 16dp |
| Between avatar and message text | 12dp |
| Between message groups (same sender) | 2dp |
| Between message groups (different sender) | 12dp |

---

## 5. Component Rules

All components must be sourced from `com.google.android.material` (MDC-Android). Do not use AppCompat equivalents when an M3 counterpart exists.

### 5.1 Buttons

| Variant | When to use |
|---|---|
| `MaterialButton` (filled) | Primary action per screen (one max) |
| `MaterialButton` (tonal) | Secondary actions |
| `MaterialButton` (outlined) | Neutral/cancel actions |
| `MaterialButton` (text) | Inline low-emphasis actions |
| `MaterialButton` (elevated) | Floating actions over surfaces |

```xml
<!-- Correct: M3 filled button -->
<com.google.android.material.button.MaterialButton
    style="@style/Widget.Material3.Button"
    android:text="Send Message"
    ... />

<!-- Correct: M3 tonal button -->
<com.google.android.material.button.MaterialButton
    style="@style/Widget.Material3.Button.TonalButton"
    android:text="Join Server"
    ... />
```

- ✅ Minimum touch target: 48×48dp
- ✅ Use icon buttons (`Widget.Material3.Button.IconButton`) for toolbar/header actions
- ❌ Never use `Button` or `AppCompatButton`
- ❌ No more than 2 buttons side-by-side in a row

### 5.2 Text Fields

Always use `TextInputLayout` + `TextInputEditText`. Never use a bare `EditText`.

```xml
<com.google.android.material.textfield.TextInputLayout
    style="@style/Widget.Material3.TextInputLayout.FilledBox"
    android:hint="Search channels"
    app:startIconDrawable="@drawable/ic_search">

    <com.google.android.material.textfield.TextInputEditText
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />

</com.google.android.material.textfield.TextInputLayout>
```

| Use case | Style |
|---|---|
| Message composer | `FilledBox` |
| Search | `FilledBox` with start icon |
| Settings / forms | `OutlinedBox` |

### 5.3 Cards

```xml
<com.google.android.material.card.MaterialCardView
    style="@style/Widget.Material3.CardView.Filled"
    app:cardCornerRadius="12dp"
    ...>
```

| Variant | When to use |
|---|---|
| `Filled` | Default content cards (server cards, embeds) |
| `Elevated` | Floating overlays, previews |
| `Outlined` | Secondary groupings within a card |

- ✅ Corner radius: always 12dp for cards
- ❌ Never set `cardElevation` manually on `Filled` cards

### 5.4 Chips

Use `Chip` for: message reactions, user roles/tags, filter pills.

```xml
<com.google.android.material.chip.Chip
    style="@style/Widget.Material3.Chip.Assist"
    android:text="😄 3"
    ... />
```

| Style | When to use |
|---|---|
| `Chip.Assist` | Reaction chips, quick-action chips |
| `Chip.Filter` | Channel filter chips, role filters |
| `Chip.Input` | Tagged users in mentions |
| `Chip.Suggestion` | Autocomplete suggestions |

### 5.5 Bottom Sheets

- Use `ModalBottomSheet` for contextual menus (long-press message actions, member options)
- Use `StandardBottomSheet` only for persistent panels (e.g., members sidebar on tablet)
- Set `app:behavior_peekHeight` to `56dp` + system navigation bar height
- Always add `app:behavior_halfExpandedRatio` for tall content

### 5.6 Dialogs

Use `MaterialAlertDialogBuilder` — never `AlertDialog.Builder`.

```kotlin
MaterialAlertDialogBuilder(context)
    .setTitle("Delete Message")
    .setMessage("This action cannot be undone.")
    .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
    .setPositiveButton("Delete") { _, _ -> viewModel.deleteMessage(id) }
    .show()
```

- ✅ Destructive confirm buttons use `colorError`
- ❌ Never show more than 3 buttons in a dialog
- ❌ Never use dialogs for simple confirmations that could be a Snackbar

### 5.7 Snackbars

```kotlin
Snackbar.make(view, "Message sent", Snackbar.LENGTH_SHORT)
    .setAction("Undo") { viewModel.undoSend() }
    .show()
```

- ✅ Use for non-blocking feedback (message sent, user muted, settings saved)
- ✅ Max one action per Snackbar
- ❌ Never use `Toast` — always use `Snackbar`
- ❌ Never queue multiple Snackbars — cancel the previous one first

### 5.8 Progress Indicators

| Indicator | When to use |
|---|---|
| `CircularProgressIndicator` | Loading avatars, sending messages, actions |
| `LinearProgressIndicator` | File upload/download progress |

```xml
<com.google.android.material.progressindicator.CircularProgressIndicator
    style="@style/Widget.Material3.CircularProgressIndicator.Small"
    ... />
```

- ✅ Show a shimmer (skeleton) layout instead of a spinner when loading list content
- ❌ Never use `ProgressBar` (use M3 indicators)

---

## 6. Navigation Patterns

### 6.1 Primary Navigation Structure

```
NavigationDrawer (servers)
    └── NavigationRail or BottomNavigationView (channels/DMs)
            └── Fragment content area
```

| Screen width | Pattern |
|---|---|
| Compact (phone) | `BottomNavigationView` (2–5 destinations) |
| Medium (foldable/small tablet) | `NavigationRail` |
| Expanded (tablet) | `NavigationDrawer` (permanent) |

### 6.2 Implementation

```xml
<!-- Compact: Bottom Nav -->
<com.google.android.material.bottomnavigation.BottomNavigationView
    style="@style/Widget.Material3.BottomNavigationView"
    app:menu="@menu/bottom_nav_menu"
    ... />

<!-- Medium: Rail -->
<com.google.android.material.navigationrail.NavigationRailView
    style="@style/Widget.Material3.NavigationRailView"
    ... />
```

### 6.3 Rules

- ✅ Use `NavigationBarView` as the base type when writing adaptive code
- ✅ Active destination always has `colorSecondaryContainer` indicator
- ❌ Never use 6+ items in Bottom Navigation — use a drawer instead
- ❌ Never nest navigations (no Bottom Nav inside a Bottom Sheet)

### 6.4 Top App Bar

| Variant | When to use |
|---|---|
| `TopAppBar` (small) | Most screens |
| `MediumTopAppBar` | Channel/DM detail screens |
| `LargeTopAppBar` | Server info / profile screens |
| `CollapsingToolbarLayout` | Wrap `LargeTopAppBar` for scroll-collapse |

```xml
<com.google.android.material.appbar.MaterialToolbar
    style="@style/Widget.Material3.Toolbar.Surface"
    android:title="@string/channel_name"
    ... />
```

---

## 7. Chat & Messaging UI

This section covers the most complex and custom part of our app.

### 7.1 Message List

- Use `RecyclerView` with `LinearLayoutManager(VERTICAL, stackFromEnd = true)`
- Implement view type separation: own messages vs others, with/without avatar, system messages
- Minimum bubble padding: 12dp horizontal, 8dp vertical
- Message bubbles use `ShapeableImageView` / a `MaterialCardView` with shaped corners:

```xml
<!-- Own message (right-aligned, primary container) -->
app:shapeAppearanceOverlay="@style/ShapeAppearance.App.OwnMessageBubble"

<!-- Other message (left-aligned, surface variant) -->
app:shapeAppearanceOverlay="@style/ShapeAppearance.App.OtherMessageBubble"
```

```xml
<!-- res/values/shape_appearances.xml -->
<style name="ShapeAppearance.App.OwnMessageBubble">
    <item name="cornerFamily">rounded</item>
    <item name="cornerSizeTopLeft">16dp</item>
    <item name="cornerSizeTopRight">4dp</item>
    <item name="cornerSizeBottomLeft">16dp</item>
    <item name="cornerSizeBottomRight">16dp</item>
</style>

<style name="ShapeAppearance.App.OtherMessageBubble">
    <item name="cornerFamily">rounded</item>
    <item name="cornerSizeTopLeft">4dp</item>
    <item name="cornerSizeTopRight">16dp</item>
    <item name="cornerSizeBottomLeft">16dp</item>
    <item name="cornerSizeBottomRight">16dp</item>
</style>
```

### 7.2 Message Composer

```
[ Avatar ] [ TextInputLayout (FilledBox) ] [ Send IconButton ]
                                            [ Attach IconButton ]
```

- Use `TextInputLayout` style `FilledBox` with `cornerRadius="24dp"` for pill shape
- Send button: `Widget.Material3.Button.IconButton.Filled` with `colorPrimary`
- Show send button only when input is non-empty; show attachment button otherwise (animate transition)

### 7.3 User Avatars

- Shape: always circular — use `ShapeableImageView` with `@style/ShapeAppearance.Material3.Corner.Full`
- Sizes: 40dp (message list), 32dp (compact/grouped), 56dp (profile header), 72dp (server icon)
- Online status dot: 10dp circle, `colorTertiary`, white border 2dp, positioned bottom-right of avatar
- Load with Coil or Glide; always set a placeholder using `MaterialShapeDrawable`

### 7.4 Reactions

- Use `ChipGroup` with `Widget.Material3.Chip.Assist`
- Selected (own reaction): `chipBackgroundColor` = `colorSecondaryContainer`
- Unselected: `chipBackgroundColor` = `colorSurfaceVariant`
- Chip text: emoji + count (e.g., "👍 5")
- Maximum visible chips per message: 8 (show "+N more" chip after)

### 7.5 Unread & Mention Indicators

- Unread badge on channel: `BadgeDrawable` from MDC attached to channel name view
- Mention badge: `colorError` background, white text
- Unread divider in message list: a full-width row with `colorError` line and "NEW" label (`labelSmall`, `colorError`)

---

## 8. Icons & Imagery

### 8.1 Icon Source

Use **Material Symbols** (outlined variant, weight 400, grade 0, optical size 24) as the default. Import via the `material-symbols` library or use `VectorDrawable` exports.

```kotlin
// build.gradle
implementation("com.google.android.material:material:1.12.0") // includes M3 icons
```

### 8.2 Icon Sizes

| Context | Size |
|---|---|
| Toolbar / navigation | 24dp |
| List item leading icon | 24dp |
| Chip icon | 18dp |
| FAB icon | 24dp |
| Small indicator icon | 16dp |

### 8.3 Icon Tinting

Always tint icons via `?attr/colorOnSurface` or the relevant semantic token — never hardcode icon colors.

```xml
<ImageView
    android:src="@drawable/ic_notifications"
    app:tint="?attr/colorOnSurfaceVariant"
    ... />
```

### 8.4 Emoji

- Use the system emoji font for all emoji rendering — do not bundle a third-party emoji font unless targeting API < 29
- For API 29+, use `EmojiCompat` with `BundledEmojiCompatConfig` as the fallback

---

## 9. Motion & Animation

We follow the **M3 Motion** system: Emphasized, Standard, Decelerated, and Accelerated curves.

### 9.1 Duration Tokens

```xml
<!-- res/values/motion.xml -->
<integer name="motion_duration_short1">50</integer>
<integer name="motion_duration_short2">100</integer>
<integer name="motion_duration_medium1">200</integer>
<integer name="motion_duration_medium2">300</integer>
<integer name="motion_duration_long1">400</integer>
<integer name="motion_duration_long2">500</integer>
```

### 9.2 Easing

| Curve | Token | Use case |
|---|---|---|
| Emphasized | `PathInterpolator(0.2f, 0f, 0f, 1f)` | Screen transitions, bottom sheet expand |
| Standard | `FastOutSlowInInterpolator` | Most UI state changes |
| Decelerated | `DecelerateInterpolator` | Elements entering the screen |
| Accelerated | `AccelerateInterpolator` | Elements leaving the screen |

### 9.3 Screen Transitions

Use `MaterialSharedAxis` and `MaterialFadeThrough` from MDC:

```kotlin
// Navigating into a channel
exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)

// Switching tabs
exitTransition = MaterialFadeThrough()
enterTransition = MaterialFadeThrough()
```

### 9.4 Rules

- ✅ Duration 200–300ms for most interactions
- ✅ Respect `Settings > Accessibility > Remove animations` — check `animator_duration_scale`
- ❌ Never animate more than 3 properties simultaneously
- ❌ No animations > 500ms except full-screen transitions

---

## 10. Theming (Light / Dark)

### 10.1 Theme Inheritance

```xml
<!-- res/values/themes.xml (Light) -->
<style name="Theme.App" parent="Theme.Material3.DayNight.NoActionBar">
    <item name="colorPrimary">@color/seed_primary</item>
    ...
</style>
```

Always use `DayNight` variants — never hardcode a `Light` or `Dark` parent theme.

### 10.2 Dark Mode Rules

- ✅ All colors must be defined in both `res/values/colors.xml` and `res/values-night/colors.xml`
- ✅ Surfaces get darker in dark mode (M3 elevation overlay is automatic — do not disable it)
- ✅ Test every new screen in both light and dark before marking the task done
- ❌ Never use `FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS` manually — use `WindowCompat.setDecorFitsSystemWindows(window, false)`
- ❌ Never check `(resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)` to branch color logic — always use theme attributes

### 10.3 Status Bar & Navigation Bar

```kotlin
// MainActivity.kt
WindowCompat.setDecorFitsSystemWindows(window, false)
// Then handle insets in each fragment via ViewCompat.setOnApplyWindowInsetsListener
```

Edge-to-edge must be enabled. The app must not draw behind the status bar without proper inset handling.

---

## 11. Accessibility

### 11.1 Touch Targets

Every interactive element must have a minimum touch target of **48×48dp**. For smaller visual elements, use `android:minWidth`, `android:minHeight`, or `ViewCompat.setMinimumHeight`.

### 11.2 Content Descriptions

```xml
<!-- Always provide for icons and images with meaning -->
<ImageButton
    android:contentDescription="@string/cd_send_message"
    ... />

<!-- Explicitly hide decorative images -->
<ImageView
    android:importantForAccessibility="no"
    ... />
```

- Every interactive icon must have a `contentDescription`
- Status indicators (online dot) must have a content description (e.g., "Online")
- Decorative images must be `importantForAccessibility="no"`

### 11.3 Color Contrast

Minimum contrast ratios (WCAG AA):

| Element | Ratio |
|---|---|
| Normal text (< 18sp) | 4.5:1 |
| Large text (≥ 18sp or 14sp bold) | 3:1 |
| UI components & icons | 3:1 |

Test contrast with [Material Theme Builder](https://m3.material.io/theme-builder) or Android Studio's Layout Inspector.

### 11.4 TalkBack

- All screens must be navigable by TalkBack in a logical order
- Use `ViewCompat.setAccessibilityHeading(view, true)` for section headers in lists
- Merge focusable child views into a single group using `android:focusable` on the parent where appropriate

---

## 12. Code Conventions

### 12.1 File & Resource Naming

| Resource type | Naming convention | Example |
|---|---|---|
| Layouts | `<type>_<screen>_<variant>.xml` | `fragment_channel_list.xml` |
| Drawables | `ic_<name>.xml` / `bg_<name>.xml` | `ic_hashtag.xml` |
| Strings | `<screen>_<element>_<description>` | `channel_list_empty_label` |
| Dimensions | `spacing_<size>` / `<component>_<property>` | `message_bubble_padding` |
| Colors | `color_<semantic_name>` | `color_online_status` |
| Styles | `Widget.App.<Component>` | `Widget.App.MessageBubble` |

### 12.2 ViewBinding

Always use ViewBinding — never `findViewById`.

```kotlin
private var _binding: FragmentChannelListBinding? = null
private val binding get() = _binding!!

override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    _binding = FragmentChannelListBinding.bind(view)
}

override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
}
```

### 12.3 Theming in Code

```kotlin
// ✅ Correct: resolve theme attribute
val color = MaterialColors.getColor(view, com.google.android.material.R.attr.colorPrimary)

// ❌ Wrong: hardcoded color
val color = Color.parseColor("#6750A4")
```

### 12.4 Style Hierarchy (most → least preferred)

1. Widget-level style in XML (`style="@style/Widget.Material3.Xxx"`)
2. Theme-level attribute override in `themes.xml`
3. Inline `app:` attribute on the view (only for one-off overrides)
4. Programmatic override in Kotlin (last resort)

---

## 13. Anti-Patterns (Never Do)

These are hard violations that will block PR approval.

| ❌ Anti-Pattern | ✅ Correct Approach |
|---|---|
| `android:textColor="#333333"` | `android:textColor="?attr/colorOnSurface"` |
| `android:background="#FFFFFF"` | `android:background="?attr/colorSurface"` |
| `new AlertDialog.Builder(ctx)` | `new MaterialAlertDialogBuilder(ctx)` |
| `Toast.makeText(...)` | `Snackbar.make(...)` |
| `EditText` directly | `TextInputLayout` + `TextInputEditText` |
| `Button` / `AppCompatButton` | `MaterialButton` |
| `android:padding="10dp"` | `android:padding="@dimen/spacing_sm"` |
| `android:textSize="14dp"` | `android:textAppearance="?attr/textAppearanceBodyMedium"` |
| Overriding `MaterialButton` background with custom drawable | Use style variants or `app:backgroundTint` |
| Setting icon colors with raw `android:tint` | `app:tint="?attr/colorOnSurfaceVariant"` |
| Custom circular button shape built from scratch | `Widget.Material3.Button.IconButton.Filled.Tonal` |
| Hardcoded `dp` dimensions not in `dimens.xml` | Extract to `@dimen/` token |
| `RecyclerView` without `clipToPadding="false"` when using edge-to-edge | Always set `clipToPadding="false"` + `paddingBottom` = nav bar height |

---

## References

- [Material Design 3 — material.io](https://m3.material.io)
- [MDC-Android Components](https://github.com/material-components/material-components-android)
- [Material Theme Builder](https://m3.material.io/theme-builder)
- [Material Design 3 Color Roles](https://m3.material.io/styles/color/roles)
- [Android Accessibility Guidelines](https://developer.android.com/guide/topics/ui/accessibility)
- [WindowSizeClass — Adaptive Layouts](https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes)