import axios from "axios";

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api/v1",
  timeout: 60000,
});

export type AttributionModel = "FIRST_TOUCH" | "LAST_TOUCH" | "LINEAR";
export type GenerationScale = "STARTER" | "FULL";

export async function getDatasetRuns() {
  return (await api.get("/dataset-runs")).data;
}

export async function getSummary(runId?: string) {
  return (await api.get("/dashboard/summary", { params: runId ? { runId } : {} })).data;
}

export async function getCampaigns(model: AttributionModel, runId?: string) {
  return (await api.get("/campaigns", { params: { model, ...(runId ? { runId } : {}) } })).data;
}

export async function getAttribution(model: AttributionModel, runId?: string) {
  return (await api.get("/attribution/results", { params: { model, ...(runId ? { runId } : {}) } })).data;
}

export async function getIssues(runId?: string) {
  return (await api.get("/quality/issues", { params: runId ? { runId } : {} })).data;
}

export async function generate(scale: GenerationScale) {
  return (await api.post("/admin/generate", { seed: 20261022, scale })).data;
}

export async function recalculate(runId?: string) {
  return (await api.post(runId ? `/admin/recalculate/${encodeURIComponent(runId)}` : "/admin/recalculate")).data;
}

export async function validateData(runId?: string) {
  return (await api.post("/admin/validate", null, { params: runId ? { runId } : {} })).data;
}
