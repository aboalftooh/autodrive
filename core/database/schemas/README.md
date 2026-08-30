# Room schemas

يولّد KSP مخططات Room داخل هذا المجلد أثناء البناء.

المسار المتوقع للنسخة الحالية:

```text
com.autodrive.app.core.database.AutoDriveDatabase/11.json
```

يجب حفظ ملف JSON المولّد في Git. يمنع رفع `AUTODRIVE_DATABASE_VERSION` دون:

1. Migration متصلة من النسخة الحالية.
2. إضافة Migration إلى `ALL_MIGRATIONS`.
3. نجاح `DatabaseMigrationTest`.
4. حفظ ملف Schema الجديد.
