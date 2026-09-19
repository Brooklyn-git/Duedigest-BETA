While AI assisted code is allowed, all AI written code needs to be reviewed and the app properly tested before submiting your request; your model also needs to follow this: 

## Style

- Always build the application after every modification, also remind the dev to test the application.
- Don't ask access outside the project directory unless it is 100% totally necessary
- When access outside of the project directory is granted, do not modify anything outside
- Always think about user's security first. The agent must work around the digital security, then the user's commodity and then the aesthetics.
- The code should be idiomatic and readable.
- Avoid unnecessary complexity. Stick to KISS (Keep It Simple Stupid).
- Avoid writting more than what is needed. Stick to DRY (Don't Repeat Youself). If there is a block of code that might be used for something else in a different part of the code, make it a reusable function.
- Think in making maintanable apps, think there might be changes later. 
- Don't "hardcode" things we might use more than once (like a value that can be in a variable)
- Avoid nesting where possible. Guard statements are hugely preferable.
- Avoid big functions, they should do one thing and do it well. The functions should be easily understood from the name alone.
- Avoid using propietary dependencies if possible. Always try free dependencies first.


## Testing

- Run the app and ensure your changes work as expected.
- Write tests for as much of your change as possible.
- Follow the TDD (Test-Driven Development) approach. This ensures you've found the precise issue you're trying to fix.
- Don't delete existing tests. If they're failing, that highlights something wrong with your code.

## Commits

- Keep commits small, focused, and atomic. They should be easily revertible.

## Constraints & Preferences
- Desktop app UI should match Android's TaskCard appearance (card with animated bg, course badge, colored relative-date label with icon, expand animation).
- FAB on tasks tab must be at bottom-right. "Clear completed" only shown when completed tasks exist.
- Date/time picker must use dropdowns (Year/Month/Day, Hour/Min, AM/PM) respecting 12h/24h setting, with localized month names.
- Month names must be text (Jan, Feb… / ene, feb…) and respect language setting.
- Dialogs must follow DueNestTheme (not default Compose colors).
- Horizontal padding everywhere: 32dp.
- Desktop layout must use a collapsible left sidebar (Connection / Tasks / Settings) instead of top tabs. Settings is a page on the right, not a dialog.
- No status text or progress bar on the Tasks tab.
- File paths in Output Settings use a graphical directory picker (JFileChooser).
- Password field must use PasswordVisualTransformation (dots).
- User-friendly sync language: no "server"/"IP"/"port" in UI. Use "Sync with nearby devices".
- Android scanner must be embedded inside the sync dialog (CameraX PreviewView), not full-screen.
- Android defaults to scan mode; QR display is secondary via "Show QR instead?" toggle.
