# Service Center app (Phase 1, offline)

Android app for a mobile and laptop service center: customers, repair jobs, advance and balance payments,
per-job expenses, walk-in enquiries, staff PIN login, and daily and monthly reports. Everything is stored
on the phone in Room. No internet is needed.

## Open and run
1. Open this folder in Android Studio (a recent version) and let Gradle sync.
   The Gradle wrapper JAR is not included. If Studio does not create it, run `gradle wrapper` once, or accept its prompt.
2. Versions are pinned to a mutually compatible older set (AGP 8.5.2, Kotlin 2.0.20, KSP, Hilt 2.52,
   Room 2.6.1, Compose BOM 2024.09.03). Newer Android Studio may offer to upgrade: use its Upgrade Assistant,
   and re-check KSP/Hilt compatibility after each step.
3. Run on a device or emulator with Android 8.0 (API 26) or higher.

This code was written without being compiled. Expect to fix a few small compile errors on the first build.

## First launch
1. The app asks for the owner's name and a 4-digit PIN.
2. Afterwards everyone logs in with their PIN. The Owner adds staff from Home, then the gear icon.

## Rules built in
- Staff: search, new customer, new job with advance, status updates, record payments, add expenses, walk-in enquiries.
- Owner only: reports and profit, edit final amount (discount), refunds, void payments and expenses, manage staff.
- Delivered needs a zero balance. A payment cannot exceed the balance. Closed jobs can only be changed by the Owner.
- Money is stored in paise. Profit = collected minus per-job expenses (no rent or salary).
- Statuses, payment modes, device types and role permissions are rows in the database, ready for the web Admin Panel.

## Layout
- `data/local`: Room entities, DAOs, seeded defaults, `AppDatabase`
- `data/auth`, `data/repository`: business rules and permission checks
- `ui/*`: Compose screens and ViewModels
- `ui/nav/AppNav.kt`: login, owner setup, bottom navigation, routes

## Known gaps (Phase 2)
- No lockout after wrong PIN attempts. A 4-digit PIN is a convenience lock, not strong security.
- No backup, restore, or sync yet. Supabase sync and the web Admin Panel are Phase 2.
  Exclude the `customer_seq` and `job_seq` settings from sync (device-local counters).
- No receipts, PDF export, or photos.
- No automated tests.
- Default launcher icon.
