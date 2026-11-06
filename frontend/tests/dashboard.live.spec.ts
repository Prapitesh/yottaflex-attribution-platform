import { expect, test } from "@playwright/test";

test.skip(!process.env.YOTTAFLEX_LIVE_API, "Set YOTTAFLEX_LIVE_API=1 when the local Spring Boot/PostgreSQL API is running.");

test("real browser dashboard reads PostgreSQL data and applies a model/run filter", async ({ page, request }) => {
  const api = await request.get("http://localhost:8080/api/v1/dataset-runs");
  expect(api.ok()).toBeTruthy();
  const runs = await api.json();
  expect(runs.length).toBeGreaterThan(0);

  await page.goto("/");
  await expect(page.getByRole("heading", { name: "Attribution overview" })).toBeVisible();
  await expect(page.locator(".campaign-panel tbody tr").first()).toBeVisible({ timeout: 30_000 });

  const targetRun = runs.find((run: { status: string }) => run.status === "VALIDATED") ?? runs[0];
  const filteredSummary = page.waitForResponse((response) =>
    response.url().includes("/api/v1/dashboard/summary") &&
    response.url().includes("runId=" + targetRun.id) &&
    response.status() === 200
  );
  await page.getByLabel("Dataset run").selectOption(targetRun.id);
  await filteredSummary;

  const modelResponse = page.waitForResponse((response) =>
    response.url().includes("/api/v1/campaigns") &&
    response.url().includes("model=FIRST_TOUCH") &&
    response.status() === 200
  );
  await page.getByLabel("Attribution model").selectOption("FIRST_TOUCH");
  await modelResponse;
  await expect(page.locator(".campaign-panel tbody tr").first()).toBeVisible();
});
