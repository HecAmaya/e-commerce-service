import { FormEvent, useEffect, useState } from 'react'
import { deleteProduct, importProducts, loadProducts, purchaseProduct, saveProduct, type Product, type ProductForm } from './services/api'
import ProductCatalog from './components/ProductCatalog'
import ProductFormView from './components/ProductForm'
import PurchaseModal from './components/PurchaseModal'

const empty: ProductForm = { name: '', sku: '', description: '', category: '', price: 0, stock: 0, weightKg: 0 }

export default function App() {
  const [products, setProducts] = useState<Product[]>([])
  const [query, setQuery] = useState('')
  const [category, setCategory] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [totalElements, setTotalElements] = useState(0)
  const [form, setForm] = useState<ProductForm>(empty)
  const [editing, setEditing] = useState<Product | null>(null)
  const [purchase, setPurchase] = useState<{ id: number, quantity: number } | null>(null)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const load = async (q = query, selectedCategory = category, selectedPage = page) => {
    try {
      const result = await loadProducts(q, selectedCategory, selectedPage)
      setProducts(result.content); setTotalPages(result.totalPages); setTotalElements(result.totalElements); setError('')
    }
    catch (e) { setError((e as Error).message) }
  }
  useEffect(() => { void load('', '', 0) }, [])

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    try {
      await saveProduct(form, editing?.id)
      setForm(empty); setEditing(null); setMessage(editing ? 'Product updated.' : 'Product created.'); await load()
    } catch (e) { setError((e as Error).message) }
  }
  const remove = async (id: number) => {
    if (!window.confirm('Delete this product?')) return
    try { await deleteProduct(id); setMessage('Product deleted.'); await load() }
    catch (e) { setError((e as Error).message) }
  }
  const buy = async () => {
    if (!purchase) return
    try {
      const order = await purchaseProduct(purchase.id, purchase.quantity)
      setPurchase(null); setMessage(`Order #${order.id} confirmed. Total: $${Number(order.total).toFixed(2)}`); await load()
    } catch (e) { setError((e as Error).message) }
  }
  const importFile = async (file?: File) => {
    if (!file) return
    try {
      const result = await importProducts(file)
      const details = result.errors.length ? ` Rejected: ${result.errors.map(x => `row ${x.row} (${x.sku || 'no SKU'}): ${x.reason}`).join(', ')}` : ''
      setMessage(`Imported ${result.imported}; rejected ${result.rejected}.${details}`); await load()
    } catch (e) { setError((e as Error).message) }
  }
  const update = (key: keyof ProductForm, value: string) => setForm({ ...form, [key]: ['price', 'stock', 'weightKg'].includes(key) ? Number(value) : value })
  const edit = (product: Product) => {
    setEditing(product)
    setForm({ name: product.name, sku: product.sku, description: product.description, category: product.category, price: product.price, stock: product.stock, weightKg: product.weightKg })
  }

  return <main>
    <header><div><span className="eyebrow">OPERATIONS</span><h1>Commerce Console</h1><p>Product catalog and order fulfillment</p></div>
      <label className="import">Import CSV<input type="file" accept=".csv,text/csv" onChange={e => void importFile(e.target.files?.[0])} /></label>
    </header>
    {(message || error) && <div className={error ? 'notice error' : 'notice'}>{error || message}<button onClick={() => { setMessage(''); setError('') }}>×</button></div>}
    <section className="toolbar"><div className="search"><span>⌕</span><input value={query} placeholder="Search products or SKU..." onChange={e => setQuery(e.target.value)} onKeyDown={e => e.key === 'Enter' && void load(query, category, 0)} /><input value={category} placeholder="Category" onChange={e => setCategory(e.target.value)} onKeyDown={e => e.key === 'Enter' && void load(query, category, 0)} /><button onClick={() => { setPage(0); void load(query, category, 0) }}>Search</button></div><span className="count">{totalElements} products</span></section>
    <section className="layout"><ProductCatalog products={products} onEdit={edit} onDelete={remove} onPurchase={id => setPurchase({ id, quantity: 1 })} /><ProductFormView editing={editing} form={form} onChange={update} onSubmit={submit} onCancel={() => { setEditing(null); setForm(empty) }} /></section>
    <nav aria-label="Catalog pages"><button disabled={page === 0} onClick={() => { const next = page - 1; setPage(next); void load(query, category, next) }}>Previous</button><span>Page {page + 1} of {totalPages}</span><button disabled={page + 1 >= totalPages} onClick={() => { const next = page + 1; setPage(next); void load(query, category, next) }}>Next</button></nav>
    {purchase && <PurchaseModal quantity={purchase.quantity} onQuantityChange={quantity => setPurchase({ ...purchase, quantity })} onConfirm={() => void buy()} onCancel={() => setPurchase(null)} />}
  </main>
}
