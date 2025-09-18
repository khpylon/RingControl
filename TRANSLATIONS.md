## Translations to Other Languages

This app is a written and maintained by a single individual (if you don't include [Stack Overflow](https://stackoverflow.com/)) whose only language is English.  Submissions from other people who are interested in the app being in their language is  
encouraged.  If you would like to contribute, here's how.

All strings which used in the app are located within [strings.xml](https://github.com/khpylon/RingControl/blob/master/app/src/main/res/values/strings.xml).  This file contains XML coded strings used throughout the application.  Some may be used in more than one place.

### Considerations when doing a translation

- *Strings not requiring translation*: Some strings are used internally and not visible to the user.  These strings will have an attribute **translatable="false"**.
- *Length specific strings*:  Some strings are used on actual widgets while others are used in buttons, menus, titles, etc.  The names of these strings all end in "**_label**".  a translated string which contains more characters than the original string may not display properly in the widget.   If the literal translation will not fit, try to use abbreviated words which will fit, or rephrase to something which preserves the meaning.
- *Apostrophes and quotes*: apostrophes (') and quotes (") inside strings need to be proceeded by a backslash (\\), so a word such as *l'application* should be written "**l\\'application**"
- *Format/pattern strings*: Strings may contains sequences such as **\\"{0}\\"** or be surrounded by **\<xliff:g\>** tags.  These are format or pattern strings used to create other strings.  Leave these sequences as-is in the translation.
- *Space before or after strings*:  If a string contains blank space before or after the string, it is enclosed inside quotes (**"Vehicle: "**).  Be sure to also enclose your translation in quotes and preserve the spaces.

### Contributing translations

Once you've translated *strings.xml*, open a new [Issue on GitHub](https://github.com/khpylon/RingControl/issues) and upload it.  Please explain which language you are submitting (so I don't have to guess, although that's always fun of course).