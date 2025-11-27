import React, { FormEvent, useEffect, useState } from "react"
import {
  ProfileResponse,
  AdminUserResponse,
  PairingResponse,
  SettingsResponse,
} from "./types"

const authFetch = async <T,>(url: string, options: RequestInit = {}): Promise<T> => {
  const res = await fetch(url, {
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {}),
    },
    ...options,
  })
  if (res.status === 401) {
    throw new Error("unauthorized")
  }
  if (!res.ok) {
    let message = "Request failed"
    try {
      const data = await res.json()
      message = (data?.message as string) || message
    } catch (_) {
      try {
        message = await res.text()
      } catch (_) {
        /* ignore */
      }
    }
    throw new Error(message)
  }
  if (res.status === 204) return null as T
  return res.json() as Promise<T>
}

const Badge: React.FC<{ children: React.ReactNode }> = ({ children }) => (
  <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold bg-red-500/20 text-red-100 border border-red-400/40 shadow shadow-red-900/30">
    {children}
  </span>
)

const Card: React.FC<{ title: string; actions?: React.ReactNode; children: React.ReactNode }> = ({
  title,
  actions,
  children,
}) => (
  <div className="relative overflow-hidden rounded-2xl border border-emerald-700/30 bg-emerald-900/40 backdrop-blur-sm shadow-xl shadow-emerald-950/40">
    <div className="absolute inset-0 bg-gradient-to-br from-emerald-800/30 via-transparent to-red-800/20 pointer-events-none" />
    <div className="relative p-6">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-semibold text-emerald-50 tracking-tight">{title}</h2>
        {actions}
      </div>
      {children}
    </div>
  </div>
)

const useProfile = () => {
  const [profile, setProfile] = useState<ProfileResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")

  const load = async () => {
    setLoading(true)
    setError("")
    try {
      const data = await authFetch<ProfileResponse>("/api/me")
      setProfile(data)
    } catch (err: any) {
      if (err.message !== "unauthorized") setError(err?.message || "Failed to load profile")
      setProfile(null)
    } finally {
      setLoading(false)
    }
  }

  return { profile, setProfile, loading, error, setError, load }
}

function App() {
  const { profile, setProfile, loading, error, setError, load } = useProfile()
  const [saving, setSaving] = useState(false)
  const [adminData, setAdminData] = useState<{
    users: AdminUserResponse[]
    pairings: PairingResponse[]
    settings: SettingsResponse | null
  }>({ users: [], pairings: [], settings: null })

  const loadAdminData = async () => {
    try {
      const [users, settings, pairings] = await Promise.all([
        authFetch<AdminUserResponse[]>("/api/admin/users"),
        authFetch<SettingsResponse>("/api/admin/settings"),
        authFetch<PairingResponse[]>("/api/admin/pairings"),
      ])
      setAdminData({ users, settings, pairings })
    } catch (err: any) {
      setError(err?.message || "Failed to load admin data")
    }
  }

  useEffect(() => {
    load()
  }, [])

  useEffect(() => {
    if (profile?.role === "ADMIN") {
      loadAdminData()
    }
  }, [profile?.role])

  const formatCurrency = (value?: number | null) => {
    if (value === null || value === undefined) return "Not set"
    return new Intl.NumberFormat(undefined, {
      style: "currency",
      currency: "EUR",
      maximumFractionDigits: 2,
    }).format(value)
  }

  const formatDate = (value?: string | null) => {
    if (!value) return "Not set"
    const parsed = new Date(value)
    return isNaN(parsed.getTime()) ? "Not set" : parsed.toLocaleDateString()
  }

  const handleLogin = () => {
    window.location.href = "/oauth2/authorization/discord"
  }

  const handleLogout = async () => {
    await fetch("/api/auth/logout", { method: "POST", credentials: "include" })
    setProfile(null)
  }

  const handleProfileSave = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSaving(true)
    setError("")
    const form = new FormData(event.currentTarget)
    const interests = (form.get("interests") as string)
      .split(",")
      .map((s) => s.trim())
      .filter(Boolean)
    try {
      const updated = await authFetch<ProfileResponse>("/api/me", {
        method: "PUT",
        body: JSON.stringify({
          displayName: form.get("displayName"),
          address: form.get("address"),
          interests,
        }),
      })
      setProfile(updated)
    } catch (err: any) {
      setError(err?.message || "Failed to save profile")
    } finally {
      setSaving(false)
    }
  }

  const handleSettingsSave = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSaving(true)
    const form = new FormData(event.currentTarget)
    try {
      const payload = {
        spendLimit: form.get("spendLimit") ? Number(form.get("spendLimit")) : null,
        sendDeadline: (form.get("sendDeadline") as string) || null,
        restrictedGuildId: (form.get("restrictedGuildId") as string) || null,
      }
      const settings = await authFetch<SettingsResponse>("/api/admin/settings", {
        method: "PUT",
        body: JSON.stringify(payload),
      })
      setAdminData((prev) => ({ ...prev, settings }))
    } catch (err: any) {
      setError(err?.message || "Failed to save settings")
    } finally {
      setSaving(false)
    }
  }

  const handlePairingsCreate = async () => {
    if (adminData.users.length < 2) {
      setError("Need at least 2 participants to pair")
      return
    }
    if (adminData.pairings.length > 0) {
      const proceed = window.confirm("Pairings already exist. Are you sure you want to pair again?")
      if (!proceed) return
    }
    setSaving(true)
    setError("")
    try {
      const pairings = await authFetch<PairingResponse[]>("/api/admin/pairings", { method: "POST" })
      setAdminData((prev) => ({ ...prev, pairings }))
      await load()
    } catch (err: any) {
      setError(err?.message || "Failed to create pairings")
    } finally {
      setSaving(false)
    }
  }

  const renderMatch = () => {
    if (!profile?.match) return <p className="text-sm text-slate-400">No match assigned yet.</p>
    const m = profile.match
    return (
      <div className="space-y-2" data-testid="match-card">
        <p className="font-semibold text-emerald-200">You are gifting:</p>
        <p className="text-slate-50">{m?.name}</p>
        <p className="text-sm text-slate-300 whitespace-pre-wrap">{m?.address || "No address yet"}</p>
        <p className="text-sm text-slate-300">
          Interests: {m?.interests?.length ? m.interests.join(", ") : "No interests yet"}
        </p>
      </div>
    )
  }

  const renderProfile = () => (
    <div className="grid md:grid-cols-2 gap-6">
      <Card title="Your Profile" actions={<Badge>{profile?.role}</Badge>}>
        <form className="space-y-4" onSubmit={handleProfileSave} aria-label="profile-form">
          <div>
            <label className="block text-sm text-slate-400">Discord</label>
            <p className="text-slate-50" data-testid="discord-username">{profile?.discordUsername}</p>
          </div>
          <div>
            <label className="block text-sm text-slate-400">Display Name</label>
            <input
              name="displayName"
              defaultValue={profile?.displayName || ""}
              className="mt-1 w-full rounded-lg bg-slate-800 border border-slate-700 px-3 py-2 text-slate-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
              required
            />
          </div>
          <div>
            <label className="block text-sm text-slate-400">Address</label>
            <textarea
              name="address"
              defaultValue={profile?.address || ""}
              rows={3}
              className="mt-1 w-full rounded-lg bg-slate-800 border border-slate-700 px-3 py-2 text-slate-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
          </div>
          <div>
            <label className="block text-sm text-slate-400">Interests (comma separated)</label>
            <input
              name="interests"
              defaultValue={(profile?.interests || []).join(", ")}
              className="mt-1 w-full rounded-lg bg-slate-800 border border-slate-700 px-3 py-2 text-slate-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
          </div>
          <button
            type="submit"
            disabled={saving}
            className="inline-flex items-center gap-2 bg-emerald-500 hover:bg-emerald-400 text-slate-900 font-semibold px-4 py-2 rounded-lg transition"
          >
            {saving ? "Saving..." : "Save"}
          </button>
        </form>
      </Card>
      <Card title="Your Match">{renderMatch()}</Card>
    </div>
  )

  const renderEventSettings = () => (
    <Card title="Secret Santa Event">
      <div className="space-y-3 text-base text-emerald-50">
        <div>
          <span className="font-semibold text-emerald-100">Spending Limit:</span>
          <span className="ml-2 text-amber-200">{formatCurrency(profile?.settings?.spendLimit)}</span>
        </div>
        <div>
          <span className="font-semibold text-emerald-100">Send Deadline:</span>
          <span className="ml-2 text-amber-200">{formatDate(profile?.settings?.sendDeadline)}</span>
        </div>
      </div>
    </Card>
  )

  const renderAdmin = () => (
    <div className="space-y-6" data-testid="admin-panel">
      <Card title="Admin Settings">
        <form className="grid md:grid-cols-3 gap-4" onSubmit={handleSettingsSave} aria-label="settings-form">
          <div>
            <label className="block text-sm text-slate-400">Spending Limit</label>
            <input
              name="spendLimit"
              type="number"
              step="0.01"
              defaultValue={adminData.settings?.spendLimit ?? ""}
              className="mt-1 w-full rounded-lg bg-slate-800 border border-slate-700 px-3 py-2 text-slate-100"
            />
          </div>
          <div>
            <label className="block text-sm text-slate-400">Send Deadline</label>
            <input
              name="sendDeadline"
              type="date"
              defaultValue={adminData.settings?.sendDeadline ?? ""}
              className="mt-1 w-full rounded-lg bg-slate-800 border border-slate-700 px-3 py-2 text-slate-100"
            />
          </div>
          <div>
            <label className="block text-sm text-slate-400">Restricted Guild ID</label>
            <input
              name="restrictedGuildId"
              defaultValue={adminData.settings?.restrictedGuildId ?? ""}
              className="mt-1 w-full rounded-lg bg-slate-800 border border-slate-700 px-3 py-2 text-slate-100"
            />
          </div>
          <div className="md:col-span-3">
            <button
              type="submit"
              disabled={saving}
              className="inline-flex items-center gap-2 bg-indigo-500 hover:bg-indigo-400 text-white font-semibold px-4 py-2 rounded-lg transition"
            >
              {saving ? "Saving..." : "Save Settings"}
            </button>
          </div>
        </form>
      </Card>

      <Card
        title="Pairings"
        actions={
          <button
            onClick={handlePairingsCreate}
            disabled={saving}
            className="inline-flex items-center gap-2 bg-emerald-500 hover:bg-emerald-400 text-slate-900 font-semibold px-3 py-2 rounded-lg"
          >
            {saving ? "Working..." : "Generate Pairings"}
          </button>
        }
      >
        {adminData.pairings.length === 0 ? (
          <p className="text-sm text-slate-400">No pairings created yet.</p>
        ) : (
          <ul className="divide-y divide-slate-800 text-sm">
            {adminData.pairings.map((p) => (
              <li key={p.id} className="py-2 flex items-center justify-between">
                <span className="text-slate-200">{p.giverId}</span>
                <span className="text-slate-500">→</span>
                <span className="text-slate-200">{p.receiverId}</span>
              </li>
            ))}
          </ul>
        )}
      </Card>

      <Card title="Participants">
        <div className="overflow-auto">
          <table className="min-w-full text-sm">
            <thead className="text-slate-400">
              <tr>
                <th className="text-left py-2">Name</th>
                <th className="text-left py-2">Discord</th>
                <th className="text-left py-2">Role</th>
                <th className="text-left py-2">Guild</th>
                <th className="text-left py-2">Interests</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800">
              {adminData.users.map((u) => (
                <tr key={u.id} className="text-slate-200">
                  <td className="py-2">{u.displayName || "-"}</td>
                  <td className="py-2">{u.discordUsername}</td>
                  <td className="py-2">
                    <Badge>{u.role}</Badge>
                  </td>
                  <td className="py-2">{u.guildMember ? "Yes" : "No"}</td>
                  <td className="py-2 text-slate-300">{u.interests.join(", ")}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  )

  return (
    <div className="max-w-6xl mx-auto px-4 py-10">
      <header className="mb-10 relative overflow-hidden rounded-3xl border border-emerald-700/40 bg-gradient-to-r from-emerald-800/70 via-red-800/60 to-emerald-800/70 shadow-2xl shadow-emerald-950/40">
        <div className="absolute inset-0 opacity-40" style={{ backgroundImage: "repeating-linear-gradient(90deg, transparent, transparent 10px, rgba(255,255,255,0.08) 11px, rgba(255,255,255,0.08) 12px)" }} />
        <div className="relative flex flex-col md:flex-row md:items-center md:justify-between gap-4 px-6 py-6">
          <div>
            <p className="text-xs md:text-sm uppercase tracking-[0.35em] text-emerald-200">Secret Santa</p>
          <h1 className="text-3xl md:text-4xl font-bold text-white drop-shadow">Secret Santa Event</h1>
          </div>
          {profile ? (
            <div className="flex items-center gap-3">
              <span className="text-sm text-emerald-50 bg-emerald-900/60 px-3 py-1 rounded-full border border-emerald-700/50">{profile.discordUsername}</span>
              <button
                onClick={handleLogout}
                className="inline-flex items-center gap-2 bg-red-500 hover:bg-red-400 text-white px-4 py-2 rounded-lg shadow shadow-red-900/30"
              >
                Logout
              </button>
            </div>
          ) : (
            <button
              onClick={handleLogin}
              className="inline-flex items-center gap-2 bg-amber-400 hover:bg-amber-300 text-emerald-950 font-semibold px-4 py-2 rounded-lg shadow shadow-amber-900/30"
            >
              Login with Discord
            </button>
          )}
        </div>
      </header>

      {error && <div className="mb-4 text-sm text-amber-200 bg-red-900/50 border border-red-700/60 rounded-lg px-4 py-2">{error}</div>}

          {loading ? (
        <div className="text-emerald-100">Loading...</div>
      ) : profile ? (
        <div className="space-y-8">
          {renderEventSettings()}
          {renderProfile()}
          {profile.role === "ADMIN" && renderAdmin()}
        </div>
      ) : (
        <div className="rounded-2xl border border-emerald-700/30 bg-emerald-900/40 p-8 text-center shadow-lg shadow-emerald-950/40">
          <p className="text-lg text-emerald-50">Sign in with Discord to join Secret Santa.</p>
        </div>
      )}
    </div>
  )
}

export default App
