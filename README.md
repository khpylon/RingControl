# Ring Control widget for Android

## Intro

For years I used an widget from the Play Store to control the ring volume on my phone.  One day I needed to wipe my phone and reinstall everything from scratch, and to my surprise that app no longer worked on Android 16.  Don't know why.  That's why I wrote this app.

## Requirements

This app was developed on Android 16 but was tested on Android 11 (API 30).  There is no guarantee it will work on anything later.

## Contributing Language Translations

If you would like to use the app in your preferred language but there is no translation for it, read [how to submit one.](https://github.com/khpylon/RingControl/blob/master/TRANSLATIONS.md)

## Bug Reports

The app has the ability to detect an app crash and generate reports.  This happens whenever the app is run. It will write a file named *ringcontrol_logcat-<datetime>.txt* to the *Downloads* folder of your phone, and
display a brief message.  Google will also upload analytics when a crash occurs, but they often do not contain as much detail as the crash report. Since the app does not communicate with the Internet, you will need to manually upload the crash report file to ["Issues"](https://github.com/khpylon/RIngControl/issues) on GitHub.  I'll then try to figure out why the app crashed and how to fix it.

## Credits

The [ColorPickerView](https://github.com/skydoves/ColorPickerView) was written by skydoves (Jawwoong Eum), and copyrighted 2017 under the [Apache Licence, Version 2.0] (https://github.com/skydoves/ColorPickerView#license).

## Disclaimer

I am NOT liable for any kind of damage (special, direct, indirect, consequential or whatsoever) resulting from the use of this app. 