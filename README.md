# Tally

Minimal Android companion for [hledger](https://hledger.org) — add journal
entries and see a quick balance/expenses overview on the go. It talks to the
**real hledger CLI running in Termux** via Termux's `RUN_COMMAND` intent, not
a custom reimplementation of the journal format — writes and reads always go
through the actual parser.

> [!NOTE]
> This is a personal, vibecoded project — built for my own use, not
> maintained as a product. **No support is given**: no issue triage, no
> feature requests, no guaranteed fixes. If it doesn't do what you need,
> fork it and point an AI coding agent at it; the codebase is small and
> straightforward enough that this works well.

## How it works

- **Add Entry**: date (native Material3 date picker), amount, payee, expense
  account (fuzzy-searched against your real `Expenses` accounts, or left
  blank to reuse the last one used), currency. Appends a well-formed
  transaction to a separate scratch journal (`phone-manual.journal` by
  default) — never touches your real journal files directly, so a bug here
  can't corrupt your actual ledger. Reconcile that scratch file into your real
  journal by hand, periodically.
- **Infos**: your real `Assets:Cash` balance, this/last month's `Expenses`
  (sorted highest-first, tap to cycle through depth 2→3→4), and the recent
  entries from the scratch journal — all fetched live from Termux's hledger.
- **Config**: theme, the scratch journal path, default entry currency, the
  reporting currency (`-X`) used for the Infos page's converted balances,
  refreshing the cached `Expenses` accounts, and accounts to hide from the
  Infos page's breakdowns.

## One-time phone setup

1. Install [Termux](https://github.com/termux/termux-app/releases) from
   GitHub Releases (not Play Store or F-Droid — both lag behind).
2. Inside Termux: `pkg install hledger`
3. Point hledger at your real journal: either export `LEDGER_FILE` in
   Termux's shell environment, or create `~/.hledger.conf` with a `-f
   /path/to/your/main.journal` line.
4. Still in Termux: `echo 'allow-external-apps = true' >> ~/.termux/termux.properties && termux-reload-settings`
5. Install this app and open it — it'll prompt for the "run commands in
   Termux" permission the first time it needs Termux. Grant it.
6. Open the **Config** tab and set the scratch journal path to somewhere
   under your synced storage (e.g. a Syncthing-shared folder), so entries
   added on the phone show up back on your other machines.

## Building

Open this folder in Android Studio, or run `./gradlew assembleRelease` with a
JDK 17+ and the Android SDK. Versions live in `gradle/libs.versions.toml`.

Release builds are signed with `app/release.p12` (gitignored). CI decodes it
from the base64 `HLEDGER_TALLY_KEYSTORE_BASE64` repository secret. Pushes to
`main` publish a release when `versionName` changes; other branches publish a
`dev-<branch>` prerelease.

## Project layout

```
app/src/main/java/dev/ghjkl/tally/
├── MainActivity.kt            — bottom-nav Scaffold, screen switching
├── data/
│   ├── AppSettings.kt         — settings (SharedPreferences, as Compose state)
│   └── Infos.kt               — Infos page content, kept across tab switches
├── termux/
│   ├── TermuxRunner.kt        — sends RUN_COMMAND, awaits the result via a
│   │                            PendingIntent-triggered Service
│   └── Hledger.kt             — the hledger invocations + output parsing
└── ui/
    ├── AccountSearchField.kt  — fuzzy-matched account dropdown
    ├── FuzzySearch.kt
    ├── RunCommandPermission.kt
    ├── theme/Theme.kt
    └── screens/
        ├── AddEntryScreen.kt
        ├── InfosScreen.kt
        └── ConfigScreen.kt
```

## Known limitations

- Reconciling the scratch journal into your real journal is manual, by design.
- Only `Expenses` accounts get autocomplete; no support for arbitrary postings
  (transfers, splits across multiple categories in one entry, etc.).
- Depends on Termux staying installed with `RUN_COMMAND` permission granted;
  if Termux's result-bundle extra keys/format ever change again in a future
  release, the parsing in `TermuxRunner.kt` may need updating to match.

## License

[Unlicense](LICENSE) — public domain. Do whatever you want with it.
