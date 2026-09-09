export type Product = {
  id: number
  name: string
  sku: string
  description: string
  category: string
  price: number
  stock: number
  weightKg: number
}

export type ProductForm = Omit<Product, 'id'>
export type ProductPage = { content: Product[], totalElements: number, totalPages: number, number: number, size: number }
export type ImportResponse = { imported: number, rejected: number, errors: { row: number, quantity: string, sku: string, name: string, reason: string }[] }
export type OrderResponse = { id: number, total: number }

export async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const headers = options?.body instanceof FormData
    ? options.headers
    : { 'Content-Type': 'application/json', ...(options?.headers || {}) }
  const response = await fetch(url, { ...options, headers })
  if (!response.ok) {
    const body = await response.json().catch(() => ({ error: response.statusText }))
    throw new Error(body.error || response.statusText)
  }
  return response.status === 204 ? undefined as T : response.json()
}

export async function loadProducts(query = '', category = '', page = 0, size = 10): Promise<ProductPage> {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (query) params.set('q', query)
  if (category) params.set('category', category)
  const result = await request<Product[] | ProductPage>(`/api/products?${params}`)
  return Array.isArray(result)
    ? { content: result, totalElements: result.length, totalPages: 1, number: 0, size: result.length }
    : result
}

export function saveProduct(product: ProductForm, id?: number | null): Promise<Product> {
  return request<Product>(id ? `/api/products/${id}` : '/api/products', {
    method: id ? 'PUT' : 'POST',
    body: JSON.stringify(product)
  })
}

export function deleteProduct(id: number): Promise<void> {
  return request<void>(`/api/products/${id}`, { method: 'DELETE' })
}

export function purchaseProduct(id: number, quantity: number): Promise<OrderResponse> {
  return request<OrderResponse>('/api/orders', {
    method: 'POST',
    body: JSON.stringify({ productId: id, quantity })
  })
}

export function importProducts(file: File): Promise<ImportResponse> {
  const data = new FormData()
  data.append('file', file)
  return request<ImportResponse>('/api/products/import', { method: 'POST', headers: {}, body: data })
}
