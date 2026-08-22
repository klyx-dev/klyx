# Translating Klyx

Every language lives in a single plain-text JSON file in this folder:

- `en.json`: English (the source of truth)
- `hi.json`: Hindi
- add `<language-code>.json` for any new language

## Rules

1. **Translate only the text on the right side of the `:`.** Never change the
   words on the left (the keys), the app looks strings up by them.
2. **Keep the `{placeholders}` as they are.** Text like `{path}` or `{files}` is
   filled in by the app at runtime (a file path, a number, ...). Move it to
   wherever it fits naturally in your sentence, but never rename or delete it.
3. **`{message:Unknown error}` means "show this if there is no value".**
   Translate the part after the `:` too (e.g. `{message:अज्ञात त्रुटि}`).
4. **Missing keys are fine.** If you have not translated a string yet, just
   leave it out of your file, the app automatically shows the English text
   until you translate it. You can translate gradually, a few strings at a time.
5. **Do not edit `_schema.json`.** It is developer configuration.

## Adding a new language

1. Copy `en.json` and rename it to your language code (e.g. `fr.json`,
   `pt-BR.json`).
2. Translate the values. Delete any line you have not translated yet, English
   is shown for those automatically.
3. Send the file back (a pull request, an issue attachment, or however the
   project accepts contributions).

That's it. the language appears in the app's settings picker automatically
when the file is bundled, with its name shown in your own language.

