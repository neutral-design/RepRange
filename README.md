# RepRange

RepRange is an Android workout logging app for strength training.

The app is built for quick day-by-day gym logging, where you can save exercises, sets, reps, and weight, then look back at previous sessions and track estimated 1RM over time.

## Current purpose

RepRange started as a simple 1RM and rep range calculator, but is now evolving into a full local-first training diary focused on:

- fast logging during workouts
- simple day-based navigation
- estimated 1RM per set
- exercise history and progress
- local storage on the phone
- a clean foundation for future stats, charts, and export

## Current features

- Diary-based home screen focused on a selected date
- Navigate between days with previous/next controls
- Choose a date from a custom calendar with logged workout days highlighted
- Create the first workout session automatically when adding the first exercise on an empty day
- Add exercises with sets, reps, and weight
- Reuse previous exercise names through suggestions while typing
- Add multiple sessions on the same day when needed
- View estimated 1RM for logged sets
- Edit sets after logging
- Delete sets
- Rename exercises for a single logged workout entry
- Delete exercises
- Delete sessions
- View exercise history across previous workouts
- View simple progress charts for estimated 1RM and volume over time
- Persist workout data locally with Room

## Planned direction

The long-term goal for RepRange is to become a simple but powerful strength training tracker with features such as:

- CSV export
- optional starter list of common exercises
- better filtering for history and stats
- richer charts and analytics
- backup and restore options

## Tech stack

- Kotlin
- Jetpack Compose
- MVVM
- Room

## Development notes

- All workout data is currently stored locally on the device
- The app is under active development and the data model is being expanded feature by feature
