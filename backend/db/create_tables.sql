
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
-- =====================================================
-- HUBBLE DATABASE SCHEMA - PostgreSQL
-- =====================================================

-- =====================================================
-- DROP EXISTING OBJECTS (reverse dependency order)
-- =====================================================

-- Drop tables first (CASCADE will also drop their triggers)
DROP TABLE IF EXISTS reactions CASCADE;
DROP TABLE IF EXISTS attachments CASCADE;
DROP TABLE IF EXISTS messages CASCADE;
DROP TABLE IF EXISTS channel_members CASCADE;
DROP TABLE IF EXISTS channels CASCADE;
DROP TABLE IF EXISTS member_roles CASCADE;
DROP TABLE IF EXISTS permissions CASCADE;
DROP TABLE IF EXISTS roles CASCADE;
DROP TABLE IF EXISTS server_members CASCADE;
DROP TABLE IF EXISTS servers CASCADE;
DROP TABLE IF EXISTS friendships CASCADE;
DROP TABLE IF EXISTS user_settings CASCADE;
DROP TABLE IF EXISTS user_sessions CASCADE;
DROP TABLE IF EXISTS user_otps CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS server_invites CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS device_tokens CASCADE;

-- Drop function (after tables are dropped)
DROP FUNCTION IF EXISTS update_updated_at() CASCADE;

-- Drop enum types
DROP TYPE IF EXISTS friendship_status;
DROP TYPE IF EXISTS message_type;
DROP TYPE IF EXISTS channel_type;
DROP TYPE IF EXISTS device_type;
DROP TYPE IF EXISTS otp_type;
DROP TYPE IF EXISTS auth_provider;
DROP TYPE IF EXISTS user_status;
DROP TYPE IF EXISTS server_invite_status;
DROP TYPE IF EXISTS notification_type;

-- =====================================================
-- Enable UUID extension
-- =====================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =====================================================
-- ENUM TYPES
-- =====================================================

CREATE TYPE user_status AS ENUM ('ONLINE', 'IDLE', 'DND', 'OFFLINE');
CREATE TYPE auth_provider AS ENUM ('LOCAL', 'GOOGLE', 'PHONE');
CREATE TYPE otp_type AS ENUM ('EMAIL_VERIFY', 'PASSWORD_RESET', 'LOGIN', 'PHONE_VERIFY');
CREATE TYPE device_type AS ENUM ('MOBILE', 'DESKTOP', 'WEB');
CREATE TYPE channel_type AS ENUM ('TEXT', 'VOICE', 'CATEGORY', 'DM', 'GROUP_DM');
CREATE TYPE message_type AS ENUM ('TEXT', 'IMAGE', 'FILE', 'SYSTEM', 'VOICE_NOTE', 'STICKER', 'GIPHY');
CREATE TYPE friendship_status AS ENUM ('PENDING', 'ACCEPTED', 'BLOCKED');
CREATE TYPE server_invite_status AS ENUM ('PENDING', 'ACCEPTED', 'DECLINED', 'EXPIRED');
CREATE TYPE notification_type AS ENUM ('NEW_MESSAGE', 'FRIEND_REQUEST', 'SERVER_INVITE', 'SYSTEM_ALERT');


-- =====================================================
-- TABLES
-- =====================================================

-- Users table
CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       password_hash TEXT,                                    -- NULL nếu đăng ký bằng Google OAuth2
                       auth_provider auth_provider NOT NULL DEFAULT 'LOCAL',  -- LOCAL | GOOGLE | PHONE
                       username VARCHAR(32) NOT NULL UNIQUE,
                       display_name VARCHAR(64),
                       email VARCHAR(255) UNIQUE,
                       phone TEXT UNIQUE DEFAULT NULL,
                       email_verified BOOLEAN DEFAULT FALSE,
                       phone_verified BOOLEAN DEFAULT FALSE,
                       avatar_url TEXT,
                       bio TEXT,
                       status user_status DEFAULT 'OFFLINE',
                       custom_status VARCHAR(128),
                       last_seen_at TIMESTAMPTZ,
                       created_at TIMESTAMPTZ DEFAULT NOW(),
                       updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- User OTPs table
CREATE TABLE user_otps (
                           id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                           user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                           otp_code VARCHAR(6) NOT NULL,
                           type otp_type NOT NULL,
                           is_used BOOLEAN DEFAULT FALSE,
                           expired_at TIMESTAMPTZ NOT NULL,
                           created_at TIMESTAMPTZ DEFAULT NOW()
);

-- User Sessions table
CREATE TABLE user_sessions (
                               id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                               user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               device_name VARCHAR(128),
                               device_type device_type,
                               ip_address TEXT,
                               refresh_token TEXT,
                               is_active BOOLEAN DEFAULT TRUE,
                               last_active_at TIMESTAMPTZ DEFAULT NOW(),
                               created_at TIMESTAMPTZ DEFAULT NOW()
);

-- User Settings table
CREATE TABLE user_settings (
                               user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                               theme VARCHAR(16) DEFAULT 'DARK',
                               locale VARCHAR(8) DEFAULT 'vi',
                               app_lock_pin VARCHAR(255),
                               notification_enabled BOOLEAN DEFAULT TRUE,
                               notification_sound BOOLEAN DEFAULT TRUE,
                               updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Servers table
CREATE TABLE servers (
                         id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                         owner_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                         name VARCHAR(100) NOT NULL,
                         description TEXT,
                         icon_url TEXT,
                         invite_code VARCHAR(16) UNIQUE,
                         is_public BOOLEAN DEFAULT FALSE,
                         created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Server Members table
CREATE TABLE server_members (
                                id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                server_id UUID NOT NULL REFERENCES servers(id) ON DELETE CASCADE,
                                user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                nickname VARCHAR(64),
                                joined_at TIMESTAMPTZ DEFAULT NOW(),
                                UNIQUE (server_id, user_id)
);

-- Roles table
CREATE TABLE roles (
                       id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       server_id UUID NOT NULL REFERENCES servers(id) ON DELETE CASCADE,
                       name VARCHAR(64) NOT NULL,
                       color INTEGER DEFAULT 0,
                       permissions BIGINT DEFAULT 0,
                       position SMALLINT DEFAULT 0,
                       is_default BOOLEAN NOT NULL DEFAULT FALSE,
                       display_separately BOOLEAN NOT NULL DEFAULT FALSE,
                       mentionable BOOLEAN NOT NULL DEFAULT FALSE,
                       created_at TIMESTAMPTZ DEFAULT NOW(),
                       updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Permissions table (lookup table)
CREATE TABLE permissions (
                             id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                             name VARCHAR(64) NOT NULL UNIQUE,
                             description TEXT
);

-- Member Roles table (many-to-many)
CREATE TABLE member_roles (
                              member_id UUID NOT NULL REFERENCES server_members(id) ON DELETE CASCADE,
                              role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
                              PRIMARY KEY (member_id, role_id)
);

-- Channels table
CREATE TABLE channels (
                          id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          server_id UUID REFERENCES servers(id) ON DELETE CASCADE,
                          parent_id UUID REFERENCES channels(id) ON DELETE SET NULL,
                          name VARCHAR(100),
                          type channel_type NOT NULL,
                          topic TEXT,
                          position SMALLINT DEFAULT 0,
                          is_private BOOLEAN DEFAULT FALSE,
                          created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Channel Members table
CREATE TABLE channel_members (
                                 channel_id UUID NOT NULL REFERENCES channels(id) ON DELETE CASCADE,
                                 user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                 last_read_at TIMESTAMPTZ,
                                 is_muted BOOLEAN DEFAULT FALSE,
                                 is_pinned BOOLEAN DEFAULT FALSE,
                                 joined_at TIMESTAMPTZ DEFAULT NOW(),
                                 PRIMARY KEY (channel_id, user_id)
);

-- Messages table
CREATE TABLE messages (
                          id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          channel_id UUID NOT NULL REFERENCES channels(id) ON DELETE CASCADE,
                          author_id UUID REFERENCES users(id) ON DELETE SET NULL,  -- nullable: giữ tin nhắn khi user bị xóa
                          reply_to_id UUID REFERENCES messages(id) ON DELETE SET NULL,
                          content TEXT,
                          type message_type DEFAULT 'TEXT',
                          is_pinned BOOLEAN DEFAULT FALSE,
                          is_deleted BOOLEAN DEFAULT FALSE,
                          edited_at TIMESTAMPTZ,
                          created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Attachments table
CREATE TABLE attachments (
                             id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                             message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
                             filename TEXT NOT NULL,
                             url TEXT NOT NULL,
                             content_type VARCHAR(128),
                             size_bytes BIGINT,
                             created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Reactions table
CREATE TABLE reactions (
                           id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                           message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
                           user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                           emoji VARCHAR(32) NOT NULL,
                           created_at TIMESTAMPTZ DEFAULT NOW(),
                           UNIQUE (message_id, user_id, emoji)
);

-- Friendships table
CREATE TABLE friendships (
                             id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                             requester_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                             addressee_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                             status friendship_status DEFAULT 'PENDING',
                             created_at TIMESTAMPTZ DEFAULT NOW(),
                             UNIQUE (requester_id, addressee_id),
                             CHECK (requester_id != addressee_id)
);

-- Server invitations table
CREATE TABLE server_invites (
                            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

                            server_id UUID NOT NULL REFERENCES servers(id) ON DELETE CASCADE,
                            inviter_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                            invitee_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

                            status server_invite_status NOT NULL DEFAULT 'PENDING',

                            created_at TIMESTAMPTZ DEFAULT NOW(),
                            expires_at TIMESTAMPTZ,
                            responded_at TIMESTAMPTZ,

                            CHECK (inviter_id <> invitee_id)
);

-- Notifications table
CREATE TABLE notifications (
                               id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                               user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               type notification_type NOT NULL,
                               reference_id TEXT,
                               content TEXT NOT NULL,
                               is_read BOOLEAN DEFAULT FALSE,
                               created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Device tokens table (FCM push)
CREATE TABLE device_tokens (
                               id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                               user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               token TEXT NOT NULL UNIQUE,
                               device_type VARCHAR(50),
                               created_at TIMESTAMPTZ DEFAULT NOW()
);


-- =====================================================
-- INDEXES
-- =====================================================

-- Users
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);

-- User OTPs
CREATE INDEX idx_user_otps_lookup ON user_otps(user_id, type, is_used, expired_at);

-- User Sessions
CREATE INDEX idx_user_sessions_user ON user_sessions(user_id, is_active);
CREATE INDEX idx_user_sessions_token ON user_sessions(refresh_token) WHERE refresh_token IS NOT NULL;

-- Servers
CREATE INDEX idx_servers_owner ON servers(owner_id);
CREATE INDEX idx_servers_invite ON servers(invite_code) WHERE invite_code IS NOT NULL;

-- Server Members
CREATE INDEX idx_server_members_user ON server_members(user_id);
CREATE INDEX idx_server_members_server ON server_members(server_id);

-- Roles
CREATE INDEX idx_roles_server ON roles(server_id);
CREATE INDEX idx_roles_position ON roles(server_id, position DESC);
CREATE INDEX idx_member_roles_role ON member_roles(role_id);
CREATE INDEX idx_member_roles_member ON member_roles(member_id);

-- Channels
CREATE INDEX idx_channels_server ON channels(server_id) WHERE server_id IS NOT NULL;
CREATE INDEX idx_channels_parent ON channels(parent_id) WHERE parent_id IS NOT NULL;
CREATE INDEX idx_channels_type ON channels(type);

-- Channel Members
CREATE INDEX idx_channel_members_user ON channel_members(user_id);
CREATE INDEX idx_channel_members_unread ON channel_members(channel_id, last_read_at);

-- Messages
CREATE INDEX idx_messages_channel ON messages(channel_id, created_at DESC);
CREATE INDEX idx_messages_author ON messages(author_id);
CREATE INDEX idx_messages_reply ON messages(reply_to_id) WHERE reply_to_id IS NOT NULL;
CREATE INDEX idx_messages_pinned ON messages(channel_id) WHERE is_pinned = TRUE;
CREATE INDEX idx_messages_content_search ON messages USING gin(to_tsvector('simple', content))
    WHERE content IS NOT NULL;  -- tránh lỗi khi content NULL (STICKER/GIPHY messages)

-- Attachments
CREATE INDEX idx_attachments_message ON attachments(message_id);

-- Reactions
CREATE INDEX idx_reactions_message ON reactions(message_id);
CREATE INDEX idx_reactions_user ON reactions(user_id);

-- Friendships
CREATE INDEX idx_friendships_requester ON friendships(requester_id, status);
CREATE INDEX idx_friendships_addressee ON friendships(addressee_id, status);

-- Invitations
CREATE INDEX idx_server_invites_server ON server_invites(server_id);
CREATE INDEX idx_server_invites_invitee ON server_invites(invitee_id);
CREATE INDEX idx_server_invites_status ON server_invites(status);
CREATE INDEX idx_server_invites_inviter ON server_invites(inviter_id);

-- Partial unique index: only one PENDING invite allowed per (server, invitee) at a time
-- ACCEPTED/DECLINED historical records are not affected
CREATE UNIQUE INDEX uq_server_invites_pending
    ON server_invites (server_id, invitee_id)
    WHERE status = 'PENDING';

-- Notifications
CREATE INDEX idx_notifications_user ON notifications(user_id, created_at DESC);
CREATE INDEX idx_notifications_unread ON notifications(user_id) WHERE is_read = FALSE;

-- Device tokens
CREATE INDEX idx_device_tokens_user ON device_tokens(user_id);


-- =====================================================
-- TRIGGERS
-- =====================================================

-- Auto update updated_at
CREATE OR REPLACE FUNCTION update_updated_at()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER tr_user_settings_updated_at
    BEFORE UPDATE ON user_settings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- =====================================================
-- SEED DATA: Permissions
-- =====================================================

INSERT INTO permissions (id, name, description) VALUES
                                                    (uuid_generate_v4(), 'VIEW_CHANNEL', 'Xem nội dung kênh'),
                                                    (uuid_generate_v4(), 'SEND_MESSAGE', 'Gửi tin nhắn trong kênh'),
                                                    (uuid_generate_v4(), 'EMBED_LINKS', 'Nhúng link có preview'),
                                                    (uuid_generate_v4(), 'ATTACH_FILES', 'Đính kèm file'),
                                                    (uuid_generate_v4(), 'ADD_REACTIONS', 'Thêm reaction vào tin nhắn'),
                                                    (uuid_generate_v4(), 'MENTION_EVERYONE', 'Sử dụng @everyone'),
                                                    (uuid_generate_v4(), 'MANAGE_MESSAGES', 'Xóa/ghim tin nhắn của người khác'),
                                                    (uuid_generate_v4(), 'READ_MESSAGE_HISTORY', 'Đọc lịch sử tin nhắn'),
                                                    (uuid_generate_v4(), 'CONNECT', 'Kết nối vào voice channel'),
                                                    (uuid_generate_v4(), 'SPEAK', 'Nói trong voice channel'),
                                                    (uuid_generate_v4(), 'MUTE_MEMBERS', 'Tắt mic thành viên khác'),
                                                    (uuid_generate_v4(), 'DEAFEN_MEMBERS', 'Tắt loa thành viên khác'),
                                                    (uuid_generate_v4(), 'MOVE_MEMBERS', 'Di chuyển thành viên giữa các voice channel'),
                                                    (uuid_generate_v4(), 'CREATE_INVITE', 'Tạo link mời vào server'),
                                                    (uuid_generate_v4(), 'KICK_MEMBERS', 'Kick thành viên khỏi server'),
                                                    (uuid_generate_v4(), 'BAN_MEMBERS', 'Ban thành viên khỏi server'),
                                                    (uuid_generate_v4(), 'MANAGE_CHANNELS', 'Tạo/sửa/xóa kênh'),
                                                    (uuid_generate_v4(), 'MANAGE_ROLES', 'Tạo/sửa/xóa role'),
                                                    (uuid_generate_v4(), 'MANAGE_SERVER', 'Chỉnh sửa cài đặt server'),
                                                    (uuid_generate_v4(), 'ADMINISTRATOR', 'Toàn quyền (bỏ qua mọi permission check)');

-- =====================================================
-- COMMENTS
-- =====================================================

COMMENT ON TABLE users IS 'Bảng người dùng chính';
COMMENT ON TABLE user_otps IS 'Mã OTP xác thực';
COMMENT ON TABLE user_sessions IS 'Quản lý phiên đăng nhập';
COMMENT ON TABLE user_settings IS 'Cài đặt cá nhân';
COMMENT ON TABLE servers IS 'Server/Nhóm';
COMMENT ON TABLE server_members IS 'Thành viên của server';
COMMENT ON TABLE roles IS 'Vai trò trong server';
COMMENT ON TABLE permissions IS 'Danh sách quyền hệ thống';
COMMENT ON TABLE member_roles IS 'Gán role cho thành viên';
COMMENT ON TABLE channels IS 'Kênh chat (text/voice/DM)';
COMMENT ON TABLE channel_members IS 'Thành viên kênh + trạng thái đọc';
COMMENT ON TABLE messages IS 'Tin nhắn';
COMMENT ON TABLE attachments IS 'Tệp đính kèm';
COMMENT ON TABLE reactions IS 'Reaction tin nhắn';
COMMENT ON TABLE friendships IS 'Quan hệ bạn bè';