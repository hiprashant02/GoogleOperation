import fs from 'fs';

const KEY_ID = process.env.B2_KEY_ID || 'YOUR_B2_KEY_ID';
const APP_KEY = process.env.B2_APPLICATION_KEY || 'YOUR_B2_APPLICATION_KEY';
const BUCKET_NAME = process.env.B2_BUCKET_NAME || 'YOUR_B2_BUCKET_NAME';

async function main() {
  console.log('Authenticating with Backblaze B2...');
  const credentials = Buffer.from(`${KEY_ID}:${APP_KEY}`).toString('base64');
  const authRes = await fetch('https://api.backblazeb2.com/b2api/v2/b2_authorize_account', {
    headers: { Authorization: `Basic ${credentials}` }
  });

  if (!authRes.ok) {
    throw new Error(`Auth failed: ${await authRes.text()}`);
  }

  const authData = await authRes.json();
  const apiUrl = authData.apiUrl;
  const authToken = authData.authorizationToken;
  const bucketId = authData.allowed?.bucketId;

  console.log(`Listing and deleting files in bucket ${BUCKET_NAME} in parallel...`);
  let nextFileName = null;
  let totalDeleted = 0;

  do {
    const listRes = await fetch(`${apiUrl}/b2api/v2/b2_list_file_names`, {
      method: 'POST',
      headers: { Authorization: authToken, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        bucketId,
        maxFileCount: 1000,
        startFileName: nextFileName
      })
    });

    if (!listRes.ok) {
      throw new Error(`List failed: ${await listRes.text()}`);
    }

    const listData = await listRes.json();
    const files = (listData.files || []).filter(
      f => f.fileName !== 'CameraBeauty.apk' && f.fileName !== 'app-release.apk'
    );

    console.log(`Fetched page with ${files.length} files to delete. Processing in parallel batches...`);

    // Process in batches of 30 concurrent deletes
    const BATCH_SIZE = 30;
    for (let i = 0; i < files.length; i += BATCH_SIZE) {
      const batch = files.slice(i, i + BATCH_SIZE);
      await Promise.all(
        batch.map(async (file) => {
          try {
            const delRes = await fetch(`${apiUrl}/b2api/v2/b2_delete_file_version`, {
              method: 'POST',
              headers: { Authorization: authToken, 'Content-Type': 'application/json' },
              body: JSON.stringify({
                fileId: file.fileId,
                fileName: file.fileName
              })
            });
            if (delRes.ok) totalDeleted++;
          } catch (_) {}
        })
      );
      process.stdout.write(`\rProgress: ${totalDeleted} files deleted...`);
    }

    nextFileName = listData.nextFileName;
  } while (nextFileName);

  console.log(`\n\n✅ STORAGE CLEANUP COMPLETE: Deleted ${totalDeleted} files from Backblaze B2.`);
}

main().catch(err => {
  console.error('Error:', err);
  process.exit(1);
});
