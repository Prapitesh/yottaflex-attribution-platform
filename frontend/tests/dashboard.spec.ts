import { expect, test } from "@playwright/test";

const runOne = { id: "run-001", status: "VALIDATED" };
const runTwo = { id: "run-new", status: "GENERATED" };
const campaigns = [{
  campaign_id: "CMP-001",
  campaign_name: "Campaign 1 - Search",
  spend: 1000,
  attributed_revenue: 2500,
  roi_percent: 150,
  roi_status: "CALCULATED",
}];

async function mockApi(page: import("@playwright/test").Page) {
  const requests: string[] = [];
  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    requests.push(request.method() + " " + url.pathname + url.search + " " + (request.postData() ?? ""));
    const path = url.pathname.replace("/api/v1", "");
    let body: unknown = {};

    if (request.method() === "GET" && path === "/dataset-runs") {
      body = [runOne, ...(requests.some((x) => x.includes("/admin/generate")) ? [runTwo] : [])];
    } else if (request.method() === "GET" && path === "/dashboard/summary") {
      body = {
        recognizedRevenue: 2500,
        reconciledSpend: 1000,
        counts: {
          campaigns: 1, contacts: 20, touchpoints: 50, web_sessions: 10,
          leads: 8, opportunities: 4, orders: 3, data_quality_issues: 0,
        },
      };
    } else if (request.method() === "GET" && path === "/campaigns") {
      body = campaigns;
    } else if (request.method() === "GET" && path === "/quality/issues") {
      body = [];
    } else if (request.method() === "GET" && path === "/attribution/results") {
      body = [{ model_name: url.searchParams.get("model"), attributed_revenue: 2500, opportunities: 1, credits: 1 }];
    } else if (request.method() === "POST" && path === "/admin/generate") {
      body = { runId: "run-new", campaigns: 12, contacts: 300, touchpoints: 1201, opportunities: 80 };
    } else if (request.method() === "POST" && path.startsWith("/admin/recalculate")) {
      body = { results: 3, opportunitiesEvaluated: 1 };
    } else if (request.method() === "POST" && path === "/admin/validate") {
      body = { issues: 5 };
    }

    await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(body) });
  });
  return requests;
}

test("loads dashboard data and renders campaign performance", async ({ page }) => {
  await mockApi(page);
  await page.goto("/");
  await expect(page.getByRole("heading", { name: "Attribution overview" })).toBeVisible();
  await expect(page.getByText("Recognized revenue")).toBeVisible();
  await expect(page.getByText("Campaign 1 - Search")).toBeVisible();
  await expect(page.getByRole("combobox", { name: "Dataset run" })).toBeVisible();
  await expect(page.getByRole("combobox", { name: "Attribution model" })).toHaveValue("LINEAR");
});

test("model selection and dataset run filters refresh the dashboard", async ({ page }) => {
  const requests = await mockApi(page);
  await page.goto("/");
  await page.getByLabel("Attribution model").selectOption("FIRST_TOUCH");
  await expect.poll(() => requests.some((x) => x.includes("/campaigns?model=FIRST_TOUCH"))).toBeTruthy();
  await page.getByRole("button", { name: /Last touch/ }).click();
  await expect.poll(() => requests.some((x) => x.includes("/campaigns?model=LAST_TOUCH"))).toBeTruthy();
  await page.getByRole("button", { name: /Linear/ }).click();
  await expect.poll(() => requests.some((x) => x.includes("/campaigns?model=LINEAR"))).toBeTruthy();
  await page.getByLabel("Dataset run").selectOption("run-001");
  await expect.poll(() => requests.some((x) => x.includes("runId=run-001"))).toBeTruthy();
});

test("sidebar navigation scrolls to implemented dashboard sections", async ({ page }) => {
  await mockApi(page);
  await page.goto("/");
  for (const [label, hash] of [
    ["Campaign analytics", "#campaign-analytics"],
    ["Attribution paths", "#attribution-models"],
    ["Data quality", "#data-quality"],
    ["Overview", "#overview"],
  ] as const) {
    await page.getByRole("link", { name: new RegExp(label) }).click();
    await expect(page).toHaveURL(new RegExp(hash + "$"));
    await expect(page.locator(hash)).toBeVisible();
  }
});

test("dashboard actions call the correct API and show feedback", async ({ page }) => {
  const requests = await mockApi(page);
  await page.goto("/");
  await page.getByRole("button", { name: /Run attribution/ }).click();
  await expect(page.getByText(/Attribution calculated:/)).toBeVisible();
  await expect.poll(() => requests.some((x) => x.startsWith("POST /api/v1/admin/recalculate"))).toBeTruthy();

  await page.getByRole("button", { name: /Run data quality checks/ }).click();
  await expect(page.getByText(/Validation complete:/)).toBeVisible();
  await expect.poll(() => requests.some((x) => x.startsWith("POST /api/v1/admin/validate"))).toBeTruthy();

  await page.getByRole("button", { name: /Generate starter data/ }).click();
  await expect(page.getByText(/Synthetic data generated:/)).toBeVisible();
  await expect(page.getByLabel("Dataset run")).toHaveValue("run-new");
});

test("full dataset action is wired and reports its result", async ({ page }) => {
  const requests = await mockApi(page);
  await page.goto("/");
  await page.getByRole("button", { name: /Generate full dataset/ }).click();
  await expect(page.getByText(/Synthetic data generated:/)).toBeVisible();
  await expect.poll(() => requests.some((x) => x.startsWith("POST /api/v1/admin/generate") && x.includes('"scale":"FULL"'))).toBeTruthy();
});

test("API failures are visible instead of leaving a blank dashboard", async ({ page }) => {
  await mockApi(page);
  await page.route("**/api/v1/dashboard/summary", (route) =>
    route.fulfill({ status: 503, contentType: "application/json", body: JSON.stringify({ message: "Dashboard service temporarily unavailable" }) })
  );
  await page.goto("/");
  await expect(page.getByText("Dashboard service temporarily unavailable")).toBeVisible();
});

test("refresh button reloads dashboard data", async ({ page }) => {
  const requests = await mockApi(page);
  await page.goto("/");
  const before = requests.filter((x) => x.startsWith("GET /api/v1/dashboard/summary")).length;
  await page.getByRole("button", { name: "Refresh dashboard" }).click();
  await expect.poll(() => requests.filter((x) => x.startsWith("GET /api/v1/dashboard/summary")).length).toBeGreaterThan(before);
});

test("mobile layout has no page-level horizontal overflow", async ({ page }) => {
  await mockApi(page);
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/");
  await expect(page.getByRole("heading", { name: "Attribution overview" })).toBeVisible();
  const dimensions = await page.evaluate(() => ({
    viewport: document.documentElement.clientWidth,
    content: document.documentElement.scrollWidth,
  }));
  expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport);
  await page.getByRole("link", { name: /Data quality/ }).click();
  await expect(page).toHaveURL(/#data-quality$/);
});
