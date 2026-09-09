import { FormEvent, useEffect, useState } from 'react'
import { deleteProduct, importProducts, loadProducts, purchaseProduct, saveProduct, type ImportResponse, type Product, type ProductForm } from './services/api'
import ProductCatalog from './components/ProductCatalog'
import ProductFormView from './components/ProductForm'
import PurchaseModal from './components/PurchaseModal'

const empty: ProductForm = { name: '', sku: '', description: '', category: '', price: 0, stock: 0, weightKg: 0 }

export default function App() {
  const [products, setProducts] = useState<Product[]>([])
  const [query, setQuery] = useState('')
  const [category, setCategory] = useState('')
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [totalPages, setTotalPages] = useState(1)
  const [totalElements, setTotalElements] = useState(0)
  const [form, setForm] = useState<ProductForm>(empty)
  const [editing, setEditing] = useState<Product | null>(null)
  const [purchase, setPurchase] = useState<{ product: Product, quantity: number } | null>(null)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [importErrors, setImportErrors] = useState<ImportResponse['errors']>([])

  const load = async (q = query, selectedCategory = category, selectedPage = page, selectedPageSize = pageSize) => {
    try {
      const result = await loadProducts(q, selectedCategory, selectedPage, selectedPageSize)
      setProducts(result.content); setTotalPages(result.totalPages); setTotalElements(result.totalElements); setError('')
    }
    catch (e) { setError((e as Error).message) }
  }
  useEffect(() => { void load('', '', 0, 10) }, [])

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    try {
      await saveProduct(form, editing?.id)
      setForm(empty); setEditing(null); setMessage(editing ? 'Product updated.' : 'Product created.'); setImportErrors([]); await load()
    } catch (e) { setError((e as Error).message) }
  }
  const remove = async (id: number) => {
    if (!window.confirm('Delete this product?')) return
    try { await deleteProduct(id); setMessage('Product deleted.'); await load() }
    catch (e) { setError((e as Error).message) }
  }
  const buy = async () => {
    if (!purchase) return
    if (purchase.quantity > purchase.product.stock) {
      setError(`Only ${purchase.product.stock} unit${purchase.product.stock === 1 ? '' : 's'} of ${purchase.product.name} remain available.`)
      return
    }
    try {
      const order = await purchaseProduct(purchase.product.id, purchase.quantity)
      setPurchase(null); setMessage(`Order #${order.id} confirmed. Total: $${Number(order.total).toFixed(2)}`); await load()
    } catch (e) { setError((e as Error).message) }
  }
  const importFile = async (file?: File) => {
    if (!file) return
    try {
      const result = await importProducts(file)
      setMessage(`Imported ${result.imported}; rejected ${result.rejected}.`)
      setImportErrors(result.errors)
      await load()
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
    {(message || error) && <div className={error ? 'notice error' : 'notice'}>{error || message}<button onClick={() => { setMessage(''); setError(''); setImportErrors([]) }}>×</button></div>}
    {importErrors.length > 0 && <section className="import-errors" aria-label="Import errors">
      <div className="import-errors-heading"><strong>Import review</strong><span>{importErrors.length} row{importErrors.length === 1 ? '' : 's'} need attention</span></div>
      <div className="import-error-list">{importErrors.map(error => <div className="import-error" key={`${error.row}-${error.sku}`}>
        <span className="error-row">Row {error.row}</span>
        <span><strong>Name:</strong> {error.name || '—'}</span>
        <span><strong>SKU:</strong> {error.sku || '—'}</span>
        <span><strong>Quantity:</strong> {error.quantity || '—'}</span>
        <span className="error-reason"><strong>Issue:</strong> {error.reason}</span>
      </div>)}</div>
    </section>}
    <section className="toolbar"><div className="search"><span>⌕</span><input value={query} placeholder="Search products or SKU..." onChange={e => setQuery(e.target.value)} onKeyDown={e => e.key === 'Enter' && void load(query, category, 0)} /><input value={category} placeholder="Category" onChange={e => setCategory(e.target.value)} onKeyDown={e => e.key === 'Enter' && void load(query, category, 0)} /><button onClick={() => { setPage(0); void load(query, category, 0) }}>Search</button></div><span className="count">{totalElements} products</span></section>
    <section className="layout"><ProductFormView editing={editing} form={form} onChange={update} onSubmit={submit} onCancel={() => { setEditing(null); setForm(empty) }} /><ProductCatalog products={products} page={page} totalPages={totalPages} pageSize={pageSize} onPageChange={next => { setPage(next); void load(query, category, next) }} onPageSizeChange={nextSize => { setPageSize(nextSize); setPage(0); void load(query, category, 0, nextSize) }} onEdit={edit} onDelete={remove} onPurchase={product => setPurchase({ product, quantity: 1 })} /></section>
    {purchase && <PurchaseModal productName={purchase.product.name} availableStock={purchase.product.stock} quantity={purchase.quantity} onQuantityChange={quantity => setPurchase({ ...purchase, quantity })} onConfirm={() => void buy()} onCancel={() => setPurchase(null)} />}
  </main>
}
