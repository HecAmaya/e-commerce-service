import type { Product } from '../services/api'
import ProductCard from './ProductCard'

type Props = {
  products: Product[]
  onEdit: (product: Product) => void
  onDelete: (id: number) => void
  onPurchase: (id: number) => void
}

export default function ProductCatalog({ products, onEdit, onDelete, onPurchase }: Props) {
  return <div className="catalog">
    <div className="section-title"><h2>Catalog</h2><span>Manage inventory</span></div>
    <div className="grid">{products.map(product => <ProductCard key={product.id} product={product} onEdit={onEdit} onDelete={onDelete} onPurchase={onPurchase} />)}</div>
    {!products.length && <div className="empty">No products found. Add one or import the sample CSV.</div>}
  </div>
}
