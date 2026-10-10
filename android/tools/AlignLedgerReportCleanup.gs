/*
 * AlignLedger temporary party-report cleanup.
 *
 * SETUP:
 * 1. Create a Google Apps Script project using the SAME Google account
 *    whose Drive is used by AlignLedger.
 * 2. Add the Advanced Google service "Drive API" (v3).
 * 3. Paste this file into Code.gs and save.
 * 4. Run setupAlignLedgerReportCleanup once and approve its permissions.
 *
 * The cleanup only considers files tagged by the Android report uploader.
 * It does not delete Ledger Backup.db or unrelated Drive files.
 */

function setupAlignLedgerReportCleanup() {
  ScriptApp.getProjectTriggers()
    .filter(function (t) {
      return t.getHandlerFunction() ===
        'cleanupExpiredAlignLedgerReports';
    })
    .forEach(function (t) {
      ScriptApp.deleteTrigger(t);
    });

  ScriptApp.newTrigger('cleanupExpiredAlignLedgerReports')
    .timeBased()
    .everyMinutes(5)
    .create();

  cleanupExpiredAlignLedgerReports();
}

function cleanupExpiredAlignLedgerReports() {
  var now = Date.now();
  var pageToken;

  do {
    var args = {
      q: "trashed = false and properties has " +
         "{ key='alignledgerTempReport' and value='true' }",
      pageSize: 1000,
      fields: "nextPageToken,files(id,name,createdTime,properties)"
    };
    if (pageToken) args.pageToken = pageToken;

    var response = Drive.Files.list(args);
    var files = response.files || [];

    files.forEach(function (file) {
      var props = file.properties || {};
      if (props.alignledgerTempReport !== 'true') return;

      var expiresAt = Number(props.expiresAt || 0);
      if (!expiresAt && file.createdTime) {
        expiresAt = new Date(file.createdTime).getTime() +
          24 * 60 * 60 * 1000;
      }

      if (expiresAt > 0 && now >= expiresAt) {
        try {
          Drive.Files.delete(file.id);
          console.log('Deleted expired AlignLedger report: ' + file.id);
        } catch (e) {
          console.error(
            'Could not delete expired report ' + file.id + ': ' + e
          );
        }
      }
    });

    pageToken = response.nextPageToken;
  } while (pageToken);
}
