import fs from 'fs';
import path from 'path';

const KEY_ID = process.env.B2_KEY_ID || 'YOUR_B2_KEY_ID';
const APP_KEY = process.env.B2_APPLICATION_KEY || 'YOUR_B2_APPLICATION_KEY';
const BUCKET_NAME = process.env.B2_BUCKET_NAME || 'YOUR_B2_BUCKET_NAME';
const APK_PATH = path.resolve('../app/build/outputs/apk/release/app-release.apk');

async function main() {
  console.log(`Reading APK from: ${APK_PATH}...`);
  if (!fs.existsSync(APK_PATH)) {
    console.error('APK file not found!');
    process.exit(1);
  }

  const apkData = fs.readFileSync(APK_PATH);
  const fileSizeMb = (apkData.length / (1024 * 1024)).toFixed(2);
  console.log(`APK Size: ${fileSizeMb} MB (${apkData.length} bytes)`);

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
  const downloadUrl = authData.downloadUrl;
  console.log(`Authenticated! API URL: ${apiUrl}, Download URL: ${downloadUrl}`);

  let bucketId = authData.allowed?.bucketId;
  if (!bucketId) {
    const bucketRes = await fetch(`${apiUrl}/b2api/v2/b2_list_buckets`, {
      method: 'POST',
      headers: { Authorization: authToken, 'Content-Type': 'application/json' },
      body: JSON.stringify({ accountId: authData.accountId })
    });
    const bucketData = await bucketRes.json();
    const bucket = bucketData.buckets?.find(b => b.bucketName === BUCKET_NAME);
    bucketId = bucket?.bucketId;
  }
  console.log(`Using bucket: ${BUCKET_NAME} (ID: ${bucketId})`);

  // Target file names
  const targets = ['CameraBeauty.apk', 'app-release.apk'];

  for (const targetName of targets) {
    console.log(`Requesting upload URL for ${targetName}...`);
    const upRes = await fetch(`${apiUrl}/b2api/v2/b2_get_upload_url`, {
      method: 'POST',
      headers: { Authorization: authToken, 'Content-Type': 'application/json' },
      body: JSON.stringify({ bucketId })
    });

    const upData = await upRes.json();
    console.log(`Uploading ${targetName} to B2...`);

    const uploadFileRes = await fetch(upData.uploadUrl, {
      method: 'POST',
      headers: {
        Authorization: upData.authorizationToken,
        'X-Bz-File-Name': targetName,
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Length': apkData.length.toString(),
        'X-Bz-Content-Sha1': 'do_not_verify'
      },
      body: apkData
    });

    if (!uploadFileRes.ok) {
      throw new Error(`Upload failed for ${targetName}: ${await uploadFileRes.text()}`);
    }

    const uploaded = await uploadFileRes.json();
    const publicUrl = `${downloadUrl}/file/${BUCKET_NAME}/${targetName}`;
    console.log(`✅ Uploaded ${targetName} successfully!`);
    console.log(`Direct Download URL: ${publicUrl}`);
  }
}

main().catch(err => {
  console.error('Error:', err);
  process.exit(1);
});
