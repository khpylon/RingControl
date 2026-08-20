**2026.08.18-10 (this release)**
- Avoid silencing the ringer when permissions are not allowed.  In this case, the app will only switch to vibrate mode.
---
**2026.08.07-08**
- Clean up logic handling active calendar events.
- Set an alarm is a new event is discovered when listing events for the user.
---
**2026.08.07-08**
- Fix issue with calendar events not changing ringer mode on Android 17.
---
**2026.05.04-18**
- Make it optional for calendar entries to show a notification when the ring mode is changed (use #RingControl/N# or #RC/N#).
- Allow phone mode to be changed earlier before an event (use #RingControl/+X# or #RC/+X#).
---
**2025.11.17-18**
- Allow calendar entries to also set the ring mode to vibrate.
- Allow calendar entries to show a notification when the ring mode is changed.
---
**2025.10.17-12**
- Support for landscape orientation.
- Make widget behave better with other app launchers.
- Add drop-down menu to access use instructions, release notes, and info about the app.
- Change placement of mode description on the widget.
---
**2025.10.06-10**
- Allow calender events to silence ringer; just add #RC# or #RingControl# to the title or description of an event.
- Add setting to change battery optimization.
- Reorganize main screen to switch between widget appearance controls and permission settings.
- New logo for the app.
- Minor bug fixes.
---
**2025.09-25-14-beta**
- The widget's size can be adjusted, in case the launcher is clipping parts of it
- The widget updates when changes are made to the ringer externally
- Silent mode no longer turns on Do Not Disturb mode
---
**2025.09-25-10-beta**
- The size of the widget can be adjusted.
- The widget updates if changes are made to the ringer externally.  Right now it's not possible for the app to detect this immediately, so there may be up to a twenty second delay.
- Silent mode is done by muting the ring and notification channels instead of using Do Not Disturb mode.
