import { render, screen } from "@testing-library/react"
import App from "./App"
import { vi } from "vitest"

describe("App", () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it("renders login prompt when not authenticated", async () => {
    vi.spyOn(window, "fetch" as any).mockResolvedValue({ status: 401, ok: false })
    render(<App />)
    expect(await screen.findByText(/Sign in with Discord/i)).toBeTruthy()
  })

  it("shows profile when authenticated", async () => {
    vi.spyOn(window, "fetch" as any).mockImplementation((url: RequestInfo | URL) => {
      if (url.toString().includes("/api/me")) {
        return Promise.resolve({
          status: 200,
          ok: true,
          json: () =>
            Promise.resolve({
              id: "1",
              discordUsername: "testuser",
              displayName: "Test User",
              address: "123 Street",
              interests: ["books"],
              role: "USER",
              match: null,
              settings: { spendLimit: 20, sendDeadline: null },
            }),
        })
      }
      return Promise.resolve({ status: 401, ok: false })
    })

    render(<App />)

    const discord = await screen.findByTestId("discord-username")
    expect(discord.textContent).toContain("testuser")
    expect(screen.getByLabelText(/profile-form/i)).toBeTruthy()
  })
})
