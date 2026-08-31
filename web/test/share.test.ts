import { beforeEach, describe, expect, test, vi } from "vitest";
import { shareOrDownload } from "../src/lib/share.ts";

describe("browser sharing", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    Object.defineProperty(navigator, "share", { configurable: true, value: undefined });
    Object.defineProperty(navigator, "canShare", { configurable: true, value: undefined });
    Object.defineProperty(URL, "createObjectURL", { configurable: true, value: vi.fn(() => "blob:test") });
    Object.defineProperty(URL, "revokeObjectURL", { configurable: true, value: vi.fn() });
  });

  test("uses the standard Web Share API when file sharing is supported", async () => {
    const share = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, "share", { configurable: true, value: share });
    Object.defineProperty(navigator, "canShare", { configurable: true, value: vi.fn(() => true) });
    const file = new File(["data"], "report.pdf", { type: "application/pdf" });
    await expect(shareOrDownload(file)).resolves.toBe("shared");
    expect(share).toHaveBeenCalledWith({ files: [file], title: "miTensión" });
  });

  test("reliably downloads when sharing is unavailable or fails", async () => {
    const click = vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => undefined);
    const file = new File(["data"], "report.csv", { type: "text/csv" });
    await expect(shareOrDownload(file)).resolves.toBe("downloaded");
    expect(click).toHaveBeenCalledOnce();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith("blob:test");
  });
});
