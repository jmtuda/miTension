import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, test, vi } from "vitest";
import { App } from "../src/App.tsx";
import type { AuthGateway } from "../src/auth.ts";
import type { Measurement, MeasurementRepository } from "../src/data/measurements.ts";

const row: Measurement = { id: "m1", measured_at: "2026-08-31T10:00:00Z", systolic: 121, diastolic: 81, pulse: 62, notes: "Después del paseo", deleted_at: null };

function authenticated() {
  const auth: AuthGateway = { getSession: vi.fn().mockResolvedValue({ email: "user@example.com" }), signIn: vi.fn(), signOut: vi.fn(), onChange: vi.fn(() => () => {}) };
  const repository: MeasurementRepository = { list: vi.fn().mockResolvedValue([row]), create: vi.fn().mockResolvedValue(undefined), softDelete: vi.fn().mockResolvedValue(undefined) };
  return { auth, repository };
}

async function enterReading(user: ReturnType<typeof userEvent.setup>, systolic: string, diastolic: string, pulse: string) {
  await user.clear(screen.getByLabelText(/^Sistólica/)); await user.type(screen.getByLabelText(/^Sistólica/), systolic);
  await user.clear(screen.getByLabelText(/^Diastólica/)); await user.type(screen.getByLabelText(/^Diastólica/), diastolic);
  await user.clear(screen.getByLabelText(/^Pulso/)); await user.type(screen.getByLabelText(/^Pulso/), pulse);
  await user.click(screen.getByRole("button", { name: "Continuar" }));
}

describe("web workflow", () => {
  beforeEach(() => vi.restoreAllMocks());

  test("requires authentication and delegates sign-in without registration", async () => {
    const auth: AuthGateway = { getSession: vi.fn().mockResolvedValue(null), signIn: vi.fn().mockResolvedValue(undefined), signOut: vi.fn(), onChange: vi.fn(() => () => {}) };
    const repository = { list: vi.fn(), create: vi.fn(), softDelete: vi.fn() } as MeasurementRepository;
    render(<App auth={auth} repository={repository} />);
    const user = userEvent.setup();
    await user.type(await screen.findByLabelText("Correo electrónico"), "user@example.com");
    await user.type(screen.getByLabelText("Contraseña"), "secret");
    await user.click(screen.getByRole("button", { name: "Acceder" }));
    expect(auth.signIn).toHaveBeenCalledWith("user@example.com", "secret");
  });

  test("confirms two readings and persists only their averages", async () => {
    const { auth, repository } = authenticated(); render(<App auth={auth} repository={repository} />); const user = userEvent.setup();
    await user.click(await screen.findByRole("button", { name: /Nueva medición/ }));
    await enterReading(user, "121", "79", "61"); await enterReading(user, "122", "82", "62");
    expect(screen.getByText("Revisar y confirmar")).toBeInTheDocument();
    await user.type(screen.getByLabelText(/Nota/), "  tranquila  ");
    await user.click(screen.getByRole("button", { name: "Confirmar y guardar" }));
    await waitFor(() => expect(repository.create).toHaveBeenCalledWith(expect.objectContaining({ systolic: 122, diastolic: 81, pulse: 62, notes: "tranquila" })));
    const saved = vi.mocked(repository.create).mock.calls[0][0] as unknown as Record<string, unknown>;
    expect(saved).not.toHaveProperty("first"); expect(saved).not.toHaveProperty("second");
  });

  test("cancel does not persist anything", async () => {
    const { auth, repository } = authenticated(); render(<App auth={auth} repository={repository} />); const user = userEvent.setup();
    await user.click(await screen.findByRole("button", { name: /Nueva medición/ }));
    await enterReading(user, "120", "80", "60");
    await user.click(screen.getByRole("button", { name: "Cancelar" }));
    expect(repository.create).not.toHaveBeenCalled(); expect(screen.getByRole("button", { name: /Nueva medición/ })).toBeInTheDocument();
  });

  test("loads filtered history and confirms soft deletion", async () => {
    const { auth, repository } = authenticated(); render(<App auth={auth} repository={repository} />); const user = userEvent.setup();
    expect(await screen.findByText("Después del paseo")).toBeInTheDocument();
    await user.type(screen.getByLabelText("Desde"), "2026-08-01");
    await waitFor(() => expect(repository.list).toHaveBeenCalledWith({ from: "2026-08-01", to: "" }));
    vi.spyOn(window, "confirm").mockReturnValue(true);
    await user.click(screen.getByRole("button", { name: /Eliminar medición/ }));
    expect(repository.softDelete).toHaveBeenCalledWith("m1");
    await waitFor(() => expect(screen.queryByText("Después del paseo")).not.toBeInTheDocument());
  });
});
