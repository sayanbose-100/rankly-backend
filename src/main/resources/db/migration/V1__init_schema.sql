-- 1. Users (Auth, preferences, and API quota tracking)
CREATE TABLE users (
   id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   email               VARCHAR(255) NOT NULL UNIQUE,
   password_hash       VARCHAR(255) NOT NULL,
   full_name           VARCHAR(120),
   theme_preference    VARCHAR(16) NOT NULL DEFAULT 'SYSTEM', -- LIGHT | DARK | SYSTEM
   email_alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE,
   daily_api_quota     INT NOT NULL DEFAULT 50,
   created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
   updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. Tracked Domain (Multi-domain per user)
CREATE TABLE domains (
     id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
     user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
     domain_name         VARCHAR(255) NOT NULL, -- normalized e.g. 'example.com' (no https:// or trailing slash)
     display_name        VARCHAR(120),
     target_country      VARCHAR(10) NOT NULL DEFAULT 'us',   -- SearchApi 'gl' param (e.g. 'us', 'in', 'gb')
     target_device       VARCHAR(16) NOT NULL DEFAULT 'desktop', -- 'desktop' | 'mobile'
     is_active           BOOLEAN NOT NULL DEFAULT TRUE,
     created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
     CONSTRAINT uq_user_domain_locale UNIQUE (user_id, domain_name, target_country, target_device)
);

CREATE INDEX idx_domains_user_id ON domains(user_id);

-- 3. Tracked Keywords (Normalized per domain to prevent duplicate crashes)
CREATE TABLE keywords (
      id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
      domain_id           BIGINT NOT NULL REFERENCES domains(id) ON DELETE CASCADE,
      phrase              VARCHAR(255) NOT NULL, -- normalized: lower(trim(phrase))
      current_rank        INT,                   -- NULL if unranked (>100) or never checked
      previous_rank       INT,
      best_rank           INT,
      ranking_url         TEXT,                  -- exact URL from domain that ranked
      check_status        VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING | CHECKING | COMPLETED | FAILED
      last_error          TEXT,
      last_checked_at     TIMESTAMPTZ,
      created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      CONSTRAINT uq_domain_keyword UNIQUE (domain_id, phrase)
);

CREATE INDEX idx_keywords_domain_id ON keywords(domain_id);
CREATE INDEX idx_keywords_status ON keywords(check_status);

-- 4. Time-Series Rank Snapshots (Powers your React charts)
CREATE TABLE rank_history (
  id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  keyword_id          BIGINT NOT NULL REFERENCES keywords(id) ON DELETE CASCADE,
  rank_position       INT,                   -- NULL means > 100 (not in top 100)
  ranking_url         TEXT,
  serp_features       JSONB,                 -- e.g. ["featured_snippet", "people_also_ask"]
  source              VARCHAR(20) NOT NULL DEFAULT 'MANUAL', -- MANUAL | SCHEDULED
  checked_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rank_history_keyword_checked ON rank_history(keyword_id, checked_at DESC);

-- 5. Rank Change Alerts & Email Delivery State
CREATE TABLE alert (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    domain_id           BIGINT NOT NULL REFERENCES domains(id) ON DELETE CASCADE,
    keyword_id          BIGINT NOT NULL REFERENCES keywords(id) ON DELETE CASCADE,
    alert_type          VARCHAR(32) NOT NULL,  -- ENTERED_TOP_10 | ENTERED_TOP_100 | DROPPED_OUT | RANK_UP | RANK_DOWN
    old_rank            INT,
    new_rank            INT,
    message             TEXT NOT NULL,
    is_read             BOOLEAN NOT NULL DEFAULT FALSE,
    email_sent          BOOLEAN NOT NULL DEFAULT FALSE,
    email_sent_at       TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_alerts_user_unread ON alert(user_id, is_read, created_at DESC);
CREATE INDEX idx_alerts_email_pending ON alert(email_sent) WHERE email_sent = FALSE;

-- 6. Async Search Batches (Tracks multi-keyword submission progress for React loader)
CREATE TABLE search_batches (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    domain_id           BIGINT NOT NULL REFERENCES domains(id) ON DELETE CASCADE,
    total_keywords      INT NOT NULL,
    completed_keywords  INT NOT NULL DEFAULT 0,
    failed_keywords     INT NOT NULL DEFAULT 0,
    status              VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS', -- IN_PROGRESS | COMPLETED | PARTIAL_FAILED
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMPTZ
);

CREATE INDEX idx_search_batches_user_domain ON search_batches(user_id, domain_id, created_at DESC);