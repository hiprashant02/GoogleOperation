// Backblaze B2 Cloud Storage Client

export interface B2AuthResponse {
  apiUrl: string;
  authorizationToken: string;
  downloadUrl: string;
  allowed: {
    bucketId?: string;
    bucketName?: string;
  };
}

export interface B2UploadUrlResponse {
  bucketId: string;
  uploadUrl: string;
  authorizationToken: string;
}

let cachedAuth: { data: B2AuthResponse; expiresAt: number } | null = null;

export async function getB2Auth(keyId: string, appKey: string): Promise<B2AuthResponse> {
  const now = Date.now();
  if (cachedAuth && cachedAuth.expiresAt > now + 300000) { // 5 min grace
    return cachedAuth.data;
  }

  const credentials = btoa(`${keyId}:${appKey}`);
  const res = await fetch('https://api.backblazeb2.com/b2api/v2/b2_authorize_account', {
    method: 'GET',
    headers: {
      'Authorization': `Basic ${credentials}`
    }
  });

  if (!res.ok) {
    const errText = await res.text();
    throw new Error(`B2 authorization failed (${res.status}): ${errText}`);
  }

  const data = await res.json<B2AuthResponse>();
  cachedAuth = {
    data,
    expiresAt: now + (23 * 3600 * 1000) // B2 tokens valid for 24h
  };

  return data;
}

export async function getB2UploadUrl(keyId: string, appKey: string, bucketId: string): Promise<B2UploadUrlResponse> {
  const auth = await getB2Auth(keyId, appKey);

  const res = await fetch(`${auth.apiUrl}/b2api/v2/b2_get_upload_url`, {
    method: 'POST',
    headers: {
      'Authorization': auth.authorizationToken,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ bucketId })
  });

  if (!res.ok) {
    const errText = await res.text();
    throw new Error(`B2 get_upload_url failed (${res.status}): ${errText}`);
  }

  return await res.json<B2UploadUrlResponse>();
}

export async function uploadToB2(
  keyId: string,
  appKey: string,
  bucketId: string,
  bucketName: string,
  fileName: string,
  data: ArrayBuffer,
  mimeType: string = 'application/octet-stream'
): Promise<{ fileUrl: string; fileId: string; fileName: string; size: number }> {
  const auth = await getB2Auth(keyId, appKey);
  const uploadInfo = await getB2UploadUrl(keyId, appKey, bucketId);

  // Encode file name for B2 header
  const encodedFileName = encodeURIComponent(fileName).replace(/%2F/g, '/');

  const res = await fetch(uploadInfo.uploadUrl, {
    method: 'POST',
    headers: {
      'Authorization': uploadInfo.authorizationToken,
      'X-Bz-File-Name': encodedFileName,
      'Content-Type': mimeType,
      'Content-Length': data.byteLength.toString(),
      'X-Bz-Content-Sha1': 'do_not_verify'
    },
    body: data
  });

  if (!res.ok) {
    const errText = await res.text();
    throw new Error(`B2 file upload failed (${res.status}): ${errText}`);
  }

  const uploadResult = await res.json<any>();
  const fileUrl = `${auth.downloadUrl}/file/${bucketName}/${encodedFileName}`;

  return {
    fileUrl,
    fileId: uploadResult.fileId,
    fileName: uploadResult.fileName,
    size: uploadResult.contentLength
  };
}

export async function fetchB2FileStream(
  keyId: string,
  appKey: string,
  bucketName: string,
  fileUrlOrPath: string
): Promise<{ body: ReadableStream | null; contentType: string; contentLength?: string; status: number }> {
  const auth = await getB2Auth(keyId, appKey);

  // Extract relative path if full B2 URL was passed
  let path = fileUrlOrPath;
  if (path.startsWith('http://') || path.startsWith('https://')) {
    const urlObj = new URL(path);
    const prefix = `/file/${bucketName}/`;
    if (urlObj.pathname.startsWith(prefix)) {
      path = decodeURIComponent(urlObj.pathname.substring(prefix.length));
    } else {
      path = decodeURIComponent(urlObj.pathname.replace(/^\/file\/[^\/]+\//, ''));
    }
  }

  const encodedPath = encodeURIComponent(path).replace(/%2F/g, '/');
  const targetUrl = `${auth.downloadUrl}/file/${bucketName}/${encodedPath}`;

  const res = await fetch(targetUrl, {
    method: 'GET',
    headers: {
      'Authorization': auth.authorizationToken
    }
  });

  return {
    body: res.body,
    contentType: res.headers.get('Content-Type') || 'application/octet-stream',
    contentLength: res.headers.get('Content-Length') || undefined,
    status: res.status
  };
}
