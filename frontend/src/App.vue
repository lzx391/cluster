<script setup>
import { ref, watch, onMounted } from "vue";
import { ElMessage } from "element-plus";
import { Moon, Sunny } from "@element-plus/icons-vue";
import http from "./api/http.js";

const THEME_KEY = "cluster_theme";

const username = ref("alice");
const email = ref("alice@example.com");
const userResult = ref(null);
const userLoading = ref(false);

const products = ref([]);
const productsLoading = ref(false);

const lastUserId = ref(
  typeof localStorage !== "undefined" ? localStorage.getItem("cluster_lastUserId") ?? "" : ""
);
const productId = ref(1);
const quantity = ref(1);
const orderResult = ref(null);
const orderLoading = ref(false);

function readInitialDark() {
  if (typeof localStorage === "undefined") return false;
  const saved = localStorage.getItem(THEME_KEY);
  if (saved === "dark") return true;
  if (saved === "light") return false;
  if (typeof window !== "undefined" && window.matchMedia("(prefers-color-scheme: dark)").matches) {
    return true;
  }
  return false;
}

const isDark = ref(readInitialDark());

function applyDarkClass(dark) {
  document.documentElement.classList.toggle("dark", dark);
  if (typeof localStorage !== "undefined") {
    localStorage.setItem(THEME_KEY, dark ? "dark" : "light");
  }
}

watch(isDark, applyDarkClass, { immediate: true });

onMounted(() => {
  loadProducts();
});

function formatError(e) {
  const d = e.response?.data;
  if (typeof d === "string") return d;
  if (d && typeof d === "object") return JSON.stringify(d);
  return e.message || "请求失败";
}

async function createUser() {
  userResult.value = null;
  userLoading.value = true;
  try {
    const { data } = await http.post("/api/users", {
      username: username.value.trim(),
      email: email.value.trim(),
    });
    userResult.value = data;
    lastUserId.value = String(data.id);
    localStorage.setItem("cluster_lastUserId", lastUserId.value);
    ElMessage.success("用户创建成功");
  } catch (e) {
    ElMessage.error(formatError(e));
  } finally {
    userLoading.value = false;
  }
}

async function loadProducts() {
  productsLoading.value = true;
  try {
    const { data } = await http.get("/api/products");
    products.value = Array.isArray(data) ? data : [];
  } catch (e) {
    products.value = [];
    ElMessage.error(formatError(e));
  } finally {
    productsLoading.value = false;
  }
}

async function placeOrder() {
  orderResult.value = null;
  const uid = Number(lastUserId.value);
  const pid = Number(productId.value);
  const qty = Number(quantity.value);
  if (!Number.isFinite(uid) || uid < 1) {
    ElMessage.warning("请先创建用户，或填写有效用户 ID");
    return;
  }
  if (!Number.isFinite(pid) || pid < 1) {
    ElMessage.warning("请填写有效商品 ID");
    return;
  }
  if (!Number.isFinite(qty) || qty < 1) {
    ElMessage.warning("数量至少为 1");
    return;
  }
  orderLoading.value = true;
  try {
    const { data } = await http.post("/api/orders", {
      userId: uid,
      productId: pid,
      quantity: qty,
    });
    orderResult.value = data;
    ElMessage.success("订单创建成功");
    await loadProducts();
  } catch (e) {
    ElMessage.error(formatError(e));
  } finally {
    orderLoading.value = false;
  }
}
</script>

<template>
  <el-container class="layout">
    <el-header height="auto" style="padding: 24px 24px 0">
      <div class="header-row">
        <div>
          <h1 class="page-title">cluster 微服务演示台</h1>
          <p class="page-sub mb-0">
            使用 <strong>Axios</strong> 请求经 Vite 代理到网关
            <el-tag size="small" type="primary">8080</el-tag>
            。先创建用户，再下单；表格数据来自 product-service。
          </p>
        </div>
        <div class="theme-switch" aria-label="主题">
          <el-switch
            v-model="isDark"
            size="large"
            inline-prompt
            :active-icon="Moon"
            :inactive-icon="Sunny"
            style="--el-switch-on-color: #4c4d4f; --el-switch-off-color: #ffd666"
          />
        </div>
      </div>
    </el-header>

    <el-main style="padding: 16px 24px 32px">
      <el-row :gutter="16">
        <el-col :xs="24" :md="12">
          <el-card shadow="hover">
            <template #header>
              <span>用户 · user-service</span>
            </template>
            <el-form label-position="top" @submit.prevent>
              <el-form-item label="用户名">
                <el-input v-model="username" autocomplete="username" clearable />
              </el-form-item>
              <el-form-item label="邮箱">
                <el-input v-model="email" type="email" autocomplete="email" clearable />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="userLoading" @click="createUser">创建用户</el-button>
              </el-form-item>
            </el-form>
            <el-alert
              v-if="userResult"
              type="success"
              :closable="false"
              show-icon
              title="最新返回"
            />
            <pre v-if="userResult" class="json-preview">{{ JSON.stringify(userResult, null, 2) }}</pre>
            <el-text v-else type="info" size="small">创建成功后会记下用户 ID 供下单使用。</el-text>
          </el-card>
        </el-col>

        <el-col :xs="24" :md="12">
          <el-card shadow="hover">
            <template #header>
              <span>订单 · order-service</span>
            </template>
            <el-form label-position="top" @submit.prevent>
              <el-form-item label="用户 ID">
                <el-input v-model="lastUserId" placeholder="创建用户后自动填入" clearable />
              </el-form-item>
              <el-row :gutter="12">
                <el-col :span="12">
                  <el-form-item label="商品 ID">
                    <el-input-number v-model="productId" :min="1" :step="1" style="width: 100%" controls-position="right" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="数量">
                    <el-input-number v-model="quantity" :min="1" :step="1" style="width: 100%" controls-position="right" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item>
                <el-button type="primary" :loading="orderLoading" @click="placeOrder">创建订单</el-button>
              </el-form-item>
            </el-form>
            <el-alert v-if="orderResult" type="success" :closable="false" show-icon title="订单返回" />
            <pre v-if="orderResult" class="json-preview">{{ JSON.stringify(orderResult, null, 2) }}</pre>
            <el-text type="info" size="small">成功时可查看 user-service 控制台「异步通知」日志。</el-text>
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="hover" style="margin-top: 16px">
        <template #header>
          <div style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px">
            <span>商品 · product-service</span>
            <el-button size="small" :loading="productsLoading" @click="loadProducts">刷新</el-button>
          </div>
        </template>
        <el-table v-loading="productsLoading" :data="products" stripe border style="width: 100%" empty-text="暂无数据，请确认网关与 product-service 已启动">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="name" label="名称" min-width="140" />
          <el-table-column prop="price" label="价格" width="120" />
          <el-table-column prop="stock" label="库存" width="100" />
        </el-table>
      </el-card>
    </el-main>
  </el-container>
</template>
