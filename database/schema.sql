-- =============================================================
--  Tic-Tac-Toe Database Schema  (v2 – full feature update)
--  Run this file on a fresh database OR use migration.sql
--  to upgrade an existing v1 database.
-- =============================================================

CREATE DATABASE IF NOT EXISTS tic_tac_toe_db;
USE tic_tac_toe_db;

-- ---------------------------------------------------------------
-- 1. SETTINGS  (single-row config – id always = 1)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS game_settings (
    id               INT PRIMARY KEY AUTO_INCREMENT,
    gamemode         VARCHAR(20)  DEFAULT 'Singleplayer',
    board_size       INT          DEFAULT 3,
    match_timer      TINYINT(1)   DEFAULT 1,
    board_info       TINYINT(1)   DEFAULT 1,
    player_counter   TINYINT(1)   DEFAULT 1,
    -- NEW v2 columns
    first_player     VARCHAR(1)   DEFAULT 'X',          -- 'X' or 'O'
    theme            VARCHAR(20)  DEFAULT 'Light',       -- Light/Dark/Blue/Green
    music_track      VARCHAR(255) DEFAULT 'background.wav', -- filename (relative to sounds/)
    x_image          VARCHAR(255) DEFAULT NULL,          -- custom X image path
    o_image          VARCHAR(255) DEFAULT NULL           -- custom O image path
);

-- ---------------------------------------------------------------
-- 2. GAME STATS  (cumulative totals – id always = 1)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS game_stats (
    id         INT PRIMARY KEY AUTO_INCREMENT,
    human_wins INT DEFAULT 0,
    bot_wins   INT DEFAULT 0,
    draws      INT DEFAULT 0
);

-- ---------------------------------------------------------------
-- 3. SESSIONS  (one per "play" attempt from WelcomePanel)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sessions (
    id           INT PRIMARY KEY AUTO_INCREMENT,
    started_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at     DATETIME     DEFAULT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'active',  -- active | completed | abandoned
    board_size   INT          NOT NULL DEFAULT 3,
    gamemode     VARCHAR(20)  NOT NULL DEFAULT 'Singleplayer',
    difficulty   VARCHAR(20)  NOT NULL DEFAULT 'Medium',
    winning_logic VARCHAR(30) NOT NULL DEFAULT 'Default',
    first_player VARCHAR(1)   NOT NULL DEFAULT 'X',
    player1_name VARCHAR(50)  DEFAULT 'Player 1',
    player2_name VARCHAR(50)  DEFAULT 'Bot'
);

-- ---------------------------------------------------------------
-- 4. GAMES  (one session may eventually hold rematches)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS games (
    id           INT PRIMARY KEY AUTO_INCREMENT,
    session_id   INT          NOT NULL,
    started_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at     DATETIME     DEFAULT NULL,
    result       VARCHAR(20)  DEFAULT NULL,   -- 'player1' | 'player2' | 'draw' | NULL(in-progress)
    duration_sec INT          DEFAULT 0,
    move_count   INT          DEFAULT 0,
    FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------
-- 5. MOVES  (individual turns within a game)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS moves (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    game_id     INT         NOT NULL,
    move_number INT         NOT NULL,
    player      VARCHAR(1)  NOT NULL,   -- 'X' or 'O'
    row_pos     INT         NOT NULL,
    col_pos     INT         NOT NULL,
    moved_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------
-- 6. CUSTOM MUSIC LIBRARY
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS music_library (
    id         INT PRIMARY KEY AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL,
    filename   VARCHAR(255) NOT NULL UNIQUE,
    added_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------
-- SEED DATA  (idempotent – only inserts if missing)
-- ---------------------------------------------------------------
INSERT INTO game_settings (id, gamemode, board_size, match_timer, board_info, player_counter,
                            first_player, theme, music_track, x_image, o_image)
SELECT 1,'Singleplayer',3,1,1,1,'X','Light','background.wav',NULL,NULL FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM game_settings WHERE id = 1);

INSERT INTO game_stats (id, human_wins, bot_wins, draws)
SELECT 1,0,0,0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM game_stats WHERE id = 1);

-- Seed the built-in background track so the dropdown always has it
INSERT INTO music_library (name, filename)
SELECT 'Default Background','background.wav' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM music_library WHERE filename = 'background.wav');

-- =============================================================
--  MIGRATION  v1 → v2
--  Safe to run on an existing v1 database.
--  All ALTER TABLE statements use IF NOT EXISTS / ignore errors
--  so existing data is never lost.
-- =============================================================
USE tic_tac_toe_db;

-- ---------------------------------------------------------------
-- 1. Extend game_settings with new v2 columns
-- ---------------------------------------------------------------
ALTER TABLE game_settings
    ADD COLUMN IF NOT EXISTS first_player  VARCHAR(1)   DEFAULT 'X'              AFTER player_counter,
    ADD COLUMN IF NOT EXISTS theme         VARCHAR(20)  DEFAULT 'Light'           AFTER first_player,
    ADD COLUMN IF NOT EXISTS music_track   VARCHAR(255) DEFAULT 'background.wav'  AFTER theme,
    ADD COLUMN IF NOT EXISTS x_image       VARCHAR(255) DEFAULT NULL              AFTER music_track,
    ADD COLUMN IF NOT EXISTS o_image       VARCHAR(255) DEFAULT NULL              AFTER x_image;

-- ---------------------------------------------------------------
-- 2. Create sessions table (if not already present)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sessions (
    id            INT PRIMARY KEY AUTO_INCREMENT,
    started_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at      DATETIME     DEFAULT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'active',
    board_size    INT          NOT NULL DEFAULT 3,
    gamemode      VARCHAR(20)  NOT NULL DEFAULT 'Singleplayer',
    difficulty    VARCHAR(20)  NOT NULL DEFAULT 'Medium',
    winning_logic VARCHAR(30)  NOT NULL DEFAULT 'Default',
    first_player  VARCHAR(1)   NOT NULL DEFAULT 'X',
    player1_name  VARCHAR(50)  DEFAULT 'Player 1',
    player2_name  VARCHAR(50)  DEFAULT 'Bot'
);

-- ---------------------------------------------------------------
-- 3. Create games table (if not already present)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS games (
    id           INT PRIMARY KEY AUTO_INCREMENT,
    session_id   INT          NOT NULL,
    started_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at     DATETIME     DEFAULT NULL,
    result       VARCHAR(20)  DEFAULT NULL,
    duration_sec INT          DEFAULT 0,
    move_count   INT          DEFAULT 0,
    FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------
-- 4. Create moves table (if not already present)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS moves (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    game_id     INT         NOT NULL,
    move_number INT         NOT NULL,
    player      VARCHAR(1)  NOT NULL,
    row_pos     INT         NOT NULL,
    col_pos     INT         NOT NULL,
    moved_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------
-- 5. Create music_library table (if not already present)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS music_library (
    id        INT PRIMARY KEY AUTO_INCREMENT,
    name      VARCHAR(100) NOT NULL,
    filename  VARCHAR(255) NOT NULL UNIQUE,
    added_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seed default track
INSERT INTO music_library (name, filename)
SELECT 'Default Background','background.wav' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM music_library WHERE filename = 'background.wav');

-- ---------------------------------------------------------------
-- Done
-- ---------------------------------------------------------------
SELECT 'Migration v1 → v2 complete.' AS status;