export type Role = "USER" | "ADMIN"

export interface ProfileResponse {
  id: string
  discordUsername: string
  displayName?: string | null
  address?: string | null
  interests: string[]
  role: Role
  match?: MatchInfo | null
  settings: SettingsResponse
}

export interface MatchInfo {
  name?: string | null
  address?: string | null
  interests: string[]
}

export interface SettingsResponse {
  spendLimit?: number | null
  sendDeadline?: string | null
  restrictedGuildId?: string | null
}

export interface AdminUserResponse {
  id: string
  discordId: string
  discordUsername: string
  displayName?: string | null
  address?: string | null
  interests: string[]
  role: Role
  guildMember: boolean
}

export interface PairingResponse {
  id: string
  giverId: string
  receiverId: string
  createdAt: string
}
