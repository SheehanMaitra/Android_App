# Photos App Android Port

This repository contains an Android Studio version of the Photos project, written in Java with XML layouts and Kotlin DSL Gradle files.

## What the app does

- Loads saved album/photo data from the previous session using Java serialization in app-internal storage.
- Shows a home screen with album names in plain text.
- Lets the user create, open, rename, and delete albums.
- Shows album photos as thumbnail tiles.
- Lets the user add a photo from device storage, remove a photo, and move a photo to another album.
- Opens a photo viewer with manual slideshow controls for previous/next navigation.
- Lets the user add and delete `person` and `location` tags.
- Searches across all albums by tag-value prefix, with case-insensitive matching, autocomplete suggestions, and AND/OR support.

## Project layout

- `app/` is the Android Studio application module.
- `Photos41/` is the older JavaFX project kept here for reference/reuse of model ideas.

## Build notes

- Open the repo in Android Studio.
- Let Android Studio use the included Gradle wrapper.
- Install an Android SDK for API 36 if your machine does not already have it.
- Run on an emulator such as Pixel 6 / 1080 x 2400 / 420 dpi.

No external image-loading library such as Picasso is used.

## Storage approach

- Photo albums/tags are serialized to a file in the app's internal storage.
- Selected images are stored as persisted document URIs, so the app can reopen them later.

## AI usage note


1. "Help me set up an Android Studio project in Java using XML layouts and Kotlin DSL Gradle files."
2. "Show a good Java class design for albums, photos, and tags in an Android version of the Photos project."
3. "How can I save album and photo data locally on Android using Java serialization or simple file storage?"
4. "Help me build a home screen that lists album names and supports create, rename, delete, and open."
5. "How do I let the user pick image files from phone storage and keep access to them later?"
6. "Help me display album photos in a RecyclerView grid with thumbnail images, without using Picasso or other image libraries."
7. "How can I make a photo viewer screen with previous and next buttons for a manual slideshow?"
8. "Help me implement tags so only person and location are allowed."
9. "How should I add and delete tags from a photo in Android Java with dialog-based input?"
10. "How can I move a photo from one album to another while checking for duplicates in the target album?"
11. "Help me search photos across all albums by tag-value pairs with AND/OR logic."
12. "How can I add case-insensitive autocomplete for person/location tag values based on what was already entered before?"
13. "Help me organize the Android code so the model/data logic is separate from the activities and adapters."
14. "Can you help me clean up the XML layouts and make the screens easier to use on a phone-sized emulator?"


