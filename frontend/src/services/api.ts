import axios from "axios";
export const api = axios.create({baseURL:"http://localhost:8080/api/v1",timeout:30000});
export async function getSummary(){return (await api.get("/dashboard/summary")).data;}
export async function getCampaigns(model:string){return (await api.get("/campaigns",{params:{model}})).data;}
export async function getIssues(){return (await api.get("/quality/issues")).data;}
export async function generate(scale:"STARTER"|"FULL"){return (await api.post("/admin/generate",{seed:20261022,scale})).data;}
export async function recalculate(){return (await api.post("/admin/recalculate")).data;}
export async function validateData(){return (await api.post("/admin/validate")).data;}
