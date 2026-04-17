import axios from "axios";

/** 与 Vite 代理一致：默认走相对路径 `/api`，开发时转发到网关 */
const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || "",
  timeout: 30_000,
  headers: { "Content-Type": "application/json" },
});

export default http;
