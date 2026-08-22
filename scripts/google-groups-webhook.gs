/**
 * Google Apps Script webhook for free @googlegroups.com member sync.
 *
 * Deploy as Web App: Execute as "Me", Who has access: "Anyone".
 * Free Gmail cannot GroupsApp.createGroup — create groups manually at groups.google.com,
 * paste the group email into the LMS course root, then use add/remove only.
 *
 * Expected JSON body:
 *   { "secret": "...", "action": "add"|"remove", "group": "course@googlegroups.com", "email": "student@gmail.com" }
 * Response:
 *   { "ok": true } or { "ok": false, "error": "..." }
 */
var WEBHOOK_SECRET = 'PASTE_SAME_SECRET_AS_GOOGLE_GROUPS_SECRET';

function doPost(e) {
  try {
    var body = JSON.parse(e.postData.contents || '{}');
    if (!body.secret || body.secret !== WEBHOOK_SECRET) {
      return json_({ ok: false, error: 'Unauthorized' });
    }

    var action = (body.action || '').toLowerCase();
    var groupEmail = (body.group || '').trim();
    var memberEmail = (body.email || '').trim();

    if (!groupEmail || !memberEmail) {
      return json_({ ok: false, error: 'group and email are required' });
    }

    if (action === 'add') {
      GroupsApp.getGroupByEmail(groupEmail).addMember(memberEmail);
      return json_({ ok: true });
    }

    if (action === 'remove') {
      GroupsApp.getGroupByEmail(groupEmail).removeMember(memberEmail);
      return json_({ ok: true });
    }

    return json_({ ok: false, error: 'Unsupported action: ' + action + ' (use add or remove)' });
  } catch (err) {
    return json_({ ok: false, error: String(err) });
  }
}

function json_(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
