import React, { useEffect, useState } from "https://esm.sh/react@18.3.1";
import { createRoot } from "https://esm.sh/react-dom@18.3.1/client";

const authFetch = async (url, options = {}) => {
  const res = await fetch(url, {
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {}),
    },
    ...options,
  });
  if (res.status === 401) {
    throw new Error("unauthorized");
  }
  if (res.status === 204) return null;
  return res.json();
};

const Badge = ({ children }) => (
  <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-semibold bg-emerald-500/20 text-emerald-200 border border-emerald-500/30">
    {children}
  </span>
);

const Card = ({ title, children, actions }) => (
  <div className="bg-slate-900/70 border border-slate-800 rounded-xl p-6 shadow-xl shadow-black/30">
    <div className="flex items-center justify-between mb-4">
      <h2 className="text-lg font-semibold text-slate-50">{title}</h2>
      {actions}
    </div>
    {children}
  </div>
);

function App() {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const [adminData, setAdminData] = useState({ users: [], pairings: [], settings: null });

  const loadProfile = async () => {
    setLoading(true);
    setError("");
    try {
      const data = await authFetch("/api/me");
      setProfile(data);
      if (data.role === "ADMIN") {
        await loadAdminData();
      }
    } catch (err) {
      if (err.message !== "unauthorized") {
        setError("Failed to load profile");
      }
      setProfile(null);
    } finally {
      setLoading(false);
    }
  };

  const loadAdminData = async () => {
    try {
      const [users, settings, pairings] = await Promise.all([
        authFetch("/api/admin/users"),
        authFetch("/api/admin/settings"),
        authFetch("/api/admin/pairings"),
      ]);
      setAdminData({ users, settings, pairings });
    } catch (err) {
      setError("Failed to load admin data");
    }
  };

  useEffect(() => {
    loadProfile();
  }, []);

  const handleLogin = () => {
    window.location.href = "/oauth2/authorization/discord";
  };

  const handleLogout = async () => {
    await fetch("/api/auth/logout", { method: "POST", credentials: "include" });
    setProfile(null);
  };

  const handleProfileSave = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    const form = new FormData(event.target);
    const interests = form
      .get("interests")
      .split(",")
      .map((s) => s.trim())
      .filter(Boolean);
    try {
      const updated = await authFetch("/api/me", {
        method: "PUT",
        body: JSON.stringify({
          displayName: form.get("displayName"),
          address: form.get("address"),
          interests,
        }),
      });
      setProfile(updated);
    } catch (err) {
      setError("Failed to save profile");
    } finally {
      setSaving(false);
    }
  };

  const handleSettingsSave = async (event) => {
    event.preventDefault();
    setSaving(true);
    const form = new FormData(event.target);
    try {
      const payload = {
        spendLimit: form.get("spendLimit") ? Number(form.get("spendLimit")) : null,
        sendDeadline: form.get("sendDeadline") || null,
        restrictedGuildId: form.get("restrictedGuildId") || null,
      };
      const settings = await authFetch("/api/admin/settings", {
        method: "PUT",
        body: JSON.stringify(payload),
      });
      setAdminData((prev) => ({ ...prev, settings }));
    } catch (err) {
      setError("Failed to save settings");
    } finally {
      setSaving(false);
    }
  };

  const handlePairingsCreate = async () => {
    setSaving(true);
    setError("");
    try {
      const pairings = await authFetch("/api/admin/pairings", { method: "POST" });
      setAdminData((prev) => ({ ...prev, pairings }));
      await loadProfile();
    } catch (err) {
      setError("Failed to create pairings");
    } finally {
      setSaving(false);
    }
  };

  const renderMatch = () => {
    if (!profile?.match) return <p className="text-sm text-slate-400">No match assigned yet.</p>;
    const m = profile.match;
    return (
      <div className="space-y-2">
        <p className="font-semibold text-emerald-200">You are gifting:</p>
        <p className="text-slate-50">{m.name}</p>
        <p className="text-sm text-slate-300 whitespace-pre-wrap">{m.address || "No address yet"}</p>
        <p className="text-sm text-slate-300">
          Interests: {m.interests && m.interests.length ? m.interests.join(", ") : "No interests yet"}
        </p>
      </div>
    );
  };

  const renderProfile = () => (
    <div className="grid md:grid-cols-2 gap-6">
      <Card title="Your Profile" actions={<Badge>{profile.role}</Badge>}>
        <form className="space-y-4" onSubmit={handleProfileSave}>
          <div>
            <label className="block text-sm text-slate-400">Discord</label>
            <p className="text-slate-50">{profile.discordUsername}</p>
          </div>
          <div>
            <label className="block text-sm text-slate-400">Display Name</label>
            <input
              name="displayName"
              defaultValue={profile.displayName || ""}
              className="mt-1 w-full rounded-lg bg-slate-800 border border-slate-700 px-3 py-2 text-slate-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
              required
            />
          </div>
          <div>
            <label className="block text-sm text-slate-400">Address</label>
            <textarea
              name="address"
              defaultValue={profile.address || ""}
              rows="3"
              className="mt-1 w-full rounded-lg bg-slate-800 border border-slate-700 px-3 py-2 text-slate-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
          </div>
          <div>
            <label className="block text-sm text-slate-400">Interests (comma separated)</label>
            <input
              name="interests"
              defaultValue={(profile.interests || []).join(", ")}
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
      <Card title="Event Settings">
        <div className="space-y-2 text-sm text-slate-300">
          <div>
            <span className="font-semibold text-slate-100">Spend Limit:</span> {profile.settings.spendLimit ?? "Not set"}
          </div>
          <div>
            <span className="font-semibold text-slate-100">Send Deadline:</span> {profile.settings.sendDeadline ?? "Not set"}
          </div>
        </div>
      </Card>
    </div>
  );

  const renderAdmin = () => (
    <div className="space-y-6">
      <Card title="Admin Settings">
        <form className="grid md:grid-cols-3 gap-4" onSubmit={handleSettingsSave}>
          <div>
            <label className="block text-sm text-slate-400">Spend Limit</label>
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
  );

  return (
    <div className="max-w-6xl mx-auto px-4 py-10">
      <header className="flex items-center justify-between mb-10">
        <div>
          <p className="text-sm uppercase tracking-[0.3em] text-emerald-400">Secret Santa</p>
          <h1 className="text-3xl font-bold text-white">Gift Matcher</h1>
        </div>
        {profile ? (
          <div className="flex items-center gap-3">
            <span className="text-sm text-slate-300">{profile.discordUsername}</span>
            <button
              onClick={handleLogout}
              className="inline-flex items-center gap-2 bg-slate-800 hover:bg-slate-700 text-slate-100 px-3 py-2 rounded-lg"
            >
              Logout
            </button>
          </div>
        ) : (
          <button
            onClick={handleLogin}
            className="inline-flex items-center gap-2 bg-emerald-500 hover:bg-emerald-400 text-slate-900 font-semibold px-4 py-2 rounded-lg"
          >
            Login with Discord
          </button>
        )}
      </header>

      {error && <div className="mb-4 text-sm text-rose-300">{error}</div>}

      {loading ? (
        <div className="text-slate-400">Loading...</div>
      ) : profile ? (
        <div className="space-y-8">
          {renderProfile()}
          {profile.role === "ADMIN" && renderAdmin()}
        </div>
      ) : (
        <div className="bg-slate-900/70 border border-slate-800 rounded-xl p-8 text-center">
          <p className="text-lg text-slate-200">Sign in with Discord to join Secret Santa.</p>
        </div>
      )}
    </div>
  );
}

const root = createRoot(document.getElementById("root"));
root.render(<App />);
