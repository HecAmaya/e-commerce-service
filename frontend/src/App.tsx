import { FormEvent, useEffect, useState } from 'react'

type Product = {
  id: number
  name: string
  sku: string
  description: string
  category: string
  price: number
  stock: number
  weightKg: number
}
type Form = Omit<Product, 'id'>
const empty: Form = { name: '', sku: '', description: '', category: '', price: 0, stock: 0, weightKg: 0 }

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const headers = options?.body instanceof FormData ? options.headers : { 'Content-Type': 'application/json', ...(options?.headers || {}) }
  const response = await fetch(url, { ...options, headers })
  if (!response.ok) {
    const body = await response.json().catch(() => ({ error: response.statusText }))
    throw new Error(body.error || response.statusText)
  }
  return response.status === 204 ? undefined as T : response.json()
}

export default function App() {
  const [products, setProducts] = useState<Product[]>([])
  const [query, setQuery] = useState('')
  const [form, setForm] = useState<Form>(empty)
  const [editing, setEditing] = useState<number | null>(null)
  const [purchase, setPurchase] = useState<{ id: number, quantity: number } | null>(null)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const load = async (q = query) => {
    try { setProducts(await request<Product[]>(`/api/products${q ? `?q=${encodeURIComponent(q)}` : ''}`)); setError('') }
    catch (e) { setError((e as Error).message) }
  }
  useEffect(() => { load('') }, [])

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    try {
      await request<Product>(editing ? `/api/products/${editing}` : '/api/products', { method: editing ? 'PUT' : 'POST', body: JSON.stringify(form) })
      setForm(empty); setEditing(null); setMessage(editing ? 'Product updated.' : 'Product created.'); await load()
    } catch (e) { setError((e as Error).message) }
  }
  const remove = async (id: number) => {
    if (!window.confirm('Delete this product?')) return
    try { await request<void>(`/api/products/${id}`, { method: 'DELETE' }); setMessage('Product deleted.'); await load() }
    catch (e) { setError((e as Error).message) }
  }
  const buy = async () => {
    if (!purchase) return
    try {
      const order = await request<{ id: number, total: number }>('/api/orders', { method: 'POST', body: JSON.stringify({ productId: purchase.id, quantity: purchase.quantity }) })
      setPurchase(null); setMessage(`Order #${order.id} confirmed. Total: $${Number(order.total).toFixed(2)}`); await load()
    } catch (e) { setError((e as Error).message) }
  }
  const importFile = async (file?: File) => {
    if (!file) return
    const data = new FormData(); data.append('file', file)
    try {
      const result = await request<{ imported: number, rejected: number, errors: { row: number, sku: string, reason: string }[] }>('/api/products/import', { method: 'POST', headers: {}, body: data })
      const details = result.errors.length ? ` Rejected: ${result.errors.map(x => `row ${x.row} (${x.sku || 'no SKU'}): ${x.reason}`).join(', ')}` : ''
      setMessage(`Imported ${result.imported}; rejected ${result.rejected}.${details}`); await load()
    } catch (e) { setError((e as Error).message) }
  }
  const update = (key: keyof Form, value: string) => setForm({ ...form, [key]: ['price', 'stock', 'weightKg'].includes(key) ? Number(value) : value })

  return <main>
    <header><div><span className="eyebrow">OPERATIONS</span><h1>Commerce Console</h1><p>Product catalog and order fulfillment</p></div>
      <label className="import">Import CSV<input type="file" accept=".csv,text/csv" onChange={e => importFile(e.target.files?.[0])} /></label>
    </header>
    {(message || error) && <div className={error ? 'notice error' : 'notice'}>{error || message}<button onClick={() => { setMessage(''); setError('') }}>×</button></div>}
    <section className="toolbar"><div className="search"><span>⌕</span><input value={query} placeholder="Search products, SKU, category..." onChange={e => setQuery(e.target.value)} onKeyDown={e => e.key === 'Enter' && load()} /><button onClick={() => load()}>Search</button></div><span className="count">{products.length} products</span></section>
    <section className="layout">
      <div className="catalog"><div className="section-title"><h2>Catalog</h2><span>Manage inventory</span></div>
        <div className="grid">{products.map(product => <article className="card" key={product.id}><div className="card-top"><span className="category">{product.category}</span><span className={product.stock > 0 ? 'in-stock' : 'out-stock'}>{product.stock > 0 ? `${product.stock} in stock` : 'Out of stock'}</span></div><h3>{product.name}</h3><p className="sku">{product.sku}</p><p className="description">{product.description}</p><div className="card-bottom"><strong>${Number(product.price).toFixed(2)}</strong><span>{Number(product.weightKg).toFixed(3)} kg</span></div><div className="actions"><button onClick={() => { setEditing(product.id); setForm({ name: product.name, sku: product.sku, description: product.description, category: product.category, price: product.price, stock: product.stock, weightKg: product.weightKg }) }}>Edit</button><button onClick={() => remove(product.id)}>Delete</button><button className="primary" disabled={!product.stock} onClick={() => setPurchase({ id: product.id, quantity: 1 })}>Purchase</button></div></article>)}</div>
        {!products.length && <div className="empty">No products found. Add one or import the sample CSV.</div>}
      </div>
      <aside><div className="section-title"><h2>{editing ? 'Edit product' : 'Add product'}</h2><span>{editing ? 'Update details' : 'New inventory item'}</span></div><form onSubmit={submit}>{(['name', 'sku', 'category'] as const).map(key => <label key={key}>{key === 'sku' ? 'SKU' : key[0].toUpperCase() + key.slice(1)}<input required value={form[key]} onChange={e => update(key, e.target.value)} /></label>)}<label>Description<textarea required rows={3} value={form.description} onChange={e => update('description', e.target.value)} /></label><div className="fields"><label>Price<input required min="0" step=".01" type="number" value={form.price} onChange={e => update('price', e.target.value)} /></label><label>Stock<input required min="0" type="number" value={form.stock} onChange={e => update('stock', e.target.value)} /></label><label>Weight (kg)<input required min="0" step=".001" type="number" value={form.weightKg} onChange={e => update('weightKg', e.target.value)} /></label></div><div className="form-actions"><button type="submit" className="primary">{editing ? 'Save changes' : 'Create product'}</button>{editing && <button type="button" onClick={() => { setEditing(null); setForm(empty) }}>Cancel</button>}</div></form></aside>
    </section>
    {purchase && <div className="modal-backdrop"><div className="modal"><h2>Confirm purchase</h2><p>How many units would you like to purchase?</p><input min="1" type="number" value={purchase.quantity} onChange={e => setPurchase({ ...purchase, quantity: Number(e.target.value) })} /><div className="form-actions"><button className="primary" onClick={buy}>Confirm order</button><button onClick={() => setPurchase(null)}>Cancel</button></div></div></div>}
  </main>
}
