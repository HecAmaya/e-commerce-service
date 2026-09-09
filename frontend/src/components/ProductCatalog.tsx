import type { Product } from '../services/api'
import ProductCard from './ProductCard'

type Props = {
  products: Product[]
  page: number
  totalPages: number
  pageSize: number
  onPageChange: (page: number) => void
  onPageSizeChange: (pageSize: number) => void
  onEdit: (product: Product) => void
  onDelete: (id: number) => void
  onPurchase: (product: Product) => void
}

export default function ProductCatalog({
  products, page, totalPages, pageSize, onPageChange, onPageSizeChange, onEdit, onDelete, onPurchase
}: Props) {
  return <div className="catalog">
    <div className="section-title">
      <div><h2>Catalog</h2><span>Manage inventory</span></div>
      <div className="catalog-controls">
        <label className="page-size">Products per page<select aria-label="Products per page" value={pageSize} onChange={e => onPageSizeChange(Number(e.target.value))}><option value={10}>10</option><option value={20}>20</option><option value={50}>50</option></select></label>
        <nav aria-label="Catalog pages">
          <button disabled={page === 0} onClick={() => onPageChange(page - 1)}>Previous</button>
          <span>Page {page + 1} of {totalPages}</span>
          <button disabled={page + 1 >= totalPages} onClick={() => onPageChange(page + 1)}>Next</button>
        </nav>
      </div>
    </div>
    <div className="grid">{products.map(product => <ProductCard key={product.id} product={product} onEdit={onEdit} onDelete={onDelete} onPurchase={onPurchase} />)}</div>
    {!products.length && <div className="empty">No products found. Add one or import the sample CSV.</div>}
  </div>
}
